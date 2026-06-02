package io.github.kentasun.stepflow.api.utils;

import io.github.kentasun.stepflow.api.exception.StepFlowException;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Objects;
import java.util.Optional;

/**
 * JavaBean Getter 对应的 {@link MethodHandle} 软引用缓存工具类。
 *
 * <p>按 {@code (Class, propertyName)} 在 {@link #HANDLE_CACHE} 中缓存查找结果；
 * 命中时直接返回已解析的句柄，未命中时扫描目标类的 public 实例无参方法并构造句柄。</p>
 * <p>未找到匹配方法时缓存 {@link Optional#empty()}，避免对同一类与属性名重复反射扫描。</p>
 *
 * <p>命名匹配规则遵循 JavaBeans 惯例：{@code getXxx}、{@code isXxx}；
 * 当 {@code propertyName} 以 {@code is} 开头时，亦支持与属性名完全相同的方法名（如 {@code isActive()}）。</p>
 *
 * @author kenta-sun
 */
public class GetterMethodsCacheUtils {

    /**
     * 方法句柄查找结果缓存：{@code (Class, propertyName)} → {@link Optional}{@code <MethodHandle>}。
     *
     * <p>值经 {@link StepFlowSoftCache} 以软引用持有，内存紧张时由 GC 回收后可按需重建。</p>
     */
    private static final StepFlowSoftCache<MethodLookupKey, Optional<MethodHandle>> HANDLE_CACHE =
            new StepFlowSoftCache<>(GetterMethodsCacheUtils::computeHandle);

    /**
     * 按类与属性名获取对应的 Getter {@link MethodHandle}（若存在）。
     *
     * @param clazz        目标类，不可为 {@code null}
     * @param propertyName 属性名（逻辑字段名，非方法名），不可为空白
     * @return 匹配到的句柄；未找到时返回 {@link Optional#empty()}
     * @throws StepFlowException 当 {@code propertyName} 为空白时
     */
    public static Optional<MethodHandle> getMethodHandleOptional(Class<?> clazz, String propertyName) {
        return HANDLE_CACHE.getOrPut(new MethodLookupKey(clazz, propertyName));
    }

    /**
     * 缓存未命中时，根据查找键扫描并构造 {@link MethodHandle}。
     *
     * <p>仅考虑 public、非 static、无参的实例方法。</p>
     * <p>按 {@link #genGetterName} 生成 {@code get*} / {@code is*} 方法名进行匹配，并兼容 {@code is*} 属性与同名方法。</p>
     *
     * @param key 查找键，包含目标类与属性名
     * @return 匹配到的句柄；未找到任何方法时返回 {@link Optional#empty()}
     * @throws StepFlowException 当 {@code key} 中的 {@code propertyName} 为空白时
     */
    private static Optional<MethodHandle> computeHandle(MethodLookupKey key) {
        Class<?> clazz = key.getClazz();
        String propertyName = key.getPropertyName();
        if (StepFlowUtils.isBlank(propertyName)) {
            throw new StepFlowException(String.format(
                    "propertyName can not be blank in class[%s]",
                    clazz.getName()
            ));
        }

        Method[] methodList = clazz.getMethods();
        if (StepFlowUtils.isEmpty(methodList)) {
            // 目标类无任何 public 方法，无法匹配 Getter
            return Optional.empty();
        }
        for (Method method : methodList) {
            int modifiers = method.getModifiers();
            // 仅匹配 public 实例无参方法（排除 static）
            if (!Modifier.isStatic(modifiers)
                    && Modifier.isPublic(modifiers)
                    && method.getParameterTypes().length == 0) {
                if (genGetterName("get", propertyName).equals(method.getName())
                        || genGetterName("is", propertyName).equals(method.getName())) {
                    MethodHandle methodHandle = buildMethodHandle(method);
                    return Optional.of(methodHandle);
                } else {
                    // boolean 属性可能直接以 isXxx 作为方法名，与 propertyName 完全一致
                    if (propertyName.startsWith("is") && propertyName.equals(method.getName())) {
                        MethodHandle methodHandle = buildMethodHandle(method);
                        return Optional.of(methodHandle);
                    }
                }
            }
        }

        // 未找到匹配方法：缓存 empty，避免后续对同一键重复全量扫描
        return Optional.empty();
    }

    /**
     * 将反射 {@link Method} 转为可反复 {@link MethodHandle#invoke} 的句柄。
     *
     * @param method 已解析的 Getter 方法
     * @return 与 {@code method} 对应的 {@link MethodHandle}
     * @throws RuntimeException 当 {@link MethodHandles.Lookup#unreflect(Method)} 因访问权限失败时
     */
    private static MethodHandle buildMethodHandle(Method method) {
        method.setAccessible(true);
        try {
            return MethodHandles.lookup().unreflect(method);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 按 JavaBeans 规则将属性名与前缀拼接为 Getter 方法名。
     *
     * <p>当属性名长度大于 1 且第 2 个字符为大写时（如 {@code uRL}），仅做前缀拼接、不再改动大小写；
     * 否则将首字母大写后拼接（如 {@code name} → {@code getName}）。<p>
     *
     * @param prefix 方法前缀，通常为 {@code get} 或 {@code is}
     * @param name   属性名，可为 {@code null}
     * @return 拼接后的方法名；{@code name} 为 {@code null} 时仅返回 {@code prefix}
     */
    private static String genGetterName(String prefix, String name) {
        if (name == null) {
            return prefix;
        }
        if (name.length() > 1 && Character.isUpperCase(name.charAt(1))) {
            return prefix + name;
        }
        return prefix + name.substring(0, 1).toUpperCase() + name.substring(1);
    }

    /**
     * 方法句柄查找的复合缓存键：{@code (Class, propertyName)}。
     */
    private static final class MethodLookupKey {

        /**
         * 待查找 Getter 的目标类。
         */
        final Class<?> clazz;

        /**
         * 逻辑属性名（用于生成 {@code get*} / {@code is*} 方法名），非反射方法名。
         */
        final String propertyName;

        /**
         * @param clazz        目标类
         * @param propertyName 逻辑属性名
         */
        MethodLookupKey(Class<?> clazz, String propertyName) {
            this.clazz = clazz;
            this.propertyName = propertyName;
        }

        public Class<?> getClazz() {
            return clazz;
        }

        public String getPropertyName() {
            return propertyName;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof MethodLookupKey)) {
                return false;
            }
            MethodLookupKey other = (MethodLookupKey) obj;
            return Objects.equals(clazz, other.clazz)
                    && StepFlowUtils.equals(propertyName, other.propertyName);
        }

        @Override
        public int hashCode() {
            return Objects.hash(clazz, propertyName);
        }

        @Override
        public String toString() {
            return "MethodLookupKey{" +
                    "clazz=" + clazz +
                    ", propertyName='" + propertyName + '\'' +
                    '}';
        }
    }
}
