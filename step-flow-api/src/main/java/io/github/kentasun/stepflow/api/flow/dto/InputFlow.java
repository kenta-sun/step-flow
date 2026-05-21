package io.github.kentasun.stepflow.api.flow.dto;

import java.util.List;

/**
 * 开发者传入的 Flow 信息
 */
public class InputFlow {

    // 流程标识
    private String flowCode;

    // 流程名称
    private String flowName;

    // 流程类型
    private String flowType;

    // 流程正文，要求是JSON格式的字符串
    private String content;

    // 返回字段列表，多个返回字段配置在这里，否则为空
    private List<String> returnFieldList;

    public InputFlow(String flowCode, String flowName, String flowType, String content, List<String> returnFieldList) {
        this.flowCode = flowCode;
        this.flowName = flowName;
        this.flowType = flowType;
        this.content = content;
        this.returnFieldList = returnFieldList;
    }

    public InputFlow() {
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

    public String getContent() {
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

    public void setContent(String content) {
        this.content = content;
    }

    public void setReturnFieldList(List<String> returnFieldList) {
        this.returnFieldList = returnFieldList;
    }

    public String toString() {
        return "InputFlow(flowCode=" + this.getFlowCode() + ", flowName=" + this.getFlowName() + ", flowType=" + this.getFlowType() + ", content=" + this.getContent() + ", returnFieldList=" + this.getReturnFieldList() + ")";
    }

    public static InputFlowBuilder builder() {
        return new InputFlowBuilder();
    }

    public static class InputFlowBuilder {
        private String flowCode;
        private String flowName;
        private String flowType;
        private String content;
        private List<String> returnFieldList;

        InputFlowBuilder() {
        }

        public InputFlowBuilder flowCode(String flowCode) {
            this.flowCode = flowCode;
            return this;
        }

        public InputFlowBuilder flowName(String flowName) {
            this.flowName = flowName;
            return this;
        }

        public InputFlowBuilder flowType(String flowType) {
            this.flowType = flowType;
            return this;
        }

        public InputFlowBuilder content(String content) {
            this.content = content;
            return this;
        }

        public InputFlowBuilder returnFieldList(List<String> returnFieldList) {
            this.returnFieldList = returnFieldList;
            return this;
        }

        public InputFlow build() {
            return new InputFlow(this.flowCode, this.flowName, this.flowType, this.content, this.returnFieldList);
        }

        public String toString() {
            return "InputFlow.InputFlowBuilder(flowCode=" + this.flowCode + ", flowName=" + this.flowName + ", flowType=" + this.flowType + ", content=" + this.content + ", returnFieldList=" + this.returnFieldList + ")";
        }
    }
}
