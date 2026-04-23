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

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.OverAllStateBuilder;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.action.InterruptionMetadata;
import com.alibaba.cloud.ai.graph.agent.hook.hip.HitlMetadataKeys;
import com.alibaba.cloud.ai.graph.agent.node.AgentToolNode;
import com.alibaba.cloud.ai.graph.agent.tool.ToolOutputEnvelope;
import com.alibaba.cloud.ai.graph.agent.tools.ToolContextConstants;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;

public class AgentToolNodeHitlTest {

	@Test
	public void testToolLevelHitlInterruptAndResume() throws Exception {
		AtomicInteger tool1Count = new AtomicInteger(0);
		AtomicInteger tool2Count = new AtomicInteger(0);
		AtomicInteger tool3Count = new AtomicInteger(0);

		ToolCallback tool1 = createCountingTool("tool1", tool1Count, false, null);
		ToolCallback tool2 = createCountingTool("tool2", tool2Count, false, null);
		ToolCallback tool3 = createCountingTool("tool3", tool3Count, true, "需要人工确认 tool3 执行");

		AgentToolNode toolNode = AgentToolNode.builder()
				.agentName("hitl-test")
				.toolCallbacks(List.of(tool1, tool2, tool3))
				.toolExecutionExceptionProcessor(null)
				.build();

		AssistantMessage.ToolCall call1 = new AssistantMessage.ToolCall("call-1", "function", "tool1", "{}" );
		AssistantMessage.ToolCall call2 = new AssistantMessage.ToolCall("call-2", "function", "tool2", "{}" );
		AssistantMessage.ToolCall call3 = new AssistantMessage.ToolCall("call-3", "function", "tool3", "{}" );

		AssistantMessage assistantMessage = AssistantMessage.builder()
				.content("plan")
				.toolCalls(List.of(call1, call2, call3))
				.build();

		OverAllState firstState = OverAllStateBuilder.builder()
				.withData(Map.of("messages", List.of(assistantMessage)))
				.build();

		RunnableConfig config = RunnableConfig.builder().build();
		Map<String, Object> firstUpdate = toolNode.apply(firstState, config);

		ToolResponseMessage firstResponse = (ToolResponseMessage) firstUpdate.get("messages");
		Assertions.assertNotNull(firstResponse, "Expected tool response message");
		Assertions.assertEquals(2, firstResponse.getResponses().size(), "Only tool1 & tool2 should respond before HITL");

		Assertions.assertEquals(1, tool1Count.get());
		Assertions.assertEquals(1, tool2Count.get());
		Assertions.assertEquals(1, tool3Count.get(), "tool3 executed and requested approval");

		InterruptionMetadata interruption = toolNode.interruptAfter("_AGENT_TOOL_", firstState, firstUpdate, config).orElse(null);
		Assertions.assertNotNull(interruption, "Expected HITL interruption after tool3 requires approval");
		Assertions.assertEquals(1, interruption.toolFeedbacks().size());
		Assertions.assertEquals("tool3", interruption.toolFeedbacks().get(0).getName());

		List<Message> resumedMessages = new ArrayList<>();
		resumedMessages.add(assistantMessage);
		resumedMessages.add(firstResponse);
		OverAllState resumedState = OverAllStateBuilder.builder()
				.withData(Map.of(
						"messages", resumedMessages,
						HitlMetadataKeys.HITL_PENDING_TOOL_FEEDBACKS_KEY, interruption.toolFeedbacks(),
						HitlMetadataKeys.HITL_PENDING_TOOL_CALL_IDS_KEY, List.of("call-3")
				))
				.build();

		InterruptionMetadata approvedFeedback = InterruptionMetadata.builder("_AGENT_TOOL_", resumedState)
				.addToolFeedback(InterruptionMetadata.ToolFeedback.builder()
						.id("call-3")
						.name("tool3")
						.arguments("{}")
						.result(InterruptionMetadata.ToolFeedback.FeedbackResult.APPROVED)
						.description("approved")
						.build())
				.build();

		RunnableConfig resumeConfig = RunnableConfig.builder()
				.addMetadata(RunnableConfig.HUMAN_FEEDBACK_METADATA_KEY, approvedFeedback)
				.build();

		Assertions.assertTrue(toolNode.interrupt("_AGENT_TOOL_", resumedState, resumeConfig).isEmpty(),
				"Feedback provided, should not interrupt again before apply");

		Map<String, Object> secondUpdate = toolNode.apply(resumedState, resumeConfig);
		ToolResponseMessage secondResponse = (ToolResponseMessage) secondUpdate.get("messages");
		Assertions.assertNotNull(secondResponse);
		Assertions.assertEquals(1, secondResponse.getResponses().size(), "Only tool3 should execute on resume");
		Assertions.assertEquals("tool3", secondResponse.getResponses().get(0).name());

		Assertions.assertEquals(1, tool1Count.get(), "tool1 should not re-run");
		Assertions.assertEquals(1, tool2Count.get(), "tool2 should not re-run");
		Assertions.assertEquals(2, tool3Count.get(), "tool3 should re-run after approval");

		Object envelopes = secondResponse.getMetadata().get(HitlMetadataKeys.TOOL_OUTPUT_ENVELOPES_METADATA_KEY);
		Assertions.assertNotNull(envelopes, "Expected tool output envelopes metadata");
	}

	private ToolCallback createCountingTool(String name, AtomicInteger counter, boolean requiresApproval,
			String approvalDescription) {
		BiFunction<String, ToolContext, String> tool = (args, context) -> {
			counter.incrementAndGet();
			if (requiresApproval) {
				Object updateObj = context.getContext().get(ToolContextConstants.AGENT_STATE_FOR_UPDATE_CONTEXT_KEY);
				if (updateObj instanceof Map) {
					@SuppressWarnings("unchecked")
					Map<String, Object> updateMap = (Map<String, Object>) updateObj;
					ToolOutputEnvelope envelope = ToolOutputEnvelope.builder()
							.status(ToolOutputEnvelope.STATUS_INTERRUPTED)
							.toolCallId(null)
							.toolName(name)
							.requiresApproval(true)
							.approvalDescription(approvalDescription)
							.build();
					updateMap.put(ToolOutputEnvelope.CONTEXT_KEY, envelope);
				}
			}
			return name + "-ok";
		};

		return FunctionToolCallback.builder(name, tool)
				.description("test tool")
				.inputType(String.class)
				.build();
	}

}
