/*
 * Copyright 2024-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.cloud.ai.graph.utils;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link CollectionsUtils}.
 */
class CollectionsUtilsTest {

    @Test
    void lastReturnsLastElementOrEmpty() {
        assertEquals(Optional.of(3), CollectionsUtils.last(List.of(1, 2, 3)));
        assertEquals(Optional.empty(), CollectionsUtils.last(List.of()));
        assertEquals(Optional.empty(), CollectionsUtils.last(null));
    }

    @Test
    void lastMinusReturnsElementCountedFromTheEnd() {
        List<Integer> values = List.of(10, 20, 30);
        assertEquals(Optional.of(30), CollectionsUtils.lastMinus(values, 0));
        assertEquals(Optional.of(20), CollectionsUtils.lastMinus(values, 1));
        assertEquals(Optional.of(10), CollectionsUtils.lastMinus(values, 2));
    }

    @Test
    void lastMinusReturnsEmptyForOutOfRangeOrInvalidInput() {
        List<Integer> values = List.of(10, 20, 30);
        assertEquals(Optional.empty(), CollectionsUtils.lastMinus(values, 3));
        assertEquals(Optional.empty(), CollectionsUtils.lastMinus(values, -1));
        assertEquals(Optional.empty(), CollectionsUtils.lastMinus(List.of(), 0));
        assertEquals(Optional.empty(), CollectionsUtils.lastMinus(null, 0));
    }

    @Test
    void mergeMapCombinesEntriesFromBothMaps() {
        Map<String, Integer> result = CollectionsUtils.mergeMap(Map.of("a", 1), Map.of("b", 2));
        assertEquals(2, result.size());
        assertEquals(1, result.get("a"));
        assertEquals(2, result.get("b"));
    }

    @Test
    void mergeMapWithoutMergeFunctionRejectsDuplicateKeys() {
        Map<String, Integer> map1 = Map.of("a", 1);
        Map<String, Integer> map2 = Map.of("a", 2);
        assertThrows(IllegalStateException.class, () -> CollectionsUtils.mergeMap(map1, map2));
    }

    @Test
    void mergeMapWithMergeFunctionResolvesCollisions() {
        Map<String, Integer> result = CollectionsUtils.mergeMap(Map.of("a", 1), Map.of("a", 2), Integer::sum);
        assertEquals(1, result.size());
        assertEquals(3, result.get("a"));
    }

    @Test
    void mergeMapRejectsNullArguments() {
        assertThrows(NullPointerException.class, () -> CollectionsUtils.mergeMap(null, Map.of()));
        assertThrows(NullPointerException.class, () -> CollectionsUtils.mergeMap(Map.of(), null));
        assertThrows(NullPointerException.class, () -> CollectionsUtils.mergeMap(Map.of(), Map.of(), null));
    }

    @Test
    void entryOfBuildsImmutableEntry() {
        Map.Entry<String, Integer> entry = CollectionsUtils.entryOf("k", 7);
        assertEquals("k", entry.getKey());
        assertEquals(7, entry.getValue());
        assertThrows(UnsupportedOperationException.class, () -> entry.setValue(9));
    }

    @Test
    void listOfCreatesListFromElements() {
        assertTrue(CollectionsUtils.listOf().isEmpty());
        assertEquals(List.of("a"), CollectionsUtils.listOf("a"));
        assertEquals(List.of("a", "b", "c"), CollectionsUtils.listOf("a", "b", "c"));
    }

    @Test
    void mapOfCreatesMapsWithGivenPairs() {
        assertTrue(CollectionsUtils.mapOf().isEmpty());
        assertEquals(Map.of("k", "v"), CollectionsUtils.mapOf("k", "v"));
        assertEquals(Map.of("a", 1, "b", 2), CollectionsUtils.mapOf("a", 1, "b", 2));
        assertEquals(Map.of("a", 1, "b", 2, "c", 3), CollectionsUtils.mapOf("a", 1, "b", 2, "c", 3));
    }

    @Test
    void toStringRendersEmptyCollectionAndMap() {
        assertEquals("[]", CollectionsUtils.toString(List.of()));
        assertEquals("{}", CollectionsUtils.toString(Map.of()));
    }

    @Test
    void toStringRendersElementsForNonEmptyCollection() {
        String rendered = CollectionsUtils.toString(List.of("x", "y"));
        assertTrue(rendered.contains("x"));
        assertTrue(rendered.contains("y"));
        assertFalse(rendered.equals("[]"));
    }
}
