package adaptivelearning.module.exams.service;

import adaptivelearning.module.exams.dto.request.ExamTemplateSectionRequest;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Order(3)
@RequiredArgsConstructor
public class ExamTemplateSectionSchemaMigration implements ApplicationRunner {

    private static final TypeReference<List<ExamTemplateSectionRequest>> SECTION_LIST = new TypeReference<>() {};

    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;

    @Override
    public void run(ApplicationArguments args) {
        addForeignKey("exam_templates", "fk_exam_template_certificate", "certificate_id", "certificates", "certificate_id");
        addForeignKey("exam_templates", "fk_exam_template_topic", "topic_id", "topics", "topic_id");
        addForeignKey("exams", "fk_exam_topic", "topic_id", "topics", "topic_id");
        addForeignKey("exams", "fk_exam_owner_user", "owner_user_id", "tbl_users", "user_id");
        jdbc.update("update exam_templates et join topics t on t.topic_id = et.topic_id " +
                        "set et.target_score_min = t.score_min, et.target_score_max = t.score_max, " +
                        "et.target_score_unit = t.score_unit where et.target_score_min is null " +
                        "and t.score_min is not null and t.score_max is not null and t.score_unit is not null");

        Integer legacyColumnCount = jdbc.queryForObject(
                "select count(*) from information_schema.columns " +
                        "where table_schema = database() and table_name = 'exam_templates' and column_name = 'sections_json'",
                Integer.class);
        if (legacyColumnCount != null && legacyColumnCount > 0) {
            List<LegacyTemplate> legacyTemplates = jdbc.query(
                    "select template_id, topic_id, sections_json from exam_templates where sections_json is not null",
                    (result, row) -> new LegacyTemplate(result.getLong("template_id"), result.getLong("topic_id"),
                            result.getString("sections_json")));
            for (LegacyTemplate legacy : legacyTemplates) {
                Integer existingSections = jdbc.queryForObject(
                        "select count(*) from exam_template_sections where template_id = ?", Integer.class, legacy.id());
                if (existingSections != null && existingSections > 0) continue;
                try {
                    List<ExamTemplateSectionRequest> sections = mapper.readValue(legacy.json(), SECTION_LIST);
                    for (int index = 0; index < sections.size(); index++) {
                        ExamTemplateSectionRequest section = sections.get(index);
                        jdbc.update("insert into exam_template_sections " +
                                        "(template_id, topic_id, section_order, name, question_type, question_count, points_per_question) " +
                                        "values (?, ?, ?, ?, ?, ?, ?)",
                                legacy.id(), legacy.topicId(), index + 1, section.getName(), section.getQuestionType(),
                                section.getQuestionCount(), section.getPointsPerQuestion());
                    }
                } catch (Exception exception) {
                    throw new IllegalStateException("Could not migrate sections for exam template " + legacy.id(), exception);
                }
            }
        }
        jdbc.update("update exam_template_sections s join exam_templates t on t.template_id = s.template_id " +
                "set s.topic_id = t.topic_id where s.topic_id is null");
        jdbc.execute("alter table exam_template_sections modify topic_id bigint not null");
        addForeignKey("exam_template_sections", "fk_exam_template_section_topic", "topic_id", "topics", "topic_id");
        if (legacyColumnCount != null && legacyColumnCount > 0) jdbc.execute("alter table exam_templates drop column sections_json");
    }

    private void addForeignKey(String table, String constraint, String column, String referencedTable,
                               String referencedColumn) {
        Integer exists = jdbc.queryForObject(
                "select count(*) from information_schema.table_constraints " +
                        "where constraint_schema = database() and table_name = ? and constraint_name = ?",
                Integer.class, table, constraint);
        if (exists == null || exists > 0) return;
        jdbc.execute("alter table " + table + " add constraint " + constraint + " foreign key (" + column +
                ") references " + referencedTable + " (" + referencedColumn + ")");
    }

    private record LegacyTemplate(long id, long topicId, String json) {}
}
