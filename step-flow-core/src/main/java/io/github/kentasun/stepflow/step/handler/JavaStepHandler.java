package io.github.kentasun.stepflow.step.handler;

import io.github.kentasun.stepflow.api.dto.OneOffParams;
import io.github.kentasun.stepflow.api.exception.StepFlowException;
import io.github.kentasun.stepflow.api.step.AbstractJavaStep;
import io.github.kentasun.stepflow.api.step.AbstractStepHandler;
import io.github.kentasun.stepflow.api.step.dto.StepData;
import io.github.kentasun.stepflow.api.utils.StepFlowUtils;
import io.github.kentasun.stepflow.step.constants.StepContentType;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Java 步骤处理器
 * <P>执行 Java 方法
 */
public class JavaStepHandler extends AbstractStepHandler {

    private final Map<String, AbstractJavaStep> methodMap;

    public JavaStepHandler(Map<String, AbstractJavaStep> javaStepMap) {
        this.methodMap = new ConcurrentHashMap<>();
        if (StepFlowUtils.isNotEmpty(javaStepMap)) {
            this.methodMap.putAll(javaStepMap);
        }
    }

    @Override
    public String getStepContentType() {
        return StepContentType.JAVA;
    }

    @Override
    public List<String> getParamNameList(StepData stepData) {
        String beanName = stepData.getContent();
        AbstractJavaStep javaStep = this.methodMap.get(beanName);
        return javaStep.getParamNameList();
    }

    @Override
    public Object doExecute(StepData stepData, OneOffParams oneOffParams) {
        String beanName = stepData.getContent();
        AbstractJavaStep javaStep = this.methodMap.get(beanName);
        if (javaStep == null) {
            throw new StepFlowException(String.format("【%s】不存在", beanName));
        }
        // 执行 Java 方法
        return javaStep.invoke(stepData, oneOffParams);
    }

    @Override
    public boolean isStepDataIllegal(StepData stepData) {
        return  StepFlowUtils.isBlank(stepData.getContent());
    }
}
