---
name: ielts-exam-builder
description: Xây dựng hệ thống tạo đề thi IELTS động cho giáo viên và màn làm bài cho học sinh theo form đề chuẩn (Listening 4 section, Reading 3 passage, Writing 2 task). Dùng khi làm màn tạo đề, schema đề thi, renderer từng dạng câu hỏi, tự nhận diện ô trống "……8……", import đề từ PDF bằng AI, hoặc chấm điểm tự động.
---

# IELTS Exam Builder — Tạo đề động & Màn làm bài

## 0. Bối cảnh & phạm vi

Hệ thống gồm 3 phần, dùng chung MỘT nguồn dữ liệu là **Exam JSON**:

| Phần | Người dùng | Việc chính |
|---|---|---|
| **Exam Builder** | Giáo viên | Dựng đề: kỹ năng → section → nhóm câu (từ câu X đến câu Y, đề bài, dạng câu) → câu hỏi + đáp án |
| **AI Parser** | Giáo viên (trợ lý) | Đọc nội dung giáo viên dán vào hoặc file PDF, tự nhận dạng dạng câu, tìm ô trống "……", đánh số, điền sẵn vào form |
| **Exam Player** | Học sinh | Hiển thị đề đúng form chuẩn, làm bài, tính giờ, nộp bài, chấm tự động |

Nguyên tắc bất biến:

1. Builder chỉ **ghi** Exam JSON, Player chỉ **đọc** Exam JSON. Không hardcode nội dung đề trong UI.
2. Số câu và phạm vi "Questions X–Y" luôn **tính ra từ dữ liệu**, không để giáo viên gõ tay rời rạc (đề giấy hay bị lệch số, xem mục 6.5).
3. AI chỉ **đề xuất**. Giáo viên xem lại và bấm xác nhận thì mới lưu. AI không tự publish.
4. Mỗi dạng câu hỏi là một module độc lập (editor + renderer + validate + grade). Thêm dạng mới thì không phải sửa code của dạng cũ.
5. Giữ đơn giản: đây là form tạo đề và làm bài. Ngoài phạm vi (trừ khi được yêu cầu riêng): quản lý lớp, ngân hàng câu hỏi, thanh toán, phân quyền phức tạp.

Ngôn ngữ: giao diện giáo viên bằng tiếng Việt; nội dung đề và màn học sinh giữ tiếng Anh như đề gốc.

---

## 1. Form đề chuẩn (phân tích từ đề mẫu "Test 5")

| Kỹ năng | Đơn vị con | Số câu chuẩn | Thời gian mặc định | Tư liệu (stimulus) |
|---|---|---|---|---|
| Listening | SECTION 1–4 | 40 (10 câu/section) | 30 phút | Audio cho từng section |
| Reading | READING PASSAGE 1–3 | 40 (13 / 13 / 14) | 60 phút (~20 phút/passage) | Bài đọc có tiêu đề, chia đoạn A, B, C…, có thể kèm ảnh |
| Writing | WRITING TASK 1–2 | 2 task | 20 + 40 phút | Task 1: ảnh biểu đồ, ≥150 từ. Task 2: đề luận, ≥250 từ |
| Speaking | Part 1–3 | — | — | Không có trong đề mẫu; chừa chỗ mở rộng |

Số câu đánh lại từ 1 ở mỗi kỹ năng (Listening 1–40, Reading 1–40). Thời gian và số câu chỉ là mặc định, giáo viên sửa được.

Mỗi khối câu hỏi trong đề luôn theo đúng khuôn dưới đây. Đây là "form" mà Builder phải sinh ra và Player phải hiển thị lại y như vậy:

```
SECTION 1                          Questions 1–10     ← tiêu đề section + phạm vi section
Questions 1–4                                         ← phạm vi nhóm
Complete the sentences below.                         ← đề bài (in nghiêng)
Write NO MORE THAN TWO WORDS for each answer.         ← giới hạn từ (phần giới hạn in đậm)
                    New Guinea                        ← tiêu đề nội dung (tuỳ chọn, căn giữa)
The Eastern side of ... became an [ 1 ] in 1975.      ← nội dung có ô trống đánh số
```

Cây dữ liệu:

```
Exam
└── Skill                 listening | reading | writing | speaking
    └── Section           "SECTION 1" | "READING PASSAGE 1" | "WRITING TASK 1"
        ├── stimulus      audio | passage (đoạn A–I) | ảnh
        ├── guidance      "You should spend about 20 minutes on Questions 1–13…"
        └── QuestionGroup "Questions 1–4"
            ├── type         dạng câu (mục 2)
            ├── range        {from, to}  ← suy ra từ questions
            ├── instruction  đề bài
            ├── constraint   giới hạn từ / số
            ├── content      tiêu đề, ghi chú, đoạn tóm tắt có ô trống {{n}}
            ├── options      hộp lựa chọn dùng chung (A–F…)
            └── Question[]   số câu, thân câu hỏi, lựa chọn riêng, đáp án
```

---

## 2. Danh mục dạng câu hỏi

Ví dụ lấy từ đề mẫu (L = Listening, R = Reading).

| type | Tên hiển thị cho GV | Ví dụ trong đề mẫu | Học sinh thấy | Chấm |
|---|---|---|---|---|
| `sentence_completion` | Hoàn thành câu | L1–4, R24–26 | Câu có ô nhập inline đúng vị trí trống | So khớp text |
| `note_completion` | Hoàn thành ghi chú | L5–10, L16–20 (tiêu đề nhỏ + gạch đầu dòng), L31–40 (trong khung viền) | Giữ nguyên tiêu đề / bullet / khung, ô nhập inline | So khớp text |
| `table_completion` | Hoàn thành bảng | (không có trong mẫu) | Bảng, ô trống là input | So khớp text |
| `summary_completion` | Hoàn thành tóm tắt | R8–10 (có tiêu đề tóm tắt) | Đoạn văn liền, ô nhập inline; có word bank thì dùng dropdown | Text hoặc chữ cái |
| `short_answer` | Trả lời ngắn | R11–13 | Câu hỏi + ô nhập bên dưới | So khớp text |
| `mcq_single` | Trắc nghiệm 1 đáp án | L11–15, L23–26 (A–C), R33–35 (A–D) | Radio A/B/C/D | Chữ cái |
| `mcq_multiple` | Trắc nghiệm nhiều đáp án | L21–22 "Choose TWO letters, A–E" | 1 câu hỏi chiếm 2 số câu, checkbox chỉ cho chọn đúng 2 | So tập hợp, không cần đúng thứ tự |
| `matching` | Nối thông tin | L27–30 (hộp A–F + các mục), R19–23 (List of Scientists A–E, "may use any letter more than once") | Hộp lựa chọn cố định phía trên, mỗi mục 1 dropdown | Chữ cái |
| `matching_information` | Tìm đoạn chứa thông tin | R14–18 "Which paragraph contains…" A–F | Dropdown nhãn đoạn, lấy tự động từ passage | Chữ cái |
| `matching_headings` | Chọn tiêu đề đoạn | (không có trong mẫu) | Dropdown i, ii, iii… cho từng đoạn | Số La Mã |
| `matching_sentence_endings` | Nối đuôi câu | R36–40 (A–H) | Đầu câu + dropdown đuôi câu | Chữ cái |
| `true_false_not_given` | True / False / Not Given | (không có trong mẫu) | Radio TRUE / FALSE / NOT GIVEN | Chuẩn hoá T/F/NG |
| `yes_no_not_given` | Yes / No / Not Given | R1–7, R27–32 | Radio YES / NO / NOT GIVEN | Chuẩn hoá Y/N/NG |
| `diagram_labelling` | Điền nhãn sơ đồ/bản đồ | (không có trong mẫu) | Ảnh + điểm đánh số + ô nhập | So khớp text |
| `writing_task` | Bài viết | Task 1 (biểu đồ tròn), Task 2 (đề luận) | Đề + ảnh bên trái, ô soạn thảo bên phải, đếm từ trực tiếp | Không tự chấm (mục 8) |

Phân loại theo **nội dung thực tế**, không chỉ theo nhãn trong đề. Ví dụ: đề mẫu ghi "Summary" ở R36–40 nhưng thực chất là nối đuôi câu A–H, nên type phải là `matching_sentence_endings`.

---

## 3. Schema (TypeScript, validate bằng Zod)

```ts
type SkillType = 'listening' | 'reading' | 'writing' | 'speaking';

type QuestionType =
  | 'sentence_completion' | 'note_completion' | 'table_completion'
  | 'summary_completion' | 'short_answer'
  | 'mcq_single' | 'mcq_multiple'
  | 'matching' | 'matching_information' | 'matching_headings'
  | 'matching_sentence_endings'
  | 'true_false_not_given' | 'yes_no_not_given'
  | 'diagram_labelling' | 'writing_task';

interface Exam {
  id: string;
  title: string;                       // "Test 5"
  status: 'draft' | 'published';
  mode: 'exam' | 'practice';           // exam: nghiêm ngặt; practice: xem đáp án sau khi nộp
  skills: Skill[];                     // làm theo đúng thứ tự trong mảng
}

interface Skill {
  type: SkillType;
  durationMinutes: number;
  sections: Section[];                 // tổng số câu suy ra từ sections, không lưu tay
}

interface Section {
  id: string;
  order: number;
  title: string;                       // "SECTION 1" | "READING PASSAGE 2" | "WRITING TASK 1"
  guidance?: string;                   // "You should spend about 20 minutes on Questions 14–26…"
  stimulus?: {
    audioUrl?: string;
    passage?: {
      title: string;                   // "Multitasks"
      imageUrl?: string;
      paragraphs: { label?: string; html: string }[];   // label: "A", "B"…
    };
    imageUrls?: string[];              // biểu đồ Writing Task 1, bản đồ Listening…
  };
  groups: QuestionGroup[];
}

interface QuestionGroup {
  id: string;
  type: QuestionType;
  range: { from: number; to: number }; // luôn tính lại từ questions, không nhập tay
  instruction: string;                 // "Complete the sentences below."
  constraint?: {
    maxWords?: number;                 // ONE WORD ONLY = 1, NO MORE THAN TWO WORDS = 2
    allowNumber?: boolean;             // "AND/OR A NUMBER"
    fromPassage?: boolean;             // "from the passage"
    minWords?: number;                 // writing: 150 / 250
  };
  content?: string;                    // markdown có token ô trống {{n}}: tiêu đề, bullet, khung, đoạn tóm tắt
  boxed?: boolean;                     // hiển thị trong khung viền (như L31–40)
  options?: Option[];                  // hộp lựa chọn dùng chung (matching, word bank, sentence endings, mcq_multiple)
  optionsTitle?: string;               // "List of Scientists", "Benefits of the cut-down water hyacinth"
  allowOptionReuse?: boolean;          // "You may use any letter more than once"
  selectCount?: number;                // mcq_multiple: số chữ cái phải chọn
  sharedPrompt?: string;               // mcq_multiple: "Which two problems are caused by water hyacinth?"
  questions: Question[];
}

interface Option { key: string; text: string }        // key: "A" | "B" | "i" | "ii"

interface Question {
  number: number;                      // số câu toàn cục trong kỹ năng
  prompt?: string;                     // thân câu (MCQ, Y/N/NG, mục matching, short answer, đề writing)
  options?: Option[];                  // lựa chọn riêng của câu (mcq_single)
  answer: Answer | null;               // null = chưa có đáp án → chặn publish
  points?: number;                     // mặc định 1
  sampleAnswer?: string;               // writing: bài mẫu, chỉ hiện ở chế độ practice sau khi nộp
}

type Answer =
  | { kind: 'text'; accepted: string[] }   // ["flavour", "flavor"]
  | { kind: 'key'; value: string }         // "C" | "YES" | "iv"
  | { kind: 'keySet'; values: string[] }   // mcq_multiple: mọi câu trong nhóm giữ cùng tập ["B","E"]
  | { kind: 'manual' };                    // writing
```

Cùng một Zod schema được dùng ở Builder, AI Parser và server.

Ví dụ JSON (trích Test 5):

```json
{
  "type": "sentence_completion",
  "range": { "from": 1, "to": 4 },
  "instruction": "Complete the sentences below.",
  "constraint": { "maxWords": 2 },
  "content": "### New Guinea\nThe Eastern side of the island of New Guinea, Papua New Guinea, became an {{1}} in 1975.\nOnly 18% of the inhabitants of Papua New Guinea reside in {{2}}.\nPapua New Guinea tribes should be seen as separate {{3}}.\nThere are a small number of tribes who have had {{4}} with neighbouring groups and the world beyond.",
  "questions": [
    { "number": 1, "answer": { "kind": "text", "accepted": ["independent country"] } },
    { "number": 2, "answer": { "kind": "text", "accepted": ["urban areas"] } },
    { "number": 3, "answer": { "kind": "text", "accepted": ["social groups"] } },
    { "number": 4, "answer": { "kind": "text", "accepted": ["no contact"] } }
  ]
}
```

```json
{
  "type": "mcq_multiple",
  "range": { "from": 21, "to": 22 },
  "instruction": "Choose TWO letters, A–E.",
  "selectCount": 2,
  "sharedPrompt": "Which two problems are caused by water hyacinth?",
  "options": [
    { "key": "A", "text": "plants and fish are poisoned" },
    { "key": "B", "text": "Farmers cannot fish" },
    { "key": "C", "text": "the dam's structure is damaged" },
    { "key": "D", "text": "Electricity production is affected." },
    { "key": "E", "text": "electricity production is affected." }
  ],
  "questions": [
    { "number": 21, "answer": { "kind": "keySet", "values": ["B", "E"] } },
    { "number": 22, "answer": { "kind": "keySet", "values": ["B", "E"] } }
  ]
}
```

---

## 4. Màn giáo viên — Exam Builder

Bố cục 3 cột:

- **Trái**: cây đề (Kỹ năng → Section → Nhóm câu) kèm phạm vi câu, kéo thả để đổi thứ tự, huy hiệu lỗi/cảnh báo.
- **Giữa**: form chỉnh sửa phần đang chọn.
- **Phải**: xem trước trực tiếp bằng chính component của Exam Player, để giáo viên thấy y hệt những gì học sinh sẽ thấy.

### 4.1 Thông tin đề
Tên đề; chọn kỹ năng (checkbox); thời gian từng kỹ năng (điền sẵn mặc định ở mục 1); chế độ exam/practice.

### 4.2 Dựng khung — 2 chế độ

**A. Nhanh — "kỹ năng này chỉ có N câu"**

Giáo viên chỉ nhập: kỹ năng, tổng số câu, (tuỳ chọn) số section. Hệ thống:
- Tạo section và chia đều số câu, phần dư dồn vào section cuối. Ví dụ Reading 20 câu / 2 passage → 1–10, 11–20.
- Đánh số liên tục từ 1.
- Để trống dạng câu; giáo viên chọn sau, hoặc dán nội dung để AI nhận dạng (mục 6).
- Kỹ năng không đủ 40 câu thì không quy đổi band, chỉ hiện điểm thô và %.

**B. Chi tiết**

Giáo viên tự thêm section, thêm nhóm câu, chọn dạng, rồi nhập phạm vi:
- Ô **"Từ câu"** tự điền bằng (câu cuối của nhóm trước + 1) và khoá lại.
- Ô **"Đến câu"** và ô **"Số câu"** đồng bộ hai chiều; giáo viên sửa ô nào cũng được.
- Đổi số câu của một nhóm → các nhóm phía sau tự dời số, kèm thông báo "Câu 11–40 sẽ được đánh lại số". Đáp án đi theo câu, không đi theo số.
- Kéo thả nhóm hoặc section → đánh lại số toàn kỹ năng.

**Mẫu khung**: lưu cấu trúc thành mẫu ("IELTS Academic đầy đủ", "Mini Listening 10 câu", "Reading 1 passage 13 câu"). Chọn mẫu → sinh sẵn khung + đề bài chuẩn.

### 4.3 Nhập nội dung từng nhóm
Mỗi dạng có editor riêng:
- **Completion** (sentence/note/summary/table): một ô soạn thảo rich text; ô trống gõ theo cú pháp ở mục 5; panel bên cạnh liệt kê các ô trống đã nhận diện.
- **MCQ**: danh sách câu, mỗi câu có prompt + lựa chọn; chữ cái A, B, C… tự sinh khi thêm/bớt lựa chọn.
- **Matching**: soạn hộp lựa chọn (tiêu đề + A–F) và danh sách mục; công tắc "Cho phép dùng lại chữ cái".
- **Yes/No/NG, True/False/NG**: danh sách nhận định.
- **matching_information / matching_headings**: nhãn đoạn lấy tự động từ passage của section.
- **Writing**: đề bài, ảnh, số từ tối thiểu, thời gian gợi ý, bài mẫu (tuỳ chọn).
- **Stimulus**: upload audio (Listening); soạn passage có nhãn đoạn A, B, C… và ảnh (Reading).

Nút **"Dán nội dung để AI điền"** có ở mọi cấp (nhóm, section, cả đề) → mục 6.

### 4.4 Đề bài tự sinh
Khi chọn dạng + phạm vi + giới hạn từ, hệ thống sinh đề bài chuẩn (giáo viên sửa được). Mọi con số trong đề bài lấy từ dữ liệu:

| Dạng | Đề bài sinh tự động |
|---|---|
| sentence_completion | Complete the sentences below. ⏎ Write **{LIMIT}** for each answer. |
| note_completion | Complete the notes below. ⏎ Write **{LIMIT}** for each answer. |
| summary_completion | Complete the summary below. ⏎ Choose **{LIMIT}** from the passage for each answer. |
| short_answer | Answer the questions below. ⏎ Choose **{LIMIT}** from the passage for each answer. |
| yes_no_not_given | Do the following statements agree with the information given in Reading Passage {p}? In boxes {from}–{to} on your answer sheet, write YES / NO / NOT GIVEN… |
| matching_information | Reading Passage {p} has {k} paragraphs, A–{last}. Which paragraph contains the following information? Write the correct letter, A–{last}, in boxes {from}–{to} on your answer sheet. |
| mcq_single | Choose the correct letter, A, B{, C}{ or D}. |
| mcq_multiple | Choose {TWO/THREE} letters, A–{last}. |

`{LIMIT}`: 1 → `ONE WORD ONLY`; 2 → `NO MORE THAN TWO WORDS`; 3 → `NO MORE THAN THREE WORDS`; thêm `AND/OR A NUMBER` nếu `allowNumber`.
`{p}` lấy đúng số passage chứa nhóm, nên không bao giờ ghi nhầm passage như đề mẫu (R27–32 nằm ở Passage 3 nhưng đề ghi "Reading Passage 1").

### 4.5 Đáp án
- Nhập ngay trong editor của từng câu, hoặc dán cả bảng key ("1 yes 2 No … 8 Rock") để AI ghép theo số câu.
- Đáp án text có nhiều biến thể thì ngăn bằng `/`: `flavour/flavor`, `fertilisers/fertilizers`. Phần không bắt buộc đặt trong ngoặc: `(a) local museum`.
- Cảnh báo nếu đáp án dài hơn giới hạn từ của nhóm.

### 4.6 Kiểm tra & xuất bản

| Mức | Quy tắc |
|---|---|
| Lỗi | Số câu trong một kỹ năng phải liên tục từ 1, không trùng, không thiếu |
| Lỗi | Số ô trống trong `content` = số câu của nhóm completion |
| Lỗi | Mọi câu có đáp án (trừ writing) |
| Lỗi | mcq_multiple: số câu của nhóm = `selectCount` = số phần tử trong `keySet` |
| Lỗi | Đáp án chữ cái phải nằm trong tập lựa chọn |
| Lỗi | matching không cho dùng lại chữ cái: số lựa chọn ≥ số mục |
| Lỗi | Section Listening thiếu audio; Writing thiếu đề bài |
| Cảnh báo | Hai lựa chọn trùng nội dung (đề mẫu: L21–22 có D và E giống hệt) |
| Cảnh báo | Đáp án vượt giới hạn từ |
| Cảnh báo | Kỹ năng khác 40 câu (không quy đổi band) |
| Cảnh báo | Nghi lỗi chính tả trong đề: gợi ý sửa, không tự sửa |

Lỗi chặn publish, cảnh báo thì không. Bấm vào từng mục trong danh sách → nhảy tới đúng chỗ trong editor.

---

## 5. Ô trống — nhận diện "từ mấy chấm đến mấy chấm"

Giáo viên gõ ô trống theo bất kỳ kiểu nào thường gặp trong đề giấy; parser chuyển hết về token chuẩn `{{n}}`.

| Giáo viên gõ | Ví dụ thật trong đề mẫu | Kết quả |
|---|---|---|
| Chấm – số – chấm | `Yet the ………8…….. covering` | `{{8}}` |
| Số rồi chấm | `the 35…… of food`, `The 25 ……….. part` | `{{35}}`, `{{25}}` |
| Chấm không số | `your surroundings is ………` | `{{?}}` → đánh số tự động |
| Gạch dưới | `became an ____ in 1975` | `{{?}}` |
| Số trong ngoặc vuông | `became an [1] in 1975` | `{{1}}` |
| Token chuẩn | `{{8}}` | giữ nguyên |
| Ô chữ nhật có số trên PDF | ô `1` ở L1–4 | `{{1}}` (qua AI import, mục 6) |

Regex tham chiếu (đã chạy thử với các chuỗi trên). Chuẩn hoá `…` thành `...` trước khi dùng:

```js
const BLANK = /(?:(?<![\w.])(\d{1,2})\s*)?\.{3,}(?:\s*(\d{1,2})\s*\.+)?|_{3,}|\[(\d{1,2})\]|\{\{(\d{1,2}|\?)\}\}/g;
// số ô = m[1] || m[2] || m[3] || m[4] || '?'
```

Thuật toán:
1. Chuẩn hoá: `…` → `...`, gộp khoảng trắng thừa.
2. Chỉ quét trong `content` của nhóm completion/short answer. **Không** quét passage/stimulus, URL, hay các nhóm không phải completion (dấu "..." trích dẫn trong bài đọc không phải ô trống).
3. Ô có số: số phải thuộc phạm vi nhóm và không trùng. Số nằm ngoài phạm vi → coi như ô không số + cảnh báo.
4. Ô không số: gán số chưa dùng tiếp theo trong phạm vi, theo thứ tự đọc.
5. Ngoại lệ: dòng kiểu `27 dired water hyacinth ............` trong nhóm có hộp lựa chọn A–F là **chỗ điền chữ cái** của mục matching, không phải ô điền từ → tạo Question matching (`prompt` = "dired water hyacinth"), không tạo `{{27}}`.
6. Báo kết quả ngay trong editor: "Phát hiện 3 ô trống: 8, 9, 10 — khớp phạm vi 8–10 ✓", hoặc "Phạm vi 8–10 nhưng chỉ thấy ô 8 và 10 — thiếu câu 9", kèm tô sáng vị trí.
7. Giáo viên bấm vào một ô để đổi số, xoá, hoặc chuyển thành chữ thường.

Phía học sinh: mỗi `{{n}}` thành ô nhập inline có số `n` mờ bên trong (giống ô đánh số trên đề giấy); độ rộng theo `maxWords` (khoảng 8ch mỗi từ, tối thiểu 10ch).

Bắt buộc có unit test cho `blanks.ts` với toàn bộ chuỗi ở bảng trên.

---

## 6. AI Parser — dán nội dung hoặc import PDF

### 6.1 Đầu vào
- Text dán vào ở cấp nhóm, section, hoặc cả đề.
- File PDF hoặc ảnh. Đề mẫu là **PDF scan** (không có lớp text, có watermark logo) → phải OCR hoặc dùng model đọc ảnh. PDF có lớp text thì lấy text trực tiếp.

### 6.2 Quy trình
1. Trích text + vị trí. Bỏ header/watermark lặp lại ở mọi trang (ví dụ "Group : real ielts exams").
2. Cắt khối theo tiêu đề: `SECTION \d`, `READING PASSAGE \d`, `WRITING TASK \d`, `Questions? \d+\s*[-–]\s*\d+`, `Key reading` / `KEY listening` / `Answer key`.
3. Phân loại dạng từng nhóm theo bảng tín hiệu 6.3 kết hợp hình dạng nội dung.
4. Tách: đề bài, giới hạn từ, nội dung có ô trống (mục 5), hộp lựa chọn, câu hỏi, passage + nhãn đoạn, ảnh.
5. Ghép đáp án từ bảng key theo kỹ năng → section → số câu.
6. Xuất **đúng** Exam JSON (mục 3) kèm `warnings[]` và `confidence` cho từng nhóm.
7. Chạy validator (mục 4.6).
8. Hiện màn **review**: trái là ảnh trang gốc, phải là form đã điền; nhóm có confidence thấp được tô vàng. Giáo viên sửa xong bấm "Xác nhận" mới lưu.

### 6.3 Tín hiệu phân loại

| Tín hiệu trong đề bài / nội dung | type | Trích thêm |
|---|---|---|
| "Complete the sentences" | sentence_completion | |
| "Complete the notes", hoặc nội dung có tiêu đề nhỏ + bullet | note_completion | `boxed` nếu có khung |
| "Complete the table" | table_completion | |
| "Complete the summary" + ô trống | summary_completion | word bank → `options` |
| Nhãn "Summary" nhưng là các đầu câu + danh sách A–H | matching_sentence_endings | |
| "Answer the questions" | short_answer | |
| "Choose the correct letter, A, B or C" | mcq_single | |
| "Choose TWO / THREE letters" | mcq_multiple | `selectCount` |
| "YES / NO / NOT GIVEN" | yes_no_not_given | |
| "TRUE / FALSE / NOT GIVEN" | true_false_not_given | |
| "Which paragraph contains" | matching_information | |
| "List of Headings" | matching_headings | |
| "Match each statement with…", hoặc hộp A–F + danh sách mục | matching | `allowOptionReuse` nếu có "more than once" |
| "Label the map / diagram / plan" | diagram_labelling | |
| "Write at least 150 / 250 words" | writing_task | `minWords`, thời gian |
| "ONE WORD ONLY" | — | `maxWords: 1` |
| "NO MORE THAN TWO / THREE WORDS" | — | `maxWords: 2 / 3` |
| "AND/OR A NUMBER" | — | `allowNumber: true` |
| "from the passage" | — | `fromPassage: true` |

### 6.4 Hợp đồng đầu ra (đưa vào system prompt của model parse)

```
Bạn chuyển đề thi IELTS thô (text hoặc ảnh) thành JSON đúng schema ExamSchema.
- Chỉ trả về JSON hợp lệ, không kèm giải thích.
- Giữ nguyên văn nội dung đề. KHÔNG tự sửa lỗi chính tả; ghi gợi ý vào warnings.
- KHÔNG bịa đáp án. Không có key thì answer = null và thêm warning.
- Số câu lấy theo số thực tế in trước câu hỏi / trong ô trống. Nếu tiêu đề
  "Questions X–Y" mâu thuẫn với số thực tế → tin số thực tế, thêm warning.
- Mọi ô trống chuyển về {{n}} theo quy tắc ô trống.
- Phân loại dạng theo nội dung, không chỉ theo nhãn.
- Mỗi nhóm có confidence 0–1; đặt < 0.7 khi không chắc dạng câu, phạm vi hoặc đáp án.
```

### 6.5 Bộ kiểm thử bắt buộc — lỗi có thật trong đề mẫu
Chuẩn hoá Test 5 thành `fixtures/test5.json`. Parser + validator phải xử lý đúng các trường hợp sau:

| Vị trí | Hiện trạng trong đề mẫu | Hành vi mong đợi |
|---|---|---|
| Listening Section 3 | Tiêu đề in "Questions 23-36", nhóm in "Questions 21-24", nhưng các câu được in là 23–26 | Phạm vi = 23–26, thêm warning |
| Reading Q1–7 | Danh sách nhảy từ câu 1 sang câu 3, dù key vẫn có "2 No" | Warning "thiếu nội dung câu 2", giữ chỗ để giáo viên bổ sung |
| Listening Q21–22 | Lựa chọn D và E trùng nội dung | Warning trùng lựa chọn |
| Reading Q27–32 | Đề bài ghi "Reading Passage 1" trong khi nằm ở Passage 3 | Warning; đề bài tự sinh sửa thành Passage 3 |
| Reading Q36–40 | Nhãn "Summary" nhưng là nối đuôi câu A–H | type = `matching_sentence_endings` |
| Listening Q27–30 | Sau mỗi mục có "…………" nhưng là chỗ điền chữ cái A–F | type = `matching`, không tạo ô điền từ |
| Key Reading 24–26 | Bảng key lệch dòng ("group meetings" nằm riêng một dòng dưới câu 25, còn số 26 ở dòng sau không có đáp án) | Ghép 26 = group meetings, confidence thấp → đưa vào review |
| Key Listening 35–36 | "flavour/flavor", "fertilisers/ fertilizers" | `accepted` có cả hai biến thể |

---

## 7. Màn học sinh — Exam Player

Chỉ render từ Exam JSON thông qua registry dạng câu (mục 9). Giao diện mô phỏng IELTS làm trên máy.

**Chung**
- Thanh trên: tên học sinh, kỹ năng hiện tại, đồng hồ đếm ngược (cảnh báo khi còn 10 và 5 phút; hết giờ tự nộp kỹ năng đó).
- Các kỹ năng làm lần lượt theo thứ tự trong đề; hết giờ kỹ năng nào thì khoá kỹ năng đó. Chế độ practice cho phép chọn kỹ năng tự do.
- Thanh dưới: điều hướng câu 1–40, chia theo section; trạng thái chưa làm / đã làm / đánh dấu xem lại / đang chọn. Bấm vào số → cuộn tới câu và focus ô.
- Tab / Shift+Tab di chuyển giữa các ô; chỉnh cỡ chữ; chế độ tương phản cao.
- Tự lưu mỗi lần thay đổi (debounce ~1 giây) vào localStorage và server; tải lại trang không mất bài.
- Nộp bài: hộp xác nhận liệt kê các câu chưa làm.

**Listening**: trình phát audio theo section. Chế độ exam chỉ nghe một lần, không tua; practice cho tua và nghe lại. Câu hỏi hiển thị toàn chiều rộng.

**Reading**: chia đôi màn hình — passage bên trái (cuộn độc lập, nhãn đoạn A, B, C in đậm, có ảnh) và câu hỏi bên phải; thanh chia kéo được; bôi đen để highlight trong passage.

**Writing**: đề + ảnh bên trái, ô soạn thảo bên phải; đếm từ trực tiếp, cảnh báo khi dưới `minWords`; tắt kiểm tra chính tả của trình duyệt (`spellcheck={false}`).

**Đúng form đề giấy**: tiêu đề section + "Questions X–Y"; đề bài in nghiêng; phần giới hạn từ in đậm; tiêu đề nội dung căn giữa; khung viền cho note/box; hộp lựa chọn có tiêu đề.

**Ô nhập**: cảnh báo nhẹ (không chặn) khi gõ vượt `maxWords`, ví dụ "Câu này tối đa 2 từ".

---

## 8. Chấm điểm

Mỗi module dạng câu có `grade(group, responses) → { number, correct, earned }[]`.

Chuẩn hoá text trước khi so: trim, lowercase, gộp khoảng trắng, bỏ dấu câu ở đầu/cuối, đổi nháy cong thành nháy thẳng.

| Dạng | Quy tắc |
|---|---|
| Text (completion, short answer, diagram) | Đúng nếu khớp một phần tử trong `accepted` sau chuẩn hoá; phần trong ngoặc là tuỳ chọn; vượt `maxWords` → sai |
| Chữ cái (mcq_single, các dạng matching) | So không phân biệt hoa thường |
| mcq_multiple | So tập hợp, không cần đúng thứ tự; điểm = số chữ đúng trong lựa chọn, tối đa `selectCount` |
| Yes/No/NG, True/False/NG | Chấp nhận Y / YES, N / NO, NG / NOT GIVEN (tương tự T, F); không chấp nhận YES cho dạng True/False/NG và ngược lại |
| Writing | Không tự chấm. Giáo viên chấm 4 tiêu chí (Task Achievement/Response, Coherence & Cohesion, Lexical Resource, Grammatical Range & Accuracy). AI có thể gợi ý nhận xét nhưng phải gắn nhãn "bản nháp AI" |

**Quy đổi band** (chỉ áp dụng khi kỹ năng có đủ 40 câu): bảng nằm trong file config, admin sửa được, không hardcode. Giá trị mặc định là bảng tham khảo phổ biến — đối chiếu nguồn chính thức trước khi dùng thật:

```json
{
  "listening":        [[39,9],[37,8.5],[35,8],[32,7.5],[30,7],[26,6.5],[23,6],[18,5.5],[16,5],[13,4.5],[10,4],[8,3.5],[6,3],[4,2.5]],
  "reading_academic": [[39,9],[37,8.5],[35,8],[33,7.5],[30,7],[27,6.5],[23,6],[19,5.5],[15,5],[13,4.5],[10,4],[8,3.5],[6,3],[4,2.5]],
  "writing_task2_weight": 2
}
```
(mỗi cặp = [điểm thô tối thiểu, band])

**Kết quả hiển thị**: điểm thô từng kỹ năng; band nếu đủ 40 câu; bảng từng câu gồm đáp án học sinh / đáp án đúng / đúng-sai. Chế độ practice hiện ngay sau khi nộp; chế độ exam chỉ hiện khi giáo viên mở kết quả.

---

## 9. Kiến trúc code

Dùng stack sẵn có của dự án. Nếu là dự án mới: React + TypeScript, Zod cho schema, một store cho trạng thái đề (Zustand hoặc Redux), backend lưu Exam JSON nguyên khối (JSONB) và một bảng `attempts` cho bài làm.

Registry dạng câu:

```ts
interface QuestionTypeModule {
  type: QuestionType;
  label: string;                                   // tên tiếng Việt, xem cột "Tên hiển thị cho GV" ở mục 2
  createEmpty(range: { from: number; to: number }): QuestionGroup;
  defaultInstruction(g: QuestionGroup, ctx: { passageNo?: number }): string;
  Editor: React.FC<{ group: QuestionGroup; onChange(g: QuestionGroup): void }>;
  Renderer: React.FC<{
    group: QuestionGroup;
    responses: Record<number, string | string[]>;
    onAnswer(n: number, v: string | string[]): void;
    readOnly?: boolean;
  }>;
  validate(g: QuestionGroup): Issue[];
  grade(g: QuestionGroup, r: Record<number, string | string[]>): GradeResult[];
}

export const questionTypes: Record<QuestionType, QuestionTypeModule> = { /* … */ };
```

Cấu trúc thư mục gợi ý:

```
src/exam/
  schema.ts              Zod schema + type
  numbering.ts           đánh số, dời số, tính range
  blanks.ts              nhận diện ô trống (mục 5)
  validate.ts            luật mục 4.6
  grading/               chuẩn hoá text, band config
  types/<type>/          Editor.tsx, Renderer.tsx, validate.ts, grade.ts
  builder/               màn giáo viên
  player/                màn học sinh
  ai/                    prompt, gọi model, map kết quả về schema, màn review
  fixtures/test5.json    đề mẫu đã chuẩn hoá
```

Thứ tự triển khai:
1. `schema.ts` + `numbering.ts` + `validate.ts` (kèm unit test).
2. `blanks.ts` + unit test với các chuỗi ở mục 5.
3. Renderer cho từng dạng + Player, chạy thử với `fixtures/test5.json`.
4. Builder (preview dùng lại Renderer).
5. Chấm điểm + band.
6. AI Parser + màn review.

---

## 10. Tiêu chí hoàn thành

- [ ] `fixtures/test5.json` render đủ Listening 40 câu, Reading 40 câu, Writing 2 task, đúng form như đề giấy.
- [ ] Nhập đúng key của Test 5 → 40/40 cho Listening và Reading, band 9.0.
- [ ] Chế độ nhanh: tạo được đề "Reading 13 câu, 1 passage" chỉ bằng cách nhập số câu; số câu và đề bài sinh đúng.
- [ ] Đổi số câu của một nhóm → các nhóm sau tự đánh lại số, đề bài cập nhật phạm vi, đáp án đi theo câu.
- [ ] Dán đoạn tóm tắt R8–10 có `………8……..`, `………9……..`, `………10………..` → nhận đúng 3 ô 8, 9, 10.
- [ ] Validator bắt đủ các trường hợp ở bảng 6.5.
- [ ] Tải lại trang giữa bài không mất câu trả lời; hết giờ tự nộp.
- [ ] Thêm một dạng câu mới chỉ cần thêm thư mục trong `types/` và đăng ký vào registry.
