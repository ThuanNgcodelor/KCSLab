[CmdletBinding()]
param(
    [switch]$Pull,
    [switch]$CheckOnly
)

$ErrorActionPreference = 'Stop'
$Root = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
$EnvFile = Join-Path $Root '.env'
$BuildScript = Join-Path $PSScriptRoot 'build.ps1'
$BackendScript = Join-Path $PSScriptRoot 'start-backend.ps1'
$FrontendDirectory = Join-Path $Root 'source\fe'
$RunStateFile = Join-Path $env:LOCALAPPDATA 'CFC-Lab\KCSLab-run.json'

function Write-Step {
    param([string]$Message)
    Write-Host "`n==> $Message" -ForegroundColor Cyan
}

function Get-DotEnvValue {
    param(
        [string]$Name,
        [string]$DefaultValue = ''
    )

    foreach ($rawLine in Get-Content -LiteralPath $EnvFile -Encoding UTF8) {
        $line = $rawLine.Trim()
        if ($line.Length -eq 0 -or $line.StartsWith('#')) { continue }
        if ($line[0] -eq [char]0xFEFF) { $line = $line.Substring(1) }

        $separator = $line.IndexOf('=')
        if ($separator -lt 1) { continue }
        if ($line.Substring(0, $separator).Trim() -ne $Name) { continue }

        $value = $line.Substring($separator + 1).Trim()
        if ($value.Length -ge 2 -and
            (($value[0] -eq '"' -and $value[$value.Length - 1] -eq '"') -or
             ($value[0] -eq "'" -and $value[$value.Length - 1] -eq "'"))) {
            $value = $value.Substring(1, $value.Length - 2)
        }
        return $value
    }

    return $DefaultValue
}

function Test-TcpPort {
    param(
        [string]$HostName,
        [int]$Port,
        [int]$TimeoutMilliseconds = 1200
    )

    $client = New-Object System.Net.Sockets.TcpClient
    try {
        $connection = $client.BeginConnect($HostName, $Port, $null, $null)
        if (-not $connection.AsyncWaitHandle.WaitOne($TimeoutMilliseconds, $false)) {
            return $false
        }
        $client.EndConnect($connection)
        return $true
    }
    catch [System.Net.Sockets.SocketException] {
        return $false
    }
    finally {
        $client.Close()
    }
}

function Wait-TcpPort {
    param(
        [string]$HostName,
        [int]$Port,
        [int]$TimeoutSeconds
    )

    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    while ((Get-Date) -lt $deadline) {
        if (Test-TcpPort -HostName $HostName -Port $Port -TimeoutMilliseconds 500) {
            return $true
        }
        Start-Sleep -Seconds 1
    }
    return $false
}

function Get-PortValue {
    param(
        [string]$VariableName,
        [int]$DefaultValue
    )

    $rawValue = Get-DotEnvValue -Name $VariableName -DefaultValue ([string]$DefaultValue)
    $parsedValue = 0
    if (-not [int]::TryParse($rawValue, [ref]$parsedValue) -or $parsedValue -lt 1 -or $parsedValue -gt 65535) {
        throw "Invalid port value in $VariableName."
    }
    return $parsedValue
}

function Test-PortOrStop {
    param(
        [string]$Label,
        [string]$HostName,
        [int]$Port
    )

    if (Test-TcpPort -HostName $HostName -Port $Port) {
        Write-Host "$Label is reachable at $HostName`:$Port." -ForegroundColor Green
        return
    }

    throw "$Label is not reachable at $HostName`:$Port. Start Laragon services and run the launcher again."
}

function Start-PowerShellWindow {
    param([string]$CommandText)

    $encodedCommand = [Convert]::ToBase64String([Text.Encoding]::Unicode.GetBytes($CommandText))
    Start-Process -FilePath 'powershell.exe' `
        -ArgumentList "-NoExit -NoProfile -ExecutionPolicy Bypass -EncodedCommand $encodedCommand" `
        -WorkingDirectory $Root -PassThru
}

function Save-RunState {
    param(
        [System.Diagnostics.Process]$BackendWindow,
        [string]$BackendCommand,
        [System.Diagnostics.Process]$FrontendWindow,
        [string]$FrontendCommand
    )

    $stateDirectory = Split-Path -Parent $RunStateFile
    if (-not (Test-Path -LiteralPath $stateDirectory)) {
        New-Item -ItemType Directory -Path $stateDirectory -Force | Out-Null
    }

    $state = [ordered]@{
        root = $Root
        windows = @(
            [ordered]@{
                role = 'backend'
                processId = $BackendWindow.Id
                startedUtcTicks = $BackendWindow.StartTime.ToUniversalTime().Ticks
                command = $BackendCommand
            },
            [ordered]@{
                role = 'frontend'
                processId = $FrontendWindow.Id
                startedUtcTicks = $FrontendWindow.StartTime.ToUniversalTime().Ticks
                command = $FrontendCommand
            }
        )
    }
    $state | ConvertTo-Json -Depth 4 | Set-Content -LiteralPath $RunStateFile -Encoding UTF8
}

if (-not (Test-Path -LiteralPath $EnvFile)) {
    throw "Project .env file is missing. Create and configure it before running."
}
if (-not (Test-Path -LiteralPath $BuildScript) -or -not (Test-Path -LiteralPath $BackendScript)) {
    throw 'build.ps1 or start-backend.ps1 is missing from scripts\windows.'
}

if (-not $CheckOnly) {
    $shouldPull = [bool]$Pull
    if (-not $shouldPull) {
        $answer = Read-Host 'Type Y to pull latest code; press Enter to use the current code'
        $shouldPull = $answer -match '^(y|yes)$'
    }

    if ($shouldPull) {
        $gitCommand = Get-Command git.exe -ErrorAction SilentlyContinue
        if (-not $gitCommand) { $gitCommand = Get-Command git -ErrorAction SilentlyContinue }
        $gitPath = if ($gitCommand) { $gitCommand.Source } else { $null }
        if (-not $gitPath) {
            $gitCandidates = @(
                "$env:ProgramFiles\Git\cmd\git.exe",
                "$env:ProgramFiles\Git\bin\git.exe",
                "$env:LOCALAPPDATA\Programs\Git\cmd\git.exe"
            )
            $gitPath = $gitCandidates | Where-Object { $_ -and (Test-Path -LiteralPath $_) } | Select-Object -First 1
        }
        if (-not $gitPath) {
            throw 'Git was not found. Install Git or pull manually, then run again and choose not to pull.'
        }

        $gitProcess = Start-Process -FilePath $gitPath `
            -ArgumentList @('-C', ('"' + $Root + '"'), 'pull', '--ff-only') `
            -NoNewWindow -Wait -PassThru
        if ($gitProcess.ExitCode -ne 0) {
            Write-Warning 'Git could not fast-forward safely. No reset, stash or cleanup was run.'
            $continueWithCurrent = Read-Host 'Build the current local code instead? Type Y to continue; press Enter to stop'
            if ($continueWithCurrent -notmatch '^(y|yes)$') {
                throw 'Pull did not complete; build was not started.'
            }
            Write-Host 'Continuing with the current local code; it was not updated by this pull.' -ForegroundColor Yellow
        }
    }
}

Write-Step 'Check Node.js, npm and Java'
$nodeCommand = Get-Command node.exe -ErrorAction SilentlyContinue
if (-not $nodeCommand) {
    $nodeCandidates = @(
        "$env:ProgramFiles\nodejs\node.exe",
        "$env:LOCALAPPDATA\Programs\nodejs\node.exe"
    )
    $nodePath = $nodeCandidates | Where-Object { $_ -and (Test-Path -LiteralPath $_) } | Select-Object -First 1
    if ($nodePath) {
        $env:PATH = "$(Split-Path -Parent $nodePath);$env:PATH"
        $nodeCommand = Get-Command node.exe -ErrorAction SilentlyContinue
    }
}
if (-not $nodeCommand) { throw 'Node.js was not found in PATH or standard install locations.' }

$nodeVersionOutput = (& node --version 2>&1 | Out-String).Trim()
if ($nodeVersionOutput -notmatch '^v(?<major>\d+)') { throw 'Could not determine the Node.js version.' }
if ([int]$Matches['major'] -lt 22) { throw "Node.js 22 or later is required; found $nodeVersionOutput." }
if (-not (Get-Command npm -ErrorAction SilentlyContinue)) { throw 'npm was not found in PATH.' }

if (-not (Get-Command java.exe -ErrorAction SilentlyContinue)) {
    $javaCandidates = @()
    if ($env:JAVA_HOME) { $javaCandidates += (Join-Path $env:JAVA_HOME 'bin\java.exe') }
    $javaRoots = @(
        "$env:ProgramFiles\Java",
        "$env:ProgramFiles\Eclipse Adoptium",
        "$env:ProgramFiles\Microsoft",
        "$env:USERPROFILE\.jdks"
    )
    foreach ($javaRoot in $javaRoots) {
        if (Test-Path -LiteralPath $javaRoot) {
            $javaCandidates += Get-ChildItem -LiteralPath $javaRoot -Directory -ErrorAction SilentlyContinue |
                Where-Object { $_.Name -match 'jdk|java|temurin' } |
                ForEach-Object { Join-Path $_.FullName 'bin\java.exe' }
        }
    }
    $javaPath = $javaCandidates | Where-Object { $_ -and (Test-Path -LiteralPath $_) } | Select-Object -First 1
    if ($javaPath) {
        $javaBin = Split-Path -Parent $javaPath
        $env:JAVA_HOME = Split-Path -Parent $javaBin
        $env:PATH = "$javaBin;$env:PATH"
    }
}
if (-not (Get-Command java -ErrorAction SilentlyContinue)) { throw 'Java was not found in PATH or standard install locations.' }

$javaVersionOutput = (& $env:ComSpec /c 'java -version 2>&1' | Out-String)
if ($javaVersionOutput -notmatch 'version\s+"(?<major>\d+)') {
    throw 'Could not determine the Java version. Check java -version.'
}
$javaMajor = [int]$Matches['major']
if ($javaMajor -lt 21) { throw "Java 21 or later is required; found Java $javaMajor." }
Write-Host "Node.js $nodeVersionOutput, npm and Java $javaMajor are ready." -ForegroundColor Green

Write-Step 'Check Laragon MySQL and Redis'
$databaseUrl = Get-DotEnvValue -Name 'DB_URL' -DefaultValue 'jdbc:mysql://127.0.0.1:3306/cfc_lab'
if ($databaseUrl -notmatch '^jdbc:mysql://(?<host>\[[^\]]+\]|[^:/?]+)(:(?<port>\d+))?') {
    throw 'Could not parse the MySQL address from DB_URL in .env; its value was not displayed.'
}
$databaseHost = $Matches['host'].TrimStart('[').TrimEnd(']')
$databasePort = if ($Matches['port']) { [int]$Matches['port'] } else { 3306 }
if ($databaseHost -eq 'localhost') { $databaseHost = '127.0.0.1' }
Test-PortOrStop -Label 'MySQL' -HostName $databaseHost -Port $databasePort

$redisHost = Get-DotEnvValue -Name 'REDIS_HOST' -DefaultValue '127.0.0.1'
$redisPort = Get-PortValue -VariableName 'REDIS_PORT' -DefaultValue 6379
if ($redisHost -eq 'localhost') { $redisHost = '127.0.0.1' }
Test-PortOrStop -Label 'Redis' -HostName $redisHost -Port $redisPort
Write-Host 'Port checks confirm the services are listening; the backend verifies database credentials at startup.' -ForegroundColor DarkGray

$frontendOrigins = @(Get-DotEnvValue -Name 'FRONTEND_ORIGIN' -DefaultValue '' -split ',' | ForEach-Object { $_.Trim() })
if ($frontendOrigins -notcontains 'https://lab.cfc.io.vn') {
    Write-Warning 'FRONTEND_ORIGIN in .env does not include https://lab.cfc.io.vn; login/WebSocket requests through the public hostname may fail.'
}
if ((Get-DotEnvValue -Name 'SECURE_COOKIE').ToLowerInvariant() -ne 'true') {
    Write-Warning 'SECURE_COOKIE is not true in .env; review this before using HTTPS login through the public hostname.'
}
if ((Get-DotEnvValue -Name 'PUBLIC_LOOKUP_URL') -ne 'https://lab.cfc.io.vn/tracuu/') {
    Write-Warning 'PUBLIC_LOOKUP_URL is not set to the public hostname; new KQKN QR codes may use an old URL.'
}

Write-Step 'Check LibreOffice'
$officeCommand = Get-DotEnvValue -Name 'REPORT_OFFICE_COMMAND'
$officeCandidates = @()
if ($officeCommand) { $officeCandidates += $officeCommand }
$officeCandidates += @(
    "$env:ProgramFiles\LibreOffice\program\soffice.com",
    "$env:ProgramFiles\LibreOffice\program\soffice.exe",
    "${env:ProgramFiles(x86)}\LibreOffice\program\soffice.com",
    "${env:ProgramFiles(x86)}\LibreOffice\program\soffice.exe"
)
$officePath = $officeCandidates | Where-Object { $_ -and (Test-Path -LiteralPath $_) } | Select-Object -First 1
if ($officePath) {
    Write-Host 'LibreOffice found; DOCX-to-PDF conversion is available.' -ForegroundColor Green
}
else {
    Write-Warning 'LibreOffice was not found. The app can run, but KQKN DOCX-to-PDF conversion will not work.'
}

Write-Step 'Check Cloudflare Tunnel and application ports'
$cloudflaredService = Get-Service -Name 'Cloudflared' -ErrorAction SilentlyContinue
if (-not $cloudflaredService) {
    Write-Warning 'Cloudflared Windows service was not found. The app can run locally, but the tunnel is not started.'
}
elseif ($cloudflaredService.Status -eq 'Running') {
    Write-Host 'Cloudflared service is running.' -ForegroundColor Green
}
elseif ($CheckOnly) {
    Write-Warning "Cloudflared service status is $($cloudflaredService.Status); check-only mode does not start it."
}
else {
    try {
        Start-Service -Name 'Cloudflared' -ErrorAction Stop
        Start-Sleep -Seconds 2
        $cloudflaredService = Get-Service -Name 'Cloudflared'
        if ($cloudflaredService.Status -eq 'Running') {
            Write-Host 'Started the Cloudflared service.' -ForegroundColor Green
        }
        else {
            Write-Warning "Cloudflared service is not running (status: $($cloudflaredService.Status)). Run the launcher as Administrator if needed."
        }
    }
    catch {
        Write-Warning "Could not start Cloudflared service: $($_.Exception.Message). Run the launcher as Administrator if needed."
    }
}

$backendPort = Get-PortValue -VariableName 'PORT' -DefaultValue 8080
$frontendPort = 5173
if (Test-TcpPort -HostName '127.0.0.1' -Port $backendPort) {
    throw "Backend port $backendPort is already in use. Close the manually started backend window, then rerun the launcher to build and start the new version."
}
if (Test-TcpPort -HostName '127.0.0.1' -Port $frontendPort) {
    throw "Frontend port $frontendPort is already in use. Close the manually started npm run dev or Apache process, then rerun the launcher."
}
Write-Host "Backend port $backendPort and frontend port $frontendPort are available." -ForegroundColor Green

if ($CheckOnly) {
    Write-Host "`nChecks complete. -CheckOnly did not build or start the application." -ForegroundColor Green
    exit 0
}

Write-Step 'Build frontend and backend'
& $BuildScript
if (-not $?) { throw 'Build failed; see the error above.' }

Write-Step 'Start backend and frontend in separate windows'
$escapedBackendScript = $BackendScript.Replace("'", "''")
$backendCommandText = '& ' + "'" + $escapedBackendScript + "'"
$backendWindow = Start-PowerShellWindow -CommandText $backendCommandText

$escapedFrontendDirectory = $FrontendDirectory.Replace("'", "''")
$frontendCommandText = "Set-Location -LiteralPath '$escapedFrontendDirectory'; npm run dev"
$frontendWindow = Start-PowerShellWindow -CommandText $frontendCommandText
Save-RunState -BackendWindow $backendWindow -BackendCommand $backendCommandText `
    -FrontendWindow $frontendWindow -FrontendCommand $frontendCommandText

Write-Step 'Wait for local application ports'
$backendReady = Wait-TcpPort -HostName '127.0.0.1' -Port $backendPort -TimeoutSeconds 90
$frontendReady = Wait-TcpPort -HostName '127.0.0.1' -Port $frontendPort -TimeoutSeconds 30
if ($backendReady) {
    Write-Host "Backend is listening at localhost:$backendPort." -ForegroundColor Green
}
else {
    Write-Warning "Backend did not open port $backendPort within 90 seconds. Inspect the backend window for DB or startup errors."
}
if ($frontendReady) {
    Write-Host "Frontend is listening at localhost:$frontendPort." -ForegroundColor Green
}
else {
    Write-Warning "Frontend did not open port $frontendPort within 30 seconds. Inspect the frontend window."
}

Write-Host "`nLauncher started the application windows." -ForegroundColor Green
Write-Host "Frontend: http://localhost:$frontendPort"
Write-Host "Backend:  http://localhost:$backendPort"
Write-Host 'Tunnel:   https://lab.cfc.io.vn'
Write-Host 'Keep both backend/frontend windows open. If the backend reports a DB error, check the credentials in .env.'
