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
package com.alibaba.cloud.ai.graph.agent;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.hook.HookPosition;
import com.alibaba.cloud.ai.graph.agent.hook.HookPositions;
import com.alibaba.cloud.ai.graph.agent.hook.ModelHook;
import com.alibaba.cloud.ai.graph.agent.hook.skills.SkillsAgentHook;
import com.alibaba.cloud.ai.graph.agent.tools.ToolContextConstants;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.skills.registry.SkillRegistry;
import com.alibaba.cloud.ai.graph.skills.registry.filesystem.FileSystemSkillRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import reactor.core.publisher.Flux;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("AgentTool config propagation tests")
class AgentToolConfigPropagationTest {

	@Test
	@DisplayName("AgentTool should override _AGENT_ for sub-agent and preserve business metadata")
	void shouldOverrideAgentMetadataAndPreserveBusinessMetadata() {
		ConfigCaptureHook captureHook = new ConfigCaptureHook();
		ReactAgent subAgent = ReactAgent.builder()
				.name("child_agent")
				.model(new FixedResponseChatModel("child response"))
				.hooks(List.of(captureHook))
				.saver(new MemorySaver())
				.build();

		RunnableConfig parentConfig = RunnableConfig.builder()
				.threadId("parent-thread")
				.addMetadata("_AGENT_", "parent_agent")
				.addMetadata("business_key", "business-value")
				.build();

		AssistantMessage result = new AgentTool.AgentToolExecutor(subAgent)
				.executeAgent("{\"input\":\"hello\"}",
						new ToolContext(Map.of(ToolContextConstants.AGENT_CONFIG_CONTEXT_KEY, parentConfig)));

		assertEquals("child response", result.getText());

		RunnableConfig capturedConfig = captureHook.capturedConfig();
		assertNotNull(capturedConfig, "Sub-agent should receive a runnable config");
		assertEquals("child_agent", capturedConfig.metadata("_AGENT_").orElse(null),
				"Sub-agent should see its own agent name");
		assertEquals("business-value", capturedConfig.metadata("business_key").orElse(null),
				"Business metadata from parent config should be preserved");
		assertEquals("parent-thread_child_agent", capturedConfig.threadId().orElse(null),
				"Sub-agent thread id should be derived from the parent thread id");
	}

	@Test
	@DisplayName("AgentTool should include context when sub-agent invocation fails")
	void shouldIncludeContextInInvocationFailureMessage() {
		ReactAgent failingSubAgent = ReactAgent.builder()
				.name("failing_child_agent")
				.model(new FixedResponseChatModel("unused"))
				.hooks(List.of(new FailingHook()))
				.saver(new MemorySaver())
				.build();

		RunnableConfig parentConfig = RunnableConfig.builder()
				.threadId("parent-thread")
				.addMetadata("business_key", "business-value")
				.build();

		RuntimeException exception = assertThrows(RuntimeException.class,
				() -> new AgentTool.AgentToolExecutor(failingSubAgent)
						.executeAgent("{\"input\":\"hello\"}",
								new ToolContext(Map.of(ToolContextConstants.AGENT_CONFIG_CONTEXT_KEY, parentConfig))));

		assertTrue(exception.getMessage().contains("Failed to execute agent tool 'failing_child_agent'"));
		assertTrue(exception.getMessage().contains("parentThreadId=parent-thread"));
		assertTrue(exception.getMessage().contains("input=hello"));
	}

	@Test
	@DisplayName("AgentTool sub-agent should execute tools activated by read_skill")
	void shouldExecuteDynamicSkillToolInSubAgent() throws Exception {
		AtomicReference<String> recordedValue = new AtomicReference<>();
		ToolCallback recordResultTool = FunctionToolCallback
				.builder("record_result",
						(RecordResultRequest request, ToolContext context) -> {
							recordedValue.set(request.value);
							return "recorded";
						})
				.description("Records a result value")
				.inputType(RecordResultRequest.class)
				.build();
		SkillRegistry registry = FileSystemSkillRegistry.builder()
				.projectSkillsDirectory(Path.of("src/test/resources/skills").toAbsolutePath().toString())
				.build();
		SkillsAgentHook skillsHook = SkillsAgentHook.builder()
				.skillRegistry(registry)
				.groupedTools(Map.of("grouped-tools-test", List.of(recordResultTool)))
				.build();
		ReactAgent subAgent = ReactAgent.builder()
				.name("skill_child_agent")
				.description("Executes tasks using dynamically activated skill tools")
				.model(new SkillToolCallingChatModel())
				.hooks(List.of(skillsHook))
				.build();
		ReactAgent parentAgent = ReactAgent.builder()
				.name("streaming_parent_agent")
				.model(new ParentToolCallingChatModel())
				.tools(AgentTool.create(subAgent))
				.build();

		RunnableConfig parentConfig = RunnableConfig.builder().threadId("parent-thread").build();
		assertNotNull(parentAgent.stream("delegate this task", parentConfig).blockLast());

		assertEquals("nested-value", recordedValue.get(),
				"Tool activated by read_skill should remain available to the sub-agent tool node");
	}

	private static class FixedResponseChatModel implements ChatModel {

		private final String responseText;

		private FixedResponseChatModel(String responseText) {
			this.responseText = responseText;
		}

		@Override
		public ChatResponse call(Prompt prompt) {
			return new ChatResponse(List.of(new Generation(new AssistantMessage(responseText))));
		}

		@Override
		public Flux<ChatResponse> stream(Prompt prompt) {
			return Flux.just(call(prompt));
		}
	}

	private static class SkillToolCallingChatModel implements ChatModel {

		private final AtomicInteger callCount = new AtomicInteger();

		@Override
		public ChatResponse call(Prompt prompt) {
			AssistantMessage response = switch (callCount.getAndIncrement()) {
				case 0 -> AssistantMessage.builder()
						.content("")
						.toolCalls(List.of(new AssistantMessage.ToolCall("read-skill", "function", "read_skill",
								"{\"skill_name\":\"grouped-tools-test\"}")))
						.build();
				case 1 -> AssistantMessage.builder()
						.content("")
						.toolCalls(List.of(new AssistantMessage.ToolCall("record-result", "function", "record_result",
								"{\"value\":\"nested-value\"}")))
						.build();
				default -> new AssistantMessage("done");
			};
			return new ChatResponse(List.of(new Generation(response)));
		}

		@Override
		public Flux<ChatResponse> stream(Prompt prompt) {
			return Flux.just(call(prompt));
		}
	}

	private static class ParentToolCallingChatModel implements ChatModel {

		private final AtomicInteger callCount = new AtomicInteger();

		@Override
		public ChatResponse call(Prompt prompt) {
			return response();
		}

		@Override
		public Flux<ChatResponse> stream(Prompt prompt) {
			return Flux.just(response());
		}

		private ChatResponse response() {
			AssistantMessage response = callCount.getAndIncrement() == 0
					? AssistantMessage.builder()
							.content("")
							.toolCalls(List.of(new AssistantMessage.ToolCall("delegate", "function",
									"skill_child_agent", "{\"input\":\"record nested-value\"}")))
							.build()
					: new AssistantMessage("parent done");
			return new ChatResponse(List.of(new Generation(response)));
		}
	}

	private static class RecordResultRequest {

		public String value;
	}

	@HookPositions(HookPosition.BEFORE_MODEL)
	private static class ConfigCaptureHook extends ModelHook {

		private final AtomicReference<RunnableConfig> capturedConfig = new AtomicReference<>();

		@Override
		public CompletableFuture<Map<String, Object>> beforeModel(OverAllState state, RunnableConfig config) {
			capturedConfig.set(config);
			return CompletableFuture.completedFuture(Map.of());
		}

		RunnableConfig capturedConfig() {
			return capturedConfig.get();
		}

		@Override
		public String getName() {
			return "ConfigCaptureHook";
		}
	}

	@HookPositions(HookPosition.BEFORE_MODEL)
	private static class FailingHook extends ModelHook {

		@Override
		public CompletableFuture<Map<String, Object>> beforeModel(OverAllState state, RunnableConfig config) {
			throw new IllegalStateException("boom");
		}

		@Override
		public String getName() {
			return "FailingHook";
		}
	}

}
