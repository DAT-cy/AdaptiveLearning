package adaptivelearning.module.rag.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;

class PdfTextExtractorTest {
    @Test
    void extractsTextFromPdf() throws Exception {
        byte[] pdf;
        try (PDDocument document = new PDDocument();
                ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                content.beginText();
                content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                content.newLineAtOffset(40, 700);
                content.showText("IELTS reading practice");
                content.endText();
            }
            document.save(output);
            pdf = output.toByteArray();
        }

        assertEquals("IELTS reading practice", PdfTextExtractor.extract(pdf));
    }

    @Test
    void rejectsScannedOrEmptyPdf() throws Exception {
        byte[] pdf;
        try (PDDocument document = new PDDocument();
                ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            document.addPage(new PDPage());
            document.save(output);
            pdf = output.toByteArray();
        }

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class, () -> PdfTextExtractor.extract(pdf));
        assertTrue(exception.getMessage().contains("OCR"));
    }

    @Test
    void rejectsNonPdfContent() {
        assertThrows(IllegalArgumentException.class,
                () -> PdfTextExtractor.extract("not a pdf".getBytes()));
    }
}
