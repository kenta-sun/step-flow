package io.github.kentasun.stepflow;

import io.github.kentasun.stepflow.dto.CalcDTO;
import io.github.kentasun.stepflow.jexl.constants.JexlStepContentType;
import io.github.kentasun.stepflow.api.flow.dto.InputFlow;
import io.github.kentasun.stepflow.api.flow.FlowProvider;
import io.github.kentasun.stepflow.javaMethod.ChooseRes;
import io.github.kentasun.stepflow.step.constants.StepContentType;
import io.github.kentasun.stepflow.api.step.dto.StepData;
import io.github.kentasun.stepflow.jexl.handler.JexlStepHandler;
import io.github.kentasun.stepflow.api.step.JavaStep;
import io.github.kentasun.stepflow.api.step.StepDataProvider;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class JexlExpressionTest {

    @Test
    public void stepFlowExecutorTest() {
        StepDataProvider stepDataProvider = () -> Arrays.asList(
                StepData.builder()
                        .stepCode("COMMON001")
                        .stepName("add")
                        .stepType("COMMON")
                        .contentType(JexlStepContentType.JEXL)
                        .content("a + b")
                        .paramNameList(Arrays.asList("a", "b"))
                        .build(),
                StepData.builder()
                        .stepCode("COMMON002")
                        .stepName("subtract")
                        .stepType("COMMON")
                        .contentType(JexlStepContentType.JEXL)
                        .content("a - b")
                        .paramNameList(Arrays.asList("a", "b"))
                        .build(),
                StepData.builder()
                        .stepCode("COMMON003")
                        .stepName("multiply")
                        .stepType("COMMON")
                        .contentType(JexlStepContentType.JEXL)
                        .content("a * b")
                        .paramNameList(Arrays.asList("a", "b"))
                        .build(),
                StepData.builder()
                        .stepCode("CONDITION001")
                        .stepName("condition1")
                        .stepType("CONDITION")
                        .contentType(JexlStepContentType.JEXL)
                        .content("calc_add > 100 && calc_subtract < 100")
                        .paramNameList(Arrays.asList("calc_add", "calc_subtract"))
                        .build(),
                StepData.builder()
                        .stepCode("COMMON004")
                        .stepName("divide")
                        .stepType("COMMON")
                        .contentType(JexlStepContentType.JEXL)
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
                        .content("SEQ("
                                + "PARALLEL("
                                + "STEP(COMMON001).PARAM(a=dto.num1,b=dto.num2).RESULT(add=calc_add),"
                                + "STEP(COMMON002).PARAM(a=dto.num3,b=dto.num4).RESULT(subtract=calc_subtract)"
                                + "),"
                                + "IF(STEP(CONDITION001))"
                                + "THEN(STEP(COMMON003).PARAM(a=calc_add,b=calc_subtract).RESULT(multiply=calc_multiply))"
                                + "ELSE(STEP(COMMON004).PARAM(a=calc_add,b=calc_subtract).RESULT(divide=calc_divide))"
                                + "ENDIF,"
                                + "STEP(JAVA001)"
                                + ")")
                        .returnFieldList(Arrays.asList("calc_Hades_res", "add"))
                        .build()
        );

        Map<String, JavaStep> javaStepMap = new HashMap<>();
        javaStepMap.put("chooseRes", new ChooseRes());
        StepFlowExecutor stepFlowExecutor = StepFlowExecutor.builder(stepDataProvider, flowProvider)
                .javaStepMap(javaStepMap)
                .stepHandlerList(new ArrayList<>(Collections.singletonList(new JexlStepHandler())))
                .build();

        Map<String, Object> contextMap = new ConcurrentHashMap<>();
        contextMap.put("dto", CalcDTO.builder()
                .num1(new BigDecimal("58"))
                .num2(new BigDecimal("77"))
                .num3(new BigDecimal("145"))
                .num4(new BigDecimal("69"))
                .build());
        Map<String, Object> resMap = stepFlowExecutor.executeByFlowCode("CALC001", contextMap);
        BigDecimal actual = (BigDecimal) resMap.get("calc_Hades_res");
        Assertions.assertEquals(0, new BigDecimal("10260").compareTo(actual));
    }
}
