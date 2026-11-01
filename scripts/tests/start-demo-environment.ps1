[CmdletBinding()]
param(
    [Parameter(Mandatory)][string]$JavaPath,
    [Parameter(Mandatory)][string]$NodePath,
    [switch]$SupplyTotpKey
)
$ErrorActionPreference = 'Stop'
$scriptsRoot = Split-Path -Parent $PSScriptRoot
$repo = Split-Path -Parent $scriptsRoot
$statePath = $null
$saved = @{}
$inherited = @{
    SPRING_CONFIG_LOCATION = 'file:./this-config-must-not-be-loaded.yml'
    SPRING_CONFIG_ADDITIONAL_LOCATION = 'file:./this-additional-config-must-not-be-loaded.yml'
    SPRING_APPLICATION_JSON = '{"mybatis":{"configuration":{"map-underscore-to-camel-case":false}}}'
    TASK_BOOTSTRAP_PASSWORD = 'Synthetic-EnvironmentProbe12345'
    TASK_TOTP_ENCRYPTION_KEY = $null
}
if ($SupplyTotpKey) {
    $keyBytes = [byte[]]::new(32)
    $generator = [Security.Cryptography.RandomNumberGenerator]::Create()
    try { $generator.GetBytes($keyBytes) } finally { $generator.Dispose() }
    $inherited.TASK_TOTP_ENCRYPTION_KEY = [Convert]::ToBase64String($keyBytes)
    [Array]::Clear($keyBytes,0,$keyBytes.Length)
}
function Get-FreeLoopbackPort {
    $listener = [Net.Sockets.TcpListener]::new([Net.IPAddress]::Loopback,0)
    try { $listener.Start(); return ([Net.IPEndPoint]$listener.LocalEndpoint).Port }
    finally { $listener.Stop() }
}
foreach ($name in $inherited.Keys) { $saved[$name] = [Environment]::GetEnvironmentVariable($name,'Process') }
$originalDbUrl = [Environment]::GetEnvironmentVariable('TASK_DB_URL','Process')
try {
    foreach ($name in $inherited.Keys) {
        $value = if ($null -eq $inherited[$name]) { [System.Management.Automation.Language.NullString]::Value } else { $inherited[$name] }
        [Environment]::SetEnvironmentVariable($name,$value,'Process')
    }
    $backendPort = Get-FreeLoopbackPort
    do { $frontendPort = Get-FreeLoopbackPort } while ($frontendPort -eq $backendPort)
    $statePath = Join-Path $repo ".cache/demo-$backendPort-$frontendPort/processes.json"
    & (Join-Path $scriptsRoot 'start-demo.ps1') -Demo -JavaPath $JavaPath -NodePath $NodePath -BackendPort $backendPort -FrontendPort $frontendPort -UseBuiltArtifacts
    $csrf = Invoke-RestMethod -Uri "http://127.0.0.1:$frontendPort/api/v1/auth/csrf" -SessionVariable probeSession -TimeoutSec 10
    $headers = @{}
    $headers[$csrf.data.headerName] = $csrf.data.token
    $body = @{ account='demo.employee'; password=$inherited.TASK_BOOTSTRAP_PASSWORD } | ConvertTo-Json -Compress
    try {
        $login = Invoke-RestMethod -Method Post -Uri "http://127.0.0.1:$frontendPort/api/v1/auth/login" -WebSession $probeSession -Headers $headers -ContentType 'application/json' -Body $body -TimeoutSec 15
    } catch {
        $statusCode = [int]$_.Exception.Response.StatusCode
        $errorCode = try { ($_.ErrorDetails.Message | ConvertFrom-Json).code } catch { 'UNKNOWN' }
        throw "Demo login failed (HTTP $statusCode, $errorCode): the isolated child must load the bundled base configuration after inherited Spring configuration overrides are removed."
    }
    if ($login.data.role -ne 'EMPLOYEE' -or -not $login.data.mustChangePassword) { throw 'Demo login did not return the expected initial employee session.' }
    foreach ($name in $inherited.Keys) {
        $restored = [Environment]::GetEnvironmentVariable($name,'Process')
        if (($null -eq $restored) -ne ($null -eq $inherited[$name]) -or $restored -cne $inherited[$name]) { throw "Launcher did not restore caller setting $name." }
    }
    $restoredDbUrl = [Environment]::GetEnvironmentVariable('TASK_DB_URL','Process')
    if (($null -eq $originalDbUrl) -ne ($null -eq $restoredDbUrl) -or $originalDbUrl -cne $restoredDbUrl) {
        throw 'Launcher did not preserve the caller database setting, including whether it was absent.'
    }
    Write-Output 'PASS: real demo login uses bundled configuration and caller environment is restored exactly.'
} finally {
    foreach ($name in $saved.Keys) {
        $value = if ($null -eq $saved[$name]) { [System.Management.Automation.Language.NullString]::Value } else { $saved[$name] }
        [Environment]::SetEnvironmentVariable($name,$value,'Process')
    }
    if ($statePath -and (Test-Path -LiteralPath $statePath)) {
        & (Join-Path $scriptsRoot 'stop-demo.ps1') -StateFile $statePath
    }
}
