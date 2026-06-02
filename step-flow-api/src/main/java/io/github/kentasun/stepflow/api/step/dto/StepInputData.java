package io.github.kentasun.stepflow.api.step.dto;

import java.util.List;

/**
 * @author kenta-sun
 */
public class StepInputData {

    // 步骤标识
    private final String stepCode;

    // 步骤名称
    private final String stepName;

    // 步骤类型
    private final String stepType;

    // 内容类型，详情见 StepContentType
    private final String contentType;

    // 步骤内容
    private final String content;

    // 返回字段列表，多个返回字段配置在这里，否则为空
    private List<String> returnFieldList;

    public StepInputData(String stepCode, String stepName, String stepType, String contentType, String content, List<String> returnFieldList) {
        this.stepCode = stepCode;
        this.stepName = stepName;
        this.stepType = stepType;
        this.contentType = contentType;
        this.content = content;
        this.returnFieldList = returnFieldList;
    }

    public String getStepCode() {
        return this.stepCode;
    }

    public String getStepName() {
        return this.stepName;
    }

    public String getStepType() {
        return this.stepType;
    }

    public String getContentType() {
        return this.contentType;
    }

    public String getContent() {
        return this.content;
    }

    public List<String> getReturnFieldList() {
        return this.returnFieldList;
    }

    public void setReturnFieldList(List<String> returnFieldList) {
        this.returnFieldList = returnFieldList;
    }

    public String toString() {
        return "StepInputData(stepCode=" + this.getStepCode() + ", stepName=" + this.getStepName() + ", stepType=" + this.getStepType() + ", contentType=" + this.getContentType() + ", content=" + this.getContent() + ", returnFieldList=" + this.getReturnFieldList() + ")";
    }

    public static StepInputDataBuilder builder() {
        return new StepInputDataBuilder();
    }

    public static class StepInputDataBuilder {
        private String stepCode;
        private String stepName;
        private String stepType;
        private String contentType;
        private String content;
        private List<String> returnFieldList;

        StepInputDataBuilder() {
        }

        public StepInputDataBuilder stepCode(String stepCode) {
            this.stepCode = stepCode;
            return this;
        }

        public StepInputDataBuilder stepName(String stepName) {
            this.stepName = stepName;
            return this;
        }

        public StepInputDataBuilder stepType(String stepType) {
            this.stepType = stepType;
            return this;
        }

        public StepInputDataBuilder contentType(String contentType) {
            this.contentType = contentType;
            return this;
        }

        public StepInputDataBuilder content(String content) {
            this.content = content;
            return this;
        }

        public StepInputDataBuilder returnFieldList(List<String> returnFieldList) {
            this.returnFieldList = returnFieldList;
            return this;
        }

        public StepInputData build() {
            return new StepInputData(this.stepCode, this.stepName, this.stepType, this.contentType, this.content, this.returnFieldList);
        }

        public String toString() {
            return "StepInputDataBuilder(stepCode=" + this.stepCode + ", stepName=" + this.stepName + ", stepType=" + this.stepType + ", contentType=" + this.contentType + ", content=" + this.content + ", returnFieldList=" + this.returnFieldList + ")";
        }
    }
}
