<#
.SYNOPSIS
Ejecuta Maven y comprueba que las suites obligatorias realmente se ejecutaron.
.DESCRIPTION
Requiere Java 25, PostgreSQL y DB_URL, DB_USER, DB_PASSWORD, DEV_USER_PASSWORD
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
$modulePath = Join-Path (Split-Path $PSScriptRoot -Parent) 'reminis-hub'
$changedVariables = @('CATALOGO_DB_TESTS', 'E1A_DB_CHECKS', 'SPRING_JPA_SHOW_SQL')
$previousValues = @{}
foreach ($name in $changedVariables) {
    $previousValues[$name] = [Environment]::GetEnvironmentVariable($name, 'Process')
}
$exitCode = 1
$locationChanged = $false
$logPath = $null
$privateValues = @('DB_URL', 'DB_USER', 'DB_PASSWORD', 'DEV_USER_PASSWORD') | ForEach-Object {
    [Environment]::GetEnvironmentVariable($_, 'Process')
} | Where-Object { ![string]::IsNullOrEmpty($_) } | Sort-Object Length -Descending
function Protect-Text([string]$text) {
    foreach ($value in $privateValues) { $text = $text.Replace($value, '[OCULTO]') }
    return $text
}
try {
    $missing = @('DB_URL', 'DB_USER', 'DB_PASSWORD', 'DEV_USER_PASSWORD') | Where-Object {
        [string]::IsNullOrWhiteSpace([Environment]::GetEnvironmentVariable($_, 'Process'))
    }
    if ($missing) { throw ('Faltan variables de entorno: ' + ($missing -join ', ')) }
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
    if ($LASTEXITCODE -ne 0) { throw 'Maven termino con errores. Revisa el log local sin compartir secretos.' }

    $reportsPath = Join-Path $modulePath 'target/surefire-reports'
    $reports = @(Get-ChildItem -LiteralPath $reportsPath -Filter "TEST-*-$runId.xml")
    if ($reports.Count -eq 0) { throw 'Maven no genero informes de esta ejecucion.' }
    $expected = @{
        'dev.reminishub.CatalogoIntegrationTests' = 7
        'dev.reminishub.EsquemaExistenteTests' = 2
        'dev.reminishub.ReminisHubApplicationTests' = 1
    }
    $seen = @{}
    $total = 0
    foreach ($report in $reports) {
        [xml]$xml = Get-Content -LiteralPath $report.FullName -Raw
        $suite = $xml.testsuite
        # El XML usa parentesis en el nombre; el archivo conserva TEST-clase-sufijo.xml.
        $name = $report.BaseName.Substring(5) -replace ('-' + $runId + '$'), ''
        $tests = [int]$suite.tests
        if ([int]$suite.failures -gt 0 -or [int]$suite.errors -gt 0 -or [int]$suite.skipped -gt 0) {
            throw "Suite con fallos, errores u omisiones: $name"
        }
        if ($tests -lt 1) { throw "Suite sin pruebas ejecutadas: $name" }
        $seen[$name] = $tests
        $total += $tests
        Write-Host "PASS: $name ($tests pruebas)"
    }
    foreach ($name in $expected.Keys) {
        if (!$seen.ContainsKey($name) -or $seen[$name] -lt $expected[$name]) {
            throw "Suite ausente o incompleta: $name"
        }
    }
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
