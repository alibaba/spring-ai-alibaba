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

import com.alibaba.cloud.ai.graph.agent.hook.returndirect.ReturnDirectModelHook;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class ReactAgentReturnDirectReproductionTest {

	private static final String DIRECT_TOOL_RESULT = "\"RAW_RESULT=42\"";
	private static final String NATURAL_LANGUAGE_RESULT = "12 加 30 的结果是 42。";

	@Test
	void defaultReactAgentShortCircuitsWhenReturnDirectIsTrue() throws GraphRunnerException {
		ScriptedToolLoopChatModel chatModel = new ScriptedToolLoopChatModel();

		ReactAgent agent = ReactAgent.builder()
			.name("return-direct-default-react-agent")
			.model(chatModel)
			.methodTools(new ReturnDirectAddTool())
			.build();

		AssistantMessage assistantMessage = agent.call("请调用工具计算 12 + 30，并用一句完整中文解释结果");

		assertEquals(DIRECT_TOOL_RESULT, assistantMessage.getText());
		assertEquals(1, chatModel.callCount.get(), "returnDirect=true should stop before the second model call");
	}

	@Test
	void defaultReactAgentContinuesWhenReturnDirectIsFalse() throws GraphRunnerException {
		ScriptedToolLoopChatModel chatModel = new ScriptedToolLoopChatModel();

		ReactAgent agent = ReactAgent.builder()
			.name("return-direct-false-react-agent")
			.model(chatModel)
			.methodTools(new NonReturnDirectAddTool())
			.build();

		AssistantMessage assistantMessage = agent.call("请调用工具计算 12 + 30，并用一句完整中文解释结果");

		assertEquals(NATURAL_LANGUAGE_RESULT, assistantMessage.getText());
		assertEquals(2, chatModel.callCount.get(), "returnDirect=false should continue into the second model call");
		assertEquals(DIRECT_TOOL_RESULT, chatModel.lastToolResponseData);
		assertNotEquals(DIRECT_TOOL_RESULT, assistantMessage.getText());
	}

	@Test
	void explicitReturnDirectHookStillShortCircuitsTheSecondModelCall() throws GraphRunnerException {
		ScriptedToolLoopChatModel chatModel = new ScriptedToolLoopChatModel();

		ReactAgent agent = ReactAgent.builder()
			.name("return-direct-hooked-react-agent")
			.model(chatModel)
			.methodTools(new ReturnDirectAddTool())
			.hooks(List.of(new ReturnDirectModelHook()))
			.build();

		AssistantMessage assistantMessage = agent.call("请调用工具计算 12 + 30，并用一句完整中文解释结果");

		assertEquals(DIRECT_TOOL_RESULT, assistantMessage.getText());
		assertEquals(1, chatModel.callCount.get(), "ReturnDirectModelHook should stop the loop before a second model call");
	}

	static class ReturnDirectAddTool {

		@Tool(name = "my_add", description = "实现两个整数相加", returnDirect = true)
		public String add(@ToolParam(required = true, description = "必须是整数") int first,
				@ToolParam(required = true, description = "必须是整数") int second) {
			return "RAW_RESULT=" + (first + second);
		}
	}

	static class NonReturnDirectAddTool {

		@Tool(name = "my_add", description = "实现两个整数相加", returnDirect = false)
		public String add(@ToolParam(required = true, description = "必须是整数") int first,
				@ToolParam(required = true, description = "必须是整数") int second) {
			return "RAW_RESULT=" + (first + second);
		}
	}

	static final class ScriptedToolLoopChatModel implements ChatModel {

		private final AtomicInteger callCount = new AtomicInteger();

		private volatile String lastToolResponseData;

		@Override
		public ChatResponse call(Prompt prompt) {
			int currentCall = callCount.incrementAndGet();
			if (currentCall == 1) {
				AssistantMessage.ToolCall toolCall = new AssistantMessage.ToolCall(
						"call-1", "function", "my_add", "{\"first\":12,\"second\":30}");
				AssistantMessage assistantMessage = AssistantMessage.builder()
					.content("")
					.toolCalls(List.of(toolCall))
					.build();
				return new ChatResponse(List.of(new Generation(assistantMessage)));
			}

			List<Message> instructions = prompt.getInstructions();
			Message lastMessage = instructions.get(instructions.size() - 1);
			ToolResponseMessage toolResponseMessage = assertInstanceOf(ToolResponseMessage.class, lastMessage);
			lastToolResponseData = toolResponseMessage.getResponses().get(0).responseData();
			System.out.println("[spring-ai-alibaba-repro] second-model-call observed tool response: " + lastToolResponseData);
			return new ChatResponse(List.of(new Generation(new AssistantMessage(NATURAL_LANGUAGE_RESULT))));
		}
	}
}
