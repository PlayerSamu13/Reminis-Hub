<# Inicia el perfil HTTPS sobre la base de pruebas autorizada. No lee archivos privados. #>
[CmdletBinding()]
param()
Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
$required = @('DB_URL', 'DB_USER', 'DB_PASSWORD', 'DEV_USER_PASSWORD',
    'RH_TLS_KEYSTORE', 'RH_TLS_PASSWORD')
foreach ($name in $required) {
    if ([string]::IsNullOrWhiteSpace([Environment]::GetEnvironmentVariable($name, 'Process'))) {
        throw "Falta configurar $name en esta terminal."
    }
}
if ($env:DB_URL -ne 'jdbc:postgresql://localhost:5432/reminishub_test') {
    throw 'Este comando solo inicia la base de pruebas autorizada reminishub_test.'
}
# Las variables canonicas pueden sustituir las propiedades basadas en DB_*.
foreach ($name in @('SPRING_DATASOURCE_URL', 'SPRING_DATASOURCE_USERNAME',
        'SPRING_DATASOURCE_PASSWORD', 'SPRING_FLYWAY_URL', 'SPRING_FLYWAY_USER',
        'SPRING_FLYWAY_PASSWORD')) {
    if (![string]::IsNullOrEmpty([Environment]::GetEnvironmentVariable($name, 'Process'))) {
        throw "Retira la sustitucion $name de esta terminal antes de usar este comando."
    }
}
Write-Host 'Base solicitada: jdbc:postgresql://localhost:5432/reminishub_test; contraseña: [OCULTA]'
Write-Host 'Perfil HTTPS local: https://localhost:8443/proyectos'
$module = Join-Path (Split-Path $PSScriptRoot -Parent) 'reminis-hub'
$previousProfile = $env:SPRING_PROFILES_ACTIVE
try {
    $env:SPRING_PROFILES_ACTIVE = 'https'
    Push-Location $module
    try {
        & .\mvnw.cmd spring-boot:run
        $result = $LASTEXITCODE
    } finally { Pop-Location }
} finally {
    $env:SPRING_PROFILES_ACTIVE = $previousProfile
}
exit $result
