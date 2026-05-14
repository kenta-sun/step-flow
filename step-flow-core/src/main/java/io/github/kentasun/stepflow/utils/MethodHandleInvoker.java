package io.github.kentasun.stepflow.utils;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 基于 {@link MethodHandles} 的公开实例方法调用工具，内置两级软引用缓存：
 *
 * <ol>
 *   <li><b>{@code cacheMethods}</b>：按 {@code (Class, methodName)} 缓存反射扫描出的
 *       {@link Method} 列表，避免重复调用 {@link Class#getMethods()}。</li>
 *   <li><b>{@code cachedHandles}</b>：按 {@code Class} 缓存已解析的 {@link MethodHandle}，
 *       内层 Map 的键为 {@code "methodName#paramCount"} 复合字符串，避免重复
 *       {@link MethodHandles.Lookup#unreflect(Method)}。</li>
 * </ol>
 *
 * <p>两级缓存均以 {@link SoftReference} 包装，配合 {@link ReferenceQueue} 感知 GC 回收事件，
 * 在内存紧张时自动释放，防止 OOM。</p>
 *
 * <p>调用方只需提供目标对象、<b>已拼接好</b>的方法名（无需再做任何前缀追加）以及期望的参数个数，
 * 工具会自动找到第一个匹配的公开实例方法并通过 {@link MethodHandle} 执行调用。</p>
 *
 * @author kenta-sun
 */
public class MethodHandleInvoker {

    // --------------------------------------------------------- 方法列表缓存

    /**
     * 方法列表缓存：{@code (Class, methodName)} → {@code List<Method>}。
     *
     * <p>以软引用包装列表，GC 在内存压力下可回收其 referent；
     * {@link #cacheMethodsRq} 用于感知回收事件并清理失效条目。</p>
     */
    private static final ConcurrentHashMap<MethodKey, Reference<List<Method>>> cacheMethods =
            new ConcurrentHashMap<>();

    /**
     * {@link #cacheMethods} 中 {@link SoftReference} 所绑定的引用队列。
     *
     * <p>当 GC 回收某个 {@link SoftReference} 的 referent 后，JVM 会自动将该
     * {@link SoftReference} 对象本身入队。{@link #clearCache} 通过轮询此队列
     * 感知 GC 事件，从而按需触发对 {@link #cacheMethods} 的失效条目清理，同时
     * 排空队列以防止 {@link SoftReference} 壳子对象在队列中持续积压。</p>
     */
    private static final ReferenceQueue<List<Method>> cacheMethodsRq = new ReferenceQueue<>();

    // --------------------------------------------------------- 方法句柄缓存

    /**
     * 方法句柄缓存：{@code Class} → {@code Map<lookupKey, MethodHandleEntry>}。
     *
     * <p>内层 Map 的键为 {@code "methodName#paramCount"} 复合字符串（{@code paramCount = -1}
     * 表示不限参数个数）；值为 {@link MethodHandleEntry}，当其 {@link MethodHandleEntry#handle}
     * 为 {@code null} 时作为"未找到"哨兵，避免对同一查询重复进行反射扫描。</p>
     */
    private static final ConcurrentHashMap<Class<?>, Reference<Map<String, MethodHandleEntry>>> cachedHandles =
            new ConcurrentHashMap<>();

    /**
     * {@link #cachedHandles} 中 {@link SoftReference} 所绑定的引用队列。
     *
     * <p>作用与 {@link #cacheMethodsRq} 相同，用于感知 {@link #cachedHandles}
     * 中软引用的 GC 回收事件，以驱动对应缓存的失效条目清理。</p>
     */
    private static final ReferenceQueue<Map<String, MethodHandleEntry>> cacheHandlesRq = new ReferenceQueue<>();

    // --------------------------------------------------------- 公开 API

    /**
     * 在目标对象上调用指定方法名的第一个匹配的公开实例方法。
     *
     * <p>匹配规则：方法名相同，且参数个数等于 {@code paramCount}（{@code -1} 则不限）；
     * 若存在多个同名同参数个数的重载方法，取 {@link Class#getMethods()} 遍历顺序中的第一个。</p>
     *
     * @param target     目标对象，不可为 {@code null}
     * @param methodName 已拼接好的方法名（如 {@code "getName"}），本方法不做任何前缀追加
     * @param paramCount 期望的参数个数；传 {@code -1} 则取同名第一个方法，不限参数个数
     * @param args       透传给目标方法的实参列表（不含 receiver，工具内部自动将 {@code target}
     *                   置于首位）
     * @return 方法调用的返回值；若方法返回 {@code void} 则为 {@code null}
     * @throws NoSuchMethodException 若在 {@code target} 所属类上找不到符合要求的方法
     * @throws Throwable             底层 {@link MethodHandle} 调用时抛出的任何异常
     */
    public static Object invoke(Object target, String methodName, int paramCount, Object... args)
            throws Throwable {
        MethodHandle handle = resolveHandle(target.getClass(), methodName, paramCount);
        if (handle == null) {
            throw new NoSuchMethodException(
                    "No public instance method [" + methodName + "] with paramCount=" + paramCount
                    + " found on " + target.getClass().getName());
        }
        // MethodHandle 的首参为 receiver（即 target），后续依次为方法实参
        Object[] invokeArgs = new Object[args.length + 1];
        invokeArgs[0] = target;
        System.arraycopy(args, 0, invokeArgs, 1, args.length);
        return handle.invokeWithArguments(invokeArgs);
    }

    /**
     * 仅解析并返回 {@link MethodHandle}，不执行调用。
     *
     * <p>适用于需要提前拿到句柄、延迟/多次调用的场景。结果已写入缓存，重复调用不会触发
     * 额外的反射扫描。</p>
     *
     * @param clazz      目标类
     * @param methodName 已拼接好的方法名
     * @param paramCount 期望参数个数；{@code -1} 表示不限
     * @return 找到的 {@link MethodHandle}；若未找到返回 {@code null}
     */
    public static MethodHandle resolveHandle(Class<?> clazz, String methodName, int paramCount) {
        Map<String, MethodHandleEntry> handleMap = getHandleMap(clazz);
        // 内层 Map 以 "methodName#paramCount" 作为复合键，区分同名不同参数个数的查询
        String lookupKey = methodName + "#" + paramCount;
        MethodHandleEntry entry = handleMap.get(lookupKey);
        if (entry != null) {
            // 缓存命中：handle 为 null 表示哨兵（之前已查询过但未找到）
            return entry.handle;
        }
        // 缓存未命中，执行反射扫描、解析句柄并写入缓存
        return retrieveAndCacheHandle(handleMap, clazz, methodName, paramCount, lookupKey);
    }

    // --------------------------------------------------------- 内部逻辑

    /**
     * 从 {@link #cacheMethods} 中查找匹配方法，解析为 {@link MethodHandle} 后写入
     * {@code handleMap} 并返回。
     *
     * <p>若未找到符合 {@code paramCount} 要求的方法，则向 {@code handleMap} 写入
     * {@code handle = null} 的哨兵条目，避免后续重复查询。</p>
     *
     * @param handleMap  当前类的句柄缓存内层 Map
     * @param clazz      目标类
     * @param methodName 方法名
     * @param paramCount 期望参数个数；{@code -1} 表示不限
     * @param lookupKey  内层 Map 的复合键（{@code "methodName#paramCount"}）
     * @return 解析到的 {@link MethodHandle}；未找到时返回 {@code null}
     */
    private static MethodHandle retrieveAndCacheHandle(
            Map<String, MethodHandleEntry> handleMap,
            Class<?> clazz,
            String methodName,
            int paramCount,
            String lookupKey) {
        List<Method> methods = getInstanceMethods(clazz, methodName);

        // 从列表中取出第一个满足参数个数要求的方法
        Method matched = null;
        if (methods != null && !methods.isEmpty()) {
            for (Method m : methods) {
                if (paramCount < 0 || m.getParameterTypes().length == paramCount) {
                    matched = m;
                    break;
                }
            }
        }

        if (matched != null) {
            try {
                matched.setAccessible(true);
                MethodHandle handle = MethodHandles.lookup().unreflect(matched);
                handleMap.put(lookupKey, new MethodHandleEntry(handle));
                return handle;
            } catch (IllegalAccessException e) {
                // 模块系统等原因导致访问被拒绝，降级处理：写入哨兵，不再重试
                handleMap.put(lookupKey, new MethodHandleEntry(null));
                return null;
            }
        }

        // 未找到任何匹配方法，写入哨兵避免重复扫描
        handleMap.put(lookupKey, new MethodHandleEntry(null));
        return null;
    }

    /**
     * 获取指定类的句柄缓存内层 Map；若缓存中尚无该类的条目则创建并放入外层缓存。
     *
     * <p>与原 {@code GetValueFromMapUtils.getClassPropertyResults} 逻辑完全一致：
     * 利用软引用的 {@code get()} 检测 referent 是否已被 GC 回收，若已回收则递归重试。</p>
     *
     * @param clazz 目标类
     * @return 该类的 {@code lookupKey → MethodHandleEntry} Map（并发安全）
     */
    private static Map<String, MethodHandleEntry> getHandleMap(Class<?> clazz) {
        Reference<Map<String, MethodHandleEntry>> existingRef = cachedHandles.get(clazz);
        Map<String, MethodHandleEntry> map = Collections.emptyMap();

        if (existingRef == null) {
            // 首次访问该类：顺手清理失效的软引用条目，再放入新缓存
            clearCache(cacheHandlesRq, cachedHandles);
            map = new ConcurrentHashMap<>();
            existingRef = cachedHandles.putIfAbsent(clazz, new SoftReference<>(map, cacheHandlesRq));
        }
        // putIfAbsent 返回 null 说明本次竞争成功写入，直接返回刚创建的 map
        if (existingRef == null) {
            return map;
        }

        Map<String, MethodHandleEntry> existing = existingRef.get();
        if (existing != null) {
            return existing;
        }

        // referent 在两次操作之间已被 GC 回收，移除失效条目后重试
        cachedHandles.remove(clazz, existingRef);
        return getHandleMap(clazz);
    }

    /**
     * 从 {@link #cacheMethods} 中获取指定类上特定名称的实例方法列表；
     * 缓存未命中时通过反射扫描并写入缓存。
     *
     * <p>与原 {@code GetValueFromMapUtils.getInstanceMethods} 逻辑完全一致。</p>
     *
     * @param clazz      目标类
     * @param methodName 方法名
     * @return 匹配的公开实例方法列表，不会为 {@code null}
     */
    private static List<Method> getInstanceMethods(Class<?> clazz, String methodName) {
        MethodKey key = new MethodKey(clazz, methodName);
        Reference<List<Method>> existingRef = cacheMethods.get(key);
        List<Method> methods = Collections.emptyList();

        if (existingRef == null) {
            clearCache(cacheMethodsRq, cacheMethods);
            methods = scanInstanceMethods(clazz, methodName);
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
        return getInstanceMethods(clazz, methodName);
    }

    /**
     * 通过反射遍历类的所有公开方法，筛选出非静态且名称匹配的实例方法列表。
     *
     * <p>与原 {@code GetValueFromMapUtils.getClassInstanceMethods} 逻辑完全一致。</p>
     *
     * @param clazz      目标类
     * @param methodName 方法名
     * @return 筛选结果，可能为空列表
     */
    private static List<Method> scanInstanceMethods(Class<?> clazz, String methodName) {
        List<Method> ret = new ArrayList<>();
        for (Method method : clazz.getMethods()) {
            int modifiers = method.getModifiers();
            if (!Modifier.isStatic(modifiers)
                    && Modifier.isPublic(modifiers)
                    && methodName.equals(method.getName())) {
                method.setAccessible(true);
                ret.add(method);
            }
        }
        return ret;
    }

    /**
     * 清理缓存中已被 GC 回收的软引用条目。
     *
     * <p>以 {@code rq.poll() != null} 作为 O(1) 的 GC 事件探针：仅当队列中存在
     * 已入队的 {@link SoftReference} 时，才执行代价较高的 O(n) 全量缓存扫描，
     * 避免在无 GC 事件时产生不必要的遍历开销。</p>
     *
     * <p>探针确认后，通过 {@code while} 循环将队列中所有已入队的
     * {@link SoftReference} 对象排空，防止其在 {@link ReferenceQueue} 内持续
     * 积压，造成 {@link SoftReference} 壳子对象本身无法被回收。随后遍历
     * {@code cache}，移除 referent 已为 {@code null} 的失效条目。</p>
     *
     * @param rq    与缓存中 {@link SoftReference} 绑定的引用队列
     * @param cache 待清理的缓存映射表
     * @param <K>   缓存键类型
     * @param <V>   缓存值类型（即 {@link SoftReference} 所包装的对象类型）
     */
    private static <K, V> void clearCache(ReferenceQueue<V> rq, ConcurrentHashMap<K, Reference<V>> cache) {
        if (rq.poll() != null) {
            // 该队列仅用来感知 GC，需排空队列释放所有已无用的 SoftReference 壳子对象
            //noinspection StatementWithEmptyBody
            while (rq.poll() != null) {}
            // 扫描缓存，移除 referent 已被 GC 回收的失效条目
            for (Map.Entry<K, Reference<V>> e : cache.entrySet()) {
                Reference<V> val = e.getValue();
                if (val != null && val.get() == null) {
                    cache.remove(e.getKey(), val);
                }
            }
        }
    }

    // --------------------------------------------------------- 内部类

    /**
     * 方法句柄缓存条目，仅保存 {@link MethodHandle}。
     *
     * <p>当 {@link #handle} 为 {@code null} 时，表示该查询键在对应类上已经查询过但未找到
     * 符合参数要求的方法（哨兵条目），后续相同查询可直接跳过反射扫描。</p>
     */
    private static final class MethodHandleEntry {

        /** 解析到的方法句柄；{@code null} 表示未找到。 */
        final MethodHandle handle;

        MethodHandleEntry(MethodHandle handle) {
            this.handle = handle;
        }

        @Override
        public String toString() {
            return "MethodHandleEntry{handle=" + handle + '}';
        }
    }

    /**
     * 方法列表缓存的复合键：{@code (Class, methodName)}。
     *
     * <p>与原 {@code GetValueFromMapUtils.MethodKey} 逻辑完全相同，此处独立声明
     * 以避免跨类引用私有内部类。</p>
     */
    private static final class MethodKey {

        final Class<?> clazz;
        final String name;

        MethodKey(Class<?> clazz, String name) {
            this.clazz = clazz;
            this.name = name;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + (clazz == null ? 0 : clazz.hashCode());
            result = prime * result + (name == null ? 0 : name.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            MethodKey other = (MethodKey) obj;
            if (!Objects.equals(clazz, other.clazz)) {
                return false;
            }
            return Objects.equals(name, other.name);
        }
    }
}
