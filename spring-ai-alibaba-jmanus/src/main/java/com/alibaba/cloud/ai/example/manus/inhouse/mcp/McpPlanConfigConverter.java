package com.alibaba.cloud.ai.example.manus.inhouse.mcp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.alibaba.cloud.ai.example.manus.inhouse.mcp.vo.McpPlanConfigVO;
import com.alibaba.cloud.ai.example.manus.inhouse.mcp.vo.McpPlanParameterVO;

/**
 * McpPlanConfig转换器
 */
public class McpPlanConfigConverter {

	private static final Logger logger = LoggerFactory.getLogger(McpPlanConfigConverter.class);

	// 预编译正则表达式，提高性能
	private static final Pattern PARAMETER_PATTERN = Pattern.compile("\\{([^}]+)\\}");

	private final ObjectMapper objectMapper;

	public McpPlanConfigConverter() {
		this.objectMapper = new ObjectMapper();
	}

	/**
	 * 将Plan JSON字符串转换为McpPlanConfigVO
	 */
	public McpPlanConfigVO convert(String planJson) {
		logger.info("开始转换Plan JSON: {}", planJson != null ? "非空" : "null");

		if (planJson == null || planJson.trim().isEmpty()) {
			throw new McpPlanConversionException("Plan JSON不能为空");
		}

		try {
			// 解析JSON
			JsonNode rootNode = objectMapper.readTree(planJson);

			// 验证必要字段
			validatePlanJson(rootNode);

			McpPlanConfigVO config = new McpPlanConfigVO();
			config.setId(getStringValue(rootNode, "planId"));
			config.setName(getStringValue(rootNode, "title"));
			config.setDescription(getStringValue(rootNode, "userRequest"));

			// 解析steps
			JsonNode stepsNode = rootNode.get("steps");
			if (stepsNode != null && stepsNode.isArray()) {
				List<String> steps = new ArrayList<>();
				for (JsonNode stepNode : stepsNode) {
					if (stepNode.isTextual()) {
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
		catch (McpPlanConversionException e) {
			logger.error("转换失败", e);
			throw e;
		}
		catch (Exception e) {
			logger.error("转换过程中发生未知异常", e);
			throw new McpPlanConversionException("转换失败: " + e.getMessage(), e);
		}
	}

	/**
	 * 验证Plan JSON的必要字段
	 */
	private void validatePlanJson(JsonNode rootNode) {
		if (getStringValue(rootNode, "planId") == null || getStringValue(rootNode, "planId").trim().isEmpty()) {
			throw new McpPlanConversionException("PlanId不能为空");
		}

		if (getStringValue(rootNode, "title") == null || getStringValue(rootNode, "title").trim().isEmpty()) {
			throw new McpPlanConversionException("Title不能为空");
		}

		if (getStringValue(rootNode, "userRequest") == null
				|| getStringValue(rootNode, "userRequest").trim().isEmpty()) {
			throw new McpPlanConversionException("UserRequest不能为空");
		}

		logger.debug("Plan JSON验证通过");
	}

	/**
	 * 从steps中解析参数
	 * @param steps 步骤列表（字符串格式）
	 * @return 参数列表
	 */
	public List<McpPlanParameterVO> parseParameters(List<String> steps) {
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
			throw new McpPlanConversionException("解析参数失败: " + e.getMessage(), e);
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
	private McpPlanParameterVO createParameter(String paramName) {
		McpPlanParameterVO parameter = new McpPlanParameterVO();
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

}
