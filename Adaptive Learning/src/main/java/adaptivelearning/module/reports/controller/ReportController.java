package adaptivelearning.module.reports.controller;

import adaptivelearning.module.reports.service.ReportService;
import adaptivelearning.config.response.DefaultRes;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class ReportController {
    private final ReportService service;

    @GetMapping
    public DefaultRes<Map<String, Object>> report(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return DefaultRes.res(200, "Report data", service.report(from, to));
    }

    @GetMapping(value = "/export.xlsx", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    public ResponseEntity<byte[]> excel(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) throws Exception {
        Map<String, Object> report = service.report(from, to);
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("Theo ngay");
            var header = sheet.createRow(0);
            String[] cols = {"Ngày", "Lượt thi", "Điểm trung bình", "Đã chấm", "Đang làm"};
            for (int i = 0; i < cols.length; i++) header.createCell(i).setCellValue(cols[i]);
            List<Map<String, Object>> daily = (List<Map<String, Object>>) report.get("daily");
            int rowNum = 1;
            for (Map<String, Object> item : daily) {
                var row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue((String) item.get("date"));
                row.createCell(1).setCellValue(((Number) item.get("attempts")).doubleValue());
                row.createCell(2).setCellValue(((Number) item.get("averageScore")).doubleValue());
                row.createCell(3).setCellValue(((Number) item.get("graded")).doubleValue());
                row.createCell(4).setCellValue(((Number) item.get("inProgress")).doubleValue());
            }
            for (int i = 0; i < cols.length; i++) sheet.autoSizeColumn(i);
            var examSheet = workbook.createSheet("De thi");
            String[] examCols = {"Đề thi", "Lượt làm", "Điểm trung bình", "Đạt", "Đã chấm", "Tỷ lệ đạt (%)"};
            writeHeader(examSheet, examCols);
            writeRows(examSheet, (List<Map<String, Object>>) report.get("exams"),
                    new String[]{"title", "attempts", "averageScore", "passed", "graded", "passRate"});
            var studentSheet = workbook.createSheet("Hoc vien");
            String[] studentCols = {"Học viên", "Tên đăng nhập", "Lượt thi", "Điểm trung bình", "Hoạt động gần nhất"};
            writeHeader(studentSheet, studentCols);
            writeRows(studentSheet, (List<Map<String, Object>>) report.get("students"),
                    new String[]{"name", "username", "attempts", "averageScore", "lastActivity"});
            workbook.write(out);
            return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=learning-report.xlsx")
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(out.toByteArray());
        }
    }

    private void writeHeader(org.apache.poi.ss.usermodel.Sheet sheet, String[] columns) {
        var row = sheet.createRow(0);
        for (int i = 0; i < columns.length; i++) row.createCell(i).setCellValue(columns[i]);
    }

    private void writeRows(org.apache.poi.ss.usermodel.Sheet sheet, List<Map<String, Object>> data, String[] keys) {
        int rowNum = 1;
        for (Map<String, Object> item : data) {
            var row = sheet.createRow(rowNum++);
            for (int i = 0; i < keys.length; i++) {
                Object value = item.get(keys[i]);
                if (value instanceof Number number) row.createCell(i).setCellValue(number.doubleValue());
                else row.createCell(i).setCellValue(value == null ? "" : value.toString());
            }
        }
        for (int i = 0; i < keys.length; i++) sheet.autoSizeColumn(i);
    }

    @GetMapping(value = "/export.pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> pdf(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) throws Exception {
        Map<String, Object> report = service.report(from, to);
        try (PDDocument doc = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(); doc.addPage(page);
            try (PDPageContentStream stream = new PDPageContentStream(doc, page)) {
                stream.beginText(); stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12); stream.newLineAtOffset(50, 750);
                stream.showText("Learning report " + from + " - " + to); stream.newLineAtOffset(0, -22);
                Map<String, Object> kpis = (Map<String, Object>) report.get("kpis");
                stream.showText("Attempts: " + kpis.get("attempts") + " | Graded: " + kpis.get("graded") + " | Average score: " + kpis.get("averageScore"));
                stream.endText();
            }
            doc.save(out);
            return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=learning-report.pdf")
                    .contentType(MediaType.APPLICATION_PDF).body(out.toByteArray());
        }
    }
}
