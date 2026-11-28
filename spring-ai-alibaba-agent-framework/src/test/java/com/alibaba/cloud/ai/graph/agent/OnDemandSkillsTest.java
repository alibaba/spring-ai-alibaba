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
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.hook.skills.SkillsAgentHook;
import com.alibaba.cloud.ai.graph.agent.interceptor.toolselection.ToolSelectionInterceptor;
import com.alibaba.cloud.ai.graph.checkpoint.savers.file.FileSystemSaver;
import com.alibaba.cloud.ai.graph.skills.SkillMetadata;
import com.alibaba.cloud.ai.graph.skills.registry.SkillRegistry;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OnDemandSkillsTest {

	@Test
	void searchesDescriptorsAndActivatesToolsOnlyAfterAValidReadInTheCurrentExecution(@TempDir Path directory)
			throws Exception {
		var skill = SkillMetadata.builder().name("reports").description("Monthly report instructions")
			.skillPath("/private/registry/reports").build();
		var other = SkillMetadata.builder().name("unused").description("Another skill")
			.skillPath("/private/registry/unused").build();
		var registry = mock(SkillRegistry.class);
		when(registry.search("monthly")).thenReturn(List.of(skill, other));
		when(registry.get("reports")).thenReturn(Optional.of(skill));
		when(registry.readSkillContent("reports")).thenReturn("abc📊def");
		var hook = SkillsAgentHook.builder().skillRegistry(registry).onDemand(1, 4)
			.groupedTools(Map.of("reports", List.of(tool("run_report")))).build();
		var rounds = new AtomicInteger();
		var model = new StubModel(prompt -> {
			var names = ((ToolCallingChatOptions) prompt.getOptions()).getToolCallbacks().stream()
				.map(callback -> callback.getToolDefinition().name()).toList();
			assertThat(names).contains("search_skills", "read_skill");
			assertThat(prompt.getSystemMessage().getText()).contains("search_skills")
				.doesNotContain("Monthly report instructions", "/private/");
			return switch (rounds.getAndIncrement()) {
				case 0 -> {
					assertThat(names).doesNotContain("run_report");
					verify(registry, never()).listAll();
					yield call("search", "search_skills", "{\"query\":\"monthly\"}");
				}
				case 1 -> {
					assertThat(names).doesNotContain("run_report");
					assertThat(lastResult(prompt)).contains("reports", "Monthly report instructions")
						.doesNotContain("/private/", "unused", "abc");
					yield call("invalid", "read_skill", "{\"skill_name\":\"reports\",\"offset\":4}");
				}
				case 2 -> {
					assertThat(names).doesNotContain("run_report");
					assertThat(lastResult(prompt)).contains("Invalid skill read request");
					yield call("read", "read_skill", "{\"skill_name\":\"reports\"}");
				}
				case 3 -> {
					assertThat(names).contains("run_report").doesNotContain("other_tool");
					assertThat(lastResult(prompt)).contains("abc", "\"nextOffset\":3");
					yield call("more", "read_skill", "{\"skill_name\":\"reports\",\"offset\":3}");
				}
				case 4 -> {
					assertThat(lastResult(prompt)).contains("📊de", "\"nextOffset\":7");
					yield answer("read complete");
				}
				case 5 -> {
					assertThat(names).doesNotContain("run_report");
					yield answer("new task");
				}
				default -> throw new AssertionError("Unexpected model request");
			};
		});
		var selector = ToolSelectionInterceptor.builder().maxTools(1)
			.selectionModel(prompt -> answer("{\"tools\":[\"run_report\"]}")).build();
		var agent = ReactAgent.builder().name("skills").model(model).hooks(List.of(hook)).interceptors(selector)
			.tools(tool("other_tool")).saver(FileSystemSaver.builder().targetFolder(directory).build()).build();
		var config = RunnableConfig.builder().threadId("skills").build();

		assertThat(agent.call("find monthly instructions", config).getText()).isEqualTo("read complete");
		assertThat(agent.call("new task", config).getText()).isEqualTo("new task");
		verify(registry, never()).listAll();
		verify(registry, times(3)).readSkillContent("reports");
	}

	@Test
	void activatesGroupedToolsForMultipleSkillsReadInOneSequentialTurn() throws Exception {
		var reports = SkillMetadata.builder().name("reports").description("Report instructions")
			.skillPath("/private/registry/reports").build();
		var charts = SkillMetadata.builder().name("charts").description("Chart instructions")
			.skillPath("/private/registry/charts").build();
		var registry = mock(SkillRegistry.class);
		when(registry.get("reports")).thenReturn(Optional.of(reports));
		when(registry.get("charts")).thenReturn(Optional.of(charts));
		when(registry.readSkillContent("reports")).thenReturn("report skill");
		when(registry.readSkillContent("charts")).thenReturn("chart skill");
		var hook = SkillsAgentHook.builder().skillRegistry(registry).onDemand(2, 64)
			.groupedTools(Map.of("reports", List.of(tool("run_report")), "charts", List.of(tool("draw_chart")))).build();
		var rounds = new AtomicInteger();
		var model = new StubModel(prompt -> {
			var names = ((ToolCallingChatOptions) prompt.getOptions()).getToolCallbacks().stream()
				.map(callback -> callback.getToolDefinition().name()).toList();
			return switch (rounds.getAndIncrement()) {
				case 0 -> {
					assertThat(names).doesNotContain("run_report", "draw_chart");
					yield new ChatResponse(List.of(new Generation(AssistantMessage.builder()
						.toolCalls(List.of(
								new AssistantMessage.ToolCall("r1", "function", "read_skill", "{\"skill_name\":\"reports\"}"),
								new AssistantMessage.ToolCall("r2", "function", "read_skill", "{\"skill_name\":\"charts\"}")))
						.build())));
				}
				case 1 -> {
					assertThat(names).contains("run_report", "draw_chart");
					yield answer("both skills active");
				}
				default -> throw new AssertionError("Unexpected model request");
			};
		});
		var agent = ReactAgent.builder().name("multi-skills").model(model).hooks(List.of(hook)).build();

		assertThat(agent.call("load both skills").getText()).isEqualTo("both skills active");
	}

	private static ToolCallback tool(String name) {
		return FunctionToolCallback.builder(name, input -> "done").description("Example " + name)
			.inputType(Map.class).build();
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
