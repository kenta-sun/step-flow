package io.github.kentasun.stepflow.engine.jexl;

import io.github.kentasun.stepflow.config.StepFlowEngineProperties;
import io.github.kentasun.stepflow.engine.StepHandlerCustomizer;
import io.github.kentasun.stepflow.utils.StepFlowUtils;
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
    public static JexlEngine buildJexlEngine(StepFlowEngineProperties config, StepHandlerCustomizer customizer) {
        if (config == null) {
            config = new StepFlowEngineProperties();
        }

        /* 缓存大小 */
        int cacheSize = StepFlowUtils.defaultIfNull(config.getMaxExpressionCache(), 2048);

        /* 严格算术逻辑：全部有效位数60位，四舍五入，小数位数40位 */
        JexlArithmetic arithmetic = new JexlArithmetic(true, new MathContext(60, RoundingMode.HALF_UP), 40);

        /* 构建 JexlBuilder */
        JexlBuilder builder = new JexlBuilder()
                .cache(cacheSize) // 缓存大小：缓存已解析的表达式
                .strict(true) // 严格模式: true-遇到未定义的变量或属性访问错误时抛出异常；false-返回 null
                .silent(false) // 静默模式: true-发生错误时返回null并记录日志；false-发生错误时直接抛出异常
                .permissions(JexlPermissions.UNRESTRICTED) // UNRESTRICTED模式，允许new对象、反射等
                .sandbox(new JexlSandbox(true)) // 黑名单沙箱。true-黑单模式
                .arithmetic(arithmetic) // 配置严格算术逻辑
                .debug(StepFlowUtils.defaultIfNull(config.getLogEnabled(), Boolean.FALSE));

        /* 自定义配置 */
        if (customizer != null) {
            customizer.customize(builder);
        }

        /* 构建引擎对象并返回 */
        return builder.create();
    }
}
