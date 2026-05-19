package io.github.kentasun.stepflow.engine.impl;

import com.googlecode.aviator.AviatorEvaluatorInstance;
import io.github.kentasun.aviatororacle.AviatorOracleBuilder;
import io.github.kentasun.stepflow.config.StepFlowEngineProperties;
import io.github.kentasun.stepflow.dto.ExecutorsContext;
import io.github.kentasun.stepflow.dto.OneOffParams;
import io.github.kentasun.stepflow.dto.StepFlowContext;
import io.github.kentasun.stepflow.engine.StepHandlerCustomizer;
import io.github.kentasun.stepflow.engine.constants.AviatorOracleStepContentType;
import io.github.kentasun.stepflow.step.dto.StepData;
import io.github.kentasun.stepflow.step.intf.StepHandler;
import io.github.kentasun.stepflow.utils.StepFlowUtils;

/**
 * aviator-oracle 表达式引擎 步骤处理器
 */
public class AviatorOracleStepHandler implements StepHandler {

    // 表达式引擎
    private final AviatorEvaluatorInstance aviatorOra;

    public AviatorOracleStepHandler(StepFlowEngineProperties config, StepHandlerCustomizer customizer) {
        if (config == null) {
            config = new StepFlowEngineProperties();
        }
        // 默认 LRU 缓存大小为 2048
        if (config.getMaxExpressionCache() == null) {
            config.setMaxExpressionCache(2048);
        }
        this.aviatorOra = AviatorOracleBuilder.builder()
                .useLRUExpressionCache(config.getMaxExpressionCache())
                .maxLoopCount(config.getMaxLoopCount())
                .traceEval(config.getLogEnabled())
                .build();
        if (customizer != null) {
            customizer.customize(this.aviatorOra);
        }
    }

    @Override
    public String getStepContentType() {
        return AviatorOracleStepContentType.AVIATOR_ORA;
    }

    @Override
    public Object execute(StepData stepData, StepFlowContext stepFlowContext, OneOffParams oneOffParams, ExecutorsContext executorsContext) {
        // 表达式
        String expression = stepData.getContent();
        // 执行表达式并返回
        return aviatorOra.execute(expression, oneOffParams.getVars());
    }

    @Override
    public boolean isStepDataIllegal(StepData stepData) {
        return StepFlowUtils.isBlank(stepData.getContent());
    }
}
