package io.github.kentasun.stepflow.step.converter;

import io.github.kentasun.stepflow.step.constants.StepReturnType;
import io.github.kentasun.stepflow.step.intf.ReturnTypeConverter;

import java.time.Instant;
import java.time.ZonedDateTime;

/**
 * 将字符串常量转换为 {@link Instant} 类型。
 * <p>输入必须是 ISO-8601 带时区字符串，例如：
 * {@code 2026-04-10T10:57:30+08:00} 或
 * {@code 2026-04-10T10:57:30+08:00[Asia/Shanghai]}
 */
public class InstantReturnTypeConverter implements ReturnTypeConverter {

    @Override
    public String getReturnType() {
        return StepReturnType.Instant;
    }

    @Override
    public Object convert(String constant) {
        return ZonedDateTime.parse(constant).toInstant();
    }
}
