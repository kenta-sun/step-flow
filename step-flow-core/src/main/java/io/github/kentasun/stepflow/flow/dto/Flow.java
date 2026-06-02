package io.github.kentasun.stepflow.flow.dto;

import io.github.kentasun.stepflow.api.dto.StepFlowContext;
import io.github.kentasun.stepflow.dto.ExecutorsContext;
import io.github.kentasun.stepflow.flow.dto.node.FlowNode;
import io.github.kentasun.stepflow.api.utils.StepFlowUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Flow {

    // 流程标识
    private String flowCode;

    // 流程名称
    private String flowName;

    // 流程类型
    private String flowType;

    // 流程正文
    private FlowNode content;

    // 返回字段列表，多个返回字段配置在这里，否则为空
    private List<String> returnFieldList;

    public Flow(String flowCode, String flowName, String flowType, FlowNode content, List<String> returnFieldList) {
        this.flowCode = flowCode;
        this.flowName = flowName;
        this.flowType = flowType;
        this.content = content;
        this.returnFieldList = returnFieldList;
    }

    public Flow() {
    }

    /**
     * 执行流程
     *
     * @param stepFlowContext  步骤上下文，用于传递
     * @param executorsContext 执行器上下文
     * @return 返回 returnFieldList 指定的参数
     */
    public Map<String, Object> execute(StepFlowContext stepFlowContext, ExecutorsContext executorsContext) {
        /* 执行 flow */
        this.content.execute(stepFlowContext, executorsContext);

        /* flow 如果配置了返回值，则直接从上下文中获取 */
        if (StepFlowUtils.isNotEmpty(this.returnFieldList)) {
            Map<String, Object> returnMap = new HashMap<>();
            for (String key : this.returnFieldList) {
                Object value = stepFlowContext.get(key);
                returnMap.put(key, value);
            }
            return returnMap;
        }

        /* 默认无返回值 */
        return null;
    }

    public String getFlowCode() {
        return this.flowCode;
    }

    public String getFlowName() {
        return this.flowName;
    }

    public String getFlowType() {
        return this.flowType;
    }

    public FlowNode getContent() {
        return this.content;
    }

    public List<String> getReturnFieldList() {
        return this.returnFieldList;
    }

    public void setFlowCode(String flowCode) {
        this.flowCode = flowCode;
    }

    public void setFlowName(String flowName) {
        this.flowName = flowName;
    }

    public void setFlowType(String flowType) {
        this.flowType = flowType;
    }

    public void setContent(FlowNode content) {
        this.content = content;
    }

    public void setReturnFieldList(List<String> returnFieldList) {
        this.returnFieldList = returnFieldList;
    }

    public String toString() {
        return "Flow(flowCode=" + this.getFlowCode() + ", flowName=" + this.getFlowName() + ", flowType=" + this.getFlowType() + ", content=" + this.getContent() + ", returnFieldList=" + this.getReturnFieldList() + ")";
    }

    public static FlowBuilder builder() {
        return new FlowBuilder();
    }

    public static class FlowBuilder {
        private String flowCode;
        private String flowName;
        private String flowType;
        private FlowNode content;
        private List<String> returnFieldList;

        FlowBuilder() {
        }

        public FlowBuilder flowCode(String flowCode) {
            this.flowCode = flowCode;
            return this;
        }

        public FlowBuilder flowName(String flowName) {
            this.flowName = flowName;
            return this;
        }

        public FlowBuilder flowType(String flowType) {
            this.flowType = flowType;
            return this;
        }

        public FlowBuilder content(FlowNode content) {
            this.content = content;
            return this;
        }

        public FlowBuilder returnFieldList(List<String> returnFieldList) {
            this.returnFieldList = returnFieldList;
            return this;
        }

        public Flow build() {
            return new Flow(this.flowCode, this.flowName, this.flowType, this.content, this.returnFieldList);
        }

        public String toString() {
            return "Flow.FlowBuilder(flowCode=" + this.flowCode + ", flowName=" + this.flowName + ", flowType=" + this.flowType + ", content=" + this.content + ", returnFieldList=" + this.returnFieldList + ")";
        }
    }
}
