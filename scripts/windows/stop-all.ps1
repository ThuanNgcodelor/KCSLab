$ErrorActionPreference = 'Stop'
$RunStateFile = Join-Path $env:LOCALAPPDATA 'CFC-Lab\KCSLab-run.json'

function Get-ProcessTreeIds {
    param(
        [int]$RootProcessId,
        [object[]]$ProcessSnapshot
    )

    $result = New-Object System.Collections.Generic.List[int]
    $queue = New-Object System.Collections.Generic.Queue[int]
    $queue.Enqueue($RootProcessId)

    while ($queue.Count -gt 0) {
        $parentId = $queue.Dequeue()
        $result.Add($parentId)
        foreach ($child in $ProcessSnapshot | Where-Object { [int]$_.ParentProcessId -eq $parentId }) {
            $queue.Enqueue([int]$child.ProcessId)
        }
    }
    return ,$result.ToArray()
}

function Confirm-ManagedWindow {
    param(
        [object]$Window,
        [object]$ProcessRecord
    )

    if ($ProcessRecord.Name -ine 'powershell.exe') { return $false }

    $encodedMatch = [regex]::Match([string]$ProcessRecord.CommandLine, '(?i)-EncodedCommand\s+([A-Za-z0-9+/=]+)')
    if (-not $encodedMatch.Success) { return $false }

    try {
        $command = [Text.Encoding]::Unicode.GetString([Convert]::FromBase64String($encodedMatch.Groups[1].Value))
    }
    catch {
        return $false
    }

    return $command -ceq [string]$Window.command
}

if (Test-Path -LiteralPath $RunStateFile) {
    $state = Get-Content -LiteralPath $RunStateFile -Raw -Encoding UTF8 | ConvertFrom-Json
    $processSnapshot = @(Get-CimInstance Win32_Process)

    foreach ($window in @($state.windows)) {
        $pidValue = 0
        if (-not [int]::TryParse([string]$window.processId, [ref]$pidValue) -or $pidValue -lt 1) {
            Write-Warning "Invalid saved process ID for $($window.role); not stopping it."
            continue
        }

        $processRecord = $processSnapshot | Where-Object { [int]$_.ProcessId -eq $pidValue } | Select-Object -First 1
        if (-not $processRecord) {
            Write-Host "$($window.role) window is already stopped." -ForegroundColor DarkGray
            continue
        }
        if (-not (Confirm-ManagedWindow -Window $window -ProcessRecord $processRecord)) {
            Write-Warning "PID $pidValue no longer matches the saved $($window.role) launcher command; leaving it untouched."
            continue
        }

        $process = Get-Process -Id $pidValue -ErrorAction SilentlyContinue
        if (-not $process -or $process.StartTime.ToUniversalTime().Ticks -ne [long]$window.startedUtcTicks) {
            Write-Warning "PID $pidValue does not match the saved process start time; leaving it untouched."
            continue
        }

        $treeIds = @(Get-ProcessTreeIds -RootProcessId $pidValue -ProcessSnapshot $processSnapshot)
        [array]::Reverse($treeIds)
        foreach ($treeProcessId in $treeIds) {
            try {
                Stop-Process -Id $treeProcessId -Force -ErrorAction Stop
            }
            catch {
                if (Get-Process -Id $treeProcessId -ErrorAction SilentlyContinue) {
                    Write-Warning "Could not stop PID ${treeProcessId}: $($_.Exception.Message)"
                }
            }
        }
        Write-Host "Stopped launcher-managed $($window.role) process tree." -ForegroundColor Green
    }
}
else {
    Write-Host 'No launcher process record found. Manually started processes will not be stopped.' -ForegroundColor DarkGray
}

$cloudflaredService = Get-Service -Name 'Cloudflared' -ErrorAction SilentlyContinue
if (-not $cloudflaredService) {
    Write-Host 'Cloudflared service is not installed.' -ForegroundColor DarkGray
}
elseif ($cloudflaredService.Status -ne 'Stopped') {
    $currentUser = [Security.Principal.WindowsPrincipal]::new(
        [Security.Principal.WindowsIdentity]::GetCurrent()
    )
    $isAdministrator = $currentUser.IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)

    if ($isAdministrator) {
        try {
            Stop-Service -Name 'Cloudflared' -ErrorAction Stop
            Write-Host 'Stopped Cloudflared service.' -ForegroundColor Green
        }
        catch {
            Write-Warning "Could not stop Cloudflared service: $($_.Exception.Message)"
            exit 1
        }
    }
    else {
        try {
            Write-Host 'Requesting Administrator permission to stop the Cloudflared service...' -ForegroundColor Yellow
            $scriptPath = $PSCommandPath.Replace('"', '""')
            $elevatedProcess = Start-Process -FilePath 'powershell.exe' `
                -ArgumentList "-NoProfile -ExecutionPolicy Bypass -File `"$scriptPath`"" `
                -Verb RunAs -Wait -PassThru -ErrorAction Stop
            if ($elevatedProcess.ExitCode -ne 0) {
                throw "Elevated stop process exited with code $($elevatedProcess.ExitCode)."
            }
        }
        catch {
            Write-Warning "Backend and frontend were stopped, but Cloudflared may still be running. Administrator permission was not granted or the service could not be stopped: $($_.Exception.Message)"
            exit 1
        }
    }
}
else {
    Write-Host 'Cloudflared service is already stopped.' -ForegroundColor DarkGray
}
