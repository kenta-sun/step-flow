package io.github.kentasun.stepflow.sfl.flowbuilder.impl;

import io.github.kentasun.stepflow.flow.constants.FlowContentType;
import io.github.kentasun.stepflow.flow.dto.node.FlowNode;
import io.github.kentasun.stepflow.flow.dto.node.SequenceFlowNode;
import io.github.kentasun.stepflow.sfl.SflParser;
import io.github.kentasun.stepflow.sfl.constants.SflTokenType;
import io.github.kentasun.stepflow.sfl.constants.SlfKeyWords;
import io.github.kentasun.stepflow.sfl.flowbuilder.FlowNodeBuilder;

import java.util.List;

/**
 * SEQ 关键字解析策略：解析 {@code SEQ(child, child, ...)} 并构造 {@link SequenceFlowNode}。
 * <p>
 * 括号内至少包含一个子 flow，不允许空列表与尾随逗号（由 {@code parseFlowList} 统一校验）。
 * 直接 new 对象，不依赖反射。
 * </p>
 */
public class SeqFlowNodeBuilder implements FlowNodeBuilder {

    @Override
    public FlowNode parse(SflParser parser, int keywordPos) {
        parser.consumeMatched(SflTokenType.SYMBOL, SlfKeyWords.LPAREN);
        List<FlowNode> children = parser.parseFlowList();
        parser.consumeMatched(SflTokenType.SYMBOL, SlfKeyWords.RPAREN);
        return new SequenceFlowNode(FlowContentType.SEQUENCE, children);
    }
}
