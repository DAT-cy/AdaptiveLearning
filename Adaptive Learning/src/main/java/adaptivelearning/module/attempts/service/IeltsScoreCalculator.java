package adaptivelearning.module.attempts.service;

public final class IeltsScoreCalculator {

    private IeltsScoreCalculator() {}

    /**
     * Quy đổi điểm IELTS Listening theo bảng chuẩn (Academic & General Training)
     * Thang 40 câu
     */
    public static double calculateListeningBand(int correctCount) {
        if (correctCount >= 39) return 9.0;
        if (correctCount >= 37) return 8.5;
        if (correctCount >= 35) return 8.0;
        if (correctCount >= 33) return 7.5;
        if (correctCount >= 30) return 7.0;
        if (correctCount >= 27) return 6.5;
        if (correctCount >= 23) return 6.0;
        if (correctCount >= 20) return 5.5;
        if (correctCount >= 16) return 5.0;
        if (correctCount >= 13) return 4.5;
        if (correctCount >= 10) return 4.0;
        if (correctCount >= 7)  return 3.5;
        if (correctCount >= 5)  return 3.0;
        if (correctCount >= 3)  return 2.5;
        if (correctCount >= 1)  return 2.0;
        return 0.0;
    }

    /**
     * Quy đổi điểm IELTS Reading Academic theo bảng chuẩn
     * Thang 40 câu
     */
    public static double calculateReadingAcademicBand(int correctCount) {
        if (correctCount >= 39) return 9.0;
        if (correctCount >= 37) return 8.5;
        if (correctCount >= 35) return 8.0;
        if (correctCount >= 33) return 7.5;
        if (correctCount >= 30) return 7.0;
        if (correctCount >= 27) return 6.5;
        if (correctCount >= 23) return 6.0;
        if (correctCount >= 20) return 5.5;
        if (correctCount >= 16) return 5.0;
        if (correctCount >= 13) return 4.5;
        if (correctCount >= 10) return 4.0;
        if (correctCount >= 7)  return 3.5;
        if (correctCount >= 5)  return 3.0;
        if (correctCount >= 3)  return 2.5;
        if (correctCount >= 1)  return 2.0;
        return 0.0;
    }

    /**
     * Quy đổi điểm IELTS Reading General Training theo bảng chuẩn
     * Thang 40 câu
     */
    public static double calculateReadingGeneralBand(int correctCount) {
        if (correctCount >= 40) return 9.0;
        if (correctCount >= 39) return 8.5;
        if (correctCount >= 38) return 8.0;
        if (correctCount >= 36) return 7.5;
        if (correctCount >= 34) return 7.0;
        if (correctCount >= 32) return 6.5;
        if (correctCount >= 30) return 6.0;
        if (correctCount >= 27) return 5.5;
        if (correctCount >= 23) return 5.0;
        if (correctCount >= 19) return 4.5;
        if (correctCount >= 15) return 4.0;
        if (correctCount >= 12) return 3.5;
        if (correctCount >= 8)  return 3.0;
        if (correctCount >= 5)  return 2.5;
        if (correctCount >= 1)  return 2.0;
        return 0.0;
    }

    /**
     * Tự động tính Band Score IELTS theo loại kỹ năng (Skill / Section)
     */
    public static Double calculateBandScore(String skillOrType, int correctCount) {
        if (skillOrType == null) {
            return calculateReadingAcademicBand(correctCount);
        }
        String normalized = skillOrType.trim().toUpperCase();
        if (normalized.contains("LISTEN")) {
            return calculateListeningBand(correctCount);
        }
        if (normalized.contains("GENERAL") || normalized.contains("GT")) {
            return calculateReadingGeneralBand(correctCount);
        }
        return calculateReadingAcademicBand(correctCount);
    }
}

