package adaptivelearning.module.reports.service;

import adaptivelearning.module.reports.repository.ReportRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReportService {
    private static final ZoneId VN = ZoneId.of("Asia/Ho_Chi_Minh");
    private final ReportRepository repository;

    public Map<String, Object> report(LocalDate from, LocalDate to) {
        if (from == null || to == null || to.isBefore(from)) throw new IllegalArgumentException("Khoảng ngày không hợp lệ");
        LocalDateTime start = from.atStartOfDay(VN).toLocalDateTime();
        LocalDateTime end = to.plusDays(1).atStartOfDay(VN).toLocalDateTime();
        Object[] totals = repository.summary(start, end);
        long attempts = number(totals[0]);
        long graded = number(totals[2]);
        long inProgress = number(totals[3]);
        List<Object[]> days = repository.daily(start, end);
        List<Object[]> exams = repository.exams(start, end);
        List<Object[]> students = repository.students(start, end);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("from", from);
        result.put("to", to);
        result.put("kpis", Map.of("attempts", attempts, "averageScore", decimal(totals[1]),
                "graded", graded, "inProgress", inProgress,
                "completionRate", attempts == 0 ? 0d : graded * 100d / attempts));
        result.put("daily", days.stream().map(r -> Map.of("date", r[0].toString(), "attempts", number(r[1]),
                "averageScore", decimal(r[2]), "graded", number(r[3]), "inProgress", number(r[4]))).toList());
        result.put("exams", exams.stream().map(r -> Map.of("examId", r[0], "title", r[1], "attempts", number(r[2]),
                "averageScore", decimal(r[3]), "passed", number(r[4]), "graded", number(r[5]),
                "passRate", number(r[5]) == 0 ? 0d : number(r[4]) * 100d / number(r[5]))).toList());
        result.put("students", students.stream().map(r -> Map.of("userId", r[0], "name", r[1] == null ? "" : r[1],
                "username", r[2], "attempts", number(r[3]), "averageScore", decimal(r[4]),
                "lastActivity", r[5] == null ? "" : r[5].toString())).toList());
        return result;
    }

    public static double completionRate(long graded, long total) { return total == 0 ? 0d : graded * 100d / total; }
    public static double passRate(long passed, long graded) { return graded == 0 ? 0d : passed * 100d / graded; }
    private static long number(Object value) { return value == null ? 0L : ((Number) value).longValue(); }
    private static double decimal(Object value) { return value == null ? 0d : ((Number) value).doubleValue(); }
}
