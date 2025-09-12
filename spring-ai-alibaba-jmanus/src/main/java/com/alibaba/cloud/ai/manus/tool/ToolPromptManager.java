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
package com.alibaba.cloud.ai.manus.tool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.alibaba.cloud.ai.manus.prompt.service.PromptService;

/**
 * Tool Prompt Manager that manages tool descriptions and parameters using PromptService
 * pattern. Each tool has two prompts: Description and Parameters. Supports real-time
 * updates and language-based versioning through prompt management backend.
 *
 * @author spring-ai-alibaba
 */
@Component
public class ToolPromptManager {

	private static final Logger log = LoggerFactory.getLogger(ToolPromptManager.class);

	private final PromptService promptService;

	public ToolPromptManager(PromptService promptService) {
		this.promptService = promptService;
	}

	/**
	 * Get tool description from prompt service
	 * @param toolName the tool name
	 * @param args optional arguments for template variables
	 * @return the tool description
	 */
	public String getToolDescription(String toolName, Object... args) {
		try {
			String promptName = buildDescriptionPromptName(toolName);
			String template = promptService.getPromptByName(promptName).getPromptContent();

			if (args != null && args.length > 0) {
				return String.format(template, args);
			}
			return template;
		}
		catch (Exception e) {
			log.warn("Failed to load prompt-based tool description for {}, using fallback", toolName, e);
			return getDefaultDescription(toolName);
		}
	}

	/**
	 * Get tool parameters from prompt service
	 * @param toolName the tool name
	 * @return the tool parameters JSON schema
	 */
	public String getToolParameters(String toolName) {
		try {
			String promptName = buildParametersPromptName(toolName);
			return promptService.getPromptByName(promptName).getPromptContent();
		}
		catch (Exception e) {
			log.warn("Failed to load prompt-based tool parameters for {}, using fallback", toolName, e);
			return getDefaultParameters(toolName);
		}
	}

	/**
	 * Build description prompt name for PromptService
	 * @param toolName the tool name
	 * @return the prompt name
	 */
	private String buildDescriptionPromptName(String toolName) {
		return String.format("%s_TOOL_DESCRIPTION", toolName.toUpperCase());
	}

	/**
	 * Build parameters prompt name for PromptService
	 * @param toolName the tool name
	 * @return the prompt name
	 */
	private String buildParametersPromptName(String toolName) {
		return String.format("%s_TOOL_PARAMETERS", toolName.toUpperCase());
	}

	/**
	 * Get default description when prompt is not found
	 * @param toolName the tool name
	 * @return default description
	 */
	private String getDefaultDescription(String toolName) {
		return String.format("Tool: %s", toolName);
	}

	/**
	 * Get default parameters when prompt is not found
	 * @param toolName the tool name
	 * @return default parameters
	 */
	private String getDefaultParameters(String toolName) {
		return """
				{
				    "type": "object",
				    "properties": {},
				    "required": []
				}
				""";
	}

	/**
	 * Refresh prompts from PromptService This will trigger PromptService to reload
	 * prompts from database
	 */
	public void refreshPrompts() {
		try {
			promptService.reinitializePrompts();
			log.info("Tool prompts refreshed from PromptService");
		}
		catch (Exception e) {
			log.error("Failed to refresh tool prompts", e);
		}
	}

}
