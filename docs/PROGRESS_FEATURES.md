# Chức năng và nghiệp vụ DTS Progress

## 1. Phạm vi dữ liệu và cấu trúc

Progress sở hữu tổng hợp tiến độ, phiên học và projection học câu hỏi. Practice sở hữu câu trả lời/chấm điểm/receipt và catalog ngân hàng; Identity sở hữu user/GPLX/JWT; Result sở hữu lịch sử kết quả thống nhất/leaderboard/analytics; Examination sở hữu thi chính thức và công bố kết quả. Progress không tự chấm lại bài Practice hoặc sửa các record lịch sử trong bước chuẩn bị Git.

| Nhóm source | Chức năng |
| --- | --- |
| controller + dto | API dashboard/chapter/streak/session/overview/preference, hai ingress |
| ProgressService | Dashboard, accumulator chapter, session legacy và chuyển nhánh canonical |
| CanonicalExamProgressService | Upsert phiên Practice theo user/exam, cập nhật delta tổng hợp |
| QuestionLearningProjectionService | Inbox dedup eventId và trạng thái từng câu theo dimension/sequence |
| LearningOverviewService | Kết hợp projection với catalog Practice hiện tại |
| PracticeLearningBankClient / PracticeProgressReceiptClient | HTTP tới Practice, forward Bearer, timeout và mapping lỗi |
| UserEventConsumer | Kafka user-events từ Identity |
| security / config | JWT, stateless security, Redis, Kafka, trace |
| resources/db/migration | V1 bảng legacy, V2 uniqueness, V3 projection/inbox |

## 2. User progress và dashboard

USER_CREATED tạo row user_progress nếu chưa có; USER_UPDATED cập nhật username hoặc tạo khi thiếu; USER_DELETED xóa row user_progress theo consumer. Không tự khẳng định consumer xóa toàn bộ chapter/session/projection của user vì code hiện chỉ thao tác repository UserProgress.

Dashboard trả tổng exams/practice/time/question/correct từ user_progress; averageScore nullable. Pass/fail/rate được tính từ study_sessions status COMPLETED và sessionType EXAM, không bao gồm TIMEOUT trong phép đếm pass/fail đó. Không có completed EXAM thì passRate=null, không tự biến thành0. Chỉ tiêu chapter của dashboard dựa chapter_progress legacy, chưa thay bằng user_question_progress.

## 3. Tiến độ chương legacy và preference

PATCH chapters nhận chapterId/name, questionsTotal, số câu của lần ghi và số đúng/sai. Service cộng dồn questionsAnswered rồi chặn ở total, correct chặn ở answered, score tính correct/total; status bắt đầu IN_PROGRESS và hoàn thành khi answered đạt total. Đây là accumulator theo lần ghi, không phải số câu duy nhất từ projection mới; không phát lại tùy ý để làm mới UI.

PUT preferences/learning-program lưu programCode vào Redis key user:<userId>:program và publish channel:user_program_events. Luồng này không tự cập nhật Identity.selectedLicenseClass. Client chọn GPLX của hồ sơ dùng contract Identity, không suy ra preference Redis là cùng một nguồn dữ liệu.

## 4. Phiên học, thi và canonical progress

POST sessions có examId sẽ lấy receipt từ Practice bằng Bearer của chủ thể, dựng EXAM_FINISHED event chuẩn rồi đi qua CanonicalExamProgressService. Những score/count/timestamp do client gửi không là nguồn grade cho nhánh này, nhưng body vẫn phải qua Bean Validation LogStudySessionRequest.

Ingress nội bộ exam-results nhận cùng DTO event qua shared key. Service kiểm tra eventId=examId, source/schema/eventType, mode EXAM/PRACTICE, status COMPLETED/TIMEOUT, số câu/điểm/thời gian hợp lệ; khóa row user_progress của user và tìm study_sessions theo (userId,examId). Nếu cùng giá trị đã lưu thì trả row cũ. Nếu dữ liệu thay đổi, upsert session và điều chỉnh tổng bằng chênh lệch đóng góp cũ/mới, không cộng nguyên lần nữa.

Average score canonical là AVG(score) của phiên COMPLETED/TIMEOUT có sessionType EXAM/PRACTICE. Streak canonical lấy các ngày hoàn tất theo Asia/Ho_Chi_Minh, quét các ngày liên tiếp để cập nhật streak/longest; không tính điểm hay pass threshold lại trong Progress. V2 unique index hỗ trợ một session cho mỗi user/examId không null.

POST sessions không có examId giữ nhánh legacy: tạo session mới, grade theo StudySession.finish hiện có, cộng tổng và recordStudy ngày hiện tại. Nhánh này không có cùng bảo đảm idempotency. Threshold correct>=21 của hàm finish legacy không phải quy tắc GPLX chung để áp vào bài canonical hoặc frontend.

## 5. Projection học câu hỏi

Practice gửi QUESTION_PROGRESS_CHANGED với eventId, eventSequence và dimension CHAPTER/CRITICAL/REVIEW. Progress ghi practice_learning_event_inbox trước; eventId đã có thì kết thúc không áp lại. Projection key là (userId,licenseClass,bankVersion,questionId), lưu chapterId, isCritical và ba trạng thái chapterLearned/criticalLearned/needsReview.

Mỗi dimension có version riêng. Trạng thái và timestamp của dimension chỉ cập nhật khi sequence mới cao hơn version đang có, giữ các dimension khác. Metadata chapterId/isCritical được cập nhật theo phần SQL hiện có; không mô tả event cũ là tuyệt đối không ảnh hưởng mọi cột. Inbox và upsert nằm cùng transaction.

Học câu hỏi theo projection không tự tăng tổng exam, không tạo study session và không thay chapter_progress legacy. Event dimension CHAPTER/CRITICAL yêu cầu learned boolean và needsReview=null; REVIEW yêu cầu needsReview boolean và learned=null. Invalid event bị từ chối400.

## 6. Learning overview

GET learning-overview lấy user từ JWT, chuẩn hóa licenseClass bằng trim/uppercase và đọc catalog Practice qua Bearer. Catalog quyết định bankVersion, tổng câu, tổng critical và chapter metadata. SQL chỉ đọc projection cùng user/hạng/bankVersion hiện tại.

Learned là số row chapter_learned OR critical_learned, một câu không bị đếm hai lần vì học qua hai dimension. Need review là needs_review; critical studied yêu cầu is_critical và đã learned qua ít nhất một dimension. Tính theo chapter, chỉ đưa chương có totalQuestions>0 từ catalog vào response.

Percent là integer Math.round(100f*count/total), total0 thì0. Status NOT_STARTED khi count0 hoặc total0, COMPLETED khi count=total, còn lại IN_PROGRESS. Nếu catalog thiếu tên chương hoặc số projection vượt total canonical tương ứng thì409, không im lặng clamp để che dữ liệu lệch. Frontend dùng số server trả, không lấy tổng câu/hạng từ fixture hoặc bảng hard-code.

## 7. Xác thực, tích hợp và lỗi

API công khai yêu cầu Bearer; userId lấy JwtUserDetails từ sub JWT. JwtValidationFilter kiểm chữ ký với Base64 access secret và issuer; không nhận header X-User-Id thay token. Hai POST internal được permitAll ở security chain nhưng controller bắt buộc X-DTS-Internal-Key không rỗng và constant-time compare. Không mở chúng qua proxy public để client tự ghi progress.

Practice catalog/receipt client có connect timeout2 giây, read timeout5 giây; lỗi upstream được map sang 400/403/404/409/502 theo từng client. ApiResponse có field null có thể bị lược do JsonInclude.NON_NULL, trong khi security handler ghi data:null; preference trả200 body rỗng. Xem contract_notes cho cách xử lý.

## 8. Dữ liệu môi trường và thay đổi tiếp theo

Git mang theo source/migration/template; credential, user data, receipt và tiến độ thật thuộc môi trường. Đợt chuẩn bị Git không chạy database, Kafka, Redis, API hay build, không sửa logic/streak/grade/constraints. Mọi schema thay đổi tiếp theo tạo version migration mới tại repo sở hữu dữ liệu.

## Authorization API người dùng

Bản bổ sung phân quyền map role/permission từ JWT sang authority, áp progress:read cho 6 API GET và progress:update cho 3 API ghi người dùng. Hai ingress nội bộ giữ shared key. Không thay công thức, DTO hoặc schema Progress. [Ma trận quyền](api-blueprint/authorization.md) và blueprint từng endpoint mô tả contract hiện tại.
