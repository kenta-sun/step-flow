package io.github.kentasun.stepflow.step.constants;

/**
 * step 类型为 {@link StepContentType#CONSTANT} 时，常量对应的返回类型。
 */
public class StepReturnType {

    /** 对应 {@link java.math.BigDecimal} */
    public static final String BigDecimal = "BigDecimal";

    /** 对应 {@link java.lang.String} */
    public static final String String = "String";

    /** 对应 {@link java.lang.Boolean} */
    public static final String Boolean = "Boolean";

    /**
     * 对应 {@link java.util.Date}。
     * <p>输入必须是 ISO-8601 带时区字符串，如 {@code 2026-04-10T10:57:30+08:00}
     */
    public static final String Date = "Date";

    /**
     * 对应 {@link java.time.LocalDateTime}。
     * <p>输入必须是 ISO-8601 带时区字符串，取其本地日期时间部分
     */
    public static final String LocalDateTime = "LocalDateTime";

    /**
     * 对应 {@link java.time.Instant}。
     * <p>输入必须是 ISO-8601 带时区字符串，如 {@code 2026-04-10T10:57:30+08:00}
     */
    public static final String Instant = "Instant";
}
