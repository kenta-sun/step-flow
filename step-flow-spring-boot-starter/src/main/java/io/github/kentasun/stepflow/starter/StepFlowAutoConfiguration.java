package io.github.kentasun.stepflow.starter;

import io.github.kentasun.stepflow.StepFlowExecutor;
import io.github.kentasun.stepflow.config.StepFlowConfigProperties;
import io.github.kentasun.stepflow.flow.intf.FlowProvider;
import io.github.kentasun.stepflow.step.intf.JavaStep;
import io.github.kentasun.stepflow.step.intf.StepDataProvider;
import io.github.kentasun.stepflow.step.intf.StepHandler;
import io.github.kentasun.stepflow.threadpool.StepFlowThreadPoolFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.lang.Nullable;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

/**
 * step-flow Spring Boot Starter 自动装配入口。
 */
public class StepFlowAutoConfiguration {

    @Bean
    @ConfigurationProperties(prefix = "stepflow")
    public StepFlowConfigProperties stepFlowConfigProperties() {
        return new StepFlowConfigProperties();
    }

    @Bean
    public StepFlowThreadPoolFactory stepFlowThreadPoolFactory(StepFlowConfigProperties stepFlowConfigProperties) {
        return new StepFlowThreadPoolFactory(stepFlowConfigProperties);
    }

    @Bean(name = "stepFlowParallelThreadPool")
    @ConditionalOnMissingBean(name = "stepFlowParallelThreadPool")
    public ExecutorService stepFlowParallelThreadPool(StepFlowThreadPoolFactory stepFlowThreadPoolFactory) {
        return stepFlowThreadPoolFactory.getStepFlowParallelThreadPool();
    }

    @Bean
    public StepFlowExecutor stepFlowExecutor(StepDataProvider stepDataProvider,
                                             FlowProvider flowProvider,
                                             StepFlowConfigProperties stepFlowConfigProperties,
                                             @Nullable Map<String, JavaStep> javaStepMap,
                                             @Nullable List<StepHandler> stepHandlerList,
                                             @Qualifier("stepFlowParallelThreadPool") ExecutorService stepFlowParallelThreadPool) {
        return StepFlowExecutor.builder(stepDataProvider, flowProvider)
                .configProperties(stepFlowConfigProperties)
                .javaStepMap(javaStepMap)
                .stepHandlerList(stepHandlerList)
                .parallelThreadPool(stepFlowParallelThreadPool)
                .build();
    }
}
