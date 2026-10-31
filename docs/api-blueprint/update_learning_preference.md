# Update Learning Program Preference

## Part 0 — Classification & Identity

- **API Name**: Update Learning Program Preference
- **Module**: `progress`
- **API Type**: Authenticated user
- **Authorization**: `progress:update`
- **Related Data**: Redis user:<userId>:program

## Part 1 — API Contract

- **HTTP Method**: `PUT`
- **URL**: `/api/v1/progress/preferences/learning-program`
- **Success Status**: `200`

### Headers

| Name | Required | Description |
| --- | --- | --- |
| Authorization | Có | Bearer access token hợp lệ của Identity |
| Content-Type | Có khi có body | application/json |

JWT claim `permissions` phải chứa đúng `progress:update`; authority runtime là `PERM_progress:update`. Role ADMIN/STUDENT không tự bypass permission. Chủ thể lấy từ JWT sub; userId trong query/body/header không chọn được tài khoản khác.

### Request Body

| Name | Type | Validation / Contract |
| --- | --- | --- |
| programCode | String | Không có Bean Validation tại controller/DTO hiện tại; Mã preference lưu Redis; không đồng nhất với selectedLicenseClass của Identity |

Các event nội bộ được service kiểm tra sau khi xác thực shared key. DTO preference chưa có @Valid; bản vá không thêm quy tắc nghiệp vụ mới.

### Response

200 với body rỗng. Client không bắt buộc parse JSON.

### Error Codes

| Error Code | HTTP | Meaning |
| --- | --- | --- |
| Không bắt buộc có errorCode | 401 | JWT thiếu, hết hạn hoặc không xác minh được |
| AUTH-403 | 403 | Đã xác thực nhưng thiếu progress:update tại method security |

Lỗi nghiệp vụ/upstream và shape lỗi giữ theo [contract notes](contract_notes.md). Không hứa mọi lỗi binding có cùng mã; method security chạy khi gọi controller sau bước bind argument.

## Part 2 — Processing Specification

1. **Security Layer**: Giữ xác minh JWT/issuer hiện có, chuẩn hóa role một lần và map permission thành `PERM_<permission>`.
2. **Controller Layer**: Kiểm `hasAuthority('PERM_progress:update')` trước gọi service; lấy userId từ principal.
3. **Service Layer**: `UserPreferenceService.updateLearningProgram`. Lấy userId từ principal, lưu Redis key user:<UUID>:program và publish channel:user_program_events như trước. Không cập nhật selectedLicenseClass của Identity. Trả 200 với body rỗng.
4. **Repository / External Interaction**: giữ truy vấn, transaction, nguồn dữ liệu và giao tiếp hiện có.

## Part 3 — Data Interaction

- **Target Data**: Redis user:<userId>:program
- **Ownership**: Truy vấn/ghi theo userId từ JWT; giữ scope hạng/bank/chương của nghiệp vụ.
- **Denied Request**: không gọi service nghiệp vụ nên không ghi DB/Redis hoặc thay tiến độ.

## Part 4 — Operational Notes

- Source baseline: `f775eaa8a9a4be14d550b1ff1e7831326888f355`; target branch `fix/progress-api-authorization`.
- Bản vá chỉ bổ sung authorization và tài liệu; không thay API request/response, scoring, schema Progress hoặc worker.
- Trước rollout, Identity phải cấp quyền bằng migration và client lấy access token mới. Không tự tạo quyền trong Progress hoặc sửa JWT phía frontend.
- Không ghi JWT/key vào log hoặc tài liệu. Xem [ma trận quyền](authorization.md).

Nghiệp vụ chi tiết: [chapters_preferences.md](chapters_preferences.md).
