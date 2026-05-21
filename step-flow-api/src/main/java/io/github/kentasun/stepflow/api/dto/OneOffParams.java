package io.github.kentasun.stepflow.api.dto;

import java.util.Map;

/**
 * StepHandler所需要的1次性参数
 */
public class OneOffParams {

    // 调用该步骤需要映射的参数，解决当前 contextMap 中的参数名和步骤需要的参数名对不上的问题。
    private Map<String, String> paramNameMap;

    // 调用该步骤需要的参数，单独隔离可以防止参数污染。
    private Map<String, Object> vars;

    public OneOffParams(Map<String, String> paramNameMap, Map<String, Object> vars) {
        this.paramNameMap = paramNameMap;
        this.vars = vars;
    }

    public OneOffParams() {
    }

    public Map<String, String> getParamNameMap() {
        return this.paramNameMap;
    }

    public Map<String, Object> getVars() {
        return this.vars;
    }

    public void setParamNameMap(Map<String, String> paramNameMap) {
        this.paramNameMap = paramNameMap;
    }

    public void setVars(Map<String, Object> vars) {
        this.vars = vars;
    }

    public String toString() {
        return "OneOffParams(paramNameMap=" + this.getParamNameMap() + ", vars=" + this.getVars() + ")";
    }

    public static OneOffParamsBuilder builder() {
        return new OneOffParamsBuilder();
    }

    public static class OneOffParamsBuilder {
        private Map<String, String> paramNameMap;
        private Map<String, Object> vars;

        OneOffParamsBuilder() {
        }

        public OneOffParamsBuilder paramNameMap(Map<String, String> paramNameMap) {
            this.paramNameMap = paramNameMap;
            return this;
        }

        public OneOffParamsBuilder vars(Map<String, Object> vars) {
            this.vars = vars;
            return this;
        }

        public OneOffParams build() {
            return new OneOffParams(this.paramNameMap, this.vars);
        }

        public String toString() {
            return "OneOffParams.OneOffParamsBuilder(paramNameMap=" + this.paramNameMap + ", vars=" + this.vars + ")";
        }
    }
}
