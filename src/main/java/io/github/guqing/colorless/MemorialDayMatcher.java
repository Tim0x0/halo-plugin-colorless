package io.github.guqing.colorless;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.MonthDay;
import java.time.format.DateTimeParseException;
import java.util.List;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

/**
 * 纪念日匹配。只比较日期，不涉及时刻。
 * 每年重复按月日匹配，指定日期仅匹配当天，清明由独立开关按节气公式计算。
 *
 * @author Tim0x0
 * @since 1.1.0
 */
final class MemorialDayMatcher {

    private MemorialDayMatcher() {
    }

    /**
     * 清明开关命中，或纪念日列表任意一条命中。
     */
    static boolean shouldGray(BasicSetting setting, LocalDate today) {
        if (setting == null || today == null) {
            return false;
        }
        if (BooleanUtils.isTrue(setting.getQingming()) && qingming(today.getYear()).equals(today)) {
            return true;
        }
        return isMemorialDay(setting.getMemorialDays(), today);
    }

    /**
     * 列表中任意一条命中当天。
     */
    static boolean isMemorialDay(List<BasicSetting.MemorialDay> days, LocalDate today) {
        if (CollectionUtils.isEmpty(days) || today == null) {
            return false;
        }
        return days.stream().anyMatch(day -> matches(day, today));
    }

    /**
     * 单条纪念日是否命中当天。未知类型或字段非法时返回 {@code false}。
     */
    static boolean matches(BasicSetting.MemorialDay day, LocalDate today) {
        if (day == null || today == null) {
            return false;
        }
        return switch (StringUtils.trimToEmpty(day.getType())) {
            case BasicSetting.TYPE_YEARLY -> matchesYearly(day, today);
            case BasicSetting.TYPE_ABSOLUTE -> matchesAbsolute(day, today);
            default -> false;
        };
    }

    private static boolean matchesYearly(BasicSetting.MemorialDay day, LocalDate today) {
        Integer month = day.getMonth();
        Integer dayOfMonth = day.getDay();
        if (month == null || dayOfMonth == null) {
            return false;
        }
        if (month == 2 && dayOfMonth == 29) {
            return false;
        }
        try {
            return MonthDay.of(month, dayOfMonth).atYear(today.getYear()).equals(today);
        } catch (DateTimeException e) {
            return false;
        }
    }

    private static boolean matchesAbsolute(BasicSetting.MemorialDay day, LocalDate today) {
        LocalDate date = parseDate(day.getDate());
        return date != null && date.equals(today);
    }

    private static LocalDate parseDate(String raw) {
        String text = StringUtils.trimToEmpty(raw);
        if (text.isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(text);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    /**
     * 计算指定年份的清明公历日。
     * 日 = [Y × 0.2422 + C] − [Y ÷ 4]，Y 为年份后两位，21 世纪 C = 4.81，20 世纪 C = 5.59。
     */
    static LocalDate qingming(int year) {
        int y = year % 100;
        double c = year < 2000 ? 5.59 : 4.81;
        int day = (int) (y * 0.2422 + c) - y / 4;
        return LocalDate.of(year, 4, day);
    }
}
