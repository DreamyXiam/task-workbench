[CmdletBinding()]
param([Parameter(Mandatory)][uri]$BaseUrl)
$ErrorActionPreference = 'Stop'
if ($BaseUrl.Scheme -ne 'https') { throw 'HTTPS URL required' }
$response = Invoke-WebRequest -Uri $BaseUrl -Method Get -MaximumRedirection 0
foreach ($header in @('Content-Security-Policy','X-Content-Type-Options','Referrer-Policy','X-Frame-Options')) {
    if (-not $response.Headers[$header]) { throw "Missing security header: $header" }
}
if ($response.Headers['X-Content-Type-Options'] -ne 'nosniff') { throw 'nosniff required' }
Write-Output 'Public response header presence passed. Browser CSRF/cookie/Host/TLS tests remain required.'
