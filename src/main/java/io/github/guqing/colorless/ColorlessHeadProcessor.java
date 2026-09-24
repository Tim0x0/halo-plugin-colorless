package io.github.guqing.colorless;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.ZoneId;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.thymeleaf.context.ITemplateContext;
import org.thymeleaf.model.IModel;
import org.thymeleaf.model.IModelFactory;
import org.thymeleaf.processor.element.IElementModelStructureHandler;
import reactor.core.publisher.Mono;
import run.halo.app.plugin.ReactiveSettingFetcher;
import run.halo.app.theme.dialect.TemplateHeadProcessor;

@Component
@AllArgsConstructor
public class ColorlessHeadProcessor implements TemplateHeadProcessor {

    private static final String INDEX_TEMPLATE_ID = "index";

    private final ReactiveSettingFetcher settingFetcher;

    @Override
    public Mono<Void> process(ITemplateContext context,
                              IModel model,
                              IElementModelStructureHandler structureHandler) {
        return settingFetcher.fetch(BasicSetting.GROUP, BasicSetting.class)
            .defaultIfEmpty(new BasicSetting())
            .doOnNext(basicSetting -> {
                if (!shouldGray(basicSetting)) {
                    return;
                }
                if (!inScope(basicSetting, context)) {
                    return;
                }
                IModelFactory modelFactory = context.getModelFactory();
                model.add(modelFactory.createText(grayscaleStyle()));
            })
            .then();
    }

    private static boolean shouldGray(BasicSetting basicSetting) {
        if (BooleanUtils.isTrue(basicSetting.getManual())) {
            return true;
        }
        LocalDate today = LocalDate.now(resolveZone(basicSetting.getTimezone()));
        return MemorialDayMatcher.shouldGray(basicSetting, today);
    }

    private static boolean inScope(BasicSetting basicSetting, ITemplateContext context) {
        if (BooleanUtils.isTrue(basicSetting.getScope())) {
            return true;
        }
        return INDEX_TEMPLATE_ID.equals(context.getVariable("_templateId"));
    }

    /**
     * 解析 IANA 时区。留空或非法时使用 Halo 服务端时区（{@link ZoneId#systemDefault()}）。
     */
    static ZoneId resolveZone(String timezone) {
        if (StringUtils.isBlank(timezone)) {
            return ZoneId.systemDefault();
        }
        try {
            return ZoneId.of(timezone.trim());
        } catch (DateTimeException e) {
            return ZoneId.systemDefault();
        }
    }

    private static String grayscaleStyle() {
        return """
            <style type="text/css">
              html {
                filter: grayscale(100%);
                -webkit-filter: grayscale(100%);
                -moz-filter: grayscale(100%);
                -ms-filter: grayscale(100%);
                -o-filter: grayscale(100%);
                -webkit-filter: grayscale(1);
              }
            </style>
            """;
    }
}
