package com.alibaba.cloud.ai.example.manus.coordinator.tool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.alibaba.cloud.ai.example.manus.coordinator.vo.CoordinatorConfigVO;
import com.alibaba.cloud.ai.example.manus.coordinator.vo.CoordinatorParameterVO;

/**
 * CoordinatorConfig Parser
 */
@Component
public class CoordinatorConfigParser {

	private static final Logger logger = LoggerFactory.getLogger(CoordinatorConfigParser.class);

	// Pre-compiled regex pattern for better performance
	private static final Pattern PARAMETER_PATTERN = Pattern.compile("\\{([^}]+)\\}");

	// Default JSON Schema template
	private static final String DEFAULT_SCHEMA = """
			{
				"$schema": "http://json-schema.org/draft-07/schema#",
				"type": "object",
				"properties": {},
				"required": []
			}
			""";

	private final ObjectMapper objectMapper;

	public CoordinatorConfigParser() {
		this.objectMapper = new ObjectMapper();
	}

	/**
	 * Convert Plan JSON string to CoordinatorConfigVO
	 */
	public CoordinatorConfigVO parser(String planJson) {
		logger.info("Starting to convert Plan JSON: {}", planJson != null ? "not null" : "null");

		if (planJson == null || planJson.trim().isEmpty()) {
			throw new IllegalArgumentException("Plan JSON cannot be empty");
		}

		try {
			// Parse JSON
			JsonNode rootNode = objectMapper.readTree(planJson);

			// Validate required fields
			validatePlanJson(rootNode);

			CoordinatorConfigVO config = new CoordinatorConfigVO();
			config.setId(getStringValue(rootNode, "planId"));
			config.setName(getStringValue(rootNode, "title"));
			config.setDescription(getStringValue(rootNode, "userRequest"));
			config.setEndpoint(getStringValue(rootNode, "endpoint"));

			// Parse steps
			config.setParameters(parseStepsToParameters(rootNode));

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
		if (getStringValue(rootNode, "planId") == null) {
			throw new IllegalArgumentException("Plan JSON missing required field: planId");
		}
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
			// Handle new format: steps is object array, each object contains stepRequirement field
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
	 * Parse parameter list
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
	 * Extract parameter names from steps
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
	 * Create parameter object
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
	 * Safely get string value
	 */
	private static String getStringValue(JsonNode node, String fieldName) {
		JsonNode fieldNode = node.get(fieldName);
		return fieldNode != null && fieldNode.isTextual() ? fieldNode.asText() : null;
	}

	/**
	 * Convert JSON string to tool Schema
	 * @param json JSON string, format like: [{"name":"name","description":"Parameter: name","type":"string"}]
	 * @return Converted JSON Schema string
	 */
	public String generateToolSchema(String json) {
		if (json == null || json.trim().isEmpty()) {
			logger.warn("JSON string is empty, returning default Schema");
			return DEFAULT_SCHEMA;
		}

		try {
			// Parse JSON array
			JsonNode parametersArray = objectMapper.readTree(json);

			if (!parametersArray.isArray()) {
				logger.warn("JSON is not in array format, returning default Schema");
				return DEFAULT_SCHEMA;
			}

			return buildJsonSchema(parametersArray);

		}
		catch (Exception e) {
			logger.error("Exception occurred while generating tool Schema: {}", e.getMessage(), e);
			return DEFAULT_SCHEMA;
		}
	}

	/**
	 * Build JSON Schema
	 */
	private String buildJsonSchema(JsonNode parametersArray) {
		StringBuilder schema = new StringBuilder();
		List<String> requiredParams = new ArrayList<>();

		// Build properties section
		schema.append("{\n");
		schema.append("    \"$schema\": \"http://json-schema.org/draft-07/schema#\",\n");
		schema.append("    \"type\": \"object\",\n");
		schema.append("    \"properties\": {\n");

		for (int i = 0; i < parametersArray.size(); i++) {
			JsonNode paramNode = parametersArray.get(i);

			if (paramNode.isObject()) {
				String name = getStringValue(paramNode, "name");
				String description = getStringValue(paramNode, "description");
				String type = getStringValue(paramNode, "type");

				if (name != null && !name.trim().isEmpty()) {
					appendProperty(schema, name, description, type, i < parametersArray.size() - 1);
					requiredParams.add(name);
				}
			}
		}

		schema.append("    },\n");

		// Add required field
		appendRequiredField(schema, requiredParams);

		schema.append("}");

		logger.info("Successfully generated tool Schema, parameter count: {}", requiredParams.size());
		return schema.toString();
	}

	/**
	 * Add property to Schema
	 */
	private void appendProperty(StringBuilder schema, String name, String description, String type, boolean hasNext) {
		schema.append("        \"").append(escapeJsonString(name)).append("\": {\n");
		schema.append("            \"type\": \"").append(convertType(type)).append("\",\n");
		schema.append("            \"description\": \"").append(escapeJsonString(description)).append("\"\n");
		schema.append("        }");

		if (hasNext) {
			schema.append(",");
		}
		schema.append("\n");
	}

	/**
	 * Add required field
	 */
	private void appendRequiredField(StringBuilder schema, List<String> requiredParams) {
		if (!requiredParams.isEmpty()) {
			schema.append("    \"required\": [\n");
			for (int i = 0; i < requiredParams.size(); i++) {
				schema.append("        \"").append(escapeJsonString(requiredParams.get(i))).append("\"");
				if (i < requiredParams.size() - 1) {
					schema.append(",");
				}
				schema.append("\n");
			}
			schema.append("    ]\n");
		}
		else {
			schema.append("    \"required\": []\n");
		}
	}

	/**
	 * Convert parameter type to JSON Schema type
	 * @param type Original type
	 * @return JSON Schema type
	 */
	private static String convertType(String type) {
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
	 * Escape JSON string
	 * @param input Input string
	 * @return Escaped string
	 */
	private static String escapeJsonString(String input) {
		if (input == null) {
			return "";
		}
		return input.replace("\\", "\\\\")
			.replace("\"", "\\\"")
			.replace("\n", "\\n")
			.replace("\r", "\\r")
			.replace("\t", "\\t");
	}

}