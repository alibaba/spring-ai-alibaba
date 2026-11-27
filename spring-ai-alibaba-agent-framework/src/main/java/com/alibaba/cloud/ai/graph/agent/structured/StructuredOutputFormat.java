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

import com.networknt.schema.Error;
import com.networknt.schema.Schema;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SpecificationVersion;

import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.converter.MarkdownCodeBlockCleaner;
import org.springframework.ai.converter.ThinkingTagCleaner;
import org.springframework.ai.util.JacksonUtils;
import org.springframework.util.StringUtils;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectReader;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/** Compiles the JSON schema and validates completed answers before they enter graph state. */
public final class StructuredOutputFormat {

	public static final String RESULT_TOOL_NAME = "__saa_structured_output";

	public static final String RESULT_METADATA = "spring.ai.alibaba.structured_output";

	private static final Pattern JSON_BLOCK = Pattern.compile("```(?:json)?\\s*(\\{.*?\\})\\s*```",
			Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

	private static final JsonMapper MAPPER = JsonMapper.builder()
			.addModules(JacksonUtils.instantiateAvailableModules())
			.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
			.build();

	private static final ObjectReader JSON = MAPPER.reader().with(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);

	private static final MarkdownCodeBlockCleaner MARKDOWN = new MarkdownCodeBlockCleaner();

	private static final ThinkingTagCleaner THINKING = new ThinkingTagCleaner();

	private final JsonNode jsonSchema;

	private final Schema validator;

	private final Class<?> type;

	private final StructuredOutputStrategy strategy;

	private final int maxRetries;

	private final String instructions;

	private StructuredOutputFormat(JsonNode jsonSchema, Class<?> type, StructuredOutputStrategy strategy,
			int maxRetries, String instructions) {
		this.jsonSchema = jsonSchema;
		this.validator = SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_2020_12).getSchema(jsonSchema);
		this.type = type;
		this.strategy = strategy;
		this.maxRetries = maxRetries;
		this.instructions = instructions;
	}

	/**
	 * Formats supplied as free-form text alone remain prompt instructions. A JSON
	 * Schema or an output type enables schema validation and strategy selection.
	 */
	public static StructuredOutputFormat from(String schema, Class<?> type, StructuredOutputStrategy strategy,
			int maxRetries) {
		String instructions = StringUtils.hasText(schema) ? schema : null;
		JsonNode json = instructions == null ? null : readSchema(instructions);
		if (json == null && type != null) {
			var converter = new BeanOutputConverter<>(type);
			json = JSON.readTree(converter.getJsonSchema());
			if (instructions == null) {
				instructions = converter.getFormat();
			}
		}
		if (json == null) {
			if (strategy != StructuredOutputStrategy.AUTO && strategy != StructuredOutputStrategy.PROMPT) {
				throw new IllegalArgumentException("Native and tool structured output require a JSON Schema");
			}
			return null;
		}
		if (instructions.stripLeading().startsWith("{")) {
			instructions = "Return only JSON that matches this schema:\n" + instructions;
		}
		return new StructuredOutputFormat(json, type, strategy, maxRetries, instructions);
	}

	private static JsonNode readSchema(String instructions) {
		String content = instructions.strip();
		if (content.startsWith("{")) {
			try {
				return JSON.readTree(content);
			}
			catch (JacksonException ex) {
				throw new IllegalArgumentException("Invalid output JSON Schema", ex);
			}
		}
		var block = JSON_BLOCK.matcher(content);
		if (block.find()) {
			return readEmbeddedSchema(block.group(1));
		}
		int start = content.indexOf('{');
		if (start >= 0 && content.endsWith("}")) {
			return readEmbeddedSchema(content.substring(start));
		}
		return null;
	}

	private static JsonNode readEmbeddedSchema(String candidate) {
		try {
			var json = JSON.readTree(candidate);
			// Legacy instructions often embed an example object rather than a schema.
			if (json.has("$schema") || json.has("$ref") || json.has("properties") || json.has("items")
					|| json.has("required") || json.has("oneOf") || json.has("anyOf") || json.has("allOf")) {
				return json;
			}
			var type = json.path("type").asText();
			return switch (type) {
				case "object", "array", "string", "integer", "number", "boolean", "null" -> json;
				default -> null;
			};
		}
		catch (JacksonException ex) {
			return null;
		}
	}

	public String jsonSchema() {
		return jsonSchema.toString();
	}

	public boolean supportsToolSchema() {
		JsonNode schemaType = jsonSchema.get("type");
		return schemaType == null ? jsonSchema.has("properties") : "object".equals(schemaType.asText());
	}

	public StructuredOutputStrategy strategy() {
		return strategy;
	}

	public int maxRetries() {
		return maxRetries;
	}

	public String instructions() {
		return instructions;
	}

	public String validate(String content) {
		if (!StringUtils.hasText(content)) {
			throw new StructuredOutputException("The model returned an empty structured answer");
		}
		JsonNode result;
		try {
			result = JSON.readTree(MARKDOWN.clean(THINKING.clean(content.strip())).strip());
		}
		catch (JacksonException ex) {
			throw new StructuredOutputException("The model did not return a single valid JSON value", ex);
		}
		if (result == null || result.isNull() || result.isMissingNode()) {
			throw new StructuredOutputException("The model returned no structured value");
		}
		List<Error> errors = validator.validate(result);
		if (!errors.isEmpty()) {
			var violations = errors.stream().limit(5)
					.map(error -> error.getInstanceLocation() + " violates " + error.getKeyword()).toList();
			throw new StructuredOutputException("JSON Schema validation failed: " + String.join(", ", violations));
		}

		String json = result.toString();
		if (type != null) {
			decode(json, type);
		}
		return json;
	}

	public static <T> T decode(String json, Class<T> type) {
		Objects.requireNonNull(type, "Structured output type is required");
		try {
			return MAPPER.readValue(json, type);
		}
		catch (JacksonException ex) {
			throw new StructuredOutputException("JSON could not be mapped to " + type.getSimpleName(), ex);
		}
	}

}
