package io.github.kentasun.stepflow.step;

import io.github.kentasun.stepflow.api.dto.OneOffParams;
import io.github.kentasun.stepflow.api.dto.StepFlowContext;
import io.github.kentasun.stepflow.api.exception.StepFlowException;
import io.github.kentasun.stepflow.api.step.AbstractStepHandler;
import io.github.kentasun.stepflow.api.step.StepDataProvider;
import io.github.kentasun.stepflow.api.step.dto.StepData;
import io.github.kentasun.stepflow.api.step.dto.StepInputData;
import io.github.kentasun.stepflow.api.utils.StepFlowUtils;
import io.github.kentasun.stepflow.step.dto.Step;
import io.github.kentasun.stepflow.step.dto.StepCacheKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * step 功能总入口
 */
public class StepExecutor {

    private static final Logger log = LoggerFactory.getLogger(StepExecutor.class);

    /**
     * stepCode → 已注册步骤
     */
    private final Map<String, Step> stepMap;

    /**
     * 內联表达式缓存
     */
    private final Map<StepCacheKey, Step> inlineExpressionStepMap;

    /**
     * StepContentType → AbstractStepHandler，供步骤组装与 IF 内联表达式执行共用。
     * key 与 {@link AbstractStepHandler#getStepContentType()} 一致，例如 AVIATOR、JEXL。
     */
    private final Map<String, AbstractStepHandler> stepHandlerMap;

    public StepExecutor(StepDataProvider stepDataProvider, List<AbstractStepHandler> stepHandlers) {
        /* 初始化 stepMap */
        this.stepMap = new ConcurrentHashMap<>();
        this.inlineExpressionStepMap = new ConcurrentHashMap<>();
        /* 获取 step 数据 */
        List<StepInputData> stepDataList = null;
        if (stepDataProvider != null) {
            stepDataList = stepDataProvider.loadStepDataList();
        }
        /* 组装 stepHandlerMap，构建后保留供运行时按 contentType 查找 Handler */
        Map<String, AbstractStepHandler> handlerMap = new HashMap<>();
        if (StepFlowUtils.isNotEmpty(stepHandlers)) {
            for (AbstractStepHandler stepHandler : stepHandlers) {
                if (handlerMap.containsKey(stepHandler.getStepContentType())) {
                    log.warn("AbstractStepHandler {} 被覆盖", stepHandler.getStepContentType());
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
            for (StepInputData stepInputData : stepDataList) {
                Step existingStep = this.stepMap.get(stepInputData.getStepCode());
                if (existingStep != null) {
                    duplicateSet.add(stepInputData.getStepCode());
                    continue;
                }
                // 查找对应的 AbstractStepHandler
                AbstractStepHandler stepHandler = this.stepHandlerMap.get(stepInputData.getContentType());
                if (stepHandler == null) {
                    illegalList.add(String.format("Step[%s] 的 contentType[%s] 不存在", stepInputData.getStepCode(), stepInputData.getContentType()));
                    continue;
                }
                // 构建 StepData
                StepData stepData = StepData.builder()
                        .stepCode(stepInputData.getStepCode())
                        .stepName(stepInputData.getStepName())
                        .stepType(stepInputData.getStepType())
                        .contentType(stepInputData.getContentType())
                        .content(stepInputData.getContent())
                        .returnFieldList(stepInputData.getReturnFieldList())
                        .build();
                // 设置 参数列表
                List<String> paramNameList = stepHandler.getParamNameList(stepData);
                if (StepFlowUtils.isNotEmpty(paramNameList)) {
                    paramNameList = paramNameList.stream().distinct().collect(Collectors.toList());
                    stepData.setParamNameList(paramNameList);
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
     * 是否已注册指定 {@link AbstractStepHandler#getStepContentType()} 对应的 Handler。
     *
     * @param contentType 步骤内容类型，如 AVIATOR
     * @return true 表示存在对应 Handler
     */
    public boolean containsStepContentType(String contentType) {
        return this.stepHandlerMap.containsKey(contentType);
    }

    /**
     * 是否未注册指定 {@link AbstractStepHandler#getStepContentType()} 对应的 Handler。
     *
     * @param contentType 步骤内容类型，如 AVIATOR
     * @return true 表示不存在对应 Handler
     */
    public boolean isMissingStepContentType(String contentType) {
        return !this.containsStepContentType(contentType);
    }

    /**
     * 使用指定 contentType 的 AbstractStepHandler 执行内联表达式（如 IF 条件中的 AVIATOR("a > b")）。
     * <p>
     * 将当前 {@link StepFlowContext#getContextMap()} 作为表达式变量传入 Handler。
     * </p>
     *
     * @param contentType     AbstractStepHandler 对应的 StepContentType
     * @param expression      表达式
     * @param stepFlowContext 流程上下文
     * @return Handler 执行结果
     */
    public Object executeInlineExpression(String contentType,
                                          String expression,
                                          StepFlowContext stepFlowContext) {
        // 获取 stepHandler
        AbstractStepHandler stepHandler = this.stepHandlerMap.get(contentType);
        if (stepHandler == null) {
            throw new StepFlowException(String.format("表达式类型[%s]不存在", contentType));
        }
        // 从缓存中获取 Step 对象，没有则新建
        Step step = this.inlineExpressionStepMap.computeIfAbsent(
                new StepCacheKey(contentType, expression), // key
                k -> {
                    StepData stepData = StepData.builder()
                            .stepCode("-")
                            .stepName("-")
                            .stepType("INLINE_EXPRESSION")
                            .contentType(contentType)
                            .content(expression)
                            .build();
                    // 获取参数列表，存入stepData
                    List<String> paramNameList = stepHandler.getParamNameList(stepData);
                    stepData.setParamNameList(paramNameList);
                    // 返回结果
                    return new Step(stepData, stepHandler);
                }
        );
        // 执行step并返回结果
        return step.execute(stepFlowContext, null);
    }
}
