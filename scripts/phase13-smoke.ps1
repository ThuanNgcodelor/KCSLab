param(
    [string]$BaseUrl = 'http://127.0.0.1:8080/api/v1'
)

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Net.Http

$repositoryRoot = Split-Path -Parent $PSScriptRoot
$environment = @{}
Get-Content -LiteralPath (Join-Path $repositoryRoot '.env') |
    Where-Object { $_ -match '^[A-Za-z_][A-Za-z0-9_]*=' } |
    ForEach-Object {
        $pair = $_ -split '=', 2
        $environment[$pair[0]] = $pair[1]
    }

if (-not $environment.ADMIN_EMAIL -or -not $environment.ADMIN_PASSWORD) {
    throw 'Missing ADMIN_EMAIL or ADMIN_PASSWORD in .env'
}

function New-ApiSession([string]$email, [string]$password) {
    $handler = [System.Net.Http.HttpClientHandler]::new()
    $handler.CookieContainer = [System.Net.CookieContainer]::new()
    $client = [System.Net.Http.HttpClient]::new($handler)
    $client.BaseAddress = [Uri]::new("$BaseUrl/")

    $csrfResponse = $client.GetAsync('auth/csrf').Result
    $csrfResponse.EnsureSuccessStatusCode()
    $csrf = ($csrfResponse.Content.ReadAsStringAsync().Result | ConvertFrom-Json).token

    $loginBody = @{ email = $email; password = $password } | ConvertTo-Json
    $loginContent = [System.Net.Http.StringContent]::new($loginBody, [Text.Encoding]::UTF8, 'application/json')
    $loginResponse = $client.PostAsync('auth/login', $loginContent).Result
    if (-not $loginResponse.IsSuccessStatusCode) {
        throw "Login failed ($([int]$loginResponse.StatusCode))"
    }

    $csrfResponse = $client.GetAsync('auth/csrf').Result
    $csrfResponse.EnsureSuccessStatusCode()
    $csrf = ($csrfResponse.Content.ReadAsStringAsync().Result | ConvertFrom-Json).token
    return @{ Client = $client; Csrf = $csrf }
}

function Send-Json($session, [string]$method, [string]$path, $body = $null) {
    $request = [System.Net.Http.HttpRequestMessage]::new(
        [System.Net.Http.HttpMethod]::new($method),
        $path
    )
    if ($method -notin @('GET', 'HEAD', 'OPTIONS')) {
        $request.Headers.Add('X-XSRF-TOKEN', $session.Csrf)
    }
    if ($null -ne $body) {
        $json = $body | ConvertTo-Json -Depth 12
        $request.Content = [System.Net.Http.StringContent]::new($json, [Text.Encoding]::UTF8, 'application/json')
    }
    $response = $session.Client.SendAsync($request).Result
    $content = $response.Content.ReadAsStringAsync().Result
    if (-not $response.IsSuccessStatusCode) {
        throw "$method $path failed ($([int]$response.StatusCode)): $content"
    }
    if (-not $content) { return $null }
    return $content | ConvertFrom-Json
}

function Upload-Form3($session, [long]$receiptId) {
    $documentPath = Get-ChildItem -LiteralPath $repositoryRoot -Filter '3*.docx' | Select-Object -First 1 -ExpandProperty FullName
    $multipart = [System.Net.Http.MultipartFormDataContent]::new()
    $stream = [IO.File]::OpenRead($documentPath)
    $fileContent = [System.Net.Http.StreamContent]::new($stream)
    $fileContent.Headers.ContentType = [System.Net.Http.Headers.MediaTypeHeaderValue]::Parse(
        'application/vnd.openxmlformats-officedocument.wordprocessingml.document'
    )
    $multipart.Add($fileContent, 'file', [IO.Path]::GetFileName($documentPath))
    $request = [System.Net.Http.HttpRequestMessage]::new(
        [System.Net.Http.HttpMethod]::Post,
        "receipts/$receiptId/attachments?category=FORM_3&description=Smoke%20test%20Phase%201"
    )
    $request.Headers.Add('X-XSRF-TOKEN', $session.Csrf)
    $request.Content = $multipart
    $response = $session.Client.SendAsync($request).Result
    $content = $response.Content.ReadAsStringAsync().Result
    if (-not $response.IsSuccessStatusCode) {
        throw "Form 3 upload failed ($([int]$response.StatusCode)): $content"
    }
    $request.Dispose()
    return $content | ConvertFrom-Json
}

function Assert-Phase([bool]$condition, [string]$message) {
    if (-not $condition) { throw "ASSERTION FAILED: $message" }
}

$stamp = Get-Date -Format 'yyyyMMddHHmmss'
$receptionSession = New-ApiSession 'nguyentrungthuan4177@gmail.com' $environment.ADMIN_PASSWORD
$receivedDate = (Get-Date).ToString('yyyy-MM-dd')
$receipt = Send-Json $receptionSession POST 'receipts' @{
    customerName = "PHASE 1/3 SMOKE TEST $stamp"
    customerPhone = "09$($stamp.Substring($stamp.Length - 8))"
    customerEmail = "phase13-$stamp@example.local"
    customerAddress = 'Can Tho'
    sampledBy = 'E2E sampler'
    sampledAt = (Get-Date).AddHours(-2).ToUniversalTime().ToString('o')
    growingArea = 'CT-E2E growing area'
    packingFacility = 'E2E packing facility'
    vehicleNumber = '65C-E2E'
    trailerNumber = 'MOOC-E2E'
    containerNumber = 'CONT-E2E'
    deliveredBy = 'E2E deliverer'
    sampleName = 'Durian E2E 01'
    quantity = '2 kg'
    packaging = 'PE bag'
    sealNumber = 'SEAL-E2E'
    sealIntact = $true
    sampleCondition = 'Intact'
    conditionDetail = 'Automated smoke-test sample'
    transportInfo = 'Cold truck'
    receivedTemperature = 18.5
    storageCondition = 'Cold'
    requestedParameters = 'Cadimi'
    expectedDate = (Get-Date).AddDays(5).ToString('yyyy-MM-dd')
    specialRequest = 'Phase 1/3 smoke-test data'
    acceptanceDecision = 'ACCEPTED'
    acceptanceReason = $null
    samples = @(
        @{ sourceCode = 'CT901'; productType = 'SR'; name = 'Durian E2E 01'; quantity = '1 kg'; packaging = 'PE bag'; condition = 'Intact'; requestedParameters = 'Cadmium' },
        @{ sourceCode = 'CT902'; productType = 'SR'; name = 'Durian E2E 02'; quantity = '1 kg'; packaging = 'PE bag'; condition = 'Intact'; requestedParameters = 'Auramine O' }
    )
}

Assert-Phase ($receipt.samples.Count -eq 2) 'Receipt must contain two samples'
Assert-Phase (-not [string]::IsNullOrWhiteSpace($receipt.receivedByName)) 'Receipt must contain receiving user'
Assert-Phase ($receipt.growingArea -eq 'CT-E2E growing area') 'Receipt must contain source data'

$attachment = Upload-Form3 $receptionSession $receipt.id
Assert-Phase ($attachment.category -eq 'FORM_3') 'Attachment must be categorized as FORM_3'
Assert-Phase (-not [string]::IsNullOrWhiteSpace($attachment.uploadedByName)) 'Attachment must contain uploader'
Assert-Phase ($null -ne $attachment.id) 'Attachment response must contain generated id'
Assert-Phase (-not [string]::IsNullOrWhiteSpace($attachment.contentUrl)) 'Attachment response must contain content URL'

$transferItems = @($receipt.samples | ForEach-Object {
    @{
        sampleId = $_.id
        weight = $_.quantity
        quantity = $_.quantity
        sampleCondition = $_.sampleCondition
        requestedParameters = $_.requestedParameters
        testMethod = 'CFC-E2E'
    }
})
$transfer = Send-Json $receptionSession POST 'transfers' @{
    receiptId = $receipt.id
    receivingDepartment = 'KCS E2E room'
    transferDate = $receivedDate
    resultsDueDate = (Get-Date).AddDays(5).ToString('yyyy-MM-dd')
    returnToCustomer = $false
    customerReturnDate = $null
    changesNote = 'Smoke test Phase 3'
    items = $transferItems
}
Assert-Phase ($transfer.status -eq 'DRAFT') 'New transfer must be DRAFT'

$transfer = Send-Json $receptionSession PUT "transfers/$($transfer.id)" @{
    receiptId = $receipt.id
    receivingDepartment = 'KCS E2E room updated'
    transferDate = $receivedDate
    resultsDueDate = (Get-Date).AddDays(5).ToString('yyyy-MM-dd')
    returnToCustomer = $false
    customerReturnDate = $null
    changesNote = 'Smoke test Phase 3 - updated draft'
    items = $transferItems
}
Assert-Phase ($transfer.status -eq 'DRAFT') 'Updated transfer must remain DRAFT'
Assert-Phase ($transfer.receivingDepartment -eq 'KCS E2E room updated') 'Draft update must persist receiving department'

$draftReceipt = Send-Json $receptionSession GET "receipts/$($receipt.id)"
Assert-Phase (($draftReceipt.samples | Where-Object status -ne 'RECEIVED').Count -eq 0) 'Draft must not lock samples'

$submitted = Send-Json $receptionSession POST "transfers/$($transfer.id)/submit"
Assert-Phase ($submitted.status -eq 'PENDING') 'Submitted transfer must be PENDING'
Assert-Phase ($null -ne $submitted.submittedAt) 'Submitted transfer must contain submittedAt'

$pendingReceipt = Send-Json $receptionSession GET "receipts/$($receipt.id)"
Assert-Phase (($pendingReceipt.samples | Where-Object status -ne 'TRANSFER_PENDING').Count -eq 0) 'Samples must be locked after submit'

$adminSession = New-ApiSession $environment.ADMIN_EMAIL $environment.ADMIN_PASSWORD
$accepted = Send-Json $adminSession POST "transfers/$($transfer.id)/decision" @{
    decision = 'ACCEPTED'
    receivingNote = 'Both E2E samples checked'
    labOpinion = 'Accepted for E2E testing'
    labRetain = $false
    labRetentionUntil = $null
}
Assert-Phase ($accepted.status -eq 'ACCEPTED') 'KCS/Admin must be able to accept transfer'
Assert-Phase (-not [string]::IsNullOrWhiteSpace($accepted.receivedBy)) 'Transfer must contain KCS receiver'

$finalReceipt = Send-Json $adminSession GET "receipts/$($receipt.id)"
Assert-Phase (($finalReceipt.samples | Where-Object status -ne 'TRANSFERRED').Count -eq 0) 'Samples must become TRANSFERRED'

[PSCustomObject]@{
    receiptId = $receipt.id
    receiptCode = $receipt.receiptCode
    sampleCount = $receipt.samples.Count
    attachmentId = $attachment.id
    transferId = $transfer.id
    transferCode = $transfer.transferCode
    finalTransferStatus = $accepted.status
    finalSampleStatus = $finalReceipt.samples[0].status
} | ConvertTo-Json
