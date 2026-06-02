package io.github.kentasun.stepflow.flow.dto.node;


import io.github.kentasun.stepflow.api.dto.OneOffParams;
import io.github.kentasun.stepflow.api.dto.StepFlowContext;
import io.github.kentasun.stepflow.dto.ExecutorsContext;
import io.github.kentasun.stepflow.flow.dto.FlowNodeValidateContext;
import io.github.kentasun.stepflow.step.dto.Step;
import io.github.kentasun.stepflow.api.utils.StepFlowUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * 执行一个 Step 的 FlowNode
 */
public class StepFlowNode extends FlowNode {

    private final String stepCode;
    // 调用该步骤需要映射的参数，解决当前 contextMap 中的参数名与步骤需要的参数名对不上的问题。
    private final Map<String, String> paramNameMap;
    /*
     * 调用该步骤需要映射的返回值，可以自定义该step在本flow中返回值的名字。
     * 解决期望的返回值名与步骤默认的返回值名对不上的问题。
     */
    private final Map<String, String> resultNameMap;

    public StepFlowNode(String type,
                        String stepCode,
                        Map<String, String> paramNameMap,
                        Map<String, String> resultNameMap) {
        super(type);
        this.stepCode = stepCode;
        this.paramNameMap = paramNameMap;
        this.resultNameMap = resultNameMap;
    }

    @Override
    public void execute(StepFlowContext stepFlowContext, ExecutorsContext executorsContext) {
        /* 执行 step */
        Object res = this.executeThenReturnRes(stepFlowContext, executorsContext);

        /* 组装 resMap */
        Map<String, Object> resMap;

        // 如果结果为null，直接返回
        if (res == null) {
            return;
        }

        if (res instanceof Map) {
            //noinspection unchecked
            resMap = (Map<String, Object>) res;
        } else {
            String stepName = this.getStepName(executorsContext);
            resMap = new HashMap<>();
            resMap.put(stepName, res);
        }

        /* 将计算结果放入上下文 */
        if (StepFlowUtils.isNotEmpty(resMap)) {
            // 有映射，先处理映射字段
            if (StepFlowUtils.isNotEmpty(this.resultNameMap)) {
                for (Map.Entry<String, String> resEntry : this.resultNameMap.entrySet()) {
                    String resMapKey = resEntry.getKey(); // 子步骤默认返回的字段名
                    String stepContextKey = resEntry.getValue(); // 映射期望的字段名
                    // 获取 value，同时在 resMap 中删除，防止重复放入相同字段
                    Object value = resMap.remove(resMapKey);
                    if (value != null) {
                        // 用 映射期望的字段名 放入上下文
                        stepFlowContext.put(stepContextKey, value);
                    }
                }
            }
            // 将所有默认字段名的字段放入上下文
            stepFlowContext.putAll(resMap);
        }
    }

    /**
     * 执行step并返回结果
     *
     * @param stepFlowContext  参数上下文对象
     * @param executorsContext 执行器上下文对象
     * @return step的执行结果
     */
    public Object executeThenReturnRes(StepFlowContext stepFlowContext, ExecutorsContext executorsContext) {
        return executorsContext.executeByStepCode(
                this.stepCode,
                stepFlowContext,
                OneOffParams.builder()
                        .paramNameMap(this.paramNameMap)
                        .build());
    }

    @Override
    public void validate(FlowNodeValidateContext context, String globalFlowCode) {
        // 校验 stepCode 是否存在
        if (context.stepCodeNotExist(this.stepCode)) {
            context.saveErrMsg(globalFlowCode, String.format("stepCode[%s] not exist", this.stepCode));
        }
    }

    private String getStepName(ExecutorsContext executorsContext) {
        Step step = executorsContext.getStep(this.stepCode);
        return step.getStepData().getStepName();
    }

    public String getStepCode() {
        return this.stepCode;
    }

    public Map<String, String> getParamNameMap() {
        return this.paramNameMap;
    }

    public Map<String, String> getResultNameMap() {
        return this.resultNameMap;
    }
}
