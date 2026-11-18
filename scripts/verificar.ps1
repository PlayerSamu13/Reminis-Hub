<#
.SYNOPSIS
Ejecuta Maven y comprueba que las suites obligatorias realmente se ejecutaron.
.DESCRIPTION
Requiere Java 25, PostgreSQL y DB_URL, DB_USER, DB_PASSWORD, RH_JWT_SECRET
en el proceso. No lee configuraciones privadas de IntelliJ ni imprime secretos.
Los informes llevan un identificador unico para no aceptar resultados antiguos.
.EXAMPLE
.\scripts\verificar.ps1 -Offline
#>
[CmdletBinding()]
param(
    [switch]$Offline,
    [string]$MavenCommand
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
# El codigo no cero de Maven se procesa despues de leer sus informes, tambien en PS 7.
$PSNativeCommandUseErrorActionPreference = $false
$modulePath = Join-Path (Split-Path $PSScriptRoot -Parent) 'reminis-hub'
$changedVariables = @('CATALOGO_DB_TESTS', 'E1A_DB_CHECKS', 'SPRING_JPA_SHOW_SQL')
$previousValues = @{}
foreach ($name in $changedVariables) {
    $previousValues[$name] = [Environment]::GetEnvironmentVariable($name, 'Process')
}
$exitCode = 1
$locationChanged = $false
$logPath = $null
$privateValues = @('DB_URL', 'DB_USER', 'DB_PASSWORD', 'RH_JWT_SECRET') | ForEach-Object {
    [Environment]::GetEnvironmentVariable($_, 'Process')
} | Where-Object { ![string]::IsNullOrEmpty($_) } | Sort-Object Length -Descending
function Protect-Text([string]$text) {
    foreach ($value in $privateValues) { $text = $text.Replace($value, '[OCULTO]') }
    return $text
}
try {
    $missing = @('DB_URL', 'DB_USER', 'DB_PASSWORD', 'RH_JWT_SECRET') | Where-Object {
        [string]::IsNullOrWhiteSpace([Environment]::GetEnvironmentVariable($_, 'Process'))
    }
    if ($missing) { throw ('Faltan variables de entorno: ' + ($missing -join ', ')) }
    if ([Text.Encoding]::UTF8.GetByteCount($env:RH_JWT_SECRET) -lt 32) {
        throw 'RH_JWT_SECRET debe tener al menos 32 bytes UTF-8: el JWT se firma con HMAC-SHA256.'
    }
    $databaseUri = $null
    if (!$env:DB_URL.StartsWith('jdbc:postgresql://', [StringComparison]::Ordinal) -or
        $env:DB_URL -match '[\s;]' -or
        ![Uri]::TryCreate($env:DB_URL.Substring(5), [UriKind]::Absolute, [ref]$databaseUri) -or
        !$databaseUri.Host -or $databaseUri.AbsolutePath -eq '/' -or $databaseUri.UserInfo) {
        throw 'DB_URL invalida: usa solo jdbc:postgresql://host:puerto/base, sin DB_URL= ni otras variables. Configura DB_USER y DB_PASSWORD por separado.'
    }
    if ($env:DB_URL -cne 'jdbc:postgresql://localhost:5432/reminishub_test') {
        throw 'Solo se permite la base de pruebas autorizada reminishub_test.'
    }
    foreach ($name in @('SPRING_DATASOURCE_URL', 'SPRING_DATASOURCE_USERNAME',
            'SPRING_DATASOURCE_PASSWORD', 'SPRING_FLYWAY_URL', 'SPRING_FLYWAY_USER',
            'SPRING_FLYWAY_PASSWORD', 'SPRING_APPLICATION_JSON', 'JAVA_TOOL_OPTIONS',
            'JDK_JAVA_OPTIONS', 'MAVEN_OPTS')) {
        if (![string]::IsNullOrEmpty([Environment]::GetEnvironmentVariable($name, 'Process'))) {
            throw "Retira la sustitucion $name antes de verificar la base de pruebas."
        }
    }
    Write-Host 'Base: jdbc:postgresql://localhost:5432/reminishub_test; contrasena: [OCULTA]'
    if (!$MavenCommand) { $MavenCommand = Join-Path $modulePath 'mvnw.cmd' }
    $maven = (Get-Command $MavenCommand -ErrorAction Stop).Source
    $runId = [Guid]::NewGuid().ToString('N')
    $outputPath = Join-Path $modulePath "target/verificacion/$runId"
    New-Item -ItemType Directory -Path $outputPath -Force | Out-Null
    $logPath = Join-Path $outputPath 'maven.log'
    $env:CATALOGO_DB_TESTS = 'true'
    $env:E1A_DB_CHECKS = 'true'
    $env:SPRING_JPA_SHOW_SQL = 'false'
    Push-Location $modulePath
    $locationChanged = $true
    $mavenArgs = @('-ntp', '-B', '-Dstyle.color=never', '-DskipTests=false',
        '-Dmaven.test.skip=false', "-Dsurefire.reportNameSuffix=$runId", 'test')
    if ($Offline) { $mavenArgs = @('-o') + $mavenArgs }
    Write-Host 'Ejecutando pruebas con PostgreSQL. El detalle queda en target/verificacion.'
    # Ocultar valores privados antes de escribir el log, incluso si Maven falla.
    & $maven @mavenArgs 2>&1 | ForEach-Object { Protect-Text ([string]$_) } |
        Out-File -LiteralPath $logPath -Encoding utf8
    $mavenExitCode = $LASTEXITCODE

    $reportsPath = Join-Path $modulePath 'target/surefire-reports'
    $reports = @()
    if (Test-Path -LiteralPath $reportsPath) {
        $reports = @(Get-ChildItem -LiteralPath $reportsPath -Filter "TEST-*-$runId.xml")
    }
    if ($reports.Count -eq 0) { throw "Maven termino con codigo $mavenExitCode sin informes de esta ejecucion. Pruebas: NOT RUN; revisa el log local." }
    $expected = @{
        'dev.reminishub.CatalogoIntegrationTests' = 11
        'dev.reminishub.EsquemaExistenteTests' = 2
        'dev.reminishub.ReminisHubApplicationTests' = 1
        'dev.reminishub.RegistroServiceTests' = 5
        'dev.reminishub.RegistroWebTests' = 12
        'dev.reminishub.RegistroIntegrationTests' = 4
        'dev.reminishub.TokenServiceTests' = 7
        'dev.reminishub.AutenticacionWebTests' = 17
        'dev.reminishub.service.IntentosLoginTests' = 1
        'dev.reminishub.SesionE2ETests' = 3
        'dev.reminishub.RegistroE2ETests' = 1
    }
    $seen = @{}
    $total = 0
    $failures = 0
    $errors = 0
    $skipped = 0
    $invalidSuite = $false
    $urlError = $false
    $summary = [Collections.Generic.List[string]]::new()
    foreach ($report in $reports) {
        [xml]$xml = Get-Content -LiteralPath $report.FullName -Raw
        $suite = $xml.testsuite
        # El XML usa parentesis en el nombre; el archivo conserva TEST-clase-sufijo.xml.
        $name = $report.BaseName.Substring(5) -replace ('-' + $runId + '$'), ''
        $tests = [int]$suite.tests
        $failed = [int]$suite.failures -gt 0 -or [int]$suite.errors -gt 0 -or [int]$suite.skipped -gt 0 -or $tests -lt 1
        $invalidSuite = $invalidSuite -or $failed
        $seen[$name] = $tests
        $total += $tests
        $failures += [int]$suite.failures
        $errors += [int]$suite.errors
        $skipped += [int]$suite.skipped
        $state = if ($failed) { 'FAIL' } else { 'PASS' }
        $summary.Add("${state}: $name; pruebas=$tests, fallos=$($suite.failures), errores=$($suite.errors), omitidas=$($suite.skipped)")
        foreach ($problem in $xml.SelectNodes('//testcase/error | //testcase/failure')) {
            # Solo clasificar causas conocidas; nunca imprimir trazas o valores de conexion.
            if ($problem.InnerText -match "'url' must start with|No suitable driver") { $urlError = $true }
        }
    }
    foreach ($name in $expected.Keys) {
        if (!$seen.ContainsKey($name) -or $seen[$name] -lt $expected[$name]) {
            $invalidSuite = $true
            $summary.Add("FAIL: suite ausente o incompleta: $name")
        }
    }
    $summary.Add("RESUMEN: pruebas=$total, fallos=$failures, errores=$errors, omitidas=$skipped; codigo Maven=$mavenExitCode")
    if ($urlError) { $summary.Add('DIAGNOSTICO: URL JDBC o controlador no valido. Revisa DB_URL y posibles propiedades Spring que sustituyan la conexion.') }
    $summary | ForEach-Object { Write-Host (Protect-Text $_) }
    $summary | ForEach-Object { Protect-Text $_ } | Set-Content -LiteralPath (Join-Path $outputPath 'resumen.txt') -Encoding utf8
    if ($mavenExitCode -ne 0 -or $invalidSuite) { throw 'Verificacion fallida; consulta el resumen anterior y el log local protegido.' }
    Write-Host "PASS: $total pruebas, sin fallos, errores ni omisiones."
    Write-Host "Informes: $reportsPath (sufijo $runId)"
    $exitCode = 0
} catch {
    # No propagar excepciones externas que puedan incluir valores privados.
    if ($_.Exception -is [System.Management.Automation.RuntimeException]) {
        Write-Host (Protect-Text "FAIL: $($_.Exception.Message)") -ForegroundColor Red
    } else {
        Write-Host 'FAIL: no se pudo completar la verificacion; revisa configuracion y log local.' -ForegroundColor Red
    }
    if ($logPath) { Write-Host "Log local: $logPath" }
} finally {
    if ($locationChanged) { Pop-Location }
    foreach ($name in $changedVariables) {
        [Environment]::SetEnvironmentVariable($name, $previousValues[$name], 'Process')
    }
}
exit $exitCode
