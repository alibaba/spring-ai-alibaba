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

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.serializer.AgentInstructionMessage;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.execution.ToolCallResultConverter;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.ai.util.json.JsonParser;
import org.springframework.ai.util.json.schema.JsonSchemaGenerator;

import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;

public class AgentTool implements BiFunction<String, ToolContext, AssistantMessage> {

	/**
	 * Framework reserved input field name for DeepSeek API compatibility.
	 * This field name is used in the default schema when no inputSchema is provided,
	 * to meet DeepSeek API's requirement that function schemas must be object type.
	 */
	private static final String FRAMEWORK_DEEPSEEK_RESERVED_INPUT_FIELD = "saaDefaultDeepseekInput";
	private static final ToolCallResultConverter CONVERTER = new MessageToolCallResultConverter();
	private final ReactAgent agent;

	public AgentTool(ReactAgent agent) {
		this.agent = agent;
	}

	public static AgentTool create(ReactAgent agent) {
		return new AgentTool(agent);
	}

	public static ToolCallback getFunctionToolCallback(ReactAgent agent) {
		// convert agent inputType to json schema
		String inputSchema = StringUtils.hasLength(agent.getInputSchema())
				? agent.getInputSchema()
				: (agent.getInputType() != null)
				? JsonSchemaGenerator.generateForType(agent.getInputType())
				: null;

		// If inputSchema is null, provide a default object schema with a string input property
		// This is required by some APIs (like DeepSeek) that require function schemas to be object type
		// Use a unique field name to avoid conflicts with user-defined schemas
		if (inputSchema == null) {
			inputSchema = String.format("""
					{
						"type": "object",
						"properties": {
							"%s": {
								"type": "string",
								"description": "The input text for the agent"
							}
						},
						"required": ["%s"]
					}
					""", FRAMEWORK_DEEPSEEK_RESERVED_INPUT_FIELD, FRAMEWORK_DEEPSEEK_RESERVED_INPUT_FIELD);
		}

		return FunctionToolCallback.builder(agent.name(), AgentTool.create(agent))
				.description(agent.description())
				.inputType(String.class) // the inputType for ToolCallback is always String
				.inputSchema(inputSchema)
				.toolCallResultConverter(CONVERTER)
				.build();
	}

	@Override
	public AssistantMessage apply(String input, ToolContext toolContext) {
//		OverAllState state = (OverAllState) toolContext.getContext().get(AGENT_STATE_CONTEXT_KEY);
		try {
			// Extract the actual input text from the input parameter
			// If input is a JSON object like {"input": "text"}, extract the "input" field
			// Otherwise, use the input as-is
			String actualInput = extractInputText(input);

			// Build the messages list to add
			// Add instruction first if present, then the user input
			// Note: We must add all messages at once because cloneState doesn't copy keyStrategies,
			// so multiple updateState calls would overwrite instead of append
			List<Message> messagesToAdd = new ArrayList<>();
			if (StringUtils.hasLength(agent.instruction())) {
				messagesToAdd.add(new AgentInstructionMessage(agent.instruction()));
			}
			messagesToAdd.add(new UserMessage(actualInput));

			Optional<OverAllState> resultState = agent.getAndCompileGraph().invoke(Map.of("messages", messagesToAdd));

			Optional<List> messages = resultState.flatMap(overAllState -> overAllState.value("messages", List.class));
			if (messages.isPresent()) {
				@SuppressWarnings("unchecked")
				List<Message> messageList = (List<Message>) messages.get();
				// Use messageList
				AssistantMessage assistantMessage = (AssistantMessage) messageList.get(messageList.size() - 1);
				return assistantMessage;
			}
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
		throw new RuntimeException("Failed to execute agent tool or failed to get agent tool result");
	}

	/**
	 * Extract the actual input text from the input parameter.
	 * If input is a JSON object with the framework reserved field, extract it.
	 * Otherwise, use the input as-is.
	 */
	private String extractInputText(String input) {
		if (!StringUtils.hasText(input)) {
			return input;
		}

		// Try to parse as JSON object
		try {
			Map<String, Object> jsonMap = JsonParser.toMap(input);
			// If it's a JSON object and contains the framework reserved field, extract it
			if (jsonMap != null && jsonMap.containsKey(FRAMEWORK_DEEPSEEK_RESERVED_INPUT_FIELD)) {
				Object inputValue = jsonMap.get(FRAMEWORK_DEEPSEEK_RESERVED_INPUT_FIELD);
				return inputValue != null ? inputValue.toString() : input;
			}
		}
		catch (Exception e) {
			// Not a JSON object, use input as-is
		}

		return input;
	}

}
