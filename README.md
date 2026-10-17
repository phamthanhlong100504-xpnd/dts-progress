# DTS Progress

Service Java21 / Spring Boot3.3.2 lưu tiến độ học tập, phiên học và số liệu dashboard của người học. PostgreSQL lưu dữ liệu, Redis lưu lựa chọn learning program theo luồng cũ, Kafka nhận user-events từ Identity. Practice chuyển dữ liệu bài thi/câu hỏi chuẩn sang hai HTTP ingress nội bộ.

| Nội dung | Tài liệu |
| --- | --- |
| Chạy máy mới, environment, migration | [Local setup](docs/LOCAL_SETUP.md) |
| Nghiệp vụ và trách nhiệm từng thành phần | [Progress features](docs/PROGRESS_FEATURES.md) |
| Danh sách API và blueprint | [API blueprints](docs/api-blueprint/README.md) |
| SQL V1–V3 | [Migration catalog](docs/db/MIGRATION_CATALOG.md) |
| Mẫu biến môi trường không chứa secret | [.env.example](.env.example) |

Phần mới so với main của đợt bàn giao gồm canonical exam progress, projection câu hỏi theo user/hạng/bankVersion, learning overview lấy catalog từ Practice và migration V2/V3. Các endpoint dashboard/chapter cũ vẫn có phạm vi dữ liệu riêng, không được tự tráo thành dữ liệu overview mới.

Progress local port8083, database dts_progress qua localhost5434. Web đi qua Gateway8888 theo /api/v1/progress/** hoặc alias /api/progress/v1/progress/**. Source Java/YAML/SQL giữ nguyên nội dung đã chạy local trong quá trình chuẩn bị Git; không build, gọi API thật hay thay database chỉ để commit.

Pull code mang theo chức năng và migration, không mang theo user session, token, secret, volume hoặc số liệu của máy khác. Máy mới cần đúng schema/config và những backend liên quan để chạy đủ luồng web.
