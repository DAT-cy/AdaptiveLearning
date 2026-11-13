package adaptivelearning.module.certificates.service.impl;

import adaptivelearning.config.response.CommonException;
import adaptivelearning.config.response.ErrorCode;
import adaptivelearning.module.certificates.dto.request.CertificateRequest;
import adaptivelearning.module.certificates.dto.response.CertificateResponse;
import adaptivelearning.module.certificates.entity.Certificate;
import adaptivelearning.module.certificates.repository.CertificateRepository;
import adaptivelearning.module.certificates.service.CertificateService;
import adaptivelearning.module.topics.entity.Topic;
import adaptivelearning.module.topics.repository.TopicRepository;
import adaptivelearning.shared.enums.TopicType;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class CertificateServiceImpl implements CertificateService {

    private final CertificateRepository certificateRepository;
    private final TopicRepository topicRepository;
    private final ObjectMapper objectMapper;

    @Override
    public List<CertificateResponse> getAll() {
        return certificateRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public CertificateResponse getById(Long id) {
        Certificate certificate = findOrThrow(id);
        return toResponse(certificate);
    }

    @Override
    public CertificateResponse create(CertificateRequest request) {
        List<String> skills = parseSkills(request.getSkills());
        Certificate certificate = toEntity(request, skills);
        Certificate saved = certificateRepository.save(certificate);
        syncSkillTopics(saved, skills, null);
        log.info("Created certificate: {}", saved.getName());
        return toResponse(saved);
    }

    @Override
    public CertificateResponse update(Long id, CertificateRequest request) {
        Certificate certificate = findOrThrow(id);
        List<String> skills = parseSkills(request.getSkills());
        String previousCode = certificate.getCode();
        certificate.setName(request.getName());
        certificate.setCode(request.getCode());
        certificate.setDescription(request.getDescription());
        certificate.setTotalScore(request.getTotalScore());
        certificate.setPassingScore(request.getPassingScore());
        certificate.setPassingBand(request.getPassingBand());
        certificate.setSkills(toSkillsJson(skills));
        Certificate saved = certificateRepository.save(certificate);
        syncSkillTopics(saved, skills, previousCode);
        log.info("Updated certificate id={}", saved.getCertificateId());
        return toResponse(saved);
    }

    @Override
    public void delete(Long id) {
        Certificate certificate = findOrThrow(id);
        certificateRepository.delete(certificate);
        log.info("Deleted certificate id={}", id);
    }

    private Certificate findOrThrow(Long id) {
        return certificateRepository.findById(id)
                .orElseThrow(() -> new CommonException(ErrorCode.ENTITY_NOT_FOUND));
    }

    private Certificate toEntity(CertificateRequest request, List<String> skills) {
        return Certificate.builder()
                .name(request.getName())
                .code(request.getCode())
                .description(request.getDescription())
                .totalScore(request.getTotalScore())
                .passingScore(request.getPassingScore())
                .passingBand(request.getPassingBand())
                .skills(toSkillsJson(skills))
                .build();
    }

    private List<String> parseSkills(String rawSkills) {
        try {
            List<String> parsed = objectMapper.readValue(rawSkills == null ? "[]" : rawSkills, new TypeReference<>() {});
            return parsed.stream()
                    .filter(skill -> skill != null && !skill.isBlank())
                    .map(String::trim)
                    .collect(java.util.stream.Collectors.collectingAndThen(
                            java.util.stream.Collectors.toCollection(LinkedHashSet::new), List::copyOf));
        } catch (Exception exception) {
            throw new IllegalArgumentException("Skills must be a JSON array of non-empty strings.");
        }
    }

    private String toSkillsJson(List<String> skills) {
        try {
            return objectMapper.writeValueAsString(skills);
        } catch (Exception exception) {
            throw new IllegalStateException("Could not serialize certificate skills.", exception);
        }
    }

    private void syncSkillTopics(Certificate certificate, List<String> skills, String previousCertificateCode) {
        String codePrefix = normalizeCode(certificate.getCode()) + "_";
        String previousCodePrefix = previousCertificateCode == null ? codePrefix : normalizeCode(previousCertificateCode) + "_";
        Set<String> configuredCodes = new LinkedHashSet<>();
        List<Topic> existingTopics = topicRepository.findByCertificateId(certificate.getCertificateId());

        for (int index = 0; index < skills.size(); index++) {
            String skill = skills.get(index);
            String topicCode = codePrefix + normalizeCode(skill);
            configuredCodes.add(topicCode);
            Topic topic = topicRepository.findByCodeAndCertificateId(topicCode, certificate.getCertificateId())
                    .orElseGet(() -> Topic.builder()
                            .name(skill)
                            .code(topicCode)
                            .certificateId(certificate.getCertificateId())
                            .type(TopicType.SKILL)
                            .description("Kỹ năng " + skill + " thuộc " + certificate.getName())
                            .build());
            topic.setName(skill);
            topic.setType(TopicType.SKILL);
            topic.setParentId(null);
            topic.setSortOrder(index);
            topic.setActive(true);
            topicRepository.save(topic);
        }

        existingTopics.stream()
                .filter(topic -> topic.getParentId() == null && topic.getType() == TopicType.SKILL)
                .filter(topic -> topic.getCode() != null
                        && (topic.getCode().startsWith(codePrefix) || topic.getCode().startsWith(previousCodePrefix)))
                .filter(topic -> !configuredCodes.contains(topic.getCode()))
                .forEach(topic -> topic.setActive(false));
    }

    private String normalizeCode(String value) {
        return value.trim().toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
    }

    private CertificateResponse toResponse(Certificate certificate) {
        return CertificateResponse.builder()
                .certificateId(certificate.getCertificateId())
                .name(certificate.getName())
                .code(certificate.getCode())
                .description(certificate.getDescription())
                .totalScore(certificate.getTotalScore())
                .passingScore(certificate.getPassingScore())
                .passingBand(certificate.getPassingBand())
                .skills(certificate.getSkills())
                .status(certificate.getStatus())
                .build();
    }
}
