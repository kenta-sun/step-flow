package io.github.kentasun.stepflow.javaMethod;

import io.github.kentasun.stepflow.api.dto.OneOffParams;
import io.github.kentasun.stepflow.api.dto.StepFlowContext;
import io.github.kentasun.stepflow.api.step.JavaStep;
import io.github.kentasun.stepflow.api.step.dto.StepData;
import io.github.kentasun.stepflow.utils.StepFlowUtils;

import java.math.BigDecimal;

/**
 * 对应原 Aviator 表达式 {@code a + b} 的 Java 实现。
 * <p>参数 {@code a}、{@code b} 由 {@link OneOffParams#getVars()} 注入。</p>
 */
public class Add implements JavaStep {

    @Override
    public Object invoke(StepData stepData, StepFlowContext stepFlowContext, OneOffParams oneOffParams) {
        BigDecimal a = StepFlowUtils.getValByMap("a", oneOffParams.getVars(), BigDecimal.class);
        BigDecimal b = StepFlowUtils.getValByMap("b", oneOffParams.getVars(), BigDecimal.class);
        return a.add(b);
    }
}
