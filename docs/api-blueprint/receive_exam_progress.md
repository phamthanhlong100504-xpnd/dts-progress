# Receive Practice Exam Progress

## Part 0 — Classification & Identity

- **API Name**: Receive Practice Exam Progress
- **Module**: `progress`
- **API Type**: Internal service to service
- **Authorization**: `Khóa service nội bộ`
- **Related Data**: user_progress, study_sessions

## Part 1 — API Contract

- **HTTP Method**: `POST`
- **URL**: `/internal/v1/practice/exam-results`
- **Success Status**: `200`

### Headers

| Name | Required | Description |
| --- | --- | --- |
| X-DTS-Internal-Key | Có | Khớp cấu hình progress.internal-key; không rỗng |
| Content-Type | Có | application/json |

Bearer của học viên và quyền progress:* không thay thế shared key. Không đưa key thật vào docs/log.

### Request Body

| Name | Type | Validation / Contract |
| --- | --- | --- |
| eventId | UUID | UUID định danh sự kiện nguồn để xử lý canonical/dedup theo contract |
| eventType | String | Loại sự kiện EXAM_FINISHED hoặc QUESTION_PROGRESS_CHANGED |
| schemaVersion | Integer | Phiên bản schema sự kiện theo contract ingress |
| source | String | Service nguồn dts-practice |
| userId | UUID | UUID chủ dữ liệu trong event từ worker đã xác thực; không phải query chọn user của API học viên |
| examId | UUID | UUID phiên nguồn Practice; nullable với phiên legacy không liên kết bài thi |
| licenseClass | String | Mã hạng GPLX; catalog Practice quyết định dữ liệu hợp lệ |
| bankVersion | String | Phiên bản ngân hàng dùng để giới hạn projection |
| mode | String | Chế độ EXAM/PRACTICE theo nguồn phiên |
| status | String | Trạng thái nghiệp vụ đã lưu của chương hoặc phiên |
| totalQuestions | Integer | Tổng câu từ receipt nguồn Practice |
| correctCount | Integer | Số câu đúng trong phiên/chương theo contract nguồn |
| wrongCountLegacy | Integer | Số sai legacy trong event, giữ tương thích nguồn |
| answeredWrongCount | Integer | Số câu đã trả lời sai từ nguồn |
| unansweredCount | Integer | Số câu chưa trả lời từ nguồn |
| score | Integer | Điểm đã lưu/tính bởi nghiệp vụ hiện có; giữ đơn vị và công thức |
| passed | Boolean | Kết quả đạt/trượt từ nguồn chấm điểm hiện có |
| durationSeconds | Integer | Thời gian tính bằng giây |
| startedAt | Instant | Thời điểm bắt đầu |
| completedAt | Instant | Thời điểm hoàn thành; nullable nếu nguồn chưa hoàn tất |

Các event nội bộ được service kiểm tra sau khi xác thực shared key. DTO preference chưa có @Valid; bản vá không thêm quy tắc nghiệp vụ mới.

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
| INTERNAL_AUTH_REQUIRED | 403 | Key thiếu, sai hoặc cấu hình rỗng |
| BAD_REQUEST | 400 | JSON/event không hợp lệ theo nhánh hiện có |

Lỗi nghiệp vụ/upstream và shape lỗi giữ theo [contract notes](contract_notes.md). Không hứa mọi lỗi binding có cùng mã; method security chạy khi gọi controller sau bước bind argument.

## Part 2 — Processing Specification

1. **Security Layer**: Cho qua JWT chain tại đúng hai POST nội bộ; controller bắt buộc kiểm shared key.
2. **Controller Layer**: Kiểm khóa trước parse event; không đòi permission học viên.
3. **Service Layer**: `CanonicalExamProgressService.apply`. Kiểm shared key không rỗng bằng constant-time compare trước khi parse JSON. Canonical service kiểm event và upsert theo user/examId, cập nhật tổng bằng delta. Giữ dedup và nguồn grade Practice.
4. **Repository / External Interaction**: giữ truy vấn, transaction, nguồn dữ liệu và giao tiếp hiện có.

## Part 3 — Data Interaction

- **Target Data**: user_progress, study_sessions
- **Ownership**: userId của event đến từ worker đã xác thực shared key; không mở ingress cho học viên tự ghi.
- **Denied Request**: không gọi service nghiệp vụ nên không ghi DB/Redis hoặc thay tiến độ.

## Part 4 — Operational Notes

- Source baseline: `f775eaa8a9a4be14d550b1ff1e7831326888f355`; target branch `fix/progress-api-authorization`.
- Bản vá chỉ bổ sung authorization và tài liệu; không thay API request/response, scoring, schema Progress hoặc worker.
- Giữ contract truy cập hiện có của endpoint.
- Không ghi JWT/key vào log hoặc tài liệu. Xem [ma trận quyền](authorization.md).

Nghiệp vụ chi tiết: [canonical_exam_progress.md](canonical_exam_progress.md).
