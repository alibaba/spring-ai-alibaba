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
package com.alibaba.cloud.ai.prompt;

import org.springframework.ai.chat.prompt.PromptTemplate;

/**
 * Callback interface that can be used to customize a {@link PromptTemplate.Builder}
 *
 * @since 1.0.0.3
 * @author yuhuangbin
 */
@FunctionalInterface
public interface PromptTemplateCustomizer {

	/**
	 * Callback to customize a {@link PromptTemplate.Builder}
	 * @param builder the template builder to customize
	 */
	void customize(PromptTemplate.Builder builder);

}
