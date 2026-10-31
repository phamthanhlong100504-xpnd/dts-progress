# Phân quyền API Progress

## Part 0 — Classification & Identity

Phạm vi là toàn bộ 12 mapping của 6 controller tại baseline f775eaa. Trong đó có 9 API người dùng, 2 API ingress nội bộ và 1 trang trạng thái service. Health/docs/OPTIONS tiếp tục theo SecurityConfig hiện có.

## Part 1 — API Contract

| Method | Path | Required authorization | Blueprint |
| --- | --- | --- | --- |
| GET | /api/v1/progress/dashboard | progress:read | [Get Learning Dashboard](get_dashboard.md) |
| GET | /api/v1/progress/chapters | progress:read | [List Chapter Progress](list_chapter_progress.md) |
| PATCH | /api/v1/progress/chapters | progress:update | [Update Chapter Progress](update_chapter_progress.md) |
| GET | /api/v1/progress/streaks | progress:read | [Get Study Streak](get_streak.md) |
| POST | /api/v1/progress/sessions | progress:update | [Log Study Session](log_study_session.md) |
| GET | /api/v1/progress/history | progress:read | [List Study Session History](list_session_history.md) |
| GET | /api/v1/progress/recent | progress:read | [List Recent Study Sessions](list_recent_sessions.md) |
| GET | /api/v1/progress/learning-overview | progress:read | [Get Learning Overview](get_learning_overview.md) |
| PUT | /api/v1/progress/preferences/learning-program | progress:update | [Update Learning Program Preference](update_learning_preference.md) |
| POST | /internal/v1/practice/exam-results | X-DTS-Internal-Key | [Receive Practice Exam Progress](receive_exam_progress.md) |
| POST | /internal/v1/practice/question-progress | X-DTS-Internal-Key | [Receive Practice Question Progress](receive_question_progress.md) |
| GET | / | Public | [Get Progress Service Home](get_service_home.md) |

`progress:read` và `progress:update` độc lập. Có quyền cập nhật không tự cấp quyền đọc và ngược lại. ADMIN không bypass permission. JWT chứa tên quyền thuần; `PERM_` chỉ là prefix authority trong Spring. Quyền cho phép hành động trên dữ liệu của principal, không cho phép chọn user khác.

## Part 2 — Processing Specification

1. JwtValidationFilter tiếp tục xác minh chữ ký/issuer như baseline. Không chuyển sang tin X-User-* do client gửi.
2. JwtAuthorityMapper giữ ROLE_STUDENT/ROLE_ADMIN đã có prefix, thêm ROLE_ một lần cho role chưa prefix; map từng permission sang PERM_<name>. Không suy quyền từ role, không mở wildcard, không cấp mặc định khi thiếu claim.
3. Method security kiểm permission tại cả 9 method người dùng. Request bị từ chối không gọi service nghiệp vụ. Bind/Bean Validation có thể chạy trước khi gọi method controller; không cam kết payload sai luôn trả 403.
4. Service/repository giữ userId từ principal, ownership, receipt canonical và projection hiện có.
5. Hai ingress chỉ được bỏ qua JWT tại đúng POST; shared key không rỗng vẫn bắt buộc, so sánh constant time trước parse event.

## Part 3 — Data Interaction

Không thay schema hoặc dữ liệu Progress. Không sửa V1–V3. Permission catalog và role_permissions thuộc Identity: cần migration mới trong repo Identity trước rollout; không thêm bảng quyền vào Progress và không cấp quyền bằng sửa tay database.

## Part 4 — Operational Notes

Triển khai theo thứ tự: migration Identity đã review trên nhánh riêng; lấy JWT mới; kiểm quyền; kiểm thử candidate; áp dụng source Progress; chạy lại service bằng cấu hình local đã dùng; test qua Gateway và giao diện. Không chạy lại lệnh boot mặc định làm mất cấu hình đang hoạt động.

Bản source này chưa chứa thay đổi xác thực Gateway của Task002. Nếu local đã thay pipeline xác thực, dừng tại preflight khi source khác và đối chiếu bản đó. Không tắt filter hoặc tin header để làm test qua.

Kiểm thử security nằm ngoài repo trong gói áp dụng; test surefire phải kiểm report failure/error/skipped thực tế vì pom baseline có testFailureIgnore=true. Công cụ test chỉ ép false trong bản staging ngoài repo.
