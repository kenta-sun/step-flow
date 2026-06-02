package io.github.kentasun.stepflow.aviatororacle.handler;

import com.googlecode.aviator.AviatorEvaluator;
import com.googlecode.aviator.AviatorEvaluatorInstance;
import com.googlecode.aviator.Expression;
import com.googlecode.aviator.Options;
import io.github.kentasun.aviatororacle.AviatorOracleBuilder;
import io.github.kentasun.stepflow.api.dto.OneOffParams;
import io.github.kentasun.stepflow.api.step.AbstractStepHandler;
import io.github.kentasun.stepflow.api.step.StepHandlerCustomizer;
import io.github.kentasun.stepflow.api.step.dto.StepData;
import io.github.kentasun.stepflow.aviatororacle.constants.AviatorOracleStepContentType;
import io.github.kentasun.stepflow.aviatororacle.dto.AviatorOracleStepHandlerProperties;

import java.util.List;

/**
 * aviator-oracle 表达式引擎 步骤处理器
 */
public class AviatorOracleStepHandler extends AbstractStepHandler {

    // 表达式引擎
    private final AviatorEvaluatorInstance aviatorOra;
    // 参数列表获取引擎
    private final AviatorEvaluatorInstance varsGetter;

    public AviatorOracleStepHandler() {
        this(null, null);
    }

    public AviatorOracleStepHandler(AviatorOracleStepHandlerProperties config) {
        this(config, null);
    }

    public AviatorOracleStepHandler(StepHandlerCustomizer<AviatorEvaluatorInstance> customizer) {
        this(null, customizer);
    }

    public AviatorOracleStepHandler(AviatorOracleStepHandlerProperties config, StepHandlerCustomizer<AviatorEvaluatorInstance> customizer) {
        if (config == null) {
            config = new AviatorOracleStepHandlerProperties();
        }
        // 默认 LRU 缓存大小为 2048
        if (config.getUseLRUExpressionCache() == null) {
            config.setUseLRUExpressionCache(2048);
        }
        this.aviatorOra = AviatorOracleBuilder.builder()
                .useLRUExpressionCache(config.getUseLRUExpressionCache())
                .maxLoopCount(config.getMaxLoopCount())
                .traceEval(config.getTraceEval())
                .build();
        if (customizer != null) {
            customizer.customize(this.aviatorOra);
        }

        // 新建独立实例用于解析表达式中的参数列表
        varsGetter = AviatorEvaluator.newInstance();
        // 明确关闭表达式缓存
        varsGetter.setCachedExpressionByDefault(false);
        // 必须用 EVAL 模式才能拿到变量元数据
        varsGetter.setOption(Options.OPTIMIZE_LEVEL, AviatorEvaluator.EVAL);
    }

    @Override
    public String getStepContentType() {
        return AviatorOracleStepContentType.AVIATOR_ORA;
    }

    @Override
    public List<String> getParamNameList(StepData stepData) {
        Expression expression = varsGetter.compile(stepData.getContent());
        return expression.getVariableNames();
    }

    @Override
    public Object doExecute(StepData stepData, OneOffParams oneOffParams) {
        // 表达式
        String expression = stepData.getContent();
        // 执行表达式并返回
        return this.aviatorOra.execute(expression, oneOffParams.getVars());
    }

    @Override
    public boolean isStepDataIllegal(StepData stepData) {
        return isBlank(stepData.getContent());
    }

    /**
     * <p>Checks if a CharSequence is empty (""), null or whitespace only.</p>
     * <p>Whitespace is defined by {@link Character#isWhitespace(char)}.</p>
     * <pre>
     * StringUtils.isBlank(null)      = true
     * StringUtils.isBlank("")        = true
     * StringUtils.isBlank(" ")       = true
     * StringUtils.isBlank("bob")     = false
     * StringUtils.isBlank("  bob  ") = false
     * </pre>
     *
     * @param cs the CharSequence to check, may be null
     * @return {@code true} if the CharSequence is null, empty or whitespace only
     */
    private static boolean isBlank(final CharSequence cs) {
        final int strLen = length(cs);
        if (strLen == 0) {
            return true;
        }
        for (int i = 0; i < strLen; i++) {
            if (!Character.isWhitespace(cs.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Gets a CharSequence length or {@code 0} if the CharSequence is
     * {@code null}.
     *
     * @param cs a CharSequence or {@code null}
     * @return CharSequence length or {@code 0} if the CharSequence is
     * {@code null}.
     * @since 2.4
     * @since 3.0 Changed signature from length(String) to length(CharSequence)
     */
    public static int length(final CharSequence cs) {
        return cs == null ? 0 : cs.length();
    }
}
