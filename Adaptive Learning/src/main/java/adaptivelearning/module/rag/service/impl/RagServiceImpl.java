package adaptivelearning.module.rag.service.impl;

import adaptivelearning.config.response.CommonException;
import adaptivelearning.config.response.ErrorCode;
import adaptivelearning.module.rag.dto.request.DocumentUploadRequest;
import adaptivelearning.module.rag.dto.request.KnowledgeBaseUploadRequest;
import adaptivelearning.module.rag.dto.request.KnowledgeDocumentUpdateRequest;
import adaptivelearning.module.rag.dto.response.ChunkResponse;
import adaptivelearning.module.rag.dto.response.KnowledgeDocumentDetailResponse;
import adaptivelearning.module.rag.dto.response.KnowledgeContextResponse;
import adaptivelearning.module.rag.dto.response.SearchResponse;
import adaptivelearning.module.rag.dto.response.StudyDocumentResponse;
import adaptivelearning.module.rag.entity.DocumentChunk;
import adaptivelearning.module.rag.entity.StudyDocument;
import adaptivelearning.module.rag.repository.DocumentChunkRepository;
import adaptivelearning.module.rag.repository.StudyDocumentRepository;
import adaptivelearning.module.rag.service.RagService;
import adaptivelearning.module.rag.service.PdfTextExtractor;
import adaptivelearning.module.certificates.repository.CertificateRepository;
import adaptivelearning.module.topics.repository.TopicRepository;
import adaptivelearning.shared.enums.DocumentStatus;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@Transactional
@RequiredArgsConstructor
public class RagServiceImpl implements RagService {
    private static final int CHUNK_SIZE = 700;
    private static final int CHUNK_OVERLAP = 120;

    private final StudyDocumentRepository documents;
    private final DocumentChunkRepository chunks;
    private final TopicRepository topics;
    private final CertificateRepository certificates;

    @Override
    public StudyDocumentResponse uploadDocument(Long userId, DocumentUploadRequest request) {
        if (request.getTitle() == null || request.getTitle().isBlank()
                || request.getTitle().trim().length() > 200
                || request.getContent() == null || request.getContent().isBlank()
                || request.getContent().length() > 500_000) {
            throw new CommonException(ErrorCode.BAD_REQUEST);
        }
        if (request.getCertificateId() != null && !certificates.existsById(request.getCertificateId())) {
            throw new CommonException(ErrorCode.ENTITY_NOT_FOUND);
        }
        StudyDocument document = documents.save(StudyDocument.builder()
                .userId(userId)
                .certificateId(request.getCertificateId())
                .title(request.getTitle())
                .fileName(request.getTitle())
                .mimeType("text/markdown")
                .sourceContent(request.getContent().trim())
                .status(DocumentStatus.READY)
                .build());
        saveChunks(document, request.getContent());
        return to(documents.save(document));
    }

    @Override
    @Transactional(readOnly = true)
    public List<StudyDocumentResponse> listDocuments(Long userId) {
        return documents.findByUserIdAndSharedFalseOrderByDocCreatedAtDesc(userId)
                .stream().map(this::to).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public StudyDocumentResponse getDocument(Long userId, Long id) {
        return to(owned(userId, id));
    }

    @Override
    @Transactional(readOnly = true)
    public SearchResponse search(Long userId, Long documentId, String query) {
        owned(userId, documentId);
        String normalized = query == null ? "" : query.trim();
        if (normalized.length() > 2000) throw new CommonException(ErrorCode.BAD_REQUEST);
        List<DocumentChunk> found = Arrays.stream(normalized.split("\\s+"))
                .filter(token -> !token.isBlank())
                .flatMap(token -> chunks.search(documentId, token).stream())
                .distinct()
                .toList();
        return SearchResponse.builder()
                .query(query)
                .documentId(documentId)
                .results(found.stream().map(this::chunk).toList())
                .build();
    }

    @Override
    public void deleteDocument(Long userId, Long id) {
        StudyDocument document = owned(userId, id);
        chunks.deleteByDocumentId(document.getDocumentId());
        documents.delete(document);
    }

    @Override
    public StudyDocumentResponse uploadKnowledgeDocument(
            Long adminId, KnowledgeBaseUploadRequest request) {
        if (request.getFileName() == null || request.getFileName().isBlank()) {
            throw new CommonException(ErrorCode.BAD_REQUEST);
        }
        String fileName = request.getFileName().trim();
        if (request.getTitle() == null || request.getTitle().isBlank()
                || request.getTitle().trim().length() > 200
                || request.getContent() == null || request.getContent().isBlank()
                || request.getContent().length() > 500_000
                || request.getCertificateId() == null || !certificates.existsById(request.getCertificateId())) {
            throw new CommonException(ErrorCode.BAD_REQUEST);
        }
        String lowerName = fileName.toLowerCase(Locale.ROOT);
        if (!lowerName.endsWith(".txt") && !lowerName.endsWith(".md")) {
            throw new CommonException(ErrorCode.BAD_REQUEST);
        }
        if (request.getTopicId() != null) {
            var topic = topics.findById(request.getTopicId())
                    .filter(item -> request.getCertificateId().equals(item.getCertificateId()))
                    .orElseThrow(() -> new CommonException(ErrorCode.BAD_REQUEST));
            if (!topic.isActive()) {
                throw new CommonException(ErrorCode.BAD_REQUEST);
            }
        }

        StudyDocument document = documents.save(StudyDocument.builder()
                .userId(adminId)
                .certificateId(request.getCertificateId())
                .topicId(request.getTopicId())
                .shared(true)
                .title(request.getTitle().trim())
                .fileName(fileName)
                .mimeType(lowerName.endsWith(".md") ? "text/markdown" : "text/plain")
                .sourceContent(request.getContent().trim())
                .status(DocumentStatus.READY)
                .build());
        saveChunks(document, request.getContent());
        return to(documents.save(document));
    }

    @Override
    public StudyDocumentResponse uploadKnowledgePdf(
            Long adminId, String title, Long certificateId, Long topicId, MultipartFile file) {
        if (title == null || title.isBlank() || title.trim().length() > 200) {
            throw new CommonException(ErrorCode.BAD_REQUEST);
        }
        if (certificateId == null || !certificates.existsById(certificateId)) {
            throw new CommonException(ErrorCode.BAD_REQUEST);
        }
        if (topicId != null) {
            var topic = topics.findById(topicId)
                    .filter(item -> certificateId.equals(item.getCertificateId()) && item.isActive())
                    .orElseThrow(() -> new CommonException(ErrorCode.BAD_REQUEST));
        }
        if (file == null || file.isEmpty() || file.getSize() > 20L * 1024 * 1024) {
            throw new CommonException(ErrorCode.BAD_REQUEST);
        }

        String fileName = safeFileName(file.getOriginalFilename());
        if (!fileName.toLowerCase(Locale.ROOT).endsWith(".pdf")) {
            throw new CommonException(ErrorCode.BAD_REQUEST);
        }

        final String text;
        try {
            text = PdfTextExtractor.extract(file.getBytes());
        } catch (java.io.IOException exception) {
            throw new CommonException(ErrorCode.BAD_REQUEST);
        }

        StudyDocument document = documents.save(StudyDocument.builder()
                .userId(adminId)
                .certificateId(certificateId)
                .topicId(topicId)
                .shared(true)
                .title(title.trim())
                .fileName(fileName)
                .mimeType("application/pdf")
                .sourceContent(text)
                .status(DocumentStatus.READY)
                .build());
        saveChunks(document, text);
        return to(documents.save(document));
    }

    @Override
    public StudyDocumentResponse updateKnowledgeDocument(
            Long id, KnowledgeDocumentUpdateRequest request) {
        if (request.getTitle() == null || request.getTitle().isBlank() || request.getTitle().trim().length() > 200
                || request.getContent() == null || request.getContent().isBlank()
                || request.getContent().length() > 500_000 || request.getCertificateId() == null) {
            throw new CommonException(ErrorCode.BAD_REQUEST);
        }
        if (!certificates.existsById(request.getCertificateId())) {
            throw new CommonException(ErrorCode.BAD_REQUEST);
        }
        validateKnowledgeTopic(request.getCertificateId(), request.getTopicId());

        StudyDocument document = sharedDocument(id);
        String content = request.getContent().trim();
        chunks.deleteByDocumentId(id);
        document.setTitle(request.getTitle().trim());
        document.setCertificateId(request.getCertificateId());
        document.setTopicId(request.getTopicId());
        document.setSourceContent(content);
        document.setChunkCount(0);
        saveChunks(document, content);
        return to(documents.save(document));
    }

    private void validateKnowledgeTopic(Long certificateId, Long topicId) {
        if (topicId == null) return;
        topics.findById(topicId)
                .filter(topic -> certificateId.equals(topic.getCertificateId()) && topic.isActive())
                .orElseThrow(() -> new CommonException(ErrorCode.BAD_REQUEST));
    }

    @Override
    @Transactional(readOnly = true)
    public List<StudyDocumentResponse> listKnowledgeDocuments() {
        return documents.findAll().stream()
                .filter(StudyDocument::isShared)
                .sorted(Comparator.comparing(StudyDocument::getDocCreatedAt).reversed())
                .map(this::to)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public KnowledgeDocumentDetailResponse getKnowledgeDocument(Long id) {
        StudyDocument document = sharedDocument(id);
        List<String> documentChunks = chunks.findByDocumentIdOrderByChunkIndexAsc(id).stream()
                .map(DocumentChunk::getContent)
                .toList();
        String content = document.getSourceContent();
        if (content == null || content.isBlank()) {
            content = String.join("\n\n", documentChunks);
        }
        return KnowledgeDocumentDetailResponse.builder()
                .documentId(document.getDocumentId())
                .title(document.getTitle())
                .fileName(document.getFileName())
                .content(content)
                .chunks(documentChunks)
                .certificateId(document.getCertificateId())
                .topicId(document.getTopicId())
                .chunkCount(document.getChunkCount())
                .active(document.isActive())
                .createdAt(document.getDocCreatedAt())
                .build();
    }

    @Override
    public StudyDocumentResponse setKnowledgeDocumentActive(Long id, boolean active) {
        StudyDocument document = sharedDocument(id);
        document.setActive(active);
        return to(documents.save(document));
    }

    @Override
    public void deleteKnowledgeDocument(Long id) {
        StudyDocument document = documents.findById(id)
                .filter(StudyDocument::isShared)
                .orElseThrow(() -> new CommonException(ErrorCode.ENTITY_NOT_FOUND));
        chunks.deleteByDocumentId(document.getDocumentId());
        documents.delete(document);
    }

    @Override
    @Transactional(readOnly = true)
    public List<KnowledgeContextResponse> retrieveKnowledge(
            Long certificateId, Long topicId, String query, int limit) {
        List<StudyDocument> matchingDocuments = documents
                .findByCertificateIdAndSharedTrueOrderByDocCreatedAtDesc(certificateId)
                .stream()
                .filter(StudyDocument::isActive)
                .toList();
        if (matchingDocuments.isEmpty()) return List.of();

        Set<String> terms = terms(query);
        List<Long> documentIds = matchingDocuments.stream()
                .map(StudyDocument::getDocumentId).toList();
        return chunks.findByDocumentIdIn(documentIds).stream()
                .map(source -> new RankedChunk(source, score(source, terms)))
                .filter(item -> item.score() > 0)
                .sorted(Comparator.comparingInt(RankedChunk::score).reversed())
                .limit(Math.max(1, limit))
                .map(item -> {
                    StudyDocument document = matchingDocuments.stream()
                            .filter(candidate -> candidate.getDocumentId()
                                    .equals(item.chunk().getDocumentId()))
                            .findFirst().orElseThrow();
                    return KnowledgeContextResponse.builder()
                            .chunkId(item.chunk().getId())
                            .documentId(document.getDocumentId())
                            .title(document.getTitle())
                            .chunkIndex(item.chunk().getChunkIndex())
                            .content(item.chunk().getContent())
                            .build();
                })
                .toList();
    }

    private void saveChunks(StudyDocument document, String rawText) {
        String text = rawText.replace("\r\n", "\n").trim();
        if (text.isEmpty()) throw new CommonException(ErrorCode.BAD_REQUEST);
        List<DocumentChunk> parts = new ArrayList<>();
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + CHUNK_SIZE, text.length());
            String part = text.substring(start, end).trim();
            if (!part.isEmpty()) {
                parts.add(DocumentChunk.builder()
                        .documentId(document.getDocumentId())
                        .chunkIndex(parts.size())
                        .content(part)
                        .keywords(keywords(part))
                        .build());
            }
            if (end == text.length()) break;
            start = end - CHUNK_OVERLAP;
        }
        chunks.saveAll(parts);
        document.setChunkCount(parts.size());
    }

    private Set<String> terms(String value) {
        if (value == null) return Set.of();
        return Arrays.stream(value.toLowerCase(Locale.ROOT).split("[^\\p{L}\\p{Nd}]+"))
                .filter(term -> term.length() > 2)
                .limit(100)
                .collect(Collectors.toSet());
    }

    private int score(DocumentChunk chunk, Set<String> queryTerms) {
        Set<String> contentTerms = terms(chunk.getContent());
        return (int) queryTerms.stream().filter(contentTerms::contains).count();
    }

    private String keywords(String content) {
        return terms(content).stream().limit(60).collect(Collectors.joining(","));
    }

    private String safeFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            throw new CommonException(ErrorCode.BAD_REQUEST);
        }
        String normalized = fileName.replace('\\', '/');
        return normalized.substring(normalized.lastIndexOf('/') + 1);
    }

    private StudyDocument owned(Long userId, Long id) {
        return documents.findById(id)
                .filter(document -> userId.equals(document.getUserId()) && !document.isShared())
                .orElseThrow(() -> new CommonException(ErrorCode.ENTITY_NOT_FOUND));
    }

    private StudyDocumentResponse to(StudyDocument document) {
        return StudyDocumentResponse.builder()
                .documentId(document.getDocumentId())
                .title(document.getTitle())
                .fileName(document.getFileName())
                .status(document.getStatus())
                .chunkCount(document.getChunkCount())
                .certificateId(document.getCertificateId())
                .topicId(document.getTopicId())
                .shared(document.isShared())
                .active(document.isActive())
                .createdAt(document.getDocCreatedAt())
                .build();
    }

    private StudyDocument sharedDocument(Long id) {
        return documents.findById(id)
                .filter(StudyDocument::isShared)
                .orElseThrow(() -> new CommonException(ErrorCode.ENTITY_NOT_FOUND));
    }

    private ChunkResponse chunk(DocumentChunk documentChunk) {
        String content = documentChunk.getContent();
        return ChunkResponse.builder()
                .id(documentChunk.getId())
                .chunkIndex(documentChunk.getChunkIndex())
                .content(content.length() > 300 ? content.substring(0, 300) : content)
                .build();
    }

    private record RankedChunk(DocumentChunk chunk, int score) {}
}
