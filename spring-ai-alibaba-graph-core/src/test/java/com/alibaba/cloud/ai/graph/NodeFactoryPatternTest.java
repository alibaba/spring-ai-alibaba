/*
 * Copyright 2024-2025 the original author or authors.
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
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.internal.node.Node;
import com.alibaba.cloud.ai.graph.internal.node.NodeScope;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
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
    public void testGraphRunnerContextNodeCaching() throws Exception {
        Node.ActionFactory mockFactory = mock(Node.ActionFactory.class);
        AtomicInteger creationCount = new AtomicInteger(0);

        when(mockFactory.apply(any(CompileConfig.class))).thenAnswer(invocation -> {
            int id = creationCount.incrementAndGet();
            AsyncNodeActionWithConfig mockAction = mock(AsyncNodeActionWithConfig.class);
            when(mockAction.toString()).thenReturn("CachedInstance-" + id);
            return mockAction;
        });

        TestableCompiledGraph compiledGraph = new TestableCompiledGraph();
        compiledGraph.putNodeFactory("cachedNode", mockFactory);

        OverAllState initialState = new OverAllState(new HashMap<>());
        RunnableConfig config = RunnableConfig.builder().build();
        GraphRunnerContext context = new GraphRunnerContext(initialState, config, compiledGraph);

        AsyncNodeActionWithConfig action1 = context.getNodeAction("cachedNode");
        assertNotNull(action1);
        assertEquals(1, creationCount.get(), "Should create one instance");

        AsyncNodeActionWithConfig action2 = context.getNodeAction("cachedNode");
        assertNotNull(action2);
        assertSame(action1, action2, "Should return cached instance");
        assertEquals(1, creationCount.get(), "Should not create another instance");

        verify(mockFactory, times(1)).apply(any(CompileConfig.class));
    }

    @Test
    public void testParallelNodeFactoryInstantiation() throws Exception {
        Node.ActionFactory branchFactory1 = mock(Node.ActionFactory.class);
        Node.ActionFactory branchFactory2 = mock(Node.ActionFactory.class);
        
        AsyncNodeActionWithConfig branch1Action = mock(AsyncNodeActionWithConfig.class);
        AsyncNodeActionWithConfig branch2Action = mock(AsyncNodeActionWithConfig.class);
        
        when(branchFactory1.apply(any(CompileConfig.class))).thenReturn(branch1Action);
        when(branchFactory2.apply(any(CompileConfig.class))).thenReturn(branch2Action);

        TestableCompiledGraph compiledGraph = new TestableCompiledGraph();
        compiledGraph.putNodeFactory("branch1", branchFactory1);
        compiledGraph.putNodeFactory("branch2", branchFactory2);

        ExecutorService executor = Executors.newFixedThreadPool(4);
        CountDownLatch latch = new CountDownLatch(4);
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < 2; i++) {
            executor.submit(() -> {
                try {
                    AsyncNodeActionWithConfig action = compiledGraph.getNodeAction("branch1");
                    assertNotNull(action);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    fail("Branch1 instantiation failed: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });

            executor.submit(() -> {
                try {
                    AsyncNodeActionWithConfig action = compiledGraph.getNodeAction("branch2");
                    assertNotNull(action);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    fail("Branch2 instantiation failed: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        assertEquals(4, successCount.get(), "All parallel instantiations should succeed");
        verify(branchFactory1, times(2)).apply(any(CompileConfig.class));
        verify(branchFactory2, times(2)).apply(any(CompileConfig.class));
    }

    @Test
    public void testNodeActionIsolationBetweenRuns() throws Exception {
        AtomicInteger stateCounter = new AtomicInteger(0);
        
        Node.ActionFactory statefulFactory = mock(Node.ActionFactory.class);
        when(statefulFactory.apply(any(CompileConfig.class))).thenAnswer(invocation -> {
            AsyncNodeActionWithConfig mockAction = mock(AsyncNodeActionWithConfig.class);
            int state = stateCounter.incrementAndGet();
            when(mockAction.toString()).thenReturn("StatefulAction-" + state);
            return mockAction;
        });

        TestableCompiledGraph compiledGraph = new TestableCompiledGraph();
        compiledGraph.putNodeFactory("statefulNode", statefulFactory);

        ExecutorService executor = Executors.newFixedThreadPool(10);
        CountDownLatch latch = new CountDownLatch(20);
        AtomicInteger uniqueInstances = new AtomicInteger(0);

        for (int run = 0; run < 20; run++) {
            executor.submit(() -> {
                try {
                    AsyncNodeActionWithConfig action = compiledGraph.getNodeAction("statefulNode");
                    assertNotNull(action);
                    uniqueInstances.incrementAndGet();
                } catch (Exception e) {
                    fail("Node instantiation failed: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        assertEquals(20, uniqueInstances.get(), "Should create 20 unique instances");
        assertEquals(20, stateCounter.get(), "Factory should be called 20 times");
        verify(statefulFactory, times(20)).apply(any(CompileConfig.class));
    }



    @Test
    public void testFactoryExceptionHandling() throws Exception {
        Node.ActionFactory faultyFactory = mock(Node.ActionFactory.class);
        when(faultyFactory.apply(any(CompileConfig.class)))
                .thenThrow(new RuntimeException("Factory creation failed"));

        TestableCompiledGraph compiledGraph = new TestableCompiledGraph();
        compiledGraph.putNodeFactory("faultyNode", faultyFactory);

        assertThrows(RuntimeException.class, () -> {
            compiledGraph.getNodeAction("faultyNode");
        }, "Factory exception should be propagated");

        verify(faultyFactory, times(1)).apply(any(CompileConfig.class));
    }

    @Test
    public void testNonExistentNodeHandling() throws Exception {
        TestableCompiledGraph compiledGraph = new TestableCompiledGraph();

        AsyncNodeActionWithConfig result = compiledGraph.getNodeAction("nonExistentNode");
        assertNull(result, "Should return null for non-existent node");
    }

    @Test
    public void testMemoryLeakPrevention() throws Exception {
        Node.ActionFactory factory = mock(Node.ActionFactory.class);
        AtomicInteger createdInstances = new AtomicInteger(0);

        when(factory.apply(any(CompileConfig.class))).thenAnswer(invocation -> {
            createdInstances.incrementAndGet();
            AsyncNodeActionWithConfig mockAction = mock(AsyncNodeActionWithConfig.class);
            when(mockAction.toString()).thenReturn("TempInstance-" + createdInstances.get());
            return mockAction;
        });

        TestableCompiledGraph compiledGraph = new TestableCompiledGraph();
        compiledGraph.putNodeFactory("tempNode", factory);

        for (int i = 0; i < 10; i++) {
            AsyncNodeActionWithConfig action = compiledGraph.getNodeAction("tempNode");
            assertNotNull(action);
        }

        assertEquals(10, createdInstances.get(), "Should create 10 instances");
        verify(factory, times(10)).apply(any(CompileConfig.class));

        System.gc();
        Thread.sleep(100);

        AsyncNodeActionWithConfig newAction = compiledGraph.getNodeAction("tempNode");
        assertNotNull(newAction);
        assertEquals(11, createdInstances.get(), "Should continue creating new instances");
    }

    @Test
    public void testFactoryThreadSafety() throws Exception {
        AtomicInteger factoryCallCount = new AtomicInteger(0);
        AtomicInteger instanceIdCounter = new AtomicInteger(0);

        Node.ActionFactory threadSafeFactory = compileConfig -> {
            int callNumber = factoryCallCount.incrementAndGet();
            int instanceId = instanceIdCounter.incrementAndGet();
            
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            AsyncNodeActionWithConfig mockAction = mock(AsyncNodeActionWithConfig.class);
            when(mockAction.toString()).thenReturn("ThreadSafeInstance-" + instanceId + "-Call-" + callNumber);
            return mockAction;
        };

        TestableCompiledGraph compiledGraph = new TestableCompiledGraph();
        compiledGraph.putNodeFactory("threadSafeNode", threadSafeFactory);

        ExecutorService executor = Executors.newFixedThreadPool(15);
        CountDownLatch latch = new CountDownLatch(30);
        AtomicInteger successCount = new AtomicInteger(0);
        java.util.Set<String> instanceIds = java.util.concurrent.ConcurrentHashMap.newKeySet();

        for (int i = 0; i < 30; i++) {
            executor.submit(() -> {
                try {
                    AsyncNodeActionWithConfig action = compiledGraph.getNodeAction("threadSafeNode");
                    assertNotNull(action);
                    instanceIds.add(action.toString());
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    fail("Thread-safe factory call failed: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        assertEquals(30, successCount.get(), "All 30 calls should succeed");
        assertEquals(30, factoryCallCount.get(), "Factory should be called exactly 30 times");
        assertEquals(30, instanceIds.size(), "Should create 30 unique instances");
    }





    @Test
    public void testCorrectNodeFactoryPatternSolution() throws Exception {
        AtomicInteger instanceCreationCount = new AtomicInteger(0);
        AtomicInteger totalStateModifications = new AtomicInteger(0);
        
        Node.ActionFactory threadSafeFactory = compileConfig -> {
            int instanceId = instanceCreationCount.incrementAndGet();
            
            AtomicInteger instanceStateCount = new AtomicInteger(0);
            
            NodeAction isolatedInstance = state -> {
                String currentMessage = (String) state.value("messages").orElse("no message");
                int localModificationCount = instanceStateCount.incrementAndGet();
                totalStateModifications.incrementAndGet();
                
                return Map.of(
                    "response", "Response from instance " + instanceId + " to: " + currentMessage,
                    "instanceId", instanceId,
                    "localModificationCount", localModificationCount,
                    "timestamp", System.nanoTime()
                );
            };
            
            return AsyncNodeActionWithConfig.node_async((state, config) -> {
                try {
                    return isolatedInstance.apply(state);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        };
        
        TestableCompiledGraph compiledGraph = new TestableCompiledGraph();
        compiledGraph.putNodeFactory("threadSafeNode", threadSafeFactory);
        
        ExecutorService executor = Executors.newFixedThreadPool(10);
        CountDownLatch latch = new CountDownLatch(20);
        AtomicInteger successCount = new AtomicInteger(0);
        Set<Integer> uniqueInstanceIds = java.util.concurrent.ConcurrentHashMap.newKeySet();
        Set<String> uniqueResponses = java.util.concurrent.ConcurrentHashMap.newKeySet();
        
        for (int i = 0; i < 20; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    AsyncNodeActionWithConfig action = compiledGraph.getNodeAction("threadSafeNode");
                    OverAllState state = new OverAllState(Map.of("messages", "Message from thread " + threadId));
                    RunnableConfig config = RunnableConfig.builder().build();
                    
                    CompletableFuture<Map<String, Object>> result = action.apply(state, config);
                    Map<String, Object> output = result.get();
                    
                    assertNotNull(output);
                    uniqueInstanceIds.add((Integer) output.get("instanceId"));
                    uniqueResponses.add((String) output.get("response"));
                    successCount.incrementAndGet();
                    
                } catch (Exception e) {
                    fail("Factory pattern call failed: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await();
        executor.shutdown();
        
        assertEquals(20, successCount.get(), "All calls should succeed");
        assertEquals(20, instanceCreationCount.get(), "Should create 20 separate instances");
        assertEquals(20, uniqueResponses.size(), "Should have 20 unique responses");
        assertEquals(20, totalStateModifications.get(), "Should have 20 total state modifications");
        
        assertTrue(uniqueInstanceIds.contains(1), "Should contain first instance");
        assertTrue(uniqueInstanceIds.contains(20), "Should contain last instance");
    }

    /**
     * 验证在高并发场景下nodeFactory模式的稳定性
     */
    @Test
    public void testHighConcurrencyNodeFactoryStability() throws Exception {
        AtomicInteger factoryCallCount = new AtomicInteger(0);
        AtomicInteger processedRequestCount = new AtomicInteger(0);
        
        Node.ActionFactory stressTestFactory = compileConfig -> {
            int factoryCallId = factoryCallCount.incrementAndGet();
            
            NodeAction action = state -> {
                int requestId = processedRequestCount.incrementAndGet();
                
                int result = 0;
                for (int i = 0; i < 1000; i++) {
                    result += i;
                }
                
                return Map.of(
                    "factoryCallId", factoryCallId,
                    "requestId", requestId,
                    "computationResult", result,
                    "threadName", Thread.currentThread().getName()
                );
            };
            
            return AsyncNodeActionWithConfig.node_async((state, config) -> {
                try {
                    return action.apply(state);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        };
        
        TestableCompiledGraph compiledGraph = new TestableCompiledGraph();
        compiledGraph.putNodeFactory("stressTestNode", stressTestFactory);
        
        int threadCount = 50;
        int callsPerThread = 10;
        int totalExpectedCalls = threadCount * callsPerThread;
        
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(totalExpectedCalls);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);
        Set<Integer> uniqueFactoryCallIds = java.util.concurrent.ConcurrentHashMap.newKeySet();
        Set<Integer> uniqueRequestIds = java.util.concurrent.ConcurrentHashMap.newKeySet();
        
        for (int threadIndex = 0; threadIndex < threadCount; threadIndex++) {
            final int threadId = threadIndex;
            
            executor.submit(() -> {
                for (int callIndex = 0; callIndex < callsPerThread; callIndex++) {
                    try {
                        AsyncNodeActionWithConfig action = compiledGraph.getNodeAction("stressTestNode");
                        OverAllState state = new OverAllState(Map.of(
                            "threadId", threadId,
                            "callIndex", callIndex
                        ));
                        RunnableConfig config = RunnableConfig.builder().build();
                        
                        CompletableFuture<Map<String, Object>> result = action.apply(state, config);
                        Map<String, Object> output = result.get();
                        
                        assertNotNull(output);
                        uniqueFactoryCallIds.add((Integer) output.get("factoryCallId"));
                        uniqueRequestIds.add((Integer) output.get("requestId"));
                        successCount.incrementAndGet();
                        
                    } catch (Exception e) {
                        errorCount.incrementAndGet();
                        System.err.println("Error in thread " + threadId + ", call " + callIndex + ": " + e.getMessage());
                    } finally {
                        latch.countDown();
                    }
                }
            });
        }
        
        latch.await();
        executor.shutdown();
        
        assertEquals(totalExpectedCalls, successCount.get(), "All calls should succeed");
        assertEquals(0, errorCount.get(), "Should have no errors");
        
        assertEquals(totalExpectedCalls, factoryCallCount.get(), "Factory should be called for each request");
        assertEquals(totalExpectedCalls, uniqueFactoryCallIds.size(), "Should have unique factory call IDs");
        assertEquals(totalExpectedCalls, uniqueRequestIds.size(), "Should have unique request IDs");
        assertEquals(totalExpectedCalls, processedRequestCount.get(), "Should process all requests");
    }
    @Test
    public void testReactAgentNodeFactoryThreadSafety() throws Exception {
        AtomicInteger llmInstanceCreationCount = new AtomicInteger(0);
        AtomicInteger toolInstanceCreationCount = new AtomicInteger(0);
        
        Node.ActionFactory llmNodeFactory = compileConfig -> {
            int instanceId = llmInstanceCreationCount.incrementAndGet();
            
            NodeAction llmNodeInstance = state -> {
                String messages = (String) state.value("messages").orElse("no messages");
                
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                
                return Map.of(
                    "messages", "LLM response to: " + messages + " (from instance " + instanceId + ")",
                    "instanceId", instanceId,
                    "threadName", Thread.currentThread().getName(),
                    "timestamp", System.nanoTime()
                );
            };
            
            return AsyncNodeActionWithConfig.node_async((state, config) -> {
                try {
                    return llmNodeInstance.apply(state);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        };
        
        Node.ActionFactory toolNodeFactory = compileConfig -> {
            int instanceId = toolInstanceCreationCount.incrementAndGet();
            
            NodeAction toolNodeInstance = state -> {
                String messages = (String) state.value("messages").orElse("no messages");
                
                return Map.of(
                    "messages", "Tool response to: " + messages + " (from tool instance " + instanceId + ")",
                    "toolInstanceId", instanceId,
                    "threadName", Thread.currentThread().getName()
                );
            };
            
            return AsyncNodeActionWithConfig.node_async((state, config) -> {
                try {
                    return toolNodeInstance.apply(state);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        };
        
        TestableCompiledGraph compiledGraph = new TestableCompiledGraph();
        compiledGraph.putNodeFactory("llm", llmNodeFactory);
        compiledGraph.putNodeFactory("tool", toolNodeFactory);
        
        ExecutorService executor = Executors.newFixedThreadPool(10);
        CountDownLatch latch = new CountDownLatch(30);
        AtomicInteger successCount = new AtomicInteger(0);
        Set<Integer> uniqueLlmInstanceIds = java.util.concurrent.ConcurrentHashMap.newKeySet();
        Set<Integer> uniqueToolInstanceIds = java.util.concurrent.ConcurrentHashMap.newKeySet();
        Set<String> uniqueLlmResponses = java.util.concurrent.ConcurrentHashMap.newKeySet();
        
        for (int i = 0; i < 30; i++) {
            final int callId = i;
            executor.submit(() -> {
                try {
                    
                    AsyncNodeActionWithConfig llmAction = compiledGraph.getNodeAction("llm");
                    assertNotNull(llmAction, "LLM action should not be null");
                    
                    OverAllState state = new OverAllState(Map.of(
                        "messages", "User message " + callId
                    ));
                    RunnableConfig config = RunnableConfig.builder().build();
                    
                    CompletableFuture<Map<String, Object>> llmResult = llmAction.apply(state, config);
                    Map<String, Object> llmOutput = llmResult.get();
                    
                    assertNotNull(llmOutput);
                    assertTrue(llmOutput.containsKey("instanceId"), "Should have instance ID");
                    
                    Integer llmInstanceId = (Integer) llmOutput.get("instanceId");
                    String llmResponse = (String) llmOutput.get("messages");
                    
                    uniqueLlmInstanceIds.add(llmInstanceId);
                    uniqueLlmResponses.add(llmResponse);
                    
                    AsyncNodeActionWithConfig toolAction = compiledGraph.getNodeAction("tool");
                    assertNotNull(toolAction, "Tool action should not be null");
                    
                    OverAllState updatedState = new OverAllState(llmOutput);
                    CompletableFuture<Map<String, Object>> toolResult = toolAction.apply(updatedState, config);
                    Map<String, Object> toolOutput = toolResult.get();
                    
                    assertNotNull(toolOutput);
                    assertTrue(toolOutput.containsKey("toolInstanceId"), "Should have tool instance ID");
                    
                    Integer toolInstanceId = (Integer) toolOutput.get("toolInstanceId");
                    uniqueToolInstanceIds.add(toolInstanceId);
                    
                    successCount.incrementAndGet();
                    
                } catch (Exception e) {
                    fail("ReactAgent simulation failed for call " + callId + ": " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await();
        executor.shutdown();
        assertEquals(30, successCount.get(), "All 30 calls should succeed");
        
        assertEquals(30, llmInstanceCreationCount.get(), "Should create 30 separate LLM instances");
        assertEquals(30, uniqueLlmInstanceIds.size(), "Should have 30 unique LLM instance IDs");
        assertEquals(30, uniqueLlmResponses.size(), "Should have 30 unique LLM responses");
        
        assertEquals(30, toolInstanceCreationCount.get(), "Should create 30 separate Tool instances");
        assertEquals(30, uniqueToolInstanceIds.size(), "Should have 30 unique Tool instance IDs");
        assertTrue(uniqueLlmInstanceIds.contains(1), "Should contain first LLM instance");
        assertTrue(uniqueLlmInstanceIds.contains(30), "Should contain last LLM instance");
        assertTrue(uniqueToolInstanceIds.contains(1), "Should contain first Tool instance");
        assertTrue(uniqueToolInstanceIds.contains(30), "Should contain last Tool instance");
        
    }

    @Test
    public void testCompiledGraphCallThreadSafety() throws Exception {
        AtomicInteger nodeExecutionCount = new AtomicInteger(0);
        AtomicInteger instanceCount = new AtomicInteger(0);
        
        Node.ActionFactory statefulNodeFactory = compileConfig -> {
            int currentInstanceId = instanceCount.incrementAndGet();
            
            AtomicReference<String> instanceState = new AtomicReference<>("initial_state_" + currentInstanceId);
            AtomicInteger instanceExecutionCount = new AtomicInteger(0);
            
            NodeAction statefulNode = state -> {
                int globalExecution = nodeExecutionCount.incrementAndGet();
                int localExecution = instanceExecutionCount.incrementAndGet();
                
                String inputMessage = (String) state.value("input").orElse("no input");
                String newState = "processed_" + inputMessage + "_instance_" + currentInstanceId + "_exec_" + localExecution;
                instanceState.set(newState);
                
                try {
                    Thread.sleep(5);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                
                return Map.of(
                    "output", "Result: " + newState,
                    "instanceId", currentInstanceId,
                    "localExecutionCount", localExecution,
                    "globalExecutionCount", globalExecution,
                    "instanceState", instanceState.get()
                );
            };
            
            return AsyncNodeActionWithConfig.node_async((state, config) -> {
                try {
                    return statefulNode.apply(state);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        };
        
        TestableCompiledGraph compiledGraph = new TestableCompiledGraph();
        compiledGraph.putNodeFactory("statefulNode", statefulNodeFactory);
        
        ExecutorService executor = Executors.newFixedThreadPool(15);
        CountDownLatch latch = new CountDownLatch(50);
        AtomicInteger successCount = new AtomicInteger(0);
        Set<Integer> uniqueInstanceIds = java.util.concurrent.ConcurrentHashMap.newKeySet();
        Set<String> uniqueOutputs = java.util.concurrent.ConcurrentHashMap.newKeySet();
        Map<Integer, Integer> instanceToExecutionCount = new java.util.concurrent.ConcurrentHashMap<>();
        
        for (int i = 0; i < 50; i++) {
            final int requestId = i;
            executor.submit(() -> {
                try {
                    AsyncNodeActionWithConfig nodeAction = compiledGraph.getNodeAction("statefulNode");
                    OverAllState state = new OverAllState(Map.of(
                        "input", "request_" + requestId
                    ));
                    RunnableConfig config = RunnableConfig.builder().build();
                    
                    CompletableFuture<Map<String, Object>> result = nodeAction.apply(state, config);
                    Map<String, Object> output = result.get();
                    
                    assertNotNull(output);
                    
                    Integer instanceId = (Integer) output.get("instanceId");
                    Integer localExecCount = (Integer) output.get("localExecutionCount");
                    String outputResult = (String) output.get("output");
                    
                    uniqueInstanceIds.add(instanceId);
                    uniqueOutputs.add(outputResult);
                    instanceToExecutionCount.put(instanceId, localExecCount);
                    
                    successCount.incrementAndGet();
                    
                } catch (Exception e) {
                    fail("CompiledGraph call failed for request " + requestId + ": " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await();
        executor.shutdown();
        
        assertEquals(50, successCount.get(), "All 50 calls should succeed");
        assertEquals(50, instanceCount.get(), "Should create 50 separate instances");
        assertEquals(50, uniqueInstanceIds.size(), "Should have 50 unique instance IDs");
        assertEquals(50, uniqueOutputs.size(), "Should have 50 unique outputs");
        assertEquals(50, nodeExecutionCount.get(), "Should have 50 total executions");
        
        for (Map.Entry<Integer, Integer> entry : instanceToExecutionCount.entrySet()) {
            assertEquals(1, entry.getValue().intValue(), 
                "Instance " + entry.getKey() + " should only execute once");
        }
    }

	@Test
	public void testGraphRunnerContextHonorsPrototypeScope() throws Exception {
        AtomicInteger factoryCallCount = new AtomicInteger(0);
        AtomicInteger instanceCreationCount = new AtomicInteger(0);
        
        Node.ActionFactory statefulNodeFactory = compileConfig -> {
            int factoryCall = factoryCallCount.incrementAndGet();
            int instanceId = instanceCreationCount.incrementAndGet();
            
            NodeAction statefulNode = state -> {
                return Map.of(
                    "factoryCallId", factoryCall,
                    "instanceId", instanceId,
                    "messages", "Response from instance " + instanceId,
                    "timestamp", System.currentTimeMillis()
                );
            };
            
            return AsyncNodeActionWithConfig.node_async((state, config) -> {
                try {
                    return statefulNode.apply(state);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        };
        
		TestableCompiledGraph compiledGraph = new TestableCompiledGraph();
		compiledGraph.putNodeFactory("llmNode", statefulNodeFactory, NodeScope.PROTOTYPE);
        
        OverAllState initialState = new OverAllState(new HashMap<>());
        RunnableConfig config = RunnableConfig.builder().build();
        GraphRunnerContext context = new GraphRunnerContext(initialState, config, compiledGraph);
        
		AsyncNodeActionWithConfig action1 = context.getNodeAction("llmNode");
		assertNotNull(action1, "First action should not be null");
		assertEquals(1, factoryCallCount.get(), "Factory should be called once");
		assertEquals(1, instanceCreationCount.get(), "Should create one instance");

		AsyncNodeActionWithConfig action2 = context.getNodeAction("llmNode");
		assertNotNull(action2, "Second action should not be null");
		assertEquals(2, factoryCallCount.get(), "Factory should be called twice for prototype scope");
		assertEquals(2, instanceCreationCount.get(), "Prototype scope should create two instances");
		assertNotSame(action1, action2, "Prototype scope must return different instances");
        
        
        Map<String, Object> input = Map.of("input", "test message");
        OverAllState state1 = new OverAllState(input);
        OverAllState state2 = new OverAllState(input);
        
        CompletableFuture<Map<String, Object>> result1 = action1.apply(state1, config);
        CompletableFuture<Map<String, Object>> result2 = action2.apply(state2, config);
        
        Map<String, Object> output1 = result1.get();
        Map<String, Object> output2 = result2.get();
        
		assertNotEquals(output1.get("instanceId"), output2.get("instanceId"),
			"Different instanceId proves prototype scope isolation");
    }

    @Test
    public void testCorrectSolutionWithoutRuntimeCache() throws Exception {
        AtomicInteger factoryCallCount = new AtomicInteger(0);
        AtomicInteger instanceCreationCount = new AtomicInteger(0);
        
        Node.ActionFactory statefulNodeFactory = compileConfig -> {
            int factoryCall = factoryCallCount.incrementAndGet();
            int instanceId = instanceCreationCount.incrementAndGet();
            
            NodeAction statefulNode = state -> {
                return Map.of(
                    "factoryCallId", factoryCall,
                    "instanceId", instanceId,
                    "messages", "Response from instance " + instanceId,
                    "timestamp", System.currentTimeMillis()
                );
            };
            
            return AsyncNodeActionWithConfig.node_async((state, config) -> {
                try {
                    return statefulNode.apply(state);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        };
        
        TestableCompiledGraph compiledGraph = new TestableCompiledGraph();
        compiledGraph.putNodeFactory("llmNode", statefulNodeFactory);
        
        AsyncNodeActionWithConfig action1 = compiledGraph.getNodeAction("llmNode");
        assertNotNull(action1, "First action should not be null");
        assertEquals(1, factoryCallCount.get(), "Factory should be called once");
        
        AsyncNodeActionWithConfig action2 = compiledGraph.getNodeAction("llmNode");
        assertNotNull(action2, "Second action should not be null");
        assertEquals(2, factoryCallCount.get(), "Factory should be called twice");
        
        assertNotSame(action1, action2, "Should return different instances each time");
        
        Map<String, Object> input = Map.of("input", "test message");
        OverAllState state1 = new OverAllState(input);
        OverAllState state2 = new OverAllState(input);
        RunnableConfig config = RunnableConfig.builder().build();
        
        CompletableFuture<Map<String, Object>> result1 = action1.apply(state1, config);
        CompletableFuture<Map<String, Object>> result2 = action2.apply(state2, config);
        
        Map<String, Object> output1 = result1.get();
        Map<String, Object> output2 = result2.get();
        
        assertNotEquals(output1.get("instanceId"), output2.get("instanceId"), 
            "Different instanceIds prove they are different instances - solving thread safety");
        
    }

    private static class TestableCompiledGraph extends CompiledGraph {

        public TestableCompiledGraph() throws GraphStateException {
            super(createMinimalStateGraph(), CompileConfig.builder().build());
        }

        public void putNodeFactory(String nodeId, Node.ActionFactory factory) {
			putNodeFactory(nodeId, factory, NodeScope.SINGLETON_PER_REQUEST);
		}

		public void putNodeFactory(String nodeId, Node.ActionFactory factory, NodeScope scope) {
			this.nodeFactories.put(nodeId, factory);
			this.nodeScopes.put(nodeId, scope);
        }

        private static StateGraph createMinimalStateGraph() throws GraphStateException {
            return new StateGraph();
        }
    }
}
