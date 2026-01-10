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

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.serializer.AgentInstructionMessage;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.DefaultToolDefinition;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.execution.ToolCallResultConverter;
import org.springframework.ai.tool.method.MethodToolCallback;
import org.springframework.ai.tool.support.ToolDefinitions;
import org.springframework.ai.util.json.JsonParser;
import org.springframework.ai.util.json.schema.JsonSchemaGenerator;

import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Factory class for creating MethodToolCallback instances for ReactAgent.
 * This class provides a programmatic way to create MethodToolCallback without requiring @Tool annotation.
 */
public class AgentTool {
	private static final ToolCallResultConverter CONVERTER = new MessageToolCallResultConverter();

	/**
	 * Create a ToolCallback using MethodToolCallback.
	 * This is a convenience method that delegates to AgentMethodToolCallback.create().
	 *
	 * @param agent the ReactAgent instance
	 * @return ToolCallback created with MethodToolCallback
	 * @see AgentTool#create(ReactAgent)
	 */
	public static ToolCallback getFunctionToolCallback(ReactAgent agent) {
		return AgentTool.create(agent);
	}

	/**
	 * Create a ToolCallback using MethodToolCallback.
	 * This method uses reflection to find the executeAgent method and builds MethodToolCallback
	 * directly without requiring @Tool annotation.
	 * 
	 * @param agent the ReactAgent instance
	 * @return ToolCallback created with MethodToolCallback
	 */
	public static ToolCallback create(ReactAgent agent) {
		// Find the executeAgent method using reflection
		java.lang.reflect.Method method = ReflectionUtils.findMethod(AgentToolExecutor.class, 
				"executeAgent", String.class, ToolContext.class);
		
		if (method == null) {
			throw new IllegalStateException("Could not find executeAgent method in AgentToolExecutor class");
		}

		// Get the original schema from inputSchema or inputType
		String originalSchema = StringUtils.hasLength(agent.getInputSchema())
				? agent.getInputSchema()
				: (agent.getInputType() != null)
				? JsonSchemaGenerator.generateForType(agent.getInputType())
				: null;

		// Wrap the original schema in an "input" parameter

		// Create ToolDefinition using ToolDefinition.builder() with the method
		DefaultToolDefinition.Builder builder = ToolDefinitions.builder(method)
				.name(agent.name())
				.description(agent.description());

		if (StringUtils.hasLength(originalSchema)) {
			String wrappedInputSchema = wrapSchemaInInputParameter(originalSchema);
			builder.inputSchema(wrappedInputSchema);
		}

		ToolDefinition toolDefinition = builder.build();

				// Create the executor instance
		AgentToolExecutor executor = new AgentToolExecutor(agent);

		// Build MethodToolCallback
		return MethodToolCallback.builder()
				.toolDefinition(toolDefinition)
				.toolMethod(method)
				.toolObject(executor)
				.toolCallResultConverter(CONVERTER)
				.build();
	}

	/**
	 * Wrap the original schema in an "input" parameter.
	 * This method handles both JSON Schema strings (including DRAFT_2020_12 format) and null cases.
	 * 
	 * @param originalSchema the original schema string (may be null, or DRAFT_2020_12 format)
	 * @return a wrapped schema with "input" parameter containing the original schema
	 */
	private static String wrapSchemaInInputParameter(String originalSchema) {
		ObjectMapper objectMapper = JsonParser.getObjectMapper();
		
		try {
			// Parse the original schema if provided
			Map<String, Object> originalSchemaMap = null;
			if (StringUtils.hasLength(originalSchema)) {
				try {
					originalSchemaMap = objectMapper.readValue(originalSchema, new TypeReference<HashMap<String, Object>>() {});
				}
				catch (Exception e) {
					// If parsing fails, treat as a simple string type
					originalSchemaMap = Map.of("type", "string");
				}
			}
			else {
				// Default to string type if no schema provided
				originalSchemaMap = Map.of("type", "string");
			}

			// Create the wrapped schema structure
			Map<String, Object> wrappedSchema = new HashMap<>();
			wrappedSchema.put("type", "object");
			
			Map<String, Object> properties = new HashMap<>();
			properties.put("input", originalSchemaMap);
			wrappedSchema.put("properties", properties);
			
			wrappedSchema.put("required", List.of("input"));

			// Convert to JSON string
			return objectMapper.writeValueAsString(wrappedSchema);
		}
		catch (Exception e) {
			// Fallback: create a simple wrapper schema
			return String.format("""
					{
						"type": "object",
						"properties": {
							"input": {
								"type": "string"
							}
						},
						"required": ["input"]
					}
					""");
		}
	}

	/**
	 * Executor class for AgentTool that contains the method to be used by MethodToolCallback.
	 * This class does not require @Tool annotation, as the method is registered via reflection.
	 */
	public static class AgentToolExecutor {

		private final ReactAgent agent;

		public AgentToolExecutor(ReactAgent agent) {
			this.agent = agent;
		}

		/**
		 * Execute the agent tool with the given input.
		 * This method is used by MethodToolCallback via reflection.
		 * The ToolContext parameter is automatically injected by Spring AI and is not exposed to the LLM.
		 * 
		 * @param input the input JSON string containing the "input" parameter
		 * @param toolContext the tool context containing state, config, etc. (automatically injected)
		 * @return AssistantMessage the response from the agent
		 */
		public AssistantMessage executeAgent(String input, ToolContext toolContext) {
			// Extract the actual input value from the wrapped JSON structure
			// The input parameter is wrapped as {"input": "actual_value"}
			String actualInput = extractInputValue(input);

			// Build the messages list to add
			// Add instruction first if present, then the user input
			// Note: We must add all messages at once because cloneState doesn't copy keyStrategies,
			// so multiple updateState calls would overwrite instead of append
			List<Message> messagesToAdd = new ArrayList<>();
			if (StringUtils.hasLength(agent.instruction())) {
				messagesToAdd.add(AgentInstructionMessage.builder().text(agent.instruction()).build());
			}
			messagesToAdd.add(new UserMessage(actualInput));

			Optional<OverAllState> resultState = agent.getAndCompileGraph().invoke(Map.of("messages", messagesToAdd));

			Optional<List> messages = resultState.flatMap(overAllState -> overAllState.value("messages", List.class));
			if (messages.isPresent()) {
				@SuppressWarnings("unchecked")
				List<Message> messageList = (List<Message>) messages.get();
				// Use messageList
				return (AssistantMessage) messageList.get(messageList.size() - 1);
			}
			
			throw new RuntimeException("Failed to execute agent tool or failed to get agent tool result");
		}

		/**
		 * Extract the actual input value from the wrapped JSON structure.
		 * The input is expected to be in the format: {"input": "actual_value"}
		 * If the input is not a valid JSON object or doesn't contain "input" field,
		 * the original input string is returned as-is.
		 * 
		 * @param input the wrapped input JSON string
		 * @return the extracted input value, or the original input if extraction fails
		 */
		private String extractInputValue(String input) {
			if (!StringUtils.hasText(input)) {
				return input;
			}

			// Try to parse as JSON object and extract the "input" field
			try {
				ObjectMapper objectMapper = JsonParser.getObjectMapper();
				Map<String, Object> jsonMap = objectMapper.readValue(input, new TypeReference<HashMap<String, Object>>() {});

				if (jsonMap != null && jsonMap.containsKey("input")) {
					Object inputValue = jsonMap.get("input");
					// Convert the input value to string
					if (inputValue != null) {
						// If it's already a string, return it
						if (inputValue instanceof String) {
							return (String) inputValue;
						}
						// Otherwise, serialize it to JSON string
						return JsonParser.getObjectMapper().writeValueAsString(inputValue);
					}
				}
			}
			catch (Exception e) {
				// Not a JSON object or parsing failed, use input as-is
			}

			// If extraction fails, return the original input
			return input;
		}
	}
}

