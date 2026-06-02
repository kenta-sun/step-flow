package io.github.kentasun.stepflow.api.utils;

import java.util.Collection;
import java.util.Map;

public class StepFlowUtils {

    /**
     * <p>Checks if a CharSequence is empty (""), null or whitespace only.</p>
     * <p>Whitespace is defined by {@link Character#isWhitespace(char)}.</p>
     * <pre>
     * StringUtils.isBlank(null)      = true
     * StringUtils.isBlank("")        = true
     * StringUtils.isBlank(" ")       = true
     * StringUtils.isBlank("bob")     = false
     * StringUtils.isBlank("  bob  ") = false
     * </pre>
     *
     * @param cs the CharSequence to check, may be null
     * @return {@code true} if the CharSequence is null, empty or whitespace only
     * @since 2.0
     * @since 3.0 Changed signature from isBlank(String) to isBlank(CharSequence)
     */
    public static boolean isBlank(final CharSequence cs) {
        final int strLen = length(cs);
        if (strLen == 0) {
            return true;
        }
        for (int i = 0; i < strLen; i++) {
            if (!Character.isWhitespace(cs.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * 字符串是否非空
     *
     * @param cs the CharSequence to check, may be null
     * @return {@code true} if the CharSequence is not null, not empty and not whitespace only
     */
    public static boolean isNotBlank(final CharSequence cs) {
        return !isBlank(cs);
    }

    /**
     * Gets a CharSequence length or {@code 0} if the CharSequence is
     * {@code null}.
     *
     * @param cs a CharSequence or {@code null}
     * @return CharSequence length or {@code 0} if the CharSequence is
     * {@code null}.
     */
    public static int length(final CharSequence cs) {
        return cs == null ? 0 : cs.length();
    }

    /**
     * 比较两个字符串是否相等
     * <p>特殊规则：</p>
     * <p>1. 如果两个字符串都为null，视为相等</p>
     * <p>2. 如果只有一个字符串为null，视为不相等</p>
     * <p>3. 都不为null时，使用String.equals进行比对</p>
     *
     * @param str1 字符串1
     * @param str2 字符串2
     * @return true:相等; false:不相等
     */
    public static boolean equals(String str1, String str2) {
        if (str1 == null && str2 == null) return true;
        if (str1 == null) return false;
        return str1.equals(str2);
    }

    public static boolean notEquals(String str1, String str2) {
        return !equals(str1, str2);
    }

    public static boolean isEmpty(Map<?, ?> map) {
        return map == null || map.isEmpty();
    }

    public static boolean isNotEmpty(Map<?, ?> map) {
        return !isEmpty(map);
    }

    public static boolean isEmpty(Collection<?> collection) {
        return collection == null || collection.isEmpty();
    }

    public static boolean isNotEmpty(Collection<?> collection) {
        return !isEmpty(collection);
    }

    public static <T> boolean isEmpty(T[] array) {
        return array == null || array.length == 0;
    }

    public static boolean isNotEmpty(Object[] array) {
        return !isEmpty(array);
    }

    /**
     * 默认值
     *
     * @param object       目标对象
     * @param defaultValue 默认值对象
     * @param <T>          目标对象的类型
     * @return 如果 {@code object} 不为 {@code null} 则返回 {@code object}；反之返回 {@code defaultValue}
     */
    public static <T> T defaultIfNull(final T object, final T defaultValue) {
        return object != null ? object : defaultValue;
    }
}
