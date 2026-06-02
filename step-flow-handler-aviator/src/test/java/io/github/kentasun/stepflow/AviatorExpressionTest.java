package io.github.kentasun.stepflow;

import io.github.kentasun.stepflow.api.flow.FlowProvider;
import io.github.kentasun.stepflow.api.flow.dto.InputFlow;
import io.github.kentasun.stepflow.api.step.AbstractJavaStep;
import io.github.kentasun.stepflow.api.step.StepDataProvider;
import io.github.kentasun.stepflow.api.step.dto.StepInputData;
import io.github.kentasun.stepflow.aviator.constants.AviatorStepContentType;
import io.github.kentasun.stepflow.aviator.handler.AviatorStepHandler;
import io.github.kentasun.stepflow.dto.CalcDTO;
import io.github.kentasun.stepflow.javaMethod.ChooseRes;
import io.github.kentasun.stepflow.step.constants.StepContentType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class AviatorExpressionTest {

    @Test
    public void stepFlowExecutorTest() {
        StepDataProvider stepDataProvider = () -> Arrays.asList(
                StepInputData.builder()
                        .stepCode("COMMON001")
                        .stepName("add")
                        .stepType("COMMON")
                        .contentType(AviatorStepContentType.AVIATOR)
                        .content("a + b")
                        .build(),
                StepInputData.builder()
                        .stepCode("COMMON002")
                        .stepName("subtract")
                        .stepType("COMMON")
                        .contentType(AviatorStepContentType.AVIATOR)
                        .content("a - b")
                        .build(),
                StepInputData.builder()
                        .stepCode("COMMON003")
                        .stepName("multiply")
                        .stepType("COMMON")
                        .contentType(AviatorStepContentType.AVIATOR)
                        .content("a * b")
                        .build(),
                StepInputData.builder()
                        .stepCode("CONDITION001")
                        .stepName("condition1")
                        .stepType("CONDITION")
                        .contentType(AviatorStepContentType.AVIATOR)
                        .content("calc_add > 100 && calc_subtract < 100")
                        .build(),
                StepInputData.builder()
                        .stepCode("COMMON004")
                        .stepName("divide")
                        .stepType("COMMON")
                        .contentType(AviatorStepContentType.AVIATOR)
                        .content("a / b")
                        .build(),
                StepInputData.builder()
                        .stepCode("JAVA001")
                        .stepName("calc_Hades_res")
                        .stepType("JAVA")
                        .contentType(StepContentType.JAVA)
                        .content("chooseRes")
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

        Map<String, AbstractJavaStep> javaStepMap = new HashMap<>();
        javaStepMap.put("chooseRes", new ChooseRes());
        StepFlowExecutor stepFlowExecutor = StepFlowExecutor.builder(stepDataProvider, flowProvider)
                .javaStepMap(javaStepMap)
                .stepHandlerList(new ArrayList<>(Collections.singletonList(new AviatorStepHandler())))
                .build();

        Map<String, Object> contextMap = new ConcurrentHashMap<>();
        contextMap.put("dto", CalcDTO.builder()
                .num1(new BigDecimal("58"))
                .num2(new BigDecimal("77"))
                .num3(new BigDecimal("145"))
                .num4(new BigDecimal("69"))
                .build());
        Map<String, Object> resMap = stepFlowExecutor.executeByFlowCode("CALC001", contextMap);
        Assertions.assertEquals(new BigDecimal("10260"), resMap.get("calc_Hades_res"));
    }
}
