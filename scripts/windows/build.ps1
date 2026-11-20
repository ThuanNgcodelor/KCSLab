# Build CFC Lab trên Windows server: frontend -> source\fe\dist, backend -> source\be\build\*.jar
# Cần: Node.js 22+, JDK 21 (JAVA_HOME). Không cần cài Maven (dùng mvnw.cmd).
# Chạy: powershell -ExecutionPolicy Bypass -File scripts\windows\build.ps1
$ErrorActionPreference = 'Stop'
$Root = Resolve-Path (Join-Path $PSScriptRoot '..\..')

Write-Host '==> Build frontend' -ForegroundColor Green
Push-Location (Join-Path $Root 'source\fe')
try {
    npm ci --no-audit --no-fund; if ($LASTEXITCODE) { throw 'npm ci lỗi' }
    npm run build;               if ($LASTEXITCODE) { throw 'npm run build lỗi' }
} finally { Pop-Location }

Write-Host '==> Build backend (.jar)' -ForegroundColor Green
Push-Location (Join-Path $Root 'source\be')
try {
    .\mvnw.cmd -B -DskipTests package; if ($LASTEXITCODE) { throw 'Build backend lỗi' }
} finally { Pop-Location }

Write-Host '==> Xong. Frontend: source\fe\dist  ·  Backend: source\be\build\*.jar' -ForegroundColor Green
