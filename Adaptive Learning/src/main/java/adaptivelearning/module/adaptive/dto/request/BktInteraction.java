package adaptivelearning.module.adaptive.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BktInteraction {
    private String studentId;
    private String skill;
    private Boolean correct;
}
