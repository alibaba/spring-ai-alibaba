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
package com.alibaba.cloud.ai.graph.agent.hook.toolexecutionfailure;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.hook.messages.MessagesModelHook;
import com.alibaba.cloud.ai.graph.serializer.AgentInstructionMessage;
import com.alibaba.cloud.ai.graph.state.ReplaceAllWith;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ToolExecutionFailureGuardHookTest {

	@Test
	void shouldOnlyAccumulateCountBeforeThreshold() {
		ToolExecutionFailureGuardHook hook = ToolExecutionFailureGuardHook.builder()
			.maxConsecutiveExecutionFailureRounds(2)
			.build();
		RunnableConfig config = RunnableConfig.builder().build();

		Map<String, Object> result = beforeModel(hook, config,
				List.of(failedToolResponse("search", ToolExecutionFailureGuardConstants.TIMEOUT_FAILURE_TYPE)));
		List<Message> messages = extractMessages(result);

		assertEquals(1, messages.size());
		assertInstanceOf(ToolResponseMessage.class, messages.get(0));
		assertNull(result.get("jump_to"));
	}

	@Test
	void shouldInjectFinalAnswerInstructionWhenThresholdIsReached() {
		ToolExecutionFailureGuardHook hook = ToolExecutionFailureGuardHook.builder()
			.maxConsecutiveExecutionFailureRounds(2)
			.build();
		RunnableConfig config = RunnableConfig.builder().build();

		beforeModel(hook, config,
				List.of(failedToolResponse("search", ToolExecutionFailureGuardConstants.TIMEOUT_FAILURE_TYPE)));
		Map<String, Object> result = beforeModel(hook, config,
				List.of(failedToolResponse("weather", ToolExecutionFailureGuardConstants.RUNTIME_EXCEPTION_FAILURE_TYPE)));
		List<Message> messages = extractMessages(result);

		assertEquals(2, messages.size());
		AgentInstructionMessage instruction = assertInstanceOf(AgentInstructionMessage.class, messages.get(1));
		assertNotNull(instruction.getText());
		assertTrue(instruction.getText().contains("weather"));
		assertTrue(instruction.getText().contains(ToolExecutionFailureGuardConstants.RUNTIME_EXCEPTION_FAILURE_TYPE));
		assertEquals(Boolean.TRUE,
				instruction.getMetadata().get(ToolExecutionFailureGuardConstants.FINAL_ANSWER_INSTRUCTION_METADATA_KEY));
	}

	@Test
	void shouldTerminateWhenModelStillCallsToolsInFinalAnswerMode() {
		ToolExecutionFailureGuardHook hook = ToolExecutionFailureGuardHook.builder()
			.maxConsecutiveExecutionFailureRounds(2)
			.build();
		RunnableConfig config = RunnableConfig.builder().build();

		beforeModel(hook, config,
				List.of(failedToolResponse("search", ToolExecutionFailureGuardConstants.TIMEOUT_FAILURE_TYPE)));
		beforeModel(hook, config,
				List.of(failedToolResponse("search", ToolExecutionFailureGuardConstants.TIMEOUT_FAILURE_TYPE)));
		Map<String, Object> result = afterModel(hook, config, List.of(assistantWithToolCall("call-3", "echo")));
		List<Message> messages = extractMessages(result);

		assertEquals("end", result.get("jump_to"));
		AssistantMessage finalMessage = assertInstanceOf(AssistantMessage.class, messages.get(0));
		assertEquals(
				"I could not continue with tool calls because tool execution kept failing, and I was still unable to produce a direct answer without tools.",
				finalMessage.getText());
	}

	@Test
	void shouldUseCustomTerminationMessageForInstructionAndFallback() {
		String terminationMessage = "请停止继续调用工具，直接基于现有上下文回答用户。";
		ToolExecutionFailureGuardHook hook = ToolExecutionFailureGuardHook.builder()
			.maxConsecutiveExecutionFailureRounds(1)
			.terminationMessage(terminationMessage)
			.build();
		RunnableConfig config = RunnableConfig.builder().build();

		Map<String, Object> beforeModelResult = beforeModel(hook, config,
				List.of(failedToolResponse("search", ToolExecutionFailureGuardConstants.TIMEOUT_FAILURE_TYPE)));
		List<Message> beforeModelMessages = extractMessages(beforeModelResult);
		AgentInstructionMessage instruction = assertInstanceOf(AgentInstructionMessage.class, beforeModelMessages.get(1));
		assertEquals(terminationMessage, instruction.getText());

		Map<String, Object> afterModelResult = afterModel(hook, config,
				List.of(assistantWithToolCall("call-2", "search")));
		List<Message> afterModelMessages = extractMessages(afterModelResult);
		AssistantMessage finalMessage = assertInstanceOf(AssistantMessage.class, afterModelMessages.get(0));
		assertEquals("end", afterModelResult.get("jump_to"));
		assertEquals(terminationMessage, finalMessage.getText());
	}

	private static Map<String, Object> beforeModel(ToolExecutionFailureGuardHook hook, RunnableConfig config,
			List<Message> messages) {
		OverAllState state = new OverAllState(Map.of("messages", messages));
		return MessagesModelHook.beforeModelAction(hook).apply(state, config).join();
	}

	private static Map<String, Object> afterModel(ToolExecutionFailureGuardHook hook, RunnableConfig config,
			List<Message> messages) {
		OverAllState state = new OverAllState(Map.of("messages", messages));
		return MessagesModelHook.afterModelAction(hook).apply(state, config).join();
	}

	@SuppressWarnings("unchecked")
	private static List<Message> extractMessages(Map<String, Object> result) {
		ReplaceAllWith<Message> replaceAllWith = assertInstanceOf(ReplaceAllWith.class, result.get("messages"));
		return replaceAllWith.newValues();
	}

	private static ToolResponseMessage failedToolResponse(String toolName, String failureType) {
		return ToolResponseMessage.builder()
			.responses(List.of(new ToolResponseMessage.ToolResponse("call-1", toolName,
					"Tool execution failed: " + toolName)))
			.metadata(Map.of(
					ToolExecutionFailureGuardConstants.ALL_TOOL_CALLS_FAILED_METADATA_KEY, true,
					ToolExecutionFailureGuardConstants.FAILED_TOOL_NAMES_METADATA_KEY, List.of(toolName),
					ToolExecutionFailureGuardConstants.FAILURE_TYPES_METADATA_KEY, List.of(failureType)))
			.build();
	}

	private static AssistantMessage assistantWithToolCall(String id, String toolName) {
		return AssistantMessage.builder()
			.content("")
			.toolCalls(List.of(new AssistantMessage.ToolCall(id, "function", toolName, "{}")))
			.build();
	}

}

