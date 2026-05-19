package io.github.kentasun.stepflow.engine;

import io.github.kentasun.stepflow.config.StepFlowEngineProperties;
import io.github.kentasun.stepflow.step.intf.StepHandler;
import lombok.Setter;

/**
 * {@link StepHandler} 提供者抽象类
 */
@Setter
public abstract class AbstractStepHandlerProvider {

    // 通用配置
    protected StepFlowEngineProperties engineProperties;

    // 定制回调
    protected StepHandlerCustomizer stepHandlerCustomizer;

    /**
     * 构建 {@link StepHandler}。
     *
     * @return {@link StepHandler} 对象
     */
    public abstract StepHandler buildStepHandler();
}
