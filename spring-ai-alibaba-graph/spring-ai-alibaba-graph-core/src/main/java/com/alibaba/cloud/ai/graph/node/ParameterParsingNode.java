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
package com.alibaba.cloud.ai.graph.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.util.StringUtils;

public class ParameterParsingNode implements NodeAction {

	private static final String PARAMETER_PARSING_PROMPT_TEMPLATE = """
			### Role
			You are a JSON-based structured data extractor. Your task is to extract parameter values
			from user input.
			### Task
			Given user input and a list of expected parameters (with names, types, and descriptions
			Type can be "string", "number", "boolean", or "array"),
			return a valid JSON object containing those parameter values.
			### Input
			Text: {inputText}
			### Parameters:
			{parameters}
			### Output Constraints
			- Return ONLY a valid JSON object containing all defined keys.
			- Missing values must be set to null.
			- DO NOT include any explanation, markdown, or preamble.
			- Output must be directly parsable as JSON.
			""";

	private static final String PARAMETER_PARSING_USER_PROMPT_1 = """
			{ "input_text":[" Please help me check the paper, paper number: 2405.10739 ."],
			"Parameters":{"name":[paper_num],“type”:[string],"description":["paper number"]}}
			""";

	private static final String PARAMETER_PARSING_ASSISTANT_PROMPT_1 = """
			json
			{"paper_num": "2405.10739"}

			""";

	private static final String PARAMETER_PARSING_USER_PROMPT_2 = """
			{ "input_text":[" Chapter 1: Encounters. The sun shines in the forest, and the young man sees the girl for the first time.
			Chapter 2: The Storm. The village was attacked, and its fate changed dramatically.
			Chapter 3: Departure. They embark on a journey to find the truth."],
			"Parameters":{"name":[array_of_story_outlines],“type”:[array],"description":["the story outlines"]}}
			""";

	private static final String PARAMETER_PARSING_ASSISTANT_PROMPT_2 = """
			json
			{
			  "array_of_story_outlines": [
			    "Chapter 1: Encounters. The sun shines in the forest, and the young man sees the girl for the first time.",
			    "Chapter 2: The Storm. The village was attacked, and its fate changed dramatically.",
			    "Chapter 3: Departure. They embark on a journey to find the truth."
			  ]
			}

			""";

	private ChatClient chatClient;

	private String inputText;

	private final String inputTextKey;

	private List<Map<String, String>> parameters;

	private SystemPromptTemplate systemPromptTemplate;

	private final String outputKey;

	public ParameterParsingNode(ChatClient chatClient, String inputTextKey, List<Map<String, String>> parameters,
			String outputKey) {
		this.chatClient = chatClient;
		this.inputTextKey = inputTextKey;
		this.parameters = parameters;
		this.outputKey = outputKey;
		this.systemPromptTemplate = new SystemPromptTemplate(PARAMETER_PARSING_PROMPT_TEMPLATE);
	}

	@Override
	public Map<String, Object> apply(OverAllState state) throws Exception {

		if (StringUtils.hasLength(inputTextKey)) {
			this.inputText = (String) state.value(inputTextKey).orElse(this.inputText);
		}

		Map<String, Object> promptInput = new HashMap<>();
		promptInput.put("inputText", inputText);
		promptInput.put("parameters", formatParameters(parameters));

		List<Message> messages = new ArrayList<>();
		UserMessage userMessage1 = new UserMessage(PARAMETER_PARSING_USER_PROMPT_1);
		AssistantMessage assistantMessage1 = new AssistantMessage(PARAMETER_PARSING_ASSISTANT_PROMPT_1);
		UserMessage userMessage2 = new UserMessage(PARAMETER_PARSING_USER_PROMPT_2);
		AssistantMessage assistantMessage2 = new AssistantMessage(PARAMETER_PARSING_ASSISTANT_PROMPT_2);
		messages.add(userMessage1);
		messages.add(assistantMessage1);
		messages.add(userMessage2);
		messages.add(assistantMessage2);

		ChatResponse response = chatClient.prompt()
			.system(systemPromptTemplate.render(promptInput))
			.user(inputText)
			.messages(messages)
			.call()
			.chatResponse();

		String rawJson = response.getResult().getOutput().getText();
		ObjectMapper mapper = new ObjectMapper();
		Map<String, Object> updateState = new HashMap<>();
		try {
			updateState.put(outputKey, mapper.readValue(rawJson, new TypeReference<>() {
			}));
		}
		catch (Exception e) {
			throw new RuntimeException("Invalid JSON response from model: " + rawJson, e);
		}
		return updateState;
	}

	private String formatParameters(List<Map<String, String>> parameters) {
		StringBuilder builder = new StringBuilder();
		for (Map<String, String> param : parameters) {
			builder.append("- ")
				.append(param.get("name"))
				.append(" (")
				.append(param.get("type"))
				.append("): ")
				.append(param.get("description"))
				.append("\n");
		}
		return builder.toString();
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {

		private String inputTextKey;

		private ChatClient chatClient;

		private List<Map<String, String>> parameters;

		private String outputKey;

		public Builder inputTextKey(String input) {
			this.inputTextKey = input;
			return this;
		}

		public Builder chatClient(ChatClient chatClient) {
			this.chatClient = chatClient;
			return this;
		}

		public Builder parameters(List<Map<String, String>> parameters) {
			this.parameters = parameters;
			return this;
		}

		public Builder outputKey(String outputKey) {
			this.outputKey = outputKey;
			return this;
		}

		public ParameterParsingNode build() {
			return new ParameterParsingNode(chatClient, inputTextKey, parameters, outputKey);
		}

	}

}
