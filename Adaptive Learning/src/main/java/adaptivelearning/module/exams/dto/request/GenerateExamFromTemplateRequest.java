package adaptivelearning.module.exams.dto.request;

import adaptivelearning.shared.enums.QuestionDifficulty;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Data;

@Data
public class GenerateExamFromTemplateRequest {

    @NotEmpty
    private List<@NotNull QuestionDifficulty> sectionDifficulties;
}
