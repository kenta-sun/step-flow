package io.github.kentasun.stepflow.sfl;

import io.github.kentasun.stepflow.flow.dto.node.FlowNode;
import io.github.kentasun.stepflow.flow.dto.node.IfBranch;
import io.github.kentasun.stepflow.flow.dto.node.IfElseFlowNode;
import io.github.kentasun.stepflow.flow.dto.node.StepFlowNode;
import io.github.kentasun.stepflow.testUtils.JsonUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * IF/ELSIF 内联表达式条件 {@code TYPE("expression")} 的 SFL 解析测试。
 */
class SflParserIfExpressionTest {

    @Test
    void shouldParseIfWithAviatorExpressionCondition() {
        String sfl = "IF(AVIATOR(\"calc_add > calc_subtract\"))"
                + "THEN(STEP(COMMON003))"
                + "ELSE(STEP(COMMON004))"
                + "ENDIF";

        FlowNode root = SflParser.parse(sfl);
        Assertions.assertInstanceOf(IfElseFlowNode.class, root);

        IfElseFlowNode ifElse = (IfElseFlowNode) root;
        IfBranch branch = ifElse.getBranches().get(0);
        Assertions.assertNull(branch.getCondition());
        Assertions.assertEquals("AVIATOR", branch.getExpressionType());
        Assertions.assertEquals("calc_add > calc_subtract", branch.getExpression());
        Assertions.assertTrue(branch.isExpressionCondition());
    }

    @Test
    void shouldParseElsifWithExpressionAndEscapedQuote() {
        String sfl = "IF(STEP(COND001))"
                + "THEN(STEP(A001))"
                + "ELSIF(AVIATOR(\"name == \\\"test\\\"\"))"
                + "THEN(STEP(A002))"
                + "ENDIF";

        FlowNode root = SflParser.parse(sfl);
        IfElseFlowNode ifElse = (IfElseFlowNode) root;
        Assertions.assertEquals(2, ifElse.getBranches().size());

        IfBranch first = ifElse.getBranches().get(0);
        Assertions.assertInstanceOf(StepFlowNode.class, first.getCondition());
        Assertions.assertEquals("COND001", first.getCondition().getStepCode());

        IfBranch second = ifElse.getBranches().get(1);
        Assertions.assertEquals("AVIATOR", second.getExpressionType());
        Assertions.assertEquals("name == \"test\"", second.getExpression());
    }

    @Test
    void parsedIfBranchShouldExposeExpressionFields() {
        String sfl = "IF(JEXL(\"a + b > 0\"))THEN(STEP(S001))ENDIF";
        FlowNode root = SflParser.parse(sfl);
        String json = JsonUtils.writeValueAsString(root);

        Assertions.assertTrue(json.contains("\"expressionType\":\"JEXL\""));
        Assertions.assertTrue(json.contains("\"expression\":\"a + b > 0\""));
    }
}
