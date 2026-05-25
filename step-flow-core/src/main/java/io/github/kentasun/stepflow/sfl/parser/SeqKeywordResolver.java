package io.github.kentasun.stepflow.sfl.parser;

import io.github.kentasun.stepflow.flow.constants.FlowContentType;
import io.github.kentasun.stepflow.flow.dto.node.FlowNode;
import io.github.kentasun.stepflow.flow.dto.node.SequenceFlowNode;
import io.github.kentasun.stepflow.sfl.SflSyntaxParser;
import io.github.kentasun.stepflow.sfl.SflTokenType;

import java.util.List;

/**
 * SEQ 关键字解析策略：解析 {@code SEQ(child, child, ...)} 并构造 {@link SequenceFlowNode}。
 * <p>
 * 括号内至少包含一个子 flow，不允许空列表与尾随逗号（由 {@code parseFlowList} 统一校验）。
 * 直接 new 对象，不依赖反射。
 * </p>
 */
public class SeqKeywordResolver implements KeywordResolver {

    @Override
    public FlowNode parse(SflSyntaxParser parser, int keywordPos) {
        // 消费左括号
        parser.expect(SflTokenType.LPAREN);
        // 递归解析所有子节点（至少一个）
        List<FlowNode> children = parser.parseFlowList();
        // 消费右括号
        parser.expect(SflTokenType.RPAREN);
        return new SequenceFlowNode(FlowContentType.SEQUENCE, children);
    }
}
