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
                new TopicSeed("IELTS", "Listening", "IELTS_LISTENING", TopicType.SKILL),
                new TopicSeed("IELTS", "Reading", "IELTS_READING", TopicType.SKILL),
                new TopicSeed("IELTS", "Writing", "IELTS_WRITING", TopicType.SKILL),
                new TopicSeed("IELTS", "Speaking", "IELTS_SPEAKING", TopicType.SKILL),
                new TopicSeed("TOEIC", "Listening", "TOEIC_LISTENING", TopicType.SKILL),
                new TopicSeed("TOEIC", "Reading", "TOEIC_READING", TopicType.SKILL),
                new TopicSeed("THPT", "Toán", "THPT_TOAN", TopicType.SUBJECT),
                new TopicSeed("THPT", "Ngữ văn", "THPT_VAN", TopicType.SUBJECT),
                new TopicSeed("THPT", "Tiếng Anh", "THPT_ANH", TopicType.SUBJECT),
                new TopicSeed("THPT", "Vật lý", "THPT_LY", TopicType.SUBJECT),
                new TopicSeed("THPT", "Hóa học", "THPT_HOA", TopicType.SUBJECT),
                new TopicSeed("THPT", "Sinh học", "THPT_SINH", TopicType.SUBJECT),
                new TopicSeed("THPT", "Lịch sử", "THPT_SU", TopicType.SUBJECT),
                new TopicSeed("THPT", "Địa lý", "THPT_DIA", TopicType.SUBJECT)
        );

        for (TopicSeed seed : catalog) {
            var certificate = certificateRepository.findByCode(seed.certificateCode()).orElse(null);
            if (certificate == null || topicRepository.existsByNameAndCertificateId(seed.name(), certificate.getCertificateId())) {
                continue;
            }
            topicRepository.save(Topic.builder()
                    .name(seed.name())
                    .code(seed.code())
                    .description("Danh mục " + seed.name() + " thuộc " + certificate.getName())
                    .type(seed.type())
                    .certificateId(certificate.getCertificateId())
                    .sortOrder(0)
                    .isActive(true)
                    .build());
        }
    }

    private record TopicSeed(String certificateCode, String name, String code, TopicType type) {}
}
