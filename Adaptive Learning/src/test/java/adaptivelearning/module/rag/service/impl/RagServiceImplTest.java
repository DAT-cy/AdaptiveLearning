package adaptivelearning.module.rag.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import adaptivelearning.config.response.CommonException;
import adaptivelearning.config.response.ErrorCode;
import adaptivelearning.module.certificates.repository.CertificateRepository;
import adaptivelearning.module.rag.dto.request.DocumentUploadRequest;
import adaptivelearning.module.rag.dto.request.KnowledgeBaseUploadRequest;
import adaptivelearning.module.rag.entity.DocumentChunk;
import adaptivelearning.module.rag.entity.StudyDocument;
import adaptivelearning.module.rag.repository.DocumentChunkRepository;
import adaptivelearning.module.rag.repository.StudyDocumentRepository;
import adaptivelearning.module.topics.entity.Topic;
import adaptivelearning.module.topics.repository.TopicRepository;
import adaptivelearning.shared.enums.DocumentStatus;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class RagServiceImplTest {

    private StudyDocumentRepository documents;
    private DocumentChunkRepository chunks;
    private TopicRepository topics;
    private CertificateRepository certificates;
    private RagServiceImpl service;

    @BeforeEach
    void setUp() {
        documents = mock(StudyDocumentRepository.class);
        chunks = mock(DocumentChunkRepository.class);
        topics = mock(TopicRepository.class);
        certificates = mock(CertificateRepository.class);
        service = new RagServiceImpl(documents, chunks, topics, certificates);
        when(documents.save(any(StudyDocument.class))).thenAnswer(invocation -> {
            StudyDocument document = invocation.getArgument(0);
            if (document.getDocumentId() == null) document.setDocumentId(41L);
            return document;
        });
    }

    @Test
    void privateTextUploadPersistsOverlappingChunksAndReturnsCount() {
        when(certificates.existsById(2L)).thenReturn(true);
        DocumentUploadRequest request = new DocumentUploadRequest();
        request.setTitle("Notes");
        request.setCertificateId(2L);
        request.setContent("alpha ".repeat(140));

        var response = service.uploadDocument(5L, request);

        assertEquals(41L, response.getDocumentId());
        assertEquals(2, response.getChunkCount());
        assertEquals(DocumentStatus.READY, response.getStatus());
        var captor = org.mockito.ArgumentCaptor.forClass(List.class);
        verify(chunks).saveAll(captor.capture());
        List<?> saved = captor.getValue();
        assertEquals(2, saved.size());
        DocumentChunk first = (DocumentChunk) saved.get(0);
        DocumentChunk second = (DocumentChunk) saved.get(1);
        assertEquals(0, first.getChunkIndex());
        assertEquals(1, second.getChunkIndex());
        assertEquals(41L, first.getDocumentId());
    }

    @Test
    void privateDocumentReadsAndDeletesAreOwnerScoped() {
        StudyDocument foreign = StudyDocument.builder().documentId(41L).userId(6L).shared(false)
                .title("Private").build();
        when(documents.findById(41L)).thenReturn(Optional.of(foreign));

        assertEquals(ErrorCode.ENTITY_NOT_FOUND,
                assertThrows(CommonException.class, () -> service.getDocument(5L, 41L)).getErrorCode());
        assertEquals(ErrorCode.ENTITY_NOT_FOUND,
                assertThrows(CommonException.class, () -> service.deleteDocument(5L, 41L)).getErrorCode());
        verify(chunks, never()).deleteByDocumentId(41L);
        verify(documents, never()).delete(foreign);
    }

    @Test
    void privateSearchIsOwnerScopedAndUsesTokenOverlapNotVectorSearch() {
        StudyDocument owned = StudyDocument.builder().documentId(41L).userId(5L).shared(false)
                .title("Notes").build();
        DocumentChunk chunk = DocumentChunk.builder().id(3L).documentId(41L).chunkIndex(0)
                .content("alpha learning result").build();
        when(documents.findById(41L)).thenReturn(Optional.of(owned));
        when(chunks.search(41L, "alpha")).thenReturn(List.of(chunk));
        when(chunks.search(41L, "learning")).thenReturn(List.of(chunk));

        var response = service.search(5L, 41L, "alpha learning");

        assertEquals(1, response.getResults().size());
        assertEquals(3L, response.getResults().get(0).getId());
        assertThrows(CommonException.class, () -> service.search(6L, 41L, "alpha"));
        assertThrows(CommonException.class, () -> service.search(5L, 41L, "q".repeat(2001)));
    }

    @Test
    void sharedUploadRequiresExistingCertificateAndMatchingActiveTopic() {
        KnowledgeBaseUploadRequest request = new KnowledgeBaseUploadRequest();
        request.setTitle("Official notes");
        request.setFileName("notes.md");
        request.setCertificateId(2L);
        request.setTopicId(3L);
        request.setContent("source material");
        when(certificates.existsById(2L)).thenReturn(false);
        assertEquals(ErrorCode.BAD_REQUEST,
                assertThrows(CommonException.class, () -> service.uploadKnowledgeDocument(9L, request)).getErrorCode());
        verify(documents, never()).save(any(StudyDocument.class));

        when(certificates.existsById(2L)).thenReturn(true);
        when(topics.findById(3L)).thenReturn(Optional.of(Topic.builder()
                .topicId(3L).certificateId(2L).isActive(false).build()));
        assertEquals(ErrorCode.BAD_REQUEST,
                assertThrows(CommonException.class, () -> service.uploadKnowledgeDocument(9L, request)).getErrorCode());
    }

    @Test
    void uploadRejectsEmptyOversizedTextAndInvalidPdfContent() {
        DocumentUploadRequest empty = new DocumentUploadRequest();
        empty.setTitle("empty");
        empty.setContent("  ");
        assertEquals(ErrorCode.BAD_REQUEST,
                assertThrows(CommonException.class, () -> service.uploadDocument(5L, empty)).getErrorCode());

        when(certificates.existsById(2L)).thenReturn(true);
        KnowledgeBaseUploadRequest oversized = new KnowledgeBaseUploadRequest();
        oversized.setTitle("large");
        oversized.setFileName("large.txt");
        oversized.setCertificateId(2L);
        oversized.setContent("x".repeat(500001));
        assertEquals(ErrorCode.BAD_REQUEST,
                assertThrows(CommonException.class, () -> service.uploadKnowledgeDocument(9L, oversized)).getErrorCode());

        MockMultipartFile invalidPdf = new MockMultipartFile("file", "fake.pdf", "application/pdf",
                "not a PDF".getBytes());
        when(certificates.existsById(2L)).thenReturn(true);
        assertThrows(IllegalArgumentException.class,
                () -> service.uploadKnowledgePdf(9L, "PDF", 2L, null, invalidPdf));
    }

    @Test
    void knowledgeRetrievalUsesActiveCertificateScopedChunks() {
        StudyDocument active = StudyDocument.builder().documentId(41L).certificateId(2L).shared(true)
                .active(true).title("Active").build();
        StudyDocument inactive = StudyDocument.builder().documentId(42L).certificateId(2L).shared(true)
                .active(false).title("Inactive").build();
        DocumentChunk chunk = DocumentChunk.builder().id(7L).documentId(41L).chunkIndex(0)
                .content("Adaptive learning depends on practice").build();
        when(documents.findByCertificateIdAndSharedTrueOrderByDocCreatedAtDesc(2L))
                .thenReturn(List.of(active, inactive));
        when(chunks.findByDocumentIdIn(List.of(41L))).thenReturn(List.of(chunk));

        var results = service.retrieveKnowledge(2L, 99L, "adaptive practice", 6);

        assertEquals(1, results.size());
        assertEquals(41L, results.get(0).getDocumentId());
        assertTrue(results.get(0).getContent().contains("practice"));
        verify(chunks).findByDocumentIdIn(List.of(41L));
    }
}
