package io.github.kentasun.stepflow.jexl;

import io.github.kentasun.stepflow.api.step.StepHandlerCustomizer;
import io.github.kentasun.stepflow.jexl.dto.JexlStepHandlerProperties;
import org.apache.commons.jexl3.JexlArithmetic;
import org.apache.commons.jexl3.JexlBuilder;
import org.apache.commons.jexl3.JexlEngine;
import org.apache.commons.jexl3.introspection.JexlPermissions;
import org.apache.commons.jexl3.introspection.JexlSandbox;

import java.math.MathContext;
import java.math.RoundingMode;

/**
 * Apache Commons JEXL 引擎实例工厂，集中管理所有 JEXL 选项配置
 */
public final class JexlInstanceBuilder {

    private JexlInstanceBuilder() {
    }

    /**
     * 根据配置构建一个独立的 {@link JexlEngine}。
     *
     * @param config     引擎配置项，传入 null 时所有选项使用内置默认值
     * @param customizer 编程式定制回调，在 {@link JexlBuilder#create()} 之前触发，
     *                   入参为 {@link JexlBuilder}，允许追加 namespaces、修改 sandbox 等。
     *                   传入 null 时跳过回调。
     * @return 配置完毕、可直接使用的 JexlEngine
     */
    public static JexlEngine buildJexlEngine(JexlStepHandlerProperties config, StepHandlerCustomizer<JexlBuilder> customizer) {
        if (config == null) {
            config = new JexlStepHandlerProperties();
        }

        /* 缓存大小 */
        int cache = defaultIfNull(config.getCache(), 2048);

        /* 严格算术逻辑：全部有效位数60位，四舍五入，小数位数40位 */
        JexlArithmetic arithmetic = new JexlArithmetic(true, new MathContext(60, RoundingMode.HALF_UP), 40);

        /* 构建 JexlBuilder */
        JexlBuilder builder = new JexlBuilder()
                .cache(cache) // 缓存大小：缓存已解析的表达式
                .strict(true) // 严格模式: true-遇到未定义的变量或属性访问错误时抛出异常；false-返回 null
                .silent(false) // 静默模式: true-发生错误时返回null并记录日志；false-发生错误时直接抛出异常
                .permissions(JexlPermissions.UNRESTRICTED) // UNRESTRICTED模式，允许new对象、反射等
                .sandbox(new JexlSandbox(true)) // 黑名单沙箱。true-黑单模式
                .arithmetic(arithmetic) // 配置严格算术逻辑
                .debug(defaultIfNull(config.getDebug(), Boolean.FALSE));

        /* 自定义配置 */
        if (customizer != null) {
            customizer.customize(builder);
        }

        /* 构建引擎对象并返回 */
        return builder.create();
    }

    /**
     * 默认值
     *
     * @param object       目标对象
     * @param defaultValue 默认值对象
     * @param <T>          目标对象的类型
     * @return 如果 {@code object} 不为 {@code null} 则返回 {@code object}；反之返回 {@code defaultValue}
     */
    private static <T> T defaultIfNull(final T object, final T defaultValue) {
        return object != null ? object : defaultValue;
    }
}
