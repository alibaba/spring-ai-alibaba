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
package com.alibaba.cloud.ai.graph.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TemplateTransformNode implements NodeAction {

	private static final Logger logger = LoggerFactory.getLogger(TemplateTransformNode.class);

	/**
	 * Regex pattern to match template placeholders of the form:
	 *   {{ key }} or {{ key ?: defaultValue }}
	 *
	 * Structure:
	 *   - \\{\\{         : Matches the opening double curly braces.
	 *   - \\s*           : Optional whitespace.
	 *   - (.+?)          : Capture group 1 - the key (variable name or path).
	 *   - (?:            : Non-capturing group for the Elvis operator.
	 *       \\s*\\?:\\s* : Matches optional whitespace, the Elvis operator '?:', and more optional whitespace.
	 *       (.+?)        : Capture group 2 - the default value if provided.
	 *     )?             : The non-capturing group is optional (for cases without a default value).
	 *   - \\s*           : Optional whitespace.
	 *   - \\}\\}         : Matches the closing double curly braces.
	 *
	 * Capture groups:
	 *   1. key           : The variable name or path to be replaced.
	 *   2. defaultValue  : The default value to use if the key is not found (may be null).
	 */
	private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{\\{\\s*(.+?)(?:\\s*\\?:\\s*(.+?))?\\s*\\}\\}");

	private static final Pattern ARRAY_INDEX_PATTERN = Pattern.compile("^(.+?)\\[(\\d+)\\]$");

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	private final String template;

	private final String outputKey;

	private TemplateTransformNode(String template, String outputKey) {
		this.template = template;
		this.outputKey = outputKey != null ? outputKey : "result";
	}

	@Override
	public Map<String, Object> apply(OverAllState state) {
		if (template == null || template.isEmpty()) {
			logger.warn("Template is null or empty, returning empty result");
			Map<String, Object> result = new HashMap<>();
			result.put(outputKey, "");
			return result;
		}

		logger.debug("Processing template: {}", template);

		StringBuffer sb = new StringBuffer();
		Matcher matcher = PLACEHOLDER_PATTERN.matcher(template);

		while (matcher.find()) {
			String key = matcher.group(1).trim();
			String defaultValue = matcher.group(2);
			String replacement;

			// Support nested object access (e.g., "http_response.body")
			Object val = getNestedValue(state, key);

			if (val != null) {
				replacement = val.toString();
				logger.debug("Replaced placeholder {{{}}} with value: {}", key, replacement);
			}
			else if (defaultValue != null) {
				replacement = parseDefaultValue(defaultValue.trim());
				logger.debug("Variable '{}' is null, using default value: {}", key, replacement);
			}
			else if (state.data().containsKey(key)) {
				replacement = "null";
				logger.debug("Variable '{}' exists but is null: {}", key, replacement);
			}
			else {
				if (defaultValue != null) {
					replacement = "{{" + key + " ?: " + defaultValue.trim() + "}}";
				} else {
					replacement = "{{" + key + "}}";
				}
				logger.debug("Variable '{}' not found, keeping placeholder: {}", key, replacement);
			}
			replacement = replacement.replace("\\", "\\\\").replace("$", "\\$");
			matcher.appendReplacement(sb, replacement);
		}
		matcher.appendTail(sb);

		String resolved = sb.toString();
		logger.debug("Template transformation completed. Result: {}", resolved);

		// Write the result back to the state
		Map<String, Object> result = new HashMap<>();
		result.put(outputKey, resolved);
		return result;
	}

	/**
	 * @param state the overall state
	 * @param key the key path (supports dot notation and array indexing)
	 * @return the value or null if not found
	 */
	private Object getNestedValue(OverAllState state, String key) {
		if (key == null || key.trim().isEmpty()) {
			return null;
		}

		String[] parts = key.split("\\.");
		Object current = null;

		String rootKey = parts[0];
		Matcher rootMatcher = ARRAY_INDEX_PATTERN.matcher(rootKey);
		if (rootMatcher.matches()) {
			String actualKey = rootMatcher.group(1);
			int index = Integer.parseInt(rootMatcher.group(2));
			if (state.data().containsKey(actualKey)) {
				Object rootValue = state.data().get(actualKey);
				if (rootValue instanceof String && isJsonString((String) rootValue)) {
					try {
						rootValue = OBJECT_MAPPER.readValue((String) rootValue, Object.class);
						logger.debug("Parsed JSON string at root key: {}", actualKey);
					}
					catch (Exception e) {
						logger.debug("Failed to parse JSON string at root: {}", e.getMessage());
						return null;
					}
				}
				current = getArrayElement(rootValue, index);
			}
			else {
				return null;
			}
		}
		else {
			if (state.data().containsKey(rootKey)) {
				current = state.data().get(rootKey);
				// For single key (no dots), return the value even if it's null
				if (parts.length == 1) {
					return current;
				}
			}
			else {
				return null;
			}
		}

		for (int i = 1; i < parts.length && current != null; i++) {
			String part = parts[i];

			Matcher matcher = ARRAY_INDEX_PATTERN.matcher(part);
			if (matcher.matches()) {
				String fieldName = matcher.group(1);
				int index = Integer.parseInt(matcher.group(2));

				current = getFieldOrProperty(current, fieldName);
				if (current != null) {
					current = getArrayElement(current, index);
				}
			}
			else {
				current = getFieldOrProperty(current, part);
			}
		}

		return current;
	}

	/**
	 * Get a field or property value from an object. Supports: 1. Map access 2. POJO
	 * reflection (getter method or direct field access) 3. JSON string auto-parsing
	 * @param obj the object to extract value from
	 * @param fieldName the field or property name
	 * @return the value or null if not found
	 */
	private Object getFieldOrProperty(Object obj, String fieldName) {
		if (obj == null) {
			return null;
		}

		if (obj instanceof String) {
			String str = (String) obj;
			if (isJsonString(str)) {
				try {
					obj = OBJECT_MAPPER.readValue(str, Object.class);
					logger.debug("Parsed JSON string to object for field access: {}", fieldName);
				}
				catch (Exception e) {
					logger.debug("Failed to parse JSON string, treating as plain string: {}", e.getMessage());
					return null;
				}
			}
			else {
				return null;
			}
		}

		if (obj instanceof Map) {
			@SuppressWarnings("unchecked")
			Map<String, Object> map = (Map<String, Object>) obj;
			return map.get(fieldName);
		}

		return getPojoProperty(obj, fieldName);
	}

	/**
	 * Get a property value from a POJO using reflection (getter method or field access)
	 * @param obj the POJO object
	 * @param propertyName the property name
	 * @return the value or null if not found
	 */
	private Object getPojoProperty(Object obj, String propertyName) {
		if (obj == null || propertyName == null) {
			return null;
		}

		Class<?> clazz = obj.getClass();

		try {
			String getterName = "get" + capitalize(propertyName);
			Method getter = clazz.getMethod(getterName);
			return getter.invoke(obj);
		}
		catch (Exception e) {
			try {
				String booleanGetterName = "is" + capitalize(propertyName);
				Method booleanGetter = clazz.getMethod(booleanGetterName);
				return booleanGetter.invoke(obj);
			}
			catch (Exception ex) {
			}
		}

		try {
			Field field = clazz.getDeclaredField(propertyName);
			field.setAccessible(true);
			return field.get(obj);
		}
		catch (Exception e) {
			logger.debug("Failed to access property '{}' on object of type {}: {}", propertyName, clazz.getName(),
					e.getMessage());
			return null;
		}
	}

	/**
	 * Get an element from an array or list
	 * @param obj the array or list object
	 * @param index the index
	 * @return the element or null if index out of bounds
	 */
	private Object getArrayElement(Object obj, int index) {
		if (obj == null) {
			return null;
		}

		if (obj instanceof List) {
			List<?> list = (List<?>) obj;
			if (index >= 0 && index < list.size()) {
				return list.get(index);
			}
			else {
				logger.debug("Index {} out of bounds for list of size {}", index, list.size());
				return null;
			}
		}

		if (obj.getClass().isArray()) {
			try {
				Object[] array = (Object[]) obj;
				if (index >= 0 && index < array.length) {
					return array[index];
				}
				else {
					logger.debug("Index {} out of bounds for array of length {}", index, array.length);
					return null;
				}
			}
			catch (ClassCastException e) {
				logger.debug("Primitive array access not fully supported");
				return null;
			}
		}

		logger.debug("Object is not an array or list: {}", obj.getClass().getName());
		return null;
	}

	/**
	 * Check if a string is a JSON string (object or array)
	 * @param str the string to check
	 * @return true if it's a JSON string
	 */
	private boolean isJsonString(String str) {
		if (str == null || str.trim().isEmpty()) {
			return false;
		}
		String trimmed = str.trim();
		ObjectMapper mapper = new ObjectMapper();
		try {
			mapper.readTree(trimmed);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * Capitalize the first letter of a string
	 * @param str the string
	 * @return the capitalized string
	 */
	private String capitalize(String str) {
		if (str == null || str.isEmpty()) {
			return str;
		}
		return Character.toUpperCase(str.charAt(0)) + str.substring(1);
	}

	/**
	 * Parse default value string, removing quotes if present Supports both single
	 * quotes and double quotes: 'default' or "default"
	 * @param defaultValue the default value string (may include quotes)
	 * @return the parsed default value without quotes
	 */
	private String parseDefaultValue(String defaultValue) {
		if (defaultValue == null || defaultValue.isEmpty()) {
			return "";
		}
		if ((defaultValue.startsWith("'") && defaultValue.endsWith("'"))
				|| (defaultValue.startsWith("\"") && defaultValue.endsWith("\""))) {
			if (defaultValue.length() >= 2) {
				return defaultValue.substring(1, defaultValue.length() - 1);
			}
		}

		return defaultValue;
	}

	public static Builder builder() {
		return new Builder();
	}

	/**
	 * Builder class for TemplateTransformNode
	 */
	public static class Builder {

		private String template;

		private String outputKey;

		public Builder template(String template) {
			if (template == null) {
				throw new IllegalArgumentException("Template cannot be null");
			}
			this.template = template;
			return this;
		}

		public Builder outputKey(String outputKey) {
			this.outputKey = outputKey;
			return this;
		}

		public TemplateTransformNode build() {
			if (template == null) {
				throw new IllegalArgumentException("Template cannot be null");
			}
			return new TemplateTransformNode(template, outputKey);
		}

	}

}
