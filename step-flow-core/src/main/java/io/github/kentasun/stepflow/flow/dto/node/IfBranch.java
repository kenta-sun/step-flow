package io.github.kentasun.stepflow.flow.dto.node;

import io.github.kentasun.stepflow.flow.dto.FlowNodeValidateContext;
import io.github.kentasun.stepflow.api.utils.StepFlowUtils;

/**
 * IF_ELSE 流程节点中的一条分支：{@code IF/ELSIF(条件)} 与紧随其后的 {@code THEN(分支体)}。
 * <p>
 * 对应 SFL 中一段 {@code IF(...)THEN(...)} 或 {@code ELSIF(...)THEN(...)}。
 * 条件支持两种形式（二选一）：
 * <ul>
 *   <li>{@code STEP(stepCode)} — {@link #condition} 非空</li>
 *   <li>{@code TYPE("expression")} — {@link #expressionType} 与 {@link #expression} 非空，
 *       其中 TYPE 为已注册 AbstractStepHandler 的 StepContentType（如 AVIATOR）</li>
 * </ul>
 * </p>
 */
public class IfBranch {

    /** STEP 条件；与 expressionType/expression 互斥 */
    private final StepFlowNode condition;

    /**
     * 内联表达式类型
     */
    private final String expressionType;

    /** 内联表达式正文（已去除外围双引号并完成 {@code \"} 转义还原） */
    private final String expression;

    /** 条件为 true 时执行的子流程 */
    private final FlowNode thenFlowNode;

    public IfBranch(StepFlowNode condition,
                    String expressionType,
                    String expression,
                    FlowNode thenFlowNode) {
        boolean hasStepCondition = condition != null;
        boolean hasExpressionCondition = StepFlowUtils.isNotBlank(expressionType)
                || StepFlowUtils.isNotBlank(expression);
        if (hasStepCondition == hasExpressionCondition) {
            throw new IllegalArgumentException(
                    "IF 分支条件须为 STEP 或 TYPE(\"expression\") 之一，不可同时存在或同时缺失");
        }
        if (hasExpressionCondition && StepFlowUtils.isBlank(expressionType)) {
            throw new IllegalArgumentException("内联表达式条件缺少 expressionType");
        }
        if (hasExpressionCondition && StepFlowUtils.isBlank(expression)) {
            throw new IllegalArgumentException("内联表达式条件缺少 expression");
        }
        this.condition = condition;
        this.expressionType = expressionType;
        this.expression = expression;
        this.thenFlowNode = thenFlowNode;
    }

    /**
     * 构造 STEP 条件分支（SFL 解析 {@code STEP(...)} 时使用）。
     */
    public IfBranch(StepFlowNode condition, FlowNode thenFlowNode) {
        this(condition, null, null, thenFlowNode);
    }

    /**
     * 构造内联表达式条件分支（SFL 解析 {@code TYPE("...")} 时使用）。
     */
    public IfBranch(String expressionType, String expression, FlowNode thenFlowNode) {
        this(null, expressionType, expression, thenFlowNode);
    }

    /**
     * 递归校验本分支的条件与子流程。
     */
    public void validate(FlowNodeValidateContext context, String globalFlowCode) {
        if (this.condition != null) {
            this.condition.validate(context, globalFlowCode);
        } else {
            if (context.getStepExecutor().isMissingStepContentType(this.expressionType)) {
                context.saveErrMsg(
                        globalFlowCode,
                        String.format("IF 条件表达式类型[%s]不存在", this.expressionType));
            }
            if (StepFlowUtils.isBlank(this.expression)) {
                context.saveErrMsg(globalFlowCode, "IF 条件表达式为空");
            }
        }
        this.thenFlowNode.validate(context, globalFlowCode);
    }

    /** 是否为 STEP 步骤条件 */
    public boolean isStepCondition() {
        return this.condition != null;
    }

    /** 是否为内联表达式条件 */
    public boolean isExpressionCondition() {
        return this.expressionType != null;
    }

    public StepFlowNode getCondition() {
        return this.condition;
    }

    public String getExpressionType() {
        return this.expressionType;
    }

    public String getExpression() {
        return this.expression;
    }

    public FlowNode getThenFlowNode() {
        return this.thenFlowNode;
    }
}
