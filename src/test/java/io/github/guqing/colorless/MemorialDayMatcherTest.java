package io.github.guqing.colorless;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * 纪念日匹配与时区回退。
 */
class MemorialDayMatcherTest {

    @Test
    void qingmingFormula() {
        assertEquals(LocalDate.of(1999, 4, 5), MemorialDayMatcher.qingming(1999));
        assertEquals(LocalDate.of(2000, 4, 4), MemorialDayMatcher.qingming(2000));
        assertEquals(LocalDate.of(2020, 4, 4), MemorialDayMatcher.qingming(2020));
        assertEquals(LocalDate.of(2021, 4, 4), MemorialDayMatcher.qingming(2021));
        assertEquals(LocalDate.of(2022, 4, 5), MemorialDayMatcher.qingming(2022));
        assertEquals(LocalDate.of(2023, 4, 5), MemorialDayMatcher.qingming(2023));
        assertEquals(LocalDate.of(2024, 4, 4), MemorialDayMatcher.qingming(2024));
        assertEquals(LocalDate.of(2025, 4, 4), MemorialDayMatcher.qingming(2025));
        assertEquals(LocalDate.of(2026, 4, 5), MemorialDayMatcher.qingming(2026));
        assertEquals(LocalDate.of(2027, 4, 5), MemorialDayMatcher.qingming(2027));
        assertEquals(LocalDate.of(2028, 4, 4), MemorialDayMatcher.qingming(2028));
    }

    @Test
    void yearlyMatchesAnyYear() {
        var day = yearly(12, 13);
        assertTrue(matches(day, "2026-12-13"));
        assertTrue(matches(day, "2031-12-13"));
        assertTrue(matches(day, "2000-12-13"));
        assertFalse(matches(day, "2026-12-12"));
        assertFalse(matches(day, "2026-12-14"));
    }

    @Test
    void yearlyIgnoresDateField() {
        var day = yearly(12, 13);
        day.setDate("2026-01-01");
        assertTrue(matches(day, "2026-12-13"));
        assertFalse(matches(day, "2026-01-01"));
    }

    @Test
    void absoluteIgnoresMonthDayFields() {
        var day = absolute("2026-04-04");
        day.setMonth(12);
        day.setDay(13);
        assertTrue(matches(day, "2026-04-04"));
        assertFalse(matches(day, "2026-12-13"));
    }

    @Test
    void absoluteOnlyThatDay() {
        var day = absolute("2026-04-04");
        assertTrue(matches(day, "2026-04-04"));
        assertFalse(matches(day, "2027-04-04"));
        assertFalse(matches(day, "2026-04-05"));
    }

    @Test
    void yearlyFeb29NeverMatches() {
        var day = yearly(2, 29);
        assertFalse(matches(day, "2024-02-29"));
        assertFalse(matches(day, "2025-02-28"));
        assertFalse(matches(day, "2028-02-29"));
    }

    @Test
    void invalidOrIncompleteNeverMatches() {
        assertFalse(matches(null, "2026-12-13"));
        assertFalse(MemorialDayMatcher.matches(yearly(12, 13), null));

        var unknownType = yearly(12, 13);
        unknownType.setType("other");
        assertFalse(matches(unknownType, "2026-12-13"));

        var missingType = yearly(12, 13);
        missingType.setType(null);
        assertFalse(matches(missingType, "2026-12-13"));

        assertFalse(matches(yearly(null, 13), "2026-12-13"));
        assertFalse(matches(yearly(12, null), "2026-12-13"));
        assertFalse(matches(yearly(4, 31), "2026-04-30"));
        assertFalse(matches(yearly(2, 29), "2024-02-29"));
        assertFalse(matches(yearly(13, 1), "2026-01-01"));
        assertFalse(matches(absolute(null), "2026-12-13"));
        assertFalse(matches(absolute(""), "2026-12-13"));
        assertFalse(matches(absolute("  "), "2026-12-13"));
        assertFalse(matches(absolute("2025-02-29"), "2025-02-28"));
        assertFalse(matches(absolute("abc"), "2026-12-13"));
    }

    @Test
    void isMemorialDayAcrossList() {
        var today = LocalDate.of(2026, 9, 30);
        assertFalse(MemorialDayMatcher.isMemorialDay(null, today));
        assertFalse(MemorialDayMatcher.isMemorialDay(List.of(), today));

        var hit = List.of(yearly(12, 13), yearly(9, 30));
        assertTrue(MemorialDayMatcher.isMemorialDay(hit, today));

        var miss = List.of(yearly(12, 13), yearly(7, 7));
        assertFalse(MemorialDayMatcher.isMemorialDay(miss, today));
    }

    @Test
    void shouldGrayHonorsQingmingSwitch() {
        var setting = new BasicSetting();
        setting.setQingming(true);
        setting.setMemorialDays(List.of(yearly(12, 13)));

        assertTrue(MemorialDayMatcher.shouldGray(setting, LocalDate.of(2026, 4, 5)));
        assertFalse(MemorialDayMatcher.shouldGray(setting, LocalDate.of(2026, 4, 4)));
        assertTrue(MemorialDayMatcher.shouldGray(setting, LocalDate.of(2025, 4, 4)));
        assertTrue(MemorialDayMatcher.shouldGray(setting, LocalDate.of(2026, 12, 13)));

        setting.setQingming(false);
        assertFalse(MemorialDayMatcher.shouldGray(setting, LocalDate.of(2026, 4, 5)));
        assertTrue(MemorialDayMatcher.shouldGray(setting, LocalDate.of(2026, 12, 13)));

        assertFalse(MemorialDayMatcher.shouldGray(null, LocalDate.of(2026, 4, 5)));
        assertFalse(MemorialDayMatcher.shouldGray(setting, null));
    }

    @Test
    void resolveZoneFallsBackToServerDefault() {
        ZoneId server = ZoneId.systemDefault();
        assertEquals(server, ColorlessHeadProcessor.resolveZone(null));
        assertEquals(server, ColorlessHeadProcessor.resolveZone(""));
        assertEquals(server, ColorlessHeadProcessor.resolveZone("  "));
        assertEquals(server, ColorlessHeadProcessor.resolveZone("Not/AZone"));
        assertEquals(ZoneId.of("UTC"), ColorlessHeadProcessor.resolveZone("UTC"));
        assertEquals(ZoneId.of("Asia/Tokyo"), ColorlessHeadProcessor.resolveZone(" Asia/Tokyo "));
        assertEquals(ZoneId.of(BasicSetting.DEFAULT_TIMEZONE),
            ColorlessHeadProcessor.resolveZone(BasicSetting.DEFAULT_TIMEZONE));
    }

    private static boolean matches(BasicSetting.MemorialDay day, String today) {
        return MemorialDayMatcher.matches(day, LocalDate.parse(today));
    }

    private static BasicSetting.MemorialDay yearly(Integer month, Integer day) {
        var memorialDay = new BasicSetting.MemorialDay();
        memorialDay.setName("test");
        memorialDay.setType(BasicSetting.TYPE_YEARLY);
        memorialDay.setMonth(month);
        memorialDay.setDay(day);
        return memorialDay;
    }

    private static BasicSetting.MemorialDay absolute(String date) {
        var memorialDay = new BasicSetting.MemorialDay();
        memorialDay.setName("test");
        memorialDay.setType(BasicSetting.TYPE_ABSOLUTE);
        memorialDay.setDate(date);
        return memorialDay;
    }
}
