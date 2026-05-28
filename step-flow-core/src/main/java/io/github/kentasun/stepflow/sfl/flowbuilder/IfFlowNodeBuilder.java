package io.github.kentasun.stepflow.sfl.flowbuilder;

import io.github.kentasun.stepflow.flow.constants.FlowContentType;
import io.github.kentasun.stepflow.flow.dto.node.FlowNode;
import io.github.kentasun.stepflow.flow.dto.node.IfElseFlowNode;
import io.github.kentasun.stepflow.flow.dto.node.StepFlowNode;
import io.github.kentasun.stepflow.sfl.SflException;
import io.github.kentasun.stepflow.sfl.SflParser;
import io.github.kentasun.stepflow.sfl.constants.SlfKeyWords;
import io.github.kentasun.stepflow.sfl.SflToken;
import io.github.kentasun.stepflow.sfl.SflTokenType;

/**
 * IF 关键字解析策略：解析 {@code IF(条件).TRUE(真分支)[.FALSE(假分支)]}
 * 并构造 {@link IfElseFlowNode}。
 * <p>
 * 语义约束：
 * <ul>
 *   <li>条件必须是 {@link StepFlowNode}，与执行引擎"条件作为单步求值"的设计一致。</li>
 *   <li>.TRUE(...) 分支必填；.FALSE(...) 分支可省略，省略时假分支为 {@code null}。</li>
 *   <li>.TRUE 和 .FALSE 之外的后缀一律报错。</li>
 * </ul>
 * </p>
 */
public class IfFlowNodeBuilder implements FlowNodeBuilder {

    @Override
    public FlowNode parse(SflParser parser, int keywordPos) {
        // 解析条件节点：IF(条件)
        parser.consumeTokenByType(SflTokenType.LPAREN);
        FlowNode conditionNode = parser.keywordToFlow();
        parser.consumeTokenByType(SflTokenType.RPAREN);

        // 条件必须是 STEP 节点
        if (!(conditionNode instanceof StepFlowNode)) {
            throw new SflException(
                    "IF 的条件必须是 " + SlfKeyWords.STEP + "(...)，实际为 ["
                            + conditionNode.getType() + "]，位置: " + keywordPos);
        }
        StepFlowNode condition = (StepFlowNode) conditionNode;

        // 解析必填的 .TRUE(...) 分支
        FlowNode trueFlowNode = parseIfBranch(parser, SlfKeyWords.IF_TRUE);

        // 解析可选的 .FALSE(...) 分支
        FlowNode falseFlowNode = null;
        if (parser.peek().getType() == SflTokenType.DOT) {
            parser.consume(); // 消费 '.'
            SflToken falseToken = parser.consumeTokenByType(SflTokenType.IDENT);
            if (!SlfKeyWords.IF_FALSE.equals(falseToken.getText())) {
                throw new SflException(
                        "IF 在 ." + SlfKeyWords.IF_TRUE + "(...) 之后仅允许 ."
                                + SlfKeyWords.IF_FALSE + "(...)，实际为 [." + falseToken.getText()
                                + "]，位置: " + falseToken.getPosition());
            }
            parser.consumeTokenByType(SflTokenType.LPAREN);
            falseFlowNode = parser.keywordToFlow();
            parser.consumeTokenByType(SflTokenType.RPAREN);
        }

        return new IfElseFlowNode(FlowContentType.IF_ELSE, condition, trueFlowNode, falseFlowNode);
    }

    /**
     * 解析 IF 的 {@code .TRUE(...)} 或 {@code .FALSE(...)} 后缀块。
     * <p>
     * 期望下一个 token 为 '.'，然后跟期望的分支标识符，再跟括号包裹的子 flow。
     * </p>
     *
     * @param parser     当前语法分析器
     * @param branchName 期望的后缀标识符字面量：{@code TRUE} 或 {@code FALSE}
     * @return 分支内的 flow 子树
     */
    private FlowNode parseIfBranch(SflParser parser, String branchName) {
        if (parser.peek().getType() != SflTokenType.DOT) {
            throw new SflException(
                    "IF 缺少 ." + branchName + "(...) 分支，位置: " + parser.peek().getPosition());
        }
        parser.consume(); // 消费 '.'
        SflToken branchToken = parser.consumeTokenByType(SflTokenType.IDENT);
        if (!branchName.equals(branchToken.getText())) {
            if (SlfKeyWords.IF_TRUE.equals(branchName)) {
                throw new SflException(
                        "IF 必须包含 ." + SlfKeyWords.IF_TRUE + "(...)，当前为 [."
                                + branchToken.getText() + "]，位置: "
                                + branchToken.getPosition());
            }
            throw new SflException(
                    "IF 期望 ." + branchName + "(...)，实际为 [." + branchToken.getText() + "]，位置: "
                            + branchToken.getPosition());
        }
        parser.consumeTokenByType(SflTokenType.LPAREN);
        FlowNode branch = parser.keywordToFlow();
        parser.consumeTokenByType(SflTokenType.RPAREN);
        return branch;
    }
}
