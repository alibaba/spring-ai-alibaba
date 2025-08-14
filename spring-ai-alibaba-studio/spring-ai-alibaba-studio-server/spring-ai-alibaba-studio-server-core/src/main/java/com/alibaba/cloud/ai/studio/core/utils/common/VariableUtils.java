/*
 * Copyright 2024-2025 the original author or authors.
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
package com.alibaba.cloud.ai.studio.core.utils.common;

import com.alibaba.cloud.ai.studio.runtime.exception.BizException;
import com.alibaba.cloud.ai.studio.runtime.enums.ErrorCode;
import com.alibaba.cloud.ai.studio.runtime.domain.file.File;
import com.alibaba.cloud.ai.studio.runtime.domain.workflow.CommonParam;
import com.alibaba.cloud.ai.studio.runtime.domain.workflow.Node;
import com.alibaba.cloud.ai.studio.runtime.domain.workflow.ValueFromEnum;
import com.alibaba.cloud.ai.studio.runtime.utils.JsonUtils;
import com.alibaba.cloud.ai.studio.runtime.enums.ParameterTypeEnum;
import com.alibaba.cloud.ai.studio.core.workflow.WorkflowContext;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import ognl.Ognl;
import ognl.OgnlException;
import org.apache.commons.lang3.StringUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for variable operations and value extraction
 *
 * @since 1.0.0.3
 */
@Slf4j
public class VariableUtils {

	// Prefix for JSON path expressions
	private static final String JSON_PATH_PREFIX = "$.";

	// Prefix for variable expressions
	private static final String VARIABLE_PREFIX = "${";

	// Suffix for variable expressions
	private static final String VARIABLE_POSTFIX = "}";

	// Pattern for validating expressions
	private static final Pattern VALID_EXPRESSION_PATTERN = Pattern.compile("[0-9a-zA-Z\\-\\._\\[\\]]+");

	// Pattern for matching ${xxx} format
	public static final Pattern VAR_EXPR_PATTERN = Pattern.compile("\\$\\{([^}]*)\\}");

	/**
	 * Decorates expression by replacing #xxxx# with ${xxxx} to match OGNL syntax
	 */
	private static String decorateExpression(String expression) {
		String pattern = "#(.*?)#";
		String replacement = "\\{$1}";
		replacement = expression.replaceAll(pattern, replacement);
		return replacement;
	}

	/**
	 * Gets value from payload using OGNL expression
	 */
	public static Object getValueFromPayload(String expression, Map<String, Object> payload) {
		if (StringUtils.isBlank(expression) || payload == null) {
			return null;
		}
		Matcher matcher = VALID_EXPRESSION_PATTERN.matcher(expression);
		if (matcher.matches()) {
			// 将[]转为{}，适配array下的获取逻辑
			String replaceExpression = expression.replaceAll("\\[", "\\{").replaceAll("\\]", "\\}");
			Object value;
			try {
				value = Ognl.getValue(replaceExpression, payload);
			}
			catch (OgnlException e) {
				log.error("getValueFromPayload error, expression:{}, payload:{}", expression, payload, e);
				return null;
			}
			return value;
		}
		return null;
	}

	/**
	 * Gets string value from payload using OGNL expression
	 */
	public static String getValueStringFromPayload(String expression, Map<String, Object> payload) {
		Object valueFromContext = getValueFromPayload(expression, payload);
		if (valueFromContext == null) {
			return null;
		}
		if (valueFromContext instanceof String || valueFromContext instanceof Number
				|| valueFromContext instanceof Boolean) {
			return String.valueOf(valueFromContext);
		}
		else {
			return JsonUtils.toJson(valueFromContext);
		}
	}

	/**
	 * Sets value in payload using OGNL expression
	 */
	public static boolean setValueForPayload(String expression, Map<String, Object> payload, Object value) {
		if (StringUtils.isBlank(expression) || payload == null) {
			return false;
		}
		try {
			Matcher matcher = VALID_EXPRESSION_PATTERN.matcher(expression);
			if (matcher.matches()) {
				// 将[]转为{}，适配array下的获取逻辑
				String replaceExpression = expression.replaceAll("\\[", "\\{").replaceAll("\\]", "\\}");
				// 这里执行的命令会被拦截
				Ognl.setValue(replaceExpression, payload, value);
				return true;
			}
			return false;
		}
		catch (Exception e) {
			return false;
		}
	}

	/**
	 * Extracts expression from ${xxx} format
	 */
	public static String getExpressionFromBracket(String expression) {
		if (StringUtils.isBlank(expression)) {
			return null;
		}
		if (expression.startsWith(VARIABLE_PREFIX) && expression.endsWith(VARIABLE_POSTFIX)) {
			return expression.substring(2, expression.length() - 1);
		}
		return expression;
	}

	/**
	 * Gets string value from workflow context
	 */
	public static String getValueStringFromContext(Node.InputParam inputParam, WorkflowContext context) {
		Object valueFromContext = getValueFromContext(inputParam, context);
		if (valueFromContext == null) {
			return null;
		}
		if (valueFromContext instanceof String || valueFromContext instanceof Number
				|| valueFromContext instanceof Boolean) {
			return String.valueOf(valueFromContext);
		}
		else {
			return JsonUtils.toJson(valueFromContext);
		}
	}

	/**
	 * Gets value from workflow context
	 */
	public static Object getValueFromContext(Node.InputParam inputParam, WorkflowContext context) {
		return getValueFromContext((CommonParam) inputParam, context);
	}

	/**
	 * Gets value from workflow context using common parameters
	 */
	public static Object getValueFromContext(CommonParam commonParam, WorkflowContext context) {
		if (commonParam == null || context == null) {
			return null;
		}
		String valueFrom = commonParam.getValueFrom();
		Object value = commonParam.getValue();
		if (StringUtils.isBlank(valueFrom)) {
			valueFrom = ValueFromEnum.refer.name();
		}
		if (value == null) {
			return null;
		}
		if (valueFrom.equals(ValueFromEnum.refer.name())) {
			String expression = VariableUtils.getExpressionFromBracket((String) value);
			if (StringUtils.isNotBlank(expression)) {
				Object o = context.getVariablesMap().get(expression);
				if (o != null) {
					return o;
				}
				else {
					return getValueFromPayload(expression, context.getVariablesMap());
				}
			}
		}
		else {
			return value;
		}
		return null;
	}

	/**
	 * Converts value to specified type
	 * @param key Parameter key
	 * @param type Target type
	 * @param value Value to convert
	 * @return Converted value
	 */
	public static Object convertValueByType(String key, String type, Object value) {
		try {
			// todo 后续删除
			if (type == null) {
				return value;
			}
			if (value == null) {
				return null;
			}
			if (ParameterTypeEnum.OBJECT.getCode().equals(type)) {
				if (value instanceof Map) {
					return value;
				}
				else if (value instanceof String) {
					return JsonUtils.fromJsonToMap((String) value);
				}
			}
			else if (ParameterTypeEnum.STRING.getCode().equals(type)) {
				if (value instanceof String) {
					return value;
				}
				else if (value instanceof Map || value instanceof List) {
					return JsonUtils.toJson(value);
				}
				else {
					return String.valueOf(value);
				}
			}
			else if (ParameterTypeEnum.NUMBER.getCode().equals(type)) {
				if (value instanceof Number) {
					return value;
				}
				else if (value instanceof String) {
					String strValue = (String) value;
					if (strValue.contains(".")) {
						return Double.parseDouble(strValue);
					}
					else {
						long longValue = Long.parseLong(strValue);
						if (longValue <= Integer.MAX_VALUE && longValue >= Integer.MIN_VALUE) {
							return (int) longValue;
						}
						else {
							return longValue;
						}
					}
				}
			}
			else if (ParameterTypeEnum.BOOLEAN.getCode().equals(type)) {
				if (value instanceof Boolean) {
					return value;
				}
				else if (value instanceof String) {
					String strValue = ((String) value).toLowerCase();
					if ("true".equals(strValue) || "false".equals(strValue)) {
						return Boolean.parseBoolean(strValue);
					}
				}
			}
			else if (ParameterTypeEnum.FILE.getCode().equals(type)) {
				if (value instanceof File) {
					return value;
				}
				else if (value instanceof String) {
					return JsonUtils.fromJson((String) value, File.class);
				}
			}
			else if (ParameterTypeEnum.ARRAY_OBJECT.getCode().equals(type)) {
				if (value instanceof List) {
					return value;
				}
				else if (value instanceof String) {
					return JsonUtils.fromJsonToList((String) value, Map.class);
				}
			}
			else if (ParameterTypeEnum.ARRAY_STRING.getCode().equals(type)) {
				if (value instanceof List) {
					return value;
				}
				else if (value instanceof String) {
					return JsonUtils.fromJsonToList((String) value, String.class);
				}
			}
			else if (ParameterTypeEnum.ARRAY_NUMBER.getCode().equals(type)) {
				if (value instanceof List) {
					return value;
				}
				else if (value instanceof String) {
					return JsonUtils.fromJsonToList((String) value, Number.class);
				}
			}
			else if (ParameterTypeEnum.ARRAY_BOOLEAN.getCode().equals(type)) {
				if (value instanceof List) {
					return value;
				}
				else if (value instanceof String) {
					return JsonUtils.fromJsonToList((String) value, Boolean.class);
				}
			}
			else if (ParameterTypeEnum.ARRAY_FILE.getCode().equals(type)) {
				if (value instanceof List) {
					return value;
				}
				else if (value instanceof String) {
					return JsonUtils.fromJsonToList((String) value, File.class);
				}
			}
		}
		catch (Exception e) {
			throw new BizException(ErrorCode.INVALID_PARAMS.toError(key,
					"expected type is " + type + " but not match the current value type."));
		}
		throw new BizException(ErrorCode.INVALID_PARAMS.toError(key,
				"expected type is " + type + " but not match the current value type."));
	}

	/**
	 * Identifies variables from text content (ordered, non-unique)
	 */
	public static List<String> identifyVariableListFromText(String content) {
		List<String> result = Lists.newArrayList();
		if (StringUtils.isBlank(content)) {
			return result;
		}
		Matcher matcher = VAR_EXPR_PATTERN.matcher(content);
		// 查找并添加匹配的内容
		while (matcher.find()) {
			result.add(matcher.group(1));
		}
		return result;
	}

	/**
	 * Identifies unique variables from text content
	 */
	public static Set<String> identifyVariableSetFromText(String content) {
		List<String> variables = identifyVariableListFromText(content);
		return new HashSet<>(variables);
	}

}
