package adaptivelearning.module.rag.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

public final class PdfTextExtractor {
    private static final int MAX_TEXT_LENGTH = 500_000;

    private PdfTextExtractor() {}

    public static String extract(byte[] bytes) {
        if (bytes == null || bytes.length == 0 || !hasPdfHeader(bytes)) {
            throw new IllegalArgumentException("Tệp tải lên không phải PDF hợp lệ.");
        }

        try (PDDocument document = Loader.loadPDF(bytes)) {
            if (document.isEncrypted()) {
                throw new IllegalArgumentException("PDF đang được bảo vệ bằng mật khẩu.");
            }
            String text = new PDFTextStripper().getText(document).trim();
            if (text.isBlank()) {
                throw new IllegalArgumentException(
                        "PDF không có lớp văn bản để đọc. PDF dạng ảnh cần OCR trước khi tải lên.");
            }
            if (text.length() > MAX_TEXT_LENGTH) {
                throw new IllegalArgumentException("Nội dung PDF vượt quá giới hạn 500.000 ký tự.");
            }
            return text;
        } catch (IOException exception) {
            throw new IllegalArgumentException("Không thể đọc PDF. Hãy kiểm tra tệp không bị lỗi hoặc mã hóa.");
        }
    }

    private static boolean hasPdfHeader(byte[] bytes) {
        byte[] header = "%PDF-".getBytes(StandardCharsets.US_ASCII);
        int searchLength = Math.min(bytes.length - header.length, 1024);
        for (int offset = 0; offset <= searchLength; offset++) {
            boolean matches = true;
            for (int index = 0; index < header.length; index++) {
                if (bytes[offset + index] != header[index]) {
                    matches = false;
                    break;
                }
            }
            if (matches) return true;
        }
        return false;
    }
}
