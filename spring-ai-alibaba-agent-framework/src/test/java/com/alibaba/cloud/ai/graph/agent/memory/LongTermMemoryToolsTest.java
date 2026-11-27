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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.interceptor.toolselection.ToolSelectionInterceptor;
import com.alibaba.cloud.ai.graph.agent.tools.ToolContextConstants;
import com.alibaba.cloud.ai.graph.store.StoreItem;
import com.alibaba.cloud.ai.graph.store.stores.MemoryStore;

import org.junit.jupiter.api.Test;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
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
