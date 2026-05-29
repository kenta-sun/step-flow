package io.github.kentasun.stepflow.sfl.flowbuilder.impl;

import io.github.kentasun.stepflow.flow.constants.FlowContentType;
import io.github.kentasun.stepflow.flow.dto.node.FlowNode;
import io.github.kentasun.stepflow.flow.dto.node.SubFlowNode;
import io.github.kentasun.stepflow.sfl.SflParser;
import io.github.kentasun.stepflow.sfl.SflToken;
import io.github.kentasun.stepflow.sfl.constants.SflTokenType;
import io.github.kentasun.stepflow.sfl.constants.SlfKeyWords;
import io.github.kentasun.stepflow.sfl.flowbuilder.FlowNodeBuilder;

/**
 * SUB_FLOW 关键字解析策略：解析 {@code SUB_FLOW(flowCode)} 并构造 {@link SubFlowNode}。
 * <p>
 * 括号内只允许出现单一标识符作为子流程编码，不支持嵌套 flow 语法。
 * </p>
 */
public class SubFlowFlowNodeBuilder implements FlowNodeBuilder {

    @Override
    public FlowNode parse(SflParser parser, int keywordPos) {
        parser.consumeMatched(SflTokenType.SYMBOL, SlfKeyWords.LPAREN);
        SflToken flowCodeToken = parser.consumeMatched(SflTokenType.LITERAL, null);
        parser.consumeMatched(SflTokenType.SYMBOL, SlfKeyWords.RPAREN);
        return new SubFlowNode(FlowContentType.SUB_FLOW, flowCodeToken.getText());
    }
}
