package adaptivelearning.module.certificates.service;

import adaptivelearning.module.certificates.entity.Certificate;
import adaptivelearning.module.certificates.repository.CertificateRepository;
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
@Order(1)
public class CertificateCatalogInitializer implements ApplicationRunner {

    private final CertificateRepository certificateRepository;

    @Override
    public void run(ApplicationArguments args) {
        List<Certificate> catalog = List.of(
                certificate("IELTS Academic", "IELTS", "Luyện thi IELTS Academic theo 4 kỹ năng.", 9.0, 6.0, 6.5,
                        "[\"LISTENING\",\"READING\",\"WRITING\",\"SPEAKING\"]"),
                certificate("TOEIC", "TOEIC", "Luyện thi TOEIC Listening & Reading.", 990, 700, 0,
                        "[\"LISTENING\",\"READING\"]"),
                certificate("THPT Quốc Gia", "THPT", "Ôn thi tốt nghiệp THPT theo môn và mục tiêu điểm.", 30, 21, 0,
                        "[\"TOAN\",\"VAN\",\"ANH\",\"LY\",\"HOA\",\"SINH\",\"SU\",\"DIA\"]")
        );
        catalog.stream()
                .filter(item -> !certificateRepository.existsByCode(item.getCode()))
                .forEach(certificateRepository::save);
    }

    private Certificate certificate(String name, String code, String description, double totalScore,
                                    double passingScore, double passingBand, String skills) {
        return Certificate.builder()
                .name(name)
                .code(code)
                .description(description)
                .totalScore(totalScore)
                .passingScore(passingScore)
                .passingBand(passingBand)
                .skills(skills)
                .build();
    }
}
