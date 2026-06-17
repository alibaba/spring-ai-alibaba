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
package com.alibaba.cloud.ai.graph.agent.interceptors;

import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.hook.skills.ReadSkillTool;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelRequest;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelResponse;
import com.alibaba.cloud.ai.graph.agent.interceptor.skills.SkillsInterceptor;
import com.alibaba.cloud.ai.graph.agent.tools.PoetTool;
import com.alibaba.cloud.ai.graph.agent.node.AgentLlmNode;
import com.alibaba.cloud.ai.graph.skills.registry.SkillRegistry;
import com.alibaba.cloud.ai.graph.skills.registry.filesystem.FileSystemSkillRegistry;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.ai.tool.resolution.ToolCallbackResolver;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import reactor.core.publisher.Flux;

import static java.util.List.of;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.springframework.ai.chat.model.Generation;

class SkillsInterceptorEnhancementsTest {

	static class MockChatModel implements ChatModel {

		@Override
		public ChatResponse call(Prompt prompt) {
			return new ChatResponse(of(new Generation(new AssistantMessage("Mock response"))));
		}

		@Override
		public Flux<ChatResponse> stream(Prompt prompt) {
			return Flux.just(new ChatResponse(of(new Generation(new AssistantMessage("Mock stream response")))));
		}

	}

	@TempDir
	Path tempDir;

	private SkillRegistry registry;

	private Path skillDir;

	@BeforeEach
	void setUp() throws Exception {
		Path skillsDir = tempDir.resolve("skills");
		skillDir = skillsDir.resolve("allowed-tools-test");
		Files.createDirectories(skillDir);
		Files.writeString(skillDir.resolve("SKILL.md"), """
				---
				name: allowed-tools-test
				description: Skill fixture for interceptor enhancement tests.
				allowed_tools:
				  - record_result
				---
				
				# Allowed Tools Test
				
				Interceptor test fixture.
				""");
		registry = FileSystemSkillRegistry.builder().projectSkillsDirectory(skillsDir.toString()).build();
	}

	@Test
	void readingSkillByPathActivatesAllowedToolsAndDeduplicatesDynamicCallbacks() {
		ToolCallback recordResultTool = FunctionToolCallback.builder("record_result", args -> "recorded")
				.description("Records a result value")
				.inputType(String.class)
				.build();
		ToolCallbackResolver resolver = toolName -> "record_result".equals(toolName) ? recordResultTool : null;

		SkillsInterceptor interceptor = SkillsInterceptor.builder()
				.skillRegistry(registry)
				.groupedTools(Map.of("allowed-tools-test", List.of(recordResultTool)))
				.toolCallbackResolver(resolver)
				.build();

		AssistantMessage.ToolCall readSkillCall = new AssistantMessage.ToolCall("call-1", "function",
				ReadSkillTool.READ_SKILL, "{\"skill_path\":\"%s\"}".formatted(skillDir.toString().replace("\\", "\\\\")));
		Message assistantMessage = AssistantMessage.builder().content("").toolCalls(List.of(readSkillCall)).build();
		ModelRequest request = ModelRequest.builder()
				.messages(List.of(assistantMessage))
				.dynamicToolCallbacks(List.of(recordResultTool))
				.context(Map.of())
				.build();

		AtomicReference<ModelRequest> captured = new AtomicReference<>();
		interceptor.interceptModel(request, modified -> {
			captured.set(modified);
			return ModelResponse.of(new AssistantMessage("ok"));
		});

		assertNotNull(captured.get());
		assertEquals(List.of("record_result"),
				captured.get().getDynamicToolCallbacks().stream()
						.map(tool -> tool.getToolDefinition().name())
						.distinct()
						.toList());
		assertEquals(1, captured.get().getDynamicToolCallbacks().size());
	}

	@Test
	void reactAgentBuilderDeduplicatesStaticToolsByName() throws Exception {
		ToolCallback tool1 = PoetTool.createPoetToolCallback("duplicate_tool", new PoetTool());
		ToolCallback tool2 = PoetTool.createPoetToolCallback("duplicate_tool", new PoetTool());

		ReactAgent agent = ReactAgent.builder()
				.name("dedup-agent")
				.model(new MockChatModel())
				.tools(tool1, tool2)
				.build();

		Field llmNodeField = ReactAgent.class.getDeclaredField("llmNode");
		llmNodeField.setAccessible(true);
		AgentLlmNode llmNode = (AgentLlmNode) llmNodeField.get(agent);

		Field toolCallbacksField = AgentLlmNode.class.getDeclaredField("toolCallbacks");
		toolCallbacksField.setAccessible(true);
		@SuppressWarnings("unchecked")
		List<ToolCallback> toolCallbacks = (List<ToolCallback>) toolCallbacksField.get(llmNode);

		assertEquals(1, toolCallbacks.size());
		assertEquals("duplicate_tool", toolCallbacks.get(0).getToolDefinition().name());
	}

}
