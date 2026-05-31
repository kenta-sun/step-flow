package io.github.kentasun.stepflow.step;

import io.github.kentasun.stepflow.api.dto.OneOffParams;
import io.github.kentasun.stepflow.api.dto.StepFlowContext;
import io.github.kentasun.stepflow.exception.StepFlowException;
import io.github.kentasun.stepflow.step.dto.Step;
import io.github.kentasun.stepflow.api.step.dto.StepData;
import io.github.kentasun.stepflow.api.step.StepDataProvider;
import io.github.kentasun.stepflow.api.step.StepHandler;
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

    /** stepCode → 已注册步骤 */
    private final Map<String, Step> stepMap;

    /**
     * StepContentType → StepHandler，供步骤组装与 IF 内联表达式执行共用。
     * key 与 {@link StepHandler#getStepContentType()} 一致，例如 AVIATOR、JEXL。
     */
    private final Map<String, StepHandler> stepHandlerMap;

    public StepExecutor(StepDataProvider stepDataProvider, List<StepHandler> stepHandlers) {
        /* 初始化 stepMap */
        this.stepMap = new ConcurrentHashMap<>();
        /* 获取 step 数据 */
        List<StepData> stepDataList = null;
        if (stepDataProvider != null) {
            stepDataList = stepDataProvider.loadStepDataList();
        }
        /* 组装 stepHandlerMap，构建后保留供运行时按 contentType 查找 Handler */
        Map<String, StepHandler> handlerMap = new HashMap<>();
        if (StepFlowUtils.isNotEmpty(stepHandlers)) {
            for (StepHandler stepHandler : stepHandlers) {
                if (handlerMap.containsKey(stepHandler.getStepContentType())) {
                    log.warn("StepHandler {} 被覆盖", stepHandler.getStepContentType());
                }
                handlerMap.put(stepHandler.getStepContentType(), stepHandler);
            }
        }
        this.stepHandlerMap = Collections.unmodifiableMap(handlerMap);
        /* 组装 step 对象 */
        if (StepFlowUtils.isNotEmpty(stepDataList)) {
            Set<String> duplicateSet = new HashSet<>();
            List<String> illegalList = new ArrayList<>();
            // 组装 Step
            for (StepData stepData : stepDataList) {
                Step existingStep = this.stepMap.get(stepData.getStepCode());
                if (existingStep != null) {
                    duplicateSet.add(stepData.getStepCode());
                    continue;
                }
                // 查找对应的 StepHandler
                StepHandler stepHandler = this.stepHandlerMap.get(stepData.getContentType());
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
                this.stepMap.put(stepData.getStepCode(), new Step(stepData, stepHandler));
            }
            if (StepFlowUtils.isNotEmpty(duplicateSet)) {
                throw new StepFlowException("这些stepCode重复了：" + duplicateSet);
            }
            if (StepFlowUtils.isNotEmpty(illegalList)) {
                throw new StepFlowException("这些step不合法：" + illegalList);
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
        Step step = this.stepMap.get(stepCode);
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
        return this.stepMap.containsKey(stepCode);
    }

    public Step getStep(String stepCode) {
        return this.stepMap.get(stepCode);
    }

    /**
     * 是否已注册指定 {@link StepHandler#getStepContentType()} 对应的 Handler。
     *
     * @param contentType 步骤内容类型，如 AVIATOR
     * @return true 表示存在对应 Handler
     */
    public boolean containsStepContentType(String contentType) {
        return this.stepHandlerMap.containsKey(contentType);
    }

    /**
     * 是否未注册指定 {@link StepHandler#getStepContentType()} 对应的 Handler。
     *
     * @param contentType 步骤内容类型，如 AVIATOR
     * @return true 表示不存在对应 Handler
     */
    public boolean isMissingStepContentType(String contentType) {
        return !this.containsStepContentType(contentType);
    }

    /**
     * 按 contentType 获取 StepHandler。
     *
     * @param contentType 步骤内容类型
     * @return 对应 Handler；不存在时返回 null
     */
    public StepHandler getStepHandler(String contentType) {
        return this.stepHandlerMap.get(contentType);
    }

    /**
     * 获取已注册的 StepContentType → StepHandler 映射（只读）。
     *
     * @return 不可变映射，key 为 StepContentType
     */
    public Map<String, StepHandler> getStepHandlerMap() {
        return stepHandlerMap;
    }

    /**
     * 使用指定 contentType 的 StepHandler 执行内联表达式（如 IF 条件中的 AVIATOR("a > b")）。
     * <p>
     * 将当前 {@link StepFlowContext#getContextMap()} 作为表达式变量传入 Handler。
     * </p>
     *
     * @param contentType     StepHandler 对应的 StepContentType
     * @param expression      表达式
     * @param stepFlowContext 流程上下文
     * @return Handler 执行结果
     */
    public Object executeInlineExpression(String contentType,
                                          String expression,
                                          StepFlowContext stepFlowContext) {
        StepHandler stepHandler = this.stepHandlerMap.get(contentType);
        if (stepHandler == null) {
            throw new StepFlowException(String.format("表达式类型[%s]不存在", contentType));
        }
        StepData stepData = StepData.builder()
                .contentType(contentType)
                .content(expression)
                .build();
        return stepHandler.execute(
                stepData,
                stepFlowContext,
                OneOffParams.builder()
                        .vars(stepFlowContext.getContextMap())
                        .build());
    }
}
