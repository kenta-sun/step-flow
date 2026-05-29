package io.github.kentasun.stepflow.flow.dto.node;

import io.github.kentasun.stepflow.api.dto.StepFlowContext;
import io.github.kentasun.stepflow.dto.ExecutorsContext;
import io.github.kentasun.stepflow.flow.dto.FlowNodeValidateContext;

/**
 * 运行另一个 Flow 的 FlowNode
 */
public class SubFlowNode extends FlowNode {

    private final String flowCode;

    public SubFlowNode(String type, String flowCode) {
        super(type);
        this.flowCode = flowCode;
    }

    @Override
    public void execute(StepFlowContext stepFlowContext, ExecutorsContext executorsContext) {
        // 执行流程
        executorsContext.executeByFlowCode(this.flowCode, stepFlowContext);
    }

    @Override
    public void validate(FlowNodeValidateContext context, String globalFlowCode) {
        // 校验 flowCode 是否存在
        if (context.flowCodeNotExist(this.flowCode)) {
            context.saveErrMsg(globalFlowCode, String.format("flowCode[%s] not exist", this.flowCode));
        }
    }

    public String getFlowCode() {
        return this.flowCode;
    }
}
