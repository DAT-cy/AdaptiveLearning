package adaptivelearning.module.rag.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

public final class PdfTextExtractor {
    private PdfTextExtractor() {}

    public static String extract(byte[] bytes) {
        if (bytes == null || bytes.length == 0 || !hasPdfHeader(bytes)) {
            throw new IllegalArgumentException("Tệp tải lên không phải PDF hợp lệ.");
        }

        try (PDDocument document = Loader.loadPDF(bytes)) {
            if (document.isEncrypted()) {
                throw new IllegalArgumentException("PDF đang được bảo vệ bằng mật khẩu.");
            }
            List<String> pages = extractPages(document);
            String text = String.join("\n\n", pages).trim();
            if (pages.stream().allMatch(String::isBlank)) {
                throw new IllegalArgumentException(
                        "PDF không có lớp văn bản để đọc. PDF dạng ảnh cần OCR trước khi tải lên.");
            }
            return text;
        } catch (IOException exception) {
            throw new IllegalArgumentException("Không thể đọc PDF. Hãy kiểm tra tệp không bị lỗi hoặc mã hóa.");
        }
    }

    public static List<String> extractPages(byte[] bytes) {
        if (bytes == null || bytes.length == 0 || !hasPdfHeader(bytes)) {
            throw new IllegalArgumentException("Tệp tải lên không phải PDF hợp lệ.");
        }
        try (PDDocument document = Loader.loadPDF(bytes)) {
            if (document.isEncrypted()) {
                throw new IllegalArgumentException("PDF đang được bảo vệ bằng mật khẩu.");
            }
            return extractPages(document);
        } catch (IOException exception) {
            throw new IllegalArgumentException("Không thể đọc PDF. Hãy kiểm tra tệp không bị lỗi hoặc mã hóa.");
        }
    }

    private static List<String> extractPages(PDDocument document) throws IOException {
        List<String> pages = new ArrayList<>(document.getNumberOfPages());
        PDFTextStripper stripper = new PDFTextStripper();
        for (int page = 1; page <= document.getNumberOfPages(); page++) {
            stripper.setStartPage(page);
            stripper.setEndPage(page);
            pages.add(stripper.getText(document).trim());
        }
        return List.copyOf(pages);
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
