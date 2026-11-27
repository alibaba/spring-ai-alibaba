/*
 * Copyright 2026-2027 the original author or authors.
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

import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.action.InterruptionMetadata;
import com.alibaba.cloud.ai.graph.action.InterruptionMetadata.ToolFeedback;
import com.alibaba.cloud.ai.graph.action.InterruptionMetadata.ToolFeedback.FeedbackResult;
import com.alibaba.cloud.ai.graph.agent.hook.hip.HumanInTheLoopHook;
import com.alibaba.cloud.ai.graph.agent.hook.toolcalllimit.ToolCallLimitHook;
import com.alibaba.cloud.ai.graph.agent.hook.toolcalllimit.ToolCallLimitExceededException;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelCallHandler;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelInterceptor;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelRequest;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelResponse;
import com.alibaba.cloud.ai.graph.agent.tools.ToolContextHelper;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.utils.FinishReasonMetadata;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.advisor.ToolCallingAdvisor;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.part.OpaquePayload;
import org.springframework.ai.chat.messages.part.ReasoningPart;
import org.springframework.ai.chat.messages.part.ToolCallPart;
import org.springframework.ai.chat.messages.part.ToolResultPart;
import org.springframework.ai.chat.metadata.ChatGenerationMetadata;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.FinishReasonCategory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.core.Ordered;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgentToolCallingAdvisorBoundaryTest {

	@Test
	void toolCallsShouldBeExecutedByAgentToolNodeNotChatClientAutoAdvisor() throws Exception {
		ToolCallThenFinalChatModel chatModel = new ToolCallThenFinalChatModel();
		BoundaryTools tools = new BoundaryTools();
		ToolCallback toolCallback = ToolCallbacks.from(tools)[0];

		var client = ChatClient.builder(chatModel).defaultAdvisors(ToolCallingAdvisor.builder().build()).build();
		ReactAgent agent = ReactAgent.builder()
				.name("tool-boundary-agent")
				.chatClient(client)
				.tools(toolCallback)
				.saver(new MemorySaver())
				.build();

		AssistantMessage result = agent.call("call the boundary tool",
				RunnableConfig.builder().threadId("tool-boundary-thread").addMetadata("_stream_", false).build());

		assertEquals("final response", result.getText());
		assertEquals(2, chatModel.callCount.get());
		assertEquals(1, tools.agentToolNodeExecutions.get());
		assertEquals(0, tools.chatClientAdvisorExecutions.get());
	}

	@Test
	void toolOnlyAgentShouldNotReplaceProviderOptionsWithGenericToolOptions() throws Exception {
		AtomicReference<ModelRequest> capturedRequest = new AtomicReference<>();
		ToolCallback toolCallback = ToolCallbacks.from(new BoundaryTools())[0];
		ReactAgent agent = ReactAgent.builder()
				.name("provider-options-agent")
				.model(new FixedResponseChatModel("provider response"))
				.tools(toolCallback)
				.interceptors(new CapturingModelInterceptor(capturedRequest))
				.saver(new MemorySaver())
				.build();

		AssistantMessage result = agent.call("use available tools if needed",
				RunnableConfig.builder().threadId("provider-options-thread").addMetadata("_stream_", false).build());

		assertEquals("provider response", result.getText());
		assertNull(capturedRequest.get().getOptions());
	}

	@Test
	void resumesWithRejectedAndEditedCallsInTheSameBatch() throws Exception {
		var tools = new BoundaryTools();
		var chatModel = new BatchToolChatModel();
		var approval = HumanInTheLoopHook.builder().approvalOn("boundary_tool", "Review this tool").build();
		var agent = ReactAgent.builder().name("approval-batch").model(chatModel)
				.tools(ToolCallbacks.from(tools)).hooks(List.of(approval)).toolCallLimits(1, 1)
				.saver(new MemorySaver()).build();
		var config = RunnableConfig.builder().threadId("approval-batch").addMetadata("_stream_", false).build();

		var interrupted = assertInstanceOf(InterruptionMetadata.class,
				agent.invokeAndGetOutput("review this batch", config).orElseThrow());
		assertEquals(2, interrupted.toolFeedbacks().size());
		assertEquals(0, tools.agentToolNodeExecutions.get());

		var decisions = InterruptionMetadata.builder(interrupted)
				.toolFeedbacks(List.of(
						ToolFeedback.builder(interrupted.toolFeedbacks().get(0))
								.result(FeedbackResult.REJECTED).description("denied").build(),
						ToolFeedback.builder(interrupted.toolFeedbacks().get(1))
								.result(FeedbackResult.EDITED).arguments("{\"edited\":true}").build()))
				.build();
		var resumed = RunnableConfig.builder().threadId("approval-batch").addMetadata("_stream_", false)
				.addHumanFeedback(decisions).build();
		agent.invokeAndGetOutput("", resumed).orElseThrow();
		assertEquals(1, tools.agentToolNodeExecutions.get());

		var assistant = chatModel.followup.get().getInstructions().stream()
				.filter(AssistantMessage.class::isInstance).map(AssistantMessage.class::cast).findFirst().orElseThrow();
		assertEquals(List.of("{}", "{\"edited\":true}"),
				assistant.getToolCalls().stream().map(AssistantMessage.ToolCall::arguments).toList());
		assertEquals(List.of(ReasoningPart.of("reasoning")), assistant.getReasoning());
		assertEquals(BatchToolChatModel.SIGNATURE, ((ToolCallPart) assistant.getParts().get(2)).payload());
		var toolMessage = chatModel.followup.get().getInstructions().stream()
				.filter(ToolResponseMessage.class::isInstance).map(ToolResponseMessage.class::cast).findFirst().orElseThrow();
		assertEquals(List.of("first", "second"), toolMessage.getResults().stream().map(ToolResultPart::id).toList());
		assertEquals(List.of(true, false), toolMessage.getResults().stream().map(ToolResultPart::isError).toList());
	}

	@Test
	void rejectsTheWholeOverBudgetBatchBeforeExecutingAnyTool() throws Exception {
		var tools = new BoundaryTools();
		var hook = ToolCallLimitHook.builder().runLimit(1).build();
		var agent = ReactAgent.builder().name("limit-batch").model(new BatchToolChatModel())
				.tools(ToolCallbacks.from(tools)).hooks(List.of(hook)).saver(new MemorySaver()).build();
		var config = RunnableConfig.builder().threadId("limit-batch").addMetadata("_stream_", false).build();
		assertTrue(agent.call("request two calls", config).getText().contains("run limit (2/1)"));
		assertEquals(0, tools.agentToolNodeExecutions.get());
	}

	@Test
	void reportsAnOverBudgetBatchWithErrorBehavior() throws Exception {
		var tools = new BoundaryTools();
		var hook = ToolCallLimitHook.builder().threadLimit(1)
				.exitBehavior(ToolCallLimitHook.ExitBehavior.ERROR).build();
		var agent = ReactAgent.builder().name("limit-error").model(new BatchToolChatModel())
				.tools(ToolCallbacks.from(tools)).hooks(List.of(hook)).saver(new MemorySaver()).build();
		var config = RunnableConfig.builder().threadId("limit-error").addMetadata("_stream_", false).build();
		assertThrows(ToolCallLimitExceededException.class, () -> agent.call("request two calls", config));
		assertEquals(0, tools.agentToolNodeExecutions.get());
	}

	@Test
	void toolMetadataRequiresConfirmationWithoutAnExplicitHook() throws Exception {
		var tools = new ConfirmationTools();
		var agent = ReactAgent.builder().name("metadata-approval").model(new BatchToolChatModel())
				.tools(ToolCallbacks.from(tools)).saver(new MemorySaver()).build();
		var config = RunnableConfig.builder().threadId("metadata-approval").addMetadata("_stream_", false).build();

		var interruption = assertInstanceOf(InterruptionMetadata.class,
				agent.invokeAndGetOutput("request a review", config).orElseThrow());
		assertEquals(0, tools.executions.get());
		var decisions = InterruptionMetadata.builder(interruption).toolFeedbacks(interruption.toolFeedbacks().stream()
				.map(decision -> ToolFeedback.builder(decision).result(FeedbackResult.APPROVED).build()).toList()).build();
		agent.invokeAndGetOutput("", RunnableConfig.builder(config).addHumanFeedback(decisions).build()).orElseThrow();
		assertEquals(2, tools.executions.get());
	}

	@Test
	void resetsTheRunLimitForANewTurnWithTheSameConfig() throws Exception {
		var tools = new BoundaryTools();
		var hook = ToolCallLimitHook.builder().runLimit(2).build();
		var agent = ReactAgent.builder().name("run-reset").model(alwaysTwoToolCalls())
				.tools(ToolCallbacks.from(tools)).hooks(List.of(hook)).saver(new MemorySaver()).build();
		var config = RunnableConfig.builder().threadId("run-reset").addMetadata("_stream_", false).build();

		assertTrue(agent.call("first batch", config).getText().contains("run limit (2/2)"));
		assertEquals(2, tools.agentToolNodeExecutions.get());
		assertTrue(agent.call("second batch", config).getText().contains("run limit (2/2)"));
		assertEquals(4, tools.agentToolNodeExecutions.get());
	}

	@Test
	void preservesTheThreadLimitWithAFreshConfigOnTheSameCheckpointThread() throws Exception {
		var tools = new BoundaryTools();
		var hook = ToolCallLimitHook.builder().threadLimit(3).build();
		var agent = ReactAgent.builder().name("thread-budget").model(alwaysTwoToolCalls())
				.tools(ToolCallbacks.from(tools)).hooks(List.of(hook)).saver(new MemorySaver()).build();

		var firstConfig = RunnableConfig.builder().threadId("thread-budget").addMetadata("_stream_", false).build();
		assertTrue(agent.call("first batch", firstConfig).getText().contains("thread limit (4/3)"));
		assertEquals(2, tools.agentToolNodeExecutions.get());

		var freshConfig = RunnableConfig.builder().threadId("thread-budget").addMetadata("_stream_", false).build();
		assertTrue(agent.call("second batch", freshConfig).getText().contains("thread limit (4/3)"));
		assertEquals(2, tools.agentToolNodeExecutions.get());
	}

	@ParameterizedTest
	@NullSource
	@ValueSource(strings = "duplicate")
	void separatesApprovalForMissingOrRepeatedCallIds(String providerId) throws Exception {
		var tools = new ConfirmationTools();
		var model = new BatchToolChatModel(providerId, providerId);
		var agent = ReactAgent.builder().name("positional-approval").model(model).tools(ToolCallbacks.from(tools))
				.saver(new MemorySaver()).build();
		var config = RunnableConfig.builder().threadId("positional-approval").addMetadata("_stream_", false).build();

		var interruption = assertInstanceOf(InterruptionMetadata.class,
				agent.invokeAndGetOutput("review both calls", config).orElseThrow());
		var pending = interruption.toolFeedbacks();
		assertEquals(List.of("#0", "#1"), pending.stream().map(ToolFeedback::getId).toList());
		var decisions = InterruptionMetadata.builder(interruption).toolFeedbacks(List.of(
				ToolFeedback.builder(pending.get(0)).result(FeedbackResult.APPROVED).build(),
				ToolFeedback.builder(pending.get(1)).result(FeedbackResult.REJECTED).description("denied").build())).build();
		agent.invokeAndGetOutput("", RunnableConfig.builder(config).addHumanFeedback(decisions).build()).orElseThrow();

		assertEquals(1, tools.executions.get());
		var result = model.followup.get().getInstructions().stream().filter(ToolResponseMessage.class::isInstance)
				.map(ToolResponseMessage.class::cast).findFirst().orElseThrow();
		assertEquals(List.of(false, true), result.getResults().stream().map(ToolResultPart::isError).toList());
		result.getResults().forEach(part -> assertEquals(providerId, part.id()));
	}

	@ParameterizedTest
	@ValueSource(booleans = { true, false })
	void exposesTheFinishReasonAndSkipsToolsInFilteredResponses(boolean stream) throws Exception {
		var tools = new ConfirmationTools();
		var model = new ChatModel() {

			@Override
			public ChatResponse call(Prompt prompt) {
				var call = new AssistantMessage.ToolCall("blocked", "function", "boundary_tool", "{}");
				var message = AssistantMessage.builder().content("filtered").toolCalls(List.of(call)).build();
				var metadata = ChatGenerationMetadata.builder().finishReason("provider_refusal")
						.finishReasonCategory(FinishReasonCategory.FILTERED).build();
				return new ChatResponse(List.of(new Generation(message, metadata)));
			}

			@Override
			public Flux<ChatResponse> stream(Prompt prompt) {
				return Flux.just(call(prompt));
			}

		};
		var agent = ReactAgent.builder().name("filtered-agent").model(model).tools(ToolCallbacks.from(tools))
				.saver(new MemorySaver()).build();
		var config = RunnableConfig.builder().threadId("filtered-agent").addMetadata("_stream_", stream).build();

		var answer = agent.call("run the tool", config);
		assertEquals("filtered", answer.getText());
		assertEquals("provider_refusal", answer.getMetadata().get(FinishReasonMetadata.FINISH_REASON));
		assertEquals("FILTERED", answer.getMetadata().get(FinishReasonMetadata.CATEGORY));
		assertEquals(0, tools.executions.get());
	}

	@ParameterizedTest
	@ValueSource(booleans = { true, false })
	void blocksToolsOutsideTheOutgoingSelection(boolean stream) throws Exception {
		for (List<String> selection : List.of(List.of("allowed"), List.<String>of())) {
			var tools = new ScopedTools();
			var callbacks = List.of(ToolCallbacks.from(tools));
			var excluded = callbacks.stream().filter(tool -> tool.getToolDefinition().name().equals("excluded"))
					.findFirst().orElseThrow();
			var model = new ScriptedToolModel(List.of(List.of(toolCall("1", "excluded"))));
			var agent = ReactAgent.builder().name("request-tool-scope").model(model).tools(callbacks)
					.resolver(name -> excluded)
					.interceptors(new ModelInterceptor() {
						@Override
						public ModelResponse interceptModel(ModelRequest request, ModelCallHandler handler) {
							return handler.call(ModelRequest.builder(request).tools(selection).build());
						}

						@Override
						public String getName() {
							return "select-tools";
						}
					}).build();

			runAgent(agent, stream);
			assertEquals(selection, promptToolNames(model, 0));
			assertEquals(0, tools.excludedCalls.get());
			assertTrue(toolResults(model, 1).get(0).isError());
		}
	}

	@ParameterizedTest
	@ValueSource(booleans = { true, false })
	void checksToolsAfterTheLastChatClientAdvisor(boolean stream) throws Exception {
		var tools = new ScopedTools();
		var callbacks = List.of(ToolCallbacks.from(tools));
		var local = callbacks.stream().filter(tool -> tool.getToolDefinition().name().equals("excluded"))
				.findFirst().orElseThrow();
		var clientTool = callbacks.stream().filter(tool -> tool.getToolDefinition().name().equals("allowed"))
				.findFirst().orElseThrow();
		var model = new ScriptedToolModel(List.of(List.of(toolCall("1", "excluded"), toolCall("2", "allowed"))));
		var client = ChatClient.builder(model).defaultTools(clientTool).defaultAdvisors(new BaseAdvisor() {
			@Override
			public ChatClientRequest before(ChatClientRequest request, AdvisorChain chain) {
				var options = (ToolCallingChatOptions) request.prompt().getOptions();
				assertEquals(List.of("allowed", "excluded"), options.getToolCallbacks().stream()
						.map(tool -> tool.getToolDefinition().name()).sorted().toList());
				return new ChatClientRequest(new Prompt(request.prompt().getInstructions(),
						options.mutate().toolCallbacks(List.of(clientTool)).build()), request.context());
			}

			@Override
			public ChatClientResponse after(ChatClientResponse response, AdvisorChain chain) {
				return response;
			}

			@Override
			public int getOrder() {
				return Ordered.LOWEST_PRECEDENCE - 1;
			}

			@Override
			public String getName() {
				return "remove-tool-before-model";
			}
		}).build();
		var agent = ReactAgent.builder().name("advisor-tool-scope").chatClient(client).tools(local).build();

		runAgent(agent, stream);
		assertEquals(List.of("allowed"), promptToolNames(model, 0));
		assertEquals(0, tools.excludedCalls.get());
		assertEquals(1, tools.allowedCalls.get());
		assertEquals(List.of(true, false), toolResults(model, 1).stream().map(ToolResultPart::isError).toList());
	}

	@Test
	void appliesTheSameScopeToToolCallsCarriedInStreamMetadata() throws Exception {
		var tools = new ScopedTools();
		var model = new ScriptedToolModel(List.of(List.of(toolCall("1", "excluded")))) {
			@Override
			public Flux<ChatResponse> stream(Prompt prompt) {
				var response = call(prompt);
				var calls = response.getResult().getOutput().getToolCalls();
				if (calls.isEmpty()) {
					return Flux.just(response);
				}
				return Flux.just(new ChatResponse(List.of(new Generation(new AssistantMessage("")))),
						new ChatResponse(List.of(), ChatResponseMetadata.builder().keyValue("toolCalls", calls).build()));
			}
		};
		var allowed = List.of(ToolCallbacks.from(tools)).stream()
				.filter(tool -> tool.getToolDefinition().name().equals("allowed")).findFirst().orElseThrow();
		var agent = ReactAgent.builder().name("metadata-tool-scope").model(model).tools(allowed)
				.resolver(name -> List.of(ToolCallbacks.from(tools)).stream()
						.filter(tool -> tool.getToolDefinition().name().equals(name)).findFirst().orElse(null))
				.build();

		runAgent(agent, true);
		assertEquals(0, tools.excludedCalls.get());
		assertTrue(toolResults(model, 1).get(0).isError());
	}

	@ParameterizedTest
	@ValueSource(booleans = { true, false })
	void appliesDefaultPerToolLimitInSequentialAndParallelBatches(boolean parallel) throws Exception {
		var tools = new ScopedTools();
		var calls = IntStream.rangeClosed(1, 41).mapToObj(i -> toolCall("call-" + i, "allowed")).toList();
		var model = new ScriptedToolModel(List.of(calls));
		var agent = ReactAgent.builder().name("default-tool-budget").model(model).tools(ToolCallbacks.from(tools))
				.parallelToolExecution(parallel).build();

		runAgent(agent, false);
		assertEquals(40, tools.allowedCalls.get());
		assertEquals(1, toolResults(model, 1).stream().filter(ToolResultPart::isError).count());
	}

	@Test
	void carriesTheTurnBudgetThroughApprovalOnAFreshConfig() throws Exception {
		var tools = new ScopedTools();
		var approval = new ConfirmationTools();
		var model = new ScriptedToolModel(List.of(List.of(toolCall("first", "allowed")),
				List.of(toolCall("second", "boundary_tool"))));
		var agent = ReactAgent.builder().name("approval-budget").model(model)
				.tools(ToolCallbacks.from(tools)).tools(ToolCallbacks.from(approval)).toolCallLimits(1, 1)
				.saver(new MemorySaver()).build();
		var config = RunnableConfig.builder().threadId("approval-budget").addMetadata("_stream_", false).build();
		var interruption = assertInstanceOf(InterruptionMetadata.class,
				agent.invokeAndGetOutput("call two tools", config).orElseThrow());
		var decisions = InterruptionMetadata.builder(interruption)
				.toolFeedbacks(List.of(ToolFeedback.builder(interruption.toolFeedbacks().get(0))
						.result(FeedbackResult.APPROVED).build()))
				.build();
		var resumed = RunnableConfig.builder().threadId("approval-budget").addMetadata("_stream_", false)
				.addHumanFeedback(decisions).build();

		agent.invokeAndGetOutput("", resumed).orElseThrow();
		assertEquals(1, tools.allowedCalls.get());
		assertEquals(0, approval.executions.get());
		assertTrue(toolResults(model, 2).get(0).isError());
		assertTrue(toolResults(model, 2).get(0).toToolResponse().responseData().contains("Total tool call limit (1)"));
	}

	private static void runAgent(ReactAgent agent, boolean stream) throws Exception {
		if (stream) {
			agent.stream("exercise the tool boundary", RunnableConfig.builder().build())
					.blockLast(Duration.ofSeconds(10));
		}
		else {
			assertEquals("final response", agent.call("exercise the tool boundary").getText());
		}
	}

	private static AssistantMessage.ToolCall toolCall(String id, String name) {
		return new AssistantMessage.ToolCall(id, "function", name, "{}");
	}

	private static List<String> promptToolNames(ScriptedToolModel model, int index) {
		return ((ToolCallingChatOptions) model.prompts.get(index).getOptions()).getToolCallbacks().stream()
				.map(tool -> tool.getToolDefinition().name()).toList();
	}

	private static List<ToolResultPart> toolResults(ScriptedToolModel model, int promptIndex) {
		return model.prompts.get(promptIndex).getInstructions().stream()
				.filter(ToolResponseMessage.class::isInstance).map(ToolResponseMessage.class::cast)
				.reduce((first, last) -> last).orElseThrow().getResults();
	}

	private static class ScriptedToolModel implements ChatModel {

		private final List<Prompt> prompts = new ArrayList<>();
		private final List<List<AssistantMessage.ToolCall>> batches;

		private ScriptedToolModel(List<List<AssistantMessage.ToolCall>> batches) {
			this.batches = batches;
		}

		@Override
		public ToolCallingChatOptions getOptions() {
			return ToolCallingChatOptions.builder().build();
		}

		@Override
		public ChatResponse call(Prompt prompt) {
			int index = prompts.size();
			prompts.add(prompt);
			var output = index < batches.size() ? AssistantMessage.builder().toolCalls(batches.get(index)).build()
					: new AssistantMessage("final response");
			return new ChatResponse(List.of(new Generation(output)));
		}

		@Override
		public Flux<ChatResponse> stream(Prompt prompt) {
			return Flux.just(call(prompt));
		}
	}

	private static final class ScopedTools {

		private final AtomicInteger allowedCalls = new AtomicInteger();
		private final AtomicInteger excludedCalls = new AtomicInteger();

		@Tool(description = "An allowed test tool")
		String allowed() {
			allowedCalls.incrementAndGet();
			return "allowed";
		}

		@Tool(description = "A tool that may be excluded")
		String excluded() {
			excludedCalls.incrementAndGet();
			return "excluded";
		}
	}

	private static ChatModel alwaysTwoToolCalls() {
		return prompt -> new BatchToolChatModel().call(prompt);
	}

	private static final class ConfirmationTools {

		private final AtomicInteger executions = new AtomicInteger();

		@Tool(name = "boundary_tool", description = "Confirm each tool call", requiresConfirmation = true)
		String confirmedTool() {
			executions.incrementAndGet();
			return "approved";
		}

	}

	private static final class BatchToolChatModel implements ChatModel {

		private static final OpaquePayload SIGNATURE = new OpaquePayload("fixture", "signature", "redacted");
		private final AtomicInteger callCount = new AtomicInteger();
		private final AtomicReference<Prompt> followup = new AtomicReference<>();

		private final String firstId;
		private final String secondId;

		private BatchToolChatModel() {
			this("first", "second");
		}

		private BatchToolChatModel(String firstId, String secondId) {
			this.firstId = firstId;
			this.secondId = secondId;
		}

		@Override
		public ChatResponse call(Prompt prompt) {
			if (callCount.incrementAndGet() == 1) {
				var first = new AssistantMessage.ToolCall(firstId, "function", "boundary_tool", "{}");
				var second = new AssistantMessage.ToolCall(secondId, "function", "boundary_tool", "{}");
				return new ChatResponse(List.of(new Generation(AssistantMessage.builder()
						.parts(List.of(ReasoningPart.of("reasoning"), ToolCallPart.of(first), new ToolCallPart(second, SIGNATURE, Map.of())))
						.build())));
			}
			followup.set(prompt);
			return new ChatResponse(List.of(new Generation(new AssistantMessage("final response"))));
		}

	}

	private static final class ToolCallThenFinalChatModel implements ChatModel {

		private final AtomicInteger callCount = new AtomicInteger();

		@Override
		public ChatResponse call(Prompt prompt) {
			if (callCount.incrementAndGet() == 1) {
				AssistantMessage.ToolCall toolCall = new AssistantMessage.ToolCall("call-1", "function",
						"boundary_tool", "{}");
				return new ChatResponse(List.of(new Generation(
						AssistantMessage.builder().content("").toolCalls(List.of(toolCall)).build())));
			}
			return new ChatResponse(List.of(new Generation(new AssistantMessage("final response"))));
		}

		@Override
		public Flux<ChatResponse> stream(Prompt prompt) {
			return Flux.just(call(prompt));
		}
	}

	private static final class FixedResponseChatModel implements ChatModel {

		private final String response;

		private FixedResponseChatModel(String response) {
			this.response = response;
		}

		@Override
		public ChatResponse call(Prompt prompt) {
			return new ChatResponse(List.of(new Generation(new AssistantMessage(this.response))));
		}
	}

	private static final class CapturingModelInterceptor extends ModelInterceptor {

		private final AtomicReference<ModelRequest> capturedRequest;

		private CapturingModelInterceptor(AtomicReference<ModelRequest> capturedRequest) {
			this.capturedRequest = capturedRequest;
		}

		@Override
		public ModelResponse interceptModel(ModelRequest request, ModelCallHandler handler) {
			this.capturedRequest.set(request);
			return handler.call(request);
		}

		@Override
		public String getName() {
			return "capturing-model-interceptor";
		}
	}

	private static final class BoundaryTools {

		private final AtomicInteger agentToolNodeExecutions = new AtomicInteger();

		private final AtomicInteger chatClientAdvisorExecutions = new AtomicInteger();

		@Tool(name = "boundary_tool", description = "Boundary test tool")
		String boundaryTool(ToolContext toolContext) {
			if (ToolContextHelper.getConfig(toolContext).isPresent()) {
				agentToolNodeExecutions.incrementAndGet();
			}
			else {
				chatClientAdvisorExecutions.incrementAndGet();
			}
			return "tool result";
		}
	}

}
