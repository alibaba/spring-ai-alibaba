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

package com.alibaba.cloud.ai.mcp.gateway.core.jsontemplate;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import com.jayway.jsonpath.JsonPath;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ResponseTemplateParser {

	private static final ObjectMapper objectMapper = new ObjectMapper();

	private static final Handlebars handlebars = new Handlebars();

	// Supports {{.}} or {{.xxx}} or {{.xxx.yyy}} multi-level variables
	private static final Pattern TEMPLATE_PATTERN = Pattern.compile("\\{\\{\\s*\\.([\\w\\$\\[\\]\\.]*)\\s*}}",
			Pattern.DOTALL);

	// Detects multi-level path access patterns like {{.xxx.yyy}}
	// This regex is fully covered by unit tests in ResponseTemplateParserTest.java
	private static final Pattern MULTI_LEVEL_PATTERN = Pattern.compile("\\{\\{\\s*\\.\\w+\\.[\\w\\.]+\\s*}}");

	/**
	 * Process response template
	 * @param rawResponse raw response (JSON or text)
	 * @param responseTemplate template string (can be jsonPath, template, null/empty)
	 * @return processed string
	 */
	public static String parse(String rawResponse, String responseTemplate) {
		if (!StringUtils.hasText(responseTemplate) || "{{.}}".equals(responseTemplate.trim())) {
			// Return raw output
			return rawResponse;
		}

		// JsonPath extraction
		if (responseTemplate.trim().startsWith("$.") || responseTemplate.trim().startsWith("$[")) {
			try {
				Object result = JsonPath.read(rawResponse, responseTemplate.trim());
				return result != null ? result.toString() : "";
			}
			catch (Exception e) {
				// JsonPath failed, fallback to template processing
			}
		}

		// Detect multi-level path access
		if (MULTI_LEVEL_PATTERN.matcher(responseTemplate).find()) {
			return parseWithHandlebars(rawResponse, responseTemplate);
		}

		// Simple template variable replacement (maintain backward compatibility)
		return parseWithSimpleTemplate(rawResponse, responseTemplate);
	}

	private static String parseWithHandlebars(String rawResponse, String responseTemplate) {
		try {
			// 1. Preprocess template: convert syntax to be compatible with Handlebars
			String handlebarsTemplateStr = responseTemplate
				// Remove dot prefix: {{ .xxx.yyy }} -> {{xxx.yyy}}
				.replaceAll("\\{\\{\\s*\\.", "{{")
				// Convert array access syntax: {{users.[0].name}} -> {{users.0.name}}
				.replaceAll("\\[([0-9]+)\\]", "$1");

			// 2. Compile template
			Template template = handlebars.compileInline(handlebarsTemplateStr);

			Map<String, Object> dataContext;
			boolean isJson = rawResponse.trim().startsWith("{") || rawResponse.trim().startsWith("[");
			if (isJson) {
				dataContext = objectMapper.readValue(rawResponse, new TypeReference<Map<String, Object>>() {
				});
			}
			else {
				// Non-JSON data, create a context containing the raw response
				dataContext = Map.of("_raw", rawResponse);
			}

			return template.apply(dataContext);

		}
		catch (Exception e) {
			return parseWithSimpleTemplate(rawResponse, responseTemplate);
		}
	}

	private static String parseWithSimpleTemplate(String rawResponse, String responseTemplate) {
		try {
			Map<String, Object> context = null;
			boolean isJson = rawResponse.trim().startsWith("{") || rawResponse.trim().startsWith("[");
			if (isJson) {
				context = objectMapper.readValue(rawResponse, new TypeReference<Map<String, Object>>() {
				});
			}
			StringBuffer sb = new StringBuffer();
			Matcher matcher = TEMPLATE_PATTERN.matcher(responseTemplate);
			while (matcher.find()) {
				String key = matcher.group(1);
				String value;
				if (key == null || key.isEmpty()) {
					// {{.}} 变量，直接替换为原始响应
					value = rawResponse;
				}
				else if (context != null && context.containsKey(key)) {
					value = String.valueOf(context.get(key));
				}
				else {
					value = "";
				}
				matcher.appendReplacement(sb, Matcher.quoteReplacement(value));
			}
			matcher.appendTail(sb);
			return sb.toString();
		}
		catch (Exception e) {
			// 模板处理失败，降级为原样输出
			return rawResponse;
		}
	}

}
