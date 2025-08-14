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

import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Prompt loader, used to load prompt templates from file system
 *
 * @author zhangshenghang
 */
public class PromptLoader {

	private static final String PROMPT_PATH_PREFIX = "prompts/";

	private static final ConcurrentHashMap<String, String> promptCache = new ConcurrentHashMap<>();

	/**
	 * Load prompt template from file
	 * @param promptName prompt file name (without path and extension)
	 * @return prompt content
	 */
	public static String loadPrompt(String promptName) {
		return promptCache.computeIfAbsent(promptName, name -> {
			try {
				String fileName = PROMPT_PATH_PREFIX + name + ".txt";
				ClassPathResource resource = new ClassPathResource(fileName);
				if (!resource.exists()) {
					throw new IllegalArgumentException("提示词文件不存在: " + fileName);
				}
				return StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
			}
			catch (IOException e) {
				throw new RuntimeException("加载提示词失败: " + name, e);
			}
		});
	}

	/**
	 * Clear prompt cache
	 */
	public static void clearCache() {
		promptCache.clear();
	}

	/**
	 * Get cache size
	 * @return number of prompts in cache
	 */
	public static int getCacheSize() {
		return promptCache.size();
	}

}
