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
		if (getStringValue(rootNode, "planId") == null || getStringValue(rootNode, "planId").trim().isEmpty()) {
			throw new IllegalArgumentException("PlanId不能为空");
		}

		if (getStringValue(rootNode, "title") == null || getStringValue(rootNode, "title").trim().isEmpty()) {
			throw new IllegalArgumentException("Title不能为空");
		}

		if (getStringValue(rootNode, "userRequest") == null
				|| getStringValue(rootNode, "userRequest").trim().isEmpty()) {
			throw new IllegalArgumentException("UserRequest不能为空");
		}

		logger.debug("Plan JSON验证通过");
	}

	/**
	 * 从steps中解析参数
	 * @param steps 步骤列表（字符串格式）
	 * @return 参数列表
	 */
	public List<CoordinatorParameterVO> parseParameters(List<String> steps) {
		if (steps == null || steps.isEmpty()) {
			logger.debug("Steps为空，返回空参数列表");
			return List.of();
		}

		try {
			Set<String> paramNames = extractParameterNames(steps);
			logger.debug("提取到参数名称: {}", paramNames);

			return paramNames.stream().map(this::createParameter).collect(Collectors.toList());
		}
		catch (Exception e) {
			logger.error("解析参数时发生异常", e);
			throw new IllegalArgumentException("解析参数失败: " + e.getMessage(), e);
		}
	}

	/**
	 * 提取参数名称 从步骤字符串中提取 {paramName} 格式的参数
	 */
	private Set<String> extractParameterNames(List<String> steps) {
		Set<String> paramNames = new HashSet<>();

		for (String step : steps) {
			if (step != null && !step.trim().isEmpty()) {
				Matcher matcher = PARAMETER_PATTERN.matcher(step);
				while (matcher.find()) {
					String paramName = matcher.group(1).trim();
					if (!paramName.isEmpty()) {
						paramNames.add(paramName);
						logger.debug("从步骤中提取到参数: {}", paramName);
					}
				}
			}
		}

		return paramNames;
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
	private String getStringValue(JsonNode node, String fieldName) {
		JsonNode fieldNode = node.get(fieldName);
		return fieldNode != null && fieldNode.isTextual() ? fieldNode.asText() : null;
	}

	/**
	 * 将CoordinatorToolEntity转换为CoordinatorConfigVO
	 * @param entity CoordinatorToolEntity实体
	 * @return CoordinatorConfigVO配置对象
	 */
	public static CoordinatorConfigVO coordinatorToolEntityParser(CoordinatorToolEntity entity) {
		if (entity == null) {
			return null;
		}

		CoordinatorConfigVO config = new CoordinatorConfigVO();
		config.setId(entity.getToolName());
		config.setName(entity.getToolName());
		config.setDescription(entity.getToolDescription());
		config.setEndpoint(entity.getEndpoint());

		// 解析inputSchema中的参数
		List<CoordinatorParameterVO> parameters = parseParametersFromSchema(entity.getInputSchema());
		config.setParameters(parameters);

		return config;
	}

	/**
	 * 将CoordinatorConfigVO转换为CoordinatorTool
	 * @param config CoordinatorConfigVO配置对象
	 * @return CoordinatorTool工具对象
	 */
	public static CoordinatorTool coordinatorConfigParser(CoordinatorConfigVO config) {
		if (config == null) {
			return null;
		}

		CoordinatorTool tool = new CoordinatorTool();
		tool.setToolName(config.getId());
		tool.setToolDescription(config.getDescription());
		tool.setEndpoint(config.getEndpoint() != null ? config.getEndpoint() : "coordinator");

		// 生成工具Schema
		String schema = generateToolSchema(config.getParameters());
		tool.setToolSchema(schema);

		return tool;
	}

	/**
	 * 从inputSchema中解析参数
	 * @param inputSchema 输入Schema字符串
	 * @return 参数列表
	 */
	private static List<CoordinatorParameterVO> parseParametersFromSchema(String inputSchema) {
		List<CoordinatorParameterVO> parameters = new ArrayList<>();
		
		if (inputSchema == null || inputSchema.trim().isEmpty()) {
			return parameters;
		}

		try {
			ObjectMapper objectMapper = new ObjectMapper();
			JsonNode schemaNode = objectMapper.readTree(inputSchema);
			
			JsonNode propertiesNode = schemaNode.get("properties");
			if (propertiesNode != null && propertiesNode.isObject()) {
				propertiesNode.fieldNames().forEachRemaining(paramName -> {
					JsonNode paramNode = propertiesNode.get(paramName);
					if (paramNode != null && paramNode.isObject()) {
						CoordinatorParameterVO parameter = new CoordinatorParameterVO();
						parameter.setName(paramName);
						parameter.setType(getParameterType(paramNode));
						parameter.setDescription(getParameterDescription(paramNode));
						parameter.setRequired(isParameterRequired(schemaNode, paramName));
						parameters.add(parameter);
					}
				});
			}
		} catch (Exception e) {
			// 如果解析失败，返回空列表
			return new ArrayList<>();
		}

		return parameters;
	}

	/**
	 * 生成工具Schema
	 * @param parameters 参数列表
	 * @return Schema字符串
	 */
	private static String generateToolSchema(List<CoordinatorParameterVO> parameters) {
		StringBuilder schema = new StringBuilder();
		schema.append("{\n");
		schema.append("    \"$schema\": \"http://json-schema.org/draft-07/schema#\",\n");
		schema.append("    \"type\": \"object\",\n");
		schema.append("    \"properties\": {\n");

		if (parameters != null && !parameters.isEmpty()) {
			for (int i = 0; i < parameters.size(); i++) {
				CoordinatorParameterVO param = parameters.get(i);
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

		schema.append("    },\n");

		// 添加required字段
		if (parameters != null && !parameters.isEmpty()) {
			List<String> requiredParams = parameters.stream()
				.filter(param -> param != null && param.isRequired() && param.getName() != null
						&& !param.getName().trim().isEmpty())
				.map(CoordinatorParameterVO::getName)
				.toList();

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
		}

		schema.append("}");
		return schema.toString();
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
			} else {
				schema.append("    \"required\": []\n");
			}

			schema.append("}");
			
			logger.info("成功生成工具Schema，参数数量: {}", requiredParams.size());
			return schema.toString();

		} catch (Exception e) {
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