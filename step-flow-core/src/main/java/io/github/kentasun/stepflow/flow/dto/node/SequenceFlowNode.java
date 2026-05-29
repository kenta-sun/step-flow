package io.github.kentasun.stepflow.flow.dto.node;

import io.github.kentasun.stepflow.api.dto.StepFlowContext;
import io.github.kentasun.stepflow.dto.ExecutorsContext;
import io.github.kentasun.stepflow.flow.dto.FlowNodeValidateContext;

import java.util.List;

/**
 * 多个 FlowNode 顺序同步执行的 FlowNode
 */
public class SequenceFlowNode extends FlowNode {

    private final List<FlowNode> flowNodeList;

    public SequenceFlowNode(String type, List<FlowNode> flowNodeList) {
        super(type);
        this.flowNodeList = flowNodeList;
    }

    @Override
    public void execute(StepFlowContext stepFlowContext, ExecutorsContext executorsContext) {
        for (FlowNode flowNode : this.flowNodeList) {
            flowNode.execute(stepFlowContext, executorsContext);
        }
    }

    @Override
    public void validate(FlowNodeValidateContext context, String globalFlowCode) {
        // 校验所有子节点
        for (FlowNode flowNode : this.flowNodeList) {
            flowNode.validate(context, globalFlowCode);
        }
    }

    public List<FlowNode> getFlowNodeList() {
        return this.flowNodeList;
    }
}
