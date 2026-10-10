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
package com.alibaba.cloud.ai.graph.agent.node;

import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.interceptor.modelretry.ModelRetryInterceptor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.web.client.ResourceAccessException;
import reactor.core.publisher.Flux;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Timeout(10)
class AgentLlmNodeErrorHandlingTest {

	@ParameterizedTest
	@NullSource
	@ValueSource(booleans = false)
	void synchronousErrorsRemainMessagesByDefault(Boolean throwOnModelError) throws Exception {
		ChatModel model = mock(ChatModel.class);
		when(model.call(any(Prompt.class))).thenThrow(failure());
		AgentLlmNode node = nodeBuilder(ChatClient.create(model), throwOnModelError).build();
		AssistantMessage message = assertInstanceOf(AssistantMessage.class, apply(node, false));

		assertEquals("Exception: model unavailable", message.getText());
		verify(model, times(1)).call(any(Prompt.class));
	}

	@Test
	void synchronousErrorsRetainTheirTypeAndCauseWhenEnabled() {
		ChatModel model = mock(ChatModel.class);
		ResourceAccessException failure = failure();
		when(model.call(any(Prompt.class))).thenThrow(failure);
		AgentLlmNode node = nodeBuilder(ChatClient.create(model), true).build();

		ResourceAccessException thrown = assertThrows(ResourceAccessException.class, () -> apply(node, false));

		assertSame(failure, thrown);
		assertSame(failure.getCause(), thrown.getCause());
		verify(model, times(1)).call(any(Prompt.class));
	}

	@ParameterizedTest
	@NullSource
	@ValueSource(booleans = false)
	void streamingSetupErrorsRemainMessagesByDefault(Boolean throwOnModelError) throws Exception {
		ChatClient client = mock(ChatClient.class);
		when(client.prompt()).thenThrow(failure());
		AgentLlmNode node = nodeBuilder(client, throwOnModelError).build();

		AssistantMessage message = assertInstanceOf(AssistantMessage.class, apply(node, true));

		assertEquals("Exception: model unavailable", message.getText());
	}

	@Test
	void streamingSetupErrorsPropagateWhenEnabled() {
		ChatClient client = mock(ChatClient.class);
		ResourceAccessException failure = failure();
		when(client.prompt()).thenThrow(failure);
		AgentLlmNode node = nodeBuilder(client, true).build();

		assertSame(failure, assertThrows(ResourceAccessException.class, () -> apply(node, true)));
	}

	@ParameterizedTest
	@NullSource
	@ValueSource(booleans = { false, true })
	void streamingErrorSignalsStillPropagateAfterPartialOutput(Boolean throwOnModelError) throws Exception {
		ChatModel model = mock(ChatModel.class);
		ResourceAccessException failure = failure();
		when(model.stream(any(Prompt.class)))
				.thenReturn(Flux.concat(Flux.just(response("partial")), Flux.error(failure)));
		AgentLlmNode node = nodeBuilder(ChatClient.create(model), throwOnModelError).build();
		List<String> received = new ArrayList<>();

		Flux<?> output = assertInstanceOf(Flux.class, apply(node, true));
		ResourceAccessException thrown = assertThrows(ResourceAccessException.class,
				() -> output.cast(ChatResponse.class)
						.doOnNext(chunk -> received.add(chunk.getResult().getOutput().getText()))
						.blockLast(Duration.ofSeconds(2)));

		assertSame(failure, thrown);
		assertEquals(List.of("partial"), received);
		verify(model, times(1)).stream(any(Prompt.class));
	}

	@ParameterizedTest
	@NullSource
	@ValueSource(booleans = { false, true })
	void streamingErrorSignalsStillPropagateBeforeAnyOutput(Boolean throwOnModelError) throws Exception {
		ChatModel model = mock(ChatModel.class);
		ResourceAccessException failure = failure();
		when(model.stream(any(Prompt.class))).thenReturn(Flux.error(failure));
		AgentLlmNode node = nodeBuilder(ChatClient.create(model), throwOnModelError).build();

		Flux<?> output = assertInstanceOf(Flux.class, apply(node, true));

		assertSame(failure, assertThrows(ResourceAccessException.class,
				() -> output.blockLast(Duration.ofSeconds(2))));
		verify(model, times(1)).stream(any(Prompt.class));
	}

	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	void successfulStreamingResponsesRemainUnchanged(boolean throwOnModelError) throws Exception {
		ChatModel model = mock(ChatModel.class);
		when(model.stream(any(Prompt.class))).thenReturn(Flux.just(response("success")));
		AgentLlmNode node = nodeBuilder(ChatClient.create(model), throwOnModelError).build();

		Flux<?> output = assertInstanceOf(Flux.class, apply(node, true));
		List<String> text = output.cast(ChatResponse.class)
				.map(chunk -> chunk.getResult().getOutput().getText())
				.collectList()
				.block(Duration.ofSeconds(2));

		assertEquals(List.of("success"), text);
		verify(model, times(1)).stream(any(Prompt.class));
	}

	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	void successfulTextIsNotClassifiedAsAnError(boolean throwOnModelError) throws Exception {
		ChatModel model = mock(ChatModel.class);
		when(model.call(any(Prompt.class))).thenReturn(response("Exception: is a prefix, not a failure"));
		AgentLlmNode node = nodeBuilder(ChatClient.create(model), throwOnModelError).build();

		AssistantMessage message = assertInstanceOf(AssistantMessage.class, apply(node, false));

		assertEquals("Exception: is a prefix, not a failure", message.getText());
	}

	@Test
	void retryInterceptorReceivesOriginalException() throws Exception {
		ChatModel model = mock(ChatModel.class);
		ResourceAccessException failure = failure();
		when(model.call(any(Prompt.class))).thenThrow(failure).thenReturn(response("recovered"));
		List<Exception> classified = new ArrayList<>();
		ModelRetryInterceptor retry = ModelRetryInterceptor.builder()
				.maxAttempts(2)
				.initialDelay(0)
				.retryableExceptionPredicate(error -> {
					classified.add(error);
					return error instanceof ResourceAccessException;
				})
				.build();
		AgentLlmNode node = nodeBuilder(ChatClient.create(model), true)
				.modelInterceptors(List.of(retry))
				.build();

		AssistantMessage message = assertInstanceOf(AssistantMessage.class, apply(node, false));

		assertEquals("recovered", message.getText());
		assertEquals(1, classified.size());
		assertSame(failure, classified.get(0));
		verify(model, times(2)).call(any(Prompt.class));
	}

	@Test
	void retryExhaustionPreservesOriginalCause() {
		ChatModel model = mock(ChatModel.class);
		ResourceAccessException failure = failure();
		when(model.call(any(Prompt.class))).thenThrow(failure);
		AgentLlmNode node = nodeBuilder(ChatClient.create(model), true)
				.modelInterceptors(List.of(ModelRetryInterceptor.builder().maxAttempts(1).build()))
				.build();

		RuntimeException thrown = assertThrows(RuntimeException.class, () -> apply(node, false));

		assertSame(failure, thrown.getCause());
		verify(model, times(1)).call(any(Prompt.class));
	}

	@ParameterizedTest
	@NullSource
	@ValueSource(booleans = { false, true })
	void publicAgentBuilderWiresErrorPolicy(Boolean throwOnModelError) throws Exception {
		ChatModel model = mock(ChatModel.class);
		ResourceAccessException failure = failure();
		when(model.call(any(Prompt.class))).thenThrow(failure);
		var builder = ReactAgent.builder().name("error-policy-test").model(model);
		if (throwOnModelError != null) {
			builder.throwOnModelError(throwOnModelError);
		}
		ReactAgent agent = builder.build();

		if (Boolean.TRUE.equals(throwOnModelError)) {
			Exception thrown = assertThrows(Exception.class, () -> agent.call("test"));
			Throwable cause = thrown;
			while (cause != null && cause != failure) {
				cause = cause.getCause();
			}
			assertSame(failure, cause, "Graph execution must retain the model exception in its cause chain");
		}
		else {
			assertEquals("Exception: model unavailable", agent.call("test").getText());
		}
		verify(model, times(1)).call(any(Prompt.class));
	}

	private static AgentLlmNode.Builder nodeBuilder(ChatClient client, Boolean throwOnModelError) {
		AgentLlmNode.Builder builder = AgentLlmNode.builder().agentName("test").chatClient(client);
		if (throwOnModelError != null) {
			builder.throwOnModelError(throwOnModelError);
		}
		return builder;
	}

	private static Object apply(AgentLlmNode node, boolean stream) throws Exception {
		OverAllState state = new OverAllState(Map.of("messages", new ArrayList<>(List.of(new UserMessage("test")))));
		RunnableConfig config = RunnableConfig.builder().addMetadata("_stream_", stream).build();
		return node.apply(state, config).get("messages");
	}

	private static ResourceAccessException failure() {
		return new ResourceAccessException("model unavailable", new SocketTimeoutException("read deadline"));
	}

	private static ChatResponse response(String text) {
		return new ChatResponse(List.of(new Generation(new AssistantMessage(text))));
	}

}
