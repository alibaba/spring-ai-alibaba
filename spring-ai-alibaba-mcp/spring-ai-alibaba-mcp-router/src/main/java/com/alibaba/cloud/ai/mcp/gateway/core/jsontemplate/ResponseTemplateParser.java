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

	// 支持 {{.}} 或 {{.xxx}} 或 {{.xxx.yyy}} 等多层级变量
	private static final Pattern TEMPLATE_PATTERN = Pattern.compile("\\{\\{\\s*\\.([\\w\\$\\[\\]\\.]*)\\s*}}",
			Pattern.DOTALL);

	// 检测是否包含多层级路径访问（如 {{.xxx.yyy}}）
	private static final Pattern MULTI_LEVEL_PATTERN = Pattern.compile("\\{\\{\\s*\\.\\w+\\.[\\w\\.]+\\s*}}");

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

		// 检测是否包含多层级路径访问，如果包含则使用 Handlebars 引擎
		if (MULTI_LEVEL_PATTERN.matcher(responseTemplate).find()) {
			return parseWithHandlebars(rawResponse, responseTemplate);
		}

		// 简单模板变量替换（保持原有逻辑以确保向后兼容）
		return parseWithSimpleTemplate(rawResponse, responseTemplate);
	}

	/**
	 * 使用 Handlebars 引擎处理多层级模板 兼容 higress.cn/ai/mcp-server 的模板语法 {{ .xxx.yyy }}
	 */
	private static String parseWithHandlebars(String rawResponse, String responseTemplate) {
		try {
			// 1. 预处理模板：转换语法以兼容 Handlebars
			String handlebarsTemplateStr = responseTemplate
				// 移除点号前缀：{{ .xxx.yyy }} -> {{xxx.yyy}}
				.replaceAll("\\{\\{\\s*\\.", "{{")
				// 转换数组访问语法：{{users.[0].name}} -> {{users.0.name}}
				.replaceAll("\\[([0-9]+)\\]", "$1");

			// 2. 编译模板
			Template template = handlebars.compileInline(handlebarsTemplateStr);

			// 3. 准备数据上下文：将JSON字符串解析为 Map
			Map<String, Object> dataContext;
			boolean isJson = rawResponse.trim().startsWith("{") || rawResponse.trim().startsWith("[");
			if (isJson) {
				dataContext = objectMapper.readValue(rawResponse, new TypeReference<Map<String, Object>>() {
				});
			}
			else {
				// 非JSON数据，创建一个包含原始响应的上下文
				dataContext = Map.of("_raw", rawResponse);
			}

			// 4. 应用模板并返回结果
			return template.apply(dataContext);

		}
		catch (Exception e) {
			// Handlebars 处理失败，降级为简单模板处理
			return parseWithSimpleTemplate(rawResponse, responseTemplate);
		}
	}

	/**
	 * 简单模板变量替换（原有逻辑）
	 */
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
