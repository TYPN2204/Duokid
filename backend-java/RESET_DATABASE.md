# Reset Database Instructions

Nếu gặp lỗi 500 Internal Server Error, có thể do database schema chưa được cập nhật với các field mới.

## Cách reset database:

1. **Dừng ứng dụng** (Ctrl+C)

2. **Xóa file database:**
   - Xóa file: `backend-java/data/duokid-db.mv.db`
   - Xóa file: `backend-java/data/duokid-db.trace.db` (nếu có)

3. **Chạy lại ứng dụng:**
   ```bash
   .\mvnw.cmd spring-boot:run
   ```

4. Database sẽ được tạo lại tự động với schema mới.

## Hoặc thay đổi trong application.properties:

Thay đổi:
```properties
spring.jpa.hibernate.ddl-auto=update
```

Thành:
```properties
spring.jpa.hibernate.ddl-auto=create-drop
```

Sau đó chạy lại ứng dụng. Lưu ý: Tất cả dữ liệu sẽ bị xóa mỗi lần restart!

