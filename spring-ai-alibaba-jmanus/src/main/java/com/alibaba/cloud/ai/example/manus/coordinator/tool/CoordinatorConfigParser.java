package com.alibaba.cloud.ai.example.manus.coordinator.tool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.alibaba.cloud.ai.example.manus.coordinator.vo.CoordinatorConfigVO;
import com.alibaba.cloud.ai.example.manus.coordinator.vo.CoordinatorParameterVO;
import com.alibaba.cloud.ai.example.manus.coordinator.entity.CoordinatorToolEntity;

/**
 * CoordinatorConfig解析器
 */
@Component
public class CoordinatorConfigParser {

	private static final Logger logger = LoggerFactory.getLogger(CoordinatorConfigParser.class);

	// 预编译正则表达式，提高性能
	private static final Pattern PARAMETER_PATTERN = Pattern.compile("\\{([^}]+)\\}");

	private final ObjectMapper objectMapper;

	public CoordinatorConfigParser() {
		this.objectMapper = new ObjectMapper();
	}

	/**
	 * 将Plan JSON字符串转换为CoordinatorConfigVO
	 */
	public CoordinatorConfigVO parser(String planJson) {
		logger.info("开始转换Plan JSON: {}", planJson != null ? "非空" : "null");

		if (planJson == null || planJson.trim().isEmpty()) {
			throw new IllegalArgumentException("Plan JSON不能为空");
		}

		try {
			// 解析JSON
			JsonNode rootNode = objectMapper.readTree(planJson);

			// 验证必要字段
			validatePlanJson(rootNode);

			CoordinatorConfigVO config = new CoordinatorConfigVO();
			config.setId(getStringValue(rootNode, "planId"));
			config.setName(getStringValue(rootNode, "title"));
			config.setDescription(getStringValue(rootNode, "userRequest"));
			config.setEndpoint(getStringValue(rootNode, "endpoint"));

			// 解析steps
			JsonNode stepsNode = rootNode.get("steps");
			if (stepsNode != null && stepsNode.isArray()) {
				List<String> steps = new ArrayList<>();
				for (JsonNode stepNode : stepsNode) {
					// 处理新的格式：steps是对象数组，每个对象包含stepRequirement字段
					if (stepNode.isObject()) {
						JsonNode stepRequirementNode = stepNode.get("stepRequirement");
						if (stepRequirementNode != null && stepRequirementNode.isTextual()) {
							steps.add(stepRequirementNode.asText());
						}
					}
					// 兼容旧格式：steps是字符串数组
					else if (stepNode.isTextual()) {
						steps.add(stepNode.asText());
					}
				}
				config.setParameters(parseParameters(steps));
			}
			else {
				config.setParameters(List.of());
			}

			logger.info("转换完成，生成配置: {}", config);
			return config;

		}
		catch (IllegalArgumentException e) {
			logger.error("转换失败", e);
			throw e;
		}
		catch (Exception e) {
			logger.error("转换过程中发生未知异常", e);
			throw new IllegalArgumentException("转换失败: " + e.getMessage(), e);
		}
	}

	/**
	 * 验证Plan JSON的必要字段
	 */
	private void validatePlanJson(JsonNode rootNode) {
		if (rootNode == null) {
			throw new IllegalArgumentException("Plan JSON解析失败，根节点为空");
		}

		if (!rootNode.isObject()) {
			throw new IllegalArgumentException("Plan JSON必须是对象格式");
		}

		// 检查必要字段
		if (getStringValue(rootNode, "planId") == null) {
			throw new IllegalArgumentException("Plan JSON缺少必要字段: planId");
		}
	}

	/**
	 * 解析参数列表
	 */
	public List<CoordinatorParameterVO> parseParameters(List<String> steps) {
		if (steps == null || steps.isEmpty()) {
			return new ArrayList<>();
		}

		// 提取所有参数名
		Set<String> parameterNames = extractParameterNames(steps);

		// 转换为参数对象
		List<CoordinatorParameterVO> parameters = new ArrayList<>();
		for (String paramName : parameterNames) {
			parameters.add(createParameter(paramName));
		}

		return parameters;
	}

	/**
	 * 从步骤中提取参数名
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
	 * 创建参数对象
	 */
	private CoordinatorParameterVO createParameter(String paramName) {
		CoordinatorParameterVO parameter = new CoordinatorParameterVO();
		parameter.setName(paramName);
		parameter.setType("String");
		parameter.setDescription("参数: " + paramName);
		parameter.setRequired(true);

		logger.debug("创建参数对象: {}", parameter);
		return parameter;
	}

	/**
	 * 安全获取字符串值
	 */
	private static String getStringValue(JsonNode node, String fieldName) {
		JsonNode fieldNode = node.get(fieldName);
		return fieldNode != null && fieldNode.isTextual() ? fieldNode.asText() : null;
	}

	/**
	 * 从inputSchema中解析参数列表
	 * @param inputSchema JSON格式的inputSchema字符串
	 * @return 参数列表
	 */
	private static List<CoordinatorParameterVO> parseParametersFromSchema(String inputSchema) {
		List<CoordinatorParameterVO> parameters = new ArrayList<>();

		if (inputSchema == null || inputSchema.trim().isEmpty()) {
			return parameters;
		}

		try {
			ObjectMapper mapper = new ObjectMapper();
			JsonNode schemaNode = mapper.readTree(inputSchema);

			// 检查是否是数组格式
			if (schemaNode.isArray()) {
				for (JsonNode paramNode : schemaNode) {
					if (paramNode.isObject()) {
						CoordinatorParameterVO parameter = new CoordinatorParameterVO();
						parameter.setName(getStringValue(paramNode, "name"));
						parameter.setType(getParameterType(paramNode));
						parameter.setDescription(getParameterDescription(paramNode));
						parameter.setRequired(isParameterRequired(schemaNode, parameter.getName()));
						parameters.add(parameter);
					}
				}
			}
			// 检查是否是对象格式（JSON Schema格式）
			else if (schemaNode.isObject()) {
				JsonNode propertiesNode = schemaNode.get("properties");
				if (propertiesNode != null && propertiesNode.isObject()) {
					Iterator<String> fieldNames = propertiesNode.fieldNames();
					while (fieldNames.hasNext()) {
						String fieldName = fieldNames.next();
						JsonNode fieldNode = propertiesNode.get(fieldName);
						if (fieldNode.isObject()) {
							CoordinatorParameterVO parameter = new CoordinatorParameterVO();
							parameter.setName(fieldName);
							parameter.setType(getParameterType(fieldNode));
							parameter.setDescription(getParameterDescription(fieldNode));
							parameter.setRequired(isParameterRequired(schemaNode, fieldName));
							parameters.add(parameter);
						}
					}
				}
			}

		}
		catch (Exception e) {
			logger.warn("解析inputSchema时发生异常: {}", e.getMessage());
		}

		return parameters;
	}

	/**
	 * 生成工具Schema
	 * @param parameters 参数列表
	 * @return JSON Schema字符串
	 */
	private static String generateToolSchema(List<CoordinatorParameterVO> parameters) {
		try {
			StringBuilder schema = new StringBuilder();
			schema.append("{\n");
			schema.append("    \"$schema\": \"http://json-schema.org/draft-07/schema#\",\n");
			schema.append("    \"type\": \"object\",\n");
			schema.append("    \"properties\": {\n");

			if (parameters != null && !parameters.isEmpty()) {
				for (int i = 0; i < parameters.size(); i++) {
					CoordinatorParameterVO param = parameters.get(i);

					if (param != null && param.getName() != null && !param.getName().trim().isEmpty()) {
						schema.append("        \"").append(escapeJsonString(param.getName())).append("\": {\n");
						schema.append("            \"type\": \"").append(convertType(param.getType())).append("\",\n");
						schema.append("            \"description\": \"")
							.append(escapeJsonString(param.getDescription()))
							.append("\"\n");
						schema.append("        }");

						if (i < parameters.size() - 1) {
							schema.append(",");
						}
						schema.append("\n");
					}
				}
			}

			schema.append("    },\n");

			// 添加required字段
			if (parameters != null && !parameters.isEmpty()) {
				List<String> requiredParams = parameters.stream()
					.filter(param -> param != null && param.isRequired() && param.getName() != null
							&& !param.getName().trim().isEmpty())
					.map(CoordinatorParameterVO::getName)
					.toList();

				if (!requiredParams.isEmpty()) {
					schema.append("    \"required\": [");
					for (int i = 0; i < requiredParams.size(); i++) {
						schema.append("\"").append(escapeJsonString(requiredParams.get(i))).append("\"");
						if (i < requiredParams.size() - 1) {
							schema.append(", ");
						}
					}
					schema.append("]\n");
				}
				else {
					schema.append("    \"required\": []\n");
				}
			}
			else {
				schema.append("    \"required\": []\n");
			}

			schema.append("}");

			return schema.toString();
		}
		catch (Exception e) {
			logger.error("生成JSON Schema时发生错误: {}", e.getMessage(), e);
			// 返回默认的简单Schema
			return """
					{
						"$schema": "http://json-schema.org/draft-07/schema#",
						"type": "object",
						"properties": {},
						"required": []
					}
					""";
		}
	}

	/**
	 * 将JSON字符串转换为工具Schema
	 * @param json JSON字符串，格式如：[{"name":"name","description":"参数: name","type":"string"}]
	 * @return 转换后的JSON Schema字符串
	 */
	public String generateToolSchema(String json) {
		if (json == null || json.trim().isEmpty()) {
			logger.warn("JSON字符串为空，返回默认Schema");
			return """
					{
						"$schema": "http://json-schema.org/draft-07/schema#",
						"type": "object",
						"properties": {},
						"required": []
					}
					""";
		}

		try {
			// 解析JSON数组
			JsonNode parametersArray = objectMapper.readTree(json);

			if (!parametersArray.isArray()) {
				logger.warn("JSON不是数组格式，返回默认Schema");
				return """
						{
							"$schema": "http://json-schema.org/draft-07/schema#",
							"type": "object",
							"properties": {},
							"required": []
						}
						""";
			}

			StringBuilder schema = new StringBuilder();
			schema.append("{\n");
			schema.append("    \"$schema\": \"http://json-schema.org/draft-07/schema#\",\n");
			schema.append("    \"type\": \"object\",\n");
			schema.append("    \"properties\": {\n");

			List<String> requiredParams = new ArrayList<>();

			for (int i = 0; i < parametersArray.size(); i++) {
				JsonNode paramNode = parametersArray.get(i);

				if (paramNode.isObject()) {
					String name = getStringValue(paramNode, "name");
					String description = getStringValue(paramNode, "description");
					String type = getStringValue(paramNode, "type");

					if (name != null && !name.trim().isEmpty()) {
						schema.append("        \"").append(escapeJsonString(name)).append("\": {\n");
						schema.append("            \"type\": \"").append(convertType(type)).append("\",\n");
						schema.append("            \"description\": \"")
							.append(escapeJsonString(description))
							.append("\"\n");
						schema.append("        }");

						if (i < parametersArray.size() - 1) {
							schema.append(",");
						}
						schema.append("\n");

						// 默认所有参数都是必需的
						requiredParams.add(name);
					}
				}
			}

			schema.append("    },\n");

			// 添加required字段
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

			schema.append("}");

			logger.info("成功生成工具Schema，参数数量: {}", requiredParams.size());
			return schema.toString();

		}
		catch (Exception e) {
			logger.error("生成工具Schema时发生异常: {}", e.getMessage(), e);
			// 返回默认的简单Schema
			return """
					{
						"$schema": "http://json-schema.org/draft-07/schema#",
						"type": "object",
						"properties": {},
						"required": []
					}
					""";
		}
	}

	/**
	 * 获取参数类型
	 * @param paramNode 参数节点
	 * @return 参数类型
	 */
	private static String getParameterType(JsonNode paramNode) {
		JsonNode typeNode = paramNode.get("type");
		return typeNode != null && typeNode.isTextual() ? typeNode.asText() : "string";
	}

	/**
	 * 获取参数描述
	 * @param paramNode 参数节点
	 * @return 参数描述
	 */
	private static String getParameterDescription(JsonNode paramNode) {
		JsonNode descNode = paramNode.get("description");
		return descNode != null && descNode.isTextual() ? descNode.asText() : "";
	}

	/**
	 * 判断参数是否必需
	 * @param schemaNode Schema根节点
	 * @param paramName 参数名称
	 * @return 是否必需
	 */
	private static boolean isParameterRequired(JsonNode schemaNode, String paramName) {
		JsonNode requiredNode = schemaNode.get("required");
		if (requiredNode != null && requiredNode.isArray()) {
			for (JsonNode requiredParam : requiredNode) {
				if (requiredParam.isTextual() && paramName.equals(requiredParam.asText())) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * 转换参数类型为JSON Schema类型
	 * @param type 原始类型
	 * @return JSON Schema类型
	 */
	private static String convertType(String type) {
		if (type == null || type.trim().isEmpty()) {
			return "string";
		}

		String lowerType = type.toLowerCase().trim();
		switch (lowerType) {
			case "int":
			case "integer":
			case "number":
				return "number";
			case "boolean":
			case "bool":
				return "boolean";
			case "array":
			case "list":
				return "array";
			case "object":
			case "map":
				return "object";
			default:
				return "string";
		}
	}

	/**
	 * 转义JSON字符串
	 * @param input 输入字符串
	 * @return 转义后的字符串
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