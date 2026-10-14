package adaptivelearning.module.adaptive.dto.request;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class BktEstimateRequest {
    String learnerId;
    List<BktInteraction> trainingInteractions;
    List<BktInteraction> learnerInteractions;
}
