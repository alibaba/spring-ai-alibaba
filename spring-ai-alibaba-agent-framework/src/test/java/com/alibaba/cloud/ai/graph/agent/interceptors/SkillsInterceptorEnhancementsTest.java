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

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.hook.skills.ReadSkillTool;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelCallHandler;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelInterceptor;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelRequest;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelResponse;
import com.alibaba.cloud.ai.graph.agent.interceptor.skills.SkillsInterceptor;
import com.alibaba.cloud.ai.graph.agent.node.AgentLlmNode;
import com.alibaba.cloud.ai.graph.agent.tools.PoetTool;
import com.alibaba.cloud.ai.graph.skills.registry.SkillRegistry;
import com.alibaba.cloud.ai.graph.skills.registry.filesystem.FileSystemSkillRegistry;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.ai.tool.resolution.ToolCallbackResolver;
import reactor.core.publisher.Flux;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.List.of;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

	@Test
	void groupedToolsSupplierIsResolvedOnEveryModelCall() {
		ToolCallback recordResultTool = FunctionToolCallback.builder("record_result", args -> "recorded")
				.description("Records a result value")
				.inputType(String.class)
				.build();
		ToolCallback extraTool = FunctionToolCallback.builder("extra_tool", args -> "extra")
				.description("Extra tool added after startup")
				.inputType(String.class)
				.build();

		AtomicReference<Map<String, List<ToolCallback>>> current = new AtomicReference<>(
				Map.of("allowed-tools-test", List.of(recordResultTool)));

		SkillsInterceptor interceptor = SkillsInterceptor.builder()
				.skillRegistry(registry)
				.groupedToolsSupplier(current::get)
				.build();

		Map<String, List<ToolCallback>> first = interceptor.getGroupedTools();
		assertEquals(1, first.get("allowed-tools-test").size());

		// Registry hot-update: a new tool joins the skill's grouped tools.
		current.set(Map.of("allowed-tools-test", List.of(recordResultTool, extraTool)));

		Map<String, List<ToolCallback>> second = interceptor.getGroupedTools();
		assertEquals(2, second.get("allowed-tools-test").size());
		assertTrue(second.get("allowed-tools-test").stream()
				.anyMatch(tool -> "extra_tool".equals(tool.getToolDefinition().name())));
	}

	@Test
	void skillNameToolCallActivatesGroupedToolsAfterFallback() {
		ToolCallback recordResultTool = FunctionToolCallback.builder("record_result", args -> "recorded")
				.description("Records a result value")
				.inputType(String.class)
				.build();
		SkillsInterceptor interceptor = SkillsInterceptor.builder()
				.skillRegistry(registry)
				.groupedTools(Map.of("allowed-tools-test", List.of(recordResultTool)))
				.build();

		AssistantMessage.ToolCall skillCall = new AssistantMessage.ToolCall("call-1", "function",
				"allowed-tools-test", "{}");
		ModelRequest request = ModelRequest.builder()
				.messages(List.of(AssistantMessage.builder().content("").toolCalls(List.of(skillCall)).build()))
				.build();
		AtomicReference<ModelRequest> captured = new AtomicReference<>();

		interceptor.interceptModel(request, modified -> {
			captured.set(modified);
			return ModelResponse.of(new AssistantMessage("ok"));
		});

		assertEquals(List.of("record_result"), captured.get().getDynamicToolCallbacks().stream()
				.map(tool -> tool.getToolDefinition().name())
				.toList());
	}

	@Test
	void sameNamedDynamicToolIsNotTreatedAsSkill() {
		ToolCallback realTool = FunctionToolCallback.builder("allowed-tools-test", args -> "real tool")
				.description("Real tool with a colliding name")
				.inputType(String.class)
				.build();
		SkillsInterceptor interceptor = SkillsInterceptor.builder()
				.skillRegistry(registry)
				.groupedTools(Map.of("allowed-tools-test", List.of(
						FunctionToolCallback.builder("record_result", args -> "recorded")
								.description("Grouped skill tool")
								.inputType(String.class)
								.build())))
				.build();

		ModelRequest request = ModelRequest.builder()
				.messages(List.of(assistantMessageFor("allowed-tools-test")))
				.dynamicToolCallbacks(List.of(realTool))
				.build();
		AtomicReference<ModelRequest> captured = new AtomicReference<>();

		interceptor.interceptModel(request, modified -> {
			captured.set(modified);
			return ModelResponse.of(new AssistantMessage("ok"));
		});

		assertEquals(List.of("allowed-tools-test"), toolNames(captured.get()));
	}

	@Test
	void sameNamedToolFromRuntimeContextIsNotTreatedAsSkill() {
		ToolCallback realTool = FunctionToolCallback.builder("allowed-tools-test", args -> "real tool")
				.description("Real tool from the runtime callback context")
				.inputType(String.class)
				.build();
		SkillsInterceptor interceptor = SkillsInterceptor.builder()
				.skillRegistry(registry)
				.groupedTools(Map.of("allowed-tools-test", List.of(realTool)))
				.build();

		ModelRequest request = ModelRequest.builder()
				.messages(List.of(assistantMessageFor("allowed-tools-test")))
				.context(Map.of(RunnableConfig.DYNAMIC_TOOL_CALLBACKS_METADATA_KEY, List.of(realTool)))
				.build();
		AtomicReference<ModelRequest> captured = new AtomicReference<>();

		interceptor.interceptModel(request, modified -> {
			captured.set(modified);
			return ModelResponse.of(new AssistantMessage("ok"));
		});

		assertEquals(List.of(), toolNames(captured.get()));
	}

	@Test
	void sameNamedToolFromResolverIsNotTreatedAsSkill() {
		ToolCallback realTool = FunctionToolCallback.builder("allowed-tools-test", args -> "real tool")
				.description("Real tool from a resolver")
				.inputType(String.class)
				.build();
		SkillsInterceptor interceptor = SkillsInterceptor.builder()
				.skillRegistry(registry)
				.groupedTools(Map.of("allowed-tools-test", List.of(
						FunctionToolCallback.builder("record_result", args -> "recorded")
								.description("Grouped skill tool")
								.inputType(String.class)
								.build())))
				.toolCallbackResolver(toolName -> "allowed-tools-test".equals(toolName) ? realTool : null)
				.build();

		ModelRequest request = ModelRequest.builder()
				.messages(List.of(assistantMessageFor("allowed-tools-test")))
				.build();
		AtomicReference<ModelRequest> captured = new AtomicReference<>();

		interceptor.interceptModel(request, modified -> {
			captured.set(modified);
			return ModelResponse.of(new AssistantMessage("ok"));
		});

		assertEquals(List.of(), toolNames(captured.get()));
	}

	@Test
	void agentResolverIsAvailableToSkillsInterceptorThroughModelRequest() throws Exception {
		ToolCallback realTool = FunctionToolCallback.builder("allowed-tools-test", args -> "real tool")
				.description("Real tool from the agent resolver")
				.inputType(String.class)
				.build();
		ToolCallbackResolver resolver = toolName -> "allowed-tools-test".equals(toolName) ? realTool : null;
		SkillsInterceptor skillsInterceptor = SkillsInterceptor.builder()
				.skillRegistry(registry)
				.groupedTools(Map.of("allowed-tools-test", List.of(
						FunctionToolCallback.builder("record_result", args -> "recorded")
								.description("Grouped skill tool")
								.inputType(String.class)
								.build())))
				.build();
		AtomicReference<ModelRequest> captured = new AtomicReference<>();
		ModelInterceptor captureInterceptor = new ModelInterceptor() {
			@Override
			public ModelResponse interceptModel(ModelRequest request, ModelCallHandler handler) {
				captured.set(request);
				return ModelResponse.of(new AssistantMessage("ok"));
			}

			@Override
			public String getName() {
				return "CaptureModelRequest";
			}
		};
		AgentLlmNode llmNode = AgentLlmNode.builder()
				.agentName("resolver-test-agent")
				.toolCallbackResolver(resolver)
				.modelInterceptors(List.of(skillsInterceptor, captureInterceptor))
				.build();

		llmNode.apply(new OverAllState(Map.of("messages", List.of(assistantMessageFor("allowed-tools-test")))),
				RunnableConfig.builder().build());

		assertNotNull(captured.get());
		assertSame(resolver, captured.get().getContext().get(RunnableConfig.TOOL_CALLBACK_RESOLVER_METADATA_KEY));
		assertEquals(List.of(), toolNames(captured.get()));
	}

	private AssistantMessage assistantMessageFor(String toolName) {
		AssistantMessage.ToolCall toolCall = new AssistantMessage.ToolCall("call-1", "function", toolName, "{}");
		return AssistantMessage.builder().content("").toolCalls(List.of(toolCall)).build();
	}

	private List<String> toolNames(ModelRequest request) {
		return request.getDynamicToolCallbacks().stream()
				.map(tool -> tool.getToolDefinition().name())
				.toList();
	}

}
