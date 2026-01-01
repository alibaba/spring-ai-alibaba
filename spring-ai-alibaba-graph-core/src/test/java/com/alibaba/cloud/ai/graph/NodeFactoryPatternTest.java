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
package com.alibaba.cloud.ai.graph;

import com.alibaba.cloud.ai.graph.action.AsyncNodeActionWithConfig;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.internal.node.Node;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


public class NodeFactoryPatternTest {

    @Test
    public void testNodeFactoryCreatesNewInstances() throws Exception {
        Node.ActionFactory mockFactory = mock(Node.ActionFactory.class);
        AsyncNodeActionWithConfig mockAction1 = mock(AsyncNodeActionWithConfig.class);
        AsyncNodeActionWithConfig mockAction2 = mock(AsyncNodeActionWithConfig.class);

        when(mockFactory.apply(any(CompileConfig.class)))
                .thenReturn(mockAction1)
                .thenReturn(mockAction2);

        TestableCompiledGraph compiledGraph = new TestableCompiledGraph();
        compiledGraph.putNodeFactory("testNode", mockFactory);

        AsyncNodeActionWithConfig action1 = compiledGraph.getNodeAction("testNode");
        AsyncNodeActionWithConfig action2 = compiledGraph.getNodeAction("testNode");

        assertNotNull(action1, "First action should not be null");
        assertNotNull(action2, "Second action should not be null");
        assertNotSame(action1, action2, "Should return different instances each time");

        verify(mockFactory, times(2)).apply(any(CompileConfig.class));
    }

    @Test
    public void testConcurrentNodeCreation() throws Exception {
        AtomicInteger creationCount = new AtomicInteger(0);
        Node.ActionFactory mockFactory = mock(Node.ActionFactory.class);

        when(mockFactory.apply(any(CompileConfig.class))).thenAnswer(invocation -> {
            creationCount.incrementAndGet();
            AsyncNodeActionWithConfig mockAction = mock(AsyncNodeActionWithConfig.class);
            when(mockAction.toString()).thenReturn("Instance-" + creationCount.get());
            return mockAction;
        });

        TestableCompiledGraph compiledGraph = new TestableCompiledGraph();
        compiledGraph.putNodeFactory("testNode", mockFactory);

        ExecutorService executor = Executors.newFixedThreadPool(20);
        CountDownLatch latch = new CountDownLatch(50);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicReference<Exception> errorRef = new AtomicReference<>();

        for (int i = 0; i < 50; i++) {
            executor.submit(() -> {
                try {
                    AsyncNodeActionWithConfig action = compiledGraph.getNodeAction("testNode");
                    assertNotNull(action, "Action should not be null");
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    errorRef.set(e);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        assertNull(errorRef.get(), "No exceptions should occur during concurrent access");
        assertEquals(50, successCount.get(), "All 50 calls should succeed");
        assertEquals(50, creationCount.get(), "Factory should be called 50 times");
    }

    private static class TestableCompiledGraph extends CompiledGraph {

        public TestableCompiledGraph() throws GraphStateException {
            super(createMinimalStateGraph(), CompileConfig.builder().build());
        }

        public void putNodeFactory(String nodeId, Node.ActionFactory factory) {
            this.nodeFactories.put(nodeId, factory);
        }

        private static StateGraph createMinimalStateGraph() throws GraphStateException {
            return new StateGraph();
        }
    }
}
