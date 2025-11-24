# Script để reset database
Write-Host "Đang xóa database cũ..." -ForegroundColor Yellow

$dbPath = "data\duokid-db.mv.db"
$tracePath = "data\duokid-db.trace.db"

if (Test-Path $dbPath) {
    Remove-Item $dbPath -Force
    Write-Host "✓ Đã xóa duokid-db.mv.db" -ForegroundColor Green
} else {
    Write-Host "⚠ File duokid-db.mv.db không tồn tại" -ForegroundColor Yellow
}

if (Test-Path $tracePath) {
    Remove-Item $tracePath -Force
    Write-Host "✓ Đã xóa duokid-db.trace.db" -ForegroundColor Green
} else {
    Write-Host "⚠ File duokid-db.trace.db không tồn tại" -ForegroundColor Yellow
}

Write-Host "`nDatabase đã được reset. Chạy lại ứng dụng để tạo database mới:" -ForegroundColor Cyan
Write-Host "  .\mvnw.cmd spring-boot:run" -ForegroundColor White

