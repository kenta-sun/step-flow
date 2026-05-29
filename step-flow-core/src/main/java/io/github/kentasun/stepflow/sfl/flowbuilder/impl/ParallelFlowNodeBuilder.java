package io.github.kentasun.stepflow.sfl.flowbuilder.impl;

import io.github.kentasun.stepflow.flow.constants.FlowContentType;
import io.github.kentasun.stepflow.flow.dto.node.FlowNode;
import io.github.kentasun.stepflow.flow.dto.node.ParallelFlowNode;
import io.github.kentasun.stepflow.sfl.SflParser;
import io.github.kentasun.stepflow.sfl.constants.SflTokenType;
import io.github.kentasun.stepflow.sfl.constants.SlfKeyWords;
import io.github.kentasun.stepflow.sfl.flowbuilder.FlowNodeBuilder;

import java.util.List;

/**
 * PARALLEL 关键字解析策略：解析 {@code PARALLEL(child, child, ...)} 并构造 {@link ParallelFlowNode}。
 * <p>
 * 与 {@link SeqFlowNodeBuilder} 结构相同，但最终生成并发执行节点，两者分开以便独立扩展。
 * 直接 new 对象，不依赖反射。
 * </p>
 */
public class ParallelFlowNodeBuilder implements FlowNodeBuilder {

    @Override
    public FlowNode parse(SflParser parser, int keywordPos) {
        parser.consumeMatched(SflTokenType.SYMBOL, SlfKeyWords.LPAREN);
        List<FlowNode> children = parser.parseFlowList();
        parser.consumeMatched(SflTokenType.SYMBOL, SlfKeyWords.RPAREN);
        return new ParallelFlowNode(FlowContentType.PARALLEL, children);
    }
}
