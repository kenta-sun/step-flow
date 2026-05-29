package io.github.kentasun.stepflow.flow.dto.node;

import io.github.kentasun.stepflow.api.dto.StepFlowContext;
import io.github.kentasun.stepflow.dto.ExecutorsContext;
import io.github.kentasun.stepflow.flow.dto.FlowNodeValidateContext;

/**
 * 流程类的公共父类，所有流程实现类都必须继承该类
 */
public abstract class FlowNode {

    /** 节点类型 */
    protected final String type;

    protected FlowNode(String type) {
        this.type = type;
    }

    public abstract void execute(StepFlowContext stepFlowContext, ExecutorsContext executorsContext);

    /**
     * 递归校验节点合法性
     *
     * @param context        递归校验上下文
     * @param globalFlowCode 此节点所属的 {@code Flow} 的步骤标识
     */
    public abstract void validate(FlowNodeValidateContext context, String globalFlowCode);

    public String getType() {
        return this.type;
    }
}
