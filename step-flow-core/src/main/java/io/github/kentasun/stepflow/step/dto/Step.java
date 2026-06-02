package io.github.kentasun.stepflow.step.dto;

import io.github.kentasun.stepflow.api.dto.OneOffParams;
import io.github.kentasun.stepflow.api.dto.StepFlowContext;
import io.github.kentasun.stepflow.api.step.AbstractStepHandler;
import io.github.kentasun.stepflow.api.step.dto.StepData;

/**
 * 步骤
 */
public class Step {

    private final StepData stepData;
    private final AbstractStepHandler stepHandler;

    public Step(StepData stepData, AbstractStepHandler stepHandler) {
        this.stepData = stepData;
        this.stepHandler = stepHandler;
    }

    /**
     * 执行步骤
     *
     * @param stepFlowContext  上下文对象
     * @param oneOffParams     步骤上下文，用于传递1次性参数，仅供当前 step 使用
     * @return 返回步骤执行结果
     */
    public Object execute(StepFlowContext stepFlowContext, OneOffParams oneOffParams) {
        return this.stepHandler.execute(
                this.stepData,
                stepFlowContext,
                oneOffParams
        );
    }

    public StepData getStepData() {
        return stepData;
    }
}
