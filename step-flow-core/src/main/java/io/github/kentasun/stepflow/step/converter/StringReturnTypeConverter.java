package io.github.kentasun.stepflow.step.converter;

import io.github.kentasun.stepflow.step.constants.StepReturnType;
import io.github.kentasun.stepflow.step.intf.ReturnTypeConverter;

/**
 * 将字符串常量原样返回为 {@link String} 类型
 */
public class StringReturnTypeConverter implements ReturnTypeConverter {

    @Override
    public String getReturnType() {
        return StepReturnType.String;
    }

    @Override
    public Object convert(String constant) {
        return constant;
    }
}
