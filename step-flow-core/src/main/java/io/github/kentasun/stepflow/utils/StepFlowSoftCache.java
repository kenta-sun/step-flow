package io.github.kentasun.stepflow.utils;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * 基于 {@link SoftReference} 的泛型缓存。
 *
 * <p>缓存值以软引用包装，JVM 在内存紧张时可自动回收，并配合 {@link ReferenceQueue}
 * 按需清理失效条目。支持两种使用方式：构造时传入固定的 {@code mappingFunction}，
 * 或在每次 {@link #getOrPut(Object, Function)} 调用时传入，适用于构造逻辑依赖运行时上下文的场景。
 *
 * @param <K> 缓存键类型
 * @param <V> 缓存值类型
 * @author kenta-sun
 */
public class StepFlowSoftCache<K, V> {

    /**
     * 核心缓存容器：键 → 软引用包装的值。
     *
     * <p>以 {@link SoftReference} 包装 V，GC 在内存压力下可回收 referent；
     * {@link #referenceQueue} 用于感知回收事件并清理失效条目。
     */
    private final ConcurrentHashMap<K, Reference<V>> cache = new ConcurrentHashMap<>();

    /**
     * 与 {@link #cache} 中软引用绑定的引用队列。
     *
     * <p>当 GC 回收某个 {@link SoftReference} 的 referent 后，JVM 会自动将该
     * {@link SoftReference} 对象本身入队，由 {@link #evictStaleEntries()} 感知并清理。
     */
    private final ReferenceQueue<V> referenceQueue = new ReferenceQueue<>();

    /**
     * 构造时传入的固定映射函数，为 {@code null} 时须通过 {@link #getOrPut(Object, Function)} 传入。
     */
    private final Function<? super K, ? extends V> mappingFunction;

    /**
     * 使用固定映射函数构造缓存，每次 {@link #getOrPut(Object)} 时均使用该函数构造值。
     *
     * @param mappingFunction 缓存未命中时根据键构造值的映射函数，不可为 {@code null}
     */
    public StepFlowSoftCache(Function<? super K, ? extends V> mappingFunction) {
        this.mappingFunction = mappingFunction;
    }

    /**
     * 构造一个不绑定固定映射函数的缓存，每次须通过 {@link #getOrPut(Object, Function)} 传入构造逻辑。
     */
    public StepFlowSoftCache() {
        this.mappingFunction = null;
    }

    /**
     * 获取与键 {@code key} 对应的缓存值，使用构造时传入的固定 {@code mappingFunction}。
     *
     * @param key 缓存键，不可为 {@code null}
     * @return 与 {@code key} 对应的缓存值
     * @throws IllegalStateException 若未在构造时提供 {@code mappingFunction}
     */
    public V getOrPut(K key) {
        if (this.mappingFunction == null) {
            throw new IllegalStateException("未提供 mappingFunction，请使用 getOrPut(key, mappingFunction)");
        }
        return this.getOrPut(key, this.mappingFunction);
    }

    /**
     * 获取与键 {@code key} 对应的缓存值，缓存未命中时调用 {@code mappingFunction} 构造值。
     *
     * <p>若缓存命中且 referent 未被 GC 回收，直接返回；当 referent 已被 GC 回收时，
     * 移除失效条目后循环重试。
     *
     * @param key             缓存键，不可为 {@code null}
     * @param mappingFunction 缓存未命中时根据键构造值的映射函数，不可为 {@code null}
     * @return 与 {@code key} 对应的缓存值
     */
    public V getOrPut(K key, Function<? super K, ? extends V> mappingFunction) {
        // 虽然可以使用递归代替 while-true，但是每次重试都吃一帧栈空间，理论上有栈溢出风险，不建议使用递归。
        while (true) {
            // computeIfAbsent 保证同一 key 的映射函数在并发场景下只执行一次
            Reference<V> ref = this.cache.computeIfAbsent(key, k -> {
                // 在构造新值前先清理已失效的条目，避免缓存无限膨胀
                this.evictStaleEntries();
                return new SoftReference<>(mappingFunction.apply(k), this.referenceQueue);
            });

            V value = ref.get();
            if (value != null) {
                return value;
            }

            // referent 已被 GC 回收：key 仍在 map 中，computeIfAbsent 不会重算，
            // 需手动移除失效条目后循环重试
            this.cache.remove(key, ref);
        }
    }

    /**
     * 使指定键的缓存失效。
     *
     * @param key 需要失效的缓存键
     */
    public void invalidate(K key) {
        this.cache.remove(key);
    }

    /**
     * 清空所有缓存条目。
     */
    public void invalidateAll() {
        this.cache.clear();
    }

    /**
     * 清理缓存中已被 GC 回收的软引用条目。
     *
     * <p>以 {@code referenceQueue.poll() != null} 作为 O(1) 的 GC 事件探针：仅当队列中存在
     * 已入队的 {@link SoftReference} 时，才执行 O(n) 全量缓存扫描，避免无 GC 事件时的额外开销。
     * 确认后排空队列，防止 {@link SoftReference} 壳子对象持续积压。
     */
    private void evictStaleEntries() {
        if (this.referenceQueue.poll() != null) {
            // 该队列仅用来感知 GC，需排空队列释放所有已无用的 SoftReference 壳子对象
            //noinspection StatementWithEmptyBody
            while (this.referenceQueue.poll() != null) {
            }
            // 扫描缓存，移除 referent 已被 GC 回收的失效条目
            for (Map.Entry<K, Reference<V>> entry : this.cache.entrySet()) {
                Reference<V> ref = entry.getValue();
                if (ref != null && ref.get() == null) {
                    this.cache.remove(entry.getKey(), ref);
                }
            }
        }
    }
}
