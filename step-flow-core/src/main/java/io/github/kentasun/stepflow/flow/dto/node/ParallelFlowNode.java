package io.github.kentasun.stepflow.flow.dto.node;

import io.github.kentasun.stepflow.api.dto.StepFlowContext;
import io.github.kentasun.stepflow.dto.ExecutorsContext;
import io.github.kentasun.stepflow.dto.ForkMap;
import io.github.kentasun.stepflow.flow.dto.FlowNodeValidateContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

/**
 * 多个 FlowNode 多线程并发执行的 FlowNode
 */
public class ParallelFlowNode extends FlowNode {

    private final List<FlowNode> flowNodeList;

    public ParallelFlowNode(String type, List<FlowNode> flowNodeList) {
        super(type);
        this.flowNodeList = flowNodeList;
    }

    @Override
    public void execute(StepFlowContext stepFlowContext, ExecutorsContext executorsContext) {
        ExecutorService stepFlowParallelThreadPool = executorsContext.getStepFlowParallelThreadPool();
        Map<String, Object> contextMap = stepFlowContext.getContextMap();

        // 异步执行所有任务 flowNode
        List<StepFlowContext> forkContextList = new ArrayList<>();
        CompletableFuture<?>[] futures = this.flowNodeList.stream()
                .map(flowNode -> CompletableFuture.runAsync(
                        () -> {
                            // 新建分支上下文，通过上下文隔离解决并发问题
                            StepFlowContext forkContext = StepFlowContext.builder()
                                    .contextMap(new ForkMap<>(contextMap))
                                    .build();
                            // 执行分支
                            flowNode.execute(forkContext, executorsContext);
                            // 保存分支上下文信息
                            forkContextList.add(forkContext);
                        },
                        stepFlowParallelThreadPool
                ))
                .toArray(CompletableFuture[]::new);

        // 等待所有 flowNode 执行完成
        CompletableFuture.allOf(futures).join();
        // 整合所有 forkContext 的数据到 stepFlowContext
        for (StepFlowContext forkContext : forkContextList) {
            ForkMap<String, Object> forkMap = (ForkMap<String, Object>) forkContext.getContextMap();
            stepFlowContext.putAll(forkMap.getPrivateMap());
        }
    }

    @Override
    public void validate(FlowNodeValidateContext context, String globalFlowCode) {
        // 校验所有子节点
        for (FlowNode flowNode : this.flowNodeList) {
            flowNode.validate(context, globalFlowCode);
        }
    }

    public List<FlowNode> getFlowNodeList() {
        return this.flowNodeList;
    }
}
