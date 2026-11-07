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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * {@link ArrayMap} backs the tray image caches, and its {@code getSize()} is the spelling the old dorkbox
 * collections API used, so both that and {@code size()} have to keep working.
 */
public
class ArrayMapTest {

    @Test
    @DisplayName("getSize and size both track put and remove")
    void sizeTracksChanges() {
        final ArrayMap<String, String> map = new ArrayMap<>();

        assertEquals(0, map.getSize());
        assertEquals(0, map.size());
        assertTrue(map.isEmpty());

        map.put("a", "1");
        map.put("b", "2");
        assertEquals(2, map.getSize());
        assertEquals(2, map.size());
        assertFalse(map.isEmpty());

        map.remove("a");
        assertEquals(1, map.getSize());
        assertEquals(1, map.size());

        map.clear();
        assertEquals(0, map.getSize());
        assertTrue(map.isEmpty());
    }

    @Test
    @DisplayName("get returns what was put, and null for keys it does not hold")
    void getReturnsValue() {
        final ArrayMap<String, String> map = new ArrayMap<>();
        map.put("a", "1");

        assertEquals("1", map.get("a"));
        assertNull(map.get("missing"));
        assertTrue(map.containsKey("a"));
        assertFalse(map.containsKey("missing"));
    }

    @Test
    @DisplayName("put returns the value it replaced, and remove returns the value it dropped")
    void putAndRemoveReturnPrevious() {
        final ArrayMap<String, String> map = new ArrayMap<>();

        assertNull(map.put("a", "1"), "the first put replaces nothing");
        assertEquals("1", map.put("a", "2"), "an overwrite returns the old value");
        assertEquals("2", map.get("a"));

        assertEquals("2", map.remove("a"));
        assertNull(map.remove("a"), "removing a missing key returns null");
        assertFalse(map.containsKey("a"));
    }

    @Test
    @DisplayName("entries keep insertion order")
    void entriesAreOrdered() {
        final ArrayMap<String, Integer> map = new ArrayMap<>();
        map.put("first", 1);
        map.put("second", 2);
        map.put("third", 3);

        final List<String> keys = new ArrayList<>();
        for (final Map.Entry<String, Integer> entry : map.entries()) {
            keys.add(entry.getKey());
        }

        assertIterableEquals(List.of("first", "second", "third"), keys);
    }

    @Test
    @DisplayName("values and getValues are live views, not copies")
    void valueViewsAreLive() {
        final ArrayMap<String, String> map = new ArrayMap<>();
        map.put("a", "1");

        assertIterableEquals(List.of("1"), map.values());
        assertIterableEquals(List.of("1"), map.getValues());

        // this is the point of a view: the tray caches read them back after later writes
        map.put("b", "2");
        assertIterableEquals(List.of("1", "2"), map.values());

        map.clear();
        assertTrue(map.values().isEmpty());
    }

    @Test
    @DisplayName("the ordered flag and initial capacity are accepted without breaking the map")
    void constructorArgumentsAreHonoured() {
        final ArrayMap<String, String> ordered = new ArrayMap<>(true, 4);
        ordered.put("b", "2");
        ordered.put("a", "1");
        assertIterableEquals(List.of("2", "1"), ordered.values(), "insertion order is kept");

        final ArrayMap<String, String> small = new ArrayMap<>(false, 0);
        small.put("a", "1");
        assertEquals("1", small.get("a"), "a capacity of 0 must still produce a usable map");
    }

    @Test
    @DisplayName("toString delegates to the backing map")
    void toStringMatchesBackingMap() {
        final ArrayMap<String, String> map = new ArrayMap<>();
        map.put("a", "1");
        map.put("b", "2");

        assertEquals("{a=1, b=2}", map.toString());
    }

    @Test
    @DisplayName("two instances do not share state")
    void instancesAreIndependent() {
        final ArrayMap<String, String> one = new ArrayMap<>();
        final ArrayMap<String, String> two = new ArrayMap<>();

        one.put("a", "1");

        assertEquals(0, two.getSize());
        assertNull(two.get("a"));
    }
}
