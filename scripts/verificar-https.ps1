<# Verifica HTTPS local usando exclusivamente el certificado publico indicado. #>
[CmdletBinding()]
param(
    [string]$Certificate = (Join-Path $env:LOCALAPPDATA 'ReminisHub\tls\localhost.crt')
)
Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
$PSNativeCommandUseErrorActionPreference = $false
if (!(Test-Path -LiteralPath $Certificate -PathType Leaf)) {
    throw 'BLOCKED: falta el certificado publico localhost.crt. Sigue docs/development/https-local.md.'
}
$certificatePath = (Resolve-Path -LiteralPath $Certificate).Path
$curl = (Get-Command curl.exe -ErrorAction Stop).Source
$outputDirectory = Join-Path (Split-Path $PSScriptRoot -Parent) ('reminis-hub/target/https/' + [Guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $outputDirectory -Force | Out-Null
$checks = @(
    @{ Name = 'catalogo-es'; Route = '/proyectos?lang=es'; Text = 'Lo que otros crearon.' },
    @{ Name = 'catalogo-en'; Route = '/proyectos?lang=en'; Text = 'Your next great idea.' },
    @{ Name = 'swagger'; Route = '/swagger-ui/index.html'; Text = 'swagger-ui' },
    @{ Name = 'openapi'; Route = '/v3/api-docs'; Text = '"openapi"' }
)
$failed = $false
foreach ($check in $checks) {
    $bodyPath = Join-Path $outputDirectory ($check.Name + '.txt')
    $errorPath = Join-Path $outputDirectory ($check.Name + '.stderr')
    # curl escribe stderr directamente al archivo: PowerShell 5.1 no debe
    # convertir un error nativo esperado en excepcion antes de leer LASTEXITCODE.
    # Fuerza loopback IPv4 para coincidir con server.address; valida TLS sin -k.
    $status = & $curl --silent --show-error --noproxy '*' --ipv4 --connect-timeout 5 --max-time 20 `
        --cacert $certificatePath --output $bodyPath --write-out '%{http_code}' `
        --stderr $errorPath ('https://localhost:8443' + $check.Route)
    $curlCode = $LASTEXITCODE
    if ($curlCode -ne 0 -or $status -ne '200') {
        Write-Host "FAIL: $($check.Name), curl=$curlCode, HTTP=$status."
        $failed = $true
        continue
    }
    $body = Get-Content -LiteralPath $bodyPath -Raw -Encoding utf8
    if (!$body.Contains($check.Text)) {
        Write-Host "FAIL: $($check.Name), contenido esperado ausente."
        $failed = $true
        continue
    }
    if ($check.Name -eq 'openapi') {
        $document = $body | ConvertFrom-Json
        if (!$document.paths.PSObject.Properties['/api/v1/proyectos']) {
            Write-Host 'FAIL: el contrato OpenAPI no contiene el catalogo.'
            $failed = $true
            continue
        }
    }
    Write-Host "PASS: $($check.Name), TLS validado y contenido esperado con HTTP 200."
}
if (!$failed) {
    # Comprueba que confiar en el certificado no permite usar otro nombre de servidor.
    & $curl --silent --show-error --noproxy '*' --connect-timeout 5 --max-time 10 `
        --cacert $certificatePath --resolve 'nombre-incorrecto.invalid:8443:127.0.0.1' `
        --output (Join-Path $outputDirectory 'nombre-incorrecto.txt') `
        --stderr (Join-Path $outputDirectory 'nombre-incorrecto.stderr') `
        'https://nombre-incorrecto.invalid:8443/proyectos'
    if ($LASTEXITCODE -eq 60) {
        Write-Host 'PASS: nombre de servidor incorrecto rechazado por validacion del certificado.'
    } else {
        Write-Host "FAIL: prueba de nombre incorrecto, curl=$LASTEXITCODE; se esperaba 60."
        $failed = $true
    }
}
Write-Host "Evidencias locales: $outputDirectory"
if ($failed) { exit 1 }
Write-Host 'PASS: verificacion HTTPS automatica completada. Falta comprobar confianza y recorrido en navegador.'
exit 0
