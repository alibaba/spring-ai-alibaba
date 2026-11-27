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
package com.alibaba.cloud.ai.graph.agent.interceptors;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import com.alibaba.cloud.ai.graph.agent.interceptor.ModelRequest;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelResponse;
import com.alibaba.cloud.ai.graph.agent.interceptor.toolselection.ToolSelectionInterceptor;
import com.alibaba.cloud.ai.graph.agent.interceptor.toolselection.ToolSelectionInterceptor.FailureBehavior;
import com.alibaba.cloud.ai.graph.agent.tool.ToolCallbackUtils;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ToolSelectionPolicyTest {

	@Test
	void keepsRequiredToolsAndPreservesTheRankOfEligibleTools() {
		ChatModel model = mock(ChatModel.class);
		when(model.call(any(Prompt.class))).thenReturn(answer("{\"tools\":[\"high\",\"unknown\",\"high\",\"middle\",\"low\"]}"));
		var selection = ToolSelectionInterceptor.builder().selectionModel(model).maxTools(3)
			.alwaysInclude("required").build();
		var request = request().availableTools(List.of("low", "middle", "required"))
			.dynamicToolCallbacks(List.of(tool("high", 20))).build();

		assertThat(selected(selection, request)).containsExactly("required", "high", "middle");
	}

	@Test
	void skipsLargeDefinitionsAndKeepsRequiredToolsWithinTheEstimatedBudget() {
		var required = tool("required", 20);
		var compact = tool("compact", 20);
		var large = tool("large", 800);
		int budget = (int) (ToolCallbackUtils.estimateSchemaTokens(required.getToolDefinition())
				+ ToolCallbackUtils.estimateSchemaTokens(compact.getToolDefinition()));
		ChatModel model = mock(ChatModel.class);
		when(model.call(any(Prompt.class))).thenReturn(answer("{\"tools\":[\"large\",\"compact\"]}"));
		var selection = ToolSelectionInterceptor.builder().selectionModel(model).maxToolSchemaTokens(budget)
			.alwaysInclude("required").build();
		var request = request().availableTools(List.of("required", "large", "compact"))
			.options(ToolCallingChatOptions.builder().toolCallbacks(required, large, compact).build()).build();

		assertThat(selected(selection, request)).containsExactly("required", "compact");
	}

	@Test
	void retriesMalformedOutputAndRetainsTheUpstreamSelectionBoundary() {
		ChatModel model = mock(ChatModel.class);
		when(model.call(any(Prompt.class))).thenReturn(answer("{\"tools\":[false]}"),
				answer("{\"tools\":[\"hidden\",\"visible\"]}"));
		var selection = ToolSelectionInterceptor.builder().selectionModel(model).maxSelectionRetries(1).build();
		var request = request().tools(List.of("visible"))
			.dynamicToolCallbacks(List.of(tool("hidden", 20))).build();

		assertThat(selected(selection, request)).containsExactly("visible");
		verify(model, times(2)).call(any(Prompt.class));
	}

	@Test
	void enforcesFailurePoliciesAfterRetriesAreExhausted() {
		ChatModel model = mock(ChatModel.class);
		when(model.call(any(Prompt.class))).thenReturn(answer("{\"tools\":[\"second\"]} trailing"));
		var builder = ToolSelectionInterceptor.builder().selectionModel(model).maxSelectionRetries(0)
			.maxTools(2).alwaysInclude("required");
		var request = request().availableTools(List.of("first", "second", "required")).build();

		assertThat(selected(builder.build(), request)).containsExactly("required", "first");
		assertThat(selected(builder.onSelectionFailure(FailureBehavior.REQUIRED_ONLY).build(), request))
			.containsExactly("required");
		assertThatThrownBy(() -> selected(builder.onSelectionFailure(FailureBehavior.ERROR).build(), request))
			.isInstanceOf(IllegalStateException.class).hasMessage("Could not select request tools");
	}

	private static ModelRequest.Builder request() {
		return ModelRequest.builder().messages(List.of(new UserMessage("Find tools for this task")));
	}

	private static List<String> selected(ToolSelectionInterceptor selection, ModelRequest request) {
		var names = new AtomicReference<List<String>>();
		selection.interceptModel(request, filtered -> {
			names.set(filtered.getTools());
			return ModelResponse.of(new AssistantMessage("done"));
		});
		return names.get();
	}

	private static ChatResponse answer(String text) {
		return new ChatResponse(List.of(new Generation(new AssistantMessage(text))));
	}

	private static ToolCallback tool(String name, int descriptionLength) {
		return FunctionToolCallback.builder(name, args -> "done").description("x".repeat(descriptionLength))
			.inputType(Map.class).build();
	}

}
