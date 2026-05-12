param(
    [Parameter(Mandatory = $false)]
    [string]$BaseUrl = "http://localhost:8080"
)

$ErrorActionPreference = "Stop"

function Normalize-BaseUrl([string]$url) {
    if ([string]::IsNullOrWhiteSpace($url)) { return "http://localhost:8080" }
    return $url.TrimEnd("/")
}

function Read-ErrorResponseBody($response) {
    try {
        $stream = $response.GetResponseStream()
        if ($null -eq $stream) { return $null }
        $reader = New-Object System.IO.StreamReader($stream)
        return $reader.ReadToEnd()
    } catch {
        return $null
    }
}

function Invoke-Api {
    param(
        [Parameter(Mandatory = $true)][string]$Method,
        [Parameter(Mandatory = $true)][string]$Path,
        [Parameter(Mandatory = $false)][object]$Body
    )

    $uri = "$script:BaseUrl$Path"
    $headers = @{ "Accept" = "application/json" }

    try {
        $iwrParams = @{
            Method  = $Method
            Uri     = $uri
            Headers = $headers
        }

        $iwrCommand = Get-Command Invoke-WebRequest -ErrorAction Stop
        if ($iwrCommand.Parameters.ContainsKey("UseBasicParsing")) {
            $iwrParams.UseBasicParsing = $true
        }

        if ($null -ne $Body) {
            $payload = $Body | ConvertTo-Json -Depth 10 -Compress
            $iwrParams.ContentType = "application/json"
            $iwrParams.Body = $payload
            $resp = Invoke-WebRequest @iwrParams
        } else {
            $resp = Invoke-WebRequest @iwrParams
        }

        $json = $null
        if ($resp.Content) {
            try { $json = $resp.Content | ConvertFrom-Json } catch { $json = $null }
        }

        return [pscustomobject]@{
            StatusCode = [int]$resp.StatusCode
            Content    = $resp.Content
            Json       = $json
        }
    } catch {
        $status = $null
        $content = $null

        try {
            $ex = $_.Exception
            $response = $null
            if ($ex.PSObject.Properties.Match("Response").Count -gt 0) {
                $response = $ex.Response
            }

            if ($null -ne $response) {
                if ($response.PSObject.Properties.Match("StatusCode").Count -gt 0) {
                    $status = [int]$response.StatusCode
                }

                if ($response.PSObject.Methods.Match("GetResponseStream").Count -gt 0) {
                    $content = Read-ErrorResponseBody $response
                } elseif ($ex.PSObject.Properties.Match("Message").Count -gt 0) {
                    $content = $ex.Message
                }
            }
        } catch {
            $status = $null
        }

        $json = $null
        if ($content) {
            try { $json = $content | ConvertFrom-Json } catch { $json = $null }
        }

        return [pscustomobject]@{
            StatusCode = $status
            Content    = $content
            Json       = $json
            Error      = $_.Exception.Message
        }
    }
}

function Assert-True([bool]$condition, [string]$message) {
    if (-not $condition) { throw $message }
}

function Assert-Status([object]$resp, [int[]]$expected, [string]$context) {
    $actual = $resp.StatusCode
    Assert-True ($expected -contains $actual) "$context failed. Expected status $($expected -join ',') but got $actual. Body: $($resp.Content)"
}

function New-RunId {
    $ts = Get-Date -Format "yyyyMMddHHmmss"
    $suffix = [guid]::NewGuid().ToString("N").Substring(0, 6)
    return "$ts-$suffix"
}

$BaseUrl = Normalize-BaseUrl $BaseUrl
$script:BaseUrl = $BaseUrl

Write-Host "BaseUrl: $BaseUrl"

$runId = New-RunId

$franchiseName = "Franquicia-$runId"
$createFranchise = Invoke-Api -Method "POST" -Path "/api/franchises" -Body @{ name = $franchiseName }
Assert-Status $createFranchise @(201) "2) POST /api/franchises"
$franchiseId = $createFranchise.Json.id
Assert-True ([string]::IsNullOrWhiteSpace($franchiseId) -eq $false) "2) POST /api/franchises failed. Missing id. Body: $($createFranchise.Content)"

$branchName = "Sucursal-$runId"
$createBranch = Invoke-Api -Method "POST" -Path "/api/franchises/$franchiseId/branches" -Body @{ name = $branchName }
Assert-Status $createBranch @(201) "3) POST /api/franchises/{franchiseId}/branches"
$branchId = $createBranch.Json.id
Assert-True ([string]::IsNullOrWhiteSpace($branchId) -eq $false) "3) POST /api/franchises/{franchiseId}/branches failed. Missing id. Body: $($createBranch.Content)"

$productName = "Producto-$runId"
$createProduct = Invoke-Api -Method "POST" -Path "/api/branches/$branchId/products" -Body @{ name = $productName; stock = 10 }
Assert-Status $createProduct @(201) "4) POST /api/branches/{branchId}/products"
$productId = $createProduct.Json.id
Assert-True ([string]::IsNullOrWhiteSpace($productId) -eq $false) "4) POST /api/branches/{branchId}/products failed. Missing id. Body: $($createProduct.Content)"

$updateStock = Invoke-Api -Method "PATCH" -Path "/api/branches/$branchId/products/$productId/stock" -Body @{ stock = 25 }
Assert-Status $updateStock @(200) "6) PATCH /api/branches/{branchId}/products/{productId}/stock"
Assert-True ([int64]$updateStock.Json.stock -eq 25) "6) PATCH /api/branches/{branchId}/products/{productId}/stock failed. Expected stock=25. Body: $($updateStock.Content)"

$deleteProduct = Invoke-Api -Method "DELETE" -Path "/api/branches/$branchId/products/$productId"
Assert-Status $deleteProduct @(204) "5) DELETE /api/branches/{branchId}/products/{productId}"

Write-Host "OK - Requirements 2-6 passed successfully"
