package io.github.guqing.colorless;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;
import tools.jackson.databind.json.JsonMapper;

/**
 * 复刻 Halo 读取设置的链路：从 settings.yaml 提取默认值，再用 Jackson 反序列化为 {@link BasicSetting}。
 */
class BasicSettingTest {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    @Test
    void defaultValuesFromSettingsYaml() {
        var setting = fetchLikeHalo(defaultBasicGroupJson());

        assertEquals(Boolean.FALSE, setting.getScope());
        assertEquals(Boolean.FALSE, setting.getManual());
        assertEquals(Boolean.TRUE, setting.getQingming());
        assertEquals("Asia/Shanghai", setting.getTimezone());

        var days = setting.getMemorialDays();
        assertNotNull(days);
        assertEquals(5, days.size());
        assertTrue(days.stream()
            .allMatch(day -> BasicSetting.TYPE_YEARLY.equals(day.getType())));
        assertEquals(
            List.of("汶川地震纪念日", "七七事变纪念日", "九一八事变纪念日", "烈士纪念日", "南京大屠杀死难者国家公祭日"),
            days.stream().map(BasicSetting.MemorialDay::getName).toList());
        assertEquals(
            List.of(5, 7, 9, 9, 12),
            days.stream().map(BasicSetting.MemorialDay::getMonth).toList());
        assertEquals(
            List.of(12, 7, 18, 30, 13),
            days.stream().map(BasicSetting.MemorialDay::getDay).toList());
        assertTrue(days.stream().allMatch(day -> day.getDate() == null));

        assertTrue(MemorialDayMatcher.shouldGray(setting, LocalDate.of(2026, 12, 13)));
        assertTrue(MemorialDayMatcher.shouldGray(setting, LocalDate.of(2026, 9, 30)));
        assertTrue(MemorialDayMatcher.shouldGray(setting, LocalDate.of(2026, 9, 18)));
        assertTrue(MemorialDayMatcher.shouldGray(setting, LocalDate.of(2026, 7, 7)));
        assertTrue(MemorialDayMatcher.shouldGray(setting, LocalDate.of(2026, 5, 12)));
        assertTrue(MemorialDayMatcher.shouldGray(setting, LocalDate.of(2026, 4, 5)));
        assertFalse(MemorialDayMatcher.shouldGray(setting, LocalDate.of(2026, 4, 4)));
        assertFalse(MemorialDayMatcher.shouldGray(setting, LocalDate.of(2026, 9, 21)));
        assertTrue(MemorialDayMatcher.shouldGray(setting, LocalDate.of(2000, 12, 13)));
    }

    @Test
    void consoleJsonDeserializesHiddenFieldsAsEmpty() {
        var setting = fetchLikeHalo("""
            {"scope":false,"manual":false,"qingming":false,"timezone":"",
             "memorialDays":[
               {"name":"测试","type":"absolute","date":"2026-09-21","month":"","day":""},
               {"name":"每年","type":"yearly","month":"5","day":"12","date":""}
             ]}
            """);
        var days = setting.getMemorialDays();

        assertEquals("", setting.getTimezone());
        assertEquals("2026-09-21", days.get(0).getDate());
        assertEquals(5, days.get(1).getMonth());
        assertEquals(12, days.get(1).getDay());
        assertTrue(MemorialDayMatcher.isMemorialDay(days, LocalDate.of(2026, 9, 21)));
        assertFalse(MemorialDayMatcher.isMemorialDay(days, LocalDate.of(2026, 9, 22)));
        assertTrue(MemorialDayMatcher.isMemorialDay(days, LocalDate.of(2026, 5, 12)));
        assertTrue(MemorialDayMatcher.isMemorialDay(days, LocalDate.of(2000, 5, 12)));
        assertFalse(MemorialDayMatcher.isMemorialDay(days, LocalDate.of(2026, 1, 1)));
    }

    private static BasicSetting fetchLikeHalo(String groupJson) {
        return MAPPER.convertValue(MAPPER.readTree(groupJson), BasicSetting.class);
    }

    /**
     * 与 Halo 一致：只取 formSchema 直接子节点中同时含 name 与 value 的项。
     */
    @SuppressWarnings("unchecked")
    private static String defaultBasicGroupJson() {
        try (InputStream in = BasicSettingTest.class
            .getResourceAsStream("/extensions/settings.yaml")) {
            assertNotNull(in, "settings.yaml 不在类路径上");
            Map<String, Object> root = new Yaml().load(in);
            var spec = (Map<String, Object>) root.get("spec");
            var forms = (List<Map<String, Object>>) spec.get("forms");
            var basic = forms.stream()
                .filter(form -> BasicSetting.GROUP.equals(form.get("group")))
                .findFirst()
                .orElseThrow(() -> new AssertionError("settings.yaml 缺少 basic 分组"));
            var schema = (List<Map<String, Object>>) basic.get("formSchema");
            Map<String, Object> defaults = new LinkedHashMap<>();
            for (var node : schema) {
                if (node.containsKey("name") && node.containsKey("value")) {
                    defaults.put((String) node.get("name"), node.get("value"));
                }
            }
            return MAPPER.writeValueAsString(defaults);
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }
}
