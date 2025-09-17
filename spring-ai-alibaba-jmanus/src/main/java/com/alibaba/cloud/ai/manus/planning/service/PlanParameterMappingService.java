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

package com.alibaba.cloud.ai.manus.planning.service;

import com.alibaba.cloud.ai.manus.planning.exception.ParameterValidationException;
import com.alibaba.cloud.ai.manus.planning.model.vo.ParameterValidationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Plan parameter mapping service implementation class providing specific implementation
 * for handling parameter placeholders in plan templates
 */
@Service
public class PlanParameterMappingService implements IPlanParameterMappingService {

	private static final Logger logger = LoggerFactory.getLogger(PlanParameterMappingService.class);

	// Parameter placeholder regex pattern: matches <<parameter_name>> format, supports
	// all Unicode characters
	private static final Pattern PARAMETER_PATTERN = Pattern.compile("<<([^<>]+)>>");

	// Parameter placeholder prefix and suffix
	private static final String PLACEHOLDER_PREFIX = "<<";

	private static final String PLACEHOLDER_SUFFIX = ">>";

	@Override
	public ParameterValidationResult validateParameters(String planJson, Map<String, Object> rawParams) {
		ParameterValidationResult result = new ParameterValidationResult();

		if (planJson == null || rawParams == null) {
			result.setValid(false);
			result.setMessage("Plan template or raw parameters are null");
			return result;
		}

		List<String> missingParams = new ArrayList<>();
		List<String> foundParams = new ArrayList<>();

		// Find all parameter placeholders
		Matcher matcher = PARAMETER_PATTERN.matcher(planJson);

		while (matcher.find()) {
			String paramName = matcher.group(1);

			if (rawParams.containsKey(paramName)) {
				foundParams.add(paramName);
				logger.debug("Parameter validation passed: {}", paramName);
			}
			else {
				missingParams.add(paramName);
				logger.warn("Parameter validation failed: {} not found in raw parameters", paramName);
			}
		}

		result.setFoundParameters(foundParams);
		result.setMissingParameters(missingParams);
		result.setValid(missingParams.isEmpty());

		if (missingParams.isEmpty()) {
			result.setMessage("All parameter validation passed, found " + foundParams.size() + " parameters");
		}
		else {
			result.setMessage("Missing parameters: " + String.join(", ", missingParams) + ", found "
					+ foundParams.size() + " parameters");
		}

		logger.info("Parameter validation result: {}", result.getMessage());

		// Throw exception if parameters are missing or incompatible
		if (!missingParams.isEmpty()) {
			String errorMessage = buildDetailedErrorMessage(missingParams, foundParams, planJson);
			throw new ParameterValidationException(errorMessage);
		}

		return result;
	}

	/**
	 * Validate parameter completeness before parameter replacement. Throws detailed
	 * exception information if validation fails
	 * @param planJson plan template JSON
	 * @param rawParams raw parameters
	 * @throws ParameterValidationException thrown when parameter validation fails
	 */
	public void validateParametersBeforeReplacement(String planJson, Map<String, Object> rawParams) {
		ParameterValidationResult result = validateParameters(planJson, rawParams);
		if (!result.isValid()) {
			// This will throw an exception since validateParameters now throws on failure
			// But we keep this method for explicit validation before replacement
			throw new ParameterValidationException("Parameter validation failed, cannot perform parameter replacement");
		}
	}

	/**
	 * Safely replace parameters, throws exception if validation fails
	 * @param planJson plan template JSON
	 * @param rawParams raw parameters
	 * @return replaced plan template
	 * @throws ParameterValidationException thrown when parameter validation fails
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
			placeholders.add(matcher.group(1)); // Only return parameter name, not
												// including <<>>
		}

		logger.debug("Extracted {} parameter placeholders: {}", placeholders.size(), placeholders);
		return placeholders;
	}

	/**
	 * Get parameter placeholder regex pattern for external testing or debugging
	 */
	public static Pattern getParameterPattern() {
		return PARAMETER_PATTERN;
	}

	/**
	 * Get parameter placeholder prefix and suffix
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
	 * Check if parameter name is valid. Parameter names can only contain letters, numbers
	 * and underscores
	 */
	public static boolean isValidParameterName(String paramName) {
		if (paramName == null || paramName.trim().isEmpty()) {
			return false;
		}
		return paramName.matches("^[a-zA-Z_][a-zA-Z0-9_]*$");
	}

	/**
	 * Safely build parameter placeholder
	 */
	public static String buildPlaceholder(String paramName) {
		if (!isValidParameterName(paramName)) {
			throw new IllegalArgumentException("Invalid parameter name: " + paramName);
		}
		return PLACEHOLDER_PREFIX + paramName + PLACEHOLDER_SUFFIX;
	}

	/**
	 * Get parameter requirements information for plan template to help users understand
	 * what parameters need to be provided
	 * @param planJson plan template JSON
	 * @return parameter requirements information
	 */
	public String getParameterRequirements(String planJson) {
		if (planJson == null) {
			return "Plan template is null, cannot get parameter requirements";
		}

		List<String> placeholders = extractParameterPlaceholders(planJson);
		if (placeholders.isEmpty()) {
			return "✅ This plan template does not require any parameters";
		}

		StringBuilder requirements = new StringBuilder();
		requirements.append("📋 This plan template requires the following parameters:\n\n");

		for (int i = 0; i < placeholders.size(); i++) {
			String param = placeholders.get(i);
			requirements.append(String.format("%d. <<%s>>\n", i + 1, param));
		}

		requirements.append("\n💡 Parameter format description:\n");
		requirements.append("   • Parameter names can only contain letters, numbers and underscores\n");
		requirements.append("   • Parameter names cannot start with numbers\n");
		requirements.append("   • Parameter names are case-sensitive\n");
		requirements.append("   • All parameters are required\n");

		return requirements.toString();
	}

	private String buildDetailedErrorMessage(List<String> missingParams, List<String> foundParams, String planJson) {
		StringBuilder errorMessage = new StringBuilder();
		errorMessage.append(
				"❌ Parameter validation failed! The plan template contains the following parameter placeholders, but the raw parameters did not provide or provided mismatched values:\n\n");

		// List missing parameters with examples
		errorMessage.append("🔍 Missing parameters:\n");
		for (String missingParam : missingParams) {
			errorMessage.append("   • <<").append(missingParam).append(">>\n");
		}

		// List found parameters
		if (!foundParams.isEmpty()) {
			errorMessage.append("\n✅ Found parameters:\n");
			for (String foundParam : foundParams) {
				errorMessage.append("   • <<").append(foundParam).append(">>\n");
			}
		}

		errorMessage.append("\n💡 Solutions:\n");
		errorMessage.append("   1. Check if parameter name spelling is correct\n");
		errorMessage.append("   2. Ensure all required parameters are provided\n");
		errorMessage.append("   3. Parameter names are case-sensitive\n");
		errorMessage.append(
				"   4. Parameter names can only contain letters, numbers and underscores, and cannot start with numbers\n\n");

		errorMessage.append("📋 Plan template content:\n");
		errorMessage.append(planJson);

		return errorMessage.toString();
	}

}
