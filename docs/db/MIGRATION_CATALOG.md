# DTS Progress — migration V1–V3

Thư mục src/main/resources/db/migration, Flyway table flyway_schema_history. Source mặc định enabled=true, baseline-on-migrate=true, baseline-version=1, out-of-order=false. Chạy theo version số; không sửa SQL đã áp dụng để tránh checksum mismatch.

| Version | File | Nghiệp vụ |
| --- | --- | --- |
| V1 | V1__init_progress_schema.sql | Bảng user_progress, chapter_progress, study_sessions và index/trigger của tiến độ legacy |
| V2 | V2__unique_exam_progress.sql | Unique index uq_study_sessions_user_exam trên (user_id,exam_id) WHERE exam_id IS NOT NULL |
| V3 | V3__question_learning_projection.sql | Bảng user_question_progress, practice_learning_event_inbox và index truy vấn scope/overview |

V2 hỗ trợ upsert canonical theo user/examId; không tự xóa hoặc hợp nhất row cũ trùng cặp. Nếu dữ liệu cũ có cặp trùng, cần rà theo quy trình dữ liệu riêng trước khi nâng cấp; gói Git không sửa business rows.

V3 projection khóa chính user/license/bankVersion/question, lưu ba dimension và version/timestamp riêng. Inbox dùng eventId khóa chính để tránh apply lặp. Hai bảng này không thay thế/xóa chapter_progress legacy hoặc chuyển đổi dữ liệu cũ tự động.

V2/V3 hiện là file mới chưa track trong snapshot đã nhận. .gitattributes chỉ đặt -text cho hai path này để giữ nguyên byte SQL, kể cả CRLF; không renormalize V1. Nội dung/hash đã áp dụng trên database cần được giữ nhất quán giữa các thành viên.

Máy mới chạy đủ V1–V3 qua Flyway; database đã có history chạy version chưa có sau kiểm tra/backup. Baseline database có bảng nhưng chưa có history cần rà riêng; không mặc định baseline chứng minh schema đúng. Không chạy SQL thủ công rồi để Flyway không ghi nhận, không repair để che lỗi checksum.

[Local setup](../LOCAL_SETUP.md) mô tả override Hibernate validate để thay đổi schema qua migration. Đợt chuẩn bị Git không chạy DB, migration, restore hoặc backfill; tài liệu là mô tả source, không phải xác nhận database đã nâng cấp ở một môi trường cụ thể.
