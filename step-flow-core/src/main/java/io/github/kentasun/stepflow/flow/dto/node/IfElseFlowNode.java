package io.github.kentasun.stepflow.flow.dto.node;

import io.github.kentasun.stepflow.api.dto.StepFlowContext;
import io.github.kentasun.stepflow.dto.ExecutorsContext;
import io.github.kentasun.stepflow.exception.StepFlowException;
import io.github.kentasun.stepflow.flow.dto.FlowNodeValidateContext;

import java.util.Collections;
import java.util.List;

/**
 * IF_ELSE 流程节点。
 */
public class IfElseFlowNode extends FlowNode {

    /** 至少包含首段 IF...THEN；后续项对应 ELSIF...THEN */
    private final List<IfBranch> branches;

    /** 对应 SFL {@code ELSE(...)}；省略 ELSE 时为 null */
    private final FlowNode elseFlowNode;

    public IfElseFlowNode(String type, List<IfBranch> branches, FlowNode elseFlowNode) {
        super(type);
        if (branches == null || branches.isEmpty()) {
            throw new IllegalArgumentException("IF_ELSE 的 branches 不能为空");
        }
        this.branches = Collections.unmodifiableList(branches);
        this.elseFlowNode = elseFlowNode;
    }

    @Override
    public void execute(StepFlowContext stepFlowContext, ExecutorsContext executorsContext) {
        for (IfBranch branch : this.branches) {
            if (evalConditionAsBoolean(branch, stepFlowContext, executorsContext)) {
                branch.getThenFlowNode().execute(stepFlowContext, executorsContext);
                return;
            }
        }
        if (this.elseFlowNode != null) {
            this.elseFlowNode.execute(stepFlowContext, executorsContext);
        }
    }

    /**
     * 执行分支条件并转为 boolean。
     *
     * @param branch IF 分支信息
     * @param stepFlowContext 上下文信息
     * @param executorsContext 执行器上下文对象
     * @return 条件判断结果 {@code boolean}
     */
    private static boolean evalConditionAsBoolean(IfBranch branch,
                                                  StepFlowContext stepFlowContext,
                                                  ExecutorsContext executorsContext) {
        /* 判断条件 */
        Object res;
        if (branch.isStepCondition()) {
            res = branch.getCondition().executeThenReturnRes(stepFlowContext, executorsContext);
        } else {
            res = executorsContext.executeInlineExpression(
                    branch.getExpressionType(),
                    branch.getExpression(),
                    stepFlowContext);
        }
        if (res instanceof Boolean) {
            return (Boolean) res;
        }

        /* 类型错误，抛出异常 */
        String conditionDesc;
        if (branch.isStepCondition()) {
            conditionDesc = String.format("step[%s]", branch.getCondition().getStepCode());
        } else {
            conditionDesc = String.format("%s(\"%s\")", branch.getExpressionType(), branch.getExpression());
        }
        if (res == null) {
            throw new StepFlowException(String.format("执行条件 %s，返回null", conditionDesc));
        }
        throw new StepFlowException(String.format(
                "执行条件 %s ，返回错误类型[%s]",
                conditionDesc,
                res.getClass().getName()));
    }

    @Override
    public void validate(FlowNodeValidateContext context, String globalFlowCode) {
        for (IfBranch branch : this.branches) {
            branch.validate(context, globalFlowCode);
        }
        if (this.elseFlowNode != null) {
            this.elseFlowNode.validate(context, globalFlowCode);
        }
    }

    public List<IfBranch> getBranches() {
        return branches;
    }

    public FlowNode getElseFlowNode() {
        return elseFlowNode;
    }
}
