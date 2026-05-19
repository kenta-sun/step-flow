package io.github.kentasun.stepflow.engine.impl;

import io.github.kentasun.stepflow.config.StepFlowEngineProperties;
import io.github.kentasun.stepflow.dto.ExecutorsContext;
import io.github.kentasun.stepflow.dto.OneOffParams;
import io.github.kentasun.stepflow.dto.StepFlowContext;
import io.github.kentasun.stepflow.engine.StepHandlerCustomizer;
import io.github.kentasun.stepflow.engine.constants.JexlStepContentType;
import io.github.kentasun.stepflow.engine.jexl.JexlInstanceBuilder;
import io.github.kentasun.stepflow.step.dto.StepData;
import io.github.kentasun.stepflow.step.intf.StepHandler;
import io.github.kentasun.stepflow.utils.StepFlowUtils;
import org.apache.commons.jexl3.JexlEngine;
import org.apache.commons.jexl3.JexlExpression;
import org.apache.commons.jexl3.MapContext;

import java.util.Map;

/**
 * aviator-oracle 表达式引擎 步骤处理器
 */
public class JexlStepHandler implements StepHandler {

    // 表达式引擎
    private final JexlEngine jexl;

    public JexlStepHandler(StepFlowEngineProperties config, StepHandlerCustomizer customizer) {
        if (config == null) {
            config = new StepFlowEngineProperties();
        }
        if (config.getMaxExpressionCache() == null) {
            config.setMaxExpressionCache(2048);
        }
        this.jexl = JexlInstanceBuilder.buildJexlEngine(config, customizer);
    }

    @Override
    public String getStepContentType() {
        return JexlStepContentType.JEXL;
    }

    @Override
    public Object execute(StepData stepData, StepFlowContext stepFlowContext, OneOffParams oneOffParams, ExecutorsContext executorsContext) {
        // 表达式
        String expression = stepData.getContent();
        // 执行表达式并返回
        Map<String, Object> vars = oneOffParams.getVars();
        JexlExpression script = this.jexl.createExpression(expression);
        return script.evaluate(new MapContext(vars));
    }

    @Override
    public boolean isStepDataIllegal(StepData stepData) {
        return StepFlowUtils.isBlank(stepData.getContent());
    }
}
