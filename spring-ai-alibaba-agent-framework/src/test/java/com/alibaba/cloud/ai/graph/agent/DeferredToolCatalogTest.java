/*
 * Copyright 2025-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.cloud.ai.graph.agent;

import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.action.InterruptionMetadata;
import com.alibaba.cloud.ai.graph.action.InterruptionMetadata.ToolFeedback;
import com.alibaba.cloud.ai.graph.action.InterruptionMetadata.ToolFeedback.FeedbackResult;
import com.alibaba.cloud.ai.graph.agent.interceptor.toolselection.ToolSelectionInterceptor;
import com.alibaba.cloud.ai.graph.agent.tool.DeferredToolCatalog;
import com.alibaba.cloud.ai.graph.checkpoint.savers.file.FileSystemSaver;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.AssistantMessage.ToolCall;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.ai.tool.metadata.ToolMetadata;

import reactor.core.publisher.Flux;

import static org.assertj.core.api.Assertions.assertThat;

class DeferredToolCatalogTest {

	private static final Duration TIMEOUT = Duration.ofSeconds(10);

	@TempDir
	Path checkpointDirectory;

	@Test
	void searchesDescriptorsAndBindsLoadedToolsForTheNextRequestOnly() throws Exception {
		var loaded = new AtomicInteger();
		var executed = new AtomicInteger();
		var calls = new AtomicInteger();
		var selection = new StubModel(prompt -> answer("{\"tools\":[\"reports\",\"unused\"]}"));
		var model = new StubModel(prompt -> switch (calls.getAndIncrement()) {
			case 0 -> {
				assertTools(prompt, "unused");
				assertThat(loaded).hasValue(0);
				yield call("search", "search_tools", "{\"query\":\"monthly\"}");
			}
			case 1 -> {
				assertTools(prompt, "unused");
				assertThat(toolResults(prompt)).anySatisfy(result -> assertThat(result)
					.contains("reports", "Monthly report lookup").doesNotContain("inputSchema"));
				assertThat(loaded).hasValue(0);
				yield toolCalls(new ToolCall("load", "function", "load_tools", "{\"names\":[\"reports\",\"missing\"]}"),
						new ToolCall("premature", "function", "reports", "{}"));
			}
			case 2 -> {
				assertTools(prompt, "reports");
				assertThat(loaded).hasValue(1);
				assertThat(executed).hasValue(0);
				assertThat(toolResults(prompt)).anySatisfy(result -> assertThat(result).contains("missing", "reports"))
					.anySatisfy(result -> assertThat(result).containsIgnoringCase("not available"));
				yield call("report", "reports", "{}");
			}
			case 3 -> {
				assertThat(toolResults(prompt)).contains("\"report complete\"");
				yield answer("finished");
			}
			case 4 -> {
				assertTools(prompt, "unused");
				yield answer("new task");
			}
			default -> throw new AssertionError("Unexpected model request");
		});
		var agent = ReactAgent.builder().name("discovery").model(model).deferredTools(catalog(loaded, executed, false))
			.tools(callback("unused", "An unrelated static tool", new AtomicInteger(), false))
			.interceptors(ToolSelectionInterceptor.builder().selectionModel(selection).maxTools(1).build())
			.saver(saver()).build();
		var config = RunnableConfig.builder().threadId("discovery-thread").build();

		assertThat(agent.call("find the monthly report", config).getText()).isEqualTo("finished");
		assertThat(agent.call("start a new task", config).getText()).isEqualTo("new task");
		assertThat(loaded).hasValue(1);
		assertThat(executed).hasValue(1);
		assertThat(calls).hasValue(5);
	}

	@Test
	void restoresAnApprovedDeferredCallFromADurableCheckpoint() throws Exception {
		var loaded = new AtomicInteger();
		var executed = new AtomicInteger();
		var calls = new AtomicInteger();
		var model = new StubModel(prompt -> switch (calls.getAndIncrement()) {
			case 0 -> {
				assertTools(prompt);
				yield call("load", "load_tools", "{\"names\":[\"reports\"]}");
			}
			case 1 -> {
				assertTools(prompt, "reports");
				yield call("report", "reports", "{}");
			}
			case 2 -> {
				assertTools(prompt, "reports");
				assertThat(toolResults(prompt)).contains("\"report complete\"");
				yield answer("finished");
			}
			default -> throw new AssertionError("Unexpected model request");
		});
		var config = RunnableConfig.builder().threadId("approval-thread").build();
		var original = ReactAgent.builder().name("approval").model(model).deferredTools(catalog(loaded, executed, true))
			.saver(saver()).build();
		var pause = (InterruptionMetadata) original.stream("load and run reports", config).blockLast(TIMEOUT);
		assertThat(pause.toolFeedbacks()).hasSize(1);
		assertThat(loaded).hasValue(1);
		assertThat(executed).hasValue(0);
		var feedback = InterruptionMetadata.builder(pause).addToolFeedback(ToolFeedback.builder(pause.toolFeedbacks().get(0))
			.result(FeedbackResult.APPROVED).build()).build();
		var resumed = RunnableConfig.builder().threadId("approval-thread").addHumanFeedback(feedback).build();
		var restarted = ReactAgent.builder().name("approval").model(model).deferredTools(catalog(loaded, executed, true))
			.saver(saver()).build();

		assertThat(restarted.stream(Map.of(), resumed).blockLast(TIMEOUT).state().<List<Message>>value("messages")
			.orElseThrow()).extracting(Message::getText).contains("finished");
		assertThat(loaded).hasValue(2);
		assertThat(executed).hasValue(1);
	}

	@Test
	void combinesToolsLoadedByParallelCalls() throws Exception {
		var loads = new AtomicInteger();
		var executed = new AtomicInteger();
		var catalog = DeferredToolCatalog.builder()
			.tool("first", "First deferred tool", () -> {
				loads.incrementAndGet();
				return callback("first", "First deferred tool", executed, false);
			})
			.tool("second", "Second deferred tool", () -> {
				loads.incrementAndGet();
				return callback("second", "Second deferred tool", executed, false);
			}).build();
		var calls = new AtomicInteger();
		var model = new StubModel(prompt -> switch (calls.getAndIncrement()) {
			case 0 -> toolCalls(new ToolCall("first", "function", "load_tools", "{\"names\":[\"first\"]}"),
					new ToolCall("second", "function", "load_tools", "{\"names\":[\"second\"]}"));
			case 1 -> {
				assertTools(prompt, "first", "second");
				yield answer("both loaded");
			}
			default -> throw new AssertionError("Unexpected model request");
		});
		var agent = ReactAgent.builder().name("parallel_discovery").model(model).deferredTools(catalog)
			.parallelToolExecution(true).build();

		assertThat(agent.call("load two tools").getText()).isEqualTo("both loaded");
		assertThat(loads).hasValue(2);
		assertThat(executed).hasValue(0);
	}

	private static DeferredToolCatalog catalog(AtomicInteger loads, AtomicInteger calls, boolean confirmation) {
		return DeferredToolCatalog.builder().tool("reports", "Monthly report lookup", () -> {
			loads.incrementAndGet();
			return callback("reports", "Monthly report lookup", calls, confirmation);
		}).build();
	}

	private static ToolCallback callback(String name, String description, AtomicInteger calls, boolean confirmation) {
		return FunctionToolCallback.builder(name, args -> {
			calls.incrementAndGet();
			return "report complete";
		}).description(description).inputType(Map.class)
			.toolMetadata(ToolMetadata.builder().requiresConfirmation(confirmation).build()).build();
	}

	private FileSystemSaver saver() {
		return FileSystemSaver.builder().targetFolder(this.checkpointDirectory).build();
	}

	private static void assertTools(Prompt prompt, String... tools) {
		var options = (ToolCallingChatOptions) prompt.getOptions();
		var expected = new ArrayList<>(DeferredToolCatalog.DISCOVERY_TOOLS);
		expected.addAll(List.of(tools));
		assertThat(options.getToolCallbacks()).extracting(callback -> callback.getToolDefinition().name())
			.containsExactlyInAnyOrderElementsOf(expected);
	}

	private static List<String> toolResults(Prompt prompt) {
		return prompt.getInstructions().stream().filter(ToolResponseMessage.class::isInstance)
			.map(ToolResponseMessage.class::cast).flatMap(message -> message.getResponses().stream())
			.map(ToolResponseMessage.ToolResponse::responseData).toList();
	}

	private static ChatResponse call(String id, String name, String arguments) {
		return toolCalls(new ToolCall(id, "function", name, arguments));
	}

	private static ChatResponse toolCalls(ToolCall... calls) {
		return new ChatResponse(List.of(new Generation(AssistantMessage.builder().toolCalls(List.of(calls)).build())));
	}

	private static ChatResponse answer(String text) {
		return new ChatResponse(List.of(new Generation(new AssistantMessage(text))));
	}

	private record StubModel(Function<Prompt, ChatResponse> response) implements ChatModel {

		@Override
		public ChatResponse call(Prompt prompt) {
			return this.response.apply(prompt);
		}

		@Override
		public Flux<ChatResponse> stream(Prompt prompt) {
			return Flux.defer(() -> Flux.just(call(prompt)));
		}

		@Override
		public ToolCallingChatOptions getOptions() {
			return ToolCallingChatOptions.builder().build();
		}

	}

}
