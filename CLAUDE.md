# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概述

Halo CMS 插件「无色界」（`plugin-colorless`）：在纪念日（默认预填中国公祭日）当天或手动开启时，向主题页面 `<head>` 注入一段 `filter: grayscale(100%)` 样式，让站点显示为灰白效果。纯后端插件，**没有前端 UI 子项目**。

## 常用命令

```bash
./gradlew build --offline     # 构建并运行测试，产物 build/libs/halo-plugin-colorless-<version>.jar
./gradlew test --offline      # 只运行单元测试
./gradlew haloServer          # 用 Docker 启动 Halo 2.26 并加载本插件（http://localhost:8090/console，admin/admin）
./gradlew reloadPlugin        # 热重载插件（haloServer 运行中）
./gradlew watch               # 监听变更自动重载
```

- 本机依赖缓存已齐全（Gradle 9.4.0、platform 2.26.0、devtools 0.8.0、lombok 9.2.0），**优先带 `--offline`**，避免无谓下载。
- **本机没有 Docker**，`haloServer` / `reloadPlugin` / `watch` 跑不起来；运行时行为（Console 表单渲染、`<head>` 注入）只能把 jar 装到真实 Halo 上验证。
- 单元测试（JUnit 5）不依赖 Halo 运行时：`MemorialDayMatcherTest` 覆盖日期匹配逻辑，`BasicSettingTest` 按 Halo 的方式加载 `settings.yaml` 默认值并反序列化成 `BasicSetting`。
- Gradle wrapper / devtools / lombok 版本与同目录的 `halo-plugin-bbs` 保持一致，升级时一起改。

## 架构

只有四个 Java 类，一条链路：

1. `ColorlessHeadProcessor` 实现 Halo 的 `TemplateHeadProcessor` 扩展点，每次渲染主题模板时被调用。它通过 `ReactiveSettingFetcher.fetch(BasicSetting.GROUP, BasicSetting.class)` 读取设置，按顺序判断：`manual` 未开启且今天不是清明、也不命中任何纪念日 → 跳过；`scope` 为「仅首页」且当前 `_templateId != "index"` → 跳过；否则注入 `<style>`。「今天」取 `LocalDate.now(timezone)`，时区留空或非法时使用 Halo 服务端时区（`ZoneId.systemDefault()`）。
2. `MemorialDayMatcher` 是纯静态的日期匹配逻辑，只比较 `LocalDate`，不涉及时刻。每天独立判断：`qingming` 开关命中，或列表任意一条命中，即变灰。
3. `BasicSetting` 是设置的 POJO（含内嵌 `MemorialDay`），字段名必须与 `settings.yaml` 中 `group: basic` 下的 FormKit `name` 一一对应；新增设置项时两边同时改。
4. `ColorlessPlugin` 仅是 `BasePlugin` 的空壳，负责让 Halo 识别插件主类。

### 日期语义

清明不进纪念日列表，用顶层开关 `qingming`（默认 true），寿星公式 `[Y×0.2422+C]−[Y/4]`（21 世纪 C=4.81）算出当天。

每条纪念日用 `type` 区分语义，选择控件按类型显隐：

- `yearly`：`month` / `day` 两个 select。日期下拉按月份限制天数（31 / 30 / 2 月到 28）。2 月 29 日不是每年都有，表单不提供，匹配时也忽略。月日非法则整条忽略。匹配时只读 `month`/`day`。
- `absolute`：`$formkit: date` 字段 `date`（YYYY-MM-DD），仅当天。无法解析则整条忽略。匹配时只读 `date`。

纪念日列表用 `$formkit: array`（官方文档已将 `repeater` 标为过时）。子项条件对齐官方主题：`if: "$value.type === yearly"`，比较值不加引号；月份用 `==`。不要用 `$get(id)`：`array` 会再挂一份隐藏的 children 表单，id 会重复。`itemLabels` 只展示 `$value.name`：Halo 不能按类型切换字段，把 `month`/`day`/`date` 都写上去时，指定日期的空月日仍会渲染成空 `<span>`，日期前面就会空出一截。不要为了列表格式加 `monthDay` 字符串字段。清明开关和纪念日列表保持顶层字段，不要包进 `$formkit: group`，否则会嵌套 JSON，Java 端对不上。连续多天用多条单日。时区是可搜索 select，默认 `Asia/Shanghai`，留空或非法使用服务端时区。

改日期语义时同步改 `MemorialDayMatcherTest`、`settings.yaml` 的表单与 `help`、README 的「纪念日」一节。

### 设置与默认值

- 默认值只维护在 `settings.yaml` 的 `value` 里（`array` 的默认列表写在容器节点上）。Halo 每次加载插件都会以这些默认值为基底、用已有 ConfigMap 做 JSON merge patch，Java 端不需要再写一份默认值。
- **不做旧数据兼容**：设置项改名或删除时直接改，不留兼容字段、忽略逻辑或升级说明。
- 每年重复用 `month`/`day`，指定日期用 `date`，按 `type` 条件显隐。select 的 value 用字符串，Jackson 会转成 `Integer`。默认 5 条公祭日为 yearly 的 month/day：5/12、7/7、9/18、9/30、12/13。
- `plugin.yaml` 中 `settingName` / `configMapName` 与 `settings.yaml` 的 `metadata.name` 绑定。
- 停用插件即总开关；设置页不再另做 enable。

## 仓库关系

本仓库是上游 `guqing/halo-plugin-colorless` 的独立分支（上游已停止维护，不回推 PR），已移除应用市场 app-id，不向 Halo 应用市场发布。Java 包名沿用上游的 `io.github.guqing.colorless`。`plugin.yaml` 的作者与仓库链接指向 Tim0x0；README 保留对原作者的致谢。

## 版本约束

- Halo platform `2.26.0`，`plugin.yaml` 的 `spec.requires: ">=2.26.0"`，两者需同步升级。
- Java 21（toolchain + `options.release = 21`）。
- CI/CD 使用 `halo-sigs/reusable-workflows@v4`，`skip-node-setup: true`；CD 仅把 jar 附到 GitHub Release（`skip-appstore-release: true`），无需任何 secret。
