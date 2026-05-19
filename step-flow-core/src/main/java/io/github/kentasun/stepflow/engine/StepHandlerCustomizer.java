package io.github.kentasun.stepflow.engine;

import io.github.kentasun.stepflow.step.intf.StepHandler;

/**
 * 实现该类可以自定义 {@link StepHandler} 的配置
 *
 * @param <T> 需要自定义的对象的类型
 */
public interface StepHandlerCustomizer<T> {

    void customize(T target);
}
