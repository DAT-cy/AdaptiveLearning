package adaptivelearning.module.attempts.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class IeltsScoreCalculatorTest {

    @Test
    void shouldCalculateListeningBandCorrectly() {
        assertEquals(9.0, IeltsScoreCalculator.calculateListeningBand(40));
        assertEquals(9.0, IeltsScoreCalculator.calculateListeningBand(39));
        assertEquals(8.5, IeltsScoreCalculator.calculateListeningBand(38));
        assertEquals(8.5, IeltsScoreCalculator.calculateListeningBand(37));
        assertEquals(8.0, IeltsScoreCalculator.calculateListeningBand(36));
        assertEquals(8.0, IeltsScoreCalculator.calculateListeningBand(35));
        assertEquals(7.5, IeltsScoreCalculator.calculateListeningBand(34));
        assertEquals(7.5, IeltsScoreCalculator.calculateListeningBand(33));
        assertEquals(7.0, IeltsScoreCalculator.calculateListeningBand(32));
        assertEquals(7.0, IeltsScoreCalculator.calculateListeningBand(30));
        assertEquals(6.5, IeltsScoreCalculator.calculateListeningBand(29));
        assertEquals(6.5, IeltsScoreCalculator.calculateListeningBand(27));
        assertEquals(6.0, IeltsScoreCalculator.calculateListeningBand(26));
        assertEquals(6.0, IeltsScoreCalculator.calculateListeningBand(23));
        assertEquals(5.5, IeltsScoreCalculator.calculateListeningBand(22));
        assertEquals(5.5, IeltsScoreCalculator.calculateListeningBand(20));
        assertEquals(5.0, IeltsScoreCalculator.calculateListeningBand(19));
        assertEquals(5.0, IeltsScoreCalculator.calculateListeningBand(16));
        assertEquals(4.5, IeltsScoreCalculator.calculateListeningBand(15));
        assertEquals(4.5, IeltsScoreCalculator.calculateListeningBand(13));
        assertEquals(4.0, IeltsScoreCalculator.calculateListeningBand(12));
        assertEquals(4.0, IeltsScoreCalculator.calculateListeningBand(10));
        assertEquals(3.5, IeltsScoreCalculator.calculateListeningBand(9));
        assertEquals(3.5, IeltsScoreCalculator.calculateListeningBand(7));
        assertEquals(3.0, IeltsScoreCalculator.calculateListeningBand(6));
        assertEquals(3.0, IeltsScoreCalculator.calculateListeningBand(5));
        assertEquals(2.5, IeltsScoreCalculator.calculateListeningBand(4));
        assertEquals(2.5, IeltsScoreCalculator.calculateListeningBand(3));
        assertEquals(2.0, IeltsScoreCalculator.calculateListeningBand(2));
        assertEquals(0.0, IeltsScoreCalculator.calculateListeningBand(0));
    }

    @Test
    void shouldCalculateReadingGeneralBandCorrectly() {
        assertEquals(9.0, IeltsScoreCalculator.calculateReadingGeneralBand(40));
        assertEquals(8.5, IeltsScoreCalculator.calculateReadingGeneralBand(39));
        assertEquals(8.0, IeltsScoreCalculator.calculateReadingGeneralBand(38));
        assertEquals(7.5, IeltsScoreCalculator.calculateReadingGeneralBand(37));
        assertEquals(7.5, IeltsScoreCalculator.calculateReadingGeneralBand(36));
        assertEquals(7.0, IeltsScoreCalculator.calculateReadingGeneralBand(35));
        assertEquals(7.0, IeltsScoreCalculator.calculateReadingGeneralBand(34));
        assertEquals(6.5, IeltsScoreCalculator.calculateReadingGeneralBand(33));
        assertEquals(6.5, IeltsScoreCalculator.calculateReadingGeneralBand(32));
        assertEquals(6.0, IeltsScoreCalculator.calculateReadingGeneralBand(31));
        assertEquals(6.0, IeltsScoreCalculator.calculateReadingGeneralBand(30));
        assertEquals(5.5, IeltsScoreCalculator.calculateReadingGeneralBand(29));
        assertEquals(5.5, IeltsScoreCalculator.calculateReadingGeneralBand(27));
        assertEquals(5.0, IeltsScoreCalculator.calculateReadingGeneralBand(26));
        assertEquals(5.0, IeltsScoreCalculator.calculateReadingGeneralBand(23));
        assertEquals(4.5, IeltsScoreCalculator.calculateReadingGeneralBand(22));
        assertEquals(4.5, IeltsScoreCalculator.calculateReadingGeneralBand(19));
        assertEquals(4.0, IeltsScoreCalculator.calculateReadingGeneralBand(18));
        assertEquals(4.0, IeltsScoreCalculator.calculateReadingGeneralBand(15));
        assertEquals(3.5, IeltsScoreCalculator.calculateReadingGeneralBand(14));
        assertEquals(3.5, IeltsScoreCalculator.calculateReadingGeneralBand(12));
        assertEquals(3.0, IeltsScoreCalculator.calculateReadingGeneralBand(11));
        assertEquals(3.0, IeltsScoreCalculator.calculateReadingGeneralBand(8));
        assertEquals(2.5, IeltsScoreCalculator.calculateReadingGeneralBand(7));
        assertEquals(2.5, IeltsScoreCalculator.calculateReadingGeneralBand(5));
    }

    @Test
    void shouldCalculateBandScoreBasedOnSkillOrType() {
        assertEquals(7.5, IeltsScoreCalculator.calculateBandScore("IELTS Listening Practice", 33));
        assertEquals(7.0, IeltsScoreCalculator.calculateBandScore("Reading General Training Test 1", 34));
        assertEquals(7.0, IeltsScoreCalculator.calculateBandScore("Reading Academic Section 1", 31));
    }
}

