/*
 * Copyright 2023-2024 the original author or authors.
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

package com.alibaba.cloud.ai.example.dotprompt.service;

import com.alibaba.cloud.ai.dotprompt.DotPromptTemplate;
import com.alibaba.cloud.ai.example.dotprompt.model.TextRequest;
import com.alibaba.cloud.ai.example.dotprompt.model.TextResponse;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class TextProcessingService {

	private final DotPromptTemplate promptTemplate;

	private final ChatClient chatClient;

	public TextProcessingService(DotPromptTemplate promptTemplate, ChatClient chatClient) {
		this.promptTemplate = promptTemplate;
		this.chatClient = chatClient;
	}

	public TextResponse translate(TextRequest request) {
		Map<String, Object> variables = new HashMap<>();
		variables.put("text", request.getText());
		variables.put("to", request.getTo());
		if (request.getFrom() != null) {
			variables.put("from", request.getFrom());
		}

		return processText("translate", variables, request);
	}

	public TextResponse summarize(TextRequest request) {
		Map<String, Object> variables = new HashMap<>();
		variables.put("text", request.getText());
		if (request.getLength() != null) {
			variables.put("length", request.getLength());
		}
		if (request.getFormat() != null) {
			variables.put("format", request.getFormat());
		}

		return processText("summarize", variables, request);
	}

	public TextResponse analyze(TextRequest request) {
		Map<String, Object> variables = new HashMap<>();
		variables.put("text", request.getText());
		if (request.getAspects() != null && !request.getAspects().isEmpty()) {
			variables.put("aspects", request.getAspects());
		}

		return processText("analyze", variables, request);
	}

	private TextResponse processText(String promptName, Map<String, Object> variables, TextRequest request) {
		DotPromptTemplate template = promptTemplate.withPrompt(promptName);

		// Apply model and config overrides if present
		if (request.getModel() != null) {
			template.withModel(request.getModel());
		}
		if (request.getModelConfig() != null) {
			template.withConfig(request.getModelConfig());
		}

		Prompt prompt = template.create(variables);

		AssistantMessage response = chatClient.prompt(prompt).call().chatResponse().getResult().getOutput();

		return TextResponse.of(response.getContent(), template.getModel(),
				prompt.getInstructions().get(0).getContent());
	}

}
