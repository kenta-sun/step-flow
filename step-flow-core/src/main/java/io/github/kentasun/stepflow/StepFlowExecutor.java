package io.github.kentasun.stepflow;

import io.github.kentasun.stepflow.api.step.AbstractStepHandler;
import io.github.kentasun.stepflow.config.StepFlowConfigProperties;
import io.github.kentasun.stepflow.dto.ExecutorsContext;
import io.github.kentasun.stepflow.api.dto.StepFlowContext;
import io.github.kentasun.stepflow.flow.FlowExecutor;
import io.github.kentasun.stepflow.api.flow.FlowProvider;
import io.github.kentasun.stepflow.step.StepExecutor;
import io.github.kentasun.stepflow.step.handler.JavaStepHandler;
import io.github.kentasun.stepflow.api.step.AbstractJavaStep;
import io.github.kentasun.stepflow.api.step.StepDataProvider;
import io.github.kentasun.stepflow.threadpool.StepFlowThreadPoolFactory;
import io.github.kentasun.stepflow.api.utils.StepFlowUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

/**
 * step-flow 核心执行器
 */
public class StepFlowExecutor {

    // 执行器上下文
    private final ExecutorsContext executorsContext;

    private StepFlowExecutor(ExecutorsContext executorsContext) {
        this.executorsContext = executorsContext;
    }

    /**
     * 根据 flowCode 执行对应流程。
     *
     * @param flowCode   流程编码
     * @param contextMap 流程执行上下文变量
     * @return 执行结果上下文映射
     */
    public Map<String, Object> executeByFlowCode(String flowCode, Map<String, Object> contextMap) {
        return this.executorsContext.executeByFlowCode(
                flowCode,
                StepFlowContext.builder().contextMap(contextMap).build()
        );
    }

    /**
     * 创建 Builder。
     *
     * @param stepDataProvider stepData 提供者（必填）
     * @param flowProvider     flow 提供者（必填）
     * @return Builder 实例
     */
    public static Builder builder(StepDataProvider stepDataProvider, FlowProvider flowProvider) {
        return new Builder(stepDataProvider, flowProvider);
    }

    /**
     * {@link StepFlowExecutor} 构建器。
     */
    public static class Builder {

        private final StepDataProvider stepDataProvider;

        private final FlowProvider flowProvider;

        private StepFlowConfigProperties configProperties;

        private Map<String, AbstractJavaStep> javaStepMap;

        private List<AbstractStepHandler> stepHandlerList;

        private ExecutorService parallelThreadPool;

        public Builder(StepDataProvider stepDataProvider, FlowProvider flowProvider) {
            this.stepDataProvider = stepDataProvider;
            this.flowProvider = flowProvider;
        }

        public Builder javaStepMap(Map<String, AbstractJavaStep> javaStepMap) {
            this.javaStepMap = javaStepMap;
            return this;
        }

        public Builder stepHandlerList(List<AbstractStepHandler> stepHandlerList) {
            this.stepHandlerList = stepHandlerList;
            return this;
        }

        public Builder configProperties(StepFlowConfigProperties configProperties) {
            this.configProperties = configProperties;
            return this;
        }

        public Builder parallelThreadPool(ExecutorService parallelThreadPool) {
            this.parallelThreadPool = parallelThreadPool;
            return this;
        }

        public StepFlowExecutor build() {
            // 给一些Bean设置默认实现
            this.setDefaultBean();
            // 构建 step 执行器
            StepExecutor stepExecutor = this.buildStepExecutor();
            // 构建 flow 执行器
            FlowExecutor flowExecutor = new FlowExecutor(this.flowProvider, stepExecutor);

            return new StepFlowExecutor(ExecutorsContext.builder()
                    .stepExecutor(stepExecutor)
                    .flowExecutor(flowExecutor)
                    .stepFlowParallelThreadPool(this.parallelThreadPool)
                    .build());
        }

        /**
         * 给未显式设置的组件填充默认实现。
         */
        private void setDefaultBean() {

            if (this.configProperties == null) {
                this.configProperties = new StepFlowConfigProperties();
            }

            if (this.parallelThreadPool == null) {
                StepFlowThreadPoolFactory factory = new StepFlowThreadPoolFactory(this.configProperties);
                this.parallelThreadPool = factory.getStepFlowParallelThreadPool();
            }

            if (this.javaStepMap == null) {
                this.javaStepMap = new HashMap<>();
            }
        }

        /**
         * 构建 {@link StepExecutor}，注册所有 AbstractStepHandler。
         */
        private StepExecutor buildStepExecutor() {
            /* AbstractStepHandler */
            // 先注册内置的 AbstractStepHandler
            JavaStepHandler javaStepHandler = new JavaStepHandler(this.javaStepMap);
            List<AbstractStepHandler> stepHandlers = new ArrayList<>();
            stepHandlers.add(javaStepHandler);
            // 注册用户自定义 AbstractStepHandler，同 StepContentType 时覆盖内置实现
            if (StepFlowUtils.isNotEmpty(this.stepHandlerList)) {
                stepHandlers.addAll(this.stepHandlerList);
            }

            /* 创建 StepExecutor 对象并返回 */
            return new StepExecutor(this.stepDataProvider, stepHandlers);
        }

    }
}
