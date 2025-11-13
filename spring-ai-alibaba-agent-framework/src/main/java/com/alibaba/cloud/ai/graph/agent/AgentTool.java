/*
 * Copyright 2024-2025 the original author or authors.
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

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.serializer.AgentInstructionMessage;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.execution.ToolCallResultConverter;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.ai.util.json.schema.JsonSchemaGenerator;

import org.springframework.util.StringUtils;

import static com.alibaba.cloud.ai.graph.agent.tools.ToolContextConstants.AGENT_STATE_CONTEXT_KEY;

public class AgentTool implements BiFunction<String, ToolContext, AssistantMessage> {

	private final ReactAgent agent;

	public AgentTool(ReactAgent agent) {
		this.agent = agent;
	}

	@Override
	public AssistantMessage apply(String input, ToolContext toolContext) {
		OverAllState state = (OverAllState) toolContext.getContext().get(AGENT_STATE_CONTEXT_KEY);
		try {
			// Copy state to avoid affecting the original state.
			// The agent that calls this tool should only be aware of the ToolCallChoice and ToolResponse.
			OverAllState newState = agent.getAndCompileGraph().cloneState(state.data());
			
			// Build the messages list to add
			// Add instruction first if present, then the user input
			// Note: We must add all messages at once because cloneState doesn't copy keyStrategies,
			// so multiple updateState calls would overwrite instead of append
			java.util.List<Message> messagesToAdd = new java.util.ArrayList<>();
			if (StringUtils.hasLength(agent.instruction())) {
				messagesToAdd.add(new AgentInstructionMessage(agent.instruction()));
			}
			messagesToAdd.add(new UserMessage(input));
			
			Map<String, Object> inputs = newState.updateState(Map.of("messages", messagesToAdd));

			Optional<OverAllState> resultState = agent.getAndCompileGraph().invoke(inputs);

			Optional<List> messages = resultState.flatMap(overAllState -> overAllState.value("messages", List.class));
			if (messages.isPresent()) {
				@SuppressWarnings("unchecked")
				List<Message> messageList = (List<Message>) messages.get();
				// Use messageList
				AssistantMessage assistantMessage = (AssistantMessage)messageList.get(messageList.size() - 1);
				return assistantMessage;
			}
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
		throw new RuntimeException("Failed to execute agent tool or failed to get agent tool result");
	}

	private static final ToolCallResultConverter CONVERTER = new MessageToolCallResultConverter();

	public static AgentTool create(ReactAgent agent) {
		return new AgentTool(agent);
	}

	public static ToolCallback getFunctionToolCallback(ReactAgent agent) {
		// convert agent inputType to json schema
		String inputSchema = StringUtils.hasLength(agent.getInputSchema())
				? agent.getInputSchema()
				: (agent.getInputType() != null )
					? JsonSchemaGenerator.generateForType(agent.getInputType())
					: null;

		return FunctionToolCallback.builder(agent.name(), AgentTool.create(agent))
			.description(agent.description())
			.inputType(String.class) // the inputType for ToolCallback is always String
			.inputSchema(inputSchema)
			.toolCallResultConverter(CONVERTER)
			.build();
	}

}
