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
package com.alibaba.cloud.ai.utils;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringTemplateUtil {

	/**
	 * turn dify string template into spring-ai template
	 * @param template e.g. "the output is {{#output}}"
	 * @param variables e.g. {{#output#}} -> context
	 * @return spring-ai template e.g. {output}
	 */
	public static String fromDifyTmpl(String template, List<String> variables) {
		String regex = "\\{\\{#(.*?)#\\}\\}";
		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(template);
		StringBuilder result = new StringBuilder();
		while (matcher.find()) {
			String variable = matcher.group(1);
			variables.add(variable);
			matcher.appendReplacement(result, "{" + variable + "}");
		}
		matcher.appendTail(result);
		return result.toString();
	}

	/**
	 * turn spring-ai template into dify string template
	 * @param template e.g. "the output is {output}"
	 * @return dify template e.g. "the output is {{#output#}}"
	 */
	public static String toDifyTmpl(String template) {
		String regex = "\\{(.*?)}";
		return template.replaceAll(regex, "{{#$1#}}");
	}

}
