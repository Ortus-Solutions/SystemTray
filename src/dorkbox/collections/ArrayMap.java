/*
 * Copyright 2024 dorkbox, llc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dorkbox.collections;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A minimal insertion-ordered map.
 *
 * This replaces the former Kotlin `dorkbox:Collections` dependency. Only the small subset of the API that this project
 * uses is provided, so that the Kotlin runtime is not needed.
 *
 * @param <K> the key type
 * @param <V> the value type
 */
@SuppressWarnings("unused")
public class ArrayMap<K, V> {
    public static final String version = "1.0";

    private final Map<K, V> map;

    /**
     * @param ordered true if the keys should retain their insertion order (this implementation always does)
     * @param capacity the initial capacity of the map
     */
    public
    ArrayMap(final boolean ordered, final int capacity) {
        this.map = new LinkedHashMap<>(Math.max(capacity, 1));
    }

    public
    ArrayMap() {
        this(true, 16);
    }

    public
    V get(final K key) {
        return map.get(key);
    }

    public
    V put(final K key, final V value) {
        return map.put(key, value);
    }

    public
    V remove(final K key) {
        return map.remove(key);
    }

    public
    boolean containsKey(final K key) {
        return map.containsKey(key);
    }

    public
    int getSize() {
        return map.size();
    }

    public
    int size() {
        return map.size();
    }

    public
    boolean isEmpty() {
        return map.isEmpty();
    }

    public
    void clear() {
        map.clear();
    }

    /**
     * @return a live view of the values in this map
     */
    public
    Collection<V> values() {
        return map.values();
    }

    /**
     * @return a live view of the values in this map
     */
    public
    Collection<V> getValues() {
        return map.values();
    }

    /**
     * @return a live view of the entries in this map
     */
    public
    Iterable<Map.Entry<K, V>> entries() {
        return map.entrySet();
    }

    @Override
    public
    String toString() {
        return map.toString();
    }
}
