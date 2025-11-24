@echo off
echo Đang xóa database cũ...

if exist "data\duokid-db.mv.db" (
    del /F "data\duokid-db.mv.db"
    echo Đã xóa duokid-db.mv.db
) else (
    echo File duokid-db.mv.db không tồn tại
)

if exist "data\duokid-db.trace.db" (
    del /F "data\duokid-db.trace.db"
    echo Đã xóa duokid-db.trace.db
) else (
    echo File duokid-db.trace.db không tồn tại
)

echo.
echo Database đã được reset. Chạy lại ứng dụng để tạo database mới:
echo   mvnw.cmd spring-boot:run
pause

