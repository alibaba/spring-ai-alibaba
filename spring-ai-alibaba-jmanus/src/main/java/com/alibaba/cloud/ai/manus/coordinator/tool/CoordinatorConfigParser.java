/*
 * Copyright 2025 the original author or authors.
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

package com.alibaba.cloud.ai.manus.coordinator.tool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.alibaba.cloud.ai.manus.coordinator.entity.vo.CoordinatorConfigVO;
import com.alibaba.cloud.ai.manus.coordinator.entity.vo.CoordinatorParameterVO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * CoordinatorConfig Parser
 *
 * Optimized JSON parser for coordinator configuration with improved performance and
 * maintainability Also handles tool conversion and schema generation
 */
@Component
public class CoordinatorConfigParser {

	private static final Logger logger = LoggerFactory.getLogger(CoordinatorConfigParser.class);

	// Pre-compiled regex pattern for better performance
	private static final Pattern PARAMETER_PATTERN = Pattern.compile("\\{([^}]+)\\}");

	// JSON Schema templates for better performance
	private static final String SCHEMA_HEADER = """
			{
				"$schema": "http://json-schema.org/draft-07/schema#",
				"type": "object",
				"properties": {
			""";

	private static final String SCHEMA_FOOTER = """
				},
				"required": []
			}
			""";

	private static final String PROPERTY_TEMPLATE = """
			"%s": {
				"type": "%s",
				"description": "%s"
			}""";

	private static final String REQUIRED_ARRAY_TEMPLATE = """
			"required": [
				%s
			]""";

	// Default JSON Schema for empty or invalid input
	private static final String DEFAULT_SCHEMA = SCHEMA_HEADER + SCHEMA_FOOTER;

	private final ObjectMapper objectMapper;

	private final JsonSchemaBuilder schemaBuilder;

	public CoordinatorConfigParser() {
		this.objectMapper = new ObjectMapper();
		this.schemaBuilder = new JsonSchemaBuilder();
	}

	/**
	 * Convert Plan JSON string to CoordinatorConfigVO
	 * @param planJson JSON string containing plan configuration
	 * @return CoordinatorConfigVO object
	 * @throws IllegalArgumentException if JSON is invalid or missing required fields
	 */
	public CoordinatorConfigVO parse(String planJson) {
		logger.info("Starting to convert Plan JSON: {}", planJson != null ? "not null" : "null");

		if (planJson == null || planJson.trim().isEmpty()) {
			throw new IllegalArgumentException("Plan JSON cannot be empty");
		}

		try {
			// Parse JSON using optimized method
			JsonNode rootNode = JsonUtils.parseJson(objectMapper, planJson, "Plan JSON");

			// Validate required fields
			validatePlanJson(rootNode);

			// Build configuration object
			CoordinatorConfigVO config = buildConfigFromJson(rootNode);

			logger.info("Conversion completed, generated config: {}", config);
			return config;

		}
		catch (IllegalArgumentException e) {
			logger.error("Conversion failed", e);
			throw e;
		}
		catch (Exception e) {
			logger.error("Unknown exception occurred during conversion", e);
			throw new IllegalArgumentException("Conversion failed: " + e.getMessage(), e);
		}
	}

	/**
	 * Validate required fields in Plan JSON
	 */
	private void validatePlanJson(JsonNode rootNode) {
		if (rootNode == null) {
			throw new IllegalArgumentException("Plan JSON parsing failed, root node is null");
		}

		if (!rootNode.isObject()) {
			throw new IllegalArgumentException("Plan JSON must be in object format");
		}

		// Check required fields
		if (JsonUtils.getStringValue(rootNode, "planId") == null) {
			throw new IllegalArgumentException("Plan JSON missing required field: planId");
		}
	}

	/**
	 * Build configuration object from JSON node
	 */
	private CoordinatorConfigVO buildConfigFromJson(JsonNode rootNode) {
		CoordinatorConfigVO config = new CoordinatorConfigVO();
		config.setId(JsonUtils.getStringValue(rootNode, "planId"));
		config.setName(JsonUtils.getStringValue(rootNode, "title"));
		config.setDescription(JsonUtils.getStringValue(rootNode, "userRequest"));
		config.setEndpoint(JsonUtils.getStringValue(rootNode, "endpoint"));

		// Parse steps to parameters
		config.setParameters(parseStepsToParameters(rootNode));

		return config;
	}

	/**
	 * Parse steps to parameter list
	 */
	private List<CoordinatorParameterVO> parseStepsToParameters(JsonNode rootNode) {
		JsonNode stepsNode = rootNode.get("steps");
		if (stepsNode == null || !stepsNode.isArray()) {
			return List.of();
		}

		List<String> steps = new ArrayList<>();
		for (JsonNode stepNode : stepsNode) {
			// Handle new format: steps is object array, each object contains
			// stepRequirement field
			if (stepNode.isObject()) {
				JsonNode stepRequirementNode = stepNode.get("stepRequirement");
				if (stepRequirementNode != null && stepRequirementNode.isTextual()) {
					steps.add(stepRequirementNode.asText());
				}
			}
			// Compatible with old format: steps is string array
			else if (stepNode.isTextual()) {
				steps.add(stepNode.asText());
			}
		}

		return parseParameters(steps);
	}

	/**
	 * Parse parameter list from steps
	 */
	public List<CoordinatorParameterVO> parseParameters(List<String> steps) {
		if (steps == null || steps.isEmpty()) {
			return new ArrayList<>();
		}

		// Extract all parameter names
		Set<String> parameterNames = extractParameterNames(steps);

		// Convert to parameter objects
		List<CoordinatorParameterVO> parameters = new ArrayList<>();
		for (String paramName : parameterNames) {
			parameters.add(createParameter(paramName));
		}

		return parameters;
	}

	/**
	 * Extract parameter names from steps using optimized regex
	 */
	private Set<String> extractParameterNames(List<String> steps) {
		Set<String> parameterNames = new HashSet<>();

		for (String step : steps) {
			if (step != null) {
				Matcher matcher = PARAMETER_PATTERN.matcher(step);
				while (matcher.find()) {
					String paramName = matcher.group(1);
					if (paramName != null && !paramName.trim().isEmpty()) {
						parameterNames.add(paramName.trim());
					}
				}
			}
		}

		return parameterNames;
	}

	/**
	 * Create parameter object with default values
	 */
	private CoordinatorParameterVO createParameter(String paramName) {
		CoordinatorParameterVO parameter = new CoordinatorParameterVO();
		parameter.setName(paramName);
		parameter.setType("String");
		parameter.setDescription(paramName);
		parameter.setRequired(true);

		logger.debug("Created parameter object: {}", parameter);
		return parameter;
	}

	/**
	 * Convert JSON string to tool Schema using optimized builder pattern
	 * @param json JSON string, format like: [{"name":"name","description":"Parameter:
	 * name","type":"string"}]
	 * @return Converted JSON Schema string
	 */
	public String generateToolSchema(String json) {
		if (json == null || json.trim().isEmpty()) {
			logger.warn("JSON string is empty, returning default Schema");
			return DEFAULT_SCHEMA;
		}

		try {
			// Parse JSON array using optimized method
			JsonNode parametersArray = JsonUtils.parseJson(objectMapper, json, "Tool Schema JSON");

			if (!parametersArray.isArray()) {
				logger.warn("JSON is not in array format, returning default Schema");
				return DEFAULT_SCHEMA;
			}

			// Use builder pattern for better performance and maintainability
			return schemaBuilder.buildSchema(parametersArray);

		}
		catch (Exception e) {
			logger.error("Exception occurred while generating tool Schema: {}", e.getMessage(), e);
			return DEFAULT_SCHEMA;
		}
	}

	/**
	 * Convert CoordinatorConfigVO to CoordinatorTool
	 * @param config CoordinatorConfigVO configuration object
	 * @return CoordinatorTool object
	 */
	public CoordinatorTool convertToCoordinatorTool(CoordinatorConfigVO config) {
		if (config == null) {
			logger.warn("CoordinatorConfigVO is null, cannot convert to CoordinatorTool");
			return null;
		}

		CoordinatorTool tool = new CoordinatorTool();

		// Set endpoint default to example
		tool.setEndpoint("example");

		// Assign config properties to tool
		if (config.getName() != null && !config.getName().trim().isEmpty()) {
			tool.setToolName(config.getId());
		}

		if (config.getDescription() != null && !config.getDescription().trim().isEmpty()) {
			tool.setToolDescription(config.getDescription());
		}

		// Use convertParametersToSchema to convert parameters and set to tool
		String schema = convertParametersToSchema(config);
		tool.setToolSchema(schema);

		logger.debug("Successfully converted CoordinatorConfigVO to CoordinatorTool: {}", tool.getToolName());
		return tool;
	}

	/**
	 * Batch convert CoordinatorConfigVO list to CoordinatorTool list
	 * @param configs CoordinatorConfigVO configuration list
	 * @return CoordinatorTool list
	 */
	public List<CoordinatorTool> convertToCoordinatorTools(List<CoordinatorConfigVO> configs) {
		if (configs == null || configs.isEmpty()) {
			logger.warn("CoordinatorConfigVO list is empty, cannot convert");
			return new ArrayList<>();
		}

		List<CoordinatorTool> tools = new ArrayList<>();
		for (CoordinatorConfigVO config : configs) {
			CoordinatorTool tool = convertToCoordinatorTool(config);
			if (tool != null) {
				tools.add(tool);
			}
		}

		logger.info("Successfully converted {} CoordinatorConfigVO to CoordinatorTool", tools.size());
		return tools;
	}

	/**
	 * Convert CoordinatorConfigVO parameters to JSON Schema
	 * @param config CoordinatorConfigVO configuration object
	 * @return JSON Schema string
	 */
	public String convertParametersToSchema(CoordinatorConfigVO config) {
		if (config == null || config.getParameters() == null) {
			logger.warn("CoordinatorConfigVO or parameters is null, returning default Schema");
			return DEFAULT_SCHEMA;
		}

		try {
			// Convert parameters to JSON format, then call generateToolSchema method
			String json = objectMapper.writeValueAsString(config.getParameters());
			logger.debug("Converted parameters to JSON format: {}", json);

			// Call existing generateToolSchema method, reuse Schema generation logic
			return generateToolSchema(json);
		}
		catch (Exception e) {
			logger.error("Error occurred while generating JSON Schema: {}", e.getMessage(), e);
			return DEFAULT_SCHEMA;
		}
	}

	/**
	 * JSON Schema Builder for optimized schema generation
	 */
	private static class JsonSchemaBuilder {

		/**
		 * Build JSON Schema from parameters array
		 */
		public String buildSchema(JsonNode parametersArray) {
			StringBuilder schema = new StringBuilder();
			List<String> requiredParams = new ArrayList<>();

			// Build properties section
			schema.append(SCHEMA_HEADER);

			// Add hardcoded sessionId parameter, which is required
			appendProperty(schema, "planId", "Plan execution ID", "string", true);
			requiredParams.add("planId");

			// Process other parameters
			for (int i = 0; i < parametersArray.size(); i++) {
				JsonNode paramNode = parametersArray.get(i);

				if (paramNode.isObject()) {
					String name = JsonUtils.getStringValue(paramNode, "name");
					String description = JsonUtils.getStringValue(paramNode, "description");
					String type = JsonUtils.getStringValue(paramNode, "type");

					if (name != null && !name.trim().isEmpty()) {
						appendProperty(schema, name, description, type, i < parametersArray.size() - 1);
						requiredParams.add(name);
					}
				}
			}

			schema.append("				},\n");

			// Add required field using optimized template
			appendRequiredField(schema, requiredParams);

			schema.append("			}\n");
			schema.append("		}");

			logger.info("Successfully generated tool Schema, parameter count: {}", requiredParams.size());
			return schema.toString();
		}

		/**
		 * Add property to schema using template
		 */
		private void appendProperty(StringBuilder schema, String name, String description, String type,
				boolean hasNext) {
			String escapedName = JsonUtils.escapeJsonString(name);
			String escapedDescription = JsonUtils.escapeJsonString(description);
			String convertedType = JsonUtils.convertType(type);

			schema.append(String.format(PROPERTY_TEMPLATE, escapedName, convertedType, escapedDescription));

			if (hasNext) {
				schema.append(",");
			}
			schema.append("\n");
		}

		/**
		 * Add required field using optimized template
		 */
		private void appendRequiredField(StringBuilder schema, List<String> requiredParams) {
			if (!requiredParams.isEmpty()) {
				String requiredString = requiredParams.stream()
					.map(param -> "\"" + JsonUtils.escapeJsonString(param) + "\"")
					.reduce((a, b) -> a + ",\n					" + b)
					.orElse("");

				schema.append(String.format(REQUIRED_ARRAY_TEMPLATE, requiredString));
			}
			else {
				schema.append("				\"required\": []");
			}
		}

	}

	/**
	 * Utility class for JSON operations
	 */
	private static class JsonUtils {

		/**
		 * Parse JSON with unified error handling
		 */
		public static JsonNode parseJson(ObjectMapper mapper, String json, String context) {
			try {
				return mapper.readTree(json);
			}
			catch (Exception e) {
				logger.error("Failed to parse JSON for {}: {}", context, e.getMessage());
				throw new IllegalArgumentException("Invalid JSON format for " + context, e);
			}
		}

		/**
		 * Safely get string value from JSON node
		 */
		public static String getStringValue(JsonNode node, String fieldName) {
			JsonNode fieldNode = node.get(fieldName);
			return fieldNode != null && fieldNode.isTextual() ? fieldNode.asText() : null;
		}

		/**
		 * Convert parameter type to JSON Schema type
		 */
		public static String convertType(String type) {
			if (type == null || type.trim().isEmpty()) {
				return "string";
			}

			String lowerType = type.toLowerCase().trim();
			return switch (lowerType) {
				case "int", "integer", "number" -> "number";
				case "boolean", "bool" -> "boolean";
				case "array", "list" -> "array";
				case "object", "map" -> "object";
				default -> "string";
			};
		}

		/**
		 * Escape JSON string with optimized performance and UTF-8 support
		 */
		public static String escapeJsonString(String input) {
			if (input == null) {
				return "";
			}

			// Use StringBuilder for better performance with large strings
			StringBuilder result = new StringBuilder(input.length() * 2);
			for (int i = 0; i < input.length(); i++) {
				char c = input.charAt(i);
				switch (c) {
					case '\\' -> result.append("\\\\");
					case '"' -> result.append("\\\"");
					case '\n' -> result.append("\\n");
					case '\r' -> result.append("\\r");
					case '\t' -> result.append("\\t");
					case '\b' -> result.append("\\b");
					case '\f' -> result.append("\\f");
					default -> {
						// Handle Unicode characters (including Chinese characters)
						if (c < 32 || c > 126) {
							// Escape non-ASCII characters as Unicode escape sequences
							result.append(String.format("\\u%04x", (int) c));
						}
						else {
							result.append(c);
						}
					}
				}
			}
			return result.toString();
		}

	}

}
