package io.github.kentasun.stepflow.javaMethod;

import io.github.kentasun.stepflow.api.dto.OneOffParams;
import io.github.kentasun.stepflow.api.dto.StepFlowContext;
import io.github.kentasun.stepflow.api.step.JavaStep;
import io.github.kentasun.stepflow.api.step.dto.StepData;
import io.github.kentasun.stepflow.utils.StepFlowUtils;

import java.math.BigDecimal;

/**
 * 对应原 Aviator 条件表达式 {@code calc_add > 100 && calc_subtract < 100} 的 Java 实现。
 * <p>用于 IF_ELSE 分支判断，返回 {@link Boolean}。</p>
 */
public class Condition1 implements JavaStep {

    private static final BigDecimal THRESHOLD = new BigDecimal("100");

    @Override
    public Object invoke(StepData stepData, StepFlowContext stepFlowContext, OneOffParams oneOffParams) {
        BigDecimal calcAdd = StepFlowUtils.getValByMap("calc_add", oneOffParams.getVars(), BigDecimal.class);
        BigDecimal calcSubtract = StepFlowUtils.getValByMap("calc_subtract", oneOffParams.getVars(), BigDecimal.class);
        return calcAdd.compareTo(THRESHOLD) > 0 && calcSubtract.compareTo(THRESHOLD) < 0;
    }
}
