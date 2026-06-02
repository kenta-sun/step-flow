package io.github.kentasun.stepflow.flow;

import io.github.kentasun.stepflow.api.dto.StepFlowContext;
import io.github.kentasun.stepflow.api.flow.FlowProvider;
import io.github.kentasun.stepflow.api.flow.dto.InputFlow;
import io.github.kentasun.stepflow.dto.ExecutorsContext;
import io.github.kentasun.stepflow.api.exception.StepFlowException;
import io.github.kentasun.stepflow.flow.dto.Flow;
import io.github.kentasun.stepflow.flow.dto.FlowNodeValidateContext;
import io.github.kentasun.stepflow.flow.dto.node.FlowNode;
import io.github.kentasun.stepflow.sfl.SflParser;
import io.github.kentasun.stepflow.step.StepExecutor;
import io.github.kentasun.stepflow.api.utils.StepFlowUtils;
import org.slf4j.Logger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * flow 功能总入口
 */
public class FlowExecutor {

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(FlowExecutor.class);

    private final Map<String, Flow> flowMap;

    public FlowExecutor(FlowProvider flowProvider, StepExecutor stepExecutor) {
        this.flowMap = new ConcurrentHashMap<>();
        List<InputFlow> inputFlowList = null;
        if (flowProvider != null) {
            inputFlowList = flowProvider.loadFlowList();
        }
        /* 组装 flow 对象 */
        if (StepFlowUtils.isNotEmpty(inputFlowList)) {
            // 所有的 flowCode 的 set
            Set<String> flowCodeSet = new HashSet<>();
            // 校验用上下文
            FlowNodeValidateContext validateContext = FlowNodeValidateContext.builder()
                    .illegalMsgMap(new HashMap<>())
                    .stepExecutor(stepExecutor)
                    .flowCodeSet(inputFlowList.stream().map(InputFlow::getFlowCode).collect(Collectors.toSet()))
                    .build();

            // 组装 flow
            for (InputFlow inputFlow : inputFlowList) {
                if (flowCodeSet.contains(inputFlow.getFlowCode())) {
                    validateContext.saveErrMsg(
                            inputFlow.getFlowCode(),
                            "flowCode重复"
                    );
                }
                flowCodeSet.add(inputFlow.getFlowCode());
                // 校验流程信息是否合法
                FlowNode flowNode = null;
                try {
                    // 使用 SFL 语法分析器解析 InputFlow.content
                    flowNode = SflParser.parse(inputFlow.getContent());
                    // 校验 flowNode
                    flowNode.validate(validateContext, inputFlow.getFlowCode());
                } catch (Throwable e) {
                    log.error("Flow 的 content 解析异常，Flow 信息：{}", inputFlow, e);
                    validateContext.saveErrMsg(
                            inputFlow.getFlowCode(),
                            "content解析异常: " + e.getMessage()
                    );
                }

                // 放入 flowMap
                this.flowMap.put(
                        inputFlow.getFlowCode(),
                        Flow.builder()
                                .flowCode(inputFlow.getFlowCode())
                                .flowName(inputFlow.getFlowName())
                                .flowType(inputFlow.getFlowType())
                                .content(flowNode)
                                .returnFieldList(inputFlow.getReturnFieldList())
                                .build()
                );
            }

            String errMsg = validateContext.buildErrMsg();
            if (StepFlowUtils.isNotBlank(errMsg)) {
                throw new StepFlowException(errMsg);
            }
            log.info("FlowExecutor has been created, {} flows has been loaded.", this.flowMap.size());
        }
    }

    /**
     * 执行流程
     *
     * @param flowCode         流程代码
     * @param stepFlowContext  上下文对象
     * @param executorsContext 用于随着上下文一起传递的各种执行器
     * @return 流程执行结果
     */
    public Map<String, Object> executeByFlowCode(final String flowCode, StepFlowContext stepFlowContext, ExecutorsContext executorsContext) {
        Flow flow = this.flowMap.get(flowCode);
        if (flow == null) {
            throw new StepFlowException(String.format("【%s】流程不存在", flowCode));
        }
        return flow.execute(stepFlowContext, executorsContext);
    }
}
