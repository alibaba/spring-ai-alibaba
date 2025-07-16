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
package com.alibaba.cloud.ai.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
public class LlmService {

	private final ChatClient chatClient;

	public LlmService(ChatClient chatClient) {
		this.chatClient = chatClient;
	}

	public String call(String prompt) {
		return chatClient.prompt().user(prompt).call().content();
	}

	public String callWithSystemPrompt(String system, String user) {
		return chatClient.prompt().system(system).user(user).call().content();
	}

	/**
	 * 流式返回用户prompt的回复
	 * @param prompt 用户输入的提示
	 * @return Flux<ChatResponse> 流式响应
	 */
	public Flux<ChatResponse> streamCall(String prompt) {
		return chatClient.prompt().user(prompt).stream().chatResponse();
	}

	/**
	 * 流式返回用户prompt的回复，带有系统提示
	 * @param system 系统提示
	 * @param user 用户输入
	 * @return Flux<ChatResponse> 流式响应
	 */
	public Flux<ChatResponse> streamCallWithSystemPrompt(String system, String user) {
		return chatClient.prompt().system(system).user(user).stream().chatResponse();
	}

}
