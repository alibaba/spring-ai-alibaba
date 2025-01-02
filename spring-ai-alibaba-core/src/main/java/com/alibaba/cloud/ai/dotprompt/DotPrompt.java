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

import lombok.Data;

import java.util.Map;

@Data
public class DotPrompt {

	private String model;

	private Map<String, Object> config;

	private InputSchema input;

	private String template;

	@Data
	public static class InputSchema {

		private Map<String, String> schema;

		private Map<String, Object> defaultValues;

	}

	public DotPrompt withModel(String model) {
		if (model != null) {
			this.model = model;
		}
		return this;
	}

	public DotPrompt withConfig(Map<String, Object> config) {
		if (config != null) {
			if (this.config == null) {
				this.config = config;
			}
			else {
				this.config.putAll(config);
			}
		}
		return this;
	}

}
