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
package com.alibaba.cloud.ai.graph.agent;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.agent.flow.agent.LlmRoutingAgent;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import com.alibaba.cloud.ai.graph.serializer.AgentInstructionMessage;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.content.Media;
import org.springframework.core.io.ByteArrayResource;

import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression tests for the fallback behavior of {@link LlmRoutingAgent}.
 *
 * <p>The tests use deterministic in-memory {@link ChatModel} implementations instead
 * of a remote model. This allows the routing model to reliably return invalid decisions
 * or throw failures, while still exercising the production ChatClient, structured-output,
 * retry, graph-routing, sub-agent, and result-merge paths.</p>
 */
class LlmRoutingAgentFallbackTest {

    /**
     * An invalid agent name should exhaust the routing retries and then execute the
     * configured fallback sub-agent.
     */
    @Test
    void invalidRoutingDecisionUsesConfiguredFallbackAgent() throws GraphRunnerException {
        CountingChatModel routingModel = new CountingChatModel(invalidRoutingDecision());
        CountingChatModel fallbackModel = new CountingChatModel("FALLBACK_EXECUTED");
        ReactAgent fallbackAgent = agent("fallback_agent", "fallback_output", fallbackModel);

        LlmRoutingAgent routingAgent = LlmRoutingAgent.builder()
                .name("router")
                .description("Routes requests")
                .model(routingModel)
                .subAgents(List.of(fallbackAgent))
                .fallbackAgent(fallbackAgent.name())
                .build();

        Optional<OverAllState> result = routingAgent.invoke("route this request");

        assertTrue(result.isPresent());
        assertEquals(3, routingModel.callCount(), "Routing should use the initial attempt and two retries");
        assertEquals(1, fallbackModel.callCount(), "Configured fallback agent should execute once");
        AssistantMessage fallbackOutput = result.get().value("fallback_output", AssistantMessage.class).orElseThrow();
        assertEquals("FALLBACK_EXECUTED", fallbackOutput.getText());
    }

    /**
     * Agent API invocations populate the graph input from the latest user message. The
     * fallback should expose that original text through the documented
     * {@code {agentName_input}} placeholder when no targeted routing query is available.
     */
    @Test
    void fallbackAgentReceivesOriginalInputForAgentSpecificPlaceholder() throws GraphRunnerException {
        CapturingChatModel fallbackModel = new CapturingChatModel("FALLBACK_EXECUTED");
        LlmRoutingAgent routingAgent = routingAgentWithFallback(fallbackModel);

        Optional<OverAllState> result = routingAgent.invoke("the original user request");

        assertTrue(result.isPresent());
        assertInstructionEquals(fallbackModel,
                "Handle this original request: the original user request");
    }

    /**
     * An explicitly supplied graph input is authoritative when it differs from the
     * conversation history.
     */
    @Test
    void explicitInputTakesPrecedenceOverLatestUserMessage() throws GraphRunnerException {
        CapturingChatModel fallbackModel = new CapturingChatModel("FALLBACK_EXECUTED");
        LlmRoutingAgent routingAgent = routingAgentWithFallback(fallbackModel);

        Optional<OverAllState> result = routingAgent.invoke(Map.of(
                OverAllState.DEFAULT_INPUT_KEY, "explicit fallback request",
                "messages", List.of(new UserMessage("message history request"))));

        assertTrue(result.isPresent());
        assertInstructionEquals(fallbackModel,
                "Handle this original request: explicit fallback request");
    }

    /**
     * Direct graph-style invocations may provide messages without the standard input key.
     * In that case, the latest user message text supplies the fallback placeholder.
     */
    @Test
    void messagesOnlyInvocationUsesLatestUserMessageText() throws GraphRunnerException {
        CapturingChatModel fallbackModel = new CapturingChatModel("FALLBACK_EXECUTED");
        LlmRoutingAgent routingAgent = routingAgentWithFallback(fallbackModel);

        Optional<OverAllState> result = routingAgent.invoke(Map.of("messages", List.of(
                new UserMessage("older request"),
                new AssistantMessage("older response"),
                new UserMessage("latest request"))));

        assertTrue(result.isPresent());
        assertInstructionEquals(fallbackModel, "Handle this original request: latest request");
    }

    /**
     * Empty text on the latest user message can represent a media-only request. It must
     * remain empty rather than falling back to stale text from an earlier user turn. The
     * fallback still receives the media separately through the propagated messages.
     */
    @Test
    void mediaOnlyLatestUserMessageDoesNotReuseOlderText() throws GraphRunnerException {
        Media image = Media.builder()
                .mimeType(Media.Format.IMAGE_PNG)
                .data(new ByteArrayResource(new byte[] { 1, 2, 3 }))
                .build();
        UserMessage latestMessage = UserMessage.builder().text("").media(image).build();
        CapturingChatModel fallbackModel = new CapturingChatModel("FALLBACK_EXECUTED");
        LlmRoutingAgent routingAgent = routingAgentWithFallback(fallbackModel, true);

        Optional<OverAllState> result = routingAgent.invoke(Map.of("messages", List.of(
                new UserMessage("older request"),
                new AssistantMessage("older response"),
                latestMessage)));

        assertTrue(result.isPresent());
        assertInstructionEquals(fallbackModel, "Handle this original request: ");
        assertTrue(fallbackModel.lastPromptContainsMedia(image));
    }

    /**
     * If neither graph input nor a user message exists, the fallback placeholder resolves
     * to an empty string so prompt rendering can continue.
     */
    @Test
    void missingInputAndUserMessageUsesEmptyString() throws GraphRunnerException {
        CapturingChatModel fallbackModel = new CapturingChatModel("FALLBACK_EXECUTED");
        LlmRoutingAgent routingAgent = routingAgentWithFallback(fallbackModel);

        Optional<OverAllState> result = routingAgent.invoke(Map.of(
                "messages", List.of(new AssistantMessage("assistant-only history"))));

        assertTrue(result.isPresent());
        assertInstructionEquals(fallbackModel, "Handle this original request: ");
    }

    /**
     * A routing-model exception should follow the same retry policy and use the fallback
     * only after the final attempt fails.
     */
    @Test
    void routingModelFailureUsesConfiguredFallbackAgent() throws GraphRunnerException {
        CountingChatModel routingModel = CountingChatModel.failing(new IllegalStateException("routing unavailable"));
        CountingChatModel fallbackModel = new CountingChatModel("FALLBACK_EXECUTED");
        ReactAgent fallbackAgent = agent("fallback_agent", "fallback_output", fallbackModel);

        LlmRoutingAgent routingAgent = LlmRoutingAgent.builder()
                .name("router")
                .description("Routes requests")
                .model(routingModel)
                .subAgents(List.of(fallbackAgent))
                .fallbackAgent(fallbackAgent.name())
                .build();

        Optional<OverAllState> result = routingAgent.invoke("route this request");

        assertTrue(result.isPresent());
        assertEquals(3, routingModel.callCount(), "Routing model failure should be retried twice before fallback");
        assertEquals(1, fallbackModel.callCount());
    }

    /**
     * Without a fallback configuration, retry exhaustion must preserve the original
     * failure behavior and must not execute an arbitrary sub-agent.
     */
    @Test
    void invalidRoutingDecisionWithoutFallbackStillThrows() {
        CountingChatModel routingModel = new CountingChatModel(invalidRoutingDecision());
        CountingChatModel subAgentModel = new CountingChatModel("SHOULD_NOT_EXECUTE");
        ReactAgent subAgent = agent("known_agent", "known_output", subAgentModel);

        LlmRoutingAgent routingAgent = LlmRoutingAgent.builder()
                .name("router")
                .description("Routes requests")
                .model(routingModel)
                .subAgents(List.of(subAgent))
                .build();

        Exception exception = assertThrows(Exception.class, () -> routingAgent.invoke("route this request"));

        assertTrue(hasMessage(exception, "Failed to get valid decision"));
        assertEquals(3, routingModel.callCount());
        assertEquals(0, subAgentModel.callCount());
    }

    /**
     * Fallback is optional. Omitting it must not fail graph construction or introduce a
     * null-value error in the routing graph configuration.
     */
    @Test
    void fallbackAgentRemainsOptionalWhenBuildingGraph() {
        CountingChatModel routingModel = new CountingChatModel(validRoutingDecision("known_agent"));
        ReactAgent subAgent = agent("known_agent", "known_output", new CountingChatModel("DONE"));

        LlmRoutingAgent routingAgent = LlmRoutingAgent.builder()
                .name("router")
                .description("Routes requests")
                .model(routingModel)
                .subAgents(List.of(subAgent))
                .build();

        assertDoesNotThrow(routingAgent::getGraph);
    }

    /**
     * A fallback name is a reference to an existing sub-agent node, so an unknown name
     * should be rejected when the routing agent is built.
     */
    @Test
    void fallbackAgentMustReferenceConfiguredSubAgent() {
        ReactAgent subAgent = agent("known_agent", "known_output", new CountingChatModel("DONE"));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> LlmRoutingAgent.builder()
                        .name("router")
                        .description("Routes requests")
                        .model(new CountingChatModel(validRoutingDecision("known_agent")))
                        .subAgents(List.of(subAgent))
                        .fallbackAgent("missing_agent")
                        .build());

        assertTrue(exception.getMessage().contains("missing_agent"));
    }

    /**
     * A valid model decision should keep the normal route and must not invoke the fallback
     * agent merely because one is configured.
     */
    @Test
    void validRoutingDecisionDoesNotUseFallbackAgent() throws GraphRunnerException {
        CountingChatModel routingModel = new CountingChatModel(validRoutingDecision("primary_agent"));
        CountingChatModel primaryModel = new CountingChatModel("PRIMARY_EXECUTED");
        CountingChatModel fallbackModel = new CountingChatModel("FALLBACK_EXECUTED");
        ReactAgent primaryAgent = agent("primary_agent", "primary_output", primaryModel);
        ReactAgent fallbackAgent = agent("fallback_agent", "fallback_output", fallbackModel);

        LlmRoutingAgent routingAgent = LlmRoutingAgent.builder()
                .name("router")
                .description("Routes requests")
                .model(routingModel)
                .subAgents(List.of(primaryAgent, fallbackAgent))
                .fallbackAgent(fallbackAgent.name())
                .build();

        Optional<OverAllState> result = routingAgent.invoke("route this request");

        assertTrue(result.isPresent());
        assertEquals(1, routingModel.callCount());
        assertEquals(1, primaryModel.callCount());
        assertEquals(0, fallbackModel.callCount());
        AssistantMessage primaryOutput = result.get().value("primary_output", AssistantMessage.class).orElseThrow();
        assertEquals("PRIMARY_EXECUTED", primaryOutput.getText());
    }

    private static LlmRoutingAgent routingAgentWithFallback(CapturingChatModel fallbackModel) {
        return routingAgentWithFallback(fallbackModel, false);
    }

    private static LlmRoutingAgent routingAgentWithFallback(CapturingChatModel fallbackModel,
                                                            boolean includeContents) {
        ReactAgent fallbackAgent = ReactAgent.builder()
                .name("fallback_agent")
                .description("Fallback agent")
                .model(fallbackModel)
                .instruction("Handle this original request: {fallback_agent_input}")
                .includeContents(includeContents)
                .outputKey("fallback_output")
                .build();

        return LlmRoutingAgent.builder()
                .name("router")
                .description("Routes requests")
                .model(new CountingChatModel(invalidRoutingDecision()))
                .subAgents(List.of(fallbackAgent))
                .fallbackAgent(fallbackAgent.name())
                .build();
    }

    private static void assertInstructionEquals(CapturingChatModel model, String expectedText) {
        assertEquals(expectedText, model.lastInstructionText());
    }

    private static ReactAgent agent(String name, String outputKey, ChatModel model) {
        return ReactAgent.builder()
                .name(name)
                .description("Test agent " + name)
                .model(model)
                .outputKey(outputKey)
                .build();
    }

    private static String invalidRoutingDecision() {
        return validRoutingDecision("unknown_agent");
    }

    private static String validRoutingDecision(String agentName) {
        return "{\"agents\":[{\"agent\":\"" + agentName + "\",\"query\":\"handle the request\"}]}";
    }

    private static boolean hasMessage(Throwable throwable, String expectedText) {
        Throwable current = throwable;
        while (current != null) {
            if (current.getMessage() != null && current.getMessage().contains(expectedText)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private static final class CapturingChatModel implements ChatModel {

        private final String responseText;

        private List<Message> lastPromptMessages = List.of();

        private CapturingChatModel(String responseText) {
            this.responseText = responseText;
        }

        @Override
        public ChatResponse call(Prompt prompt) {
            this.lastPromptMessages = List.copyOf(prompt.getInstructions());
            return new ChatResponse(List.of(new Generation(new AssistantMessage(responseText))));
        }

        @Override
        public Flux<ChatResponse> stream(Prompt prompt) {
            return Flux.just(call(prompt));
        }

        private String lastInstructionText() {
            return lastPromptMessages.stream()
                    .filter(AgentInstructionMessage.class::isInstance)
                    .map(Message::getText)
                    .findFirst()
                    .orElseThrow();
        }

        private boolean lastPromptContainsMedia(Media media) {
            return lastPromptMessages.stream()
                    .filter(UserMessage.class::isInstance)
                    .map(UserMessage.class::cast)
                    .flatMap(message -> message.getMedia().stream())
                    .anyMatch(media::equals);
        }
    }

    /**
     * Deterministic model used for both routing and sub-agent responses. Besides avoiding
     * network and API-key dependencies, the counter verifies the exact retry and fallback
     * execution counts.
     */
    private static final class CountingChatModel implements ChatModel {

        private final String responseText;

        private final RuntimeException failure;

        private final AtomicInteger calls = new AtomicInteger();

        private CountingChatModel(String responseText) {
            this(responseText, null);
        }

        private CountingChatModel(String responseText, RuntimeException failure) {
            this.responseText = responseText;
            this.failure = failure;
        }

        private static CountingChatModel failing(RuntimeException failure) {
            return new CountingChatModel(null, failure);
        }

        @Override
        public ChatResponse call(Prompt prompt) {
            calls.incrementAndGet();
            if (failure != null) {
                throw failure;
            }
            return new ChatResponse(List.of(new Generation(new AssistantMessage(responseText))));
        }

        @Override
        public Flux<ChatResponse> stream(Prompt prompt) {
            return Flux.just(call(prompt));
        }

        private int callCount() {
            return calls.get();
        }
    }
}
