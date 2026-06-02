package io.github.kentasun.stepflow.cache;

import io.github.kentasun.stepflow.api.utils.StepFlowSoftCache;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.ref.Reference;
import java.lang.reflect.Field;  // 仅用于访问 StepFlowSoftCache 自身的私有 cache 字段
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link StepFlowSoftCache} 单元测试。
 *
 * <p><b>GC 模拟原理：</b>无法在测试中直接触发 JVM GC 回收软引用，
 * 因此通过反射完成以下两步来等效模拟：
 * <ol>
 *   <li>将 {@code Reference#referent} 字段置为 {@code null}，令 {@code ref.get()} 返回 {@code null}</li>
 *   <li>调用 {@link Reference#enqueue()}，将该软引用推入 {@link java.lang.ref.ReferenceQueue}，
 *       触发 {@code evictStaleEntries()} 内的 GC 事件探针</li>
 * </ol>
 *
 * @author kenta-sun
 */
@DisplayName("StepFlowSoftCache 单元测试")
public class StepFlowSoftCacheTest {

    // =========================================================================
    // 反射工具方法（全组共享）
    // =========================================================================

    /**
     * 通过反射获取 {@link StepFlowSoftCache} 内部私有的 {@code ConcurrentHashMap} 字段。
     * 用于在测试中直接验证缓存内部状态（条目是否存在、条目数量等）。
     */
    @SuppressWarnings("unchecked")
    private <K, V> ConcurrentHashMap<K, Reference<V>> getInternalCache(
            StepFlowSoftCache<K, V> softCache) throws Exception {
        Field field = StepFlowSoftCache.class.getDeclaredField("cache");
        field.setAccessible(true);
        return (ConcurrentHashMap<K, Reference<V>>) field.get(softCache);
    }

    /**
     * 模拟 JVM GC 回收指定 key 所对应软引用的全过程：
     * <ol>
     *   <li>调用 {@link Reference#clear()}（public 方法）将 referent 置为 {@code null}，
     *       令 {@code ref.get()} 返回 {@code null}，等价于 GC 清除 referent</li>
     *   <li>调用 {@link Reference#enqueue()} 将其推入 {@code referenceQueue}，
     *       触发 {@code evictStaleEntries()} 的 O(1) 探针</li>
     * </ol>
     *
     * <p>注意：{@link Reference#clear()} 与 {@link Reference#enqueue()} 均为 public API，
     * 无需反射访问 JDK 私有字段，在 JDK 8 ～ 21 均兼容。
     *
     * @param softCache 目标缓存实例
     * @param key       需要模拟 GC 回收的缓存键
     */
    private <K, V> void simulateGc(StepFlowSoftCache<K, V> softCache, K key) throws Exception {
        ConcurrentHashMap<K, Reference<V>> cache = this.getInternalCache(softCache);
        Reference<V> ref = cache.get(key);
        assertNotNull(ref, "simulateGc 前该 key 必须已存在于缓存中，key=" + key);

        // Reference.clear() 是 public 方法，将 referent 清空（等价于 GC 清除操作）
        ref.clear();

        // 手动入队，触发 evictStaleEntries() 的 referenceQueue 探针（等价于 GC 入队通知）
        ref.enqueue();
    }

    // =========================================================================
    // 第一组：构造器行为
    // =========================================================================

    @Nested
    @DisplayName("1. 构造器行为")
    class ConstructorTests {

        @Test
        @DisplayName("带 mappingFunction 的构造器：getOrPut(key) 应正常返回值")
        void withMappingFunction_getOrPutNoArg_shouldReturnValue() {
            StepFlowSoftCache<String, String> cache = new StepFlowSoftCache<>(k -> "value-" + k);
            assertEquals("value-hello", cache.getOrPut("hello"));
        }

        @Test
        @DisplayName("无参构造器：getOrPut(key) 应抛出 IllegalStateException")
        void noArgConstructor_getOrPutNoArg_shouldThrowIllegalStateException() {
            StepFlowSoftCache<String, String> cache = new StepFlowSoftCache<>();
            assertThrows(IllegalStateException.class, () -> cache.getOrPut("hello"));
        }
    }

    // =========================================================================
    // 第二组：基础缓存命中 / 未命中
    // =========================================================================

    @Nested
    @DisplayName("2. 基础缓存命中 / 未命中")
    class CacheHitMissTests {

        @Test
        @DisplayName("首次 getOrPut：mappingFunction 被调用一次，返回正确值")
        void firstGetOrPut_shouldCallMappingFunctionOnceAndReturnValue() {
            AtomicInteger callCount = new AtomicInteger(0);
            StepFlowSoftCache<String, String> cache = new StepFlowSoftCache<>();

            String result = cache.getOrPut("key1", k -> {
                callCount.incrementAndGet();
                return "value-" + k;
            });

            assertEquals("value-key1", result, "应返回 mappingFunction 计算的值");
            assertEquals(1, callCount.get(), "首次访问应调用 mappingFunction 一次");
        }

        @Test
        @DisplayName("相同 key 连续调用两次：mappingFunction 只被调用一次（缓存命中）")
        void sameKey_calledTwice_mappingFunctionCalledOnce() {
            AtomicInteger callCount = new AtomicInteger(0);
            StepFlowSoftCache<String, String> cache = new StepFlowSoftCache<>();
            Function<String, String> fn = k -> {
                callCount.incrementAndGet();
                return "value-" + k;
            };

            String first = cache.getOrPut("key1", fn);
            String second = cache.getOrPut("key1", fn);

            assertEquals(first, second, "两次调用应返回相同的值");
            assertEquals(1, callCount.get(), "缓存命中时 mappingFunction 不应被重复调用");
        }

        @Test
        @DisplayName("不同 key 各自独立：每个 key 触发一次 mappingFunction，后续命中不重复调用")
        void differentKeys_eachTriggersOneMappingFunctionCall() {
            AtomicInteger callCount = new AtomicInteger(0);
            StepFlowSoftCache<String, String> cache = new StepFlowSoftCache<>();
            Function<String, String> fn = k -> {
                callCount.incrementAndGet();
                return "value-" + k;
            };

            // 三个不同 key 首次写入
            cache.getOrPut("keyA", fn);
            cache.getOrPut("keyB", fn);
            cache.getOrPut("keyC", fn);
            assertEquals(3, callCount.get(), "3 个不同 key 应各调用一次 mappingFunction");

            // 再次访问，全部命中缓存
            assertEquals("value-keyA", cache.getOrPut("keyA", fn));
            assertEquals("value-keyB", cache.getOrPut("keyB", fn));
            assertEquals("value-keyC", cache.getOrPut("keyC", fn));
            assertEquals(3, callCount.get(), "缓存命中，callCount 不应增加");
        }

        @Test
        @DisplayName("Integer 类型的 key 正常工作")
        void integerKey_shouldWork() {
            StepFlowSoftCache<Integer, String> cache = new StepFlowSoftCache<>(k -> "num-" + k);
            assertEquals("num-42", cache.getOrPut(42));
            assertEquals("num-42", cache.getOrPut(42), "相同 key 再次访问应命中缓存");
        }
    }

    // =========================================================================
    // 第三组：invalidate / invalidateAll
    // =========================================================================

    @Nested
    @DisplayName("3. invalidate / invalidateAll")
    class InvalidationTests {

        @Test
        @DisplayName("invalidate(key) 后再次 getOrPut：mappingFunction 被重新调用")
        void afterInvalidate_getOrPut_shouldRecompute() {
            AtomicInteger callCount = new AtomicInteger(0);
            StepFlowSoftCache<String, String> cache = new StepFlowSoftCache<>();
            // 每次调用返回不同的值，便于验证是否真的重新计算
            Function<String, String> fn = k -> "value-v" + callCount.incrementAndGet();

            cache.getOrPut("key1", fn);
            assertEquals(1, callCount.get());

            cache.invalidate("key1");

            String recomputedValue = cache.getOrPut("key1", fn);
            assertEquals(2, callCount.get(), "invalidate 后应重新调用 mappingFunction");
            assertEquals("value-v2", recomputedValue, "重新计算的值应来自第二次调用");
        }

        @Test
        @DisplayName("invalidate 不存在的 key：不抛异常")
        void invalidateNonExistentKey_shouldNotThrow() {
            StepFlowSoftCache<String, String> cache = new StepFlowSoftCache<>();
            assertDoesNotThrow(() -> cache.invalidate("non-existent-key"));
        }

        @Test
        @DisplayName("invalidateAll() 后所有 key 均重新计算")
        void afterInvalidateAll_allKeysShouldRecompute() {
            AtomicInteger callCount = new AtomicInteger(0);
            StepFlowSoftCache<String, String> cache = new StepFlowSoftCache<>();
            Function<String, String> fn = k -> {
                callCount.incrementAndGet();
                return "value-" + k;
            };

            cache.getOrPut("k1", fn);
            cache.getOrPut("k2", fn);
            cache.getOrPut("k3", fn);
            assertEquals(3, callCount.get(), "初始写入应各触发一次 mappingFunction");

            cache.invalidateAll();

            cache.getOrPut("k1", fn);
            cache.getOrPut("k2", fn);
            cache.getOrPut("k3", fn);
            assertEquals(6, callCount.get(), "invalidateAll 后三个 key 均应重新调用 mappingFunction");
        }
    }

    // =========================================================================
    // 第四组：软引用 GC 回收后的 retry 逻辑
    // =========================================================================

    @Nested
    @DisplayName("4. 软引用 GC 回收后的 retry 逻辑")
    class SoftReferenceGcRetryTests {

        @Test
        @DisplayName("单个 key 的 referent 被回收：getOrPut 应 retry 并重新计算新值")
        void singleKeyGcEvicted_getOrPut_shouldRetryAndRecompute() throws Exception {
            AtomicInteger callCount = new AtomicInteger(0);
            StepFlowSoftCache<String, String> cache = new StepFlowSoftCache<>();
            // 返回值携带调用次数，便于区分是第几次计算结果
            Function<String, String> fn = k -> "value-v" + callCount.incrementAndGet();

            // 第一次写入
            String firstValue = cache.getOrPut("key1", fn);
            assertEquals("value-v1", firstValue);
            assertEquals(1, callCount.get());

            // 模拟 GC：清空 referent + 推入 referenceQueue
            simulateGc(cache, "key1");

            // getOrPut 内部 retry 逻辑：检测到 ref.get() == null → 移除失效条目 → 重新计算
            String secondValue = cache.getOrPut("key1", fn);
            assertEquals("value-v2", secondValue, "GC 后 retry 应返回 mappingFunction 的新计算值");
            assertEquals(2, callCount.get(), "GC 回收后应重新调用 mappingFunction 一次");
        }

        @Test
        @DisplayName("多个 key 中部分被回收：被回收的 key 重新计算，未被回收的 key 仍命中缓存")
        void partialGcEviction_evictedKeyRecomputed_survivorCacheHit() throws Exception {
            AtomicInteger callCount = new AtomicInteger(0);
            StepFlowSoftCache<String, Integer> cache = new StepFlowSoftCache<>();
            // 值直接使用 callCount，便于验证
            Function<String, Integer> fn = k -> callCount.incrementAndGet();

            cache.getOrPut("a", fn); // callCount = 1
            cache.getOrPut("b", fn); // callCount = 2
            cache.getOrPut("c", fn); // callCount = 3

            // 模拟 a、c 被 GC 回收，b 不受影响
            simulateGc(cache, "a");
            simulateGc(cache, "c");

            // b 仍然命中缓存
            int bVal = cache.getOrPut("b", fn);
            assertEquals(2, bVal, "b 的 referent 未被回收，应直接命中缓存");
            assertEquals(3, callCount.get(), "b 命中缓存，callCount 不应增加");

            // a 和 c 被回收，retry 后重新计算
            cache.getOrPut("a", fn); // callCount = 4
            cache.getOrPut("c", fn); // callCount = 5
            assertEquals(5, callCount.get(), "a 和 c 被回收后各重新调用一次 mappingFunction");
        }
    }

    // =========================================================================
    // 第五组：evictStaleEntries 间接验证
    // =========================================================================

    @Nested
    @DisplayName("5. evictStaleEntries 间接验证")
    class EvictStaleEntriesTests {

        @Test
        @DisplayName("stale 条目在下一次 computeIfAbsent 内被 evictStaleEntries 批量清除")
        void staleEntries_shouldBeEvictedOnNextComputeIfAbsent() throws Exception {
            StepFlowSoftCache<String, String> cache = new StepFlowSoftCache<>();
            Function<String, String> fn = k -> "value-" + k;

            // 准备 3 个缓存条目
            cache.getOrPut("stale1", fn);
            cache.getOrPut("stale2", fn);
            cache.getOrPut("alive", fn);

            // 对 stale1、stale2 完整模拟 GC（清空 referent + 推入 referenceQueue）
            simulateGc(cache, "stale1");
            simulateGc(cache, "stale2");

            ConcurrentHashMap<String, Reference<String>> internalCache = getInternalCache(cache);
            // evictStaleEntries 尚未执行，stale 条目仍在 map 中
            assertTrue(internalCache.containsKey("stale1"), "evictStaleEntries 触发前 stale1 应仍在 map 中");
            assertTrue(internalCache.containsKey("stale2"), "evictStaleEntries 触发前 stale2 应仍在 map 中");

            // 触发一次新 key 的 getOrPut → computeIfAbsent → 内部调用 evictStaleEntries
            // evictStaleEntries：poll referenceQueue（有条目，进入 if）→ 排空队列 → 扫描 cache → 清除 null referent 条目
            cache.getOrPut("trigger", fn);

            // 验证 stale 条目已被批量清除
            assertFalse(internalCache.containsKey("stale1"), "evictStaleEntries 应清除 stale1");
            assertFalse(internalCache.containsKey("stale2"), "evictStaleEntries 应清除 stale2");
            // 验证非 stale 条目不受影响
            assertTrue(internalCache.containsKey("alive"), "alive 的 referent 未被回收，不应被清除");
            assertTrue(internalCache.containsKey("trigger"), "新写入的 trigger 应存在于 map 中");
        }

        @Test
        @DisplayName("referenceQueue 为空时：evictStaleEntries 不扫描缓存，现有条目不受影响")
        void emptyReferenceQueue_evictStaleEntries_shouldNotTouchExistingEntries() throws Exception {
            StepFlowSoftCache<String, String> cache = new StepFlowSoftCache<>();
            Function<String, String> fn = k -> "value-" + k;

            cache.getOrPut("k1", fn);
            cache.getOrPut("k2", fn);

            ConcurrentHashMap<String, Reference<String>> internalCache = getInternalCache(cache);
            int sizeBefore = internalCache.size();

            // 触发新 key 的 getOrPut：referenceQueue 为空，evictStaleEntries 不扫描，不移除任何条目
            cache.getOrPut("k3", fn);

            // k1、k2 不应被误删，map 大小应恰好增加 1
            assertEquals(sizeBefore + 1, internalCache.size(),
                    "无 GC 事件时 evictStaleEntries 不应移除任何条目");
            assertTrue(internalCache.containsKey("k1"));
            assertTrue(internalCache.containsKey("k2"));
        }
    }

    // =========================================================================
    // 第六组：并发安全
    // =========================================================================

    @Nested
    @DisplayName("6. 并发安全")
    class ConcurrencyTests {

        @Test
        @DisplayName("100 线程同时 getOrPut 同一 key：mappingFunction 只被调用一次，所有线程拿到相同值")
        void concurrentGetOrPutSameKey_mappingFunctionCalledExactlyOnce() throws Exception {
            int threadCount = 100;
            AtomicInteger callCount = new AtomicInteger(0);
            StepFlowSoftCache<String, String> cache = new StepFlowSoftCache<>();
            Function<String, String> fn = k -> {
                callCount.incrementAndGet();
                // 增加耗时，扩大并发竞争窗口
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return "value-" + k;
            };

            // 所有线程在 startLatch 放闸后同时起跑
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch doneLatch = new CountDownLatch(threadCount);
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            List<String> results = new CopyOnWriteArrayList<>();

            for (int i = 0; i < threadCount; i++) {
                executor.submit(() -> {
                    try {
                        startLatch.await();
                        results.add(cache.getOrPut("shared-key", fn));
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        doneLatch.countDown();
                    }
                });
            }

            startLatch.countDown();
            assertTrue(doneLatch.await(10, TimeUnit.SECONDS), "所有线程应在超时前完成");
            executor.shutdown();

            assertEquals(threadCount, results.size(), "所有线程均应拿到返回值");
            assertTrue(results.stream().allMatch("value-shared-key"::equals),
                    "所有线程应拿到相同的值");
            assertEquals(1, callCount.get(), "并发场景下 mappingFunction 只应被调用一次");
        }

        @Test
        @DisplayName("并发 invalidate 与 getOrPut 交叉：不抛任何异常")
        void concurrentInvalidateAndGetOrPut_shouldNotThrow() throws Exception {
            int threadCount = 50;
            StepFlowSoftCache<String, String> cache = new StepFlowSoftCache<>(k -> "value-" + k);
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch doneLatch = new CountDownLatch(threadCount * 2);
            ExecutorService executor = Executors.newFixedThreadPool(threadCount * 2);
            List<Throwable> errors = new CopyOnWriteArrayList<>();

            // 一半线程做 getOrPut，另一半线程做 invalidate，对同一个 key 高并发交叉
            for (int i = 0; i < threadCount; i++) {
                executor.submit(() -> {
                    try {
                        startLatch.await();
                        cache.getOrPut("key");
                    } catch (Exception e) {
                        errors.add(e);
                    } finally {
                        doneLatch.countDown();
                    }
                });
                executor.submit(() -> {
                    try {
                        startLatch.await();
                        cache.invalidate("key");
                    } catch (Exception e) {
                        errors.add(e);
                    } finally {
                        doneLatch.countDown();
                    }
                });
            }

            startLatch.countDown();
            assertTrue(doneLatch.await(10, TimeUnit.SECONDS), "所有线程应在超时前完成");
            executor.shutdown();

            assertTrue(errors.isEmpty(), "并发操作不应抛出任何异常，实际异常：" + errors);
        }
    }
}
