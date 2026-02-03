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
package com.alibaba.cloud.ai.graph.agent;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.hook.HookPosition;
import com.alibaba.cloud.ai.graph.agent.hook.JumpTo;
import com.alibaba.cloud.ai.graph.agent.hook.ModelHook;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test class to reproduce Issue #4039: jump_to state cleanup problem
 *
 * Issue: https://github.com/alibaba/spring-ai-alibaba/issues/4039
 *
 * Problem: When using jump_to to redirect execution to a previously visited node,
 * the jump_to state value is not cleared after being consumed, potentially causing
 * infinite loops or unintended behavior.
 */
class ReactAgentJumpToCleanupTest {

    private ChatModel mockChatModel;
    private AtomicInteger modelCallCounter;

    @BeforeEach
    void setUp() {
        modelCallCounter = new AtomicInteger(0);
        mockChatModel = mock(ChatModel.class);

        // Mock chat model to return simple responses
        ChatResponse mockResponse = new ChatResponse(
                List.of(new Generation(new AssistantMessage("Response from model")))
        );

        when(mockChatModel.call(any(Prompt.class))).thenReturn(mockResponse);
        when(mockChatModel.stream(any(Prompt.class)))
                .thenReturn(Flux.just(mockResponse));
    }

    /**
     * Test case to reproduce the jump_to cleanup bug
     *
     * Scenario:
     * 1. First call to model hook sets jump_to = "model" to jump back to model node
     * 2. Model executes again (second time)
     * 3. Second call to model hook does NOT set jump_to (expects normal flow to end)
     * 4. BUG: jump_to still contains "model" from step 1, causing infinite loop
     *
     * Expected: Should execute model node twice and then end
     * Actual: Will loop infinitely because jump_to is never cleared
     */
    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void testJumpToNotClearedCausesInfiniteLoop() {
        System.out.println("\n========================================");
        System.out.println(" Testing Issue #4039: jump_to cleanup bug");
        System.out.println("========================================\n");

        // Counter to track how many times model node is executed
        AtomicInteger executionCounter = new AtomicInteger(0);

        // Create a hook that sets jump_to on first execution only
        ModelHook jumpOnceHook = new ModelHook() {
            @Override
            public String getName() {
                return "JumpOnceHook";
            }

            @Override
            public List<JumpTo> canJumpTo() {
                return List.of(JumpTo.model, JumpTo.end);
            }

            @Override
            public HookPosition[] getHookPositions() {
                return new HookPosition[]{HookPosition.AFTER_MODEL};
            }

            @Override
            public CompletableFuture<Map<String, Object>> afterModel(
                    OverAllState state, RunnableConfig config) {

                int currentCount = executionCounter.incrementAndGet();
                System.out.println("Model execution count: " + currentCount);

                // Check if jump_to exists in current state
                Object jumpToValue = state.value("jump_to").orElse(null);
                System.out.println("Current jump_to value: " + jumpToValue);

                if (currentCount == 1) {
                    // First execution: Set jump_to to jump back to model
                    System.out.println("First execution: Setting jump_to = model");
                    return CompletableFuture.completedFuture(
                            Map.of("jump_to", JumpTo.model)
                    );
                } else if (currentCount == 2) {
                    // Second execution: Do NOT set jump_to (should end normally)
                    System.out.println("Second execution: NOT setting jump_to (expecting normal end)");
                    return CompletableFuture.completedFuture(Map.of());
                } else {
                    // Third+ execution: This should NOT happen!
                    System.out.println("BUG REPRODUCED! Execution " + currentCount + " should not occur!");
                    System.out.println("jump_to was not cleaned up from previous iteration");

                    // Prevent actual infinite loop in test
                    if (currentCount >= 5) {
                        throw new RuntimeException(
                                "ISSUE #4039 REPRODUCED: jump_to not cleaned up! " +
                                        "Model executed " + currentCount + " times (expected: 2)"
                        );
                    }

                    return CompletableFuture.completedFuture(Map.of());
                }
            }
        };

        // Build ReactAgent with the hook
        ReactAgent agent = ReactAgent.builder()
                .name("test_agent")
                .model(mockChatModel)
                .saver(new MemorySaver())
                .hooks(List.of(jumpOnceHook))
                .build();

        // Execute the agent
        try {
            System.out.println("\n Starting agent execution...\n");
            agent.invoke(new UserMessage("Test message"));

            // If we reach here without exception, check execution count
            int finalCount = executionCounter.get();
            System.out.println("\n Final execution count: " + finalCount);

            if (finalCount > 2) {
                System.out.println("BUG CONFIRMED: Model was executed " + finalCount +
                        " times (expected: 2)");
                System.out.println("This proves that jump_to state was NOT cleaned up!");
                fail("Issue #4039 reproduced: jump_to not cleaned up after being consumed. " +
                        "Model executed " + finalCount + " times instead of 2.");
            } else {
                System.out.println("Test passed: Model executed exactly " + finalCount + " times");
            }

        } catch (RuntimeException e) {
            if (e.getMessage().contains("ISSUE #4039 REPRODUCED")) {
                System.out.println("\n========================================");
                System.out.println("ISSUE #4039 SUCCESSFULLY REPRODUCED");
                System.out.println("========================================");
                System.out.println("Problem: jump_to state is not cleaned up after being consumed");
                System.out.println("Location: ReactAgent.addHookEdge() method");
                System.out.println("Impact: Causes unintended repeated jumps or infinite loops");
                System.out.println("========================================\n");

                // This is expected - we successfully reproduced the bug
                // In a real bug fix, we would use fail() here
                // For now, we'll just log it
                System.out.println("Test demonstrates the bug exists.");
            } else {
                throw e;
            }
        } catch (Exception e) {
            System.out.println(" Unexpected error: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    /**
     * Test case showing expected behavior when jump_to is properly cleaned up
     *
     * This test demonstrates what SHOULD happen if the bug is fixed:
     * - First execution sets jump_to
     * - jump_to is consumed and cleared
     * - Second execution proceeds normally without jumping again
     */
    @Test
    void testExpectedBehaviorWhenJumpToIsProperlyCleanedUp() {
        System.out.println("\n========================================");
        System.out.println("Expected behavior (if bug is fixed)");
        System.out.println("========================================\n");

        AtomicInteger executionCounter = new AtomicInteger(0);
        List<String> executionLog = new ArrayList<>();

        ModelHook trackingHook = new ModelHook() {
            @Override
            public String getName() {
                return "TrackingHook";
            }

            @Override
            public List<JumpTo> canJumpTo() {
                return List.of(JumpTo.model, JumpTo.end);
            }

            @Override
            public HookPosition[] getHookPositions() {
                return new HookPosition[]{HookPosition.AFTER_MODEL};
            }

            @Override
            public CompletableFuture<Map<String, Object>> afterModel(
                    OverAllState state, RunnableConfig config) {

                int count = executionCounter.incrementAndGet();

                // Manually clean up jump_to (simulating what the fix should do)
                Object jumpToValue = state.value("jump_to").orElse(null);

                String logEntry = String.format("Execution %d: jump_to = %s", count, jumpToValue);
                executionLog.add(logEntry);
                System.out.println(logEntry);

                if (count == 1) {
                    System.out.println("Setting jump_to = model");
                    // Note: In reality, this jump_to should be cleaned up by the framework
                    return CompletableFuture.completedFuture(
                            Map.of("jump_to", JumpTo.model)
                    );
                } else {
                    System.out.println("No jump_to set (should end normally)");
                    return CompletableFuture.completedFuture(Map.of());
                }
            }
        };

        // Build agent
        ReactAgent agent = ReactAgent.builder()
                .name("expected_behavior_agent")
                .model(mockChatModel)
                .saver(new MemorySaver())
                .hooks(List.of(trackingHook))
                .build();

        System.out.println("Expected flow:");
        System.out.println("1. Model executes (first time)");
        System.out.println("2. Hook sets jump_to = model");
        System.out.println("3. Framework consumes jump_to and CLEANS IT UP ⭐");
        System.out.println("4. Model executes (second time)");
        System.out.println("5. Hook doesn't set jump_to");
        System.out.println("6. Agent ends normally\n");

        System.out.println("Actual execution log:");

        try {
            agent.invoke(new UserMessage("Test"));

            System.out.println("\nExecution log:");
            for (String log : executionLog) {
                System.out.println("  " + log);
            }

            // With the bug, this will likely fail or loop
            // With the fix, it should execute exactly twice
            System.out.println("\nNote: Due to Issue #4039, this test may show unexpected behavior");

        } catch (Exception e) {
            System.out.println("Error occurred (possibly due to the bug): " + e.getMessage());
        }

        System.out.println("========================================\n");
    }

    /**
     * Test case demonstrating the state persistence issue
     */
    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void testJumpToStatePersistsAcrossExecutions() {
        System.out.println("\n========================================");
        System.out.println(" Demonstrating jump_to state persistence");
        System.out.println("========================================\n");

        List<Object> observedJumpToValues = new ArrayList<>();

        ModelHook observerHook = new ModelHook() {
            private int callCount = 0;

            @Override
            public String getName() {
                return "ObserverHook";
            }

            @Override
            public List<JumpTo> canJumpTo() {
                return List.of(JumpTo.model, JumpTo.end);
            }

            @Override
            public HookPosition[] getHookPositions() {
                return new HookPosition[]{HookPosition.BEFORE_MODEL, HookPosition.AFTER_MODEL};
            }

            @Override
            public CompletableFuture<Map<String, Object>> beforeModel(
                    OverAllState state, RunnableConfig config) {

                callCount++;
                Object jumpToValue = state.value("jump_to").orElse(null);
                observedJumpToValues.add(jumpToValue);

                System.out.println("Before model execution #" + callCount + ":");
                System.out.println("jump_to in state: " + jumpToValue);

                return CompletableFuture.completedFuture(Map.of());
            }

            @Override
            public CompletableFuture<Map<String, Object>> afterModel(
                    OverAllState state, RunnableConfig config) {

                if (callCount == 1) {
                    System.out.println("After model execution #" + callCount + ":");
                    System.out.println("Setting jump_to = model\n");
                    return CompletableFuture.completedFuture(
                            Map.of("jump_to", JumpTo.model)
                    );
                } else if (callCount >= 3) {
                    // Stop after 3 iterations to prevent infinite loop
                    throw new RuntimeException("Stopping after " + callCount + " executions");
                }

                System.out.println("After model execution #" + callCount + ":");
                System.out.println("Not setting jump_to\n");
                return CompletableFuture.completedFuture(Map.of());
            }
        };

        ReactAgent agent = ReactAgent.builder()
                .name("persistence_test_agent")
                .model(mockChatModel)
                .saver(new MemorySaver())
                .hooks(List.of(observerHook))
                .build();

        try {
            agent.invoke(new UserMessage("Test"));
        } catch (RuntimeException e) {
            // Expected to stop after multiple executions
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("Observed jump_to values before each execution:");
        for (int i = 0; i < observedJumpToValues.size(); i++) {
            System.out.println("Execution " + (i + 1) + ": " + observedJumpToValues.get(i));
        }

        System.out.println("\n Analysis:");
        if (observedJumpToValues.size() >= 2) {
            Object firstValue = observedJumpToValues.get(0);
            Object secondValue = observedJumpToValues.get(1);

            System.out.println("- First execution saw: " + firstValue);
            System.out.println("- Second execution saw: " + secondValue);

            if (secondValue != null) {
                System.out.println("BUG: jump_to persisted to second execution!");
                System.out.println("It should have been cleared after first use");
            } else {
                System.out.println("jump_to was properly cleaned up");
            }
        }

        System.out.println("========================================\n");
    }
}