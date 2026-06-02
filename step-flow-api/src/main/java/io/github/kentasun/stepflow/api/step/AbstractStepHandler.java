package io.github.kentasun.stepflow.api.step;

import io.github.kentasun.stepflow.api.dto.OneOffParams;
import io.github.kentasun.stepflow.api.dto.StepFlowContext;
import io.github.kentasun.stepflow.api.step.dto.StepData;
import io.github.kentasun.stepflow.api.utils.StepFlowUtils;
import io.github.kentasun.stepflow.api.utils.StepVarsUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * step 的执行类型，每个 Handler 实现都对应一个 StepContentType。
 */
public abstract class AbstractStepHandler {

    /**
     * 该 Handler 对应的 StepContentType
     *
     * @return {@code AbstractStepHandler} 对应的 StepContentType 类型
     */
    public abstract String getStepContentType();

    /**
     * 该步骤需要的参数名称列表
     *
     * @param stepData 步骤信息
     * @return 参数名称列表
     */
    public abstract List<String> getParamNameList(StepData stepData);

    /**
     * step 行为的抽象方法
     *
     * @param stepData     步骤信息
     * @param oneOffParams 1次性参数，仅供当前 step 使用
     * @return 计算结果
     */
    public abstract Object doExecute(StepData stepData, OneOffParams oneOffParams);

    /**
     * 校验 {@code StepData} 是否非法
     *
     * @param stepData 待校验的 {@code StepData}
     * @return true-非法；false-合法
     */
    public abstract boolean isStepDataIllegal(StepData stepData);

    /**
     * 执行
     *
     * @param stepData        步骤信息
     * @param stepFlowContext 上下文对象
     * @param oneOffParams    1次性参数，仅供当前 step 使用
     * @return 计算结果
     */
    public Object execute(StepData stepData, StepFlowContext stepFlowContext, OneOffParams oneOffParams) {
        // 获取 该步骤需要的参数名称列表
        List<String> paramNameList = this.getParamNameListWithCache(stepData);
        // 获取参数映射关系
        Map<String, String> paramNameMap = this.getParamNameMap(oneOffParams);
        // 获取 该步骤需要的参数集合
        Map<String, Object> vars = StepVarsUtils.buildVars(paramNameList, stepFlowContext.getContextMap(), paramNameMap);
        if (oneOffParams == null) {
            oneOffParams = OneOffParams.builder().vars(vars).build();
        } else {
            oneOffParams.setVars(vars);
        }
        // 执行
        return this.doExecute(stepData, oneOffParams);
    }

    /**
     * 先从 {@link StepData} 中获取 {@code paramNameList}，如果没有再调用 {@link #getParamNameList}
     *
     * @param stepData 步骤信息
     * @return 参数名称列表
     */
    private List<String> getParamNameListWithCache(StepData stepData) {
        List<String> paramNameList = stepData.getParamNameList();
        if (paramNameList == null) {
            List<String> list = this.getParamNameList(stepData);
            if (StepFlowUtils.isNotEmpty(list)) {
                list = list.stream().distinct().collect(Collectors.toList());
                paramNameList = Collections.unmodifiableList(list);
            } else {
                paramNameList = Collections.emptyList();
            }
            stepData.setParamNameList(paramNameList);
        }
        return paramNameList;
    }

    private Map<String, String> getParamNameMap(OneOffParams oneOffParams) {
        if (oneOffParams == null) return null;
        return oneOffParams.getParamNameMap();
    }
}
