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

package com.alibaba.cloud.ai.example.deepresearch.service.multiagent;

import com.alibaba.cloud.ai.example.deepresearch.config.SmartAgentProperties;
import com.alibaba.cloud.ai.example.deepresearch.model.multiagent.AgentType;
import com.alibaba.cloud.ai.example.deepresearch.util.multiagent.SmartAgentUtil;
import com.alibaba.cloud.ai.example.deepresearch.util.multiagent.AgentPromptTemplateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Question Classifier Service: Determines the appropriate type of agent based on the content of the user's question.
 * Refactored to utilize the SmartAgentUtil and AgentPromptTemplateUtil utility classes.
 *
 * @author Makoto
 * @since 2025/07/17
 */
@Service
@ConditionalOnProperty(prefix = SmartAgentProperties.PREFIX, name = "enabled", havingValue = "true",
		matchIfMissing = false)
public class QuestionClassifierService {

	private static final Logger logger = LoggerFactory.getLogger(QuestionClassifierService.class);

	private final ChatClient classifierClient;

	public QuestionClassifierService(DashScopeChatModel chatModel) {
		this.classifierClient = ChatClient.builder(chatModel)
			.defaultSystem(AgentPromptTemplateUtil.getClassificationPrompt())
			.build();
	}

	/**
	 * Classify user questions and return the corresponding agent type
	 */
	public AgentType classifyQuestion(String question) {
		if (question == null || question.trim().isEmpty()) {
			return AgentType.GENERAL_RESEARCH;
		}

		// Directly use the AI model for question classification decisions
		String aiClassification = classifierClient.prompt()
			.user("请分析以下问题并返回最适合的Agent类型代码：\n\n" + question)
			.call()
			.content();

		AgentType result = SmartAgentUtil.parseAiClassification(aiClassification);
		logger.info("AI classification result for question '{}': {}", question, result);
		return result;
	}

}
