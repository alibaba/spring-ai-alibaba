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
package com.alibaba.cloud.ai.graph.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class TemplateTransformNode implements NodeAction {

	private static final Logger logger = LoggerFactory.getLogger(TemplateTransformNode.class);

	private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{\\{\\s*(.+?)\\s*\\}\\}");

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

		// Replace {{key}} in the template with the value of state.get(key)
		StringBuffer sb = new StringBuffer();
		Matcher matcher = PLACEHOLDER_PATTERN.matcher(template);

		while (matcher.find()) {
			String key = matcher.group(1).trim();
			Optional<Object> valueOpt = state.value(key);
			String replacement;

			if (valueOpt.isPresent()) {
				Object val = valueOpt.get();
				replacement = val != null ? val.toString() : "null";
				// Escape special regex characters in replacement
				replacement = replacement.replace("\\", "\\\\").replace("$", "\\$");
				logger.debug("Replaced placeholder {{{}}} with value: {}", key, replacement);
			}
			else {
				// Keep the placeholder if the variable is not found
				replacement = "{{" + key + "}}";
				replacement = replacement.replace("\\", "\\\\").replace("$", "\\$");
				logger.debug("Variable '{}' not found, keeping placeholder: {}", key, replacement);
			}

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
	 * Create a new builder for TemplateTransformNode
	 * @return a new Builder instance
	 */
	public static Builder builder() {
		return new Builder();
	}

	/**
	 * Builder class for TemplateTransformNode
	 */
	public static class Builder {

		private String template;

		private String outputKey;

		/**
		 * Set the template string with placeholders
		 * @param template the template string, e.g., "Hello {{name}}!"
		 * @return this builder
		 * @throws IllegalArgumentException if template is null
		 */
		public Builder template(String template) {
			if (template == null) {
				throw new IllegalArgumentException("Template cannot be null");
			}
			this.template = template;
			return this;
		}

		/**
		 * Set the output key for the transformed result
		 * @param outputKey the key to store the result in state, defaults to "result"
		 * @return this builder
		 */
		public Builder outputKey(String outputKey) {
			this.outputKey = outputKey;
			return this;
		}

		/**
		 * Build the TemplateTransformNode
		 * @return a new TemplateTransformNode instance
		 * @throws IllegalArgumentException if template is null or empty
		 */
		public TemplateTransformNode build() {
			if (template == null || template.trim().isEmpty()) {
				throw new IllegalArgumentException("Template cannot be null or empty");
			}
			return new TemplateTransformNode(template, outputKey);
		}

	}

}
