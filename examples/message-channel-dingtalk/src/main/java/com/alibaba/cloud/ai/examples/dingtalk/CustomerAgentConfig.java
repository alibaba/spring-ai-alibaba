/*
 * Copyright 2025-2026 the original author or authors.
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

package com.alibaba.cloud.ai.examples.dingtalk;

import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CustomerAgentConfig {

	private static final String INSTRUCTION = """
			你是一名公司客服助手，名字叫小阿。
			用户会通过钉钉跟你提问，请用简洁、礼貌的中文回答。
			遇到不确定的问题就直说不知道，不要编造。
			""";

	@Bean
	public MemorySaver memorySaver() {
		return new MemorySaver();
	}

	/**
	 * Bean name {@code customerAgent} matches
	 * {@code spring.ai.alibaba.message-channel.channels.dingtalk.bind-agent} in
	 * application.yml — the message-channel starter looks the Agent up by that name.
	 */
	@Bean
	public ReactAgent customerAgent(ChatModel chatModel, MemorySaver memorySaver) {
		return ReactAgent.builder()
				.name("customerAgent")
				.model(chatModel)
				.instruction(INSTRUCTION)
				.saver(memorySaver)
				.enableLogging(true)
				.build();
	}

}
