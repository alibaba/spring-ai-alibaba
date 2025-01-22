/*
 * Copyright 2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.cloud.ai.graph;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.model.function.FunctionCallback;

public class IsAgentService {

	public final ToolService toolService;

	private final ChatClient chatClient;

	public IsAgentService(ChatClient.Builder chatClientBuilder, ToolService toolService) {
		var functions = toolService.agentFunctionsCallback().toArray(FunctionCallback[]::new);

		this.chatClient = chatClientBuilder.defaultSystem("You are a helpful AI Assistant answering questions.")
			.defaultFunctions(functions)
			.build();
		this.toolService = toolService;
	}

	public ChatResponse execute(String input) {
		return chatClient.prompt().user(input).call().chatResponse();
	}

	public ChatResponse executeByPrompt(String input, String prompt) {
		return chatClient.prompt(prompt).user(input).call().chatResponse();
	}

}
