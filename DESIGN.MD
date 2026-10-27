# Adaptive Learning — Hướng dẫn thiết kế giao diện

> Tài liệu này mô tả hệ thống thiết kế (design system) cho trang quản trị **Adaptive Learning** — nền tảng học & luyện thi online cho nhiều kỳ thi.
> Mọi màn hình mới hoặc màn hình được làm lại **phải** dùng đúng các giá trị trong file này. Không tự chọn màu, font, bo góc hay khoảng cách khác.

---

## 0. Cách dùng file này với AI

Đưa file này cho công cụ AI kèm một yêu cầu như sau:

```text
Đọc kỹ DESIGN.md. Làm lại giao diện của <tên trang / đường dẫn file> theo đúng
design system trong file đó:
- Dùng đúng màu, font, cỡ chữ, bo góc, bóng đổ, khoảng cách đã định nghĩa (mục 2–4).
- Trước tiên thêm design tokens (mục 11) vào dự án nếu chưa có, rồi dùng token
  thay vì viết mã màu trực tiếp.
- Dựng component theo đặc tả ở mục 6; trang Kho kiến thức theo mục 8.
- Giữ nguyên logic, API, tên route và state hiện có — chỉ thay phần giao diện.
- Tuân thủ checklist accessibility ở mục 9.
Làm từng bước: tokens → layout chung (sidebar, topbar) → component → từng trang.
```

---

## 1. Nguyên tắc thiết kế

1. **Tông sáng, sạch.** Nền trang xám-xanh rất nhạt, nội dung đặt trên card trắng. Không dùng nền tối cho sidebar.
2. **Một màu chủ đạo: xanh ngọc (teal).** Dùng cho nút chính, trạng thái đang chọn, liên kết, trạng thái "đang dùng".
3. **Màu nhận diện theo nhóm.** Mỗi mục menu, mỗi thẻ thống kê và mỗi kỹ năng có một màu riêng lấy từ bảng màu phụ (mục 2.3). Màu chỉ để nhận diện nhanh — luôn đi kèm chữ hoặc icon, không bao giờ là cách duy nhất để truyền tải thông tin.
4. **Phân cấp rõ ràng.** Tiêu đề trang to và đậm, nhãn trường nhỏ nhưng đậm, chữ phụ màu xám đủ tương phản.
5. **Thao tác phản hồi tức thì.** Bật/tắt, lọc, chọn đều đổi giao diện ngay (cập nhật lạc quan), có hiệu ứng chuyển ngắn 150ms.
6. **Dễ chạm, dễ đọc.** Vùng bấm tối thiểu 44×44px. Chữ thường tương phản tối thiểu 4.5:1.
7. **Không dùng:** gradient phủ cả nền, emoji làm icon, card có viền màu một bên trái, chữ xám nhạt dưới 4.5:1.

---

## 2. Màu sắc

### 2.1 Màu nền & chữ (neutral)

| Token | Hex | Dùng cho |
|---|---|---|
| `bg-page` | `#F5F7FB` | Nền toàn trang |
| `surface` | `#FFFFFF` | Card, sidebar, topbar, input |
| `surface-subtle` | `#F8FAFC` | Ô tìm kiếm topbar, dòng tài liệu đã tắt |
| `surface-footer` | `#FAFBFD` | Footer của card (vùng nút hành động) |
| `surface-hover` | `#F4F6FA` | Hover mục menu |
| `surface-muted` | `#F1F4F9` | Nền thanh lọc dạng tab |
| `border` | `#E6EAF2` | Viền card, sidebar, topbar |
| `border-subtle` | `#EEF1F6` | Đường kẻ trong card |
| `border-item` | `#E9EDF3` | Viền dòng trong danh sách |
| `border-control` | `#8391A5` | Viền input, select, nút phụ, chip chưa chọn, switch tắt (đạt 3:1) |
| `text` | `#0F172A` | Chữ chính, tiêu đề, số liệu |
| `text-strong` | `#1E293B` | Nhãn trường, chữ trên nút phụ |
| `text-secondary` | `#334155` | Chữ menu, icon topbar |
| `text-muted` | `#526070` | Mô tả, chú thích, metadata |
| `text-muted-alt` | `#475569` | Chữ trên nền pastel của thẻ thống kê |
| `text-overline` | `#64748B` | Nhãn nhóm menu (chữ in hoa nhỏ) |
| `text-placeholder` | `#66717F` | Placeholder |

### 2.2 Màu chủ đạo (teal)

| Token | Hex | Dùng cho |
|---|---|---|
| `primary` | `#0F766E` | Nút chính, switch bật, icon tile đang chọn, link, focus ring |
| `primary-hover` | `#115E59` | Hover nút chính / link |
| `primary-ink` | `#0F5F58` | Chữ trên nền teal nhạt |
| `primary-soft` | `#E6F7F3` | Nền mục menu đang chọn, banner thành công, badge đếm |
| `primary-soft-2` | `#D9F5EF` | Nền badge "MD", vùng thả file khi đang kéo |
| `primary-tint` | `#F2FBF9` | Nền vùng thả file, empty state |
| `primary-border` | `#C9ECE2` | Viền thẻ thống kê teal, ô file đã chọn |
| `primary-dashed` | `#2A9D8F` | Viền nét đứt vùng thả file |
| `brand-gradient` | `linear-gradient(135deg, #0F766E 0%, #0E7490 100%)` | **Chỉ** cho logo, avatar, icon tiêu đề card, thanh tiến độ, icon empty state |

### 2.3 Bảng màu phụ (nhận diện)

Mỗi màu có 5 sắc độ. Dùng `solid` cho icon tile đặc (icon trắng), `ink` cho chữ trên nền nhạt, `soft` cho nền tag / icon tile nhạt, `card` + `card-border` cho thẻ thống kê.

| Màu | `solid` | `ink` | `soft` | `card` | `card-border` |
|---|---|---|---|---|---|
| Blue | `#2563EB` | `#1D4ED8` | `#E0EDFF` | `#EEF4FF` | `#D6E4FD` |
| Teal | `#0F766E` | `#0F5F58` | `#D9F5EF` | `#E9F8F4` | `#C9ECE2` |
| Violet | `#7C3AED` | `#6D28D9` | `#EFE9FF` | `#F3EEFF` | `#E2D6FD` |
| Amber | `#D97706` | `#B45309` | `#FFF1DB` | `#FFF5E6` | `#F9E0B8` |
| Rose | `#E11D48` | `#BE123C` | `#FFE4EC` | `#FFF1F4` | `#FBD0DC` |

> Icon trên nền `soft` dùng màu `ink` (Amber dùng `#B45309`, không dùng `#D97706` vì không đủ 3:1).

**Gán màu cố định:**

| Thành phần | Màu |
|---|---|
| Menu: Tổng quan | Blue |
| Menu: Tài khoản | Violet |
| Menu: Kỳ thi & chứng chỉ | Amber |
| Menu: Bài kiểm tra | Rose |
| Menu: Kho kiến thức | Teal |
| Thẻ: Tổng tài liệu | Blue |
| Thẻ: Đang sử dụng | Teal |
| Thẻ: Đã tắt | Amber |
| Thẻ: Tổng đoạn kiến thức | Violet |

**Màu kỹ năng / phần thi (động):** danh sách kỹ năng phụ thuộc kỳ thi (IELTS: Listening/Reading/Writing/Speaking; TOEIC: Part 1–7; …). Gán màu theo **thứ tự** kỹ năng trong cấu hình kỳ thi, xoay vòng: Blue → Teal → Violet → Rose → Amber → (lặp lại). Lưu màu đã gán vào cấu hình để mỗi kỹ năng luôn giữ một màu ở mọi màn hình.

### 2.4 Màu trạng thái

| Token | Hex | Dùng cho |
|---|---|---|
| `success-dot` | `#10B981` | Chấm "Hệ thống hoạt động" (kèm vòng `rgba(16,185,129,.2)` 3px) |
| `danger` | `#B42318` | Viền input lỗi, chữ thông báo lỗi |
| `required` | `#E11D48` | Dấu `*` trường bắt buộc |
| `notify-dot` | `#F43F5E` | Chấm thông báo mới trên chuông |
| `off-track` | `#8391A5` | Switch tắt |
| `off-soft` | `#EEF1F5` | Nền badge / tag khi tài liệu đã tắt |

---

## 3. Chữ (Typography)

**Font:** [Be Vietnam Pro](https://fonts.google.com/specimen/Be+Vietnam+Pro) — hiển thị tiếng Việt tốt. Trọng lượng: 400, 500, 600, 700.

```html
<link rel="preconnect" href="https://fonts.googleapis.com">
<link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
<link href="https://fonts.googleapis.com/css2?family=Be+Vietnam+Pro:wght@400;500;600;700&display=swap" rel="stylesheet">
```

Font stack: `'Be Vietnam Pro', system-ui, -apple-system, 'Segoe UI', sans-serif`

| Vai trò | Cỡ / dòng | Độ đậm | Ghi chú |
|---|---|---|---|
| Tiêu đề trang (h1) | 28px / 36px | 700 | `letter-spacing: -0.02em` |
| Số liệu thống kê | 28px / 34px | 700 | `font-variant-numeric: tabular-nums`, `-0.02em` |
| Tiêu đề card (h2) | 17px / 24px | 600 | |
| Tiêu đề card phụ (h3) | 15px / 22px | 600 | |
| Tên thương hiệu | 16px | 700 | "Learning" màu `primary` |
| Chữ thường | 14px / 1.5 | 400 | Menu dùng 500 |
| Nhãn trường | 13px | 600 | màu `text-strong` |
| Mô tả, chú thích | 13px | 400 | màu `text-muted` |
| Metadata nhỏ | 12.5px | 400 | |
| Caption | 12px | 400 | |
| Badge / tag | 11.5px | 600 | |
| Overline (nhãn nhóm menu) | 11px | 700 | in hoa, `letter-spacing: 0.08em` |

---

## 4. Khoảng cách, bo góc, bóng đổ

**Khoảng cách (px):** 2 · 4 · 6 · 8 · 12 · 14 · 16 · 20 · 24 · 28 · 40

| Vị trí | Giá trị |
|---|---|
| Khoảng giữa các khối lớn trong trang | 24 |
| Khoảng giữa 2 cột / giữa các card | 20 |
| Khoảng giữa các thẻ thống kê | 16 |
| Padding card | 20 (card phụ) / 24 (card form) |
| Padding header card | 20 × 24 |
| Khoảng giữa các trường form | 20 (dọc), 16 (ngang) |
| Nhãn → input | 6 |
| Padding trang | `28px clamp(16px, 3vw, 40px) 56px` |

**Bo góc:**

| Token | Giá trị | Dùng cho |
|---|---|---|
| `radius-sm` | 8px | Nút đóng nhỏ |
| `radius-md` | 10px | Input, select, textarea, icon tile menu |
| `radius-lg` | 12px | Nút, icon tile, ô tìm kiếm, thanh tab |
| `radius-xl` | 14px | Dòng trong danh sách |
| `radius-2xl` | 16px | Thẻ thống kê, vùng thả file |
| `radius-3xl` | 18px | Card chính |
| `radius-full` | 999px | Chip, badge, tag, switch |

**Bóng đổ:**

| Token | Giá trị |
|---|---|
| `shadow-card` | `0 1px 2px rgba(15,23,42,.04)` |
| `shadow-hover` | `0 10px 24px rgba(15,23,42,.07)` |
| `shadow-row-hover` | `0 4px 14px rgba(15,23,42,.06)` |
| `shadow-tab-active` | `0 1px 3px rgba(15,23,42,.10), 0 0 0 1px #E6EAF2` |
| `shadow-icon-<màu>` | `0 6px 14px rgba(<màu solid>, .25)` — dưới icon tile đặc màu |
| `shadow-primary-btn` | `0 6px 16px rgba(15,118,110,.28)` |

**Chuyển động:** 150ms cho hover, bật/tắt, đổi tab. Thẻ thống kê khi hover: `translateY(-2px)` + `shadow-hover`. Icon tile menu khi hover: `translateY(-1px) scale(1.05)`. Tôn trọng `prefers-reduced-motion`.

---

## 5. Bố cục chung (App shell)

```
┌──────────────┬──────────────────────────────────────────────┐
│              │ Topbar (68px, trắng, viền dưới)               │
│   Sidebar    ├──────────────────────────────────────────────┤
│   264px      │ Breadcrumb                                    │
│   trắng      │ Tiêu đề trang (h1)                             │
│   viền phải  │ Mô tả                                          │
│              │ [Thẻ thống kê × 4]                             │
│              │ [Nội dung chính]                               │
└──────────────┴──────────────────────────────────────────────┘
```

- Nội dung chính: `max-width: 1400px`, padding theo mục 4.
- Hàng thẻ thống kê: `grid-template-columns: repeat(auto-fit, minmax(220px, 1fr))`, gap 16.
- Bố cục 2 cột: flex-wrap, gap 20, cột trái `flex: 1.5 1 540px`, cột phải `flex: 1 1 360px`, cả hai `min-width: 0`.
- **Responsive:**
  - < 1024px: 2 cột tự xuống hàng.
  - < 768px: sidebar chuyển thành drawer mở bằng nút menu trên topbar.
  - < 640px: ẩn tên + vai trò người dùng trên topbar, chỉ giữ avatar.

---

## 6. Component

Icon dùng bộ **[Lucide](https://lucide.dev)**, `stroke-width: 2`, nét bo tròn. Icon trang trí đặt `aria-hidden="true"`.

### 6.1 Sidebar

- Nền `surface`, viền phải `1px border`, padding `20px 16px`, các khối cách nhau 26px.
- **Logo:** icon tile 42×42, bo 13px, nền `brand-gradient`, icon `ShieldCheck` trắng 21px, bóng `0 6px 16px rgba(14,116,144,.28)`. Bên phải: "Adaptive **Learning**" (16/700, "Learning" màu `primary`) và dòng phụ "Trung tâm quản trị" (12/500, `text-overline`).
- **Nhãn nhóm:** "QUẢN TRỊ HỌC TẬP CÁ NHÂN" — overline, padding `0 10px 8px`.
- **Mục menu:** `<a>` cao tối thiểu 48px, padding `6px 10px`, bo 12px, gap 12, chữ 14/500 `text-secondary`.
  - Icon tile 34×34, bo 10px, nền `soft` + icon màu `ink`/`solid` theo bảng 2.3, icon 18px.
  - Hover: nền `surface-hover`, chữ `text`; icon tile nhô nhẹ.
  - Đang chọn (`aria-current="page"`): nền `primary-soft`, chữ `primary-ink` 600, icon tile nền `primary` + icon trắng + bóng `0 4px 10px rgba(15,118,110,.3)`, chấm tròn 7px màu `primary` ở cuối dòng.
- **Thẻ trạng thái cuối sidebar:** padding 14, bo 14, nền `primary-tint`, viền `primary-border`. Dòng 1: chấm `success-dot` + "Hệ thống hoạt động" (13/600 `primary-ink`). Dòng 2: "Quyền quản trị" (12, `text-muted-alt`).

Icon menu: Tổng quan `LayoutGrid` · Tài khoản `Users` · Kỳ thi & chứng chỉ `GraduationCap` · Bài kiểm tra `ClipboardCheck` · Kho kiến thức `Database`.

### 6.2 Topbar

- Cao tối thiểu 68px, nền `surface`, viền dưới `border`, padding ngang như nội dung.
- **Ô tìm kiếm:** `flex: 1 1 320px; max-width: 460px`, cao 44, bo 12, viền `border-control`, nền `surface-subtle`, icon `Search` 17px ở trái (padding-left 42px).
- **Nút icon** (chuông, đăng xuất): 44×44, bo 12, nền trong suốt, hover `#F1F5F9`, bắt buộc `aria-label`. Chuông có chấm `notify-dot` 8px viền trắng 2px.
- **Avatar:** tròn 38px, nền `brand-gradient`, chữ cái trắng 14/700, vòng ngoài `0 0 0 3px primary-soft`. Bên cạnh: tên (14/600) + vai trò (12, `text-muted`). Có vạch ngăn dọc 1×28px màu `border` trước khối avatar.

### 6.3 Đầu trang

- Breadcrumb 13px `text-muted`, phân cách bằng icon `ChevronRight` 14px; mục hiện tại màu `primary` 600, `aria-current="page"`.
- h1 theo mục 3. Mô tả 14px `text-muted`, `max-width: 680px`. Khoảng cách 6px giữa các dòng.

### 6.4 Thẻ thống kê

- Nền `card`, viền `card-border` theo màu (bảng 2.3), bo 16, padding `18px 20px`, flex ngang gap 14.
- Icon tile 44×44, bo 12, nền `solid`, icon trắng 20px, bóng `shadow-icon-<màu>`.
- Cột chữ: nhãn (13/600 `text-muted-alt`) → số (28/700) → chú thích (12 `text-muted-alt`).
- Hover: nhô lên 2px + `shadow-hover`.
- Số liệu luôn tính từ dữ liệu thật, định dạng `vi-VN` (1.234). Chú thích có thể là giá trị suy ra, ví dụ "80% tổng tài liệu".

### 6.5 Card

- Nền `surface`, viền `border`, bo 18, `shadow-card`, `overflow: hidden`.
- **Header card:** padding `20px 24px`, viền dưới `border-subtle`. Icon tile 42×42 nền `brand-gradient` icon trắng + tiêu đề h2 + mô tả 13px.
- **Thân:** padding 24, các phần cách 20.
- **Footer card:** padding `14px 24px`, viền trên `border-subtle`, nền `surface-footer`. Trái: gợi ý (icon `Info` 16 + chữ 12.5 `text-muted`). Phải: các nút.

### 6.6 Nút

| Loại | Kiểu |
|---|---|
| Chính | Cao 44, padding `0 18px`, bo 12, nền `primary`, chữ trắng 14/600, icon 17px trái cách 8, `shadow-primary-btn`. Hover `primary-hover`. |
| Phụ | Cao 44, padding `0 16px`, bo 12, nền trắng, viền `border-control`, chữ `text-strong` 14/600. Hover nền `#F1F5F9`. |
| Icon | 44×44, bo 12, trong suốt, bắt buộc `aria-label`. |

- **Không vô hiệu hoá nút chính** khi form chưa hợp lệ. Cho bấm và hiện lỗi ngay dưới trường thiếu, để người dùng biết vì sao chưa gửi được.
- **Focus:** `outline: 2px solid primary; outline-offset: 2px` (chỉ khi `:focus-visible`).

### 6.7 Trường nhập liệu

- Nhãn đặt trên trường (`<label for>`), 13/600 `text-strong`, trường bắt buộc có `*` màu `required` (`aria-hidden`) + thuộc tính `required` trên input.
- Input / select: cao 44, padding `0 14px`, bo 10, viền `border-control`, nền trắng, chữ 14.
- Select: `appearance: none`, icon `ChevronDown` 16px đặt tuyệt đối bên phải (`pointer-events: none`), padding-right 40.
- Textarea: tối thiểu 220px, `resize: vertical`, padding `12px 14px`, line-height 1.6.
- Gợi ý dưới trường: 12.5px `text-muted`.
- **Lỗi:** viền `danger`, thông báo 12.5px `danger` với `role="alert"`; xoá lỗi ngay khi người dùng gõ lại.

### 6.8 Vùng thả file (Dropzone)

- `<label for="file-input">` bọc toàn vùng; `<input type="file">` ẩn trực quan nhưng vẫn focus được; vùng hiện viền focus khi input được focus (`:focus-within`).
- Padding `26px 20px`, bo 16, viền `1.5px dashed primary-dashed`, nền `primary-tint`, căn giữa.
- Icon tròn 52px nền trắng, bóng `0 6px 16px rgba(15,118,110,.15)`, icon `UploadCloud` 24px màu `primary`.
- Chữ: "**Kéo thả file vào đây** hoặc <u>chọn từ máy</u>" (phần sau màu `primary`, gạch chân) + dòng phụ định dạng & giới hạn.
- Đang kéo file qua: viền `primary`, nền `primary-soft-2`.
- Sau khi chọn: thay bằng ô file (viền `primary-border`, nền `primary-tint`, icon tile teal đặc, tên file + dung lượng + số ký tự, nút phụ "Bỏ file").

### 6.9 Chip chọn (kỹ năng / phần thi)

- `<button aria-pressed>` cao 44, padding `0 14px`, bo `full`, chữ 13.5/600, gap 7.
- Chưa chọn: nền trắng, viền `border-control`, chữ `text-strong`, chấm tròn 8px màu `solid` của kỹ năng.
- Đã chọn: nền `soft`, chữ `ink`, viền `ink` + `box-shadow: 0 0 0 1px <ink>` (viền 2px), icon `Check` 15px thay chấm.

### 6.10 Thanh lọc dạng tab (segmented)

- Khung: nền `surface-muted`, bo 12, padding 4, gap 4.
- Mỗi tab: `<button aria-pressed>`, chia đều chiều ngang, cao 40, bo 9, chữ 13/600 + badge đếm.
- Đang chọn: nền trắng + `shadow-tab-active`, chữ `text`, badge nền `primary-soft` chữ `primary-ink`.
- Chưa chọn: trong suốt, chữ `text-muted-alt`, badge nền `#E3E8EF` chữ `text-secondary`.

### 6.11 Switch bật/tắt

- `<button role="switch" aria-checked aria-label="Dùng tài liệu <tên>">`, vùng bấm 48×40.
- Track 40×24 bo `full`: bật `primary`, tắt `off-track`. Núm 18×18 trắng, `top: 3px`, `left: 3px` → `19px` khi bật, bóng `0 1px 2px rgba(15,23,42,.25)`, chuyển động 150ms.
- Dưới switch: nhãn trạng thái 11.5/600 — "Đang dùng" (`primary`) / "Đã tắt" (`text-muted`).

### 6.12 Badge & tag

- **Badge đếm:** cạnh tiêu đề, 12/700, padding `1px 9px`, bo `full`, nền `primary-soft`, chữ `primary-ink`.
- **Tag kỹ năng:** 11.5/600, padding `2px 8px`, bo `full`, nền `soft`, chữ `ink`, chấm 6px màu `ink` phía trước. Tài liệu đã tắt: nền `off-soft`, chữ `text-muted-alt`.
- **Badge loại file:** ô 40×40, bo 11, chữ 10.5/700 `letter-spacing: .04em`. MD: nền `primary-soft-2` chữ `primary-ink`. TXT: nền Blue `soft` chữ Blue `ink`. Đã tắt: nền `off-soft` chữ `text-muted`.

### 6.13 Dòng tài liệu trong danh sách

- `<ul>` không bullet, gap 8. Mỗi `<li>`: flex, gap 12, padding 12, viền `border-item`, bo 14, nền trắng (đã tắt: `surface-subtle`).
- Cột: badge loại file → [tiêu đề 14/600 (đã tắt: `text-muted-alt`) · tên file 12.5 `text-muted` · hàng tag kỹ năng + "Kỳ thi · N đoạn · dd/mm/yyyy" 12px] → switch.
- Hover: viền `#B9E4DA` + `shadow-row-hover`.
- Tiêu đề dài được xuống dòng, không cắt `…`.

### 6.14 Banner thành công

- `role="status"`, padding `10px 8px 10px 14px`, bo 12, nền `primary-soft`, chữ `primary-ink` 13.5.
- Icon `CheckCircle` 18 + "Đã thêm **“Tên tài liệu”** vào kho kiến thức." + nút đóng 36×36 (`aria-label="Đóng thông báo"`).

### 6.15 Trạng thái trống

- Căn giữa, padding `36px 20px`, viền `1.5px dashed #9ED8CB`, bo 16, nền `primary-tint`.
- Icon tile 60×60 bo 18 nền `brand-gradient`, icon trắng 28. Tiêu đề 15/600, mô tả 13 `text-muted` tối đa 320px, link hành động 13.5/600 trỏ tới form.

### 6.16 Thanh tiến độ & bộ đếm

- Track cao 5, bo `full`, nền `border-subtle`; phần đã dùng nền `brand-gradient`, `aria-hidden` (con số bên cạnh đã đủ thông tin).
- Bộ đếm: "12.345 / 500.000 ký tự", 12px `text-muted`, `tabular-nums`.

### 6.17 Danh sách bước có số

- `<ol>` không bullet, gap 14. Số trong ô 28×28 bo 9, chữ 13/700. Màu lần lượt: Blue `soft`/`ink` → Violet → Teal.
- Bên phải: tiêu đề bước 13.5/600 + mô tả 12.5 `text-muted`.

---

## 7. Biểu tượng (Lucide)

| Nơi dùng | Icon |
|---|---|
| Logo | `ShieldCheck` |
| Tìm kiếm | `Search` |
| Thông báo | `Bell` |
| Đăng xuất | `LogOut` |
| Breadcrumb | `ChevronRight` |
| Select | `ChevronDown` |
| Tổng tài liệu / file | `FileText` |
| Đang sử dụng / thành công | `CheckCircle` (`CircleCheck`) |
| Đã tắt | `PauseCircle` (`CirclePause`) |
| Đoạn kiến thức | `Layers` |
| Thêm tài liệu | `Plus` |
| Tải file lên | `UploadCloud` (`CloudUpload`) |
| Kho kiến thức / nút đưa vào kho | `Database` |
| Gợi ý | `Info` |
| Đóng | `X` |
| Chip đã chọn | `Check` |

---

## 8. Đặc tả trang: Kho kiến thức

**Mục đích:** admin nhập tài liệu (.txt/.md hoặc dán nội dung), gắn kỳ thi + kỹ năng; AI dùng tài liệu **đang bật** làm nguồn để sinh câu luyện tập.

### 8.1 Cấu trúc

1. Breadcrumb "Trung tâm quản trị › Kho kiến thức", h1 "Kho kiến thức", mô tả.
2. **4 thẻ thống kê:** Tổng tài liệu · Đang sử dụng (kèm % trên tổng) · Đã tắt · Tổng đoạn kiến thức. Tất cả tính từ dữ liệu.
3. **Cột trái — Card "Thêm tài liệu vào kho"**
   - Banner thành công (khi vừa thêm).
   - Tệp nguồn: dropzone (6.8).
   - Lưới 2 cột (`repeat(auto-fit, minmax(220px, 1fr))`): **Tên tài liệu*** · **Tên file nguồn** (gợi ý "Tự điền khi bạn chọn file.").
   - Hàng flex-wrap: **Kỳ thi** (select, `flex: 1 1 200px`) · **Kỹ năng / môn thi** (chip, `flex: 2 1 320px`).
   - **Nội dung tài liệu***: nhãn + "Hỗ trợ Markdown" bên phải, textarea, thanh tiến độ + bộ đếm (giới hạn 500.000 ký tự).
   - Footer: gợi ý "Tài liệu mới được bật ngay sau khi nhập." · nút phụ "Làm mới" · nút chính "Đưa vào kho kiến thức" (icon `Database`).
4. **Cột phải**
   - Card "Tài liệu đã nhập" + badge tổng + mô tả "Dùng chung cho mọi lộ trình học."
     - Ô tìm kiếm (theo tên, file, kỹ năng, kỳ thi).
     - Thanh lọc: Tất cả (n) · Đang dùng (n) · Đã tắt (n).
     - Danh sách dòng tài liệu (6.13); không có kết quả → khung nét đứt "Không có tài liệu nào khớp với bộ lọc hiện tại."
     - Kho rỗng → trạng thái trống (6.15), ẩn ô tìm kiếm và thanh lọc.
   - Card "Cách AI dùng kho kiến thức": 3 bước (6.17) — Nhập tài liệu → Tách thành đoạn kiến thức → AI tạo câu luyện tập.

### 8.2 Hành vi

- **Chọn hoặc thả file:** chỉ nhận `.txt`, `.md` (sai định dạng → lỗi dưới dropzone). Đọc nội dung vào textarea (cắt tại 500.000 ký tự và báo "đã cắt"), điền tên file nguồn, điền tên tài liệu từ tên file nếu ô tên đang trống.
- **Kỹ năng phụ thuộc kỳ thi:** danh sách chip lấy từ cấu hình kỳ thi đang chọn (không viết cứng). Đổi kỳ thi thì chọn lại kỹ năng đầu tiên. Màu chip theo mục 2.3.
- **Gửi:** kiểm tra tên và nội dung; thiếu → hiện lỗi tại trường, focus vào trường lỗi đầu tiên. Hợp lệ → gọi API, thêm tài liệu lên đầu danh sách với trạng thái "Đang dùng", reset form, hiện banner thành công, đặt bộ lọc về "Tất cả".
- **Xử lý nền:** tách đoạn và tạo embedding chạy ngầm. Trong lúc xử lý, dòng tài liệu hiển thị "Đang xử lý…" thay cho số đoạn; giao diện không bị khoá.
- **Bật/tắt:** cập nhật giao diện ngay (lạc quan), gọi API sau; lỗi thì hoàn tác và báo lỗi.
- **Tìm kiếm / lọc:** lọc phía client khi ít dữ liệu; nhiều dữ liệu thì debounce 300ms rồi gọi API.
- **Định dạng:** số theo `vi-VN` (`toLocaleString('vi-VN')`), ngày `dd/mm/yyyy`.

---

## 9. Checklist accessibility

- [ ] Phần tử tương tác là `<button>`, `<a href>`, `<input>`, `<select>` thật — không gắn `onClick` lên `<div>`/`<span>`.
- [ ] Mọi input có `<label for>`; nhóm chip có `role="group"` + `aria-labelledby`.
- [ ] Nút chỉ có icon có `aria-label`; icon trang trí có `aria-hidden="true"`.
- [ ] Chip và tab dùng `aria-pressed`; switch dùng `role="switch"` + `aria-checked`; menu đang chọn dùng `aria-current="page"`.
- [ ] Lỗi dùng `role="alert"`; thông báo thành công dùng `role="status"`.
- [ ] Chữ thường tương phản ≥ 4.5:1; viền control, icon, focus ring ≥ 3:1 (đã kiểm với bảng màu trên — đừng thay màu nhạt hơn).
- [ ] Focus ring hiển thị rõ khi dùng bàn phím (`:focus-visible`).
- [ ] Vùng bấm ≥ 44×44px.
- [ ] `<html lang="vi">`.
- [ ] Có `@media (prefers-reduced-motion: reduce)` tắt hiệu ứng chuyển động.

---

## 10. Nên / Không nên

| Nên | Không nên |
|---|---|
| Dùng token màu | Viết mã màu tuỳ ý trong component |
| Icon Lucide nét 2px | Emoji, icon nhiều bộ trộn lẫn |
| `brand-gradient` chỉ ở logo, avatar, icon tiêu đề, thanh tiến độ | Gradient làm nền card hoặc nền trang |
| Màu kèm chữ / icon | Chỉ dùng màu để phân biệt trạng thái |
| Báo lỗi ngay tại trường | Vô hiệu hoá nút mà không nói lý do |
| Số liệu tính từ dữ liệu thật | Số giả, lorem ipsum |
| Một màu chủ đạo (teal) cho hành động chính | Nhiều nút chính khác màu trên cùng màn hình |

---

## 11. Code tokens dán vào dự án

### 11.1 CSS variables (dùng được với mọi framework)

```css
:root {
  /* Neutral */
  --bg-page: #F5F7FB;
  --surface: #FFFFFF;
  --surface-subtle: #F8FAFC;
  --surface-footer: #FAFBFD;
  --surface-hover: #F4F6FA;
  --surface-muted: #F1F4F9;
  --border: #E6EAF2;
  --border-subtle: #EEF1F6;
  --border-item: #E9EDF3;
  --border-control: #8391A5;
  --text: #0F172A;
  --text-strong: #1E293B;
  --text-secondary: #334155;
  --text-muted: #526070;
  --text-muted-alt: #475569;
  --text-overline: #64748B;
  --text-placeholder: #66717F;

  /* Primary */
  --primary: #0F766E;
  --primary-hover: #115E59;
  --primary-ink: #0F5F58;
  --primary-soft: #E6F7F3;
  --primary-soft-2: #D9F5EF;
  --primary-tint: #F2FBF9;
  --primary-border: #C9ECE2;
  --primary-dashed: #2A9D8F;
  --brand-gradient: linear-gradient(135deg, #0F766E 0%, #0E7490 100%);

  /* Accents: solid / ink / soft / card / card-border */
  --blue: #2563EB;   --blue-ink: #1D4ED8;   --blue-soft: #E0EDFF;   --blue-card: #EEF4FF;   --blue-card-border: #D6E4FD;
  --teal: #0F766E;   --teal-ink: #0F5F58;   --teal-soft: #D9F5EF;   --teal-card: #E9F8F4;   --teal-card-border: #C9ECE2;
  --violet: #7C3AED; --violet-ink: #6D28D9; --violet-soft: #EFE9FF; --violet-card: #F3EEFF; --violet-card-border: #E2D6FD;
  --amber: #D97706;  --amber-ink: #B45309;  --amber-soft: #FFF1DB;  --amber-card: #FFF5E6;  --amber-card-border: #F9E0B8;
  --rose: #E11D48;   --rose-ink: #BE123C;   --rose-soft: #FFE4EC;   --rose-card: #FFF1F4;   --rose-card-border: #FBD0DC;

  /* Status */
  --success-dot: #10B981;
  --danger: #B42318;
  --required: #E11D48;
  --notify-dot: #F43F5E;
  --off-track: #8391A5;
  --off-soft: #EEF1F5;

  /* Radius */
  --radius-sm: 8px;
  --radius-md: 10px;
  --radius-lg: 12px;
  --radius-xl: 14px;
  --radius-2xl: 16px;
  --radius-3xl: 18px;
  --radius-full: 999px;

  /* Shadow */
  --shadow-card: 0 1px 2px rgba(15, 23, 42, .04);
  --shadow-hover: 0 10px 24px rgba(15, 23, 42, .07);
  --shadow-row-hover: 0 4px 14px rgba(15, 23, 42, .06);
  --shadow-primary-btn: 0 6px 16px rgba(15, 118, 110, .28);

  /* Font */
  --font-sans: 'Be Vietnam Pro', system-ui, -apple-system, 'Segoe UI', sans-serif;
}

body {
  margin: 0;
  background: var(--bg-page);
  color: var(--text);
  font-family: var(--font-sans);
  font-size: 14px;
  line-height: 1.5;
}

::placeholder { color: var(--text-placeholder); opacity: 1; }

:focus-visible { outline: 2px solid var(--primary); outline-offset: 2px; }

@media (prefers-reduced-motion: reduce) {
  *, *::before, *::after { transition: none !important; animation: none !important; }
}
```

### 11.2 Tailwind CSS v4 (`@theme` trong file CSS chính)

```css
@import "tailwindcss";

@theme {
  --font-sans: 'Be Vietnam Pro', system-ui, -apple-system, 'Segoe UI', sans-serif;

  --color-page: #F5F7FB;
  --color-surface: #FFFFFF;
  --color-surface-subtle: #F8FAFC;
  --color-surface-hover: #F4F6FA;
  --color-surface-muted: #F1F4F9;
  --color-line: #E6EAF2;
  --color-line-subtle: #EEF1F6;
  --color-line-control: #8391A5;
  --color-ink: #0F172A;
  --color-ink-strong: #1E293B;
  --color-ink-secondary: #334155;
  --color-ink-muted: #526070;
  --color-ink-muted-alt: #475569;

  --color-primary: #0F766E;
  --color-primary-hover: #115E59;
  --color-primary-ink: #0F5F58;
  --color-primary-soft: #E6F7F3;
  --color-primary-soft-2: #D9F5EF;
  --color-primary-tint: #F2FBF9;
  --color-primary-border: #C9ECE2;

  --color-blue: #2563EB;   --color-blue-ink: #1D4ED8;   --color-blue-soft: #E0EDFF;   --color-blue-card: #EEF4FF;
  --color-violet: #7C3AED; --color-violet-ink: #6D28D9; --color-violet-soft: #EFE9FF; --color-violet-card: #F3EEFF;
  --color-amber: #D97706;  --color-amber-ink: #B45309;  --color-amber-soft: #FFF1DB;  --color-amber-card: #FFF5E6;
  --color-rose: #E11D48;   --color-rose-ink: #BE123C;   --color-rose-soft: #FFE4EC;   --color-rose-card: #FFF1F4;

  --color-danger: #B42318;

  --radius-md: 10px;
  --radius-lg: 12px;
  --radius-xl: 14px;
  --radius-2xl: 16px;
  --radius-3xl: 18px;
}
```

Ví dụ dùng: `bg-page`, `bg-surface border border-line rounded-3xl`, `text-ink-muted`, `bg-primary hover:bg-primary-hover text-white rounded-lg h-11 px-[18px]`.

### 11.3 Tailwind CSS v3 (`tailwind.config.js`)

```js
/** @type {import('tailwindcss').Config} */
module.exports = {
  content: ['./src/**/*.{js,jsx,ts,tsx,vue,html}'],
  theme: {
    extend: {
      fontFamily: { sans: ['"Be Vietnam Pro"', 'system-ui', '-apple-system', '"Segoe UI"', 'sans-serif'] },
      colors: {
        page: '#F5F7FB',
        surface: { DEFAULT: '#FFFFFF', subtle: '#F8FAFC', hover: '#F4F6FA', muted: '#F1F4F9' },
        line: { DEFAULT: '#E6EAF2', subtle: '#EEF1F6', control: '#8391A5' },
        ink: { DEFAULT: '#0F172A', strong: '#1E293B', secondary: '#334155', muted: '#526070', 'muted-alt': '#475569' },
        primary: { DEFAULT: '#0F766E', hover: '#115E59', ink: '#0F5F58', soft: '#E6F7F3', 'soft-2': '#D9F5EF', tint: '#F2FBF9', border: '#C9ECE2' },
        blue:   { DEFAULT: '#2563EB', ink: '#1D4ED8', soft: '#E0EDFF', card: '#EEF4FF' },
        violet: { DEFAULT: '#7C3AED', ink: '#6D28D9', soft: '#EFE9FF', card: '#F3EEFF' },
        amber:  { DEFAULT: '#D97706', ink: '#B45309', soft: '#FFF1DB', card: '#FFF5E6' },
        rose:   { DEFAULT: '#E11D48', ink: '#BE123C', soft: '#FFE4EC', card: '#FFF1F4' },
        danger: '#B42318',
      },
      borderRadius: { md: '10px', lg: '12px', xl: '14px', '2xl': '16px', '3xl': '18px' },
      boxShadow: {
        card: '0 1px 2px rgba(15,23,42,.04)',
        hover: '0 10px 24px rgba(15,23,42,.07)',
        'primary-btn': '0 6px 16px rgba(15,118,110,.28)',
      },
    },
  },
};
```

> Lưu ý: cấu hình v3 ở trên **ghi đè** các thang màu `blue`, `violet`, `amber`, `rose` mặc định của Tailwind. Nếu dự án đang dùng `blue-500`… ở chỗ khác, đổi tên thành `acc-blue`, `acc-violet`… để tránh xung đột.