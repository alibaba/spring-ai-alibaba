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
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.context.ToolResultArchive;
import com.alibaba.cloud.ai.graph.agent.interceptor.contextediting.ContextEditingInterceptor;
import com.alibaba.cloud.ai.graph.agent.interceptor.toolselection.ToolSelectionInterceptor;
import com.alibaba.cloud.ai.graph.store.StoreItem;
import com.alibaba.cloud.ai.graph.store.stores.FileSystemStore;
import com.alibaba.cloud.ai.graph.store.stores.MemoryStore;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.part.TextPart;
import org.springframework.ai.chat.messages.part.ToolResultPart;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.ai.tool.metadata.ToolMetadata;

import static org.assertj.core.api.Assertions.assertThat;

class ToolResultArchiveTest {

	private static final String CONTENT = "文🙂字🔍abc".repeat(60);

	private static final String AGENT = "worker";

	private static final Pattern REFERENCE = Pattern.compile("[a-f0-9]{64}");

	@TempDir
	Path directory;

	@Test
	void readsBoundedUnicodePagesAfterReopeningTheStoreAndRejectsOtherScopes() {
		var scope = config("tenant-a", "thread");
		var original = original();
		var archive = scopedArchive(new FileSystemStore(this.directory));

		var stored = archive.archive(scope, AGENT, "call", original);
		String reference = reference(stored);
		assertThat(stored.id()).isEqualTo(original.id());
		assertThat(stored.attributes()).containsEntry("fixture", "kept");
		assertThat(reference(archive.archive(scope, AGENT, "call", original))).isEqualTo(reference);
		var differentResult = new ToolResultPart("call", "produce", List.of(TextPart.of(CONTENT + "more")), false,
				null, Map.of());
		assertThat(reference(archive.archive(scope, AGENT, "call", differentResult))).isNotEqualTo(reference);

		var reopened = scopedArchive(new FileSystemStore(this.directory));
		StringBuilder retrieved = new StringBuilder();
		for (Integer page = 1; page != null;) {
			var result = reopened.readPage(scope, AGENT, reference, page).orElseThrow();
			assertThat(result.text().codePointCount(0, result.text().length())).isLessThanOrEqualTo(40);
			assertThat(result.text()).doesNotEndWith("\uD83D");
			retrieved.append(result.text());
			page = result.nextPage();
		}
		assertThat(retrieved.toString()).isEqualTo(CONTENT);
		assertThat(reopened.readPage(config("tenant-b", "thread"), AGENT, reference, 1)).isEmpty();
		assertThat(reopened.readPage(config("tenant-a", "other-thread"), AGENT, reference, 1)).isEmpty();
		assertThat(reopened.readPage(scope, "other-worker", reference, 1)).isEmpty();
		assertThat(reopened.readPage(scope, AGENT, reference, Integer.MAX_VALUE)).isEmpty();
		assertThat(reopened.readPage(scope, AGENT, "../../archive", 1)).isEmpty();
	}

	@Test
	void publishesOnlyCompleteArchivesAndKeepsOriginalResultsWithoutScope() {
		var failWrite = new AtomicBoolean(true);
		var store = new MemoryStore() {
			@Override
			public void putItem(StoreItem item) {
				if (failWrite.get() && item.getKey().endsWith("-manifest")) {
					throw new IllegalStateException("interrupted write");
				}
				super.putItem(item);
			}
		};
		var archive = ToolResultArchive.builder(store).thresholdChars(30).pageSizeChars(40).build();
		var context = config("tenant-a", "thread");
		var original = original();

		assertThat(archive.archive(context, AGENT, "call", original)).isSameAs(original);
		assertThat(store.size()).isPositive();
		assertThat(archive.archive(RunnableConfig.builder().build(), AGENT, "call", original)).isSameAs(original);

		failWrite.set(false);
		String reference = reference(archive.archive(context, AGENT, "call", original));
		assertThat(archive.readPage(context, AGENT, reference, 1)).isPresent();
	}

	@Test
	void exposesAPageReaderToTheModelWithoutArchivingItsOutputAgain() throws Exception {
		var scope = config("tenant-a", "thread");
		var archive = ToolResultArchive.builder(new MemoryStore()).thresholdChars(30).pageSizeChars(40).build();
		var producer = FunctionToolCallback.builder("produce", (Map<String, Object> input) -> CONTENT)
			.description("Produce a long result").inputType(Map.class).build();
		var calls = new AtomicInteger();
		var mapper = new ObjectMapper();
		String serialized = mapper.writeValueAsString(CONTENT);
		ChatModel model = prompt -> {
			int step = calls.getAndIncrement();
			if (step == 0) {
				return toolCall("write", "produce", "{}");
			}
			var results = prompt.getInstructions().stream()
				.filter(ToolResponseMessage.class::isInstance)
				.map(ToolResponseMessage.class::cast)
				.flatMap(message -> message.getResults().stream())
				.toList();
			var result = results.get(results.size() - 1);
			if (step == 1) {
				assertThat(ToolResultArchive.isArchived(result)).isTrue();
				assertThat(result.id()).isEqualTo("write");
				assertThat(result.toToolResponse().responseData()).doesNotContain(CONTENT);
				return toolCall("read", ToolResultArchive.READ_TOOL_NAME,
						"{\"reference\":\"" + reference(result) + "\",\"page\":2}");
			}
			assertThat(ToolResultArchive.isArchived(result)).isFalse();
			assertThat(results.get(0).toToolResponse().responseData()).contains("Tool result archived");
			JsonNode read = readTree(mapper, result.toToolResponse().responseData());
			assertThat(read.path("page").intValue()).isEqualTo(2);
			assertThat(read.path("text").textValue()).isEqualTo(serialized.substring(serialized.offsetByCodePoints(0, 40),
					serialized.offsetByCodePoints(0, 80)));
			return new ChatResponse(List.of(new Generation(new AssistantMessage("done"))));
		};
		var agent = ReactAgent.builder().name(AGENT).model(model).tools(producer).toolResultArchive(archive)
			.interceptors(ContextEditingInterceptor.builder().trigger(0).keep(1).build()).build();

		assertThat(agent.call("Produce and read", scope).getText()).isEqualTo("done");
		assertThat(calls).hasValue(3);
	}

	@Test
	void preservesASuccessfulReturnDirectResult() throws Exception {
		var store = new MemoryStore();
		var archive = ToolResultArchive.builder(store).thresholdChars(30).build();
		var producer = FunctionToolCallback.builder("produce", (Map<String, Object> input) -> CONTENT)
			.description("Produce a long result").inputType(Map.class)
			.toolMetadata(ToolMetadata.builder().returnDirect(true).build()).build();
		var calls = new AtomicInteger();
		ChatModel model = prompt -> {
			calls.incrementAndGet();
			return toolCall("call", "produce", "{}");
		};
		var agent = ReactAgent.builder().name(AGENT).model(model).tools(producer).toolResultArchive(archive).build();

		assertThat(agent.call("Produce", config("tenant-a", "thread")).getText())
			.isEqualTo(new ObjectMapper().writeValueAsString(CONTENT));
		assertThat(calls).hasValue(1);
		assertThat(store.isEmpty()).isTrue();
	}

	@Test
	void retainsPageReaderWhenToolSelectionInterceptorLimitsTools() throws Exception {
		var scope = config("tenant-a", "thread");
		var archive = ToolResultArchive.builder(new MemoryStore()).thresholdChars(30).pageSizeChars(40).build();
		var producer = FunctionToolCallback.builder("produce", (Map<String, Object> input) -> CONTENT)
			.description("Produce a long result").inputType(Map.class).build();
		var unused = FunctionToolCallback.builder("unused", (Map<String, Object> input) -> "unused")
			.description("Unused business tool").inputType(Map.class).build();
		var calls = new AtomicInteger();
		ChatModel model = new ChatModel() {
			@Override
			public ChatResponse call(Prompt prompt) {
				var toolNames = ((ToolCallingChatOptions) prompt.getOptions()).getToolCallbacks().stream()
					.map(callback -> callback.getToolDefinition().name()).toList();
				assertThat(toolNames).contains(ToolResultArchive.READ_TOOL_NAME, "produce").doesNotContain("unused");
				int step = calls.getAndIncrement();
				if (step == 0) {
					return toolCall("write", "produce", "{}");
				}
				var results = prompt.getInstructions().stream()
					.filter(ToolResponseMessage.class::isInstance)
					.map(ToolResponseMessage.class::cast)
					.flatMap(message -> message.getResults().stream())
					.toList();
				if (step == 1) {
					return toolCall("read", ToolResultArchive.READ_TOOL_NAME,
							"{\"reference\":\"" + reference(results.get(results.size() - 1)) + "\",\"page\":1}");
				}
				return new ChatResponse(List.of(new Generation(new AssistantMessage("done"))));
			}

			@Override
			public ToolCallingChatOptions getOptions() {
				return ToolCallingChatOptions.builder().build();
			}
		};
		var selector = ToolSelectionInterceptor.builder().maxTools(1)
			.selectionModel(prompt -> new ChatResponse(List.of(new Generation(new AssistantMessage("{\"tools\":[\"produce\"]}")))))
			.build();
		var agent = ReactAgent.builder().name(AGENT).model(model).tools(producer, unused).toolResultArchive(archive)
			.interceptors(selector).build();

		assertThat(agent.call("Produce and read with tool selection", scope).getText()).isEqualTo("done");
		assertThat(calls).hasValue(3);
	}

	private static ToolResultArchive scopedArchive(FileSystemStore store) {
		return ToolResultArchive.builder(store)
			.ownerScope(config -> (String) config.metadata("owner").orElseThrow())
			.thresholdChars(30).pageSizeChars(40).build();
	}

	private static RunnableConfig config(String owner, String thread) {
		return RunnableConfig.builder().threadId(thread).addMetadata("owner", owner).build();
	}

	private static ToolResultPart original() {
		return new ToolResultPart("call", "produce", List.of(TextPart.of(CONTENT)), false, null,
				Map.of("fixture", "kept"));
	}

	private static String reference(ToolResultPart result) {
		var matcher = REFERENCE.matcher(result.toToolResponse().responseData());
		assertThat(matcher.find()).isTrue();
		return matcher.group();
	}

	private static ChatResponse toolCall(String id, String name, String arguments) {
		var assistant = AssistantMessage.builder()
			.toolCalls(List.of(new AssistantMessage.ToolCall(id, "function", name, arguments))).build();
		return new ChatResponse(List.of(new Generation(assistant)));
	}

	private static JsonNode readTree(ObjectMapper mapper, String input) {
		try {
			return mapper.readTree(input);
		}
		catch (Exception error) {
			throw new AssertionError(error);
		}
	}

}
