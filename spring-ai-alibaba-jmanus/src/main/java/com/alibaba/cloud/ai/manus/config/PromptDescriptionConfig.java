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

import com.alibaba.cloud.ai.manus.prompt.PromptDescriptionLoader;
import com.alibaba.cloud.ai.manus.prompt.model.enums.PromptEnum;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

/**
 * Configuration class to initialize the prompt description loader
 */
@Configuration
public class PromptDescriptionConfig {

	@Autowired
	private PromptDescriptionLoader promptDescriptionLoader;

	@PostConstruct
	public void initializeDescriptionLoader() {
		PromptEnum.setDescriptionLoader(promptDescriptionLoader);
	}

}
