[CmdletBinding()]
param([Parameter(Mandatory)][string]$StateFile)
$ErrorActionPreference = 'Stop'
$state = Get-Content -LiteralPath $StateFile -Raw | ConvertFrom-Json
foreach ($entry in @($state.frontend,$state.backend)) {
    $process = Get-Process -Id ([int]$entry.pid) -ErrorAction SilentlyContinue
    if ($null -eq $process) { continue }
    $expectedStart = [DateTime]::Parse($entry.startedUtc).ToUniversalTime()
    if ([Math]::Abs(($process.StartTime.ToUniversalTime() - $expectedStart).TotalSeconds) -gt 1 -or $process.Path -ne $entry.executable) {
        throw 'Process identity changed. Refusing to stop a reused PID.'
    }
    # The verified main process and its Vite worker children belong to this demo launch.
    & taskkill.exe /PID ([string]$process.Id) /T /F | Out-Null
    if ($LASTEXITCODE -ne 0) { throw "Could not stop demo PID $($process.Id)." }
    Write-Output "Stopped demo PID $($process.Id)"
}
Remove-Item -LiteralPath $StateFile
