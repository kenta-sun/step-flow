package io.github.kentasun.stepflow.step;

import io.github.kentasun.stepflow.api.dto.OneOffParams;
import io.github.kentasun.stepflow.api.dto.StepFlowContext;
import io.github.kentasun.stepflow.exception.StepFlowException;
import io.github.kentasun.stepflow.step.dto.Step;
import io.github.kentasun.stepflow.api.step.dto.StepData;
import io.github.kentasun.stepflow.api.step.StepDataProvider;
import io.github.kentasun.stepflow.api.step.StepHandler;
import io.github.kentasun.stepflow.utils.StepFlowJsonUtils;
import io.github.kentasun.stepflow.utils.StepFlowUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * step 功能总入口
 */
public class StepExecutor {

    private static final Logger log = LoggerFactory.getLogger(StepExecutor.class);

    private final Map<String, Step> stepMap;

    public StepExecutor(StepDataProvider stepDataProvider, List<StepHandler> stepHandlers) {
        /* 初始化 stepMap */
        this.stepMap = new ConcurrentHashMap<>();
        /* 获取 step 数据 */
        List<StepData> stepDataList = null;
        if (stepDataProvider != null) {
            stepDataList = stepDataProvider.loadStepDataList();
        }
        /* 组装 stepHandlerMap，用于后续组装 step 对象 */
        Map<String, StepHandler> stepHandlerMap = new HashMap<>();
        if (StepFlowUtils.isNotEmpty(stepHandlers)) {
            for (StepHandler stepHandler : stepHandlers) {
                if (stepHandlerMap.containsKey(stepHandler.getStepContentType())) {
                    log.warn("StepHandler {} 被覆盖", stepHandler.getStepContentType());
                }
                stepHandlerMap.put(stepHandler.getStepContentType(), stepHandler);
            }
        }
        /* 组装 step 对象 */
        if (StepFlowUtils.isNotEmpty(stepDataList)) {
            Set<String> duplicateSet = new HashSet<>();
            List<String> illegalList = new ArrayList<>();
            // 组装 Step
            for (StepData stepData : stepDataList) {
                Step existingStep = stepMap.get(stepData.getStepCode());
                if (existingStep != null) {
                    duplicateSet.add(stepData.getStepCode());
                    continue;
                }
                // 查找对应的 StepHandler
                StepHandler stepHandler = stepHandlerMap.get(stepData.getContentType());
                if (stepHandler == null) {
                    illegalList.add(String.format("Step[%s] 的 contentType[%s] 不存在", stepData.getStepCode(), stepData.getContentType()));
                    continue;
                }
                // 校验步骤信息是否合法
                if (stepHandler.isStepDataIllegal(stepData)) {
                    illegalList.add(String.format(
                            "Step[%s] contentType 为 [%s]，未通过 [%s#isStepDataIllegal] 方法的校验",
                            stepData.getStepCode(),
                            stepData.getContentType(),
                            stepHandler.getClass().getName()
                    ));
                    continue;
                }
                // 放入 stepMap
                stepMap.put(stepData.getStepCode(), new Step(stepData, stepHandler));
            }
            if (StepFlowUtils.isNotEmpty(duplicateSet)) {
                throw new StepFlowException("这些stepCode重复了：" + StepFlowJsonUtils.writeValueAsString(duplicateSet));
            }
            if (StepFlowUtils.isNotEmpty(illegalList)) {
                throw new StepFlowException("这些step不合法：" + StepFlowJsonUtils.writeValueAsString(illegalList));
            }
        }
    }

    /**
     * 执行步骤
     *
     * @param stepCode        步骤代码
     * @param stepFlowContext 上下文对象
     * @param oneOffParams    1次性参数，仅供当前 step 使用
     * @return 步骤执行结果
     */
    public Object executeByStepCode(final String stepCode, StepFlowContext stepFlowContext, OneOffParams oneOffParams) {
        Step step = stepMap.get(stepCode);
        if (step == null) {
            throw new StepFlowException(String.format("【%s】步骤不存在", stepCode));
        }
        return step.execute(stepFlowContext, oneOffParams);
    }

    /**
     * 校验：是否存在指定的 stepCode
     *
     * @param stepCode 步骤标识
     * @return true-存在; false-不存在
     */
    public boolean hasStepCode(String stepCode) {
        return stepMap.containsKey(stepCode);
    }

    public Step getStep(String stepCode) {
        return stepMap.get(stepCode);
    }
}
