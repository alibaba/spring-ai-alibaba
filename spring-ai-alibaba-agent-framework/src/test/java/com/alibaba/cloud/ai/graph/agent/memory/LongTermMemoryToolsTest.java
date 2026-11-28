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
package com.alibaba.cloud.ai.graph.agent.memory;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.interceptor.toolselection.ToolSelectionInterceptor;
import com.alibaba.cloud.ai.graph.agent.tools.ToolContextConstants;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.store.StoreItem;
import com.alibaba.cloud.ai.graph.store.stores.FileSystemStore;
import com.alibaba.cloud.ai.graph.store.stores.MemoryStore;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationHandler;
import io.micrometer.observation.ObservationRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.util.JsonHelper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class LongTermMemoryToolsTest {

	private static final JsonHelper JSON = new JsonHelper();

	@Test
	void searchesAndPagesMemoryOnlyOnRequestInsideTheExactRuntimeNamespace() throws Exception {
		var store = spy(new MemoryStore());
		var alice = List.of("profiles", "alice");
		store.putItem(StoreItem.of(alice, "profile", Map.of("preference", "tea 📚")));
		store.putItem(StoreItem.of(List.of("profiles", "alice", "private"), "a-private", Map.of("preference", "coffee")));
		store.putItem(StoreItem.of(List.of("profiles", "bob"), "a-other-user", Map.of("preference", "juice")));
		var tools = LongTermMemoryTools.builder().namespace(LongTermMemoryToolsTest::namespace)
			.maxSearchResults(1).pageCharacters(8).build();
		var reads = new AtomicInteger();
		var pages = new ArrayList<String>();
		var model = new StubModel(prompt -> {
			int step = reads.getAndIncrement();
			if (step == 0) {
				verify(store, never()).searchItems(any());
				verify(store, never()).getItem(any(), any());
				return call("search", "search_memory", "{\"query\":\"preference\"}");
			}
			String latest = lastResult(prompt);
			if (step == 1) {
				assertThat(latest).isEqualTo("[\"profile\"]");
				verify(store, never()).getItem(any(), any());
				return call("read", "read_memory", "{\"key\":\"profile\"}");
			}
			var page = JSON.fromJsonToMap(latest);
			String content = (String) page.get("content");
			assertThat(content).hasSizeLessThanOrEqualTo(8);
			pages.add(content);
			if (page.get("nextOffset") instanceof Number next) {
				return call("read-" + step, "read_memory", "{\"key\":\"profile\",\"offset\":" + next + "}");
			}
			return answer("memory retrieved");
		});
		var selector = ToolSelectionInterceptor.builder().maxTools(1).selectionModel(prompt -> {
			throw new AssertionError("Both memory tools must stay available");
		}).build();
		var agent = ReactAgent.builder().name("memory").model(model).tools(tools.tools()).interceptors(selector).build();
		var config = RunnableConfig.builder().threadId("fresh-session").store(store).addMetadata("user", "alice").build();

		assertThat(agent.call("Retrieve my preference", config).getText()).isEqualTo("memory retrieved");
		assertThat(pages).hasSizeGreaterThan(1);
		assertThat(JSON.fromJsonToMap(String.join("", pages))).containsExactlyEntriesOf(Map.of("preference", "tea 📚"));
	}

	@Test
	void rejectsMissingScopeAndCannotReadAnItemFromAnotherNamespace() {
		var store = spy(new MemoryStore());
		store.putItem(StoreItem.of(List.of("profiles", "bob"), "profile", Map.of("secret", "private")));
		var tools = LongTermMemoryTools.builder().store(store).namespace(LongTermMemoryToolsTest::namespace).build().tools();
		var search = tools.get(0);
		var read = tools.get(1);
		var missing = context(RunnableConfig.builder().build());
		var alice = context(RunnableConfig.builder().addMetadata("user", "alice").build());

		assertThat(search.call("{}", missing)).contains("Long-term memory is unavailable");
		assertThat(read.call("{\"key\":\"profile\"}", missing)).contains("Long-term memory is unavailable");
		verify(store, never()).searchItems(any());
		verify(store, never()).getItem(any(), any());
		assertThat(read.call("{\"key\":\"profile\"}", alice)).contains("Memory item is unavailable")
			.doesNotContain("private");
		assertThat(search.call("{}", alice)).isEqualTo("[]");
	}

	@Test
	void restoresScopedMemoryAcrossThreadsUsingFiles(@TempDir Path directory) throws Exception {
		var initialStore = new FileSystemStore(directory);
		var first = scopedConfig("first", "tenant-a", "alice");
		initialStore.putItem(StoreItem.of(List.of("tenant-a", "alice"), "profile", Map.of("language", "Mandarin")));
		initialStore.putItem(StoreItem.of(List.of("tenant-a", "alice", "unshared"), "a-private",
				Map.of("language", "Private")));
		initialStore.putItem(StoreItem.of(List.of("tenant-b", "alice"), "profile", Map.of("language", "Other tenant")));
		initialStore.putItem(StoreItem.of(List.of("tenant-a", "bob"), "profile", Map.of("language", "Other user")));
		var scoped = LongTermMemoryTools.builder().store(new FileSystemStore(directory))
				.namespace(config -> List.of(config.metadata("tenant").orElseThrow().toString(),
						config.metadata("user").orElseThrow().toString()))
				.maxSearchResults(1).build();
		var model = new StubModel(prompt -> {
			long results = prompt.getInstructions().stream().filter(ToolResponseMessage.class::isInstance).count();
			if (results == 0) {
				return call("search", "search_memory", "{\"query\":\"language\"}");
			}
			if (results == 1) {
				assertThat(lastResult(prompt)).isEqualTo("[\"profile\"]");
				return call("read", "read_memory", "{\"key\":\"profile\"}");
			}
			return answer(lastResult(prompt));
		});
		var agent = ReactAgent.builder().name("persistent-memory").model(model).saver(new MemorySaver())
				.tools(scoped.tools()).build();
		assertThat(agent.call("Find my preferred language", first).getText()).contains("Mandarin");
		assertThat(agent.call("Find it in a new thread", scopedConfig("second", "tenant-a", "alice")).getText())
				.contains("Mandarin").doesNotContain("Private");
		assertThat(agent.call("Find my language", scopedConfig("third", "tenant-b", "alice")).getText())
				.contains("Other tenant").doesNotContain("Mandarin");
		assertThat(agent.call("Find my language", scopedConfig("fourth", "tenant-a", "bob")).getText())
				.contains("Other user").doesNotContain("Mandarin");
	}

	@Test
	void distinguishesStoreFailureAndRejectsInvalidRequestsBeforeAccess() {
		var store = spy(new MemoryStore());
		doThrow(new IllegalStateException("private-store-details")).when(store).searchItems(any());
		var registry = ObservationRegistry.create();
		var observations = new ArrayList<Observation.Context>();
		registry.observationConfig().observationHandler(new ObservationHandler<Observation.Context>() {
			@Override
			public boolean supportsContext(Observation.Context context) {
				return true;
			}

			@Override
			public void onStop(Observation.Context context) {
				observations.add(context);
			}
		});
		var tools = LongTermMemoryTools.builder().store(store).namespace(LongTermMemoryToolsTest::namespace)
				.maxQueryCharacters(4).observationRegistry(registry).build().tools();
		var alice = context(RunnableConfig.builder().addMetadata("user", "alice").build());
		var invalid = context(RunnableConfig.builder().addMetadata("user", "../alice").build());
		assertThat(tools.get(0).call("{\"query\":\"too long\"}", alice)).contains("query is too long");
		assertThat(tools.get(1).call("{\"key\":\"profile\",\"offset\":-1}", alice)).contains("Invalid memory read");
		assertThat(tools.get(1).call("{\"key\":\"profile\"}", invalid)).contains("Long-term memory is unavailable");
		verify(store, never()).searchItems(any());
		verify(store, never()).getItem(any(), any());
		assertThat(tools.get(0).call("{\"query\":\"test\"}", alice))
				.contains("temporarily unavailable").doesNotContain("private-store-details");
		assertThat(observations).extracting(o -> o.getLowCardinalityKeyValue("outcome").getValue())
				.containsExactly("invalid_request", "invalid_request", "scope_unavailable", "backend_error");
	}

	@Test
	void limitsRecallAcrossModelStepsAndResetsForTheNextTurn() throws Exception {
		var store = spy(new MemoryStore());
		store.putItem(StoreItem.of(List.of("profiles", "alice"), "profile", Map.of("preference", "tea")));
		var tools = LongTermMemoryTools.builder().store(store).namespace(LongTermMemoryToolsTest::namespace)
				.maxCallsPerToolPerTurn(1).build();
		var step = new AtomicInteger();
		var model = new StubModel(prompt -> switch (step.getAndIncrement() % 3) {
			case 0, 1 -> call("read-" + step.get(), "read_memory", "{\"key\":\"profile\"}");
			default -> answer(lastResult(prompt));
		});
		var agent = ReactAgent.builder().name("bounded-memory").model(model).tools(tools.tools())
				.saver(new MemorySaver()).build();
		var config = RunnableConfig.builder().threadId("thread").addMetadata("user", "alice").build();
		assertThat(agent.call("Read twice", config).getText()).contains("Tool call limit (1)");
		assertThat(agent.call("Read twice again", config).getText()).contains("Tool call limit (1)");
		verify(store, times(2)).getItem(List.of("profiles", "alice"), "profile");
	}

	private static RunnableConfig scopedConfig(String thread, String tenant, String user) {
		return RunnableConfig.builder().threadId(thread).addMetadata("tenant", tenant).addMetadata("user", user).build();
	}

	private static ToolContext context(RunnableConfig config) {
		return new ToolContext(Map.of(ToolContextConstants.AGENT_CONFIG_CONTEXT_KEY, config));
	}

	private static List<String> namespace(RunnableConfig config) {
		return config.metadata("user").filter(String.class::isInstance).map(String.class::cast)
			.map(user -> List.of("profiles", user)).orElseGet(List::of);
	}

	private static String lastResult(Prompt prompt) {
		var results = prompt.getInstructions().stream().filter(ToolResponseMessage.class::isInstance)
			.map(ToolResponseMessage.class::cast).toList();
		return results.get(results.size() - 1).getResponses().get(0).responseData();
	}

	private static ChatResponse answer(String text) {
		return new ChatResponse(List.of(new Generation(new AssistantMessage(text))));
	}

	private static ChatResponse call(String id, String name, String arguments) {
		var tool = new AssistantMessage.ToolCall(id, "function", name, arguments);
		return new ChatResponse(List.of(new Generation(AssistantMessage.builder().toolCalls(List.of(tool)).build())));
	}

	private record StubModel(Function<Prompt, ChatResponse> respond) implements ChatModel {

		@Override
		public ChatResponse call(Prompt prompt) {
			return this.respond.apply(prompt);
		}

		@Override
		public ToolCallingChatOptions getOptions() {
			return ToolCallingChatOptions.builder().build();
		}

	}

}
