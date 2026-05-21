package io.github.kentasun.stepflow.api.dto;

import java.util.Map;

/**
 * 表达式引擎计算公式需要的入参实体类
 */
public class StepFlowContext {

    // 参数 map
    private Map<String, Object> contextMap;

    public StepFlowContext(Map<String, Object> contextMap) {
        this.contextMap = contextMap;
    }

    public StepFlowContext() {
    }

    public void putAll(Map<String, Object> map) {
        contextMap.putAll(map);
    }

    public void put(String key, Object value) {
        contextMap.put(key, value);
    }

    public Object get(String key) {
        return contextMap.get(key);
    }

    public Map<String, Object> getContextMap() {
        return this.contextMap;
    }

    public void setContextMap(Map<String, Object> contextMap) {
        this.contextMap = contextMap;
    }

    public String toString() {
        return "StepFlowContext(contextMap=" + this.getContextMap() + ")";
    }

    public static StepFlowContextBuilder builder() {
        return new StepFlowContextBuilder();
    }

    public static class StepFlowContextBuilder {
        private Map<String, Object> contextMap;

        StepFlowContextBuilder() {
        }

        public StepFlowContextBuilder contextMap(Map<String, Object> contextMap) {
            this.contextMap = contextMap;
            return this;
        }

        public StepFlowContext build() {
            return new StepFlowContext(this.contextMap);
        }

        public String toString() {
            return "StepFlowContext.StepFlowContextBuilder(contextMap=" + this.contextMap + ")";
        }
    }
}
