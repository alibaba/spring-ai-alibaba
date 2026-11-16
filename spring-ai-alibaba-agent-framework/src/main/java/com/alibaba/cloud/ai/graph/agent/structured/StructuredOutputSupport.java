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
package com.alibaba.cloud.ai.graph.agent.structured;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Support for the tool-call based structured output strategy of the agent framework.
 *
 * <p>
 * When structured output is requested through a JSON schema or a Java type, the agent
 * registers a {@code structured_output} tool whose input schema is the requested output
 * schema and asks the model to call that tool with its final result. The tool call
 * arguments are then parsed and exposed through the {@link #STATE_KEY} state key, so
 * callers do not have to parse the assistant message themselves.
 * </p>
 */
public final class StructuredOutputSupport {

	/**
	 * State key holding the structured result of the agent.
	 */
	public static final String STATE_KEY = "structured_output";

	/**
	 * Name of the internal tool that carries the structured result.
	 */
	public static final String TOOL_NAME = "structured_output";

	/**
	 * Instruction appended to the user message to tell the model how to return the final
	 * structured result.
	 */
	public static final String INSTRUCTION = "When you have the final result, call the '"
			+ TOOL_NAME
			+ "' tool once with a JSON object that matches its schema. Do not repeat that JSON object in a plain text reply.";

	/**
	 * Prefix used by {@link com.alibaba.cloud.ai.graph.agent.node.AgentLlmNode} for a
	 * model call that failed.
	 */
	public static final String MODEL_FAILURE_PREFIX = "Exception: ";

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	private StructuredOutputSupport() {
	}

	/**
	 * Whether the given value is a JSON object that can be used as a tool input schema.
	 * @param value the candidate value
	 * @return true when the value is a JSON object schema
	 */
	public static boolean isJsonSchema(String value) {
		if (!StringUtils.hasText(value)) {
			return false;
		}
		String trimmed = value.trim();
		if (!trimmed.startsWith("{") || !trimmed.endsWith("}")) {
			return false;
		}
		try {
			Map<String, Object> schema = OBJECT_MAPPER.readValue(trimmed, new TypeReference<>() {
			});
			return isObjectSchema(schema);
		}
		catch (Exception ex) {
			return false;
		}
	}

	/**
	 * Resolve the JSON object schema used by the tool-call structured output strategy.
	 * <p>
	 * An explicit output schema takes precedence over {@code outputType}, matching the
	 * prompt based behavior. Only object schemas can be used as tool input; a Java type
	 * that does not map to a JSON object (for example {@code Double}) and plain prompt
	 * instructions keep the prompt based behavior.
	 * </p>
	 * @param outputSchema the configured output schema or format instructions
	 * @param outputType the configured output type
	 * @return the JSON object schema, or {@code null} when prompt based output should be
	 * used
	 */
	public static String resolveObjectSchema(String outputSchema, Class<?> outputType) {
		if (StringUtils.hasLength(outputSchema)) {
			return isJsonSchema(outputSchema) ? outputSchema.trim() : null;
		}
		if (outputType == null) {
			return null;
		}
		try {
			String candidate = new BeanOutputConverter<>(outputType).getJsonSchema();
			return isJsonSchema(candidate) ? candidate.trim() : null;
		}
		catch (Exception ex) {
			return null;
		}
	}

	/**
	 * Resolve the Java type used to convert a structured result.
	 * <p>
	 * When an explicit schema wins over {@code outputType}, no Java type conversion is
	 * attempted because the two may not match.
	 * </p>
	 * @param outputSchema the configured output schema or format instructions
	 * @param outputType the configured output type
	 * @return the conversion type, or {@code null} to keep the result as a map
	 */
	public static Class<?> resolveConversionType(String outputSchema, Class<?> outputType) {
		return StringUtils.hasLength(outputSchema) ? null : outputType;
	}

	/**
	 * Build the assistant message used when the model call failed.
	 * @param error the model call error
	 * @return the failure message
	 */
	public static AssistantMessage modelFailure(Throwable error) {
		return AssistantMessage.builder().content(MODEL_FAILURE_PREFIX + error.getMessage()).build();
	}

	/**
	 * Whether the assistant message represents a failed model call rather than a model
	 * answer.
	 * @param message the assistant message
	 * @return true when the message is a model failure
	 */
	public static boolean isModelFailure(AssistantMessage message) {
		String text = message.getText();
		return text != null && text.startsWith(MODEL_FAILURE_PREFIX);
	}

	/**
	 * Create the internal tool that carries the structured result.
	 * @param jsonSchema the JSON schema of the structured result
	 * @return the tool callback
	 */
	public static ToolCallback createToolCallback(String jsonSchema) {
		return FunctionToolCallback
			.builder(TOOL_NAME, (String arguments, ToolContext context) -> arguments)
			.description("Return the final answer as a JSON object that matches the provided schema.")
			.inputType(String.class)
			.inputSchema(jsonSchema)
			.build();
	}

	/**
	 * Finalize a model response that carries the structured result.
	 * <p>
	 * When the only tool call is the internal {@code structured_output} call, the tool
	 * call is replaced by a text-only assistant message so the ReAct loop can stop and
	 * callers of {@code agent.call(...)} receive the JSON result. When the model mixes an
	 * ordinary tool call with the internal call, the ordinary calls are kept and routed to
	 * the tool node, and the model is asked to finalize again on the next round.
	 * </p>
	 * @param message the assistant message returned by the model
	 * @return the message to store, or empty when no finalization is required
	 */
	public static Optional<AssistantMessage> finalizeToolCallMessage(AssistantMessage message) {
		if (message == null) {
			return Optional.empty();
		}

		List<AssistantMessage.ToolCall> toolCalls = message.getToolCalls();
		if (toolCalls == null || toolCalls.isEmpty()) {
			return Optional.empty();
		}

		AssistantMessage.ToolCall structuredOutputCall = null;
		List<AssistantMessage.ToolCall> ordinaryCalls = new ArrayList<>();
		for (AssistantMessage.ToolCall toolCall : toolCalls) {
			if (TOOL_NAME.equals(toolCall.name())) {
				structuredOutputCall = toolCall;
			}
			else {
				ordinaryCalls.add(toolCall);
			}
		}
		if (structuredOutputCall == null) {
			return Optional.empty();
		}

		if (!ordinaryCalls.isEmpty()) {
			return Optional.of(AssistantMessage.builder()
				.content(message.getText())
				.properties(message.getMetadata())
				.toolCalls(ordinaryCalls)
				.build());
		}

		String json = StringUtils.hasText(structuredOutputCall.arguments()) ? structuredOutputCall.arguments() : "{}";
		return Optional.of(AssistantMessage.builder()
			.content(json)
			.properties(message.getMetadata())
			.build());
	}

	/**
	 * Parse the structured result from an assistant message text.
	 * @param text the assistant message text
	 * @param outputType the requested output type, or {@code null} to keep a map
	 * @return the parsed value, or {@code null} when the text is not a valid structured
	 * result
	 */
	public static Object parseStructuredOutput(String text, Class<?> outputType) {
		String json = extractJson(text);
		if (!StringUtils.hasText(json)) {
			return null;
		}
		return convert(json, outputType);
	}

	private static boolean isObjectSchema(Map<String, Object> schema) {
		if (schema == null || schema.isEmpty()) {
			return false;
		}
		Object type = schema.get("type");
		if ("object".equals(type)) {
			return true;
		}
		// Schemas generated from a Java type may omit "type" but list properties.
		return schema.get("properties") instanceof Map<?, ?>;
	}

	private static String extractJson(String text) {
		if (!StringUtils.hasText(text)) {
			return null;
		}
		String trimmed = text.trim();
		if (trimmed.startsWith("```")) {
			int bodyStart = trimmed.indexOf('\n');
			int bodyEnd = trimmed.lastIndexOf("```");
			if (bodyStart >= 0 && bodyEnd > bodyStart) {
				trimmed = trimmed.substring(bodyStart + 1, bodyEnd).trim();
			}
		}
		int start = trimmed.indexOf('{');
		int end = trimmed.lastIndexOf('}');
		if (start < 0 || end <= start) {
			return null;
		}
		return trimmed.substring(start, end + 1);
	}

	private static Object convert(String json, Class<?> outputType) {
		try {
			if (outputType != null) {
				return new BeanOutputConverter<>(outputType).convert(json);
			}
			return OBJECT_MAPPER.readValue(json, new TypeReference<Map<String, Object>>() {
			});
		}
		catch (Exception ex) {
			return null;
		}
	}

}
