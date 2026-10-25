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
package com.alibaba.cloud.ai.graph.agent.hooks.hip;

import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.action.InterruptionMetadata;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.hook.hip.HumanInTheLoopHook;
import com.alibaba.cloud.ai.graph.agent.hook.hip.ToolConfig;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.ToolCallAdvisor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.tool.function.FunctionToolCallback;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import reactor.core.publisher.Flux;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * An approval-gated tool must not run before HumanInTheLoopHook can interrupt, even when the ChatClient handed to
 * the agent carries Spring AI's ToolCallAdvisor (which runs its own tool loop inside the model call).
 */
public class HumanInTheLoopToolCallAdvisorTest {

	public record Transfer(String to, double amount) {
	}

	/** Asks for the gated tool until a tool result comes back, then answers in text. */
	private static class ToolCallingChatModel implements ChatModel {

		@Override
		public ChatResponse call(Prompt prompt) {
			List<Message> messages = prompt.getInstructions();
			if (!messages.isEmpty() && messages.get(messages.size() - 1) instanceof ToolResponseMessage) {
				return new ChatResponse(List.of(new Generation(new AssistantMessage("done"))));
			}
			AssistantMessage toolCall = AssistantMessage.builder()
				.content("")
				.toolCalls(List.of(new AssistantMessage.ToolCall("call_1", "function", "wire_transfer",
						"{\"to\":\"ACCT-X\",\"amount\":100}")))
				.build();
			return new ChatResponse(List.of(new Generation(toolCall)));
		}

		@Override
		public Flux<ChatResponse> stream(Prompt prompt) {
			return Flux.just(call(prompt));
		}

	}

	private static ReactAgent gatedAgent(AtomicInteger executions, boolean withToolCallAdvisor) throws Exception {
		ChatClient.Builder clientBuilder = ChatClient.builder(new ToolCallingChatModel());
		if (withToolCallAdvisor) {
			clientBuilder.defaultAdvisors(ToolCallAdvisor.builder().build());
		}
		return ReactAgent.builder()
			.name("treasury_agent")
			.chatClient(clientBuilder.build())
			.tools(List.of(FunctionToolCallback.builder("wire_transfer", (Transfer t) -> {
				executions.incrementAndGet();
				return "transferred";
			}).description("Move money").inputType(Transfer.class).build()))
			.hooks(HumanInTheLoopHook.builder()
				.approvalOn("wire_transfer", ToolConfig.builder().description("needs approval").build())
				.build())
			.saver(new MemorySaver())
			.build();
	}

	@Test
	void gatedToolIsNotExecutedBeforeApprovalWhenChatClientHasToolCallAdvisor() throws Exception {
		AtomicInteger executions = new AtomicInteger();
		ReactAgent agent = gatedAgent(executions, true);

		Optional<NodeOutput> out = agent.invokeAndGetOutput("pay ACCT-X 100",
				RunnableConfig.builder().threadId("hitl-tool-call-advisor").build());

		Assertions.assertEquals(0, executions.get(), "gated tool ran before the human-in-the-loop hook could interrupt");
		Assertions.assertTrue(out.isPresent() && out.get() instanceof InterruptionMetadata,
				"expected the agent to pause for approval");
	}

	@Test
	void gatedToolIsNotExecutedBeforeApprovalWhenStreamingWithToolCallAdvisor() throws Exception {
		AtomicInteger executions = new AtomicInteger();
		ReactAgent agent = gatedAgent(executions, true);

		List<NodeOutput> outputs = agent
			.stream("pay ACCT-X 100", RunnableConfig.builder().threadId("hitl-tool-call-advisor-stream").build())
			.collectList()
			.block();

		Assertions.assertEquals(0, executions.get(), "gated tool ran before the human-in-the-loop hook could interrupt");
		Assertions.assertTrue(outputs != null && outputs.stream().anyMatch(InterruptionMetadata.class::isInstance),
				"expected the agent to pause for approval");
	}

	@Test
	void gatedToolIsNotExecutedBeforeApprovalWithoutToolCallAdvisor() throws Exception {
		AtomicInteger executions = new AtomicInteger();
		ReactAgent agent = gatedAgent(executions, false);

		Optional<NodeOutput> out = agent.invokeAndGetOutput("pay ACCT-X 100",
				RunnableConfig.builder().threadId("hitl-no-advisor").build());

		Assertions.assertEquals(0, executions.get());
		Assertions.assertTrue(out.isPresent() && out.get() instanceof InterruptionMetadata);
	}

}
