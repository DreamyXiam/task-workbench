[CmdletBinding()]
param([switch]$Offline)
$ErrorActionPreference = 'Stop'
$repo = Split-Path -Parent $PSScriptRoot
Push-Location $repo
try {
    $mavenArgs = @('-B', '-ntp', '-f', 'backend/pom.xml', 'verify')
    if ($Offline) { $mavenArgs += '-o' }
    & mvn @mavenArgs
    if ($LASTEXITCODE -ne 0) { throw 'Backend verification failed' }
    Push-Location frontend
    try {
        & npm ci
        if ($LASTEXITCODE -ne 0) { throw 'npm ci failed' }
        foreach ($step in @('lint', 'typecheck', 'test', 'build')) {
            & npm run $step
            if ($LASTEXITCODE -ne 0) { throw "Frontend $step failed" }
        }
    } finally { Pop-Location }
} finally { Pop-Location }
