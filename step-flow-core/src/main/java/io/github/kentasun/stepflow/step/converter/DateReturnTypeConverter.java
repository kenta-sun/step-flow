package io.github.kentasun.stepflow.step.converter;

import io.github.kentasun.stepflow.step.constants.StepReturnType;
import io.github.kentasun.stepflow.step.intf.ReturnTypeConverter;

import java.time.ZonedDateTime;
import java.util.Date;

/**
 * 将字符串常量转换为 {@link Date} 类型
 */
public class DateReturnTypeConverter implements ReturnTypeConverter {

    @Override
    public String getReturnType() {
        return StepReturnType.Date;
    }

    @Override
    public Object convert(String constant) {
        return Date.from(ZonedDateTime.parse(constant).toInstant());
    }
}
