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
package com.alibaba.cloud.ai.tool;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.AssistantMessage.ToolCall;

import static org.assertj.core.api.Assertions.assertThat;

class ObservableToolCallingManagerTests {

	@Test
	void mergeToolCallsShouldCombineStreamingChunks() {
		List<ToolCall> chunks = List.of(new ToolCall("1", "function", "weather", ""),
				new ToolCall(null, null, null, "{\"location\":\""), new ToolCall(null, null, null, "Paris\"}"),
				new ToolCall("2", "function", "time", ""), new ToolCall(null, null, null, "{}"));
		AssistantMessage message = new AssistantMessage("", Map.of(), chunks);

		AssistantMessage merged = ObservableToolCallingManager.mergeToolCalls(message);

		assertThat(merged.getToolCalls()).hasSize(2);
		ToolCall first = merged.getToolCalls().get(0);
		assertThat(first.id()).isEqualTo("1");
		assertThat(first.name()).isEqualTo("weather");
		assertThat(first.arguments()).isEqualTo("{\"location\":\"Paris\"}");
		ToolCall second = merged.getToolCalls().get(1);
		assertThat(second.id()).isEqualTo("2");
		assertThat(second.name()).isEqualTo("time");
		assertThat(second.arguments()).isEqualTo("{}");
	}

}
