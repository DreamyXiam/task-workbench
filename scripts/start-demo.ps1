[CmdletBinding()]
param(
    [switch]$Demo,
    [Parameter(Mandatory)][string]$JavaPath,
    [Parameter(Mandatory)][string]$NodePath,
    [string]$MavenPath = 'mvn',
    [ValidateRange(1024,65535)][int]$BackendPort = 8080,
    [ValidateRange(1024,65535)][int]$FrontendPort = 5173,
    [switch]$UseBuiltArtifacts
)
$ErrorActionPreference = 'Stop'
function Set-DemoProcessEnvironment([string]$Name,[AllowNull()][object]$Value) {
    # PowerShell converts an untyped $null to an empty string for this .NET API.
    # Empty SPRING_CONFIG_LOCATION suppresses Spring's bundled application.yml.
    if ($null -eq $Value) {
        [Environment]::SetEnvironmentVariable($Name,[System.Management.Automation.Language.NullString]::Value,'Process')
    } else {
        [Environment]::SetEnvironmentVariable($Name,[string]$Value,'Process')
    }
}
if (-not $Demo) { throw 'Pass -Demo explicitly. This launcher supports only the isolated local H2 demo.' }
if ($BackendPort -eq $FrontendPort) { throw 'BackendPort and FrontendPort must differ.' }
$repo = Split-Path -Parent $PSScriptRoot
$JavaPath = (Get-Item -LiteralPath $JavaPath).FullName
$NodePath = (Get-Item -LiteralPath $NodePath).FullName
$savedErrorActionPreference = $ErrorActionPreference
try {
    $ErrorActionPreference = 'Continue'
    $javaVersion = (& $JavaPath -version 2>&1 | Out-String)
} finally {
    $ErrorActionPreference = $savedErrorActionPreference
}
if ($LASTEXITCODE -ne 0 -or $javaVersion -notmatch 'version "21\.') { throw 'JavaPath must be a working supported Java21 executable.' }
$nodeVersion = (& $NodePath --version | Out-String).Trim()
$nodeRelease = if ($nodeVersion -match '^v\d+\.\d+\.\d+$') { [version]$nodeVersion.Substring(1) } else { $null }
$supportedNode = ($nodeVersion -match '^v22\.' -and $nodeRelease -ge [version]'22.23.4') -or
    ($nodeVersion -match '^v24\.' -and $nodeRelease -ge [version]'24.22.1')
if ($LASTEXITCODE -ne 0 -or -not $supportedNode) { throw 'NodePath must be Node22.23.4 or later in the Node22 line, or Node24.22.1 or later in the Node24 line.' }
foreach ($port in @($BackendPort,$FrontendPort)) {
    $listener = [System.Net.Sockets.TcpListener]::new([System.Net.IPAddress]::Loopback,$port)
    try { $listener.Start() } catch { throw "Port $port is already in use; choose another port." } finally { $listener.Stop() }
}
$jar = Join-Path $repo 'backend/target/task-workbench-1.5.0-SNAPSHOT.jar'
$vite = Join-Path $repo 'frontend/node_modules/vite/bin/vite.js'
if (-not (Test-Path -LiteralPath $vite -PathType Leaf)) { throw 'Frontend dependencies are missing. Run npm ci in frontend with Node24, then retry.' }
if (-not $UseBuiltArtifacts) {
    $oldJavaHome = $env:JAVA_HOME
    try {
        $env:JAVA_HOME = Split-Path -Parent (Split-Path -Parent $JavaPath)
        & $MavenPath -B -ntp -f (Join-Path $repo 'backend/pom.xml') package
        if ($LASTEXITCODE -ne 0) { throw 'Backend package failed. No demo process started.' }
    } finally { $env:JAVA_HOME = $oldJavaHome }
}
if (-not (Test-Path -LiteralPath $jar -PathType Leaf)) { throw 'Executable backend JAR missing. Build backend/pom.xml first.' }
$bootstrap = $env:TASK_BOOTSTRAP_PASSWORD
if ([string]::IsNullOrWhiteSpace($bootstrap)) {
    $secure = Read-Host 'Unique demo bootstrap password (12+ characters, letters and digits)' -AsSecureString
    $bootstrap = [System.Net.NetworkCredential]::new('', $secure).Password
}
if ($bootstrap.Length -lt 12 -or [System.Text.Encoding]::UTF8.GetByteCount($bootstrap) -gt 72 -or $bootstrap -notmatch '[A-Za-z]' -or $bootstrap -notmatch '[0-9]') {
    throw 'Demo password needs at least12 characters, letters and digits, at most72 UTF-8 bytes.'
}
# Windows PowerShell 5.1 does not autoload the assembly containing DPAPI.
Add-Type -AssemblyName System.Security
function Protect-DemoKey([string]$Key) {
    $plainBytes = [System.Text.Encoding]::UTF8.GetBytes($Key)
    try {
        $protectedBytes = [System.Security.Cryptography.ProtectedData]::Protect($plainBytes,$null,[System.Security.Cryptography.DataProtectionScope]::CurrentUser)
        return [Convert]::ToBase64String($protectedBytes)
    } finally {
        [Array]::Clear($plainBytes,0,$plainBytes.Length)
    }
}
function Unprotect-DemoKey([string]$ProtectedKey) {
    $protectedBytes = [Convert]::FromBase64String($ProtectedKey.Trim())
    try {
        $plainBytes = [System.Security.Cryptography.ProtectedData]::Unprotect($protectedBytes,$null,[System.Security.Cryptography.DataProtectionScope]::CurrentUser)
        try { return [System.Text.Encoding]::UTF8.GetString($plainBytes) }
        finally { [Array]::Clear($plainBytes,0,$plainBytes.Length) }
    } finally {
        [Array]::Clear($protectedBytes,0,$protectedBytes.Length)
    }
}
$demoRoot = Join-Path $repo ".task-storage/demo-$BackendPort"
New-Item -ItemType Directory -Force $demoRoot | Out-Null
$keyFile = Join-Path $demoRoot 'totp-key.dpapi'
$demoKey = $env:TASK_TOTP_ENCRYPTION_KEY
# Local demo only: Windows DPAPI binds this file to the current user and machine.
if (Test-Path -LiteralPath $keyFile) {
    try { $savedKey = Unprotect-DemoKey -ProtectedKey (Get-Content -LiteralPath $keyFile -Raw) }
    catch { throw 'Existing demo TOTP key cannot be read with the current DPAPI format. Use a new BackendPort for a fresh demo.' }
    if (-not [string]::IsNullOrWhiteSpace($demoKey) -and $demoKey -ne $savedKey) { throw 'Configured demo TOTP key differs from this database key. Use its original key or a new BackendPort for a fresh demo.' }
    $demoKey = $savedKey
} else {
    if ([string]::IsNullOrWhiteSpace($demoKey)) {
        $bytes = [byte[]]::new(32)
        $generator = [System.Security.Cryptography.RandomNumberGenerator]::Create()
        try { $generator.GetBytes($bytes) } finally { $generator.Dispose() }
        $demoKey = [Convert]::ToBase64String($bytes)
    }
    try { $keyBytes = [Convert]::FromBase64String($demoKey) } catch { throw 'TASK_TOTP_ENCRYPTION_KEY must be base64 encoding of32 bytes.' }
    if ($keyBytes.Length -ne 32) { throw 'TASK_TOTP_ENCRYPTION_KEY must encode exactly32 bytes.' }
    [System.IO.File]::WriteAllText($keyFile,(Protect-DemoKey -Key $demoKey),[System.Text.UTF8Encoding]::new($false))
}
$logRoot = Join-Path $repo ".cache/demo-$BackendPort-$FrontendPort"
New-Item -ItemType Directory -Force $demoRoot,$logRoot | Out-Null
$h2Url = 'jdbc:h2:file:' + ((Join-Path $demoRoot 'database') -replace '\\','/') + ';MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1'
$statePath = Join-Path $logRoot 'processes.json'
# Preserve the caller's database and runtime settings; the demo key is local DPAPI protected.
$overrides = @{
    TASK_BOOTSTRAP_PASSWORD = $bootstrap
    TASK_TOTP_ENCRYPTION_KEY = $demoKey
    TASK_DB_URL = $h2Url
    TASK_DB_USER = 'sa'
    TASK_DB_PASSWORD = ''
    TASK_FLYWAY_ENABLED = 'true'
    TASK_SECURE_COOKIE = 'false'
    TASK_BIND_ADDRESS = '127.0.0.1'
    TASK_PORT = [string]$BackendPort
    TASK_STORAGE_ROOT = (Join-Path $demoRoot 'attachments')
    TASK_EXPORT_FONT = $(if ($env:TASK_EXPORT_FONT) { $env:TASK_EXPORT_FONT } elseif (Test-Path -LiteralPath 'C:\Windows\Fonts\simhei.ttf') { 'C:\Windows\Fonts\simhei.ttf' } else { '' })
    TASK_JOBS_ENABLED = 'true'
    TASK_SCANNER_COMMAND = ''
    SPRING_PROFILES_ACTIVE = 'demo'
    SPRING_DATASOURCE_URL = $h2Url
    SPRING_DATASOURCE_USERNAME = 'sa'
    SPRING_DATASOURCE_PASSWORD = ''
    SPRING_APPLICATION_JSON = $null
    SPRING_CONFIG_LOCATION = $null
    SPRING_CONFIG_ADDITIONAL_LOCATION = $null
    TASK_DEV_API_TARGET = "http://127.0.0.1:$BackendPort"
}
$saved = @{}
foreach ($name in $overrides.Keys) { $saved[$name] = [Environment]::GetEnvironmentVariable($name,'Process') }
function ConvertTo-ProcessArgument([string]$Value) {
    if ($Value.Contains('"') -or $Value.Contains("`n") -or $Value.Contains("`r")) { throw 'Invalid process argument.' }
    return '"' + $Value + '"'
}
function Wait-DemoEndpoint([string]$Url,[System.Diagnostics.Process]$Process,[int]$Seconds) {
    $deadline = [DateTime]::UtcNow.AddSeconds($Seconds)
    while ([DateTime]::UtcNow -lt $deadline) {
        if ($Process.HasExited) { throw 'Demo child process exited before readiness. Review its local log files.' }
        try {
            $response = Invoke-WebRequest -Uri $Url -UseBasicParsing -TimeoutSec 2
            if ($response.StatusCode -eq 200) { return }
        } catch { }
        Start-Sleep -Milliseconds 400
    }
    throw 'Demo readiness timed out. Review its local log files.'
}
$backend = $null
$frontend = $null
try {
    foreach ($name in $overrides.Keys) { Set-DemoProcessEnvironment $name $overrides[$name] }
    # Root health must remain unready until ApplicationRunner finishes seeding accounts.
    $backendArguments = @('-jar',$jar,'--spring.profiles.active=demo',"--spring.datasource.url=$h2Url",'--spring.datasource.username=sa','--spring.datasource.password=',"--server.port=$BackendPort",'--server.address=127.0.0.1','--server.servlet.session.cookie.secure=false','--management.health.readinessstate.enabled=true')
    $backend = Start-Process -FilePath $JavaPath -ArgumentList ($backendArguments | ForEach-Object { ConvertTo-ProcessArgument $_ }) -WorkingDirectory $repo -WindowStyle Hidden -RedirectStandardOutput (Join-Path $logRoot 'backend.stdout.log') -RedirectStandardError (Join-Path $logRoot 'backend.stderr.log') -PassThru
    # Never pass bootstrap credentials or the TOTP encryption key to the frontend process.
    Set-DemoProcessEnvironment 'TASK_BOOTSTRAP_PASSWORD' $null
    Set-DemoProcessEnvironment 'TASK_TOTP_ENCRYPTION_KEY' $null
    try {
        $frontendArguments = @($vite,'--host','127.0.0.1','--port',[string]$FrontendPort,'--strictPort')
        $frontend = Start-Process -FilePath $NodePath -ArgumentList ($frontendArguments | ForEach-Object { ConvertTo-ProcessArgument $_ }) -WorkingDirectory (Join-Path $repo 'frontend') -WindowStyle Hidden -RedirectStandardOutput (Join-Path $logRoot 'frontend.stdout.log') -RedirectStandardError (Join-Path $logRoot 'frontend.stderr.log') -PassThru
    } finally { Set-DemoProcessEnvironment 'TASK_TOTP_ENCRYPTION_KEY' $demoKey }
    Wait-DemoEndpoint "http://127.0.0.1:$BackendPort/actuator/health" $backend 90
    Wait-DemoEndpoint "http://127.0.0.1:$FrontendPort" $frontend 30
    $state = @{
        backend = @{ pid=$backend.Id; startedUtc=$backend.StartTime.ToUniversalTime().ToString('o'); executable=$JavaPath }
        frontend = @{ pid=$frontend.Id; startedUtc=$frontend.StartTime.ToUniversalTime().ToString('o'); executable=$NodePath }
        backendUrl = "http://127.0.0.1:$BackendPort"
        frontendUrl = "http://127.0.0.1:$FrontendPort"
    }
    $state | ConvertTo-Json -Depth 4 | Set-Content -LiteralPath $statePath -Encoding UTF8
    Write-Output "Demo ready: http://127.0.0.1:$FrontendPort (backend http://127.0.0.1:$BackendPort)"
    Write-Output "Backend PID $($backend.Id); frontend PID $($frontend.Id)"
    $quotedStatePath = $statePath.Replace("'", "''")
    Write-Output "Stop: ./scripts/stop-demo.ps1 -StateFile '$quotedStatePath'"
    Write-Output "Logs: $logRoot"
} catch {
    foreach ($child in @($frontend,$backend)) {
        if ($null -ne $child -and -not $child.HasExited) { Stop-Process -InputObject $child -Force -ErrorAction SilentlyContinue }
    }
    throw
} finally {
    foreach ($name in $saved.Keys) { Set-DemoProcessEnvironment $name $saved[$name] }
    $bootstrap = $null
    $demoKey = $null
    $secure = $null
}
