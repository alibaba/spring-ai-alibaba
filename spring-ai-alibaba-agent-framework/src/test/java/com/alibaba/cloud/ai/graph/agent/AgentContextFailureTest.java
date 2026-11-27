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

import java.util.List;
import java.util.Map;

import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.extension.file.FilesystemBackend;
import com.alibaba.cloud.ai.graph.agent.extension.file.WriteResult;
import com.alibaba.cloud.ai.graph.agent.extension.interceptor.LargeResultEvictionInterceptor;
import com.alibaba.cloud.ai.graph.agent.hook.summarization.SummarizationHook;
import com.alibaba.cloud.ai.graph.agent.interceptor.ToolCallRequest;
import com.alibaba.cloud.ai.graph.agent.interceptor.ToolCallResponse;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AgentContextFailureTest {

	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	void summaryFailureOrBlankResultPreservesTheHistory(boolean failure) throws Exception {
		List<Message> history = List.of(new UserMessage("original question"),
				new AssistantMessage("original answer"), new UserMessage("new question"));
		ChatModel summaryModel = prompt -> {
			if (failure) {
				throw new IllegalStateException("summary unavailable");
			}
			return response(" ");
		};
		ChatModel model = prompt -> {
			assertThat(prompt.getInstructions()).extracting(Message::getText)
				.containsExactly("original question", "original answer", "new question");
			return response("done");
		};
		var hook = SummarizationHook.builder().model(summaryModel).maxTokensBeforeSummary(1).messagesToKeep(1).build();
		var agent = ReactAgent.builder().name("context").model(model).hooks(hook).build();
		var config = RunnableConfig.builder().addMetadata("_stream_", false).build();

		var state = agent.invoke(history, config).orElseThrow();
		assertThat(state.value("messages")).hasValueSatisfying(messages ->
			assertThat((List<?>) messages).hasSize(history.size() + 1));
	}

	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	void preservesToolResultWhenTheBackendCannotWrite(boolean throwsException) {
		var backend = mock(FilesystemBackend.class);
		when(backend.write(anyString(), anyString())).thenAnswer(invocation -> {
			if (throwsException) {
				throw new IllegalStateException("backend unavailable");
			}
			return new WriteResult(null, "backend unavailable", null);
		});
		var interceptor = LargeResultEvictionInterceptor.builder().backend(backend).toolTokenLimitBeforeEvict(1)
			.build();
		var request = ToolCallRequest.builder().toolName("lookup").toolCallId("call").build();
		var original = ToolCallResponse.builder().toolName("lookup").toolCallId("call")
			.content("large result".repeat(120)).status(ToolCallResponse.SUCCESS_STATUS)
			.metadata(Map.of("origin", "lookup")).build();

		assertThat(interceptor.interceptToolCall(request, ignored -> original)).isSameAs(original);
	}

	@Test
	void usesTheBackendConfirmedPathAndPreservesToolMetadata() {
		var backend = mock(FilesystemBackend.class);
		when(backend.write(anyString(), anyString())).thenReturn(new WriteResult("/archive/confirmed", null, null));
		var request = ToolCallRequest.builder().toolName("lookup").toolCallId("call").build();
		var original = ToolCallResponse.builder().toolName("lookup").toolCallId("call")
			.content("large result".repeat(120)).metadata(Map.of("origin", "lookup")).build();
		var interceptor = LargeResultEvictionInterceptor.builder().backend(backend).toolTokenLimitBeforeEvict(1)
			.build();

		var result = interceptor.interceptToolCall(request, ignored -> original);
		assertThat(result.getResult()).contains("/archive/confirmed");
		assertThat(result.getToolCallId()).isEqualTo("call");
		assertThat(result.getMetadata()).isEqualTo(original.getMetadata());
	}

	private static ChatResponse response(String text) {
		return new ChatResponse(List.of(new Generation(new AssistantMessage(text))));
	}

}
