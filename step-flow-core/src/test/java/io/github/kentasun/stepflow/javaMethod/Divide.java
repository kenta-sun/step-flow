package io.github.kentasun.stepflow.javaMethod;

import io.github.kentasun.stepflow.api.dto.OneOffParams;
import io.github.kentasun.stepflow.api.dto.StepFlowContext;
import io.github.kentasun.stepflow.api.step.JavaStep;
import io.github.kentasun.stepflow.api.step.dto.StepData;
import io.github.kentasun.stepflow.utils.StepFlowUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 对应原 Aviator 表达式 {@code a / b} 的 Java 实现。
 * <p>使用 {@link BigDecimal#divide(BigDecimal, int, RoundingMode)} 避免除不尽时抛异常。</p>
 */
public class Divide implements JavaStep {

    @Override
    public Object invoke(StepData stepData, StepFlowContext stepFlowContext, OneOffParams oneOffParams) {
        BigDecimal a = StepFlowUtils.getValByMap("a", oneOffParams.getVars(), BigDecimal.class);
        BigDecimal b = StepFlowUtils.getValByMap("b", oneOffParams.getVars(), BigDecimal.class);
        return a.divide(b, 10, RoundingMode.HALF_UP);
    }
}
