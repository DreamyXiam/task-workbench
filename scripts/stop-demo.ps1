[CmdletBinding()]
param([Parameter(Mandatory)][string]$StateFile)
$ErrorActionPreference = 'Stop'
$state = Get-Content -LiteralPath $StateFile -Raw | ConvertFrom-Json
foreach ($entry in @($state.frontend,$state.backend)) {
    $process = Get-Process -Id ([int]$entry.pid) -ErrorAction SilentlyContinue
    if ($null -eq $process) { continue }
    # PowerShell 7 deserializes ISO timestamps as DateTime; reparsing that value
    # as text drops its UTC kind and applies the local offset a second time.
    $expectedStart = if ($entry.startedUtc -is [DateTime]) {
        $entry.startedUtc.ToUniversalTime()
    } else {
        [DateTime]::Parse([string]$entry.startedUtc,[Globalization.CultureInfo]::InvariantCulture,[Globalization.DateTimeStyles]::RoundtripKind).ToUniversalTime()
    }
    if ($process.StartTime.ToUniversalTime().Ticks -ne $expectedStart.Ticks -or $process.Path -ne $entry.executable) {
        throw 'Process identity changed. Refusing to stop a reused PID.'
    }
    # The verified main process and its Vite worker children belong to this demo launch.
    & taskkill.exe /PID ([string]$process.Id) /T /F | Out-Null
    if ($LASTEXITCODE -ne 0) { throw "Could not stop demo PID $($process.Id)." }
    Write-Output "Stopped demo PID $($process.Id)"
}
Remove-Item -LiteralPath $StateFile
