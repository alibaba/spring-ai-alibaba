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

/**
 * Agent提示词模板工具类 从classpath加载markdown文件作为提示词模板
 *
 * @author Makoto
 * @since 2025/07/17
 */
public class AgentPromptTemplateUtil {

	private static final String CLASSIFIER_PROMPT_PATH = "prompts/multiagent/classifier.md";

	/**
	 * 获取指定Agent类型的系统提示词
	 * @param agentType Agent类型
	 * @return 系统提示词
	 * @throws RuntimeException 当文件加载失败时抛出异常
	 */
	public static String getSystemPrompt(AgentType agentType) {
		return ResourceUtil.loadFileContent(agentType.getPromptFilePath());
	}

	/**
	 * 获取问题分类的系统提示词
	 * @return 分类系统提示词
	 * @throws RuntimeException 当文件加载失败时抛出异常
	 */
	public static String getClassificationPrompt() {
		return ResourceUtil.loadFileContent(CLASSIFIER_PROMPT_PATH);
	}

	public static String getCitationGuidance(AgentType agentType) {
		return agentType.getCitationGuidance();
	}

	/**
	 * 构建完整的Agent提示词（系统提示词 + 引用指导）
	 * @param agentType Agent类型
	 * @return 完整的提示词
	 * @throws RuntimeException 当Agent类型不支持或文件加载失败时抛出异常
	 */
	public static String buildCompletePrompt(AgentType agentType) {
		return getSystemPrompt(agentType) + "\n\n" + getCitationGuidance(agentType);
	}

}
