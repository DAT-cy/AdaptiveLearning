# BÁO CÁO TIẾN ĐỘ DỰ ÁN ADAPTIVE LEARNING AI

> **Ngày cập nhật:** 27/10/2026  
> **Phiên bản tài liệu:** 1.2  
> **Mục đích:** Theo dõi chi tiết tiến độ thực hiện, trạng thái các tính năng đã hoàn thành, đang triển khai và các bước tiếp theo của hệ thống Học tập Thích ứng (Adaptive Learning).

---

## 1. TỔNG QUAN HỆ THỐNG VÀ KIẾN TRÚC

- **Backend:** Java 17, Spring Boot 3.2, Spring Security + JWT, JPA/Hibernate, Gradle, RESTful APIs (~15 business modules, hơn 160 Java classes).
- **Frontend:** Vue 3 (Composition API), Vite, TypeScript/JavaScript, TailwindCSS + Vanilla CSS, Lucide Icons, Reka UI / Headless primitives.
- **Sidecar / AI / Thuật toán:**
  - Python sidecar: Hỗ trợ mô hình Bayesian Knowledge Tracing (pyBKT) cho đánh giá năng lực học viên.
  - LLM Gateway: Tích hợp AI chấm bài Writing (theo Rubric), Speaking (theo Transcript), AI Tutor đối thoại cá nhân và AI Content Generation (sinh câu hỏi trắc nghiệm).
- **Cơ sở dữ liệu & Triển khai:**
  - MySQL 8.0 chạy trên Docker Compose (`adaptive-learning-mysql`).
  - Backend Docker Compose (`adaptive-learning-backend`).
  - Frontend chạy dev server tại cổng `http://localhost:3000`.

---

## 2. BẢNG TIẾN ĐỘ TỔNG THỂ CÁC PHASE

| Phase | Tên giai đoạn | Trạng thái | Mức hoàn thành | Ghi chú chính |
|---|---|---|---|---|
| **Phase 0** | Chuẩn bị nền tảng & Kiến trúc cốt lõi | **DONE** | 100% | Spring Boot, Gradle, Docker MySQL, JWT, Global Exception Handler. |
| **Phase 1** | Foundation & API Nghiệp vụ MVP | **CODE COMPLETE** | 85% | Quản lý kỳ thi, môn/topic, ngân hàng câu hỏi, luồng thi, nộp bài, tính điểm tự động. Chờ hoàn thành E2E test. |
| **Phase 2** | AI Grading & AI Content Generation | **IMPLEMENTED MVP** | 70% | Đã có pipeline chấm Writing theo Rubric, Speaking transcript, sinh câu hỏi nháp. Cần test với AI model thật. |
| **Phase 3** | AI Tutor, RAG & Đánh giá thích ứng (pyBKT) | **IMPLEMENTED MVP** | 65% | AI Tutor theo ngữ cảnh bài học; RAG keyword search trên tài liệu; pyBKT sidecar đã tích hợp sẵn fallback. |
| **Phase 4** | Study Planner & Phân tích lộ trình | **IMPLEMENTED MVP** | 60% | Lập kế hoạch học tập theo mục tiêu điểm và ngày thi; phân bổ task ôn tập. |
| **Phase 5** | Giao diện Frontend (User & Admin) | **IN PROGRESS** | 80% | Giao diện đăng ký/đăng nhập chuẩn hóa, dashboard User, quản lý kỳ thi, ngân hàng câu hỏi, quản lý gói cước & học viên cho Admin. |
| **Phase 6** | Kiểm thử, Bảo mật & Sẵn sàng Production | **PLANNED** | 25% | Cần bổ sung Unit/Integration tests, Flyway migration, Rate limiting, CORS production. |

---

## 3. CHI TIẾT TRẠNG THÁI TỪNG PHÂN HỆ NGHIỆP VỤ

### 3.1. Phân hệ Xác thực & Quản lý Người dùng (Auth & User)
- [x] **Đăng ký / Đăng nhập tài khoản:** Mã hóa mật khẩu BCrypt, cấp phát JWT token.
- [x] **Đăng ký với Họ tên:** Bổ sung trường nhập Họ và tên đầy đủ (`full_name`), lưu trữ và hiển thị đúng trên trang cá nhân.
- [x] **Đăng nhập Google OAuth2:** Tích hợp Google Identity Services (Client ID cấu hình qua `.env` và `docker-compose.yml`), xử lý xác thực backend và tự động kích hoạt tài khoản.
- [x] **Đồng bộ hóa giao diện Login & Register:** Thiết kế layout 2 cột đồng bộ, responsive, form nhập liệu bo góc hiện đại, validation trực quan.
- [x] **Phân quyền vai trò:** Tách biệt rõ ràng `ROLE_USER` (học viên) và `ROLE_ADMIN` (quản trị viên/biên soạn).

### 3.2. Phân hệ Giao diện & Trải nghiệm Người dùng (UI / UX)
- [x] **Tiêu đề & Favicon Trình duyệt:** Thiết lập động theo từng trang (`Đăng nhập · Adaptive Learning`, `Tạo tài khoản`, v.v.) và biểu tượng SVG sắc nét.
- [x] **Menu xổ xuống tại Avatar (Profile Dropdown):**
  - Chuyển trạng thái sẵn sàng học tập ("Sẵn sàng" / "Bận" với chấm đèn xanh).
  - Công tắc bật/tắt nhận thông báo hệ thống.
  - Xem thông tin cá nhân / hồ sơ cán bộ.
  - Modal đổi mật khẩu trực tiếp.
  - Nút Đăng xuất popup nhanh (không chuyển trang trắng).
- [x] **Hệ thống Toast thông báo (System Notification Popups):**
  - Sử dụng `<Teleport to="body">` với định vị góc trên bên phải (`position: fixed; top: 20px; right: 20px; z-index: 999999`).
  - Không bao giờ bị xô lệch layout hay rớt xuống cuối trang.
  - Tự động đóng sau **2 giây** (`2000ms`), hỗ trợ icon trạng thái và nút tắt nhanh `(X)`.

### 3.3. Phân hệ Quản lý Gói cước & Thanh toán (Billing & Payment)
- [x] **Quản lý danh sách gói cước (Admin):**
  - Xem danh sách các gói cước đang áp dụng (Free, Pro Tháng, Pro Năm, v.v.).
  - Thêm mới gói cước, chỉnh sửa giá bán, thời hạn, giới hạn số lượt thi và trạng thái kích hoạt.
  - Bật/tắt trạng thái gói hoặc xóa gói cước.
- [x] **Quản lý học viên & Hạn mức (Admin):**
  - Tra cứu tài khoản học viên, hiển thị gói cước hiện tại, số lượt thi đã dùng / tổng lượt được cấp.
  - Gán/nâng cấp gói cước trực tiếp cho học viên.
  - Reset số lượt thi về 0 khi cần.
  - Xem lịch sử giao dịch thanh toán chi tiết của từng học viên.
- [x] **Cổng thanh toán & Simulation:**
  - Hỗ trợ mô hình xử lý Stripe và PayPal (idempotency, webhook verification).
  - Tích hợp cổng thanh toán Sandbox Simulation trực tiếp trên web (`/payment/sandbox`): mô phỏng quét VietQR / thẻ thử nghiệm, cho phép test end-to-end luồng checkout, xác nhận giao dịch thành công/hủy và tự động kích hoạt gói Premium ngay lập tức trên môi trường local.
  - Cấu hình giá mặc định `PREMIUM_PRICE=199000 VND` để tính năng thanh toán luôn sẵn sàng hoạt động mà không bị treo ở trạng thái bảo trì khi chưa cấu hình Stripe/PayPal live key.

### 3.4. Phân hệ Ngân hàng Câu hỏi & Tạo Đề (Exam & Question Bank)
- [x] **Quản lý Chứng chỉ & Topic:** Cấu hình kỳ thi (IELTS, TOEIC, THPT), phân cấp cây kỹ năng (Listening, Reading, Writing, Speaking) và chủ đề.
- [x] **Ngân hàng câu hỏi:** Thêm mới, chỉnh sửa, phân loại độ khó, gắn đáp án đúng và lời giải chi tiết.
- [x] **Workflow duyệt câu hỏi:** Quy trình `DRAFT` (Bản nháp) → `IN_REVIEW` (Chờ duyệt) → `APPROVED` (Đã duyệt) → `PUBLISHED` (Phát hành vào đề thi).
- [x] **Bộ tạo đề thi (Exam Builder):** Thiết lập đề thi đơn, cấu hình thời gian làm bài, tổng số câu, tỷ lệ điểm và publish đề thi cho học viên.

### 3.5. Phân hệ Làm bài thi & Chấm điểm (Mock Test Engine)
- [x] **Làm bài thi trực tuyến:** Đồng hồ đếm ngược (`timer`), tự động lưu tiến độ làm bài (`autosave`).
- [x] **Chống gian lận (Anti-cheat):** Ghi nhận vi phạm chuyển tab (`tab switch`), sao chép câu hỏi (`copy`), mất tiêu điểm (`blur`).
- [x] **Chấm điểm tự động:** Tự động tính điểm ngay khi nộp bài cho các phần trắc nghiệm/khách quan.
- [x] **Xử lý hết giờ:** Tự động thu bài (`auto-expire`) khi quá hạn thời gian cho phép.

### 3.6. Phân hệ AI & Học thích ứng (AI & Adaptive Intelligence)
- [x] **Knowledge Base RAG (MVP):** Admin tải lên tài liệu định dạng `.txt`/`.md`, gắn kỳ thi/topic; tìm kiếm trích dẫn từ khóa để phục vụ AI sinh câu hỏi.
- [x] **AI Content Generation (MVP):** Sinh câu hỏi trắc nghiệm dựa trên đoạn trích từ tài liệu tri thức đã nạp, tạo bản nháp để Admin thẩm định.
- [x] **AI Writing Evaluation (MVP):** Tiếp nhận bài luận, phân tích theo tiêu chí Rubric (Task Achievement, Coherence & Cohesion, Lexical Resource, Grammatical Accuracy).
- [x] **AI Speaking Evaluation (MVP):** Đánh giá bài nói dựa trên transcript (từ vựng, ngữ pháp, độ mạch lạc).
- [x] **pyBKT Sidecar Integration:** Kết nối service Python để tính toán xác suất thành thạo kiến thức theo từng skill, tự động fallback về heuristic khi chưa đủ dữ liệu học viên.

---

## 4. CÁC TÍNH NĂNG VỪA HOÀN TẤT TRONG ĐỢT CẬP NHẬT GẦN NHẤT

1. **Chuẩn hóa thông báo lỗi dạng Popup:**
   - Sửa lỗi component Toast bị rơi xuống chân trang (dưới hình minh họa).
   - Chuyển toàn bộ sang `<Teleport to="body">` với tọa độ cố định góc trên bên phải màn hình.
   - Thiết lập thời gian hiển thị và tự biến mất về mức **2 giây**.
2. **Nâng cấp Menu Profile Avatar:**
   - Dropdown chuẩn theo thiết kế Figma/mockup: chấm trạng thái sẵn sàng, toggle thông báo, đổi mật khẩu và đăng xuất dạng popup không giật trang.
3. **Đồng bộ hóa màn hình Đăng ký / Đăng nhập:**
   - Thêm trường nhập Họ và tên đầy đủ ở form đăng ký.
   - Thống nhất giao diện 2 cột với banner minh họa chuyên nghiệp.
   - Đảm bảo Google Sign-In hoạt động thông suốt.
4. **Trang Quản lý Gói cước & Thanh toán:**
   - Bổ sung tab quản trị gói dịch vụ, gán gói cước và theo dõi lịch sử thanh toán học viên.

---

## 5. KẾ HOẠCH VÀ ƯU TIÊN TIẾP THEO

- [ ] **Kiểm thử E2E trọn vẹn luồng học viên:**
  - Học viên đăng ký → Làm bài thi thử → Hệ thống tự chấm → Phân tích lỗ hổng kiến thức (`Gap Analysis`) → Sinh đề luyện tập cá nhân hóa.
- [ ] **Tích hợp Model AI Production:**
  - Cấu hình khóa `AI_API_KEY` chính thức (OpenAI / Gemini / Claude) để chạy thử nghiệm chấm Writing thật và sinh câu hỏi theo thời gian thực.
- [ ] **Mở rộng RAG & Ngân hàng tri thức:**
  - Hỗ trợ thêm parser cho tài liệu `.pdf` và `.docx`.
  - Nghiên cứu tích hợp vector database (pgvector / Chroma) thay thế cho trích xuất từ khóa đơn thuần.
- [ ] **Kiểm thử và Tối ưu mã nguồn (Phase 6):**
  - Viết bộ Unit test cho scoring engine và state transition.
  - Bổ sung database migration với Flyway để cố định schema.
