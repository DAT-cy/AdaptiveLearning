package adaptivelearning.module.topics.service;

import adaptivelearning.module.certificates.repository.CertificateRepository;
import adaptivelearning.module.topics.entity.Topic;
import adaptivelearning.module.topics.repository.TopicRepository;
import adaptivelearning.shared.enums.TopicType;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Profile("local")
@RequiredArgsConstructor
@Order(2)
public class TopicCatalogInitializer implements ApplicationRunner {

    private final CertificateRepository certificateRepository;
    private final TopicRepository topicRepository;

    @Override
    public void run(ApplicationArguments args) {
        List<TopicSeed> catalog = List.of(
                new TopicSeed("IELTS", "Listening", "IELTS_LISTENING", TopicType.SKILL, 1, 9, 0.5, "band"),
                new TopicSeed("IELTS", "Reading", "IELTS_READING", TopicType.SKILL, 1, 9, 0.5, "band"),
                new TopicSeed("IELTS", "Writing", "IELTS_WRITING", TopicType.SKILL, 1, 9, 0.5, "band"),
                new TopicSeed("IELTS", "Speaking", "IELTS_SPEAKING", TopicType.SKILL, 1, 9, 0.5, "band"),
                new TopicSeed("TOEIC", "Listening", "TOEIC_LISTENING", TopicType.SKILL, 5, 495, 5, "points"),
                new TopicSeed("TOEIC", "Reading", "TOEIC_READING", TopicType.SKILL, 5, 495, 5, "points"),
                new TopicSeed("THPT", "Toán", "THPT_TOAN", TopicType.SUBJECT, 0, 10, 0.25, "points"),
                new TopicSeed("THPT", "Ngữ văn", "THPT_VAN", TopicType.SUBJECT, 0, 10, 0.25, "points"),
                new TopicSeed("THPT", "Tiếng Anh", "THPT_ANH", TopicType.SUBJECT, 0, 10, 0.25, "points"),
                new TopicSeed("THPT", "Vật lý", "THPT_LY", TopicType.SUBJECT, 0, 10, 0.25, "points"),
                new TopicSeed("THPT", "Hóa học", "THPT_HOA", TopicType.SUBJECT, 0, 10, 0.25, "points"),
                new TopicSeed("THPT", "Sinh học", "THPT_SINH", TopicType.SUBJECT, 0, 10, 0.25, "points"),
                new TopicSeed("THPT", "Lịch sử", "THPT_SU", TopicType.SUBJECT, 0, 10, 0.25, "points"),
                new TopicSeed("THPT", "Địa lý", "THPT_DIA", TopicType.SUBJECT, 0, 10, 0.25, "points")
        );

        for (TopicSeed seed : catalog) {
            var certificate = certificateRepository.findByCode(seed.certificateCode()).orElse(null);
            if (certificate == null) continue;
            var existing = topicRepository.findByCodeAndCertificateId(seed.code(), certificate.getCertificateId());
            if (existing.isPresent()) {
                Topic topic = existing.get();
                if (topic.getScoreMin() == null || topic.getScoreMax() == null || topic.getScoreStep() == null) {
                    topic.setScoreMin(seed.scoreMin());
                    topic.setScoreMax(seed.scoreMax());
                    topic.setScoreStep(seed.scoreStep());
                    topic.setScoreUnit(seed.scoreUnit());
                    topicRepository.save(topic);
                }
                continue;
            }
            if (topicRepository.existsByNameAndCertificateId(seed.name(), certificate.getCertificateId())) continue;
            topicRepository.save(Topic.builder()
                    .name(seed.name())
                    .code(seed.code())
                    .description("Danh mục " + seed.name() + " thuộc " + certificate.getName())
                    .type(seed.type())
                    .certificateId(certificate.getCertificateId())
                    .sortOrder(0)
                    .isActive(true)
                    .scoreMin(seed.scoreMin())
                    .scoreMax(seed.scoreMax())
                    .scoreStep(seed.scoreStep())
                    .scoreUnit(seed.scoreUnit())
                    .build());
        }
    }

    private record TopicSeed(String certificateCode, String name, String code, TopicType type,
                             double scoreMin, double scoreMax, double scoreStep, String scoreUnit) {}
}
