[CmdletBinding()]
param(
    [Parameter(Mandatory)][string]$JavaPath,
    [Parameter(Mandatory)][string]$NodePath
)
$ErrorActionPreference = 'Stop'
$scriptsRoot = Split-Path -Parent $PSScriptRoot
$launcher = Join-Path $scriptsRoot 'start-demo.ps1'
$powershell = (Get-Command powershell.exe -ErrorAction Stop).Source

function Quote-Single([string]$Value) {
    return "'" + $Value.Replace("'", "''") + "'"
}

$occupied = [System.Net.Sockets.TcpListener]::new([System.Net.IPAddress]::Loopback,0)
try {
    $occupied.Start()
    $backendPort = ([System.Net.IPEndPoint]$occupied.LocalEndpoint).Port
    $frontendProbe = [System.Net.Sockets.TcpListener]::new([System.Net.IPAddress]::Loopback,0)
    try {
        $frontendProbe.Start()
        $frontendPort = ([System.Net.IPEndPoint]$frontendProbe.LocalEndpoint).Port
    } finally {
        $frontendProbe.Stop()
    }
    $command = '& ' + (Quote-Single $launcher) +
        ' -Demo -JavaPath ' + (Quote-Single $JavaPath) +
        ' -NodePath ' + (Quote-Single $NodePath) +
        " -MavenPath 'mvn' -BackendPort $backendPort -FrontendPort $frontendPort -UseBuiltArtifacts"
    $encoded = [Convert]::ToBase64String([Text.Encoding]::Unicode.GetBytes($command))
    $savedPreference = $ErrorActionPreference
    try {
        $ErrorActionPreference = 'Continue'
        $output = (& $powershell -NoProfile -NonInteractive -ExecutionPolicy Bypass -EncodedCommand $encoded 2>&1 | Out-String)
        $exitCode = $LASTEXITCODE
    } finally {
        $ErrorActionPreference = $savedPreference
    }
} finally {
    $occupied.Stop()
}

if ($exitCode -eq 0) { throw 'Probe unexpectedly succeeded with an occupied backend port.' }
if ($output -match 'NativeCommandError') {
    throw "Windows PowerShell treated java -version stderr as a terminating native-command error.`n$output"
}
if ($output -notmatch [regex]::Escape("Port $backendPort is already in use")) {
    throw "Launcher did not continue past runtime version probes to the occupied-port check.`n$output"
}
Write-Output 'PASS: Windows PowerShell accepted Java 21 version output and continued to port validation.'
