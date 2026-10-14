package adaptivelearning.module.adaptive.dto.response;

import java.util.Map;
import lombok.Data;

@Data
public class BktEstimateResponse {
    private boolean ready;
    private Map<String, Double> masteryBySkill;
    private int trainingStudentCount;
    private int trainingEventCount;
    private String message;
}
