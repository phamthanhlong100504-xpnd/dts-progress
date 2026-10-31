# Progress — quy ước contract và nguồn dữ liệu

## Xác thực / chủ thể

API public lấy JwtUserDetails từ JWT sub UUID; JwtValidationFilter kiểm chữ ký bằng BASE64 access secret và issuer trong cấu hình. Không lấy userId từ query/body/header X-User-Id để chọn chủ thể. Role được chuẩn hóa một lần: ROLE_STUDENT giữ nguyên, STUDENT thành ROLE_STUDENT. Permission tên thuần từ JWT được map thành PERM_<permission>; không suy quyền từ role và không cấp mặc định. API đọc cần progress:read; ba API ghi người dùng cần progress:update. Xem [authorization](authorization.md).

Security chain cho public home/health/info/docs/OPTIONS, hai internal POST chính xác được permitAll để controller kiểm shared key. Các request còn lại authenticated; 9 method nghiệp vụ người dùng còn kiểm permission cụ thể bằng @PreAuthorize. Internal controller dùng X-DTS-Internal-Key, constant-time compare; key cấu hình rỗng cũng bị từ chối. Không coi permitAll là API tự do ghi progress.

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
| Thiếu permission tại method security | 403 AUTH-403 |
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

## Authorization cập nhật ngày 31 tháng 10 năm 2026

Các API người dùng trong trang này phải có quyền tương ứng trong [ma trận authorization](authorization.md): GET dùng `progress:read`; PATCH chapters, POST sessions và PUT preference dùng `progress:update`. Hai POST ingress vẫn dùng shared key riêng, không dùng permission học viên. Giữ ownership, DTO và nghiệp vụ đã mô tả. Blueprint từng endpoint trong [mục lục](README.md) bổ sung Part 0–4.
