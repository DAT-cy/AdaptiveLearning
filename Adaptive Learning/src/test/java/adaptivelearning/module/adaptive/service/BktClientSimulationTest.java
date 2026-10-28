package adaptivelearning.module.adaptive.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import adaptivelearning.module.adaptive.dto.request.BktEstimateRequest;
import adaptivelearning.module.adaptive.dto.request.BktInteraction;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

class BktClientSimulationTest {

    private String simulationBaseUrl;

    @BeforeEach
    void requireSimulationUrl() {
        simulationBaseUrl = System.getenv("BKT_SIM_BASE_URL");
        Assumptions.assumeTrue(simulationBaseUrl != null && !simulationBaseUrl.isBlank(),
                "Set BKT_SIM_BASE_URL when running against the Postman pyBKT simulation");
    }

    @Test
    void mapsReadyEstimateResponseFromHttpService() {
        var response = client(null, Duration.ofSeconds(10)).estimate(request(2));

        assertTrue(response.isPresent());
        assertTrue(response.get().isReady(), response.get().toString());
        assertEquals(2, response.get().getTrainingStudentCount());
        assertEquals(4, response.get().getTrainingEventCount());
        assertEquals(0.75, response.get().getMasteryBySkill().get("TOPIC_1"), 0.001);
    }

    @Test
    void preservesNotReadyResponseAsAValidNonErrorResult() {
        var response = client(null, Duration.ofSeconds(10)).estimate(request(1));

        assertTrue(response.isPresent());
        assertFalse(response.get().isReady());
        assertTrue(response.get().getMasteryBySkill().isEmpty());
        assertEquals(1, response.get().getTrainingStudentCount());
    }

    @Test
    void providerHttpErrorFallsBackToEmptyOptional() {
        var response = client("failure", Duration.ofSeconds(10)).estimate(request(2));

        assertTrue(response.isEmpty());
    }

    @Test
    void providerTimeoutFallsBackToEmptyOptional() {
        var response = client("timeout", Duration.ofMillis(100)).estimate(request(2));

        assertTrue(response.isEmpty());
    }

    private BktClient client(String mode, Duration timeout) {
        WebClient.Builder builder = WebClient.builder();
        if (mode != null) builder.defaultHeader("x-test-response", mode);
        return new BktClient(builder, simulationBaseUrl, timeout);
    }

    private BktEstimateRequest request(int trainingLearners) {
        List<BktInteraction> training = trainingLearners == 1
                ? List.of(interaction("learner-1", true), interaction("learner-1", false))
                : List.of(interaction("learner-1", true), interaction("learner-1", false),
                        interaction("learner-2", true), interaction("learner-2", true));
        return BktEstimateRequest.builder()
                .learnerId("learner-3")
                .trainingInteractions(training)
                .learnerInteractions(List.of(interaction("learner-3", true)))
                .build();
    }

    private BktInteraction interaction(String studentId, boolean correct) {
        return BktInteraction.builder().studentId(studentId).skill("TOPIC_1").correct(correct).build();
    }
}
