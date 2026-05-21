package io.github.kentasun.stepflow.threadpool;

import io.github.kentasun.stepflow.config.StepFlowConfigProperties;
import io.github.kentasun.stepflow.utils.StepFlowUtils;

import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 线程池工厂
 */
public class StepFlowThreadPoolFactory {

    private final StepFlowConfigProperties configProperties;

    public StepFlowThreadPoolFactory(StepFlowConfigProperties configProperties) {
        this.configProperties = configProperties;
    }

    /**
     * 创建异步执行 FLowNode 的线程池
     *
     * @return 异步执行 FLowNode 的线程池
     */
    public ExecutorService getStepFlowParallelThreadPool() {
        /* 线程工厂 */
        StepFlowThreadFactory threadFactory = new StepFlowThreadFactory("pool-stepFlowParallelThreadPool-thread-");
        // 设为非守护线程，主线程完成后子线程会继续完成自己的任务，而不会因为主线程结束而中断。
        threadFactory.setDaemon(false);
        // 线程会继承父线程的优先级。为了防止线程池里的线程因为“出身”问题导致执行过慢或抢占过多资源，显式重置为“正常优先级”
        threadFactory.setThreadPriority(Thread.NORM_PRIORITY);

        /* 线程池配置项 */
        StepFlowConfigProperties.ThreadPoolProperty properties = Optional.ofNullable(configProperties.getParallelThreadPool()).orElse(new StepFlowConfigProperties.ThreadPoolProperty());

        /* 创建线程池 */
        return new ThreadPoolExecutor(
                StepFlowUtils.defaultIfNull(properties.getCorePoolSize(), 16),
                StepFlowUtils.defaultIfNull(properties.getMaximumPoolSize(), 16),
                StepFlowUtils.defaultIfNull(properties.getKeepAliveTime(), 0L),
                StepFlowUtils.defaultIfNull(properties.getUnit(), TimeUnit.SECONDS),
                new LinkedBlockingDeque<>(StepFlowUtils.defaultIfNull(properties.getWorkQueueSize(), 10000)),
                threadFactory,
                StepFlowUtils.defaultIfNull(properties.getRejectedHandler(), new ThreadPoolExecutor.CallerRunsPolicy())
        );
    }
}
