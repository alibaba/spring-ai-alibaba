/*
 * Copyright 2026 the original author or authors.
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

import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.agent.tool.CancellationToken;
import com.alibaba.cloud.ai.graph.agent.tool.DefaultCancellationToken;
import com.alibaba.cloud.ai.graph.agent.tool.StreamingToolCallback;
import com.alibaba.cloud.ai.graph.agent.tool.ToolStreamPart;
import com.alibaba.cloud.ai.graph.agent.tools.ToolContextHelper;
import com.alibaba.cloud.ai.graph.streaming.RunEvent;
import com.alibaba.cloud.ai.graph.streaming.StreamOptions;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.tool.definition.ToolDefinition;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.ArrayList;
import java.util.function.Function;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.streaming.RunEvent.Type;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AgentRunEventTest {

	@Test
	void agentNodeUsesItsOwnScopeAndFinalMessage() throws Exception {
		var agent = ReactAgent.builder().name("researcher").model(model(prompt -> Flux.just(reply("result")))).build();
		var graph = new StateGraph("parent", Map::of).addNode("researcher", agent.asNode(true, false))
				.addEdge(START, "researcher").addEdge("researcher", END).compile();
		var events = graph.streamEvents(Map.of("input", "test"), config(), StreamOptions.all())
				.collectList().block(Duration.ofSeconds(5));
		assertThat(events.stream().filter(event -> event.type() == Type.MODEL_DELTA).findFirst()).hasValueSatisfying(event ->
				assertThat(event.scope().namespace()).extracting(RunEvent.PathSegment::kind)
						.contains(RunEvent.ScopeKind.AGENT));
		assertThat(events.get(events.size() - 1).type()).isEqualTo(Type.RUN_COMPLETED);
	}

	@Test
	void streamsBothToolDeltasAndTheDelegatedModelBeforeCompleting() throws Exception {
		var gate = new CompletableFuture<ToolStreamPart>();
		var received = new CountDownLatch(1);
		var toolCalls = new AtomicInteger();
		var tool = new StreamingToolCallback() {
			@Override
			public ToolDefinition getToolDefinition() {
				return ToolDefinition.builder().name("read").description("Read data").inputSchema("{}").build();
			}

			@Override
			public Flux<ToolStreamPart> stream(String arguments, ToolContext context, CancellationToken cancellation) {
				toolCalls.incrementAndGet();
				ToolContextHelper.streamWriter(context).tryEmit("reading", 1);
				return Flux.<ToolStreamPart>just(new ToolStreamPart.Delta("partial")).concatWith(Mono.fromFuture(gate));
			}
		};
		var worker = ReactAgent.builder().name("worker").description("Delegated worker").model(model(prompt -> Flux.just(
				toolResult(prompt) == null ? calls("read", "{}") : reply(toolResult(prompt))))).tools(tool).build();
		var parent = ReactAgent.builder().name("parent").tools(AgentTool.create(worker))
				.model(model(prompt -> Flux.just(toolResult(prompt) == null
						? calls("worker", "{\"input\":\"request\"}") : reply("answer " + toolResult(prompt)))))
				.build();
		var future = parent.streamEvents("request", config(), StreamOptions.all()).doOnNext(event -> {
			if (event.type() == Type.TOOL_DELTA) {
				received.countDown();
			}
		}).collectList().toFuture();
		assertThat(received.await(5, TimeUnit.SECONDS)).isTrue();
		assertThat(future).isNotDone();
		gate.complete(new ToolStreamPart.Completed("tool answer"));
		var events = future.get(5, TimeUnit.SECONDS);
		assertThat(toolCalls).hasValue(1);
		assertThat(events).extracting(RunEvent::type).contains(Type.CUSTOM, Type.TOOL_COMPLETED);
		var delta = events.stream().filter(event -> event.type() == Type.TOOL_DELTA).findFirst().orElseThrow();
		assertThat(delta.scope().namespace()).extracting(RunEvent.PathSegment::name).contains("parent", "worker", "read");
		assertThat(delta.data()).isInstanceOfSatisfying(RunEvent.Tool.class, data -> assertThat(data.value()).isEqualTo("partial"));
		assertThat(events.stream().filter(event -> event.type() == Type.MODEL_COMPLETED).map(RunEvent::data))
			.anySatisfy(data -> assertThat(((RunEvent.ModelCompleted) data).message().getText()).isEqualTo("answer tool answer"));
		assertThat(events.get(events.size() - 1).type()).isEqualTo(Type.RUN_COMPLETED);
	}

	@Test
	void rejectsToolDataAfterFinalResultAndPropagatesCancellation() throws Exception {
		var deltas = new ArrayList<>();
		StreamingToolCallback invalid = callback(Flux.just(new ToolStreamPart.Delta("visible"),
				new ToolStreamPart.Completed("final"), new ToolStreamPart.Delta("invalid")));
		var context = new ToolContext(Map.of());
		assertThatThrownBy(() -> invalid.callAsync("{}", context, CancellationToken.NONE,
				value -> Mono.fromRunnable(() -> deltas.add(value))).join())
				.hasRootCauseMessage("Streaming tool emitted data after its final result");
		assertThat(deltas).containsExactly("visible");
		var cancelled = new CountDownLatch(1);
		var activeCall = new DefaultCancellationToken();
		var pending = callback(Flux.<ToolStreamPart>never().doOnCancel(cancelled::countDown))
				.callAsync("{}", context, activeCall);
		activeCall.cancel();
		assertThat(cancelled.await(5, TimeUnit.SECONDS)).isTrue();
		assertThat(pending).isCancelled();
	}

	private static StreamingToolCallback callback(Flux<ToolStreamPart> parts) {
		return new StreamingToolCallback() {
			@Override
			public ToolDefinition getToolDefinition() {
				return ToolDefinition.builder().name("sample").description("Sample tool").inputSchema("{}").build();
			}

			@Override
			public Flux<ToolStreamPart> stream(String arguments, ToolContext context, CancellationToken cancellation) {
				return parts;
			}
		};
	}

	private static String toolResult(Prompt prompt) {
		return prompt.getInstructions().stream().filter(ToolResponseMessage.class::isInstance).map(ToolResponseMessage.class::cast)
				.reduce((left, right) -> right).map(result -> result.getResponses().get(0).responseData()).orElse(null);
	}

	private static ChatModel model(Function<Prompt, Flux<ChatResponse>> response) {
		return new ChatModel() {
			@Override
			public ChatResponse call(Prompt prompt) {
				return response.apply(prompt).blockLast();
			}

			@Override
			public Flux<ChatResponse> stream(Prompt prompt) {
				return response.apply(prompt);
			}
		};
	}

	private static ChatResponse calls(String name, String args) {
		var call = new AssistantMessage.ToolCall(name + "-call", "function", name, args);
		return new ChatResponse(List.of(new Generation(AssistantMessage.builder().toolCalls(List.of(call)).build())));
	}

	private static ChatResponse reply(String text) {
		return new ChatResponse(List.of(new Generation(new AssistantMessage(text))));
	}

	private static RunnableConfig config() {
		return RunnableConfig.builder().threadId("agent-events").build();
	}
}
