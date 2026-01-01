# Gateway Rate Limiting Test - API Endpoint
$ErrorActionPreference = "Continue"

Write-Host "============================================" -ForegroundColor Cyan
Write-Host "Gateway Rate Limiting Test - API Endpoint" -ForegroundColor Cyan
Write-Host "============================================" -ForegroundColor Cyan
Write-Host ""

# Test with real API endpoint (should have rate limiting)
$endpoint = "http://localhost:8080/member-service/members/1"

Write-Host "Endpoint: $endpoint" -ForegroundColor White
Write-Host "Note: 401 responses are expected (no auth token)" -ForegroundColor Cyan
Write-Host "      We're testing Rate Limiting, not authorization" -ForegroundColor Cyan
Write-Host ""
Write-Host "Test: Sending 15 requests (Rate Limit = 10)" -ForegroundColor Yellow
Write-Host "------------------------------------------------" -ForegroundColor Yellow

$okCount = 0
$authErrorCount = 0
$rateLimitedCount = 0
$otherErrorCount = 0

for ($i=1; $i -le 15; $i++) {
    try {
        $response = Invoke-WebRequest -Uri $endpoint -Method Get -UseBasicParsing -ErrorAction Stop
        $statusCode = $response.StatusCode
        
        if ($statusCode -eq 200) {
            Write-Host "  Request $($i.ToString().PadLeft(2)) : HTTP 200 (OK)" -ForegroundColor Green
            $okCount++
        }
    } catch {
        $statusCode = $_.Exception.Response.StatusCode.value__
        
        if ($statusCode -eq 429) {
            Write-Host "  Request $($i.ToString().PadLeft(2)) : HTTP 429 (RATE LIMITED!)" -ForegroundColor Yellow
            $rateLimitedCount++
        } elseif ($statusCode -eq 401) {
            Write-Host "  Request $($i.ToString().PadLeft(2)) : HTTP 401 (Unauthorized)" -ForegroundColor Gray
            $authErrorCount++
        } else {
            Write-Host "  Request $($i.ToString().PadLeft(2)) : HTTP $statusCode" -ForegroundColor Red
            $otherErrorCount++
        }
    }
    
    Start-Sleep -Milliseconds 50
}

Write-Host ""
Write-Host "============================================" -ForegroundColor Cyan
Write-Host "Results:" -ForegroundColor Cyan
Write-Host "============================================" -ForegroundColor Cyan
Write-Host "  HTTP 200 (OK):           $okCount" -ForegroundColor Green
Write-Host "  HTTP 401 (Unauthorized): $authErrorCount" -ForegroundColor Gray
Write-Host "  HTTP 429 (Rate Limited): $rateLimitedCount" -ForegroundColor Yellow
Write-Host "  Other errors:            $otherErrorCount" -ForegroundColor Red
Write-Host ""

$processedRequests = $okCount + $authErrorCount
Write-Host "  Total processed before rate limit: $processedRequests" -ForegroundColor White
Write-Host ""

if ($rateLimitedCount -gt 0) {
    Write-Host "SUCCESS: Rate Limiting is working!" -ForegroundColor Green
    Write-Host "Expected behavior:" -ForegroundColor White
    Write-Host "  - First 10 requests: Processed (200 OK or 401)" -ForegroundColor White
    Write-Host "  - After 10 requests: 429 Rate Limited" -ForegroundColor White
    Write-Host ""
    Write-Host "Actual result:" -ForegroundColor White
    Write-Host "  - $processedRequests requests processed" -ForegroundColor White
    Write-Host "  - $rateLimitedCount requests rate limited" -ForegroundColor White
} else {
    Write-Host "NOTICE: No rate limiting detected in 15 requests" -ForegroundColor Yellow
    Write-Host "Possible reasons:" -ForegroundColor White
    Write-Host "  1. Rate limit might be configured higher than 10" -ForegroundColor White
    Write-Host "  2. Rate limiting filter might not be applied to this route" -ForegroundColor White
    Write-Host "  3. Need to check gateway-service.yml configuration" -ForegroundColor White
}

Write-Host ""
Write-Host "============================================" -ForegroundColor Cyan

