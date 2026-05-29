package io.github.kentasun.stepflow;

import io.github.kentasun.stepflow.api.flow.FlowProvider;
import io.github.kentasun.stepflow.api.flow.dto.InputFlow;
import io.github.kentasun.stepflow.api.step.JavaStep;
import io.github.kentasun.stepflow.api.step.StepDataProvider;
import io.github.kentasun.stepflow.api.step.dto.StepData;
import io.github.kentasun.stepflow.dto.CalcDTO;
import io.github.kentasun.stepflow.javaMethod.Add;
import io.github.kentasun.stepflow.javaMethod.ChooseRes;
import io.github.kentasun.stepflow.javaMethod.Condition1;
import io.github.kentasun.stepflow.javaMethod.Divide;
import io.github.kentasun.stepflow.javaMethod.Multiply;
import io.github.kentasun.stepflow.javaMethod.Subtract;
import io.github.kentasun.stepflow.step.constants.StepContentType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 从 {@code AviatorExpressionTest} 迁移的流程执行集成测试。
 * <p>core 模块仅支持 {@link StepContentType#JAVA}，原 Aviator 表达式步骤均改为 {@link JavaStep} 实现。</p>
 */
public class JavaExpressionTest {

    @Test
    public void stepFlowExecutorTest() {
        StepDataProvider stepDataProvider = () -> Arrays.asList(
                StepData.builder()
                        .stepCode("COMMON001")
                        .stepName("add")
                        .stepType("COMMON")
                        .contentType(StepContentType.JAVA)
                        .content("add")
                        .paramNameList(Arrays.asList("a", "b"))
                        .build(),
                StepData.builder()
                        .stepCode("COMMON002")
                        .stepName("subtract")
                        .stepType("COMMON")
                        .contentType(StepContentType.JAVA)
                        .content("subtract")
                        .paramNameList(Arrays.asList("a", "b"))
                        .build(),
                StepData.builder()
                        .stepCode("COMMON003")
                        .stepName("multiply")
                        .stepType("COMMON")
                        .contentType(StepContentType.JAVA)
                        .content("multiply")
                        .paramNameList(Arrays.asList("a", "b"))
                        .build(),
                StepData.builder()
                        .stepCode("CONDITION001")
                        .stepName("condition1")
                        .stepType("CONDITION")
                        .contentType(StepContentType.JAVA)
                        .content("condition1")
                        .paramNameList(Arrays.asList("calc_add", "calc_subtract"))
                        .build(),
                StepData.builder()
                        .stepCode("COMMON004")
                        .stepName("divide")
                        .stepType("COMMON")
                        .contentType(StepContentType.JAVA)
                        .content("divide")
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
                                + "STEP(COMMON001).PARAM(a=dto.num1,b=dto.num2).result(add=calc_add),"
                                + "STEP(COMMON002).PARAM(a=dto.num3,b=dto.num4).result(subtract=calc_subtract)"
                                + "),"
                                + "IF(STEP(CONDITION001))"
                                + "THEN(STEP(COMMON003).PARAM(a=calc_add,b=calc_subtract).result(multiply=calc_multiply))"
                                + "ELSE(STEP(COMMON004).PARAM(a=calc_add,b=calc_subtract).result(divide=calc_divide))"
                                + "ENDIF,"
                                + "STEP(JAVA001)"
                                + ")")
                        .returnFieldList(Arrays.asList("calc_Hades_res", "add"))
                        .build()
        );

        Map<String, JavaStep> javaStepMap = new HashMap<>();
        javaStepMap.put("add", new Add());
        javaStepMap.put("subtract", new Subtract());
        javaStepMap.put("multiply", new Multiply());
        javaStepMap.put("divide", new Divide());
        javaStepMap.put("condition1", new Condition1());
        javaStepMap.put("chooseRes", new ChooseRes());

        // core 仅注册内置 JavaStepHandler，无需 AviatorStepHandler
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
        Map<String, Object> resMap = stepFlowExecutor.executeByFlowCode("CALC001", contextMap);
        BigDecimal actual = (BigDecimal) resMap.get("calc_Hades_res");
        Assertions.assertEquals(0, new BigDecimal("10260").compareTo(actual));
    }
}
