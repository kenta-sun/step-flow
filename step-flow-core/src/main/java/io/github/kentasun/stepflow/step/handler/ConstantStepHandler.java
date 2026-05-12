package io.github.kentasun.stepflow.step.handler;

import io.github.kentasun.stepflow.dto.ExecutorsContext;
import io.github.kentasun.stepflow.dto.OneOffParams;
import io.github.kentasun.stepflow.dto.StepFlowContext;
import io.github.kentasun.stepflow.exception.StepFlowException;
import io.github.kentasun.stepflow.step.constants.StepContentType;
import io.github.kentasun.stepflow.step.dto.StepData;
import io.github.kentasun.stepflow.step.intf.ReturnTypeConverter;
import io.github.kentasun.stepflow.step.intf.StepHandler;
import io.github.kentasun.stepflow.utils.StepFlowUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 常量步骤处理器。
 * <p>根据 {@link StepData#getReturnType()} 从 converterMap 中查找对应的
 * {@link ReturnTypeConverter}，将字符串常量转换为目标 Java 类型。</p>
 */
public class ConstantStepHandler implements StepHandler {

    /** key = returnType 字符串，value = 对应的类型转换器 */
    private final Map<String, ReturnTypeConverter> converterMap;

    public ConstantStepHandler(List<ReturnTypeConverter> list) {
        this.converterMap = new HashMap<>();
        if (StepFlowUtils.isNotEmpty(list)) {
            for (ReturnTypeConverter converter : list) {
                converterMap.put(converter.getReturnType(), converter);
            }
        }
    }

    @Override
    public String getStepContentType() {
        return StepContentType.CONSTANT;
    }

    @Override
    public Object execute(StepData stepData, StepFlowContext stepFlowContext, OneOffParams oneOffParams, ExecutorsContext executorsContext) {
        String constant = stepData.getContent();
        String returnType = stepData.getReturnType();

        // 从策略 Map 中查找对应的转换器，交由其完成具体的类型转换
        ReturnTypeConverter converter = converterMap.get(returnType);
        if (converter == null) {
            throw new StepFlowException("未知的returnType类型：" + returnType);
        }
        return converter.convert(constant);
    }

    @Override
    public boolean isStepDataIllegal(StepData stepData) {
        return StepFlowUtils.isBlank(stepData.getContent()) || this.isConstantTypeIllegal(stepData.getReturnType());
    }

    /**
     * 校验 returnType 是否在已注册的 converterMap 中存在。
     *
     * @param returnType 待校验的 returnType 字符串
     * @return true-非法（不存在）；false-合法
     */
    private boolean isConstantTypeIllegal(String returnType) {
        if (StepFlowUtils.isBlank(returnType)) {
            return true;
        }
        return !converterMap.containsKey(returnType);
    }
}
