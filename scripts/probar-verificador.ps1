# Regresiones del verificador: Maven simulado, sin conectar a PostgreSQL.
Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
$PSNativeCommandUseErrorActionPreference = $false
$root = Split-Path $PSScriptRoot -Parent
$verifier = Join-Path $PSScriptRoot 'verificar.ps1'
$fixturePath = Join-Path $root ('reminis-hub/target/prueba-verificador/' + [Guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $fixturePath -Force | Out-Null
$fakeMaven = Join-Path $fixturePath 'maven-simulado.ps1'
@'
Write-Output $env:DB_PASSWORD
if ($env:RH_TEST_CASE -eq 'maven-error') { exit 7 }
if ($env:RH_TEST_CASE -eq 'sin-informes') { exit 0 }
$suffix = ($args | Where-Object { $_ -like '-Dsurefire.reportNameSuffix=*' }) -replace '^-Dsurefire.reportNameSuffix=', ''
$directory = Join-Path (Get-Location) 'target/surefire-reports'
New-Item -ItemType Directory -Path $directory -Force | Out-Null
$suites = @{'CatalogoIntegrationTests'=7; 'EsquemaExistenteTests'=2; 'ReminisHubApplicationTests'=1}
foreach ($name in $suites.Keys) {
    if ($env:RH_TEST_CASE -eq 'ausente' -and $name -eq 'CatalogoIntegrationTests') { continue }
    $skipped = 0; $errors = 0; $detail = ''
    if ($env:RH_TEST_CASE -eq 'omitida') { $skipped = $suites[$name] }
    if ($env:RH_TEST_CASE -eq 'errores') {
        $errors = $suites[$name]
        $detail = '<testcase><error>java.lang.IllegalArgumentException: &apos;url&apos; must start with jdbc</error></testcase>'
    }
    $xml = '<testsuite name="dev.reminishub.' + $name + '" tests="' + $suites[$name] + '" failures="0" errors="' + $errors + '" skipped="' + $skipped + '">' + $detail + '</testsuite>'
    Set-Content -LiteralPath (Join-Path $directory ("TEST-dev.reminishub.$name-$suffix.xml")) -Value $xml
}
if ($env:RH_TEST_CASE -eq 'errores') { exit 1 }
exit 0
'@ | Set-Content -LiteralPath $fakeMaven -Encoding utf8
$names = @('DB_URL','DB_USER','DB_PASSWORD','DEV_USER_PASSWORD','RH_TEST_CASE')
$oldValues = @{}
foreach ($name in $names) { $oldValues[$name] = [Environment]::GetEnvironmentVariable($name, 'Process') }
try {
    $env:DB_USER = 'usuario_simulado'
    $env:DB_PASSWORD = 'secreto_simulado_no_real'
    $env:DEV_USER_PASSWORD = 'dev_simulado_no_real'
    foreach ($case in @('url-invalida','maven-error','sin-informes','ausente','omitida','errores','correcto')) {
        $env:RH_TEST_CASE = $case
        $env:DB_URL = if ($case -eq 'url-invalida') { 'DB_URL=jdbc:postgresql://localhost/base' } else { 'jdbc:postgresql://localhost/base' }
        $result = & (Get-Process -Id $PID).Path -NoProfile -File $verifier -MavenCommand $fakeMaven 2>&1
        $code = $LASTEXITCODE
        $text = $result -join "`n"
        $expectedCode = if ($case -eq 'correcto') { 0 } else { 1 }
        if ($code -ne $expectedCode) { throw "Codigo incorrecto en $case" }
        if ($text.Contains($env:DB_PASSWORD)) { throw "Secreto impreso en $case" }
        if ($case -eq 'url-invalida' -and !$text.Contains('DB_URL invalida')) { throw 'Falta diagnostico previo de URL' }
        if ($case -eq 'errores' -and (!$text.Contains('errores=10') -or !$text.Contains('DIAGNOSTICO:'))) { throw 'Falta resumen de los diez errores' }
        if ($case -eq 'correcto' -and !$text.Contains('PASS: 10 pruebas')) { throw 'Falta resumen exitoso' }
        $logMatch = [regex]::Match($text, 'Log local: (.+)')
        if ($logMatch.Success) {
            $log = Get-Content -LiteralPath $logMatch.Groups[1].Value.Trim() -Raw
            if ($log.Contains($env:DB_PASSWORD) -or !$log.Contains('[OCULTO]')) { throw "Log sin proteccion en $case" }
        }
        Write-Host "PASS: $case"
    }
} finally {
    foreach ($name in $names) { [Environment]::SetEnvironmentVariable($name, $oldValues[$name], 'Process') }
}
