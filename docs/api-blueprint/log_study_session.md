# Log Study Session

## Part 0 — Classification & Identity

- **API Name**: Log Study Session
- **Module**: `progress`
- **API Type**: Authenticated user
- **Authorization**: `progress:update`
- **Related Data**: study_sessions, user_progress

## Part 1 — API Contract

- **HTTP Method**: `POST`
- **URL**: `/api/v1/progress/sessions`
- **Success Status**: `201`

### Headers

| Name | Required | Description |
| --- | --- | --- |
| Authorization | Có | Bearer access token hợp lệ của Identity |
| Content-Type | Có khi có body | application/json |

JWT claim `permissions` phải chứa đúng `progress:update`; authority runtime là `PERM_progress:update`. Role ADMIN/STUDENT không tự bypass permission. Chủ thể lấy từ JWT sub; userId trong query/body/header không chọn được tài khoản khác.

### Request Body

| Name | Type | Validation / Contract |
| --- | --- | --- |
| sessionType | String | @NotNull; Loại phiên theo contract nguồn EXAM/PRACTICE |
| examType | String | Hạng GPLX hoặc mã loại nguồn đã lưu; không đổi tên field trong bản vá |
| mode | String | Chế độ EXAM/PRACTICE theo nguồn phiên |
| examId | UUID | UUID phiên nguồn Practice; nullable với phiên legacy không liên kết bài thi |
| questionsCount | Integer | @NotNull, @Min(1); Số câu của phiên hoặc số câu trong lần cập nhật chương |
| correctCount | Integer | @NotNull, @Min(0); Số câu đúng trong phiên/chương theo contract nguồn |
| wrongCount | Integer | @NotNull, @Min(0); Số câu sai theo DTO hiện có; không tự thay bằng unansweredCount |
| durationSeconds | Integer | @NotNull, @Min(0); Thời gian tính bằng giây |

Giữ Bean Validation và kiểm tra nghiệp vụ của DTO/service hiện có.

### Response

`ApiResponse<SessionHistoryResponse>`. Field null của wrapper có thể bị lược; không tự thêm field hoặc thay đơn vị.

| Field | Java Type | Description |
| --- | --- | --- |
| id | UUID | UUID study_sessions của Progress trong SessionHistoryResponse |
| examId | UUID | UUID phiên nguồn Practice; nullable với phiên legacy không liên kết bài thi |
| sessionType | String | Loại phiên theo contract nguồn EXAM/PRACTICE |
| examType | String | Hạng GPLX hoặc mã loại nguồn đã lưu; không đổi tên field trong bản vá |
| mode | String | Chế độ EXAM/PRACTICE theo nguồn phiên |
| status | String | Trạng thái nghiệp vụ đã lưu của chương hoặc phiên |
| questionsCount | Integer | Số câu của phiên hoặc số câu trong lần cập nhật chương |
| correctCount | Integer | Số câu đúng trong phiên/chương theo contract nguồn |
| wrongCount | Integer | Số câu sai theo DTO hiện có; không tự thay bằng unansweredCount |
| score | Double | Điểm đã lưu/tính bởi nghiệp vụ hiện có; giữ đơn vị và công thức |
| passed | Boolean | Kết quả đạt/trượt từ nguồn chấm điểm hiện có |
| durationSeconds | Integer | Thời gian tính bằng giây |
| startedAt | Instant | Thời điểm bắt đầu |
| completedAt | Instant | Thời điểm hoàn thành; nullable nếu nguồn chưa hoàn tất |

### Error Codes

| Error Code | HTTP | Meaning |
| --- | --- | --- |
| Không bắt buộc có errorCode | 401 | JWT thiếu, hết hạn hoặc không xác minh được |
| AUTH-403 | 403 | Đã xác thực nhưng thiếu progress:update tại method security |
| VAL-400 / REQ-400 | 400 | Bean Validation hoặc JSON lỗi theo handler hiện có |

Lỗi nghiệp vụ/upstream và shape lỗi giữ theo [contract notes](contract_notes.md). Không hứa mọi lỗi binding có cùng mã; method security chạy khi gọi controller sau bước bind argument.

## Part 2 — Processing Specification

1. **Security Layer**: Giữ xác minh JWT/issuer hiện có, chuẩn hóa role một lần và map permission thành `PERM_<permission>`.
2. **Controller Layer**: Kiểm `hasAuthority('PERM_progress:update')` trước gọi service; lấy userId từ principal.
3. **Service Layer**: `ProgressService.logStudySession`. Có examId: forward Bearer tới Practice để lấy receipt chính chủ rồi upsert canonical, không dùng điểm client làm nguồn chấm. Không examId: giữ nhánh legacy tạo phiên/cộng tổng hiện có. Không thay grade, streak hoặc semantics retry.
4. **Repository / External Interaction**: giữ truy vấn, transaction, nguồn dữ liệu và giao tiếp hiện có.

## Part 3 — Data Interaction

- **Target Data**: study_sessions, user_progress
- **Ownership**: Truy vấn/ghi theo userId từ JWT; giữ scope hạng/bank/chương của nghiệp vụ.
- **Denied Request**: không gọi service nghiệp vụ nên không ghi DB/Redis hoặc thay tiến độ.

## Part 4 — Operational Notes

- Source baseline: `f775eaa8a9a4be14d550b1ff1e7831326888f355`; target branch `fix/progress-api-authorization`.
- Bản vá chỉ bổ sung authorization và tài liệu; không thay API request/response, scoring, schema Progress hoặc worker.
- Trước rollout, Identity phải cấp quyền bằng migration và client lấy access token mới. Không tự tạo quyền trong Progress hoặc sửa JWT phía frontend.
- Không ghi JWT/key vào log hoặc tài liệu. Xem [ma trận quyền](authorization.md).

Nghiệp vụ chi tiết: [canonical_exam_progress.md](canonical_exam_progress.md).
