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

    @Test
    void testMergeObjectFields() {
        PlanState oldPlan = new PlanState("Original title", "Initial thought", null);
        PlanState newPlan = new PlanState(null, "Updated thought", "Completed");

        PlanState result = (PlanState) strategy.apply(oldPlan, newPlan);

        assertNotSame(oldPlan, result);
        assertNotSame(newPlan, result);
        assertEquals("Original title", result.title);
        assertEquals("Updated thought", result.thought);
        assertEquals("Completed", result.status);
    }

    @Test
    void testMergeNestedObjectFields() {
        PlanState oldPlan = new PlanState("Original title", "Initial thought", "Running");
        oldPlan.metrics = new PlanMetrics(4, null);
        PlanState newPlan = new PlanState(null, "Updated thought", null);
        newPlan.metrics = new PlanMetrics(null, 2);

        PlanState result = (PlanState) strategy.apply(oldPlan, newPlan);

        assertEquals("Original title", result.title);
        assertEquals("Updated thought", result.thought);
        assertEquals("Running", result.status);
        assertEquals(4, result.metrics.totalSteps);
        assertEquals(2, result.metrics.completedSteps);
    }

    @Test
    void testMergeObjectMapFields() {
        PlanState oldPlan = new PlanState("Original title", null, null);
        oldPlan.metadata.put("source", "planner");
        PlanState newPlan = new PlanState(null, null, "Completed");
        newPlan.metadata.put("owner", "worker");

        PlanState result = (PlanState) strategy.apply(oldPlan, newPlan);

        assertEquals("Original title", result.title);
        assertEquals("Completed", result.status);
        assertEquals("planner", result.metadata.get("source"));
        assertEquals("worker", result.metadata.get("owner"));
    }

    @Test
    void testKeepReplacementForObjectsWithoutNoArgsConstructor() {
        ImmutablePlanState oldPlan = new ImmutablePlanState("Original title");
        ImmutablePlanState newPlan = new ImmutablePlanState("Updated title");

        Object result = strategy.apply(oldPlan, newPlan);

        assertSame(newPlan, result);
    }

    @Test
    void testKeepReplacementForObjectsWithoutFields() {
        EmptyState oldState = new EmptyState();
        EmptyState newState = new EmptyState();

        Object result = strategy.apply(oldState, newState);

        assertSame(newState, result);
    }

    @Test
    void testIncompatibleTypes() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> strategy.apply(new PlanState("Original title", null, null), new PlanMetrics(1, 0)));

        assertTrue(exception.getMessage().contains("Cannot merge incompatible types"));
    }

    private static class PlanState {

        private String title;

        private String thought;

        private String status;

        private PlanMetrics metrics;

        private Map<String, String> metadata = new HashMap<>();

        PlanState() {
        }

        PlanState(String title, String thought, String status) {
            this.title = title;
            this.thought = thought;
            this.status = status;
        }

    }

    private static class PlanMetrics {

        private Integer totalSteps;

        private Integer completedSteps;

        PlanMetrics() {
        }

        PlanMetrics(Integer totalSteps, Integer completedSteps) {
            this.totalSteps = totalSteps;
            this.completedSteps = completedSteps;
        }

    }

    private static class ImmutablePlanState {

        private final String title;

        ImmutablePlanState(String title) {
            this.title = title;
        }

    }

    private static class EmptyState {

    }
}
