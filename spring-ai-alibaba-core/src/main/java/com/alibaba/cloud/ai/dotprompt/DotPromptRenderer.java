/*
 * Copyright 2023-2024 the original author or authors.
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

package com.alibaba.cloud.ai.dotprompt;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class DotPromptRenderer {

	private final Handlebars handlebars;

	public DotPromptRenderer(DotPromptProperties properties) {
		this.handlebars = new Handlebars();
		if (properties.isCacheEnabled()) {
			this.handlebars.with(new LRUTemplateCache(properties.getCacheSize()));
		}
	}

	public String render(DotPrompt prompt, Map<String, Object> variables) {
		Map<String, Object> context = new HashMap<>();

		// Merge default values with provided variables
		if (prompt.getInput() != null && prompt.getInput().getDefaultValues() != null) {
			context.putAll(prompt.getInput().getDefaultValues());
		}
		if (variables != null) {
			context.putAll(variables);
		}

		// Validate required variables
		if (prompt.getInput() != null && prompt.getInput().getSchema() != null) {
			for (Map.Entry<String, String> entry : prompt.getInput().getSchema().entrySet()) {
				String key = entry.getKey();
				if (!key.endsWith("?") && !context.containsKey(key)) {
					throw new IllegalArgumentException("Missing required variable: " + key);
				}
			}
		}

		try {
			Template template = handlebars.compileInline(prompt.getTemplate());
			return template.apply(context);
		}
		catch (IOException e) {
			throw new RuntimeException("Failed to render template", e);
		}
	}

}
