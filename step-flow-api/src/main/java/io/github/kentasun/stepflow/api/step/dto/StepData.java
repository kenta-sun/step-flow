package io.github.kentasun.stepflow.api.step.dto;

import java.util.List;

/**
 * 步骤信息表
 */
public class StepData {

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

    /*
     * 该步骤需要的参数名称列表
     * 表达式或javaMethod类型可以有参数。如果公共参数map中的名称不对，需要映射成该列表中的名称。
     */
    private volatile List<String> paramNameList;

    // 返回字段列表，多个返回字段配置在这里，否则为空
    private List<String> returnFieldList;

    public StepData(String stepCode, String stepName, String stepType, String contentType, String content, List<String> paramNameList, List<String> returnFieldList) {
        this.stepCode = stepCode;
        this.stepName = stepName;
        this.stepType = stepType;
        this.contentType = contentType;
        this.content = content;
        this.paramNameList = paramNameList;
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

    public List<String> getParamNameList() {
        return this.paramNameList;
    }

    public List<String> getReturnFieldList() {
        return this.returnFieldList;
    }

    public void setParamNameList(List<String> paramNameList) {
        this.paramNameList = paramNameList;
    }

    public void setReturnFieldList(List<String> returnFieldList) {
        this.returnFieldList = returnFieldList;
    }

    public String toString() {
        return "StepData(stepCode=" + this.getStepCode() + ", stepName=" + this.getStepName() + ", stepType=" + this.getStepType() + ", contentType=" + this.getContentType() + ", content=" + this.getContent() + ", paramNameList=" + this.getParamNameList() + ", returnFieldList=" + this.getReturnFieldList() + ")";
    }

    public static StepDataBuilder builder() {
        return new StepDataBuilder();
    }

    public static class StepDataBuilder {
        private String stepCode;
        private String stepName;
        private String stepType;
        private String contentType;
        private String content;
        private List<String> paramNameList;
        private List<String> returnFieldList;

        StepDataBuilder() {
        }

        public StepDataBuilder stepCode(String stepCode) {
            this.stepCode = stepCode;
            return this;
        }

        public StepDataBuilder stepName(String stepName) {
            this.stepName = stepName;
            return this;
        }

        public StepDataBuilder stepType(String stepType) {
            this.stepType = stepType;
            return this;
        }

        public StepDataBuilder contentType(String contentType) {
            this.contentType = contentType;
            return this;
        }

        public StepDataBuilder content(String content) {
            this.content = content;
            return this;
        }

        public StepDataBuilder paramNameList(List<String> paramNameList) {
            this.paramNameList = paramNameList;
            return this;
        }

        public StepDataBuilder returnFieldList(List<String> returnFieldList) {
            this.returnFieldList = returnFieldList;
            return this;
        }

        public StepData build() {
            return new StepData(this.stepCode, this.stepName, this.stepType, this.contentType, this.content, this.paramNameList, this.returnFieldList);
        }

        public String toString() {
            return "StepData.StepDataBuilder(stepCode=" + this.stepCode + ", stepName=" + this.stepName + ", stepType=" + this.stepType + ", contentType=" + this.contentType + ", content=" + this.content + ", paramNameList=" + this.paramNameList + ", returnFieldList=" + this.returnFieldList + ")";
        }
    }
}
