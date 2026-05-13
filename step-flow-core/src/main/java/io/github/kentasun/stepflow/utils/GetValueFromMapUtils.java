package io.github.kentasun.stepflow.utils;

import io.github.kentasun.stepflow.exception.StepFlowException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 *
 * @author kenta-sun
 */
public class GetValueFromMapUtils {

    private static final Logger log = LoggerFactory.getLogger(GetValueFromMapUtils.class);

    private static final ConcurrentHashMap<MethodKey, Reference<List<Method>>> cacheMethods = new ConcurrentHashMap<>();
    private static final ReferenceQueue<List<Method>> cacheMethodsRq = new ReferenceQueue<>();

    /**
     * static and instance fields property caching
     */
    private static final ConcurrentHashMap<Class<?>, Reference<Map<String, PropertyFoundResult>>> cachedProperties = new ConcurrentHashMap<>();
    private static final ReferenceQueue<Map<String, PropertyFoundResult>> cachePropertyRq = new ReferenceQueue<>();

    private static final Pattern SPLIT_PAT = Pattern.compile("\\.");

    public static Object getValueFromContextMap(Map<String, Object> env, String name) {
        if (containsDot(name) && !env.containsKey(name)) {
            return getProperty(name, env);
        }
        return env.get(name);
    }

    private static Object getProperty(final String name, final Map<String, Object> env) {
        try {
            String[] names = SPLIT_PAT.split(name);
            return GetValueFromMapUtils.fastGetProperty(name, names, env);
        } catch (Throwable t) {
            log.info("Could not get property [{}]", name);
            return null;
        }
    }

    /**
     * 名字里面是否有 {@code .}
     *
     * @param name 待校验的名称字符串
     * @return {@code true} -有点；{@code false} -没有点
     */
    private static boolean containsDot(String name) {
        return name.contains(".");
    }

    private static Object fastGetProperty(String name, String[] names, Map<String, Object> env) {
        Target target = GetValueFromMapUtils.Target.withEnv(env);
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
                    val = fastGetProperty(target.targetObject, rName);
                }
            }

            if (arrayIndex >= 0) {
                if (val.getClass().isArray()) {
                    val = get(val, arrayIndex);
                } else if (val instanceof List) {
                    val = ((List<?>) val).get(arrayIndex);
                } else if (val instanceof CharSequence) {
                    val = ((CharSequence) val).charAt(arrayIndex);
                } else {
                    throw new IllegalArgumentException("Can't access " + val + " with index `" + arrayIndex
                            + "`, it's not an array, list or CharSequence");
                }
            }
            if (keyIndex != null) {
                if (Map.class.isAssignableFrom(val.getClass())) {
                    val = ((Map<?, ?>) val).get(keyIndex);
                } else {
                    throw new IllegalArgumentException(
                            "Can't access " + val + " with key `" + keyIndex + "`, it's not a map");
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
        return throwNoSuchPropertyException("Variable `" + name + "` not found in env: " + env);
    }

    private static void capitalize(StringBuilder sb, String s) {
        if (s == null) {
            return;
        }
        if (s.length() > 1 && Character.isUpperCase(s.charAt(1))) {
            sb.append(s);
            return;
        }
        sb.append(s.substring(0, 1).toUpperCase());
        sb.append(s.substring(1));
    }

    private static String genGetterName(String prefix, String name) {
        StringBuilder sb = new StringBuilder(prefix);
        capitalize(sb, name);
        return sb.toString();
    }

    private static Object fastGetProperty(Object obj, String name) {
        final Class<?> clazz = obj.getClass();
        Map<String, PropertyFoundResult> results;

        results = getClassPropertyResults(clazz);

        try {
            PropertyFoundResult result = results.get(name);
            if (result == null) {
                result = retrieveGetterHandle(results, clazz, name);
            }

            if (result.handle != null) {
                Object ret = result.handle.invoke(obj);
                if (result.isBooleanType && !(ret instanceof Boolean)) {
                    putDummyHandle(name, results);
                    return throwNoSuchPropertyException(
                            "Property `" + name + "` not found in java bean: " + obj);
                }
                return ret;
            } else {
                return throwNoSuchPropertyException(
                        "Property `" + name + "` not found in java bean: " + obj);
            }
        } catch (Throwable t) {
            if (!results.containsKey(name)) {
                putDummyHandle(name, results);
            }
            if (t instanceof RuntimeException) {
                throw (RuntimeException) t;
            } else {
                throw new RuntimeException(t);
            }
        }
    }

    private static Object throwNoSuchPropertyException(String msg) {
        throw new StepFlowException(msg);
    }

    private static PropertyFoundResult retrieveGetterHandle(Map<String, PropertyFoundResult> results, Class<?> clazz, String name)
            throws IllegalAccessException {
        PropertyFoundResult result;
        List<Method> methods = getInstanceMethods(clazz, genGetterName("get", name));
        boolean isBooleanType = false;

        if (methods == null || methods.isEmpty()) {
            methods = getInstanceMethods(clazz, genGetterName("is", name));
            isBooleanType = true;
        }

        if ((methods == null || methods.isEmpty()) && name.startsWith("is")) {
            // Fix https://github.com/killme2008/aviatorscript/issues/517
            methods = getInstanceMethods(clazz, name);
        }

        if (methods != null && !methods.isEmpty()) {
            Method method = methods.get(0);
            for (Method m : methods) {
                if (m.getParameterTypes().length == 0) {
                    method = m;
                    break;
                }
            }
            method.setAccessible(true);
            MethodHandle handle = MethodHandles.lookup().unreflect(method);
            result = new PropertyFoundResult(handle, isBooleanType);
        } else {
            result = new PropertyFoundResult(null, isBooleanType);
        }
        results.put(name, result);
        return result;
    }

    private static void putDummyHandle(String name, Map<String, PropertyFoundResult> handles) {
        handles.put(name, new PropertyFoundResult(null, false));
    }

    private static Map<String, PropertyFoundResult> getClassPropertyResults(Class<?> clazz) {
        Reference<Map<String, PropertyFoundResult>> existsRef = cachedProperties.get(clazz);
        Map<String, PropertyFoundResult> results = Collections.emptyMap();
        if (existsRef == null) {
            clearCache(cachePropertyRq, cachedProperties);
            results = new ConcurrentHashMap<>();
            existsRef = cachedProperties.putIfAbsent(clazz, new SoftReference<>(results, cachePropertyRq));
        }
        if (existsRef == null) {
            return results;
        }

        results = existsRef.get();
        if (results != null) {
            return results;
        }
        cachedProperties.remove(clazz, existsRef);
        return getClassPropertyResults(clazz);
    }


    private static <K, V> void clearCache(ReferenceQueue<V> rq, ConcurrentHashMap<K, Reference<V>> cache) {
        // cleanup any dead entries
        if (rq.poll() != null) {
            while (rq.poll() != null) {
                ;
            }
            for (Map.Entry<K, Reference<V>> e : cache.entrySet()) {
                Reference<V> val = e.getValue();
                if (val != null && val.get() == null) {
                    cache.remove(e.getKey(), val);
                }
            }
        }
    }

    private static List<Method> getInstanceMethods(Class<?> clazz, String methodName) {
        MethodKey key = new MethodKey(clazz, methodName);
        Reference<List<Method>> existingRef = cacheMethods.get(key);
        List<Method> methods = Collections.emptyList();

        if (existingRef == null) {
            clearCache(cacheMethodsRq, cacheMethods);
            methods = getClassInstanceMethods(clazz, methodName);
            existingRef = cacheMethods.putIfAbsent(key, new SoftReference<>(methods, cacheMethodsRq));
        }
        if (existingRef == null) {
            return methods;
        }

        List<Method> existingMethods = existingRef.get();
        if (existingMethods != null) {
            return existingMethods;
        }

        // entry died in the interim, do over
        cacheMethods.remove(key, existingRef);
        // retry
        return getInstanceMethods(clazz, methodName);
    }

    private static List<Method> getClassInstanceMethods(Class<?> c, String methodName) {
        List<Method> ret = new ArrayList<>();
        for (Method method : c.getMethods()) {
            int modifiers = method.getModifiers();
            if (!Modifier.isStatic(modifiers) && Modifier.isPublic(modifiers)
                    && methodName.equals(method.getName())) {
                method.setAccessible(true);
                ret.add(method);
            }
        }
        return ret;
    }

    private static Object get(final Object a, final int index) {
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

    private static class PropertyFoundResult {
        MethodHandle handle;
        boolean isBooleanType;

        PropertyFoundResult(MethodHandle handle, boolean isBooleanType) {
            super();
            this.isBooleanType = isBooleanType;
            this.handle = handle;
        }

        @Override
        public String toString() {
            return "PropertyFoundResult{" +
                    "handle=" + handle +
                    ", isBooleanType=" + isBooleanType +
                    '}';
        }
    }

    /**
     *
     * Class's instance method cache key
     *
     */
    private static class MethodKey {
        Class<?> clazz;
        String name;

        MethodKey(Class<?> clazz, String name) {
            super();
            this.clazz = clazz;
            this.name = name;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((this.clazz == null) ? 0 : this.clazz.hashCode());
            result = prime * result + ((this.name == null) ? 0 : this.name.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            MethodKey other = (MethodKey) obj;
            if (this.clazz == null) {
                if (other.clazz != null) {
                    return false;
                }
            } else if (!this.clazz.equals(other.clazz)) {
                return false;
            }
            if (this.name == null) {
                return other.name == null;
            } else {
                return this.name.equals(other.name);
            }
        }

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
