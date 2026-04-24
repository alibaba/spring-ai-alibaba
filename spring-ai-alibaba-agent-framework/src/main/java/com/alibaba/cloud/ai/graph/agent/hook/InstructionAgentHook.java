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
package com.alibaba.cloud.ai.graph.agent.hook;

import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.hook.messages.AgentCommand;
import com.alibaba.cloud.ai.graph.agent.hook.messages.MessagesAgentHook;
import com.alibaba.cloud.ai.graph.serializer.AgentInstructionMessage;
import com.alibaba.cloud.ai.graph.RunnableConfig;

import org.springframework.ai.chat.messages.Message;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * MessagesAgentHook that injects the ReactAgent's instruction into messages before each agent run.
 * <p>
 * When this hook is active, it runs at {@link HookPosition#BEFORE_AGENT} and reads
 * {@link ReactAgent#instruction()} from {@link #getAgent()}. If the instruction is non-empty,
 * it prepends an {@link AgentInstructionMessage} to the given messages and returns an
 * {@link AgentCommand} with {@link com.alibaba.cloud.ai.graph.agent.hook.messages.UpdatePolicy#REPLACE}.
 * This allows ReactAgent to avoid adding instruction again in the subgraph adapter when used as a subgraph node.
 * <p>
 * This hook is added by default in ReactAgent when no other hook is an InstructionAgentHook.
 * It runs first among beforeAgent hooks (lowest order).
 */
@HookPositions(HookPosition.BEFORE_AGENT)
public class InstructionAgentHook extends MessagesAgentHook {

	private ReactAgent reactAgent;

	@Override
	public AgentCommand beforeAgent(List<Message> previousMessages, RunnableConfig config) {
		if (reactAgent == null) {
			return new AgentCommand(previousMessages);
		}
		String instruction = reactAgent.instruction();
		if (!StringUtils.hasLength(instruction)) {
			return new AgentCommand(previousMessages);
		}
		AgentInstructionMessage instructionMessage = AgentInstructionMessage.builder().text(instruction).build();
		List<Message> newMessages = new ArrayList<>(previousMessages);
		newMessages.add(instructionMessage);
		return new AgentCommand(newMessages);
	}

	@Override
	public String getName() {
		return "InstructionAgentHook";
	}

	@Override
	public int getOrder() {
		return -100;
	}

	@Override
	public ReactAgent getAgent() {
		return reactAgent;
	}

	@Override
	public void setAgent(ReactAgent agent) {
		this.reactAgent = agent;
	}

	/**
	 * Create the default InstructionAgentHook instance (used when no other hook handles instruction).
	 * @return a new InstructionAgentHook
	 */
	public static InstructionAgentHook create() {
		return new InstructionAgentHook();
	}
}
