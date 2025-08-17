package com.alibaba.cloud.ai.example.manus.planning.service;

import com.alibaba.cloud.ai.example.manus.planning.model.vo.ParameterValidationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 计划参数映射服务实现类 提供处理计划模板中参数占位符的具体实现
 */
@Service
public class PlanParameterMappingService implements IPlanParameterMappingService {

	private static final Logger logger = LoggerFactory.getLogger(PlanParameterMappingService.class);

	// 参数占位符的正则表达式模式：匹配 <<参数名>> 格式
	private static final Pattern PARAMETER_PATTERN = Pattern.compile("<<(\\w+)>>");

	// 参数占位符的前缀和后缀
	private static final String PLACEHOLDER_PREFIX = "<<";

	private static final String PLACEHOLDER_SUFFIX = ">>";

	@Override
	public ParameterValidationResult validateParameters(String planJson, Map<String, Object> rawParams) {
		ParameterValidationResult result = new ParameterValidationResult();

		if (planJson == null || rawParams == null) {
			result.setValid(false);
			result.setMessage("计划模板或原始参数为空");
			return result;
		}

		List<String> missingParams = new ArrayList<>();
		List<String> foundParams = new ArrayList<>();

		// 查找所有参数占位符
		Matcher matcher = PARAMETER_PATTERN.matcher(planJson);

		while (matcher.find()) {
			String paramName = matcher.group(1);

			if (rawParams.containsKey(paramName)) {
				foundParams.add(paramName);
				logger.debug("参数验证通过: {}", paramName);
			}
			else {
				missingParams.add(paramName);
				logger.warn("参数验证失败: {} 未在原始参数中找到", paramName);
			}
		}

		result.setFoundParameters(foundParams);
		result.setMissingParameters(missingParams);
		result.setValid(missingParams.isEmpty());

		if (missingParams.isEmpty()) {
			result.setMessage("所有参数验证通过，共找到 " + foundParams.size() + " 个参数");
		}
		else {
			result.setMessage("缺少以下参数: " + String.join(", ", missingParams) + "，共找到 " + foundParams.size() + " 个参数");
		}

		logger.info("参数验证结果: {}", result.getMessage());
		return result;
	}

	@Override
	public List<String> extractParameterPlaceholders(String planJson) {
		List<String> placeholders = new ArrayList<>();

		if (planJson == null) {
			return placeholders;
		}

		Matcher matcher = PARAMETER_PATTERN.matcher(planJson);
		while (matcher.find()) {
			placeholders.add(matcher.group(1)); // 只返回参数名，不包含 <<>>
		}

		logger.debug("提取到 {} 个参数占位符: {}", placeholders.size(), placeholders);
		return placeholders;
	}

	/**
	 * 获取参数占位符的正则表达式模式 用于外部测试或调试
	 */
	public static Pattern getParameterPattern() {
		return PARAMETER_PATTERN;
	}

	/**
	 * 获取参数占位符的前缀和后缀
	 */
	public static String getPlaceholderPrefix() {
		return PLACEHOLDER_PREFIX;
	}

	public static String getPlaceholderSuffix() {
		return PLACEHOLDER_SUFFIX;
	}

	@Override
	public String replaceParametersInJson(String planJson, Map<String, Object> rawParams) {
		if (planJson == null || rawParams == null) {
			logger.warn("Plan template or raw parameters are null, skipping parameter replacement");
			return planJson;
		}

		if (rawParams.isEmpty()) {
			logger.debug("Raw parameters are empty, no parameter replacement needed");
			return planJson;
		}

		String result = planJson;
		int replacementCount = 0;

		// Find all parameter placeholders
		Matcher matcher = PARAMETER_PATTERN.matcher(planJson);

		while (matcher.find()) {
			String placeholder = matcher.group(0); // Complete placeholder, e.g.,
													// <<args1>>
			String paramName = matcher.group(1); // Parameter name, e.g., args1

			// Get value from raw parameters
			Object paramValue = rawParams.get(paramName);

			if (paramValue != null) {
				// Replace placeholder
				String stringValue = paramValue.toString();
				result = result.replace(placeholder, stringValue);
				replacementCount++;

				logger.debug("Parameter replacement successful: {} -> {}", placeholder, stringValue);
			}
			else {
				logger.warn("Parameter {} not found in raw parameters, keeping placeholder: {}", paramName,
						placeholder);
			}
		}

		if (replacementCount > 0) {
			logger.info("Parameter replacement completed, replaced {} parameter placeholders", replacementCount);
		}
		else {
			logger.debug("No parameter placeholders found for replacement");
		}

		return result;
	}

	/**
	 * 检查参数名是否有效 参数名只能包含字母、数字和下划线
	 */
	public static boolean isValidParameterName(String paramName) {
		if (paramName == null || paramName.trim().isEmpty()) {
			return false;
		}
		return paramName.matches("^[a-zA-Z_][a-zA-Z0-9_]*$");
	}

	/**
	 * 安全地构建参数占位符
	 */
	public static String buildPlaceholder(String paramName) {
		if (!isValidParameterName(paramName)) {
			throw new IllegalArgumentException("无效的参数名: " + paramName);
		}
		return PLACEHOLDER_PREFIX + paramName + PLACEHOLDER_SUFFIX;
	}

}
