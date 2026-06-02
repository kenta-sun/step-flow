package io.github.kentasun.stepflow.dto;

import java.util.*;

/**
 * 为了多线程分叉运行、无锁隔离设计的高性能 Map 包装器。
 * @author kenta-sun
 */
public class ForkMap<K, V> implements Map<K, V> {

    // 全局共享、只读的原始 Map
    private final Map<K, V> globalMap;
    // 当前多线程分支下，私有、存放增量/修改数据的 HashMap
    private final Map<K, V> privateMap;

    /**
     * 构造函数
     * @param globalMap 全局共享且此时只读的 Map，不能为 null
     */
    public ForkMap(Map<K, V> globalMap) {
        if (globalMap == null) {
            throw new IllegalArgumentException("Shared parent map cannot be null");
        }
        this.globalMap = globalMap;
        this.privateMap = new HashMap<>();
    }

    @Override
    public int size() {
        throw new UnsupportedOperationException("不支持 size() 方法");
    }

    @Override
    public boolean isEmpty() {
        throw new UnsupportedOperationException("不支持 isEmpty() 方法");
    }

    @Override
    public boolean containsKey(Object key) {
        // 优先看子 Map 有没有，子 Map 没有再看父 Map
        return privateMap.containsKey(key) || globalMap.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        throw new UnsupportedOperationException("不支持 containsValue() 方法");
    }

    @Override
    public V get(Object key) {
        // 优先从子 Map 找新产生或被修改的数据，找不到再去全局只读的 parent 找
        if (privateMap.containsKey(key)) {
            return privateMap.get(key);
        }
        return globalMap.get(key);
    }

    @Override
    public V put(K key, V value) {
        // 多线程并发运行中，所有新产生或修改的数据，一律只写入自己私有的子 Map
        return privateMap.put(key, value);
    }

    @Override
    public V remove(Object key) {
        throw new UnsupportedOperationException("不支持 remove() 方法");
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        // 不允许修改 globalMap，所以用 privateMap 接收所有数据
        this.privateMap.putAll(m);
    }

    @Override
    public void clear() {
        // 仅清空当前线程分支的增量数据，无权也不应该影响全局只读的 parent Map
        privateMap.clear();
    }

    @Override
    public Set<K> keySet() {
        throw new UnsupportedOperationException("不支持 keySet() 方法");
    }

    @Override
    public Collection<V> values() {
        throw new UnsupportedOperationException("不支持 values() 方法");
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        throw new UnsupportedOperationException("不支持 entrySet() 方法");
    }

    public Map<K, V> getPrivateMap() {
        return privateMap;
    }
}
