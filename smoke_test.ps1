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
            Headers    = $resp.Headers
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
            Headers    = @{}
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

$root = Invoke-Api -Method "GET" -Path "/"
Assert-Status $root @(200) "GET /"
Assert-True ($root.Json.status -eq "ok") "GET / failed. Expected status=ok. Body: $($root.Content)"
Assert-True ([string]::IsNullOrWhiteSpace($root.Json.docs) -eq $false) "GET / failed. Expected docs not empty. Body: $($root.Content)"

$openApi = Invoke-Api -Method "GET" -Path "/v3/api-docs"
Assert-Status $openApi @(200) "GET /v3/api-docs"

$swagger = Invoke-Api -Method "GET" -Path "/swagger-ui.html"
Assert-Status $swagger @(200, 302) "GET /swagger-ui.html"

$franchiseName = "Franquicia-$runId"
$createFranchise = Invoke-Api -Method "POST" -Path "/api/franchises" -Body @{ name = $franchiseName }
Assert-Status $createFranchise @(201) "POST /api/franchises"
$franchiseId = $createFranchise.Json.id
Assert-True ([string]::IsNullOrWhiteSpace($franchiseId) -eq $false) "POST /api/franchises failed. Missing id. Body: $($createFranchise.Content)"

$getFranchises = Invoke-Api -Method "GET" -Path "/api/franchises"
Assert-Status $getFranchises @(200) "GET /api/franchises"
Assert-True ($getFranchises.Json.Count -ge 1) "GET /api/franchises failed. Expected >=1. Body: $($getFranchises.Content)"

$getFranchise = Invoke-Api -Method "GET" -Path "/api/franchises/$franchiseId"
Assert-Status $getFranchise @(200) "GET /api/franchises/{id}"
Assert-True ($getFranchise.Json.name -eq $franchiseName) "GET /api/franchises/{id} failed. Unexpected name. Body: $($getFranchise.Content)"

$renamedFranchiseName = "FranquiciaRenombrada-$runId"
$renameFranchise = Invoke-Api -Method "PATCH" -Path "/api/franchises/$franchiseId" -Body @{ name = $renamedFranchiseName }
Assert-Status $renameFranchise @(200) "PATCH /api/franchises/{id}"
Assert-True ($renameFranchise.Json.name -eq $renamedFranchiseName) "PATCH /api/franchises/{id} failed. Body: $($renameFranchise.Content)"

$branch1Name = "Sucursal-1-$runId"
$branch2Name = "Sucursal-2-$runId"

$createBranch1 = Invoke-Api -Method "POST" -Path "/api/franchises/$franchiseId/branches" -Body @{ name = $branch1Name }
Assert-Status $createBranch1 @(201) "POST /api/franchises/{id}/branches"
$branch1Id = $createBranch1.Json.id
Assert-True ([string]::IsNullOrWhiteSpace($branch1Id) -eq $false) "Missing branchId for branch1. Body: $($createBranch1.Content)"

$createBranch2 = Invoke-Api -Method "POST" -Path "/api/franchises/$franchiseId/branches" -Body @{ name = $branch2Name }
Assert-Status $createBranch2 @(201) "POST /api/franchises/{id}/branches"
$branch2Id = $createBranch2.Json.id
Assert-True ([string]::IsNullOrWhiteSpace($branch2Id) -eq $false) "Missing branchId for branch2. Body: $($createBranch2.Content)"

$getAllBranches = Invoke-Api -Method "GET" -Path "/api/branches"
Assert-Status $getAllBranches @(200) "GET /api/branches"
Assert-True ($getAllBranches.Json.Count -ge 2) "GET /api/branches failed. Expected >=2. Body: $($getAllBranches.Content)"

$getBranches = Invoke-Api -Method "GET" -Path "/api/franchises/$franchiseId/branches"
Assert-Status $getBranches @(200) "GET /api/franchises/{id}/branches"
Assert-True ($getBranches.Json.Count -ge 2) "GET /api/franchises/{id}/branches failed. Expected >=2. Body: $($getBranches.Content)"

$renameBranch1Name = "Sucursal-1-Ren-$runId"
$renameBranch1 = Invoke-Api -Method "PATCH" -Path "/api/branches/$branch1Id" -Body @{ name = $renameBranch1Name }
Assert-Status $renameBranch1 @(200) "PATCH /api/branches/{branchId}"
Assert-True ($renameBranch1.Json.name -eq $renameBranch1Name) "PATCH /api/branches/{branchId} failed. Body: $($renameBranch1.Content)"

$getBranch1 = Invoke-Api -Method "GET" -Path "/api/branches/$branch1Id"
Assert-Status $getBranch1 @(200) "GET /api/branches/{branchId}"
Assert-True ($getBranch1.Json.name -eq $renameBranch1Name) "GET /api/branches/{branchId} failed. Body: $($getBranch1.Content)"

$productAName = "Producto-A-$runId"
$productBName = "Producto-B-$runId"
$productCName = "Producto-C-$runId"

$createProductA = Invoke-Api -Method "POST" -Path "/api/branches/$branch1Id/products" -Body @{ name = $productAName; stock = 10 }
Assert-Status $createProductA @(201) "POST /api/branches/{branchId}/products (A)"
$productAId = $createProductA.Json.id

$createProductB = Invoke-Api -Method "POST" -Path "/api/branches/$branch1Id/products" -Body @{ name = $productBName; stock = 50 }
Assert-Status $createProductB @(201) "POST /api/branches/{branchId}/products (B)"
$productBId = $createProductB.Json.id

$createProductC = Invoke-Api -Method "POST" -Path "/api/branches/$branch2Id/products" -Body @{ name = $productCName; stock = 5 }
Assert-Status $createProductC @(201) "POST /api/branches/{branchId}/products (C)"
$productCId = $createProductC.Json.id

$productsBranch1 = Invoke-Api -Method "GET" -Path "/api/branches/$branch1Id/products"
Assert-Status $productsBranch1 @(200) "GET /api/branches/{branchId}/products"
Assert-True ($productsBranch1.Json.Count -ge 2) "Expected >=2 products in branch1. Body: $($productsBranch1.Content)"

$getProductA = Invoke-Api -Method "GET" -Path "/api/branches/$branch1Id/products/$productAId"
Assert-Status $getProductA @(200) "GET /api/branches/{branchId}/products/{productId}"
Assert-True ($getProductA.Json.name -eq $productAName) "GET /api/branches/{branchId}/products/{productId} unexpected name. Body: $($getProductA.Content)"

$updateStockA = Invoke-Api -Method "PATCH" -Path "/api/branches/$branch1Id/products/$productAId/stock" -Body @{ stock = 80 }
Assert-Status $updateStockA @(200) "PATCH /api/branches/{branchId}/products/{productId}/stock"
Assert-True ([int64]$updateStockA.Json.stock -eq 80) "PATCH /api/branches/{branchId}/products/{productId}/stock failed. Body: $($updateStockA.Content)"

$maxStock = Invoke-Api -Method "GET" -Path "/api/franchises/$franchiseId/branches/max-stock-products"
Assert-Status $maxStock @(200) "GET /api/franchises/{id}/branches/max-stock-products"

$maxBranch1 = $maxStock.Json | Where-Object { $_.branchId -eq $branch1Id } | Select-Object -First 1
$maxBranch2 = $maxStock.Json | Where-Object { $_.branchId -eq $branch2Id } | Select-Object -First 1

Assert-True ($null -ne $maxBranch1) "Expected max-stock result for branch1. Body: $($maxStock.Content)"
Assert-True ($null -ne $maxBranch2) "Expected max-stock result for branch2. Body: $($maxStock.Content)"

Assert-True ($maxBranch1.product.id -eq $productAId) "Expected max product for branch1 to be productA after stock update. Body: $($maxStock.Content)"
Assert-True ([int64]$maxBranch1.product.stock -eq 80) "Expected max stock=80 for branch1. Body: $($maxStock.Content)"

Assert-True ($maxBranch2.product.id -eq $productCId) "Expected max product for branch2 to be productC. Body: $($maxStock.Content)"

$deleteProductB = Invoke-Api -Method "DELETE" -Path "/api/branches/$branch1Id/products/$productBId"
Assert-Status $deleteProductB @(204) "DELETE /api/branches/{branchId}/products/{productId}"

$productsBranch1AfterDelete = Invoke-Api -Method "GET" -Path "/api/branches/$branch1Id/products"
Assert-Status $productsBranch1AfterDelete @(200) "GET /api/branches/{branchId}/products (after delete)"
$stillThere = $productsBranch1AfterDelete.Json | Where-Object { $_.id -eq $productBId } | Select-Object -First 1
Assert-True ($null -eq $stillThere) "Expected deleted product not present in branch1 list. Body: $($productsBranch1AfterDelete.Content)"

$renameProductAName = "Producto-A-Ren-$runId"
$renameProductA = Invoke-Api -Method "PATCH" -Path "/api/branches/$branch1Id/products/$productAId" -Body @{ name = $renameProductAName }
Assert-Status $renameProductA @(200) "PATCH /api/branches/{branchId}/products/{productId}"
Assert-True ($renameProductA.Json.name -eq $renameProductAName) "PATCH /api/branches/{branchId}/products/{productId} failed. Body: $($renameProductA.Content)"

$getProductAAfterRename = Invoke-Api -Method "GET" -Path "/api/branches/$branch1Id/products/$productAId"
Assert-Status $getProductAAfterRename @(200) "GET /api/branches/{branchId}/products/{productId} (after rename)"
Assert-True ($getProductAAfterRename.Json.name -eq $renameProductAName) "GET /api/branches/{branchId}/products/{productId} after rename failed. Body: $($getProductAAfterRename.Content)"

$invalidId = Invoke-Api -Method "GET" -Path "/api/franchises/not-a-uuid"
Assert-Status $invalidId @(400) "GET /api/franchises/not-a-uuid"

Write-Host "OK - Smoke test completed successfully"

