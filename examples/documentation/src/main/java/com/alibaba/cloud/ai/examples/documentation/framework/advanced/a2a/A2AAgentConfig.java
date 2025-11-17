/*
 * Copyright 2024-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alibaba.cloud.ai.examples.documentation.framework.advanced.a2a;

import com.alibaba.cloud.ai.graph.agent.ReactAgent;

import org.springframework.ai.chat.model.ChatModel;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 定义并暴露本地 ReactAgent
 */
@Configuration
public class A2AAgentConfig {

	@Bean(name = "dataAnalysisAgent")
	public ReactAgent dataAnalysisAgent(@Qualifier("dashscopeChatModel") ChatModel chatModel) {
		return ReactAgent.builder()
				.name("data_analysis_agent")
				.model(chatModel)
				.description("专门用于数据分析和统计计算的本地智能体")
				.instruction("你是一个专业的数据分析专家，擅长处理各类数据统计和分析任务。" +
						"你能够理解用户的数据分析需求，提供准确的统计计算结果和专业的分析建议。")
				.outputKey("messages")
				.build();
	}
}
