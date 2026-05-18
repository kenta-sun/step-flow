package io.github.kentasun.stepflow.utils;

import io.github.kentasun.stepflow.exception.StepFlowException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Array;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 *
 * @author kenta-sun
 */
public class GetValueFromMapUtils {

    private static final Logger log = LoggerFactory.getLogger(GetValueFromMapUtils.class);

    private static final Pattern SPLIT_PAT = Pattern.compile("\\.");

    public static Object getValueFromContextMap(String name, Map<String, Object> env) {
        if (containsSymbol(name) && !env.containsKey(name)) {
            try {
                return fastGetProperty(name, env);
            } catch (Throwable t) {
                log.info("Could not get property [{}]", name);
                return null;
            }
        }
        return env.get(name);
    }

    private static Object fastGetProperty(String name, Map<String, Object> env) {
        String[] names = SPLIT_PAT.split(name);
        Target target = Target.withEnv(env);
        int max = names.length;
        for (int i = 0; i < max; i++) {
            String rName = names[i];
            int arrayIndex = -1;
            String keyIndex = null;

            // compatible with PropertyUtilsBean indexed and mapped formats.
            // https://commons.apache.org/proper/commons-beanutils/apidocs/org/apache/commons/beanutils/PropertyUtilsBean.html
            switch (rName.charAt(rName.length() - 1)) {
                case ']':
                    int idx1 = rName.indexOf("[");
                    if (idx1 < 0) {
                        throw new IllegalArgumentException("Should not happen, doesn't contains '['");
                    }
                    String rawName1 = rName;
                    rName = rName.substring(0, idx1);
                    arrayIndex = Integer.parseInt(rawName1.substring(idx1 + 1, rawName1.length() - 1));
                    break;
                case ')':
                    int idx2 = rName.indexOf("(");
                    if (idx2 < 0) {
                        throw new IllegalArgumentException("Should not happen, doesn't contains '('");
                    }
                    String rawName2 = rName;
                    rName = rName.substring(0, idx2);
                    keyIndex = rawName2.substring(idx2 + 1, rawName2.length() - 1);
                    break;
            }

            Object val;
            // in the format of a.b.[0].c
            if (rName.isEmpty()) {
                if (!(arrayIndex >= 0 || keyIndex != null)) {
                    throw new IllegalArgumentException("Invalid format");
                }
                if (target.innerEnv != null) {
                    val = target.innerEnv;
                } else {
                    val = target.targetObject;
                }
            } else {
                if (target.innerEnv != null) {
                    val = target.innerEnv.get(rName);
                } else {
                    val = getPropertyFromObject(target.targetObject, rName);
                }
            }

            if (arrayIndex >= 0) {
                if (val.getClass().isArray()) {
                    val = getArrayElement(val, arrayIndex);
                } else if (val instanceof List) {
                    val = ((List<?>) val).get(arrayIndex);
                } else if (val instanceof CharSequence) {
                    val = ((CharSequence) val).charAt(arrayIndex);
                } else {
                    throw new IllegalArgumentException(String.format(
                            "Can't access %s with index [%s], it's not an array, list or CharSequence",
                            val,
                            arrayIndex
                    ));
                }
            }
            if (keyIndex != null) {
                if (Map.class.isAssignableFrom(val.getClass())) {
                    val = ((Map<?, ?>) val).get(keyIndex);
                } else {
                    throw new IllegalArgumentException(String.format(
                            "Can't access %s with key [%s], it's not a map",
                            val,
                            keyIndex
                    ));
                }
            }

            if (i == max - 1) {
                return val;
            }
            if (val instanceof Map) {
                //noinspection unchecked
                target.innerEnv = (Map<String, Object>) val;
                target.targetObject = null;
            } else if (val == null) {
                throw new NullPointerException(rName);
            } else {
                target.targetObject = val;
                target.innerEnv = null;
            }
        }

        throw new StepFlowException(String.format("Property[%s] not found in env", name));
    }

    private static Object getPropertyFromObject(Object obj, String name) {
        final Class<?> clazz = obj.getClass();
        Optional<MethodHandle> methodHandleOptional = GetterMethodsCacheUtils.getMethodHandleOptional(clazz, name);
        if (methodHandleOptional.isPresent()) {
            MethodHandle methodHandle = methodHandleOptional.get();
            try {
                return methodHandle.invoke(obj);
            } catch (Throwable e) {
                if (e instanceof RuntimeException) {
                    throw (RuntimeException) e;
                } else {
                    throw new RuntimeException(e);
                }
            }
        } else {
            throw new StepFlowException(String.format(
                    "The getter method for the Property[%s] can not found in class[%s].",
                    name,
                    clazz.getName()
            ));
        }
    }

    /**
     * 名字里面是否有 {@code .}、{@code []}、{@code ()}
     *
     * @param name 待校验的名称字符串
     * @return {@code true} -有点；{@code false} -没有点
     */
    private static boolean containsSymbol(String name) {
        return name.contains(".") || (name.contains("[") && name.contains("]")) || (name.contains("(") && name.contains(")"));
    }

    private static Object getArrayElement(final Object a, final int index) {
        if (a instanceof byte[]) {
            return ((byte[]) a)[index];
        } else if (a instanceof short[]) {
            return ((short[]) a)[index];
        } else if (a instanceof int[]) {
            return ((int[]) a)[index];
        } else if (a instanceof long[]) {
            return ((long[]) a)[index];
        } else if (a instanceof float[]) {
            return ((float[]) a)[index];
        } else if (a instanceof double[]) {
            return ((double[]) a)[index];
        } else if (a instanceof String[]) {
            return ((String[]) a)[index];
        } else if (a instanceof Object[]) {
            return ((Object[]) a)[index];
        }

        return Array.get(a, index);
    }

    private static class Target {
        Map<String, Object> innerEnv;
        Object targetObject;

        Target() {
            super();
        }

        static Target withEnv(Map<String, Object> env) {
            Target t = new Target();
            t.innerEnv = env;
            return t;
        }
    }
}
