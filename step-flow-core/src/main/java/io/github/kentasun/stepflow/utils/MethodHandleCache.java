package io.github.kentasun.stepflow.utils;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 基于 {@link MethodHandle} 的方法调用缓存工具。
 *
 * <p>职责单一：给定一个具体的 {@link Method} 对象，将其对应的 {@link MethodHandle} 以
 * 软引用缓存起来，避免重复调用 {@link MethodHandles.Lookup#unreflect(Method)}；
 * 同时提供一步完成"取句柄 + 执行调用"的便捷方法。</p>
 *
 * <p>缓存以 {@link SoftReference} 包装，配合 {@link ReferenceQueue} 感知 GC 回收事件，
 * 在内存紧张时自动释放，防止 OOM。</p>
 *
 * @author kenta-sun
 */
public class MethodHandleCache {

    // --------------------------------------------------------- 方法句柄缓存

    /**
     * 方法句柄缓存：{@link Method} → {@link MethodHandle}。
     *
     * <p>以软引用包装 {@link MethodHandle}，GC 在内存压力下可回收其 referent；
     * {@link #cacheHandlesRq} 用于感知回收事件并清理失效条目。</p>
     */
    private static final ConcurrentHashMap<Method, Reference<MethodHandle>> cachedHandles = new ConcurrentHashMap<>();

    /**
     * {@link #cachedHandles} 中 {@link SoftReference} 所绑定的引用队列。
     *
     * <p>当 GC 回收某个 {@link SoftReference} 的 referent 后，JVM 会自动将该
     * {@link SoftReference} 对象本身入队。{@link #clearCache} 通过轮询此队列
     * 感知 GC 事件，从而按需触发对 {@link #cachedHandles} 的失效条目清理，同时
     * 排空队列以防止 {@link SoftReference} 壳子对象在队列中持续积压。</p>
     */
    private static final ReferenceQueue<MethodHandle> cacheHandlesRq = new ReferenceQueue<>();

    /* ---------- 公开 API ---------- */

    /**
     * 通过 {@link MethodHandle} 调用目标方法并返回结果。
     *
     * <p>内部调用 {@link #unReflect(Method)} 获取（或创建）句柄，再以
     * {@link MethodHandle#invokeWithArguments} 执行调用。{@code receiver} 将被置于
     * 参数列表首位作为 receiver，{@code args} 依次排列在后。</p>
     *
     * @param receiver 方法所属对象（实例方法的 receiver），不可为 {@code null}
     * @param method   目标方法，不可为 {@code null}
     * @param args     透传给目标方法的实参列表（不含 receiver）
     * @return 方法调用的返回值；若方法返回 {@code void} 则为 {@code null}
     */
    public static Object invoke(Object receiver, Method method, Object... args) {
        MethodHandle handle = unReflect(method);
        // MethodHandle 首参为 receiver，后续依次为方法实参
        Object[] invokeArgs = new Object[args.length + 1];
        invokeArgs[0] = receiver;
        System.arraycopy(args, 0, invokeArgs, 1, args.length);
        try {
            return handle.invokeWithArguments(invokeArgs);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 获取指定 {@link Method} 对应的 {@link MethodHandle}，优先从缓存中返回。
     *
     * @param method 目标方法，不可为 {@code null}
     * @return 与 {@code method} 对应的 {@link MethodHandle}
     */
    public static MethodHandle unReflect(Method method) {
        Reference<MethodHandle> existingRef = cachedHandles.get(method);

        MethodHandle handle = null;
        if (existingRef == null) {
            // 缓存未命中：顺手清理失效条目，再创建句柄并写入缓存
            clearCache();
            // 支持非 public 可见性场景
            method.setAccessible(true);
            // 缓存未命中或 referent 已被 GC 回收时，重新创建并写入缓存。
            try {
                handle = MethodHandles.lookup().unreflect(method);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
            existingRef = cachedHandles.putIfAbsent(method, new SoftReference<>(handle, cacheHandlesRq));
            // putIfAbsent 返回 null 说明本次竞争成功写入，直接返回
            if (existingRef == null) {
                return handle;
            }
            // 另一线程已抢先写入，继续走下方的 get() 逻辑
        }

        handle = existingRef.get();
        if (handle != null) {
            return handle;
        }

        // referent 在两次操作之间已被 GC 回收，移除失效条目后重试
        cachedHandles.remove(method, existingRef);
        return unReflect(method);
    }

    /* ---------- 内部逻辑 ---------- */

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
     */
    private static void clearCache() {
        if (cacheHandlesRq.poll() != null) {
            // 该队列仅用来感知 GC，需排空队列释放所有已无用的 SoftReference 壳子对象
            //noinspection StatementWithEmptyBody
            while (cacheHandlesRq.poll() != null) {
            }
            // 扫描缓存，移除 referent 已被 GC 回收的失效条目
            for (Map.Entry<Method, Reference<MethodHandle>> e : cachedHandles.entrySet()) {
                Reference<MethodHandle> val = e.getValue();
                if (val != null && val.get() == null) {
                    cachedHandles.remove(e.getKey(), val);
                }
            }
        }
    }
}
