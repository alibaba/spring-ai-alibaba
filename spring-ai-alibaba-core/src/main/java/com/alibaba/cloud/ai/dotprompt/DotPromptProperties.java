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
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = DotPromptProperties.PREFIX)
public class DotPromptProperties {

	public static final String PREFIX = "spring.ai.alibaba.dotprompt";

	/**
	 * Base path for prompt files, defaults to "classpath:prompts/"
	 */
	private String basePath = "classpath:prompts/";

	/**
	 * File extension for prompt files, defaults to ".prompt"
	 */
	private String fileExtension = ".prompt";

	/**
	 * Enable caching of parsed prompts
	 */
	private boolean cacheEnabled = true;

	/**
	 * Cache size, defaults to 100
	 */
	private int cacheSize = 100;

}
