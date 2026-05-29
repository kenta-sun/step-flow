package io.github.kentasun.stepflow.sfl;

import io.github.kentasun.stepflow.flow.dto.node.FlowNode;
import io.github.kentasun.stepflow.testUtils.JsonUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Map;

/**
 * 校验 SFL 文本经 {@link SflParser} 解析
 */
class SflParserTest {

    private static final String expected =
            "{\"type\":\"SEQUENCE\",\"flowNodeList\":[{\"type\":\"PARALLEL\",\"flowNodeList\":"
                    + "[{\"type\":\"STEP\",\"stepCode\":\"COMMON001\",\"paramNameMap\":{\"a\":\"dto.num1\",\"b\":\"dto.num2\"},"
                    + "\"resultNameMap\":{\"add\":\"calc_add\"}},{\"type\":\"STEP\",\"stepCode\":\"COMMON002\","
                    + "\"paramNameMap\":{\"a\":\"dto.num3\",\"b\":\"dto.num4\"},\"resultNameMap\":{\"subtract\":\"calc_subtract\"}}]},"
                    + "{\"type\":\"IF_ELSE\",\"branches\":[{\"condition\":{\"type\":\"STEP\",\"stepCode\":\"CONDITION001\"},"
                    + "\"thenFlowNode\":{\"type\":\"STEP\",\"stepCode\":\"COMMON003\",\"paramNameMap\":{\"a\":\"calc_add\",\"b\":\"calc_subtract\"},"
                    + "\"resultNameMap\":{\"multiply\":\"calc_multiply\"}},\"stepCondition\": true,\"expressionCondition\": false},"
                    + " {\"expressionType\":\"AVIATOR\",\"expression\":\"a > b || c == \\\"hello\\\"\","
                    + " \"thenFlowNode\":{\"type\":\"STEP\",\"stepCode\":\"COMMON005\"},\"stepCondition\": false,\"expressionCondition\": true}],"
                    + "\"elseFlowNode\":{\"type\":\"STEP\",\"stepCode\":\"COMMON004\","
                    + "\"paramNameMap\":{\"a\":\"calc_add\",\"b\":\"calc_subtract\"},\"resultNameMap\":{\"divide\":\"calc_divide\"}}},"
                    + "{\"type\":\"STEP\",\"stepCode\":\"JAVA001\"}]}";

    private static final String SFL =
            "SEQ("
                    + "PARALLEL("
                    + "STEP(COMMON001).PARAM(a=dto.num1,b=dto.num2).result(add=calc_add),"
                    + "STEP(COMMON002).PARAM(a=dto.num3,b=dto.num4).result(subtract=calc_subtract)"
                    + "),"
                    + "IF(STEP(CONDITION001))"
                    + "THEN(STEP(COMMON003).PARAM(a=calc_add,b=calc_subtract).result(multiply=calc_multiply))"
                    + "ELSIF(AVIATOR(\"a > b || c == \\\"hello\\\"\"))"
                    + "THEN(STEP(COMMON005))"
                    + "ELSE(STEP(COMMON004).PARAM(a=calc_add,b=calc_subtract).result(divide=calc_divide))"
                    + "ENDIF,"
                    + "STEP(JAVA001)"
                    + ")";

    /**
     * 将 SFL 文本解析为 {@link FlowNode}，经序列化后，与既有 JSON content 做结构化比对。
     */
    @Test
    void testSflParser() {
        FlowNode actual = SflParser.parse(SFL);
        Map<String, String> diffMap = JsonUtils.compare(actual, expected);
        Assertions.assertTrue(
                diffMap.isEmpty(),
                () -> "SFL 解析序列化结果与 JSON content 不一致，差异: " + diffMap
        );
    }
}
