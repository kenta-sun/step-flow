package io.github.kentasun.stepflow.step.converter;

import io.github.kentasun.stepflow.step.constants.StepReturnType;
import io.github.kentasun.stepflow.step.intf.ReturnTypeConverter;

import java.math.BigDecimal;

/**
 * 将字符串常量转换为 {@link BigDecimal} 类型
 */
public class BigDecimalReturnTypeConverter implements ReturnTypeConverter {

    @Override
    public String getReturnType() {
        return StepReturnType.BigDecimal;
    }

    @Override
    public Object convert(String constant) {
        return new BigDecimal(constant);
    }
}
