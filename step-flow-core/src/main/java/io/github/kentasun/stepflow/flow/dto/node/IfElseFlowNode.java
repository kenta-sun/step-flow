package io.github.kentasun.stepflow.flow.dto.node;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import io.github.kentasun.stepflow.api.dto.StepFlowContext;
import io.github.kentasun.stepflow.dto.ExecutorsContext;
import io.github.kentasun.stepflow.exception.StepFlowException;
import io.github.kentasun.stepflow.flow.dto.FlowNodeValidateContext;

/**
 * 条件判断 FlowNode。
 * <p>if (condition) {trueFlowNode} else {falseFlowNode}</p>
 */
public class IfElseFlowNode extends FlowNode {

    @JsonSetter(nulls = Nulls.FAIL)
    private final StepFlowNode condition;

    @JsonSetter(nulls = Nulls.FAIL)
    private final FlowNode trueFlowNode;

    private final FlowNode falseFlowNode;

    @JsonCreator
    public IfElseFlowNode(@JsonProperty("type") String type,
                          @JsonProperty("condition") StepFlowNode condition,
                          @JsonProperty("trueFlowNode") FlowNode trueFlowNode,
                          @JsonProperty("falseFlowNode") FlowNode falseFlowNode) {
        super(type);
        this.condition = condition;
        this.trueFlowNode = trueFlowNode;
        this.falseFlowNode = falseFlowNode;
    }

    @Override
    public void execute(StepFlowContext stepFlowContext, ExecutorsContext executorsContext) {
        Object res = condition.executeThenReturnRes(stepFlowContext, executorsContext);
        Boolean isTrue;
        if (res instanceof Boolean) {
            isTrue = (Boolean) res;
        } else if (res == null) {
            throw new StepFlowException(String.format("执行step[%s]，返回null", condition.getStepCode()));
        } else {
            throw new StepFlowException(String.format("执行step[%s]，返回错误类型：%s", condition.getStepCode(), res.getClass().getName()));
        }
        if (isTrue) {
            trueFlowNode.execute(stepFlowContext, executorsContext);
        } else {
            // falseFlowNode 为null表示不需要任何操作
            if (falseFlowNode != null) {
                falseFlowNode.execute(stepFlowContext, executorsContext);
            }
        }
    }

    @Override
    public void validate(FlowNodeValidateContext context, String globalFlowCode) {
        // 校验条件节点
        condition.validate(context, globalFlowCode);
        // 校验2个子节点
        trueFlowNode.validate(context, globalFlowCode);
        falseFlowNode.validate(context, globalFlowCode);
    }

    public StepFlowNode getCondition() {
        return this.condition;
    }

    public FlowNode getTrueFlowNode() {
        return this.trueFlowNode;
    }

    public FlowNode getFalseFlowNode() {
        return this.falseFlowNode;
    }
}
