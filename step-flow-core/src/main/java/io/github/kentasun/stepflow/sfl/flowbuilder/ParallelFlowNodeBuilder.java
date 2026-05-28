package io.github.kentasun.stepflow.sfl.flowbuilder;

import io.github.kentasun.stepflow.flow.constants.FlowContentType;
import io.github.kentasun.stepflow.flow.dto.node.FlowNode;
import io.github.kentasun.stepflow.flow.dto.node.ParallelFlowNode;
import io.github.kentasun.stepflow.sfl.SflParser;
import io.github.kentasun.stepflow.sfl.SflTokenType;

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
        // 消费左括号
        parser.consumeTokenByType(SflTokenType.LPAREN);
        // 递归解析所有子节点（至少一个）
        List<FlowNode> children = parser.parseFlowList();
        // 消费右括号
        parser.consumeTokenByType(SflTokenType.RPAREN);
        return new ParallelFlowNode(FlowContentType.PARALLEL, children);
    }
}
