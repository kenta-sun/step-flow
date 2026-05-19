package io.github.kentasun.stepflow;

import io.github.kentasun.stepflow.config.StepFlowConfigProperties;
import io.github.kentasun.stepflow.dto.ExecutorsContext;
import io.github.kentasun.stepflow.dto.StepFlowContext;
import io.github.kentasun.stepflow.engine.*;
import io.github.kentasun.stepflow.exception.StepFlowException;
import io.github.kentasun.stepflow.flow.FlowExecutor;
import io.github.kentasun.stepflow.flow.intf.FlowProvider;
import io.github.kentasun.stepflow.step.StepExecutor;
import io.github.kentasun.stepflow.step.handler.ExpressionStepHandler;
import io.github.kentasun.stepflow.step.handler.JavaStepHandler;
import io.github.kentasun.stepflow.step.intf.JavaStep;
import io.github.kentasun.stepflow.step.intf.StepDataProvider;
import io.github.kentasun.stepflow.step.intf.StepHandler;
import io.github.kentasun.stepflow.threadpool.StepFlowThreadPoolFactory;
import io.github.kentasun.stepflow.utils.StepFlowUtils;

import java.util.*;
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
    public Map<String, Object> executeByFLowCode(String flowCode, Map<String, Object> contextMap) {
        return executorsContext.executeByFLowCode(
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

        private Map<String, JavaStep> javaStepMap;

        private List<StepHandler> stepHandlerList;

        private ExecutorService parallelThreadPool;

        private ExpressionEngine expressionEngine;

        private EngineCustomizer<?> engineCustomizer;

        public Builder(StepDataProvider stepDataProvider, FlowProvider flowProvider) {
            this.stepDataProvider = stepDataProvider;
            this.flowProvider = flowProvider;
        }

        public Builder javaStepMap(Map<String, JavaStep> javaStepMap) {
            this.javaStepMap = javaStepMap;
            return this;
        }

        public Builder stepHandlerList(List<StepHandler> stepHandlerList) {
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

        public Builder expressionEngine(ExpressionEngine expressionEngine) {
            this.expressionEngine = expressionEngine;
            return this;
        }

        public Builder engineCustomizer(EngineCustomizer<?> engineCustomizer) {
            this.engineCustomizer = engineCustomizer;
            return this;
        }

        public StepFlowExecutor build() {
            // 给一些Bean设置默认实现
            this.setDefaultBean();
            // 设置表达式引擎
            this.setExpressionEngine();
            // 构建 step 执行器
            StepExecutor stepExecutor = this.buildStepExecutor();
            // 构建 flow 执行器
            FlowExecutor flowExecutor = new FlowExecutor(flowProvider, stepExecutor);

            return new StepFlowExecutor(ExecutorsContext.builder()
                    .configProperties(configProperties)
                    .stepExecutor(stepExecutor)
                    .flowExecutor(flowExecutor)
                    .stepFlowParallelThreadPool(parallelThreadPool)
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
                StepFlowThreadPoolFactory factory = new StepFlowThreadPoolFactory(configProperties);
                this.parallelThreadPool = factory.getStepFlowParallelThreadPool();
            }

            if (this.javaStepMap == null) {
                this.javaStepMap = new HashMap<>();
            }
        }

        /**
         * 设置表达式引擎
         */
        private void setExpressionEngine() {
            // 若表达式引擎未显式设置，则通过 SPI 加载统一的 ExpressionEngineProvider
            if (this.expressionEngine == null) {
                // 通过 SPI 发现引擎 Provider 实现，避免 core 直接依赖任何具体引擎库
                AbstractExpressionEngineProvider provider = loadSpi();

                // 从 configProperties 中读取表达式引擎的独立配置，分别传入
                provider.setEngineProperties(configProperties.getEngineProperties());

                // 注入定制回调
                provider.setEngineCustomizer(this.engineCustomizer);

                // 创建引擎对象
                this.expressionEngine = provider.buildExpressionEngine();
            }
        }

        /**
         * 通过 Java SPI 加载 {@link AbstractExpressionEngineProvider} 的第一个实现类。
         *
         * @return {@link AbstractExpressionEngineProvider} 的第一个可用的实现实例
         * @throws StepFlowException 若 classpath 上没有任何实现时，抛出含引导信息的异常
         */
        private AbstractExpressionEngineProvider loadSpi() {
            Iterator<AbstractExpressionEngineProvider> it = ServiceLoader.load(AbstractExpressionEngineProvider.class).iterator();
            if (it.hasNext()) {
                return it.next();
            }
            throw new StepFlowException("No [engine.io.github.kentasun.stepflow.AbstractExpressionEngineProvider] implementation found on classpath.");
        }

        /**
         * 构建 {@link StepExecutor}，注册所有 StepHandler。
         */
        private StepExecutor buildStepExecutor() {
            /* StepHandler */
            // 先注册内置的 StepHandler
            List<StepHandler> stepHandlers = new ArrayList<>(Arrays.asList(
                    new JavaStepHandler(this.javaStepMap),
                    new ExpressionStepHandler(this.expressionEngine)
            ));
            // 注册用户自定义 StepHandler，同 StepContentType 时覆盖内置实现
            if (StepFlowUtils.isNotEmpty(stepHandlerList)) {
                stepHandlers.addAll(stepHandlerList);
            }

            /* 创建 StepExecutor 对象并返回 */
            return new StepExecutor(this.stepDataProvider, stepHandlers);
        }

    }
}
