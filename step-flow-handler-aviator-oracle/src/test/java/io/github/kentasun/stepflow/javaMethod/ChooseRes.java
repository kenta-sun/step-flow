package io.github.kentasun.stepflow.javaMethod;

import io.github.kentasun.aviatororacle.number.OraDecimal;
import io.github.kentasun.stepflow.dto.OneOffParams;
import io.github.kentasun.stepflow.dto.StepFlowContext;
import io.github.kentasun.stepflow.step.dto.StepData;
import io.github.kentasun.stepflow.step.intf.JavaStep;
import io.github.kentasun.stepflow.utils.StepFlowUtils;

public class ChooseRes implements JavaStep {

    @Override
    public Object invoke(StepData stepData, StepFlowContext stepFlowContext, OneOffParams oneOffParams) {
        OraDecimal calcMultiply = StepFlowUtils.getValByMap("calc_multiply", stepFlowContext.getContextMap(), OraDecimal.class);
        OraDecimal calcDivide = StepFlowUtils.getValByMap("calc_divide", stepFlowContext.getContextMap(), OraDecimal.class);

        if (calcMultiply == null) {
            return calcDivide;
        } else {
            return calcMultiply;
        }
    }
}
