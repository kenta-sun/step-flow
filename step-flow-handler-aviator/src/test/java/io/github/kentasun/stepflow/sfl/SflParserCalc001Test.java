package io.github.kentasun.stepflow.sfl;

import io.github.kentasun.stepflow.AviatorExpressionTest;
import io.github.kentasun.stepflow.flow.FlowExecutor;
import io.github.kentasun.stepflow.flow.dto.node.FlowNode;
import io.github.kentasun.stepflow.testUtils.JsonUtils;
import io.github.kentasun.stepflow.utils.StepFlowJsonUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Map;

/**
 * 校验 CALC001 流程的 SFL 文本经 {@link SflParser} 解析并 Jackson 序列化后，
 * 与 {@link AviatorExpressionTest} 中 JSON 形式的 {@code InputFlow.content} 结构一致。
 */
class SflParserCalc001Test {

    /**
     * {@link AviatorExpressionTest#stepFlowExecutorTest()} 第 80 行起 {@code InputFlow.content} 的 JSON 原文。
     * 作为 SFL 解析结果的比对基准。
     */
    private static final String CALC001_JSON_CONTENT =
            "{\"type\":\"SEQUENCE\",\"flowNodeList\":[{\"type\":\"PARALLEL\",\"flowNodeList\":"
                    + "[{\"type\":\"STEP\",\"stepCode\":\"COMMON001\",\"paramNameMap\":{\"a\":\"dto.num1\",\"b\":\"dto.num2\"},"
                    + "\"resultNameMap\":{\"add\":\"calc_add\"}},{\"type\":\"STEP\",\"stepCode\":\"COMMON002\","
                    + "\"paramNameMap\":{\"a\":\"dto.num3\",\"b\":\"dto.num4\"},\"resultNameMap\":{\"subtract\":\"calc_subtract\"}}]},"
                    + "{\"type\":\"IF_ELSE\",\"condition\":{\"type\":\"STEP\",\"stepCode\":\"CONDITION001\"},"
                    + "\"trueFlowNode\":{\"type\":\"STEP\",\"stepCode\":\"COMMON003\",\"paramNameMap\":{\"a\":\"calc_add\",\"b\":\"calc_subtract\"},"
                    + "\"resultNameMap\":{\"multiply\":\"calc_multiply\"}},\"falseFlowNode\":{\"type\":\"STEP\",\"stepCode\":\"COMMON004\","
                    + "\"paramNameMap\":{\"a\":\"calc_add\",\"b\":\"calc_subtract\"},\"resultNameMap\":{\"divide\":\"calc_divide\"}}},"
                    + "{\"type\":\"STEP\",\"stepCode\":\"JAVA001\"}]}";

    /**
     * 与 {@link #CALC001_JSON_CONTENT} 等价的 Step Flow Language（SFL）编排文本。
     * <ul>
     *   <li>{@code SEQ} — 对应 JSON {@code SEQUENCE}</li>
     *   <li>{@code PARALLEL} — 并行子流程列表</li>
     *   <li>{@code STEP(code).param(k=v,...).result(k=v,...)} — 单步及参数/结果映射</li>
     *   <li>{@code IF(STEP(...)).TRUE(...).FALSE(...)} — 条件分支</li>
     * </ul>
     */
    private static final String CALC001_SFL =
            "SEQ("
                    + "PARALLEL("
                    + "STEP(COMMON001).param(a=dto.num1,b=dto.num2).result(add=calc_add),"
                    + "STEP(COMMON002).param(a=dto.num3,b=dto.num4).result(subtract=calc_subtract)"
                    + "),"
                    + "IF(STEP(CONDITION001)).TRUE(STEP(COMMON003).param(a=calc_add,b=calc_subtract).result(multiply=calc_multiply))"
                    + ".FALSE(STEP(COMMON004).param(a=calc_add,b=calc_subtract).result(divide=calc_divide)),"
                    + "STEP(JAVA001)"
                    + ")";

    /**
     * 将 CALC001 的 SFL 文本解析为 {@link FlowNode}，经与 {@link FlowExecutor} 相同的
     * {@link StepFlowJsonUtils} 序列化后，与既有 JSON content 做结构化比对。
     * <p>
     * 比对走 {@link JsonUtils#compare(Object, Object)}，按 JSON 树递归 diff 字段路径，
     * 避免字符串字面量因空白或键序造成的假阴性。
     * </p>
     */
    @Test
    void calc001SflParseShouldMatchJsonContent() {
        FlowNode root = SflParser.parse(CALC001_SFL);
        String actualJson = StepFlowJsonUtils.writeValueAsString(root);

        Map<String, String> diffMap = JsonUtils.compare(actualJson, CALC001_JSON_CONTENT);
        Assertions.assertTrue(
                diffMap.isEmpty(),
                () -> "SFL 解析序列化结果与 JSON content 不一致，差异: " + diffMap
        );
    }
}
