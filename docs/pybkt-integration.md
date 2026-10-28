# Tích hợp pyBKT

## Thành phần trong dự án

Hệ thống đã tích hợp `pyBKT` qua Python sidecar ở `Adaptive Learning/pybkt`. Backend Spring gọi `POST /v1/mastery/estimate` thông qua `BktClient`; sidecar không truy cập database trực tiếp. Docker Compose khởi động sidecar trước backend và cấu hình `BKT_URL=http://pybkt:8001`.

Sau khi bài thi được nộp, backend gom các câu hỏi khách quan đã chấm của learner vào `learnerInteractions` và dữ liệu của những learner khác cùng certificate vào `trainingInteractions`. Kỹ năng hiện được định danh là `TOPIC_<topicId>`. Tự luận, câu chưa được chấm và câu khác certificate bị loại. Backend sắp xếp theo thời gian nộp bài rồi thứ tự câu hỏi; sidecar giữ thứ tự phần tử để phát lại chuỗi phản hồi.

## Chạy service

Từ thư mục `Adaptive Learning`:

```powershell
docker compose up --build pybkt
```

Kiểm tra service:

```powershell
Invoke-RestMethod http://localhost:8001/health
```

Trong môi trường không dùng Docker, cần Python và cài `Adaptive Learning/pybkt/requirements.txt`, sau đó chạy `python app.py`; mặc định service lắng nghe cổng `8001` (có thể đổi bằng biến `PORT`).

## Dữ liệu đầu vào

pyBKT nhận DataFrame hoặc CSV/TSV. Với dữ liệu nội bộ, thư viện cần ánh xạ các cột tương ứng với người học, kỹ năng, thứ tự sự kiện và kết quả đúng/sai. Sidecar thực hiện ánh xạ đó từ JSON: `studentId` thành learner ID, `skill` thành skill name, `correct` thành nhãn 0/1, và thứ tự mảng thành `order_id`.

```json
{
  "learnerId": "learner-3",
  "trainingInteractions": [
    {"studentId": "learner-1", "skill": "TOPIC_7", "correct": true},
    {"studentId": "learner-1", "skill": "TOPIC_7", "correct": false},
    {"studentId": "learner-2", "skill": "TOPIC_7", "correct": false},
    {"studentId": "learner-2", "skill": "TOPIC_7", "correct": true}
  ],
  "learnerInteractions": [
    {"studentId": "learner-3", "skill": "TOPIC_7", "correct": false},
    {"studentId": "learner-3", "skill": "TOPIC_7", "correct": true}
  ]
}
```

Yêu cầu thực tế:

- Mỗi event cần `studentId`, `skill` không rỗng và `correct` không null. `correct` phải là boolean; dữ liệu sai bị loại hoặc không được chuyển thành nhãn hợp lệ.
- `trainingInteractions` phải có ít nhất hai learner khác nhau, và phải có cùng skill với chuỗi learner mục tiêu để hiệu chuẩn được skill đó. Chỉ có hai learner là ngưỡng kỹ thuật hiện tại, không phải khuyến nghị về độ tin cậy thống kê.
- Chuỗi cần đúng thứ tự thời gian cho từng learner và skill. Dữ liệu chuẩn là một event cho mỗi câu trả lời đã chấm, không phải tổng điểm bài thi.
- Không dùng dữ liệu learner mục tiêu trong tập huấn luyện cohort. Backend hiện đã tách như vậy.

Gọi thử service đang chạy:

```powershell
$body = Get-Content .\request.json -Raw
Invoke-RestMethod -Method Post `
  -Uri http://localhost:8001/v1/mastery/estimate `
  -ContentType 'application/json' `
  -Body $body
```

Phản hồi khi đã hiệu chuẩn có dạng:

```json
{
  "ready": true,
  "masteryBySkill": {"TOPIC_7": 0.72},
  "trainingStudentCount": 2,
  "trainingEventCount": 4,
  "message": "..."
}
```

`masteryBySkill` là xác suất mastery sau khi phát lại chuỗi learner, trong khoảng 0-1; nó không phải điểm thi hay band IELTS/TOEIC. `ready=false` có thể xảy ra khi thiếu learner huấn luyện hoặc không có skill trùng. Backend hiện giữ heuristic cũ khi BKT chưa sẵn sàng hoặc service lỗi.

## Chạy ví dụ upstream

Repo `CAHLR/pyBKT-examples` chứa script nghiên cứu và bộ dữ liệu mẫu, không phải service HTTP. Trong môi trường Python đã cài `pyBKT` cùng các dependency của script, clone repo rồi chạy ví dụ từ thư mục repo, chẳng hạn `python BasicForgets.py`. Các ví dụ có thể tải/đọc dataset mẫu như Assistments hoặc Cognitive Tutor; không đưa dữ liệu mẫu này vào cohort production. README của pyBKT cũng chỉ rõ dữ liệu cần các cột learner, skill, thứ tự và correct/incorrect, với nhãn `-1` cho no-response, `0` sai và `1` đúng. API nội bộ hiện chỉ nhận phản hồi đã chấm dạng boolean, không biểu diễn no-response.

## Kiểm tra đã thực hiện

Đã build image `Adaptive Learning/pybkt/Dockerfile`, chạy sidecar, xác nhận `/health`, và gửi payload gồm hai learner huấn luyện cùng chuỗi learner mục tiêu. Service trả `ready=true` và xác suất mastery cho `TOPIC_7`. Trên Windows máy phát triển hiện chưa cài Python/pandas/pyBKT; smoke test được chạy bằng Docker image của dự án.

Nguồn: [pyBKT](https://github.com/CAHLR/pyBKT), [pyBKT-examples](https://github.com/CAHLR/pyBKT-examples).
