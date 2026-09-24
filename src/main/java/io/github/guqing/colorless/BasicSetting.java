package io.github.guqing.colorless;

import java.util.List;
import lombok.Data;

/**
 * @author guqing
 * @since 2.0.0
 */
@Data
public class BasicSetting {
    public static final String GROUP = "basic";

    public static final String TYPE_YEARLY = "yearly";
    public static final String TYPE_ABSOLUTE = "absolute";

    public static final String DEFAULT_TIMEZONE = "Asia/Shanghai";

    private Boolean scope;

    /**
     * 开启后立即变灰，直到手动关闭；关闭后仍按清明与纪念日判断。
     */
    private Boolean manual;

    /**
     * 清明当天按节气公式变灰。默认开启。
     */
    private Boolean qingming;

    /**
     * 纪念日列表。命中任意一条即当天变灰。
     */
    private List<MemorialDay> memorialDays;

    /**
     * IANA 时区。表单默认 {@link #DEFAULT_TIMEZONE}；留空或非法时使用 Halo 服务端时区。
     */
    private String timezone;

    /**
     * 一条纪念日。每年重复使用 {@code month}/{@code day}，指定日期使用 {@code date}。
     */
    @Data
    public static class MemorialDay {

        /**
         * 名称，列表折叠时显示此项。
         */
        private String name;

        /**
         * {@code yearly}（每年重复）或 {@code absolute}（指定日期）。
         */
        private String type;

        /**
         * 每年重复的月份，1–12。
         */
        private Integer month;

        /**
         * 每年重复的日。
         */
        private Integer day;

        /**
         * 指定日期，格式 {@code YYYY-MM-DD}。
         */
        private String date;
    }
}
