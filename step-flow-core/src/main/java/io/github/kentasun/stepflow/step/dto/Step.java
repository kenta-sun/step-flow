package io.github.kentasun.stepflow.step.dto;

import io.github.kentasun.stepflow.dto.ExecutorsContext;
import io.github.kentasun.stepflow.dto.OneOffParams;
import io.github.kentasun.stepflow.dto.StepFlowContext;
import io.github.kentasun.stepflow.step.intf.StepHandler;
import io.github.kentasun.stepflow.utils.StepFlowUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 步骤
 */
public class Step {

    private final StepData stepData;
    private final StepHandler stepHandler;

    public Step(StepData stepData, StepHandler stepHandler) {
        this.stepData = stepData;
        this.stepHandler = stepHandler;
    }

    /**
     * 执行步骤
     *
     * @param stepFlowContext 步骤上下文，用于传递
     * @param oneOffParams    1次性参数，仅供当前 step 使用
     * @param executorsContext 用于随着上下文一起传递的各种执行器
     * @return 返回步骤设置的参数
     */
    public Map<String, Object> execute(StepFlowContext stepFlowContext, OneOffParams oneOffParams, ExecutorsContext executorsContext) {
        /* 准备参数 */
        // 该步骤用到的参数名称
        List<String> paramNameList = stepData.getParamNameList();
        // 参数集合
        Map<String, Object> contextMap = stepFlowContext.getContextMap();
        // 参数名称在参数集合中的映射
        Map<String, String> paramNameMap = StepFlowUtils.getParamNameMap(oneOffParams);

        // 准备计算用到的参数
        Map<String, Object> vars = new HashMap<>();
        if (StepFlowUtils.isNotEmpty(paramNameList)) {
            for (String paramName : paramNameList) {
                Object value;
                String tempName = this.getTempName(paramName, paramNameMap);
                // 不为空说明需要映射
                if (StepFlowUtils.isNotBlank(tempName)) {
                    //noinspection DataFlowIssue
                    if (tempName.contains(".")) {
                        // 说明存在`policyInfo.applyDate`类型的参数获取，需要调用表达式引擎的getParam方法
                        value = executorsContext.getParam(tempName, contextMap);
                    } else {
                        // 普通情况下直接从 contextMap 中获取
                        value = contextMap.get(tempName);
                    }
                } else {
                    value = contextMap.get(paramName);
                }
                if (value != null) {
                    vars.put(paramName, value);
                }
            }
        }

        /* 执行步骤 */
        Object result = stepHandler.execute(
                stepData,
                stepFlowContext,
                OneOffParams.builder()
                        .vars(vars)
                        .build(),
                executorsContext
        );

        /* 处理返回值类型、名称 */
        if (result != null) {
            if (result instanceof Map) {
                return (Map<String, Object>) result;
            } else {
                String stepName = stepData.getStepName();
                Map<String, Object> resMap = new HashMap<>();
                resMap.put(stepName, result);
                return resMap;
            }
        }
        return null;
    }

    private String getTempName(String paramName, Map<String, String> map) {
        if (StepFlowUtils.isNotEmpty(map)) {
            return map.get(paramName);
        } else {
            return null;
        }
    }
}
