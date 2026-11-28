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
package com.alibaba.cloud.ai.graph.agent.memory;

import java.util.List;
import java.util.Map;

import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.store.StoreItem;
import com.alibaba.cloud.ai.graph.store.stores.MemoryStore;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;

import static org.assertj.core.api.Assertions.assertThat;

class LongTermMemoryTest {

	@Test
	void applicationPrefetchesAnAllowlistedPreferenceWithoutSharingConversationHistory() throws Exception {
		var namespace = List.of("tenant-a", "support", "alice", "preferences");
		var store = new MemoryStore();
		store.putItem(StoreItem.of(namespace, "reply_language", Map.of("language", "zh-CN", "note", "ignored")));
		ChatModel chat = prompt -> {
			String latest = prompt.getUserMessages().get(prompt.getUserMessages().size() - 1).getText();
			var previous = prompt.getUserMessages().stream().map(UserMessage::getText).toList();
			assertThat(previous).noneMatch(text -> text.contains("ignored"));
			if (!latest.equals("Private Atlas topic")) {
				assertThat(previous).doesNotContain("Private Atlas topic");
				assertThat(prompt.getSystemMessage().getText()).isEqualTo("Help with support requests.");
			}
			return new ChatResponse(List.of(new Generation(new AssistantMessage(latest))));
		};
		var agent = ReactAgent.builder().name("support").model(chat).systemPrompt("Help with support requests.")
			.saver(new MemorySaver()).build();

		var first = RunnableConfig.builder().threadId("first").build();
		var second = RunnableConfig.builder().threadId("second").build();
		agent.call("Private Atlas topic", first);

		String language = store.getItem(namespace, "reply_language").map(item -> item.getValue().get("language"))
				.filter(String.class::isInstance).map(String.class::cast)
				.filter(value -> List.of("en-US", "zh-CN").contains(value)).orElse("en-US");
		var answer = agent.call("Answer my question. Preferred language: " + language, second);
		assertThat(answer.getText()).contains("zh-CN");
	}

}
