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
package com.alibaba.cloud.ai.graph.agent.hook.returndirect;

import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.Prioritized;
import com.alibaba.cloud.ai.graph.agent.hook.HookPosition;
import com.alibaba.cloud.ai.graph.agent.hook.HookPositions;
import com.alibaba.cloud.ai.graph.agent.hook.JumpTo;
import com.alibaba.cloud.ai.graph.agent.hook.messages.AgentCommand;
import com.alibaba.cloud.ai.graph.agent.hook.messages.MessagesModelHook;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;

import java.util.ArrayList;
import java.util.List;

/**
 * MessagesModelHook that checks for FINISH_REASON in ToolResponseMessage metadata.
 * If found, generates an AssistantMessage and jumps to END node.
 * This hook is designed to execute first among all hooks.
 */
@HookPositions({HookPosition.BEFORE_MODEL})
public class ReturnDirectModelHook extends MessagesModelHook {

	@Override
	public String getName() {
		return "finish_reason_check_messages_model_hook";
	}

	@Override
	public int getOrder() {
		return Prioritized.HIGHEST_PRECEDENCE;
	}

	@Override
	public List<JumpTo> canJumpTo() {
		return List.of(JumpTo.end);
	}

	@Override
	public AgentCommand beforeModel(List<Message> previousMessages, RunnableConfig config) {
		// Check the last message - if it's a ToolResponseMessage, continue processing
		if (previousMessages.isEmpty()) {
			return new AgentCommand(previousMessages);
		}

		Message lastMessage = previousMessages.get(previousMessages.size() - 1);
		if (!(lastMessage instanceof ToolResponseMessage toolResponseMessage)) {
			// Last message is not a ToolResponseMessage, return normally
			return new AgentCommand(previousMessages);
		}

		if (ReturnDirectMessageSupport.isReturnDirect(toolResponseMessage)) {
			AssistantMessage newAssistantMessage = ReturnDirectMessageSupport.toAssistantMessage(toolResponseMessage);

			// Create new messages list with the generated AssistantMessage
			List<Message> newMessages = new ArrayList<>(previousMessages);
			newMessages.add(newAssistantMessage);

			// Return with JumpTo.end to jump to END node
			return new AgentCommand(JumpTo.end, newMessages);
		}

		// No FINISH_REASON found, return normally
		return new AgentCommand(previousMessages);
	}
}
