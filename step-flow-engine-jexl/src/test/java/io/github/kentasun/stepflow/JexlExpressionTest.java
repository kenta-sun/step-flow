package io.github.kentasun.stepflow;

import io.github.kentasun.stepflow.dto.CalcDTO;
import io.github.kentasun.stepflow.flow.dto.InputFlow;
import io.github.kentasun.stepflow.flow.intf.FlowProvider;
import io.github.kentasun.stepflow.javaMethod.ChooseRes;
import io.github.kentasun.stepflow.step.constants.StepContentType;
import io.github.kentasun.stepflow.step.dto.StepData;
import io.github.kentasun.stepflow.step.intf.JavaStep;
import io.github.kentasun.stepflow.step.intf.StepDataProvider;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class JexlExpressionTest {

    @Test
    public void stepFlowExecutorTest() {
        StepDataProvider stepDataProvider = () -> Arrays.asList(
                StepData.builder()
                        .stepCode("COMMON001")
                        .stepName("add")
                        .stepType("COMMON")
                        .contentType(StepContentType.EXPRESSION)
                        .content("a + b")
                        .paramNameList(Arrays.asList("a", "b"))
                        .build(),
                StepData.builder()
                        .stepCode("COMMON002")
                        .stepName("subtract")
                        .stepType("COMMON")
                        .contentType(StepContentType.EXPRESSION)
                        .content("a - b")
                        .paramNameList(Arrays.asList("a", "b"))
                        .build(),
                StepData.builder()
                        .stepCode("COMMON003")
                        .stepName("multiply")
                        .stepType("COMMON")
                        .contentType(StepContentType.EXPRESSION)
                        .content("a * b")
                        .paramNameList(Arrays.asList("a", "b"))
                        .build(),
                StepData.builder()
                        .stepCode("COMMON004")
                        .stepName("divide")
                        .stepType("COMMON")
                        .contentType(StepContentType.EXPRESSION)
                        .content("a / b")
                        .paramNameList(Arrays.asList("a", "b"))
                        .build(),
                StepData.builder()
                        .stepCode("JAVA001")
                        .stepName("calc_Hades_res")
                        .stepType("JAVA")
                        .contentType(StepContentType.JAVA)
                        .content("chooseRes")
                        .paramNameList(Arrays.asList("calc_multiply", "calc_divide"))
                        .build()
        );

        FlowProvider flowProvider = () -> Collections.singletonList(
                InputFlow.builder()
                        .flowCode("CALC001")
                        .flowName("calc_Hades")
                        .flowType("CALC")
                        .content("{\"type\":\"SEQUENCE\",\"flowNodeList\":[{\"type\":\"PARALLEL\",\"flowNodeList\":" +
                                "[{\"type\":\"STEP\",\"stepCode\":\"COMMON001\",\"paramNameMap\":{\"a\":\"dto.num1\",\"b\":\"dto.num2\"}," +
                                "\"resultNameMap\":{\"add\":\"calc_add\"}},{\"type\":\"STEP\",\"stepCode\":\"COMMON002\"," +
                                "\"paramNameMap\":{\"a\":\"dto.num3\",\"b\":\"dto.num4\"},\"resultNameMap\":{\"subtract\":\"calc_subtract\"}}]}," +
                                "{\"type\":\"IF_ELSE\",\"condition\":\"calc_add > 100 && calc_subtract < 100\"," +
                                "\"trueFlowNode\":{\"type\":\"STEP\",\"stepCode\":\"COMMON003\",\"paramNameMap\":{\"a\":\"calc_add\",\"b\":\"calc_subtract\"}," +
                                "\"resultNameMap\":{\"multiply\":\"calc_multiply\"}},\"falseFlowNode\":{\"type\":\"STEP\",\"stepCode\":\"COMMON004\"," +
                                "\"paramNameMap\":{\"a\":\"calc_add\",\"b\":\"calc_subtract\"},\"resultNameMap\":{\"divide\":\"calc_divide\"}}}," +
                                "{\"type\":\"STEP\",\"stepCode\":\"JAVA001\"}]}")
                        .returnFieldList(Arrays.asList("calc_Hades_res", "add"))
                        .build()
        );

        Map<String, JavaStep> javaStepMap = new HashMap<>();
        javaStepMap.put("chooseRes", new ChooseRes());
        StepFlowExecutor stepFlowExecutor = StepFlowExecutor.builder(stepDataProvider, flowProvider)
                .javaStepMap(javaStepMap)
                .build();

        Map<String, Object> contextMap = new ConcurrentHashMap<>();
        contextMap.put("dto", CalcDTO.builder()
                .num1(new BigDecimal("58"))
                .num2(new BigDecimal("77"))
                .num3(new BigDecimal("145"))
                .num4(new BigDecimal("69"))
                .build());
        Map<String, Object> resMap = stepFlowExecutor.executeByFLowCode("CALC001", contextMap);
        BigDecimal actual = (BigDecimal) resMap.get("calc_Hades_res");
        Assertions.assertEquals(0, new BigDecimal("10260").compareTo(actual));
    }
}
