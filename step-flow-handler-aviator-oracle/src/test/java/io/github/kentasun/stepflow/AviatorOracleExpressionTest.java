package io.github.kentasun.stepflow;

import io.github.kentasun.aviatororacle.number.OraDecimal;
import io.github.kentasun.stepflow.dto.DateCalcDTO;
import io.github.kentasun.stepflow.api.flow.dto.InputFlow;
import io.github.kentasun.stepflow.api.flow.FlowProvider;
import io.github.kentasun.stepflow.api.step.dto.StepData;
import io.github.kentasun.stepflow.aviatororacle.constants.AviatorOracleStepContentType;
import io.github.kentasun.stepflow.aviatororacle.handler.AviatorOracleStepHandler;
import io.github.kentasun.stepflow.api.step.StepDataProvider;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AviatorOracleExpressionTest {

    /**
     * 贷款期限与费用综合计算测试
     * <p>对应 Oracle_PLSQL_1.sql 脚本</p>
     */
    @Test
    public void dateCalcFlowTest() throws Exception {
        // ---- Step 数据定义 ----
        StepDataProvider stepDataProvider = () -> Arrays.asList(
                // DC001: months_between 计算月数差，trunc 保留 4 位小数
                StepData.builder()
                        .stepCode("DC001")
                        .stepName("calc_months_raw")
                        .stepType("DATE")
                        .contentType(AviatorOracleStepContentType.AVIATOR_ORA)
                        .content("trunc(months_between(endDate, startDate), 4)")
                        .paramNameList(Arrays.asList("endDate", "startDate"))
                        .build(),
                // DC002: abs 取带负号数值的绝对值
                StepData.builder()
                        .stepCode("DC002")
                        .stepName("calc_abs_val")
                        .stepType("DATE")
                        .contentType(AviatorOracleStepContentType.AVIATOR_ORA)
                        .content("abs(signedValue)")
                        .paramNameList(Collections.singletonList("signedValue"))
                        .build(),
                // DC003: IF_ELSE THEN 分支——floor 向下取整 * 本金 + ceil 向上取整
                StepData.builder()
                        .stepCode("DC003")
                        .stepName("calc_base")
                        .stepType("DATE")
                        .contentType(AviatorOracleStepContentType.AVIATOR_ORA)
                        .content("floor(calc_months_raw) * principal + ceil(rateInput * calc_abs_val)")
                        .paramNameList(Arrays.asList("calc_months_raw", "principal", "rateInput", "calc_abs_val"))
                        .build(),
                // DC004: IF_ELSE ELSE 分支——round 四舍五入 * 本金 - floor 向下取整（本例不会执行）
                StepData.builder()
                        .stepCode("DC004")
                        .stepName("calc_base")
                        .stepType("DATE")
                        .contentType(AviatorOracleStepContentType.AVIATOR_ORA)
                        .content("round(calc_months_raw, 1) * principal - floor(rateInput * calc_abs_val)")
                        .paramNameList(Arrays.asList("calc_months_raw", "principal", "rateInput", "calc_abs_val"))
                        .build(),
                // DC005: 日期 + 1.5 天（1.5 天 = 36 小时），测试日期加小数
                StepData.builder()
                        .stepCode("DC005")
                        .stepName("calc_date_shifted")
                        .stepType("DATE")
                        .contentType(AviatorOracleStepContentType.AVIATOR_ORA)
                        .content("startDate + 1.5")
                        .paramNameList(Collections.singletonList("startDate"))
                        .build(),
                // DC006: add_months 整月偏移（开始日期 + 6 个月）
                StepData.builder()
                        .stepCode("DC006")
                        .stepName("calc_add_months")
                        .stepType("DATE")
                        .contentType(AviatorOracleStepContentType.AVIATOR_ORA)
                        .content("add_months(startDate, 6)")
                        .paramNameList(Collections.singletonList("startDate"))
                        .build(),
                // DC007: last_day 取结束日期所在月份的最后一天
                StepData.builder()
                        .stepCode("DC007")
                        .stepName("calc_last_day")
                        .stepType("DATE")
                        .contentType(AviatorOracleStepContentType.AVIATOR_ORA)
                        .content("last_day(endDate)")
                        .paramNameList(Collections.singletonList("endDate"))
                        .build(),
                // DC008: 两个日期相减得到天数差（依赖 PARALLEL 中 calc_last_day，故在其后顺序执行）
                StepData.builder()
                        .stepCode("DC008")
                        .stepName("calc_date_diff")
                        .stepType("DATE")
                        .contentType(AviatorOracleStepContentType.AVIATOR_ORA)
                        .content("calc_last_day - endDate")
                        .paramNameList(Arrays.asList("calc_last_day", "endDate"))
                        .build(),
                // DC009: decode + nvl + coalesce，extraFactor 为 null 时全部走 NULL 分支，结果 = 5
                StepData.builder()
                        .stepCode("DC009")
                        .stepName("calc_extra")
                        .stepType("DATE")
                        .contentType(AviatorOracleStepContentType.AVIATOR_ORA)
                        .content("decode(nvl(extraFactor, 5), 5, coalesce(extraFactor, nvl(extraFactor, 5)), 0)")
                        .paramNameList(Collections.singletonList("extraFactor"))
                        .build(),
                // DC010: round + power 综合最终计算
                //        round(expr, 2) 对应 Oracle NUMBER(12,2) 的自动四舍五入效果
                StepData.builder()
                        .stepCode("DC010")
                        .stepName("calc_final")
                        .stepType("DATE")
                        .contentType(AviatorOracleStepContentType.AVIATOR_ORA)
                        .content("round(calc_base + calc_date_diff * calc_extra - power(calc_extra, 2) / calc_abs_val, 2)")
                        .paramNameList(Arrays.asList("calc_base", "calc_date_diff", "calc_extra", "calc_abs_val"))
                        .build(),
                StepData.builder()
                        .stepCode("CONDITION001")
                        .stepName("condition1")
                        .stepType("CONDITION")
                        .contentType(AviatorOracleStepContentType.AVIATOR_ORA)
                        .content("calc_months_raw < 18.16121")
                        .paramNameList(Collections.singletonList("calc_months_raw"))
                        .build()
        );

        // ---- Flow 数据定义 ----
        FlowProvider flowProvider = () -> Arrays.asList(
                // ---- 子流程 CALC_DATE_SUB ----
                // 结构：SEQUENCE [ PARALLEL(DC005, DC006, DC007), STEP DC008 ]
                // DC005/DC006/DC007 互不依赖，并行执行；DC008 依赖 calc_last_day，顺序在其后
                InputFlow.builder()
                        .flowCode("CALC_DATE_SUB")
                        .flowName("子流程-日期计算")
                        .flowType("DATE")
                        .content("SEQ("
                                + "PARALLEL("
                                + "STEP(DC005).PARAM(startDate=dto.startDate),"
                                + "STEP(DC006).PARAM(startDate=dto.startDate),"
                                + "STEP(DC007).PARAM(endDate=dto.endDate)"
                                + "),"
                                + "STEP(DC008).PARAM(endDate=dto.endDate)"
                                + ")")
                        .build(),

                // ---- 主流程 CALC_DATE_MAIN ----
                // 覆盖全部 5 种 FlowNode 类型：SEQUENCE / PARALLEL / IF_ELSE / SUB_FLOW / STEP
                InputFlow.builder()
                        .flowCode("CALC_DATE_MAIN")
                        .flowName("主流程-贷款期限与费用计算")
                        .flowType("DATE")
                        .content("SEQ("
                                + "PARALLEL("
                                + "STEP(DC001).PARAM(endDate=dto.endDate,startDate=dto.startDate),"
                                + "STEP(DC002).PARAM(signedValue=dto.signedValue)"
                                + "),"
                                + "IF(STEP(CONDITION001))"
                                + "THEN(STEP(DC003).PARAM(principal=dto.principal,rateInput=dto.rateInput))"
                                + "ELSE(STEP(DC004).PARAM(principal=dto.principal,rateInput=dto.rateInput))"
                                + "ENDIF,"
                                + "SUB_FLOW(CALC_DATE_SUB),"
                                + "STEP(DC009).PARAM(extraFactor=dto.extraFactor),"
                                + "STEP(DC010)"
                                + ")")
                        .returnFieldList(Arrays.asList(
                                "calc_months_raw", "calc_abs_val", "calc_base",
                                "calc_date_shifted", "calc_add_months", "calc_last_day",
                                "calc_date_diff", "calc_extra", "calc_final"))
                        .build()
        );

        // ---- 构建执行器（无需 JavaStep，纯表达式引擎即可）----
        StepFlowExecutor stepFlowExecutor = StepFlowExecutor.builder(stepDataProvider, flowProvider)
                .stepHandlerList(new ArrayList<>(Collections.singletonList(new AviatorOracleStepHandler())))
                .build();

        // ---- 构造输入 DTO ----
        SimpleDateFormat dateFmt = new SimpleDateFormat("yyyy-MM-dd");
        Map<String, Object> contextMap = new ConcurrentHashMap<>();
        contextMap.put("dto", DateCalcDTO.builder()
                .startDate(dateFmt.parse("2023-03-15"))  // 贷款开始日期
                .endDate(dateFmt.parse("2024-09-20"))    // 贷款结束日期
                .principal(new OraDecimal("100"))         // 本金基数
                .rateInput(new OraDecimal("3.7"))         // 费率系数
                .signedValue(new OraDecimal("-7.4325923421")) // 带负号数值，用于 abs
                .extraFactor(null)                        // 故意为 null，触发 nvl/coalesce/decode 的 NULL 分支
                .build());

        // ---- 执行主流程 ----
        Map<String, Object> resMap = stepFlowExecutor.executeByFlowCode("CALC_DATE_MAIN", contextMap);

        SimpleDateFormat dateTimeFmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        // ---- 断言（Oracle 期望值来自 Oracle_PLSQL_1.sql 的执行结果）----

        // Oracle: trunc(months_between('2024-09-20', '2023-03-15'), 4) = 18.1612
        Assertions.assertEquals(new OraDecimal("18.1612"), resMap.get("calc_months_raw"));

        // Oracle: abs(-7.4325923421) = 7.4325923421
        Assertions.assertEquals(new OraDecimal("7.4325923421"), resMap.get("calc_abs_val"));

        // Oracle: 18.1612 < 18.16121 → true 分支：floor(18.1612)*100 + ceil(3.7*7.4325923421)
        //         = 18*100 + ceil(27.5005916357) = 1800 + 28 = 1828
        Assertions.assertEquals(new OraDecimal("1828"), resMap.get("calc_base"));

        // Oracle: TO_DATE('2023-03-15') + 1.5 = '2023-03-16 12:00:00'（1.5天 = 129600秒）
        Assertions.assertEquals(dateTimeFmt.parse("2023-03-16 12:00:00"), resMap.get("calc_date_shifted"));

        // Oracle: add_months('2023-03-15', 6) = '2023-09-15'
        Assertions.assertEquals(dateFmt.parse("2023-09-15"), resMap.get("calc_add_months"));

        // Oracle: last_day('2024-09-20') = '2024-09-30'（9月共30天）
        Assertions.assertEquals(dateFmt.parse("2024-09-30"), resMap.get("calc_last_day"));

        // Oracle: last_day('2024-09-20') - '2024-09-20' = 10（天数差）
        // 用 compareTo 比较以规避 daysBetween 返回 scale=40 的 OraDecimal 与 scale=0 的 equals 不一致问题
        Assertions.assertEquals(0, new OraDecimal("10").compareTo((OraDecimal) resMap.get("calc_date_diff")));

        // Oracle: decode(nvl(null,5), 5, coalesce(null,nvl(null,5)), 0) = 5
        //         extraFactor 为 null → nvl(null,5)=5 → decode 命中 → coalesce(null,5)=5
        //         AviatorScript 整数字面量 5 计算后返回 Long 类型
        Assertions.assertEquals(5L, resMap.get("calc_extra"));

        // Oracle: round(calc_base + calc_date_diff*calc_extra - power(calc_extra,2)/calc_abs_val, 2)
        //         = round(1828 + 10*5 - 25/7.4325923421, 2)
        //         = round(1878 - 3.363802..., 2)
        //         = round(1874.636..., 2) = 1874.64
        //         对应 Oracle NUMBER(12,2) 的自动四舍五入效果，Flow 表达式用 round(expr, 2) 实现
        Assertions.assertEquals(new OraDecimal("1874.64"), resMap.get("calc_final"));
    }
}
