package adaptivelearning.module.rag.controller;

import adaptivelearning.config.response.DefaultRes;
import adaptivelearning.module.rag.dto.request.KnowledgeBaseUploadRequest;
import adaptivelearning.module.rag.dto.request.KnowledgeDocumentActiveRequest;
import adaptivelearning.module.rag.dto.response.KnowledgeDocumentDetailResponse;
import adaptivelearning.module.rag.dto.response.StudyDocumentResponse;
import adaptivelearning.module.rag.service.RagService;
import adaptivelearning.utils.ClientUtils;
import adaptivelearning.utils.SecurityUtils;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ClientUtils.VERSION + "/admin/knowledge-base")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class AdminKnowledgeBaseController {
    private final RagService ragService;

    @PostMapping
    public ResponseEntity<DefaultRes<StudyDocumentResponse>> upload(
            @Valid @RequestBody KnowledgeBaseUploadRequest request) {
        StudyDocumentResponse uploaded = ragService.uploadKnowledgeDocument(
                SecurityUtils.getCurrentUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(DefaultRes.res(HttpStatus.CREATED.value(), "Knowledge imported", uploaded));
    }

    @GetMapping
    public DefaultRes<List<StudyDocumentResponse>> list() {
        return DefaultRes.res(HttpStatus.OK.value(), "Knowledge documents retrieved",
                ragService.listKnowledgeDocuments());
    }

    @GetMapping("/{id}")
    public DefaultRes<KnowledgeDocumentDetailResponse> get(@PathVariable Long id) {
        return DefaultRes.res(HttpStatus.OK.value(), "Knowledge document retrieved",
                ragService.getKnowledgeDocument(id));
    }

    @PatchMapping("/{id}/active")
    public DefaultRes<StudyDocumentResponse> setActive(
            @PathVariable Long id,
            @Valid @RequestBody KnowledgeDocumentActiveRequest request) {
        return DefaultRes.res(HttpStatus.OK.value(), "Knowledge document status updated",
                ragService.setKnowledgeDocumentActive(id, request.getActive()));
    }

    @DeleteMapping("/{id}")
    public DefaultRes<Void> delete(@PathVariable Long id) {
        ragService.deleteKnowledgeDocument(id);
        return DefaultRes.res(HttpStatus.OK.value(), "Knowledge document deleted");
    }
}
