package com.alibaba.cloud.ai.graph.state.strategy;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class MergeStrategyTest {

    private final MergeStrategy strategy = new MergeStrategy();

    @Test
    void testMergeSimpleValues() {
        Object result = strategy.apply("Old", "New");
        assertEquals("New", result);
    }

    @Test
    void testMergeMaps() {
        Map<String, String> oldMap = new HashMap<>();
        oldMap.put("key1", "value1");

        Map<String, String> newMap = new HashMap<>();
        newMap.put("key2", "value2");

        Map<String, String> result = (Map<String, String>) strategy.apply(oldMap, newMap);

        assertEquals(2, result.size());
        assertEquals("value1", result.get("key1"));
        assertEquals("value2", result.get("key2"));
    }

    @Test
    void testOptionalUnwrap() {
        Object result = strategy.apply(Optional.of("Old"), Optional.of("New"));
        assertEquals("New", result);
    }

    @Test
    void testOptionalMixed() {
        Object result1 = strategy.apply(Optional.of("Old"), "New");
        assertEquals("New", result1);

        Object result2 = strategy.apply("Old", Optional.of("New"));
        assertEquals("New", result2);
    }

    @Test
    void testOptionalEmpty() {
        Object result = strategy.apply(Optional.of("Old"), Optional.empty());
        assertEquals("Old", result);

        Object result2 = strategy.apply(Optional.empty(), Optional.of("New"));
        assertEquals("New", result2);
    }

    @Test
    void testNullHandling() {
        assertEquals("Old", strategy.apply("Old", null));
        assertEquals("New", strategy.apply(null, "New"));
        assertNull(strategy.apply(null, null));
    }
}
