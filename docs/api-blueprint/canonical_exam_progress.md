# API Blueprint: Canonical exam progress

## Hai điểm vào cùng nghiệp vụ

| Điểm vào | Xác thực | Thành công |
| --- | --- | --- |
| POST /api/v1/progress/sessions có examId | Bearer của chủ thể, forward sang Practice | 201 ApiResponse<SessionHistoryResponse> |
| POST /internal/v1/practice/exam-results | X-DTS-Internal-Key theo shared key cấu hình | 200 cùng kiểu data |

Session công khai lấy userId từ JWT, không từ body. Ingress nội bộ nhận userId từ event của worker tin cậy; không phải API frontend.

## POST sessions có examId

LogStudySessionRequest vẫn bắt buộc sessionType, questionsCount>=1, correctCount/wrongCount/durationSeconds>=0; examType/mode optional theo DTO. Ví dụ:

```json
{
  "sessionType": "EXAM",
  "examType": "A1",
  "mode": "EXAM",
  "examId": "11111111-1111-4111-8111-111111111111",
  "questionsCount": 25,
  "correctCount": 0,
  "wrongCount": 25,
  "durationSeconds": 0
}
```

Các count minh họa không là nguồn điểm: nếu examId khác null, Progress gọi GET /api/v1/exams/{examId}/progress ở Practice bằng Bearer, kiểm tra envelope/examId rồi dựng EXAM_FINISHED từ receipt. Client vẫn cần gửi field hợp lệ để qua Bean Validation. Không dùng Progress để finish bài chưa kết thúc.

Practice400/409 →409 CONFLICT, 401/403 →403 FORBIDDEN, 404 →404 NOT_FOUND, lỗi mạng/upstream khác →502 PRACTICE_UNAVAILABLE, receipt shape sai →502 PRACTICE_RESPONSE.

## Internal event

Header X-DTS-Internal-Key phải khớp progress.internal-key, body JSON PracticeExamFinishedEvent gồm eventId, eventType, schemaVersion, source, userId, examId, licenseClass, bankVersion, mode, status, totalQuestions, correctCount, wrongCountLegacy, answeredWrongCount, unansweredCount, score, passed, durationSeconds, startedAt, completedAt.

Điều kiện service: eventId=examId, eventType=EXAM_FINISHED, schemaVersion=1, source=dts-practice, mode EXAM/PRACTICE, status COMPLETED/TIMEOUT, tổng câu>0, count/time không âm, score0–100, passed có giá trị, completedAt>=startedAt. correct+answeredWrong+unanswered=total và correct+wrongCountLegacy=total. bankVersion nullable, nếu có thì không trắng. Grade lấy từ receipt/event canonical; Progress không tự áp lại passing threshold.

## Upsert và idempotency

Tạo user_progress nếu thiếu, khóa row user theo FOR UPDATE, tìm StudySession bằng userId/examId. Cùng giá trị đã lưu trả row cũ. Trường hợp mới/thay đổi lưu session, tính đóng góp trước đó của phiên COMPLETED/TIMEOUT và cập nhật tổng bằng delta. V2 unique index ngăn trùng user/examId không null.

Average là AVG(score) các phiên COMPLETED/TIMEOUT EXAM/PRACTICE. Cập nhật streak từ các ngày hoàn tất Asia/Ho_Chi_Minh. Không áp dụng cách cộng session legacy thêm lần nữa cho cùng bài khi worker và frontend cũ cùng gửi.

Response data: id (ID row Progress), examId (ID nguồn Practice), sessionType, examType, mode, status, questionsCount, correctCount, wrongCount, score, passed, durationSeconds, startedAt, completedAt. wrongCount ở đây là wrongCountLegacy, gồm cả chưa trả lời; không đổi nghĩa thành answeredWrongCount.

## Nhánh không có examId

Giữ logic legacy tạo session và cộng tổng cho mỗi POST; không có idempotency theo examId, không tự kiểm chứng grade với Practice. Không gửi lại nhánh này để refresh dữ liệu. Xem dashboard_history_streak và source trước khi dùng cho nghiệp vụ mới.

Key sai/rỗng →403 INTERNAL_AUTH_REQUIRED; JSON event sai →400 BAD_REQUEST; event vi phạm canonical validation →400 BAD_REQUEST. Không gắn internal key thật vào tài liệu hoặc gọi ingress bằng account người dùng để thử ghi dữ liệu khi chuẩn bị Git.

Nguồn: [ProgressService](../../src/main/java/com/dts/progress/service/ProgressService.java), [CanonicalExamProgressService](../../src/main/java/com/dts/progress/service/CanonicalExamProgressService.java), [Receipt client](../../src/main/java/com/dts/progress/service/PracticeProgressReceiptClient.java), [Ingress](../../src/main/java/com/dts/progress/controller/PracticeExamProgressIngressController.java).

## Authorization cập nhật ngày 31 tháng 10 năm 2026

Các API người dùng trong trang này phải có quyền tương ứng trong [ma trận authorization](authorization.md): GET dùng `progress:read`; PATCH chapters, POST sessions và PUT preference dùng `progress:update`. Hai POST ingress vẫn dùng shared key riêng, không dùng permission học viên. Giữ ownership, DTO và nghiệp vụ đã mô tả. Blueprint từng endpoint trong [mục lục](README.md) bổ sung Part 0–4.
