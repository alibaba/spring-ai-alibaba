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

package com.alibaba.cloud.ai.mcp.nacos2.gateway.jsontemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ResponseTemplateParser {

	private static final ObjectMapper objectMapper = new ObjectMapper();

	// 支持 {{.}} 或 {{.xxx}} 变量
	private static final Pattern TEMPLATE_PATTERN = Pattern.compile("\\{\\{\\s*\\.([\\w\\$\\[\\]\\.]*)\\s*}}",
			Pattern.DOTALL);

	/**
	 * 处理响应模板
	 * @param rawResponse 原始响应（JSON或文本）
	 * @param responseTemplate 模板字符串（可为jsonPath、模板、null/空）
	 * @return 处理后的字符串
	 */
	public static String parse(String rawResponse, String responseTemplate) {
		if (!StringUtils.hasText(responseTemplate) || "{{.}}".equals(responseTemplate.trim())) {
			// 原样输出
			return rawResponse;
		}

		// jsonPath 提取
		if (responseTemplate.trim().startsWith("$.") || responseTemplate.trim().startsWith("$[")) {
			try {
				Object result = JsonPath.read(rawResponse, responseTemplate.trim());
				return result != null ? result.toString() : "";
			}
			catch (Exception e) {
				// jsonPath 失败，降级为模板处理
			}
		}

		// 模板变量替换
		try {
			Map<String, Object> context = null;
			boolean isJson = rawResponse.trim().startsWith("{") || rawResponse.trim().startsWith("[");
			if (isJson) {
				context = objectMapper.readValue(rawResponse, Map.class);
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
