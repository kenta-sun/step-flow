package io.github.kentasun.stepflow.step.intf;

/**
 * 常量类型转换器接口。
 * <p>每个实现对应一种 {@link io.github.kentasun.stepflow.step.constants.StepReturnType}，
 * 负责将数据库中存储的字符串常量转换为目标 Java 类型。
 * <p>框架内置了 BigDecimal、String、Boolean、Date、LocalDateTime、Instant 六种实现。
 * 使用者可自行实现该接口，并通过
 * {@link io.github.kentasun.stepflow.StepFlowExecutor.Builder#returnTypeConverterList}
 * 注册，以支持自定义类型。
 */
public interface ReturnTypeConverter {

    /**
     * 该 Converter 对应的 returnType 字符串，与
     * {@link io.github.kentasun.stepflow.step.constants.StepReturnType} 中的常量值对应。
     *
     * @return returnType 标识字符串
     */
    String getReturnType();

    /**
     * 将字符串形式的常量值转换为目标 Java 对象。
     *
     * @param constant 配置在数据库中的字符串常量值
     * @return 转换后的目标类型对象
     */
    Object convert(String constant);
}
