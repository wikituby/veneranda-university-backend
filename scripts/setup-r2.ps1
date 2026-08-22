<#
.SYNOPSIS
  Automates Cloudflare R2 setup for Veneranda LMS private videos.

.DESCRIPTION
  Fully automated once you provide:
    - Cloudflare Account ID
    - Cloudflare API Token (permission: Workers R2 Storage Write)
    - R2 S3 Access Key ID + Secret (from R2 → Manage API Tokens — one dashboard step)

  The script will:
    1. Create the R2 bucket (if missing)
    2. Apply CORS for http://localhost:4900
    3. Save R2_* User environment variables
    4. Optionally restart Quarkus

.EXAMPLE
  .\setup-r2.ps1
#>

[CmdletBinding()]
param(
    [string]$AccountId = $env:CLOUDFLARE_ACCOUNT_ID,
    [string]$ApiToken = $env:CLOUDFLARE_API_TOKEN,
    [string]$Bucket = $(if ($env:R2_BUCKET) { $env:R2_BUCKET } else { 'veneranda-videos' }),
    [string]$Origin = 'http://localhost:4900',
    [string]$AccessKeyId = $env:R2_ACCESS_KEY_ID,
    [string]$SecretAccessKey = $env:R2_SECRET_ACCESS_KEY,
    [switch]$SkipRestart,
    [switch]$OpenDashboard
)

$ErrorActionPreference = 'Stop'
$apiBase = 'https://api.cloudflare.com/client/v4'

function Read-Secret([string]$prompt) {
    $secure = Read-Host -Prompt $prompt -AsSecureString
    $bstr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure)
    try {
        return [Runtime.InteropServices.Marshal]::PtrToStringBSTR($bstr)
    } finally {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($bstr)
    }
}

function Invoke-CfApi {
    param(
        [string]$Method,
        [string]$Path,
        [object]$Body = $null
    )
    $headers = @{
        Authorization = "Bearer $ApiToken"
        'Content-Type' = 'application/json'
    }
    $uri = "$apiBase$Path"
    $params = @{
        Method = $Method
        Uri = $uri
        Headers = $headers
        UseBasicParsing = $true
    }
    if ($null -ne $Body) {
        $params.Body = ($Body | ConvertTo-Json -Depth 8 -Compress)
    }
    $response = Invoke-RestMethod @params
    if (-not $response.success) {
        $err = ($response.errors | ConvertTo-Json -Compress)
        throw "Cloudflare API failed: $err"
    }
    return $response
}

Write-Host ''
Write-Host '=== Veneranda · Cloudflare R2 setup ===' -ForegroundColor Cyan
Write-Host ''

if (-not $AccountId) {
    $AccountId = Read-Host 'Cloudflare Account ID (R2 overview page)'
}
if (-not $ApiToken) {
    Write-Host 'Create a Cloudflare API Token with permission: Account → Workers R2 Storage → Edit'
    Write-Host 'https://dash.cloudflare.com/profile/api-tokens'
    Write-Host ''
    $ApiToken = Read-Secret 'Cloudflare API Token'
}

if (-not $AccountId -or -not $ApiToken) {
    throw 'Account ID and API Token are required.'
}

$AccountId = $AccountId.Trim()
$ApiToken = $ApiToken.Trim()
$Bucket = $Bucket.Trim().ToLowerInvariant()

# --- 1) Create bucket ---
Write-Host "[1/4] Ensuring bucket '$Bucket' exists..." -ForegroundColor Yellow
try {
    Invoke-CfApi -Method POST -Path "/accounts/$AccountId/r2/buckets" -Body @{ name = $Bucket } | Out-Null
    Write-Host "      Created bucket '$Bucket'." -ForegroundColor Green
} catch {
    $msg = "$_"
    if ($msg -match '10004|already exists|409|Bucket already exists') {
        Write-Host "      Bucket already exists — OK." -ForegroundColor Green
    } else {
        # Try GET list to see if it exists
        try {
            $list = Invoke-CfApi -Method GET -Path "/accounts/$AccountId/r2/buckets"
            $names = @($list.result.buckets | ForEach-Object { $_.name })
            if ($names -contains $Bucket) {
                Write-Host "      Bucket already exists — OK." -ForegroundColor Green
            } else {
                throw
            }
        } catch {
            throw "Could not create/verify bucket: $msg"
        }
    }
}

# --- 2) CORS ---
Write-Host "[2/4] Applying CORS for $Origin ..." -ForegroundColor Yellow
$corsBody = @{
    rules = @(
        @{
            allowed = @{
                origins = @($Origin)
                methods = @('GET', 'HEAD')
                headers = @('*')
            }
            exposeHeaders = @('ETag', 'Content-Length', 'Content-Type')
            maxAgeSeconds = 3600
        }
    )
}
Invoke-CfApi -Method PUT -Path "/accounts/$AccountId/r2/buckets/$Bucket/cors" -Body $corsBody | Out-Null
Write-Host '      CORS applied.' -ForegroundColor Green

# --- 3) S3 credentials ---
Write-Host '[3/4] R2 S3 Access Key (for Quarkus signed URLs)...' -ForegroundColor Yellow
if (-not $AccessKeyId -or -not $SecretAccessKey) {
    $tokenUrl = "https://dash.cloudflare.com/$AccountId/r2/api-tokens"
    Write-Host ''
    Write-Host '  Cloudflare does not expose S3 key creation via API.' -ForegroundColor DarkYellow
    Write-Host '  Do this once in the browser:' -ForegroundColor DarkYellow
    Write-Host "  1. Open: $tokenUrl"
    Write-Host '  2. Create Account API token → Object Read & Write → this bucket'
    Write-Host '  3. Copy Access Key ID + Secret Access Key'
    Write-Host ''
    if ($OpenDashboard -or (Read-Host 'Open that page in your browser now? (Y/n)') -notmatch '^[nN]') {
        Start-Process $tokenUrl
    }
    $AccessKeyId = Read-Host 'Paste Access Key ID'
    $SecretAccessKey = Read-Secret 'Paste Secret Access Key'
}

$AccessKeyId = $AccessKeyId.Trim()
$SecretAccessKey = $SecretAccessKey.Trim()
if (-not $AccessKeyId -or -not $SecretAccessKey) {
    throw 'Access Key ID and Secret Access Key are required.'
}

$endpoint = "https://$AccountId.r2.cloudflarestorage.com"

Write-Host '[4/4] Saving Windows User environment variables...' -ForegroundColor Yellow
[Environment]::SetEnvironmentVariable('R2_ENABLED', 'true', 'User')
[Environment]::SetEnvironmentVariable('R2_ENDPOINT', $endpoint, 'User')
[Environment]::SetEnvironmentVariable('R2_ACCESS_KEY_ID', $AccessKeyId, 'User')
[Environment]::SetEnvironmentVariable('R2_SECRET_ACCESS_KEY', $SecretAccessKey, 'User')
[Environment]::SetEnvironmentVariable('R2_BUCKET', $Bucket, 'User')
[Environment]::SetEnvironmentVariable('R2_PLAYBACK_TTL_SECONDS', '900', 'User')
[Environment]::SetEnvironmentVariable('CLOUDFLARE_ACCOUNT_ID', $AccountId, 'User')

# Current process too (so restart below sees them)
$env:R2_ENABLED = 'true'
$env:R2_ENDPOINT = $endpoint
$env:R2_ACCESS_KEY_ID = $AccessKeyId
$env:R2_SECRET_ACCESS_KEY = $SecretAccessKey
$env:R2_BUCKET = $Bucket
$env:R2_PLAYBACK_TTL_SECONDS = '900'

Write-Host '      Saved:' -ForegroundColor Green
Write-Host "        R2_ENABLED=true"
Write-Host "        R2_ENDPOINT=$endpoint"
Write-Host "        R2_BUCKET=$Bucket"
Write-Host "        R2_ACCESS_KEY_ID=$AccessKeyId"
Write-Host "        R2_SECRET_ACCESS_KEY=********"

$backendDir = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
# script lives in backend/scripts → backend root is parent of scripts
$backendDir = Split-Path $PSScriptRoot -Parent

if (-not $SkipRestart) {
    $restart = Read-Host 'Restart Quarkus backend now? (Y/n)'
    if ($restart -notmatch '^[nN]') {
        Write-Host 'Starting mvn quarkus:dev (new window)...' -ForegroundColor Yellow
        $cmd = @"
`$env:GOOGLE_CLIENT_ID=[Environment]::GetEnvironmentVariable('GOOGLE_CLIENT_ID','User')
`$env:GOOGLE_GROUPS_ENABLED=[Environment]::GetEnvironmentVariable('GOOGLE_GROUPS_ENABLED','User')
`$env:GOOGLE_GROUPS_WEBHOOK_URL=[Environment]::GetEnvironmentVariable('GOOGLE_GROUPS_WEBHOOK_URL','User')
`$env:GOOGLE_GROUPS_SECRET=[Environment]::GetEnvironmentVariable('GOOGLE_GROUPS_SECRET','User')
`$env:GOOGLE_GROUPS_AUTO_CREATE_ON_ROOT=[Environment]::GetEnvironmentVariable('GOOGLE_GROUPS_AUTO_CREATE_ON_ROOT','User')
`$env:GOOGLE_GROUPS_DEFAULT_DOMAIN=[Environment]::GetEnvironmentVariable('GOOGLE_GROUPS_DEFAULT_DOMAIN','User')
`$env:R2_ENABLED='true'
`$env:R2_ENDPOINT='$endpoint'
`$env:R2_ACCESS_KEY_ID='$AccessKeyId'
`$env:R2_SECRET_ACCESS_KEY='$SecretAccessKey'
`$env:R2_BUCKET='$Bucket'
`$env:R2_PLAYBACK_TTL_SECONDS='900'
Set-Location '$backendDir'
mvn quarkus:dev -DskipTests
"@
        Start-Process powershell -ArgumentList '-NoExit', '-Command', $cmd
        Write-Host '      Backend restart window opened. Wait until Listening on :8081' -ForegroundColor Green
    }
}

Write-Host ''
Write-Host 'Done. In the LMS Videos tab you should see Cloudflare R2 connected + Upload.' -ForegroundColor Cyan
Write-Host ''
