package io.github.kentasun.stepflow.step.handler;

import io.github.kentasun.stepflow.dto.ExecutorsContext;
import io.github.kentasun.stepflow.dto.OneOffParams;
import io.github.kentasun.stepflow.dto.StepFlowContext;
import io.github.kentasun.stepflow.engine.ExpressionEngine;
import io.github.kentasun.stepflow.step.constants.StepContentType;
import io.github.kentasun.stepflow.step.dto.StepData;
import io.github.kentasun.stepflow.step.intf.StepHandler;
import io.github.kentasun.stepflow.utils.StepFlowUtils;

/**
 * 表达式引擎步骤处理器
 */
public class ExpressionStepHandler implements StepHandler {

    // 表达式引擎
    private final ExpressionEngine expressionEngine;

    public ExpressionStepHandler(ExpressionEngine expressionEngine) {
        this.expressionEngine = expressionEngine;
    }

    @Override
    public String getStepContentType() {
        return StepContentType.EXPRESSION;
    }

    @Override
    public Object execute(StepData stepData, StepFlowContext stepFlowContext, OneOffParams oneOffParams, ExecutorsContext executorsContext) {
        // 表达式
        String expression = stepData.getContent();
        // 执行表达式并返回
        return expressionEngine.execute(expression, oneOffParams.getVars());
    }

    @Override
    public boolean isStepDataIllegal(StepData stepData) {
        return StepFlowUtils.isBlank(stepData.getContent());
    }
}
