package io.github.kentasun.stepflow.flow.dto;

import io.github.kentasun.stepflow.step.StepExecutor;
import io.github.kentasun.stepflow.api.utils.StepFlowUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * FlowNode 递归校验上下文
 */
public class FlowNodeValidateContext {

    private Map<String, List<String>> illegalMsgMap;

    private StepExecutor stepExecutor;

    private Set<String> flowCodeSet;

    public FlowNodeValidateContext(Map<String, List<String>> illegalMsgMap, StepExecutor stepExecutor, Set<String> flowCodeSet) {
        this.illegalMsgMap = illegalMsgMap;
        this.stepExecutor = stepExecutor;
        this.flowCodeSet = flowCodeSet;
    }

    public FlowNodeValidateContext() {
    }

    /**
     * flowCode 是否不存在
     *
     * @param flowCode 流程标识
     * @return true-不存在; false-存在
     */
    public boolean flowCodeNotExist(String flowCode) {
        return !this.flowCodeSet.contains(flowCode);
    }

    /**
     * stepCode 是否不存在
     *
     * @param stepCode 步骤标识
     * @return true-不存在; false-存在
     */
    public boolean stepCodeNotExist(String stepCode) {
        return !this.stepExecutor.hasStepCode(stepCode);
    }

    /**
     * 保存校验发现的异常信息
     *
     * @param globalFlowCode FlowNode 所在的 Flow 对象的 flowCode
     * @param errMsg         异常信息
     */
    public void saveErrMsg(String globalFlowCode, String errMsg) {
        List<String> list = this.illegalMsgMap.computeIfAbsent(globalFlowCode, k -> new ArrayList<>());
        list.add(errMsg);
    }

    /**
     * 拼接最终的信息字符串
     *
     * @return 最终的信息字符串
     */
    public String buildErrMsg() {
        if (StepFlowUtils.isEmpty(this.illegalMsgMap)) {
            return null;
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append("========== 以下flow不合法 ==========").append("\n");
            for (Map.Entry<String, List<String>> entry : this.illegalMsgMap.entrySet()) {
                String flowCode = entry.getKey();
                List<String> msgList = entry.getValue();
                sb.append(flowCode).append(":\n");
                for (String msg : msgList) {
                    sb.append("    ").append(msg).append("\n");
                }
            }
            return sb.toString();
        }
    }

    public Map<String, List<String>> getIllegalMsgMap() {
        return this.illegalMsgMap;
    }

    public StepExecutor getStepExecutor() {
        return this.stepExecutor;
    }

    public Set<String> getFlowCodeSet() {
        return this.flowCodeSet;
    }

    public static FlowNodeValidateContextBuilder builder() {
        return new FlowNodeValidateContextBuilder();
    }

    public static class FlowNodeValidateContextBuilder {
        private Map<String, List<String>> illegalMsgMap;
        private StepExecutor stepExecutor;
        private Set<String> flowCodeSet;

        FlowNodeValidateContextBuilder() {
        }

        public FlowNodeValidateContextBuilder illegalMsgMap(Map<String, List<String>> illegalMsgMap) {
            this.illegalMsgMap = illegalMsgMap;
            return this;
        }

        public FlowNodeValidateContextBuilder stepExecutor(StepExecutor stepExecutor) {
            this.stepExecutor = stepExecutor;
            return this;
        }

        public FlowNodeValidateContextBuilder flowCodeSet(Set<String> flowCodeSet) {
            this.flowCodeSet = flowCodeSet;
            return this;
        }

        public FlowNodeValidateContext build() {
            return new FlowNodeValidateContext(this.illegalMsgMap, this.stepExecutor, this.flowCodeSet);
        }

        public String toString() {
            return "FlowNodeValidateContext.FlowNodeValidateContextBuilder(illegalMsgMap=" + this.illegalMsgMap + ", stepExecutor=" + this.stepExecutor + ", flowCodeSet=" + this.flowCodeSet + ")";
        }
    }
}
