package com.alibaba.cloud.ai.example.manus.planning.service;

import com.alibaba.cloud.ai.example.manus.planning.exception.ParameterValidationException;
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

		// Throw exception if parameters are missing or incompatible
		if (!missingParams.isEmpty()) {
			String errorMessage = buildDetailedErrorMessage(missingParams, foundParams, planJson);
			throw new ParameterValidationException(errorMessage);
		}

		return result;
	}

	/**
	 * 在参数替换之前验证参数完整性 如果验证失败，抛出详细的异常信息
	 * @param planJson 计划模板JSON
	 * @param rawParams 原始参数
	 * @throws ParameterValidationException 当参数验证失败时抛出
	 */
	public void validateParametersBeforeReplacement(String planJson, Map<String, Object> rawParams) {
		ParameterValidationResult result = validateParameters(planJson, rawParams);
		if (!result.isValid()) {
			// This will throw an exception since validateParameters now throws on failure
			// But we keep this method for explicit validation before replacement
			throw new ParameterValidationException("参数验证失败，无法进行参数替换");
		}
	}

	/**
	 * 安全地替换参数，如果验证失败则抛出异常
	 * @param planJson 计划模板JSON
	 * @param rawParams 原始参数
	 * @return 替换后的计划模板
	 * @throws ParameterValidationException 当参数验证失败时抛出
	 */
	public String replaceParametersSafely(String planJson, Map<String, Object> rawParams) {
		// First validate parameters
		validateParametersBeforeReplacement(planJson, rawParams);
		// Then perform replacement (this should not throw since validation passed)
		return replaceParametersInJson(planJson, rawParams);
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
		List<String> missingParams = new ArrayList<>();

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
				missingParams.add(paramName);
				logger.warn("Parameter {} not found in raw parameters, keeping placeholder: {}", paramName,
						placeholder);
			}
		}

		// Throw exception if any parameters are missing
		if (!missingParams.isEmpty()) {
			String errorMessage = buildDetailedErrorMessage(missingParams, new ArrayList<>(), planJson);
			throw new ParameterValidationException(errorMessage);
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

	/**
	 * 获取计划模板的参数要求信息 帮助用户了解需要提供哪些参数
	 * @param planJson 计划模板JSON
	 * @return 参数要求信息
	 */
	public String getParameterRequirements(String planJson) {
		if (planJson == null) {
			return "计划模板为空，无法获取参数要求";
		}

		List<String> placeholders = extractParameterPlaceholders(planJson);
		if (placeholders.isEmpty()) {
			return "✅ 此计划模板不需要任何参数";
		}

		StringBuilder requirements = new StringBuilder();
		requirements.append("📋 此计划模板需要以下参数：\n\n");

		for (int i = 0; i < placeholders.size(); i++) {
			String param = placeholders.get(i);
			requirements.append(String.format("%d. <<%s>>\n", i + 1, param));
		}

		requirements.append("\n💡 参数格式说明：\n");
		requirements.append("   • 参数名只能包含字母、数字和下划线\n");
		requirements.append("   • 参数名不能以数字开头\n");
		requirements.append("   • 参数名区分大小写\n");
		requirements.append("   • 所有参数都是必需的\n");

		return requirements.toString();
	}

	private String buildDetailedErrorMessage(List<String> missingParams, List<String> foundParams, String planJson) {
		StringBuilder errorMessage = new StringBuilder();
		errorMessage.append("❌ 参数验证失败！计划模板中存在以下参数占位符，但原始参数中未提供或提供不匹配的值：\n\n");

		// List missing parameters with examples
		errorMessage.append("🔍 缺失的参数：\n");
		for (String missingParam : missingParams) {
			errorMessage.append("   • <<").append(missingParam).append(">>\n");
		}

		// List found parameters
		if (!foundParams.isEmpty()) {
			errorMessage.append("\n✅ 已找到的参数：\n");
			for (String foundParam : foundParams) {
				errorMessage.append("   • <<").append(foundParam).append(">>\n");
			}
		}

		errorMessage.append("\n💡 解决方案：\n");
		errorMessage.append("   1. 检查参数名称拼写是否正确\n");
		errorMessage.append("   2. 确保所有必需的参数都已提供\n");
		errorMessage.append("   3. 参数名称区分大小写\n");
		errorMessage.append("   4. 参数名只能包含字母、数字和下划线，且不能以数字开头\n\n");

		errorMessage.append("📋 计划模板内容：\n");
		errorMessage.append(planJson);

		return errorMessage.toString();
	}

}
