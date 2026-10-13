# Adaptive Learning - Front-end Design

Tài liệu này mô tả giao diện người học và quy ước triển khai trong `AdaptiveLearningFE`. Cấu trúc và cách chạy tuân theo [README.md](README.md): Vue 3, Vite, chia tính năng trong `src/features`, layout dùng chung, router điều hướng, và service tập trung các yêu cầu API.

## Mục tiêu giao diện

Giao diện tiếng Việt cho người học, lấy bố cục dashboard trong mẫu tham khảo làm chuẩn: thanh điều hướng dọc, thanh công cụ phía trên, nội dung theo từng màn hình, nền sáng, màu xanh dương làm màu hành động và các màu phụ riêng cho trạng thái/kỹ năng. Ưu tiên khả năng quét nhanh thông tin, tương phản rõ, khoảng cách nhất quán và dùng tốt trên màn hình nhỏ.

## Các màn hình

| Màn hình | Nội dung và hành động chính |
| --- | --- |
| Trang chủ | Lời chào, tóm tắt học tập, lộ trình đang học, kỹ năng và gợi ý tiếp theo |
| Lộ trình học | Danh sách kế hoạch, timeline ba mốc trạng thái, tiến độ từng kế hoạch và tạo lộ trình mới |
| Khóa học | Thẻ chứng chỉ/chương trình học, tỷ lệ hoàn thành dựa trên đề đã nộp, tìm kiếm và chuyển tới đề luyện tập |
| Bài kiểm tra | Thẻ đề đã xuất bản, lịch sử, làm bài có đếm giờ, lưu đáp án và nộp bài |
| Thống kê tiến độ | Tóm tắt điểm và lượt thi, biểu đồ cột từ lịch sử bài đã nộp, mức độ thành thạo theo chứng chỉ |
| Trợ lý AI | Danh sách hội thoại, gợi ý tính năng, tạo hội thoại và gửi câu hỏi cho gia sư AI |

Các màn được triển khai trong `src/features/Home/pages/Home.vue` và có URL riêng đăng ký tại `src/router/Home/home.js`: `/home`, `/plans`, `/courses`, `/tests`, `/progress` và `/tutor` (ngoài ra có `/history` và `/settings`). Sidebar điều hướng qua Vue Router nên có thể mở trực tiếp từng màn. `src/layouts/Default.vue` dành cho layout dùng chung; trang học tập tự dựng shell dashboard theo mẫu tham khảo.

## Quy ước triển khai

- Giữ route, trang, service và state theo các thư mục trong README gốc; không gọi Axios trực tiếp trong component.
- Khai báo endpoint trong `src/services/learningService.js` hoặc `src/services/userService.js`.
- Dùng `lucide-vue-next` cho biểu tượng, văn bản hiển thị bằng tiếng Việt và trạng thái tải/lỗi/rỗng phải có nội dung rõ ràng.
- Giữ các breakpoint responsive của dashboard; trên mobile, sidebar chuyển thành menu đóng/mở và các cột nội dung xếp dọc.
- API trả về wrapper `DefaultRes` của backend. Interceptor trong `src/services/Axios.js` xác nhận `code`/`statusCode`, sau đó service lấy phần `data`.

## Kết nối backend

Tạo `.env` trong thư mục `AdaptiveLearningFE` với URL gốc của backend:

```dotenv
VITE_API_KEY=http://localhost:9090
```

Backend hiện cấu hình cổng `9090` trong `Adaptive Learning/src/main/resources/application.yml`. Vite chỉ nạp biến có tiền tố `VITE_`; các biến này được đóng gói vào mã trình duyệt và không phải nơi lưu bí mật. `.env` đang được giữ ngoài `.gitignore` theo yêu cầu của dự án, vì vậy chỉ commit URL không nhạy cảm; không đưa mật khẩu, JWT secret, API key riêng tư hoặc thông tin xác thực vào file này. Sau khi đổi env cần khởi động lại Vite.

`env.production` cũng trỏ tới `http://localhost:9090` để khớp cấu hình backend local. Khi triển khai, thay giá trị bằng URL backend có thể truy cập từ trình duyệt người dùng.

## Chạy và kiểm tra

Từ thư mục `AdaptiveLearningFE`:

```sh
npm install
npm run dev
npm run build
```

Ứng dụng cần backend hoạt động và người dùng đã đăng nhập để tải dữ liệu được bảo vệ. API hiện được dùng gồm `/api/v1/auth`, `/certificates`, `/exams`, `/questions`, `/attempts`, `/plans`, `/analytics` và `/tutor` (đều nằm dưới `/api/v1`).
