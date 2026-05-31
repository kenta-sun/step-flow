package io.github.kentasun.stepflow.step.dto;

import io.github.kentasun.stepflow.api.dto.OneOffParams;
import io.github.kentasun.stepflow.api.dto.StepFlowContext;
import io.github.kentasun.stepflow.api.step.StepHandler;
import io.github.kentasun.stepflow.api.step.dto.StepData;
import io.github.kentasun.stepflow.utils.GetValueFromMapUtils;
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
     * @param stepFlowContext  步骤上下文，用于传递
     * @param oneOffParams     1次性参数，仅供当前 step 使用
     * @return 返回步骤执行结果
     */
    public Object execute(StepFlowContext stepFlowContext, OneOffParams oneOffParams) {
        /* 准备参数 */
        // 该步骤用到的参数名称
        List<String> paramNameList = this.stepData.getParamNameList();
        // 参数集合
        Map<String, Object> contextMap = stepFlowContext.getContextMap();
        // 参数名称在参数集合中的映射
        Map<String, String> paramNameMap = this.getParamNameMap(oneOffParams);

        // 准备计算用到的参数
        Map<String, Object> vars = new HashMap<>();
        if (StepFlowUtils.isNotEmpty(paramNameList)) {
            for (String paramName : paramNameList) {
                Object value;
                String tempName = this.getTempName(paramName, paramNameMap);
                if (StepFlowUtils.isNotBlank(tempName)) {
                    // 不为空说明需要映射
                    value = GetValueFromMapUtils.getValueFromContextMap(tempName, contextMap);
                } else {
                    // 不需要映射
                    value = GetValueFromMapUtils.getValueFromContextMap(paramName, contextMap);
                }
                if (value != null) {
                    vars.put(paramName, value);
                }
            }
        }

        /* 执行步骤并返回 */
        return this.stepHandler.execute(
                this.stepData,
                stepFlowContext,
                OneOffParams.builder()
                        .vars(vars)
                        .build()
        );
    }

    private String getTempName(String paramName, Map<String, String> map) {
        if (StepFlowUtils.isNotEmpty(map)) {
            return map.get(paramName);
        } else {
            return null;
        }
    }

    private Map<String, String> getParamNameMap(OneOffParams oneOffParams) {
        if (oneOffParams == null) return null;
        return oneOffParams.getParamNameMap();
    }

    public StepData getStepData() {
        return stepData;
    }
}
