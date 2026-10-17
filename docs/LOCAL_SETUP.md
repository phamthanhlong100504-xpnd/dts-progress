# Chạy DTS Progress trên máy mới

## 1. Công cụ và repo

Dùng JDK21 và Maven hệ thống. Snapshot Progress hiện không có Maven Wrapper được versioned; .gitignore đang bỏ qua .mvn/mvnw/mvnw.cmd, nên không yêu cầu thành viên chạy wrapper lấy từ repo khác. Repo ở ví dụ D:\DTS\dts-progress, main class com.dts.progress.DtsProgressApplication. Nhánh bàn giao hiện là feature/progress-backend-completion cho tới khi nhóm merge về main.

```powershell
Set-Location -LiteralPath 'D:\DTS\dts-progress'
java -version
mvn -version
```

## 2. Các thành phần cần chạy

| Thành phần | Địa chỉ host-local | Mục đích |
| --- | --- | --- |
| Progress | localhost:8083 | API tiến độ và ingress nội bộ |
| PostgreSQL | localhost:5434 / dts_progress | user_progress, chapter_progress, study_sessions, projection/inbox |
| Practice | http://localhost:8087 | Learning catalog, receipt bài thi, worker gửi ingress |
| Identity | http://localhost:8081 | Cấp JWT và user-events |
| Redis | localhost:6379 | Luồng learning-program preference hiện có |
| Kafka | localhost:9092 | Consumer user-events, group progress-service-group |
| Gateway | http://localhost:8888 | Web gọi /api/v1/progress/** |

Progress không tự khởi động các repo khác. Redis/Kafka thường dùng hạ tầng DTS đã có; không tạo container trùng tên/cổng. Kết quả lịch sử/leaderboard/analytics thuộc dts-result, không được thay thế bằng số liệu Progress.

## 3. Cấu hình môi trường

Trên máy mới chưa có file .env, sao chép mẫu rồi điền giá trị của môi trường nhóm:

```powershell
Copy-Item -LiteralPath '.env.example' -Destination '.env'
notepad .env
```

Nếu máy đang có cấu hình hoạt động thì giữ cấu hình hiện có. DB_PASSWORD dùng credential thật của database, JWT_ACCESS_SECRET phải là Base64 secret và issuer khớp Identity: JwtValidationFilter Progress giải mã BASE64 trực tiếp, không có fallback raw-string như một số repo khác.

EXAM_PROGRESS_INTERNAL_KEY là shared key dùng cho cả exam-results và question-progress, phải khớp cấu hình worker Practice đang gửi. Giá trị rỗng làm ingress từ chối403; không viết key thật trong tài liệu. PRACTICE_SERVICE_URL phải trỏ Practice thực tế để đọc catalog/receipt. Redis không dùng password thì REDIS_PASSWORD để trống. EUREKA_ENABLED=false cho local không có Eureka.

Spring Boot/Maven không tự đọc .env. Cấu hình các biến trong IDE hoặc nạp dòng KEY=value vào terminal trước khi chạy Java. Không thêm nháy bao ngoài value; cách nạp dưới đây không thực thi file như mã PowerShell:

```powershell
& {
    $envPath = Join-Path (Get-Location).Path '.env'
    foreach ($line in [IO.File]::ReadAllLines($envPath)) {
        if ([string]::IsNullOrWhiteSpace($line) -or $line.TrimStart().StartsWith('#')) {
            continue
        }
        $pair = $line -split '=', 2
        if ($pair.Count -ne 2 -or $pair[0] -notmatch '^[A-Z][A-Z0-9_]*$') {
            throw 'Dòng .env không đúng KEY=value.'
        }
        [Environment]::SetEnvironmentVariable($pair[0], $pair[1], 'Process')
    }
}
```

## 4. PostgreSQL và migration

Repo có docker-compose.infra.yml cho riêng PostgreSQL. Chỉ trên môi trường chưa có container dts-progress-postgres/cổng5434 tương ứng:

```powershell
docker compose -f docker-compose.infra.yml up -d postgres
```

Nếu container đã có thì dùng database hiện tại, không tạo stack thứ hai hoặc down -v. Sửa DB_PASSWORD trong .env không đổi password của volume PostgreSQL đã khởi tạo; dùng credential thật. Java trên host dùng port5434; Java chạy cùng network Docker dùng port5432 trong container và hostname phù hợp.

Flyway đọc src/main/resources/db/migration, dùng flyway_schema_history, version số V1 → V2 → V3. Mẫu environment bật Flyway/validate-on-migrate và đặt Hibernate ddl-auto=validate để schema thay đổi qua SQL. application.yml hiện có ddl-auto:update được giữ nguyên trong đợt Git; override chỉ có hiệu lực khi nạp vào tiến trình mới.

- Database trống chạy đầy đủ V1–V3.
- Database có lịch sử V1 chạy các version còn thiếu sau khi kiểm tra điều kiện nâng cấp và giữ backup.
- V2 tạo unique index (user_id,exam_id) khi exam_id không null; nếu dữ liệu cũ có cặp trùng, migration sẽ lỗi. Không xóa/sửa dữ liệu cũ tự động hoặc sửa SQL để bỏ qua điều kiện.
- Database có bảng nhưng chưa có lịch sử phải được rà riêng: baseline-on-migrate=true, baseline-version=1 không chứng minh schema đã đầy đủ.
- Không chạy SQL thủ công rồi bỏ qua Flyway history, không đổi checksum/repair để lách lỗi, không viết lại V1/V2/V3 đã áp dụng.

## 5. Khởi động Progress

Sau khi cấu hình được nạp và các phụ thuộc phù hợp đang chạy:

```powershell
Set-Location -LiteralPath 'D:\DTS\dts-progress'
mvn spring-boot:run
```

Khởi động có thể chạy migration đang thiếu và consumer; đây là bước vận hành ghi dữ liệu, không phải bước kiểm tra chỉ đọc. Giữ terminal mở. Đợt chuẩn bị Git không tự chạy lệnh này, migration hoặc những thao tác account/API.

- Swagger: http://localhost:8083/swagger-ui.html
- OpenAPI động: http://localhost:8083/api-docs
- Health: http://localhost:8083/actuator/health
- API trực tiếp: http://localhost:8083/api/v1/progress/...
- Gateway phải được start với PROGRESS_SERVICE_URL=http://localhost:8083; cấu hình host Docker mặc định có thể không resolve được trên Windows.

## 6. Dữ liệu học tập đi vào như thế nào

Identity user-events tạo/cập nhật/xóa row user_progress theo consumer hiện có. Dữ liệu thi từ Practice được worker gửi HTTP tới /internal/v1/practice/exam-results, hoặc client cũ POST sessions với examId để Progress lấy receipt có Bearer từ Practice. Dữ liệu học theo câu được worker gửi HTTP tới /internal/v1/practice/question-progress. Hai ingress dùng shared key, không phải topic Kafka consumer mới trong Progress.

Learning overview cần Practice đúng version để trả /api/v1/questions/learning-catalog. Pull Progress không tự có câu hỏi hay tiến độ của máy người phát triển. Dữ liệu projection được cập nhật khi các luồng Practice tương ứng chạy; không chạy lại fixture hoặc sửa số liệu chỉ để commit source.

src/test/resources/docker-java.properties là cấu hình docker-java api.version=1.44 cho classpath test đã có, không phải secret hay lệnh đổi Docker daemon. Giữ trong nhóm source/test; không thay build/runtime đang hoạt động.
