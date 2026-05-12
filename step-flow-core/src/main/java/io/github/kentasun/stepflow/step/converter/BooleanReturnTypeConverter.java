package io.github.kentasun.stepflow.step.converter;

import io.github.kentasun.stepflow.step.constants.StepReturnType;
import io.github.kentasun.stepflow.step.intf.ReturnTypeConverter;

/**
 * 将字符串常量转换为 {@link Boolean} 类型
 */
public class BooleanReturnTypeConverter implements ReturnTypeConverter {

    @Override
    public String getReturnType() {
        return StepReturnType.Boolean;
    }

    @Override
    public Object convert(String constant) {
        return Boolean.valueOf(constant);
    }
}
