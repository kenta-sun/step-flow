package io.github.kentasun.stepflow.engine;

import io.github.kentasun.stepflow.config.StepFlowEngineProperties;
import lombok.Setter;

/**
 * 表达式引擎提供者抽象基类
 */
@Setter
public abstract class AbstractExpressionEngineProvider {

    // 参数取值引擎通用配置
    protected StepFlowEngineProperties paramEngineProperties;

    // 条件判断引擎通用配置
    protected StepFlowEngineProperties conditionEngineProperties;

    // 业务计算引擎通用配置
    protected StepFlowEngineProperties businessEngineProperties;

    // 参数取值引擎编程式定制回调
    protected EngineCustomizer paramEngineCustomizer;

    // 条件判断引擎编程式定制回调
    protected EngineCustomizer conditionEngineCustomizer;

    // 业务计算引擎编程式定制回调
    protected EngineCustomizer businessEngineCustomizer;


    /**
     * 构建参数取值引擎。
     */
    public abstract ParamExpressionEngine buildParamExpressionEngine();

    /**
     * 构建条件判断引擎。
     */
    public abstract ConditionExpressionEngine buildConditionExpressionEngine();

    /**
     * 构建业务计算引擎。
     */
    public abstract BusinessExpressionEngine buildBusinessExpressionEngine();
}
