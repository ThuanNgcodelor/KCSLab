# Chạy backend CFC Lab trên Windows server (Java 21 + LibreOffice cài trên máy, MySQL/Redis của Laragon).
# Đọc cấu hình từ file .env ở thư mục gốc dự án (cùng định dạng với máy Linux).
# Chạy: powershell -ExecutionPolicy Bypass -File scripts\windows\start-backend.ps1
$ErrorActionPreference = 'Stop'
$Root = Resolve-Path (Join-Path $PSScriptRoot '..\..')
$EnvFile = Join-Path $Root '.env'
if (-not (Test-Path $EnvFile)) { throw "Thiếu file .env tại $EnvFile" }

foreach ($raw in Get-Content $EnvFile -Encoding UTF8) {
    $line = $raw.Trim()
    if ($line -eq '' -or $line.StartsWith('#')) { continue }
    $i = $line.IndexOf('=')
    if ($i -lt 1) { continue }
    $name = $line.Substring(0, $i).Trim()
    $value = $line.Substring($i + 1).Trim()
    if ($value.Length -ge 2 -and (($value[0] -eq '"' -and $value[-1] -eq '"') -or ($value[0] -eq "'" -and $value[-1] -eq "'"))) {
        $value = $value.Substring(1, $value.Length - 2)
    }
    [Environment]::SetEnvironmentVariable($name, $value, 'Process')
}
if (-not $env:SPRING_PROFILES_ACTIVE) { $env:SPRING_PROFILES_ACTIVE = 'mysql' }

# LibreOffice: dùng đường dẫn trong .env (REPORT_OFFICE_COMMAND) hoặc tự tìm bản cài mặc định.
# soffice.com là bản console: chờ chuyển xong rồi mới trả về.
if (-not $env:REPORT_OFFICE_COMMAND) {
    $found = @(
        "$env:ProgramFiles\LibreOffice\program\soffice.com",
        "$env:ProgramFiles\LibreOffice\program\soffice.exe",
        "${env:ProgramFiles(x86)}\LibreOffice\program\soffice.com",
        "${env:ProgramFiles(x86)}\LibreOffice\program\soffice.exe"
    ) | Where-Object { $_ -and (Test-Path $_) } | Select-Object -First 1
    if ($found) { $env:REPORT_OFFICE_COMMAND = $found }
    else { Write-Warning 'Không tìm thấy LibreOffice: tải file KQKN dạng DOCX sẽ không chuyển được sang PDF.' }
}

$Backend = Join-Path $Root 'source\be'
$Jar = Get-ChildItem (Join-Path $Backend 'build') -Filter '*.jar' -ErrorAction SilentlyContinue |
    Where-Object { $_.Name -notlike '*original*' } | Select-Object -First 1
if (-not $Jar) { throw 'Chưa có file .jar. Chạy scripts\windows\build.ps1 trước.' }

# Thư mục làm việc = source\be, nên file tải lên nằm ở source\be\storage.
Set-Location $Backend
$port = if ($env:PORT) { $env:PORT } else { '8080' }
Write-Host "==> Backend: http://localhost:$port  (LibreOffice: $env:REPORT_OFFICE_COMMAND)  · Ctrl+C để dừng" -ForegroundColor Green
& java '-Duser.timezone=Asia/Ho_Chi_Minh' '-Dfile.encoding=UTF-8' '-jar' $Jar.FullName
if ($LASTEXITCODE -ne 0) { throw "Backend exited with code $LASTEXITCODE." }
