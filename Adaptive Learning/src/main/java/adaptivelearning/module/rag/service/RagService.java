package adaptivelearning.module.rag.service;

import adaptivelearning.module.rag.dto.request.DocumentUploadRequest;
import adaptivelearning.module.rag.dto.request.KnowledgeBaseUploadRequest;
import adaptivelearning.module.rag.dto.request.KnowledgeDocumentUpdateRequest;
import adaptivelearning.module.rag.dto.response.*;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public interface RagService {
    StudyDocumentResponse uploadDocument(Long userId, DocumentUploadRequest request);
    List<StudyDocumentResponse> listDocuments(Long userId);
    StudyDocumentResponse getDocument(Long userId, Long id);
    SearchResponse search(Long userId, Long documentId, String query);
    void deleteDocument(Long userId, Long id);
    StudyDocumentResponse uploadKnowledgeDocument(Long adminId, KnowledgeBaseUploadRequest request);
    StudyDocumentResponse uploadKnowledgePdf(Long adminId, String title, Long certificateId, Long topicId, MultipartFile file);
    StudyDocumentResponse updateKnowledgeDocument(Long id, KnowledgeDocumentUpdateRequest request);
    List<StudyDocumentResponse> listKnowledgeDocuments();
    KnowledgeDocumentDetailResponse getKnowledgeDocument(Long id);
    StudyDocumentResponse setKnowledgeDocumentActive(Long id, boolean active);
    void deleteKnowledgeDocument(Long id);
    List<KnowledgeContextResponse> retrieveKnowledge(Long certificateId, Long topicId, String query, int limit);
}
