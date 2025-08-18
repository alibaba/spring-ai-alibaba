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
 * Agent提示词模板工具类 从classpath加载markdown文件作为提示词模板
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
	 * 获取问题分类的系统提示词
	 * @return 分类系统提示词
	 * @throws RuntimeException 当文件加载失败时抛出异常
	 */
	public static String getClassificationPrompt() {
		return loadFileContent(CLASSIFIER_PROMPT_PATH);
	}

	/**
	 * 获取搜索平台选择的系统提示词
	 * @return 搜索平台选择系统提示词
	 * @throws RuntimeException 当文件加载失败时抛出异常
	 */
	public static String getSearchPlatformSelectionPrompt() {
		return loadFileContent(SEARCH_PLATFORM_SELECTOR_PROMPT_PATH);
	}

	/**
	 * 构建完整的Agent提示词（系统提示词 + 引用指导）
	 * @param agentType Agent类型
	 * @return 完整的提示词
	 * @throws RuntimeException 当Agent类型不支持或文件加载失败时抛出异常
	 */
	public static String buildCompletePrompt(AgentType agentType) {
		return getSystemPrompt(agentType) + "\n\n" + agentType.getCitationGuidance();
	}

}
