package io.github.kentasun.stepflow.flow.dto.node;

import io.github.kentasun.stepflow.api.dto.StepFlowContext;
import io.github.kentasun.stepflow.dto.ExecutorsContext;
import io.github.kentasun.stepflow.flow.dto.FlowNodeValidateContext;

import java.util.List;
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

        // 异步执行所有任务 flowNode
        CompletableFuture<?>[] futures = this.flowNodeList.stream()
                .map(flowNode -> CompletableFuture.runAsync(
                        () -> flowNode.execute(stepFlowContext, executorsContext),
                        stepFlowParallelThreadPool
                ))
                .toArray(CompletableFuture[]::new);

        // 等待所有 flowNode 执行完成
        CompletableFuture.allOf(futures).join();
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
