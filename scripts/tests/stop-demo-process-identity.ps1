[CmdletBinding()]
param()
$ErrorActionPreference = 'Stop'
$stopPath = Join-Path (Split-Path -Parent $PSScriptRoot) 'stop-demo.ps1'
$shellPath = (Get-Process -Id $PID).Path
$testRoot = Join-Path ([System.IO.Path]::GetTempPath()) ('stop-demo-identity-' + [guid]::NewGuid().ToString('N'))
$null = New-Item -ItemType Directory -Path $testRoot
$failures = @()

# These cases catch timestamp reparsing, skipped creation-time checks (a reused
# PID), and skipped executable checks by observing real child-process lifetime.
$cases = @(
    @{ Name = 'matching identity'; StartOffsetMinutes = 0; WrongExecutable = $false; ShouldStop = $true },
    @{ Name = 'reused PID with older creation time'; StartOffsetMinutes = -5; WrongExecutable = $false; ShouldStop = $false },
    @{ Name = 'incorrect future creation time'; StartOffsetMinutes = 5; WrongExecutable = $false; ShouldStop = $false },
    @{ Name = 'subsecond creation-time mismatch'; StartOffsetMinutes = 0; StartOffsetSeconds = 0.5; WrongExecutable = $false; ShouldStop = $false },
    @{ Name = 'different executable'; StartOffsetMinutes = 0; WrongExecutable = $true; ShouldStop = $false },
    @{ Name = 'configuration probe cleanup refuses a reused PID'; StartOffsetMinutes = -5; WrongExecutable = $false; ShouldStop = $false; UseProbeCleanup = $true }
)

try {
    foreach ($case in $cases) {
        $ownedChildren = @()
        $statePath = Join-Path $testRoot 'processes.json'
        try {
            $payload = [Convert]::ToBase64String([Text.Encoding]::Unicode.GetBytes('Start-Sleep -Seconds 120'))
            foreach ($role in @('frontend','backend')) {
                $child = Start-Process -FilePath $shellPath -ArgumentList @('-NoProfile','-NonInteractive','-EncodedCommand',$payload) -WindowStyle Hidden -PassThru
                $owned = @{ Process = $child; Start = $child.StartTime.ToUniversalTime(); Executable = $shellPath }
                $ownedChildren += $owned
                # Windows can briefly report ntdll.dll as a new process's path.
                $deadline = [DateTime]::UtcNow.AddSeconds(5)
                do {
                    $liveChild = Get-Process -Id $child.Id -ErrorAction Stop
                    if ($liveChild.StartTime.ToUniversalTime().Ticks -ne $owned.Start.Ticks) { throw 'Test child identity changed during startup.' }
                    if ($liveChild.Path -ieq $shellPath) { break }
                    Start-Sleep -Milliseconds 20
                } while ([DateTime]::UtcNow -lt $deadline)
                if ($liveChild.Path -ine $shellPath) { throw 'Test child executable did not finish initializing.' }
            }
            $frontend = $ownedChildren[0]
            $backend = $ownedChildren[1]
            $entry = @{
                pid = $frontend.Process.Id
                startedUtc = $frontend.Start.AddMinutes($case.StartOffsetMinutes).AddSeconds([double]$case.StartOffsetSeconds).ToString('o')
                executable = if ($case.WrongExecutable) { Join-Path $testRoot 'different.exe' } else { $frontend.Executable }
            }
            $backendEntry = @{ pid = $backend.Process.Id; startedUtc = $backend.Start.ToString('o'); executable = $backend.Executable }
            @{ frontend = $entry; backend = $backendEntry } | ConvertTo-Json -Depth 3 | Set-Content -LiteralPath $statePath -Encoding UTF8

            $stopError = $null
            try {
                if ($case.UseProbeCleanup) {
                    # Exercise the probe's real finalizer without launching its
                    # Java/Node services or touching ports and bootstrap keys.
                    $scriptsRoot = Split-Path -Parent $PSScriptRoot
                    $probePath = Join-Path $testRoot 'unused-probe.ps1'
                    $savedPassword = $env:TASK_BOOTSTRAP_PASSWORD
                    $tokens = $null
                    $parseErrors = $null
                    $probeAst = [Management.Automation.Language.Parser]::ParseFile((Join-Path $PSScriptRoot 'start-demo-configuration.ps1'),[ref]$tokens,[ref]$parseErrors)
                    if ($parseErrors.Count -gt 0) { throw 'Configuration probe could not be parsed.' }
                    $probeTry = @($probeAst.EndBlock.Statements | Where-Object { $_ -is [Management.Automation.Language.TryStatementAst] })[-1]
                    $finalizer = $probeTry.Finally.Extent.Text
                    & ([ScriptBlock]::Create($finalizer.Substring(1,$finalizer.Length - 2))) | Out-Null
                } else {
                    & $stopPath -StateFile $statePath | Out-Null
                }
            } catch { $stopError = $_ }
            if ($case.ShouldStop) {
                if ($stopError) { throw "Matching identity was rejected: $stopError" }
                foreach ($owned in $ownedChildren) {
                    if (-not $owned.Process.WaitForExit(5000)) { throw 'Verified child was not stopped.' }
                }
                if (Test-Path -LiteralPath $statePath) { throw 'Successful stop left the state file behind.' }
            } else {
                foreach ($owned in $ownedChildren) {
                    $owned.Process.Refresh()
                    if ($owned.Process.HasExited) { throw 'Mismatched identity stopped a child.' }
                }
                if (-not $stopError) { throw 'Mismatched identity was accepted.' }
                if (-not (Test-Path -LiteralPath $statePath)) { throw 'Rejected stop removed the state file.' }
            }
            Write-Output "PASS: $($case.Name) (PowerShell $($PSVersionTable.PSVersion))"
        } catch {
            $failures += "$($case.Name): $_"
            Write-Output "FAIL: $($case.Name): $_"
        } finally {
            foreach ($owned in $ownedChildren) {
                $liveChild = Get-Process -Id $owned.Process.Id -ErrorAction SilentlyContinue
                if ($null -ne $liveChild) {
                    if ($liveChild.StartTime.ToUniversalTime().Ticks -ne $owned.Start.Ticks -or $liveChild.Path -ine $owned.Executable) {
                        throw 'Test cleanup refused a child whose process identity changed.'
                    }
                    Stop-Process -InputObject $liveChild -Force
                    $null = $liveChild.WaitForExit(5000)
                }
            }
            if (Test-Path -LiteralPath $statePath) { Remove-Item -LiteralPath $statePath -Force }
        }
    }
} finally {
    Remove-Item -LiteralPath $testRoot
}
if ($failures.Count -gt 0) { throw ($failures -join [Environment]::NewLine) }
