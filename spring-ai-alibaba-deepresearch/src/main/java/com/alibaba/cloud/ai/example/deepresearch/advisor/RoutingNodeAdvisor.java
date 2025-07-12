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

package com.alibaba.cloud.ai.example.deepresearch.advisor;

import com.alibaba.cloud.ai.example.deepresearch.model.NodeDefinition;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.util.Assert;

import java.util.Map;

/**
 * Intelligent content routing based on LLM’s understanding capabilities
 *
 * @author ViliamSun
 * @since 1.0.0
 */
public class RoutingNodeAdvisor implements BaseAdvisor {

	private final PromptTemplate promptTemplate;

	private final Iterable<String> selections;

	private final ObjectMapper objectMapper;

	private static final String SELECTION = "selection";

	private RoutingNodeAdvisor(Iterable<String> selections, ObjectMapper objectMapper) {
		this.promptTemplate = new PromptTemplate(
				"""
						    Parse the input and select the most appropriate support node from the following options: {selection} First explain your rationale,
						    then provide your selection in JSON format:
						    \\{
						     "reasoning": "Briefly explain why this ticket should be routed to a specific node. Consider key terms, user intent, and urgency level. ",
						     "selection": "Selected node name"
						    \\}
						    .input: {input}
						""");
		Assert.notNull(selections, "Selections must not be null");
		this.selections = selections;
		this.objectMapper = objectMapper;
	}

	@Override
	public ChatClientRequest before(ChatClientRequest chatClientRequest, AdvisorChain advisorChain) {
		var userMessage = chatClientRequest.prompt().getUserMessage();

		String input = promptTemplate.render(Map.of(SELECTION, selections, "input", userMessage.getText()));

		return chatClientRequest.mutate()
			.prompt(chatClientRequest.prompt().augmentUserMessage(input))
			.context(chatClientRequest.context())
			.build();
	}

	@Override
	public ChatClientResponse after(ChatClientResponse chatClientResponse, AdvisorChain advisorChain) {
		ChatResponse.Builder chatResponseBuilder;
		if (chatClientResponse.chatResponse() == null) {
			chatResponseBuilder = ChatResponse.builder();
		}
		else {
			String chatResponse = StringUtils.EMPTY;
			Generation result = chatClientResponse.chatResponse().getResult();
			if (result != null) {
				chatResponse = result.getOutput().getText();
			}
			// Parse response results
			try {
				var selectionNode = objectMapper.readValue(chatResponse,
						new TypeReference<NodeDefinition.SelectionNode>() {
						});
				chatResponseBuilder = ChatResponse.builder()
					.from(chatClientResponse.chatResponse())
					.metadata(ChatResponseMetadata.builder().metadata(Map.of("routerNode", selectionNode)).build());
			}
			catch (JsonProcessingException e) {
				// If parsing fails, log the error and return an empty chat response
				chatResponseBuilder = ChatResponse.builder();
			}
		}

		return ChatClientResponse.builder()
			.chatResponse(chatResponseBuilder.build())
			.context(chatClientResponse.context())
			.build();
	}

	@Override
	public int getOrder() {
		return 0;
	}

	public static Builder Builder() {
		return new Builder();
	}

	public static class Builder {

		private Iterable<String> selections;

		;

		private ObjectMapper objectMapper;

		public Builder selections(Iterable<String> selections) {
			this.selections = selections;
			return this;
		}

		public Builder JSONParser(ObjectMapper objectMapper) {
			this.objectMapper = objectMapper;
			return this;
		}

		public RoutingNodeAdvisor build() {
			return new RoutingNodeAdvisor(selections, objectMapper);
		}

	}

}
