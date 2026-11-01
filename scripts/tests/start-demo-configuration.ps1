[CmdletBinding()]
param(
    [Parameter(Mandatory)][string]$JavaPath,
    [Parameter(Mandatory)][string]$NodePath,
    [Parameter(Mandatory)][string]$MavenPath
)
$ErrorActionPreference = 'Stop'
$scriptsRoot = Split-Path -Parent $PSScriptRoot
$repo = Split-Path -Parent $scriptsRoot
$sourcePath = Join-Path $scriptsRoot 'start-demo.ps1'
$probePath = Join-Path $scriptsRoot ('.start-demo-probe-{0}.ps1' -f $PID)
$statePath = $null
$savedPassword = $env:TASK_BOOTSTRAP_PASSWORD

function Get-FreeLoopbackPort {
    $listener = [System.Net.Sockets.TcpListener]::new([System.Net.IPAddress]::Loopback,0)
    try {
        $listener.Start()
        return ([System.Net.IPEndPoint]$listener.LocalEndpoint).Port
    } finally {
        $listener.Stop()
    }
}

try {
    $source = Get-Content -LiteralPath $sourcePath -Raw
    if ($source -match 'ConvertTo-SecureString|ConvertFrom-SecureString') {
        throw 'Demo launcher must not depend on the PowerShell Security module for DPAPI key storage.'
    }
    if ($source -notmatch 'Security\.Cryptography\.ProtectedData') {
        throw 'Demo launcher must use Windows DPAPI ProtectedData for its local TOTP key.'
    }
    $pattern = '(?s)function Wait-DemoEndpoint\(\[string\]\$Url,\[System\.Diagnostics\.Process\]\$Process,\[int\]\$Seconds\) \{.*?\r?\n\}\r?\n\$backend = \$null'
    $replacement = "function Wait-DemoEndpoint([string]`$Url,[System.Diagnostics.Process]`$Process,[int]`$Seconds) { return }`r`n`$backend = `$null"
    $probe = [regex]::Replace($source,$pattern,$replacement,1)
    if ($probe -eq $source) { throw 'Could not instrument the launcher readiness boundary.' }
    Set-Content -LiteralPath $probePath -Value $probe -Encoding UTF8

    $backendPort = Get-FreeLoopbackPort
    do { $frontendPort = Get-FreeLoopbackPort } while ($frontendPort -eq $backendPort)
    $passwordBytes = [byte[]]::new(8)
    [System.Security.Cryptography.RandomNumberGenerator]::Fill($passwordBytes)
    $env:TASK_BOOTSTRAP_PASSWORD = 'ProbeOnly2026' + [Convert]::ToHexString($passwordBytes)
    & $probePath -Demo -JavaPath $JavaPath -NodePath $NodePath -MavenPath $MavenPath -BackendPort $backendPort -FrontendPort $frontendPort -UseBuiltArtifacts
    $statePath = Join-Path $repo ".cache/demo-$backendPort-$frontendPort/processes.json"
    Start-Sleep -Seconds 15
    $state = Get-Content -LiteralPath $statePath -Raw | ConvertFrom-Json
    $backend = Get-Process -Id ([int]$state.backend.pid) -ErrorAction SilentlyContinue
    if ($null -eq $backend) {
        $logPath = Join-Path $repo ".cache/demo-$backendPort-$frontendPort/backend.stdout.log"
        $log = if (Test-Path -LiteralPath $logPath) { Get-Content -LiteralPath $logPath -Raw } else { 'backend log missing' }
        $tail = (($log -split '\r?\n') | Select-Object -Last 40) -join [Environment]::NewLine
        if ($log -match 'Management-specific server address cannot be configured') {
            throw "Demo launcher configured an invalid same-port management address.`n$tail"
        }
        if ($log -match 'Unable to establish loopback connection' -and $log -match 'Permission denied: connect') {
            Write-Warning 'Runtime liveness skipped because the execution sandbox denied Java NIO internal loopback access.'
            Write-Output 'PASS: launcher did not trigger the same-port management-address regression.'
            return
        }
        throw "Demo backend exited after launcher return.`n$tail"
    }
    Write-Output "PASS: demo backend remained alive on port $backendPort after launcher return."
} finally {
    $env:TASK_BOOTSTRAP_PASSWORD = $savedPassword
    try {
        if ($statePath -and (Test-Path -LiteralPath $statePath)) {
            & (Join-Path $scriptsRoot 'stop-demo.ps1') -StateFile $statePath
        }
    } finally {
        if (Test-Path -LiteralPath $probePath) { Remove-Item -LiteralPath $probePath -Force }
    }
}
