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

package com.alibaba.cloud.ai.manus.config;

import org.springframework.stereotype.Component;

/**
 * Default LLM Configuration for DashScope
 */
@Component
public class DefaultLlmConfiguration {

	public static final String DEFAULT_MODEL_NAME = "qwen-plus";

	public static final String DEFAULT_BASE_URL = "https://dashscope.aliyuncs.com/compatible-mode";

	public static final String DEFAULT_DESCRIPTION = "Alibaba Cloud DashScope Qwen Plus Model";

	public static final String DEFAULT_COMPLETIONS_PATH = "/v1/chat/completions";

	public String getDefaultModelName() {
		return DEFAULT_MODEL_NAME;
	}

	public String getDefaultBaseUrl() {
		return DEFAULT_BASE_URL;
	}

	public String getDefaultDescription() {
		return DEFAULT_DESCRIPTION;
	}

	public String getDefaultCompletionsPath() {
		return DEFAULT_COMPLETIONS_PATH;
	}

}
