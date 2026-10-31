# Receive Practice Question Progress

## Part 0 — Classification & Identity

- **API Name**: Receive Practice Question Progress
- **Module**: `progress`
- **API Type**: Internal service to service
- **Authorization**: `Khóa service nội bộ`
- **Related Data**: practice_learning_event_inbox, user_question_progress

## Part 1 — API Contract

- **HTTP Method**: `POST`
- **URL**: `/internal/v1/practice/question-progress`
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
| eventSequence | Long | Số thứ tự sự kiện để cập nhật dimension projection đúng thứ tự |
| userId | UUID | UUID chủ dữ liệu trong event từ worker đã xác thực; không phải query chọn user của API học viên |
| licenseClass | String | Mã hạng GPLX; catalog Practice quyết định dữ liệu hợp lệ |
| bankVersion | String | Phiên bản ngân hàng dùng để giới hạn projection |
| questionId | Integer | ID câu hỏi trong ngân hàng Practice |
| chapterId | Integer | Định danh chương trong ngân hàng đang dùng |
| critical | Boolean | Cờ critical trong event; trong overview là nhóm thống kê critical theo DTO |
| dimension | String | CHAPTER, CRITICAL hoặc REVIEW theo contract projection |
| learned | Boolean | Trạng thái đã học của dimension học; theo validator nguồn |
| needsReview | Boolean | Trạng thái cần ôn của dimension REVIEW |
| studiedAt | Instant | Thời điểm học theo sự kiện nguồn |

Các event nội bộ được service kiểm tra sau khi xác thực shared key. DTO preference chưa có @Valid; bản vá không thêm quy tắc nghiệp vụ mới.

### Response

200 `ApiResponse` với `data.accepted=true`.

### Error Codes

| Error Code | HTTP | Meaning |
| --- | --- | --- |
| INTERNAL_AUTH_REQUIRED | 403 | Key thiếu, sai hoặc cấu hình rỗng |
| BAD_REQUEST | 400 | JSON/event không hợp lệ theo nhánh hiện có |

Lỗi nghiệp vụ/upstream và shape lỗi giữ theo [contract notes](contract_notes.md). Không hứa mọi lỗi binding có cùng mã; method security chạy khi gọi controller sau bước bind argument.

## Part 2 — Processing Specification

1. **Security Layer**: Cho qua JWT chain tại đúng hai POST nội bộ; controller bắt buộc kiểm shared key.
2. **Controller Layer**: Kiểm khóa trước parse event; không đòi permission học viên.
3. **Service Layer**: `QuestionLearningProjectionService.apply`. Kiểm shared key không rỗng bằng constant-time compare trước parse JSON. Projection service kiểm eventId/sequence/dimension, ghi inbox và trạng thái câu trong cùng transaction. Giữ chống trùng và thứ tự sự kiện.
4. **Repository / External Interaction**: giữ truy vấn, transaction, nguồn dữ liệu và giao tiếp hiện có.

## Part 3 — Data Interaction

- **Target Data**: practice_learning_event_inbox, user_question_progress
- **Ownership**: userId của event đến từ worker đã xác thực shared key; không mở ingress cho học viên tự ghi.
- **Denied Request**: không gọi service nghiệp vụ nên không ghi DB/Redis hoặc thay tiến độ.

## Part 4 — Operational Notes

- Source baseline: `f775eaa8a9a4be14d550b1ff1e7831326888f355`; target branch `fix/progress-api-authorization`.
- Bản vá chỉ bổ sung authorization và tài liệu; không thay API request/response, scoring, schema Progress hoặc worker.
- Giữ contract truy cập hiện có của endpoint.
- Không ghi JWT/key vào log hoặc tài liệu. Xem [ma trận quyền](authorization.md).

Nghiệp vụ chi tiết: [question_progress_ingress.md](question_progress_ingress.md).
