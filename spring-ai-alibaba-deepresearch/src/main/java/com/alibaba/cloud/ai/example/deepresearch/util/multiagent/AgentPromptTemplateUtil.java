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

package com.alibaba.cloud.ai.example.deepresearch.util.multiagent;

import com.alibaba.cloud.ai.example.deepresearch.model.multiagent.AgentType;
import com.alibaba.cloud.ai.example.deepresearch.util.ResourceUtil;

import static com.alibaba.cloud.ai.example.deepresearch.util.ResourceUtil.loadFileContent;

/**
 * Agent Prompt Template Utility Class
 * Loads markdown files from classpath as prompt templates
 *
 * @author Makoto
 * @since 2025/07/17
 */
public class AgentPromptTemplateUtil {

	private static final String CLASSIFIER_PROMPT_PATH = "prompts/multiagent/classifier.md";

	private static final String SEARCH_PLATFORM_SELECTOR_PROMPT_PATH = "prompts/multiagent/search-platform-selector.md";

	public static String getSystemPrompt(AgentType agentType) {
		return ResourceUtil.loadFileContent(agentType.getPromptFilePath());
	}

	/**
	 * Retrieves the system prompt for question classification
	 * @return Classification system prompt
	 * @throws RuntimeException When file loading fails
	 */
	public static String getClassificationPrompt() {
		return loadFileContent(CLASSIFIER_PROMPT_PATH);
	}

	/**
	 * Retrieves the system prompt for search platform selection
	 * @return Search platform selection system prompt
	 * @throws RuntimeException When file loading fails
	 */
	public static String getSearchPlatformSelectionPrompt() {
		return loadFileContent(SEARCH_PLATFORM_SELECTOR_PROMPT_PATH);
	}

	/**
	 * Constructs a complete agent prompt (system prompt + citation guidance)
	 * @param agentType Agent type
	 * @return Complete prompt
	 * @throws RuntimeException When the agent type is unsupported or file loading fails
	 */
	public static String buildCompletePrompt(AgentType agentType) {
		return getSystemPrompt(agentType) + "\n\n" + agentType.getCitationGuidance();
	}

}
