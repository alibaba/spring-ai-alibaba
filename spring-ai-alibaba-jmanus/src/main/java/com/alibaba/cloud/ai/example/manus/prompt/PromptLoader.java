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
package com.alibaba.cloud.ai.example.manus.prompt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.chat.messages.Message;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Prompt loader that loads prompt template files from resources/prompts directory
 */
@Component
public class PromptLoader {

	private static final Logger log = LoggerFactory.getLogger(PromptLoader.class);

	private static final String PROMPT_BASE_PATH = "prompts/";

	// Cache for loaded prompt content
	private final Map<String, String> promptCache = new ConcurrentHashMap<>();

	/**
	 * Load prompt template content
	 * @param promptPath Relative path of prompt file (relative to prompts directory)
	 * @return Prompt content
	 */
	public String loadPrompt(String promptPath) {
		return promptCache.computeIfAbsent(promptPath, this::loadPromptFromResource);
	}

	/**
	 * Load prompt content from resource file
	 * @param promptPath Prompt file path
	 * @return Prompt content
	 */
	private String loadPromptFromResource(String promptPath) {
		try {
			String fullPath = PROMPT_BASE_PATH + promptPath;
			Resource resource = new ClassPathResource(fullPath);

			if (!resource.exists()) {
				log.warn("Prompt file not found: {}", fullPath);
				return "";
			}

			String content = resource.getContentAsString(StandardCharsets.UTF_8);
			log.debug("Loaded prompt from: {}", fullPath);
			return content;

		}
		catch (IOException e) {
			log.error("Failed to load prompt from: {}", promptPath, e);
			return "";
		}
	}

	/**
	 * Create system prompt template message
	 * @param promptPath Prompt file path
	 * @param variables Variable mapping
	 * @return System message
	 */
	public Message createSystemMessage(String promptPath, Map<String, Object> variables) {
		String promptContent = loadPrompt(promptPath);
		SystemPromptTemplate template = new SystemPromptTemplate(promptContent);
		return template.createMessage(variables != null ? variables : Map.of());
	}

	/**
	 * Create user prompt template message
	 * @param promptPath Prompt file path
	 * @param variables Variable mapping
	 * @return User message
	 */
	public Message createUserMessage(String promptPath, Map<String, Object> variables) {
		String promptContent = loadPrompt(promptPath);
		PromptTemplate template = new PromptTemplate(promptContent);
		return template.createMessage(variables != null ? variables : Map.of());
	}

	/**
	 * Render prompt template
	 * @param promptPath Prompt file path
	 * @param variables Variable mapping
	 * @return Rendered prompt content
	 */
	public String renderPrompt(String promptPath, Map<String, Object> variables) {
		String promptContent = loadPrompt(promptPath);
		PromptTemplate template = new PromptTemplate(promptContent);
		return template.render(variables != null ? variables : Map.of());
	}

	/**
	 * Clear prompt cache
	 */
	public void clearCache() {
		promptCache.clear();
		log.info("Prompt cache cleared");
	}

}
