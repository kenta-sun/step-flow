package io.github.kentasun.stepflow.flow.constants;

/**
 * 流程节点类型常量
 */
public class FlowContentType {

    public static final String STEP = "STEP"; // 单个步骤
    public static final String SUB_FLOW = "SUB_FLOW"; // 子流程
    public static final String SEQUENCE = "SEQUENCE"; // 多个流程顺序执行
    public static final String PARALLEL = "PARALLEL"; // 多个流程并发执行
    public static final String IF_ELSE = "IF_ELSE"; // if-else判断后执行流程
}
