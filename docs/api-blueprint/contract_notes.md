# Progress — quy ước contract và nguồn dữ liệu

## Xác thực / chủ thể

API public lấy JwtUserDetails từ JWT sub UUID; JwtValidationFilter kiểm chữ ký bằng BASE64 access secret và issuer trong cấu hình. Không lấy userId từ query/body/header X-User-Id để chọn chủ thể. Các claim roles/permissions được đọc theo code hiện có; không sửa cách map authority trong đợt Git.

Security chain cho public home/health/info/docs/OPTIONS, hai internal POST chính xác được permitAll để controller kiểm shared key. Các request còn lại authenticated. Internal controller dùng X-DTS-Internal-Key, constant-time compare; key cấu hình rỗng cũng bị từ chối. Không coi permitAll là API tự do ghi progress.

## Wrapper / lỗi

ApiResponse có success, message, data, errorCode, traceId, timestamp và @JsonInclude.NON_NULL: field null của wrapper có thể bị bỏ. Security handler ghi Map có data:null. Endpoint preferences trả body rỗng. Client kiểm tra HTTP/content trước khi parse và không giả định mọi lỗi có đủ field.

| Trường hợp | HTTP / errorCode |
| --- | --- |
| Bean Validation | 400 VAL-400 |
| JSON không đọc được | 400 REQ-400 |
| BusinessException.badRequest | 400 BAD_REQUEST |
| forbidden / notFound / conflict | 403 FORBIDDEN /404 NOT_FOUND /409 CONFLICT |
| Shared key sai/thiếu | 403 INTERNAL_AUTH_REQUIRED |
| Catalog/receipt không đúng shape | 502 PRACTICE_RESPONSE |
| Practice không truy cập được theo client mapping | 502 PRACTICE_UNAVAILABLE |
| Chưa xác thực tại SecurityConfig | 401, body Map không bắt buộc errorCode |
| Ngoài các mapping đã có | 500 SYS-500 |

Các client Practice có mapping riêng cho từng HTTP upstream, xem blueprint tương ứng. Không hứa mọi lỗi parameter/binding/constraint hoặc unique DB đều có mapping400/409 nếu source chưa khai báo handler đó.

## Ba nguồn số liệu

- user_progress/chapter_progress/study_sessions: dashboard, chương legacy, streak, phiên học.
- user_question_progress + catalog Practice: learning overview theo hạng/bankVersion, số câu learned duy nhất.
- dts-result: lịch sử kết quả thống nhất, leaderboard/EXP và analytics; không nằm trong Progress.

Không thay số projection bằng chapter accumulator, không dùng history Progress như result detail, không recompute grade/total từ fixture frontend. selectedLicenseClass hồ sơ thuộc Identity, programCode Redis preference là luồng khác.

## Giao tiếp sự kiện

Progress có Kafka consumer user-events từ Identity. EXAM_FINISHED/QUESTION_PROGRESS_CHANGED của Practice được nhận bằng HTTP ingress và shared key trong source mới; không thêm Kafka consumer cho hai event đó ở repo này. Có inbox eventId cho question projection, không có bằng chứng một outbox mới của Progress để tự phát lại mọi event.

## Phạm vi bàn giao Git

24 file chức năng local đã có được giữ nguyên, gồm YAML thêm practice.service-url/progress.internal-key, hai SQL V2/V3 và test đã có. Template environment mới không tự nạp và không làm đổi service đang chạy. Không chạy migration, gọi account/API, reset dữ liệu hoặc sửa ba phiên TIMEOUT lịch sử đã được giữ nguyên.

Nguồn: [SecurityConfig](../../src/main/java/com/dts/progress/config/SecurityConfig.java), [GlobalExceptionHandler](../../src/main/java/com/dts/progress/exception/GlobalExceptionHandler.java), [ApiResponse](../../src/main/java/com/dts/progress/dto/response/ApiResponse.java), [UserEventConsumer](../../src/main/java/com/dts/progress/messaging/UserEventConsumer.java).
