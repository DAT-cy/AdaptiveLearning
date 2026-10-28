package adaptivelearning.module.reports.repository;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;
import adaptivelearning.module.attempts.entity.ExamAttempt;

public interface ReportRepository extends Repository<ExamAttempt, Long> {
    @Query("select function('date', a.startedAt), count(a), avg(a.score), " +
            "sum(case when a.status = adaptivelearning.shared.enums.AttemptStatus.GRADED then 1 else 0 end), " +
            "sum(case when a.status = adaptivelearning.shared.enums.AttemptStatus.IN_PROGRESS then 1 else 0 end) " +
            "from ExamAttempt a where a.startedAt >= :from and a.startedAt < :until " +
            "group by function('date', a.startedAt) order by function('date', a.startedAt)")
    List<Object[]> daily(@Param("from") LocalDateTime from, @Param("until") LocalDateTime until);

    @Query("select a.examId, e.title, count(a), avg(a.score), " +
            "sum(case when a.status = adaptivelearning.shared.enums.AttemptStatus.GRADED and a.score >= e.passingScore then 1 else 0 end), " +
            "sum(case when a.status = adaptivelearning.shared.enums.AttemptStatus.GRADED then 1 else 0 end) " +
            "from ExamAttempt a join adaptivelearning.module.exams.entity.Exam e on e.examId = a.examId " +
            "where a.startedAt >= :from and a.startedAt < :until group by a.examId, e.title, e.passingScore " +
            "order by count(a) desc")
    List<Object[]> exams(@Param("from") LocalDateTime from, @Param("until") LocalDateTime until);

    @Query("select a.userId, u.fullName, u.username, count(a), avg(a.score), max(a.startedAt) " +
            "from ExamAttempt a join adaptivelearning.module.users.entity.User u on u.userId = a.userId " +
            "where a.startedAt >= :from and a.startedAt < :until group by a.userId, u.fullName, u.username " +
            "order by count(a) desc")
    List<Object[]> students(@Param("from") LocalDateTime from, @Param("until") LocalDateTime until);

    @Query("select count(a), avg(a.score), " +
            "sum(case when a.status = adaptivelearning.shared.enums.AttemptStatus.GRADED then 1 else 0 end), " +
            "sum(case when a.status = adaptivelearning.shared.enums.AttemptStatus.IN_PROGRESS then 1 else 0 end) " +
            "from ExamAttempt a where a.startedAt >= :from and a.startedAt < :until")
    Object[] summary(@Param("from") LocalDateTime from, @Param("until") LocalDateTime until);
}
