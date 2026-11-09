package adaptivelearning.module.examimports.dto;

import java.time.LocalDateTime;

public record ExamImportResourceResponse(Long resourceId, String sectionKey, String sectionTitle,
                                         String resourceType, String fileName, String contentType,
                                         Long fileSize, LocalDateTime uploadedAt) {}
