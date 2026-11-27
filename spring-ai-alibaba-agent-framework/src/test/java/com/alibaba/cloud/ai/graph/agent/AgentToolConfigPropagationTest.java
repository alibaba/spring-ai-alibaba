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
import com.alibaba.cloud.ai.graph.agent.context.AgentToolContext;
import com.alibaba.cloud.ai.graph.agent.hook.HookPosition;
import com.alibaba.cloud.ai.graph.agent.hook.HookPositions;
import com.alibaba.cloud.ai.graph.agent.hook.ModelHook;
import com.alibaba.cloud.ai.graph.agent.tools.ToolContextConstants;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.store.stores.MemoryStore;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.util.JsonHelper;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("AgentTool config propagation tests")
class AgentToolConfigPropagationTest {

	@Test
	void isolatesRepeatedToolInvocationsOfTheSameAgent() {
		var capture = new ConfigCaptureHook();
		var worker = ReactAgent.builder().name("worker").model(new FixedResponseChatModel("done"))
				.hooks(List.of(capture)).saver(new MemorySaver()).build();
		var caller = RunnableConfig.builder().threadId("parent").checkPointId("model-step").build();
		var executor = new AgentTool.AgentToolExecutor(worker);
		executor.executeAgent("{\"input\":\"left\"}", new ToolContext(Map.of(
				ToolContextConstants.AGENT_CONFIG_CONTEXT_KEY, caller, ToolContextConstants.AGENT_TOOL_CALL_ID_CONTEXT_KEY, "left")));
		String left = capture.capturedConfig().threadId().orElseThrow();
		executor.executeAgent("{\"input\":\"right\"}", new ToolContext(Map.of(
				ToolContextConstants.AGENT_CONFIG_CONTEXT_KEY, caller, ToolContextConstants.AGENT_TOOL_CALL_ID_CONTEXT_KEY, "right")));
		String right = capture.capturedConfig().threadId().orElseThrow();
		assertNotEquals(left, right);
	}

	@Test
	void onlyForwardsSelectedBusinessMetadata() {
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
				.addMetadata("private_key", "private-value")
				.store(new MemoryStore())
				.build();
		parentConfig.context().put("private_context", "private-value");

		var scope = AgentToolContext.builder().metadataKeys("business_key").build();
		AssistantMessage result = new AgentTool.AgentToolExecutor(subAgent, scope)
				.executeAgent("{\"input\":\"hello\"}",
						new ToolContext(Map.of(ToolContextConstants.AGENT_CONFIG_CONTEXT_KEY, parentConfig)));

		assertEquals("child response", result.getText());

		RunnableConfig capturedConfig = captureHook.capturedConfig();
		assertNotNull(capturedConfig, "Sub-agent should receive a runnable config");
		assertEquals("child_agent", capturedConfig.metadata("_AGENT_").orElse(null),
				"Sub-agent should see its own agent name");
		assertEquals("business-value", capturedConfig.metadata("business_key").orElse(null),
				"Business metadata from parent config should be preserved");
		assertTrue(capturedConfig.metadata("private_key").isEmpty());
		assertFalse(capturedConfig.context().containsKey("private_context"));
		assertNotEquals("parent-thread", capturedConfig.threadId().orElseThrow());
		assertNull(capturedConfig.store());
	}

	@Test
	void delegatesSnapshotDataAndReturnsOnlyTheSelectedOutputsThroughTheToolNode() throws Exception {
		var captured = new AtomicReference<Map<String, Object>>();
		var scope = AgentToolContext.builder().stateKeys("reference").metadataKeys("project")
				.outputKeys("child_answer").build();
		var model = new FixedResponseChatModel("worker answer") {
			@Override
			public ChatResponse call(Prompt prompt) {
				var messages = prompt.getInstructions().stream().filter(UserMessage.class::isInstance)
						.map(Message::getText).toList();
				assertEquals(List.of("Delegated reference data (JSON):\n{\"reference\":{\"title\":\"public\"}}",
						"inspect the reference"), messages);
				return super.call(prompt);
			}
		};
		var capture = new ConfigCaptureHook() {
			@Override
			public CompletableFuture<Map<String, Object>> beforeModel(OverAllState state, RunnableConfig config) {
				captured.set(state.<Map<String, Object>>value(AgentToolContext.STATE_KEY).orElseThrow());
				assertEquals("spring", config.metadata("project").orElseThrow());
				assertTrue(config.metadata("private_metadata").isEmpty());
				assertTrue(state.value("private_state").isEmpty());
				return CompletableFuture.completedFuture(Map.of("private_output", "child-private"));
			}
		};
		var worker = ReactAgent.builder().name("worker").description("Delegate narrow tasks").model(model)
				.outputKey("child_answer").hooks(List.of(capture)).build();
		var parentModel = new FixedResponseChatModel("parent answer") {
			@Override
			public ChatResponse call(Prompt prompt) {
				var result = prompt.getInstructions().stream().filter(ToolResponseMessage.class::isInstance)
						.map(ToolResponseMessage.class::cast).findFirst();
				if (result.isPresent()) {
					var output = result.get().getResponses().get(0).responseData();
					assertEquals(Map.of("answer", "worker answer", "outputs", Map.of("child_answer", "worker answer")),
							new JsonHelper().fromJsonToMap(output));
					return super.call(prompt);
				}
				var tool = new AssistantMessage.ToolCall("delegate", "function", "worker",
						"{\"input\":\"inspect the reference\",\"stateKeys\":[\"private_state\"]}");
				return new ChatResponse(List.of(new Generation(AssistantMessage.builder().toolCalls(List.of(tool)).build())));
			}
		};
		var parent = ReactAgent.builder().name("parent").model(parentModel).tools(AgentTool.create(worker, scope)).build();
		var config = RunnableConfig.builder().threadId("parent").addMetadata("project", "spring")
				.addMetadata("private_metadata", "parent-private").build();
		var reference = new HashMap<>(Map.of("title", "public"));
		var inputs = Map.<String, Object>of("messages", List.of(new UserMessage("parent history")),
				"reference", reference, "private_state", "parent-private");

		var output = parent.invokeAndGetOutput(inputs, config).orElseThrow();

		assertEquals(Map.of("reference", reference), captured.get());
		assertTrue(output.state().value("child_answer").isEmpty());
		assertTrue(output.state().value("private_output").isEmpty());
	}

	@Test
	void isolatesStandaloneCallsThatShareASaver() {
		var model = new FixedResponseChatModel("done") {
			@Override
			public ChatResponse call(Prompt prompt) {
				assertEquals(1, prompt.getInstructions().stream().filter(UserMessage.class::isInstance).count());
				return super.call(prompt);
			}
		};
		var worker = ReactAgent.builder().name("worker").description("Worker").model(model).saver(new MemorySaver()).build();
		var tool = AgentTool.create(worker);
		assertEquals("done", tool.call("{\"input\":\"first\"}"));
		assertEquals("done", tool.call("{\"input\":\"second\"}"));
	}

	@Test
	void rejectsReservedInputsAndOverBudgetContext() {
		assertThrows(IllegalArgumentException.class, () -> AgentToolContext.builder().stateKeys("messages"));
		assertThrows(IllegalArgumentException.class, () -> AgentToolContext.builder().metadataKeys("HUMAN_FEEDBACK"));
		var worker = ReactAgent.builder().name("worker").model(new FixedResponseChatModel("unused")).build();
		var scope = AgentToolContext.builder().stateKeys("reference").maxContextCharacters(5).build();
		var context = new ToolContext(Map.of(ToolContextConstants.AGENT_STATE_CONTEXT_KEY,
				new OverAllState(Map.of("reference", "oversized"))));
		var error = assertThrows(IllegalArgumentException.class, () -> new AgentTool.AgentToolExecutor(worker, scope)
				.executeAgent("{\"input\":\"delegate\"}", context));
		assertTrue(error.getMessage().contains("configured character limit"));
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

		@Override
		public ToolCallingChatOptions getOptions() {
			return ToolCallingChatOptions.builder().build();
		}
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
