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

import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.action.InterruptionMetadata;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.extension.interceptor.SubAgentInterceptor;
import com.alibaba.cloud.ai.graph.agent.extension.interceptor.SubAgentSpec;
import com.alibaba.cloud.ai.graph.agent.hook.hip.HumanInTheLoopHook;
import com.alibaba.cloud.ai.graph.agent.hook.hip.ToolConfig;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import reactor.core.publisher.Flux;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * A tool gated by {@link HumanInTheLoopHook} must not run without approval when the main agent delegates it to a
 * subagent through the {@code task} tool of {@link SubAgentInterceptor}, given that the hook is passed as
 * {@code defaultHooks}: neither for the built-in general-purpose subagent nor for a spec subagent added before
 * {@code defaultHooks(...)} was called.
 */
public class SubAgentDefaultHooksTest {

	public record Transfer(String to, double amount) {
	}

	/**
	 * Main agent (the only one offered the task tool): delegates to {@code target}. Subagent: calls the gated tool.
	 * Either one answers in text once a tool result is back.
	 */
	private static class DelegatingChatModel implements ChatModel {

		private final String target;

		final List<String> toolResultsSeenByMainAgent = new ArrayList<>();

		DelegatingChatModel(String target) {
			this.target = target;
		}

		@Override
		public ChatResponse call(Prompt prompt) {
			List<Message> messages = prompt.getInstructions();
			boolean mainAgent = offeredTools(prompt).contains("task");
			if (!messages.isEmpty() && messages.get(messages.size() - 1) instanceof ToolResponseMessage trm) {
				if (mainAgent) {
					trm.getResponses().forEach(r -> toolResultsSeenByMainAgent.add(r.responseData()));
				}
				return new ChatResponse(List.of(new Generation(new AssistantMessage("done"))));
			}
			AssistantMessage.ToolCall call = mainAgent
					? new AssistantMessage.ToolCall("call_task", "function", "task",
							"{\"subagent_type\":\"" + target + "\",\"description\":\"pay ACCT-X 100\"}")
					: new AssistantMessage.ToolCall("call_wire", "function", "wire_transfer",
							"{\"to\":\"ACCT-X\",\"amount\":100}");
			return new ChatResponse(List.of(new Generation(AssistantMessage.builder().content("").toolCalls(List.of(call)).build())));
		}

		@Override
		public Flux<ChatResponse> stream(Prompt prompt) {
			return Flux.just(call(prompt));
		}

		private static List<String> offeredTools(Prompt prompt) {
			List<String> names = new ArrayList<>();
			if (prompt.getOptions() instanceof ToolCallingChatOptions options) {
				options.getToolCallbacks().forEach(cb -> names.add(cb.getToolDefinition().name()));
				names.addAll(options.getToolNames());
			}
			return names;
		}

	}

	private static HumanInTheLoopHook approvalOnWireTransfer() {
		return HumanInTheLoopHook.builder()
			.approvalOn("wire_transfer", ToolConfig.builder().description("needs approval").build())
			.build();
	}

	private static List<ToolCallback> tools(AtomicInteger executions) {
		return List.of(FunctionToolCallback.builder("wire_transfer", (Transfer t) -> {
			executions.incrementAndGet();
			return "transferred";
		}).description("Move money").inputType(Transfer.class).build());
	}

	private static ReactAgent mainAgent(ChatModel model, List<ToolCallback> tools, HumanInTheLoopHook hitl,
			SubAgentInterceptor subAgents) {
		return ReactAgent.builder()
			.name("treasury_agent")
			.model(model)
			.tools(tools)
			.hooks(hitl)
			.interceptors(subAgents)
			.saver(new MemorySaver())
			.build();
	}

	@Test
	void generalPurposeSubagentCarriesDefaultHooks() throws Exception {
		AtomicInteger executions = new AtomicInteger();
		DelegatingChatModel model = new DelegatingChatModel("general-purpose");
		List<ToolCallback> tools = tools(executions);
		HumanInTheLoopHook hitl = approvalOnWireTransfer();
		SubAgentInterceptor subAgents = SubAgentInterceptor.builder()
			.defaultModel(model)
			.defaultTools(tools)
			.defaultHooks(hitl)
			.build();

		mainAgent(model, tools, hitl, subAgents).invokeAndGetOutput("pay ACCT-X 100",
				RunnableConfig.builder().threadId("subagent-gp").build());

		Assertions.assertEquals(0, executions.get(),
				"general-purpose subagent ran the approval-gated tool without approval");
		Assertions.assertTrue(model.toolResultsSeenByMainAgent.stream().anyMatch(r -> r.contains("NOT executed")),
				"main agent was not told that the delegated call is waiting for approval: " + model.toolResultsSeenByMainAgent);
	}

	@Test
	void specSubagentAddedBeforeDefaultHooksStillCarriesThem() throws Exception {
		AtomicInteger executions = new AtomicInteger();
		DelegatingChatModel model = new DelegatingChatModel("payables-agent");
		List<ToolCallback> tools = tools(executions);
		HumanInTheLoopHook hitl = approvalOnWireTransfer();
		SubAgentInterceptor subAgents = SubAgentInterceptor.builder()
			.defaultModel(model)
			.defaultTools(tools)
			.addSubAgent(SubAgentSpec.builder()
				.name("payables-agent")
				.description("Pays supplier invoices")
				.systemPrompt("You pay supplier invoices.")
				.build())
			.defaultHooks(hitl)
			.includeGeneralPurpose(false)
			.build();

		mainAgent(model, tools, hitl, subAgents).invokeAndGetOutput("pay ACCT-X 100",
				RunnableConfig.builder().threadId("subagent-spec-order").build());

		Assertions.assertEquals(0, executions.get(),
				"spec subagent added before defaultHooks(...) ran the approval-gated tool without approval");
	}

	@Test
	void directCallOnMainAgentStillPausesForApproval() throws Exception {
		AtomicInteger executions = new AtomicInteger();
		List<ToolCallback> tools = tools(executions);
		HumanInTheLoopHook hitl = approvalOnWireTransfer();
		// the model never sees "task" here because the main agent has no SubAgentInterceptor
		ReactAgent agent = ReactAgent.builder()
			.name("treasury_agent")
			.model(new DelegatingChatModel("unused"))
			.tools(tools)
			.hooks(hitl)
			.saver(new MemorySaver())
			.build();

		Optional<NodeOutput> out = agent.invokeAndGetOutput("pay ACCT-X 100",
				RunnableConfig.builder().threadId("direct").build());

		Assertions.assertEquals(0, executions.get());
		Assertions.assertTrue(out.isPresent() && out.get() instanceof InterruptionMetadata,
				"expected the main agent to pause for approval");
	}

}
