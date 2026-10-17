# API Blueprint: Question progress ingress

## Endpoint / xác thực

POST /internal/v1/practice/question-progress, gọi trực tiếp Progress bởi worker Practice. Header X-DTS-Internal-Key khớp progress.internal-key (EXAM_PROGRESS_INTERNAL_KEY trong cấu hình). Chỉ POST này và exam-results được permitAll ở security chain để controller kiểm tra shared key; không có Bearer không đồng nghĩa ai cũng được ghi.

## Event DTO

```json
{
  "eventId": "22222222-2222-4222-8222-222222222222",
  "eventType": "QUESTION_PROGRESS_CHANGED",
  "schemaVersion": 1,
  "source": "dts-practice",
  "eventSequence": 1,
  "userId": "33333333-3333-4333-8333-333333333333",
  "licenseClass": "A1",
  "bankVersion": "example-bank",
  "questionId": 1,
  "chapterId": 1,
  "critical": false,
  "dimension": "CHAPTER",
  "learned": true,
  "needsReview": null,
  "studiedAt": "2026-10-18T00:00:00Z"
}
```

Ví dụ chỉ mô tả shape, không phải lệnh tạo dữ liệu thử. EventId/userId bắt buộc; sequence>0; license/bankVersion không trắng; questionId/chapterId>0; critical/studiedAt bắt buộc. CHAPTER/CRITICAL yêu cầu learned và needsReview=null; REVIEW yêu cầu needsReview và learned=null. Source/schema/eventType phải đúng ví dụ.

## Transaction, replay và thứ tự

1. Validate event; insert practice_learning_event_inbox ON CONFLICT(event_id) DO NOTHING.
2. Đã có eventId thì return, không áp lại projection.
3. Upsert user_question_progress theo user/license/bankVersion/questionId.
4. CHAPTER, CRITICAL, REVIEW có version riêng; chỉ cập nhật trạng thái/timestamp liên quan khi sequence của dimension mới cao hơn version đang lưu. Không ghi đè dimension khác bằng giá trị false/null của event không thuộc dimension đó.
5. Metadata chapterId/isCritical được cập nhật theo SQL hiện tại; không tuyên bố mọi cột miễn thay đổi trước event cũ. Ghi inbox và projection cùng transaction.

Projection không tự tạo study session/tăng exam counter hoặc sửa chapter_progress legacy. Không trộn event bankVersion khác vào cùng overview; overview truy vấn bankVersion của catalog hiện tại.

## Response / lỗi

200 ApiResponse có data={"accepted":true}, kể cả eventId đã xử lý. accepted không có nghĩa frontend vừa tạo một session mới.

403 INTERNAL_AUTH_REQUIRED khi key thiếu/rỗng/sai. 400 BAD_REQUEST khi JSON hoặc validation event sai. Lỗi DB ngoài mapping có thể500 SYS-500; không che lỗi bằng accepted=true trước khi transaction ghi xong.

Nguồn: [Ingress controller](../../src/main/java/com/dts/progress/controller/PracticeQuestionProgressIngressController.java), [Projection service](../../src/main/java/com/dts/progress/service/QuestionLearningProjectionService.java), [Event DTO](../../src/main/java/com/dts/progress/dto/event/PracticeQuestionProgressEvent.java), [V3](../../src/main/resources/db/migration/V3__question_learning_projection.sql).
