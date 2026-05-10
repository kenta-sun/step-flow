package io.github.kentasun.stepflow.flow.dto.node;

import io.github.kentasun.stepflow.dto.ExecutorsContext;
import io.github.kentasun.stepflow.dto.StepFlowContext;
import io.github.kentasun.stepflow.flow.dto.FlowNodeValidateContext;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import lombok.Getter;

/**
 * 条件判断 FlowNode。
 * <p>if (condition) {trueFlowNode} else {falseFlowNode}</p>
 */
public class IfElseFlowNode extends FlowNode {

    @JsonSetter(nulls = Nulls.FAIL)
    @Getter
    private final String condition;

    @JsonSetter(nulls = Nulls.FAIL)
    @Getter
    private final FlowNode trueFlowNode;

    @JsonSetter(nulls = Nulls.FAIL)
    @Getter
    private final FlowNode falseFlowNode;

    @JsonCreator
    public IfElseFlowNode(@JsonProperty("type") String type,
                          @JsonProperty("condition") String condition,
                          @JsonProperty("trueFlowNode") FlowNode trueFlowNode,
                          @JsonProperty("falseFlowNode") FlowNode falseFlowNode) {
        super(type);
        this.condition = condition;
        this.trueFlowNode = trueFlowNode;
        this.falseFlowNode = falseFlowNode;
    }

    @Override
    public void execute(StepFlowContext stepFlowContext, ExecutorsContext executorsContext) {
        Boolean isTrue = executorsContext.isTrue(condition, stepFlowContext.getContextMap());
        if (isTrue) {
            trueFlowNode.execute(stepFlowContext, executorsContext);
        } else {
            falseFlowNode.execute(stepFlowContext, executorsContext);
        }
    }

    @Override
    public void validate(FlowNodeValidateContext context, String globalFlowCode) {
        // 校验2个子节点
        trueFlowNode.validate(context, globalFlowCode);
        falseFlowNode.validate(context, globalFlowCode);
    }
}
