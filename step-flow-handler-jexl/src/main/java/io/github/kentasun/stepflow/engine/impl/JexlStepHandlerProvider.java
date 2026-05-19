package io.github.kentasun.stepflow.engine.impl;

import io.github.kentasun.stepflow.engine.AbstractStepHandlerProvider;
import io.github.kentasun.stepflow.step.intf.StepHandler;

/**
 * 基于 Apache Commons JEXL 的 {@link StepHandler} 提供者，实现 {@link AbstractStepHandlerProvider} SPI 接口。
 */
public class JexlStepHandlerProvider extends AbstractStepHandlerProvider {

    public JexlStepHandlerProvider() {}

    @Override
    public StepHandler buildStepHandler() {
        return new JexlStepHandler(engineProperties, stepHandlerCustomizer);
    }
}
