package adaptivelearning.module.billing.service;

import adaptivelearning.module.users.repository.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LegacyQuotaInitializer implements ApplicationRunner {
    private final UserMapper userMapper;

    @Value("${FREE_EXAM_LIMIT:5}")
    private int freeExamLimit;

    @Override
    public void run(ApplicationArguments args) {
        userMapper.initializeLegacyFreeExamUsage(Math.max(1, freeExamLimit));
    }
}
