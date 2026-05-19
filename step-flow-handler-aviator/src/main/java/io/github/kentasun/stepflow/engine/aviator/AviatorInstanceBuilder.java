package io.github.kentasun.stepflow.engine.aviator;

import io.github.kentasun.stepflow.config.StepFlowEngineProperties;
import io.github.kentasun.stepflow.utils.StepFlowUtils;
import com.googlecode.aviator.AviatorEvaluator;
import com.googlecode.aviator.AviatorEvaluatorInstance;
import com.googlecode.aviator.Options;

import java.math.MathContext;
import java.math.RoundingMode;

/**
 * AviatorScript 引擎实例工厂，集中管理所有 Aviator 选项配置
 */
public class AviatorInstanceBuilder {

    /**
     * 根据配置构建一个独立的 {@link AviatorEvaluatorInstance}。
     *
     * @param config 引擎配置项，传入 null 时所有选项使用内置默认值
     * @return 配置完毕、可直接使用的 AviatorEvaluatorInstance
     */
    public static AviatorEvaluatorInstance buildAviatorEvaluatorInstance(StepFlowEngineProperties config) {
        if (config == null) {
            config = new StepFlowEngineProperties();
        }

        // 每次创建独立实例，避免全局状态污染
        AviatorEvaluatorInstance aviator = AviatorEvaluator.newInstance();

        // 编译模式：表达式被直接翻译成 Java 字节码，性能最优
        aviator.setOption(Options.OPTIMIZE_LEVEL, AviatorEvaluator.COMPILE);

        // 开启 LRU 缓存策略，避免重复编译相同表达式
        aviator.setCachedExpressionByDefault(true);

        // 设置最大缓存表达式数量
        aviator.useLRUExpressionCache(StepFlowUtils.defaultIfNull(config.getMaxExpressionCache(), 2048));

        // decimal 数字精度，四舍五入，精度 40 位
        aviator.setOption(Options.MATH_CONTEXT, new MathContext(40, RoundingMode.HALF_UP));

        // 浮点数统一使用 BigDecimal，避免精度丢失
        aviator.setOption(Options.ALWAYS_PARSE_FLOATING_POINT_NUMBER_INTO_DECIMAL, true);

        // 单次脚本执行超时时间（毫秒），防止恶意/死循环表达式长期占用线程
        aviator.setOption(Options.EVAL_TIMEOUT_MS, 5000L);

        // 最大循环次数，防止死循环
        aviator.setOption(Options.MAX_LOOP_COUNT, StepFlowUtils.defaultIfNull(config.getMaxLoopCount(), 10000));

        // 调试日志，生产环境建议关闭
        aviator.setOption(Options.TRACE_EVAL, StepFlowUtils.defaultIfNull(config.getLogEnabled(), Boolean.FALSE));

        return aviator;
    }
}
