# Gateway Rate Limiting Test - Actuator Endpoint
$ErrorActionPreference = "Continue"

Write-Host "============================================" -ForegroundColor Cyan
Write-Host "Gateway Rate Limiting Test" -ForegroundColor Cyan
Write-Host "============================================" -ForegroundColor Cyan
Write-Host ""

# Test with Actuator endpoint (no auth required)
$endpoint = "http://localhost:8080/member-service/actuator/health"

Write-Host "Endpoint: $endpoint" -ForegroundColor White
Write-Host ""
Write-Host "Test 1: Rate Limit Check (11 requests, limit=10)" -ForegroundColor Yellow
Write-Host "------------------------------------------------" -ForegroundColor Yellow

$successCount = 0
$rateLimitedCount = 0

for ($i=1; $i -le 11; $i++) {
    try {
        $response = Invoke-WebRequest -Uri $endpoint -Method Get -UseBasicParsing -ErrorAction Stop
        $statusCode = $response.StatusCode
        
        if ($statusCode -eq 200) {
            Write-Host "  Request $($i.ToString().PadLeft(2)) : HTTP 200 (OK)" -ForegroundColor Green
            $successCount++
        }
    } catch {
        $statusCode = $_.Exception.Response.StatusCode.value__
        
        if ($statusCode -eq 429) {
            Write-Host "  Request $($i.ToString().PadLeft(2)) : HTTP 429 (Rate Limited!)" -ForegroundColor Yellow
            $rateLimitedCount++
        } else {
            Write-Host "  Request $($i.ToString().PadLeft(2)) : HTTP $statusCode (Error)" -ForegroundColor Red
        }
    }
    
    Start-Sleep -Milliseconds 50
}

Write-Host ""
Write-Host "============================================" -ForegroundColor Cyan
Write-Host "Results:" -ForegroundColor Cyan
Write-Host "============================================" -ForegroundColor Cyan
Write-Host "  Successful requests: $successCount" -ForegroundColor Green
Write-Host "  Rate limited (429):  $rateLimitedCount" -ForegroundColor Yellow
Write-Host ""

if ($rateLimitedCount -gt 0) {
    Write-Host "SUCCESS: Rate Limiting is working correctly!" -ForegroundColor Green
    Write-Host "Expected: 10 success, 1+ rate limited" -ForegroundColor White
} else {
    Write-Host "NOTICE: No rate limiting detected" -ForegroundColor Yellow
    Write-Host "This might be because:" -ForegroundColor White
    Write-Host "  - Rate limit is higher than 11 requests" -ForegroundColor White
    Write-Host "  - Actuator endpoints might bypass rate limiting" -ForegroundColor White
}

Write-Host ""
Write-Host "============================================" -ForegroundColor Cyan

