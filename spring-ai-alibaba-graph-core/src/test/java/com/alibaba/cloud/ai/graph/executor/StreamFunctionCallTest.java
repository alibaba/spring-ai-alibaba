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

package com.alibaba.cloud.ai.graph.executor;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.state.strategy.AppendStrategy;
import com.alibaba.cloud.ai.graph.stream.LLmNodeAction;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.metadata.ChatGenerationMetadata;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Issue #4406 reproduction test:
 * stream tool call chunks should be merged into one complete tool call.
 */
public class StreamFunctionCallTest {

    @Test
    void testStreamingToolCallFragmentsShouldMergeIntoSingleToolCall() throws Exception {
        ChatModel chatModel = createMockChatModelWithSplitToolCallChunks();

        StateGraph stateGraph = new StateGraph(() -> {
            Map<String, com.alibaba.cloud.ai.graph.KeyStrategy> keyStrategyMap = new HashMap<>();
            keyStrategyMap.put("messages", new AppendStrategy());
            return keyStrategyMap;
        }).addNode("llmNode", node_async(new LLmNodeAction(chatModel, "llmNode")))
                .addEdge(START, "llmNode")
                .addEdge("llmNode", END);

        CompiledGraph compiledGraph = stateGraph.compile();

        Map<String, Object> input = new HashMap<>();
        input.put(OverAllState.DEFAULT_INPUT_KEY, "weather in beijing");

        Optional<OverAllState> finalState = compiledGraph.invoke(input);
        assertTrue(finalState.isPresent());

        List<?> messages = finalState.get().value("messages", List.class).orElseThrow();
        assertNotNull(messages);
        assertTrue(!messages.isEmpty());

        Object lastMessage = messages.get(messages.size() - 1);
        assertInstanceOf(AssistantMessage.class, lastMessage);

        AssistantMessage assistantMessage = (AssistantMessage) lastMessage;
        assertTrue(assistantMessage.hasToolCalls());
        assertEquals(1, assistantMessage.getToolCalls().size());

        AssistantMessage.ToolCall toolCall = assistantMessage.getToolCalls().get(0);
        assertEquals("call_1", toolCall.id());
        assertEquals("get_weather", toolCall.name());
        assertEquals("{\"city\":\"Beijing\"}", toolCall.arguments());
    }

    private static ChatModel createMockChatModelWithSplitToolCallChunks() {
        return new ChatModel() {
            @Override
            public ChatResponse call(Prompt prompt) {
                return new ChatResponse(List.of(new Generation(new AssistantMessage("fallback"))));
            }

            @Override
            public Flux<ChatResponse> stream(Prompt prompt) {
                AssistantMessage.ToolCall chunk1 = new AssistantMessage.ToolCall("call_1", "function", "get_weather", "");
                AssistantMessage.ToolCall chunk2 = new AssistantMessage.ToolCall(null, "function", null, "{\"city\":\"Bei");
                AssistantMessage.ToolCall chunk3 = new AssistantMessage.ToolCall(null, "function", null, "jing\"}");

                return Flux.just(
                        new ChatResponse(List.of(new Generation(
                                AssistantMessage.builder().content("").toolCalls(List.of(chunk1)).build(),
                                ChatGenerationMetadata.NULL))),
                        new ChatResponse(List.of(new Generation(
                                AssistantMessage.builder().content("").toolCalls(List.of(chunk2)).build(),
                                ChatGenerationMetadata.NULL))),
                        new ChatResponse(List.of(new Generation(
                                AssistantMessage.builder().content("").toolCalls(List.of(chunk3)).build(),
                                ChatGenerationMetadata.NULL)))
                );
            }
        };
    }

}
