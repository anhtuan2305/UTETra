# UTETra — Bản nâng cấp để mở bằng Spring Tool Suite

Đọc **HUONG_DAN_CHAY_STS.md** trước. Java 17, Maven Wrapper, Spring Boot 3.2.5, Thymeleaf, JPA, Spring Security.

## Trạng thái kiểm chứng — đọc rõ trước khi sử dụng

- Đã chạy 32 kiểm tra lõi độc lập bằng Java 17: chuyển trạng thái, số lượng/tồn kho, giá size/topping và chữ ký thanh toán.
- Đã phân tích cú pháp tất cả file Java, kiểm tra cấu trúc HTML, kiểm tra cú pháp JavaScript.
- **Chưa build/khởi động toàn bộ Spring Boot hoặc chạy JUnit tích hợp trong môi trường tạo ZIP**, vì không truy cập được Maven Central. Đây là bản nguồn đã nâng cấp, cần chạy `mvnw.cmd clean test` và kiểm thử trên STS trước khi dùng làm bản nộp cuối.
- SMTP, Cloudinary, VNPAY phải dùng cấu hình của nhóm và kiểm thử với dịch vụ thật/sandbox. Không có khóa bí mật đi kèm.

## Chạy nhanh

1. Giải nén vào thư mục riêng.
2. STS → File → Import → Maven → Existing Maven Projects → chọn thư mục có `pom.xml`.
3. Chọn JDK 17, Maven → Update Project.
4. Run As → Spring Boot App (`vn.tuan.UteTraApplication`).
5. Mở http://localhost:8080. Mặc định profile `demo`, H2 lưu dữ liệu trong `data/`.

Tài khoản mẫu (chỉ được tạo trong profile demo hoặc khi bật seed):

| Vai trò | Email | Mật khẩu |
|---|---|---|
| Admin | admin@utetra.vn | Utetra@123 |
| User | user@utetra.vn | Utetra@123 |
| Vendor Thủ Đức | shop1@utetra.vn | Utetra@123 |
| Vendor Bình Thạnh | shop2@utetra.vn | Utetra@123 |
| Manager Thủ Đức | manager@utetra.vn | Utetra@123 |
| Shipper | shipper1@utetra.vn | Utetra@123 |
| Shipper thứ hai | shipper2@utetra.vn | Utetra@123 |

Các đơn và tài khoản đã có trong database cũ không nằm trong ZIP nguồn. Không chỉ thay cấu hình để trỏ ngay vào DB cũ; đọc hướng dẫn database mới.

## Tài liệu trong ZIP

- HUONG_DAN_CHAY_STS.md: chạy demo, SQL Server, SMTP, Cloudinary, VNPAY, JWT.
- DOI_CHIEU_YEU_CAU.md: các chức năng và phạm vi quyền.
- KIEM_THU.md: kết quả đã chạy và checklist cần chạy trên máy nhóm.
- CHANGELOG.md: thay đổi chính so với bản ZIP người dùng gửi.
- database/create-database.sql: tạo database SQL Server riêng, không xóa DB cũ.
- src/test: bộ kiểm thử tích hợp và bảo mật để chạy bằng Maven.

## Thiết kế nghiệp vụ

Một giỏ/đơn thuộc một chi nhánh; mỗi chi nhánh quản lý thực đơn và tồn kho riêng. Dùng bộ lọc chi nhánh trước khi chọn món. Không đổi chi nhánh khi thanh toán mà trừ kho của quán khác.

NEW → CONFIRMED → PREPARING → READY → SHIPPING → COMPLETED.

User hủy NEW; quán hủy NEW/CONFIRMED và hoàn tồn kho. Shipper chỉ nhận READY chưa có người nhận; chỉ Shipper đã nhận mới được hoàn tất. Trả hàng trong 7 ngày: RETURN_REQUESTED → RETURN_APPROVED → REFUNDED. Đồ uống trả lại **không** cộng lại kho bán được. Hoàn tiền là xác nhận nghiệp vụ sau khi chuyển tiền thực tế; không giả lập một giao dịch hoàn tiền ngân hàng thành công.

Cổng web dùng session + CSRF. API `/api/**` dùng JWT Bearer stateless, token 15 phút; đây là hai kênh xác thực riêng, không lưu JWT trong localStorage cho web.
