package io.github.kentasun.stepflow.sfl;

import io.github.kentasun.stepflow.flow.dto.node.FlowNode;
import io.github.kentasun.stepflow.flow.dto.node.IfElseFlowNode;
import io.github.kentasun.stepflow.flow.dto.node.ParallelFlowNode;
import io.github.kentasun.stepflow.flow.dto.node.SequenceFlowNode;
import io.github.kentasun.stepflow.flow.dto.node.StepFlowNode;
import io.github.kentasun.stepflow.flow.dto.node.SubFlowNode;

import java.util.Arrays;

/**
 * 将 {@link FlowNode} 树格式化为缩进文本，供解析结果的人工核对与日志诊断。
 * <p>
 * 输出内容非 SFL 源码的逆序列化，仅反映内存中节点类型与子结构，避免与
 * {@link SflParser} 形成循环依赖。
 * </p>
 */
public final class SflDebugPrinter {

    private SflDebugPrinter() {
    }

    /**
     * 从根节点开始，以两空格一级缩进打印整棵树。
     *
     * @param node 解析得到的流程根，允许为任意 {@link FlowNode} 子类型
     * @return 多行文本，末尾含换行符
     */
    public static String format(FlowNode node) {
        return format(node, 0);
    }

    /**
     * 递归格式化节点及其子节点。
     *
     * @param node  当前节点
     * @param depth 当前缩进层级，根为 0
     * @return 当前子树对应的文本片段
     */
    private static String format(FlowNode node, int depth) {
        String indent = repeat(' ', depth * 2);
        StringBuilder sb = new StringBuilder();
        sb.append(indent).append(node.getType());

        if (node instanceof StepFlowNode) {
            sb.append(" stepCode=").append(((StepFlowNode) node).getStepCode()).append('\n');
        } else if (node instanceof SequenceFlowNode) {
            sb.append(" children=").append(((SequenceFlowNode) node).getFlowNodeList().size()).append('\n');
            for (FlowNode child : ((SequenceFlowNode) node).getFlowNodeList()) {
                sb.append(format(child, depth + 1));
            }
        } else if (node instanceof ParallelFlowNode) {
            sb.append(" children=").append(((ParallelFlowNode) node).getFlowNodeList().size()).append('\n');
            for (FlowNode child : ((ParallelFlowNode) node).getFlowNodeList()) {
                sb.append(format(child, depth + 1));
            }
        } else if (node instanceof IfElseFlowNode) {
            IfElseFlowNode iff = (IfElseFlowNode) node;
            sb.append('\n');
            sb.append(indent).append("  condition:\n");
            sb.append(format(iff.getCondition(), depth + 2));
            sb.append(indent).append("  trueFlowNode:\n");
            sb.append(format(iff.getTrueFlowNode(), depth + 2));
            sb.append(indent).append("  falseFlowNode: ")
                    .append(iff.getFalseFlowNode() == null ? "(无，不执行)" : "").append('\n');
            if (iff.getFalseFlowNode() != null) {
                sb.append(format(iff.getFalseFlowNode(), depth + 2));
            }
        } else if (node instanceof SubFlowNode) {
            sb.append(" flowCode=").append(((SubFlowNode) node).getFlowCode()).append('\n');
        } else {
            sb.append('\n');
        }
        return sb.toString();
    }

    private static String repeat(char ch, int count) {
        char[] arr = new char[count];
        Arrays.fill(arr, ch);
        return new String(arr);
    }
}
