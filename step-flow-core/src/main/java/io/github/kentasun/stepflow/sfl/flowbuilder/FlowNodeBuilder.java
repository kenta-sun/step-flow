package io.github.kentasun.stepflow.sfl.flowbuilder;

import io.github.kentasun.stepflow.flow.dto.node.FlowNode;
import io.github.kentasun.stepflow.sfl.exception.SflException;
import io.github.kentasun.stepflow.sfl.SflParser;

/**
 * SFL 关键字解析策略接口。
 * <p>
 * 每个实现对应一个顶层 SFL 关键字（SEQ、PARALLEL、STEP、SUB_FLOW、IF），
 * 在 {@link SflParser#keywordToFlow()} 识别到关键字文本后，
 * 从策略注册表中查找对应实现并委托执行具体的语法解析与节点构建。
 * </p>
 * <p>
 * 调用约定：关键字 token 已被消费，实现只需从关键字后的第一个 token 开始消费。
 * </p>
 */
@FunctionalInterface
public interface FlowNodeBuilder {

    /**
     * 解析关键字之后的语法片段，返回对应的流程节点。
     *
     * @param parser     当前语法分析器
     * @param keywordLocation 关键字在源文本中的可读位置描述，用于错误定位
     * @return 解析结果节点，不为 null
     * @throws SflException 语法或语义规则违反时
     */
    FlowNode parse(SflParser parser, String keywordLocation);
}
