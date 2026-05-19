package io.github.kentasun.stepflow.testUtils;

import java.util.concurrent.ConcurrentHashMap;

public class ConcurrentHashMapBuilder<K, V> {

    private final ConcurrentHashMap<K, V> map;

    public ConcurrentHashMapBuilder(ConcurrentHashMap<K, V> map) {
        this.map = map;
    }

    public static <K, V> ConcurrentHashMapBuilder<K, V> builder() {
        return new ConcurrentHashMapBuilder<>(new ConcurrentHashMap<>());
    }

    public ConcurrentHashMapBuilder<K, V> put(K key, V value) {
        this.map.put(key, value);
        return this;
    }

    public ConcurrentHashMap<K, V> build() {
        return this.map;
    }
}
