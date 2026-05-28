package io.github.kentasun.stepflow.sfl.flowbuilder;

import io.github.kentasun.stepflow.flow.constants.FlowContentType;
import io.github.kentasun.stepflow.flow.dto.node.FlowNode;
import io.github.kentasun.stepflow.flow.dto.node.SubFlowNode;
import io.github.kentasun.stepflow.sfl.SflParser;
import io.github.kentasun.stepflow.sfl.SflToken;
import io.github.kentasun.stepflow.sfl.SflTokenType;

/**
 * SUB_FLOW 关键字解析策略：解析 {@code SUB_FLOW(flowCode)} 并构造 {@link SubFlowNode}。
 * <p>
 * 括号内只允许出现单一标识符作为子流程编码，不支持嵌套 flow 语法。
 * </p>
 */
public class SubFlowFlowNodeBuilder implements FlowNodeBuilder {

    @Override
    public FlowNode parse(SflParser parser, int keywordPos) {
        // 消费左括号
        parser.consumeTokenByType(SflTokenType.LPAREN);
        // 解析子流程编码（单一标识符）
        SflToken flowCodeToken = parser.consumeTokenByType(SflTokenType.IDENT);
        // 消费右括号
        parser.consumeTokenByType(SflTokenType.RPAREN);
        return new SubFlowNode(FlowContentType.SUB_FLOW, flowCodeToken.getText());
    }
}
