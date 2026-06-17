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
package com.alibaba.cloud.ai.graph.agent.hooks.unknowntool;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.hook.messages.MessagesModelHook;
import com.alibaba.cloud.ai.graph.agent.hook.unknowntool.UnknownToolGuardConstants;
import com.alibaba.cloud.ai.graph.agent.hook.unknowntool.UnknownToolGuardHook;
import com.alibaba.cloud.ai.graph.state.ReplaceAllWith;
import com.alibaba.cloud.ai.graph.serializer.AgentInstructionMessage;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UnknownToolGuardHookTest {

	/**
	 * Verifies that with one self-repair retry configured, the first unknown-tool round only
	 * increments the guard state and does not terminate.
	 */
	@Test
	void shouldOnlyAccumulateCountBeforeSelfRepairRetriesAreExhausted() {
		UnknownToolGuardHook hook = UnknownToolGuardHook.builder().maxSelfRepairRetries(1).build();
		RunnableConfig config = RunnableConfig.builder().build();

		Map<String, Object> result = beforeModel(hook, config, List.of(unknownToolResponse("missing_tool", List.of("echo"))));
		List<Message> messages = extractMessages(result);

		assertEquals(1, messages.size());
		assertInstanceOf(ToolResponseMessage.class, messages.get(0));
		assertNull(result.get("jump_to"));

		Map<String, Object> afterModelResult = afterModel(hook, config, List.of(assistantWithToolCall("call-1")));
		assertNull(afterModelResult.get("jump_to"));
	}

	/**
	 * Verifies that the hook appends a final-answer instruction after the configured
	 * self-repair retries are exhausted.
	 */
	@Test
	void shouldInjectFinalAnswerInstructionWhenSelfRepairRetriesAreExhausted() {
		UnknownToolGuardHook hook = UnknownToolGuardHook.builder().maxSelfRepairRetries(1).build();
		RunnableConfig config = RunnableConfig.builder().build();

		beforeModel(hook, config, List.of(unknownToolResponse("missing_tool_1", List.of("echo"))));
		Map<String, Object> result = beforeModel(hook, config,
				List.of(unknownToolResponse("missing_tool_2", List.of("echo", "weather"))));
		List<Message> messages = extractMessages(result);

		assertEquals(2, messages.size());
		AgentInstructionMessage instruction = assertInstanceOf(AgentInstructionMessage.class, messages.get(1));
		assertNotNull(instruction.getText());
		assertTrue(instruction.getText().contains("missing_tool_2"));
		assertTrue(instruction.getText().contains("echo"));
		assertEquals(Boolean.TRUE,
				instruction.getMetadata().get(UnknownToolGuardConstants.FINAL_ANSWER_INSTRUCTION_METADATA_KEY));
	}

	/**
	 * Verifies that the hook ends the loop when the model still tries to call tools in final-answer mode.
	 */
	@Test
	void shouldTerminateWhenModelStillCallsToolsInFinalAnswerMode() {
		UnknownToolGuardHook hook = UnknownToolGuardHook.builder().maxSelfRepairRetries(1).build();
		RunnableConfig config = RunnableConfig.builder().build();

		beforeModel(hook, config, List.of(unknownToolResponse("missing_tool_1", List.of("echo"))));
		beforeModel(hook, config, List.of(unknownToolResponse("missing_tool_2", List.of("echo"))));
		Map<String, Object> result = afterModel(hook, config, List.of(assistantWithToolCall("call-3")));
		List<Message> messages = extractMessages(result);

		assertEquals("end", result.get("jump_to"));
		AssistantMessage finalMessage = assertInstanceOf(AssistantMessage.class, messages.get(0));
		assertEquals(
				"I had to stop calling tools because the requested tools were unavailable, " +
						"and I could not safely complete your request without them in this turn. " +
						"Would you like me to continue with a best-effort answer based on the current context, " +
						"or would you prefer to update the tool choice and try again?",
				finalMessage.getText());
	}

	/**
	 * Verifies that custom instruction and fallback overrides are applied independently.
	 */
	@Test
	void shouldUseCustomMessagesForInstructionAndFallback() {
		String customInstruction = "Please stop using the tool and answer the user directly based on the current context.";
		String customFallback = "The requested tool is unavailable. Would you like me to continue without tools?";
		UnknownToolGuardHook hook = UnknownToolGuardHook.builder()
				.maxSelfRepairRetries(0)
				.customFinalAnswerInstruction(customInstruction)
				.customFallbackAnswerMessage(customFallback)
				.build();
		RunnableConfig config = RunnableConfig.builder().build();

		Map<String, Object> beforeModelResult = beforeModel(hook, config,
				List.of(unknownToolResponse("missing_tool", List.of("echo"))));
		List<Message> beforeModelMessages = extractMessages(beforeModelResult);
		AgentInstructionMessage instruction = assertInstanceOf(AgentInstructionMessage.class, beforeModelMessages.get(1));
		assertEquals(customInstruction, instruction.getText());

		Map<String, Object> afterModelResult = afterModel(hook, config, List.of(assistantWithToolCall("call-2")));
		List<Message> afterModelMessages = extractMessages(afterModelResult);
		AssistantMessage finalMessage = assertInstanceOf(AssistantMessage.class, afterModelMessages.get(0));
		assertEquals("end", afterModelResult.get("jump_to"));
		assertEquals(customFallback, finalMessage.getText());
	}

	/**
	 * Verifies that zero self-repair retries enter final-answer mode on the first unknown-tool round.
	 */
	@Test
	void shouldEnterFinalAnswerModeImmediatelyWhenSelfRepairRetriesIsZero() {
		UnknownToolGuardHook hook = UnknownToolGuardHook.builder().maxSelfRepairRetries(0).build();
		RunnableConfig config = RunnableConfig.builder().build();

		Map<String, Object> result = beforeModel(hook, config, List.of(unknownToolResponse("missing_tool", List.of("echo"))));
		List<Message> messages = extractMessages(result);

		assertEquals(2, messages.size());
		assertInstanceOf(AgentInstructionMessage.class, messages.get(1));
	}

	/**
	 * Verifies that allowing two self-repair retries keeps the guard in accumulation mode
	 * for the first two unknown-tool rounds and only enters final-answer mode on the third.
	 */
	@Test
	void shouldEnterFinalAnswerModeAfterConfiguredSelfRepairRetriesAreExhausted() {
		UnknownToolGuardHook hook = UnknownToolGuardHook.builder().maxSelfRepairRetries(2).build();
		RunnableConfig config = RunnableConfig.builder().build();

		Map<String, Object> firstRound = beforeModel(hook, config, List.of(unknownToolResponse("missing_tool_1", List.of("echo"))));
		assertEquals(1, extractMessages(firstRound).size());

		Map<String, Object> secondRound = beforeModel(hook, config, List.of(unknownToolResponse("missing_tool_2", List.of("echo", "weather"))));
		assertEquals(1, extractMessages(secondRound).size());

		Map<String, Object> thirdRound = beforeModel(hook, config, List.of(unknownToolResponse("missing_tool_3", List.of("echo", "weather"))));
		List<Message> messages = extractMessages(thirdRound);

		assertEquals(2, messages.size());
		assertInstanceOf(AgentInstructionMessage.class, messages.get(1));
	}

	/**
	 * Verifies that a normal round clears the consecutive unknown-tool counter.
	 */
	@Test
	void shouldResetConsecutiveCountWhenLoopReturnsToNormalRound() {
		UnknownToolGuardHook hook = UnknownToolGuardHook.builder().maxSelfRepairRetries(1).build();
		RunnableConfig config = RunnableConfig.builder().build();

		beforeModel(hook, config, List.of(unknownToolResponse("missing_tool_1", List.of("echo"))));
		beforeModel(hook, config, List.of(new AssistantMessage("normal answer")));
		Map<String, Object> result = beforeModel(hook, config,
				List.of(unknownToolResponse("missing_tool_2", List.of("echo"))));
		List<Message> messages = extractMessages(result);

		assertEquals(1, messages.size());
		assertFalse(messages.get(0) instanceof AgentInstructionMessage);
		assertNull(result.get("jump_to"));
	}

	/**
	 * Invokes the hook's before-model phase with the provided messages.
	 */
	private static Map<String, Object> beforeModel(UnknownToolGuardHook hook, RunnableConfig config,
			List<Message> messages) {
		OverAllState state = new OverAllState(Map.of("messages", messages));
		return MessagesModelHook.beforeModelAction(hook).apply(state, config).join();
	}

	/**
	 * Invokes the hook's after-model phase with the provided messages.
	 */
	private static Map<String, Object> afterModel(UnknownToolGuardHook hook, RunnableConfig config,
			List<Message> messages) {
		OverAllState state = new OverAllState(Map.of("messages", messages));
		return MessagesModelHook.afterModelAction(hook).apply(state, config).join();
	}

	/**
	 * Extracts the replacement message list produced by the hook result.
	 */
	@SuppressWarnings("unchecked")
	private static List<Message> extractMessages(Map<String, Object> result) {
		ReplaceAllWith<Message> replaceAllWith = assertInstanceOf(ReplaceAllWith.class, result.get("messages"));
		return replaceAllWith.newValues();
	}

	/**
	 * Creates a tool response that marks the requested tool as unavailable.
	 */
	private static ToolResponseMessage unknownToolResponse(String requestedToolName, List<String> availableToolNames) {
		return ToolResponseMessage.builder()
			.responses(List.of(new ToolResponseMessage.ToolResponse("call-1", requestedToolName,
					"Unknown tool: " + requestedToolName)))
			.metadata(Map.of(
					UnknownToolGuardConstants.ALL_TOOL_CALLS_UNKNOWN_METADATA_KEY, true,
					UnknownToolGuardConstants.REQUESTED_TOOL_NAMES_METADATA_KEY, List.of(requestedToolName),
					UnknownToolGuardConstants.AVAILABLE_TOOL_NAMES_METADATA_KEY, availableToolNames))
			.build();
	}

	/**
	 * Creates an assistant message that attempts to call a tool again.
	 */
	private static AssistantMessage assistantWithToolCall(String id) {
		return AssistantMessage.builder()
			.content("")
			.toolCalls(List.of(new AssistantMessage.ToolCall(id, "function", "echo", "{}")))
			.build();
	}

}

