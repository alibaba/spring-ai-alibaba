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
package com.alibaba.cloud.ai.graph.executor;

import com.alibaba.cloud.ai.graph.*;
import com.alibaba.cloud.ai.graph.state.strategy.AppendStrategy;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.AssistantMessage.ToolCall;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 *
 * test for NodeExecutor tool call lost Bugfix
 */
public class NodeExecutorToolCallTest {


    private ChatResponse createChatResponseWithText(String text) {
        AssistantMessage message = new AssistantMessage(text);
        Generation generation = new Generation(message);
        return new ChatResponse(List.of(generation));
    }


    private ChatResponse createChatResponseWithToolCalls(String text, List<ToolCall> toolCalls) {
        AssistantMessage message = new AssistantMessage(text, Map.of(), toolCalls);
        Generation generation = new Generation(message);
        return new ChatResponse(List.of(generation));
    }


    private ChatResponse createChatResponseWithOnlyToolCalls(List<ToolCall> toolCalls) {
        AssistantMessage message = new AssistantMessage("", Map.of(), toolCalls);
        Generation generation = new Generation(message);
        return new ChatResponse(List.of(generation));
    }


    @Test
    public void testToolCallsNotLostInStreamingResponse() throws Exception {
        ToolCall toolCall1 = new ToolCall("call_123", "function", "get_weather", "{\"location\":\"Beijing\"}");
        List<ToolCall> toolCalls = List.of(toolCall1);

        Flux<ChatResponse> chatResponseFlux = Flux.just(
                createChatResponseWithText("Let me "),
                createChatResponseWithText("check the "),
                createChatResponseWithText("weather "),
                createChatResponseWithToolCalls("for you.", toolCalls)
        );

        StateGraph stateGraph = new StateGraph(() -> {
            Map<String, KeyStrategy> keyStrategyMap = new HashMap<>();
            keyStrategyMap.put("messages", new AppendStrategy());
            return keyStrategyMap;
        })
                .addNode("llmNode", node_async(state -> {
                    // 返回流式 ChatResponse
                    return Map.of("messages", chatResponseFlux);
                }))
                .addEdge(START, "llmNode")
                .addEdge("llmNode", END);

        CompiledGraph app = stateGraph.compile();

        CountDownLatch latch = new CountDownLatch(1);

        app.stream(Map.of("input", "What's the weather in Beijing?"))
                .subscribe(
                        output -> {
                            System.out.println("Stream output: " + output);
                        },
                        error -> {
                            System.err.println("Stream error: " + error);
                            latch.countDown();
                        },
                        () -> {
                            System.out.println("Stream completed");
                            latch.countDown();
                        }
                );

        assertTrue(latch.await(10, TimeUnit.SECONDS), "should ok");

        Optional<OverAllState> result = app.invoke(Map.of("input", "What's the weather in Beijing?"));

        assertTrue(result.isPresent(), "result should exist");

        OverAllState finalState = result.get();
        Optional<Object> messagesOpt = finalState.value("messages");

        assertTrue(messagesOpt.isPresent(), "result should contain messages");

        Object messagesObj = messagesOpt.get();
        assertNotNull(messagesObj, "messages should not be  null");

        if (messagesObj instanceof AssistantMessage) {
            AssistantMessage finalMessage = (AssistantMessage) messagesObj;

            String expectedText = "Let me check the weather for you.";
            assertEquals(expectedText, finalMessage.getText(), "text should be merged");

            assertTrue(finalMessage.hasToolCalls(), "message sould contain toolCalls");
            List<ToolCall> finalToolCalls = finalMessage.getToolCalls();
            assertNotNull(finalToolCalls, "toolCalls should not be null");
            assertEquals(1, finalToolCalls.size(), "should have 1  toolCall");

            ToolCall finalToolCall = finalToolCalls.get(0);
            assertEquals("call_123", finalToolCall.id(), "toolCall id correct");
            assertEquals("function", finalToolCall.type(), "toolCall type correct");
            assertEquals("get_weather", finalToolCall.name(), "toolCall name correct");
            assertEquals("{\"location\":\"Beijing\"}", finalToolCall.arguments(), "toolCall arguments correct");

        }
    }


    @Test
    public void testToolCallsUpdatedByLatestResponse() throws Exception {
        ToolCall toolCall1 = new ToolCall("call_123", "function", "get_weather", "{\"location\":\"Beijing\"}");
        ToolCall toolCall2 = new ToolCall("call_456", "function", "get_time", "{\"timezone\":\"Asia/Shanghai\"}");

        Flux<ChatResponse> chatResponseFlux = Flux.just(
                createChatResponseWithText("Let me "),
                createChatResponseWithToolCalls("check ", List.of(toolCall1)),
                createChatResponseWithText("the time "),
                createChatResponseWithToolCalls("for you.", List.of(toolCall2))
        );

        StateGraph stateGraph = new StateGraph(() -> {
            Map<String, KeyStrategy> keyStrategyMap = new HashMap<>();
            keyStrategyMap.put("messages", new AppendStrategy());
            return keyStrategyMap;
        })
                .addNode("llmNode", node_async(state -> {
                    return Map.of("messages", chatResponseFlux);
                }))
                .addEdge(START, "llmNode")
                .addEdge("llmNode", END);

        CompiledGraph app = stateGraph.compile();

        Optional<OverAllState> result = app.invoke(Map.of("input", "test"));
        assertTrue(result.isPresent(), "result should exist");

        Optional<Object> messagesOpt = result.get().value("messages");
        assertTrue(messagesOpt.isPresent(), "messages should exist");

        Object messagesObj = messagesOpt.get();
        if (messagesObj instanceof AssistantMessage) {
            AssistantMessage finalMessage = (AssistantMessage) messagesObj;

            assertTrue(finalMessage.hasToolCalls(), "message should contain toolCalls");
            List<ToolCall> finalToolCalls = finalMessage.getToolCalls();
            assertEquals(1, finalToolCalls.size(), "should have 1 toolCall");

            ToolCall finalToolCall = finalToolCalls.get(0);
            assertEquals("call_456", finalToolCall.id(), "should be new  toolCall");
            assertEquals("get_time", finalToolCall.name(), "should be new toolCall name");

        }
    }


    @Test
    public void testStreamingResponseWithoutToolCalls() throws Exception {
        Flux<ChatResponse> chatResponseFlux = Flux.just(
                createChatResponseWithText("This "),
                createChatResponseWithText("is "),
                createChatResponseWithText("a "),
                createChatResponseWithText("test.")
        );

        StateGraph stateGraph = new StateGraph(() -> {
            Map<String, KeyStrategy> keyStrategyMap = new HashMap<>();
            keyStrategyMap.put("messages", new AppendStrategy());
            return keyStrategyMap;
        })
                .addNode("llmNode", node_async(state -> {
                    return Map.of("messages", chatResponseFlux);
                }))
                .addEdge(START, "llmNode")
                .addEdge("llmNode", END);

        CompiledGraph app = stateGraph.compile();

        Optional<OverAllState> result = app.invoke(Map.of("input", "test"));
        assertTrue(result.isPresent(), "result should exisit");

        Optional<Object> messagesOpt = result.get().value("messages");
        assertTrue(messagesOpt.isPresent(), "messages should exisit");

        Object messagesObj = messagesOpt.get();
        if (messagesObj instanceof AssistantMessage) {
            AssistantMessage finalMessage = (AssistantMessage) messagesObj;

            assertEquals("This is a test.", finalMessage.getText(), "ok");

            assertFalse(finalMessage.hasToolCalls(), "should not have toolCalls");

        }
    }


    @Test
    public void testToolCallsArriveAfterTextChunks() throws Exception {
        ToolCall toolCall = new ToolCall("call_789", "function", "calculate", "{\"expr\":\"1+1\"}");

        Flux<ChatResponse> chatResponseFlux = Flux.just(
                createChatResponseWithText("Let me "),
                createChatResponseWithText("calculate "),
                createChatResponseWithText("that "),
                createChatResponseWithOnlyToolCalls(List.of(toolCall))
        );

        StateGraph stateGraph = new StateGraph(() -> {
            Map<String, KeyStrategy> keyStrategyMap = new HashMap<>();
            keyStrategyMap.put("messages", new AppendStrategy());
            return keyStrategyMap;
        })
                .addNode("llmNode", node_async(state -> {
                    return Map.of("messages", chatResponseFlux);
                }))
                .addEdge(START, "llmNode")
                .addEdge("llmNode", END);

        CompiledGraph app = stateGraph.compile();

        Optional<OverAllState> result = app.invoke(Map.of("input", "test"));
        assertTrue(result.isPresent(), "should exsist");

        Optional<Object> messagesOpt = result.get().value("messages");
        assertTrue(messagesOpt.isPresent(), "messages should exsist");

        Object messagesObj = messagesOpt.get();
        if (messagesObj instanceof AssistantMessage) {
            AssistantMessage finalMessage = (AssistantMessage) messagesObj;

            assertEquals("Let me calculate that ", finalMessage.getText(), "merge ok");
            assertTrue(finalMessage.hasToolCalls(), "should contain toolCalls");

            List<ToolCall> finalToolCalls = finalMessage.getToolCalls();
            assertEquals(1, finalToolCalls.size(), "should 1 toolCall");
            assertEquals("call_789", finalToolCalls.get(0).id(), "toolCall ok");

        }
    }


    @Test
    public void testEmptyTextWithToolCalls() throws Exception {
        ToolCall toolCall = new ToolCall("call_empty", "function", "no_text_func", "{}");

        // 只有 toolCalls，没有文本
        Flux<ChatResponse> chatResponseFlux = Flux.just(
                createChatResponseWithOnlyToolCalls(List.of(toolCall))
        );

        StateGraph stateGraph = new StateGraph(() -> {
            Map<String, KeyStrategy> keyStrategyMap = new HashMap<>();
            keyStrategyMap.put("messages", new AppendStrategy());
            return keyStrategyMap;
        })
                .addNode("llmNode", node_async(state -> {
                    return Map.of("messages", chatResponseFlux);
                }))
                .addEdge(START, "llmNode")
                .addEdge("llmNode", END);

        CompiledGraph app = stateGraph.compile();

        Optional<OverAllState> result = app.invoke(Map.of("input", "test"));
        assertTrue(result.isPresent(), "result ok");

        Optional<Object> messagesOpt = result.get().value("messages");
        assertTrue(messagesOpt.isPresent(), "messages ok");

        Object messagesObj = messagesOpt.get();
        if (messagesObj instanceof AssistantMessage) {
            AssistantMessage finalMessage = (AssistantMessage) messagesObj;

            assertTrue(finalMessage.hasToolCalls(), "should contain toolCalls");
            assertEquals("call_empty", finalMessage.getToolCalls().get(0).id(), "toolCall ok");

        }
    }
}

