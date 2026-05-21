package io.github.kentasun.stepflow.javaMethod;

import io.github.kentasun.stepflow.api.dto.OneOffParams;
import io.github.kentasun.stepflow.api.dto.StepFlowContext;
import io.github.kentasun.stepflow.api.step.JavaStep;
import io.github.kentasun.stepflow.api.step.dto.StepData;
import io.github.kentasun.stepflow.utils.StepFlowUtils;

import java.math.BigDecimal;

/**
 * 根据 IF_ELSE 分支结果选择最终返回值：优先取 {@code calc_multiply}，否则取 {@code calc_divide}。
 * <p>与 Aviator/Jexl 模块中的 {@link ChooseRes} 逻辑一致，从流程上下文中读取变量。</p>
 */
public class ChooseRes implements JavaStep {

    @Override
    public Object invoke(StepData stepData, StepFlowContext stepFlowContext, OneOffParams oneOffParams) {
        BigDecimal calcMultiply = StepFlowUtils.getValByMap(
                "calc_multiply", stepFlowContext.getContextMap(), BigDecimal.class);
        BigDecimal calcDivide = StepFlowUtils.getValByMap(
                "calc_divide", stepFlowContext.getContextMap(), BigDecimal.class);

        if (calcMultiply == null) {
            return calcDivide;
        }
        return calcMultiply;
    }
}
