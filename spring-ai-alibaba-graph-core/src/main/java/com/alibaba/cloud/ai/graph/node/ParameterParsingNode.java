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
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 从自然语言中提取参数。返回三部分：{@code is_success}、{@code data}、{@code reason}
 */
public class ParameterParsingNode implements NodeAction {

	private static final String PARAMETER_PARSING_PROMPT_TEMPLATE = """
			### Role
			You are a JSON-based structured data extractor. Your task is to extract parameter values from user input and return a structured response.

			### Task
			Given user input and a list of expected parameters (with names, types, and descriptions; type can be "String", "Number", "Boolean", or "List"), return a JSON object with the following structure:
			\\{
			  "data": \\{ ... \\},  // extracted parameter values
			  "is_success": boolean,  // true if extraction successful
			  "reason": string  // set to 'success' when successful, explain the reason for failure
			\\}

			### Input
			Text: {inputText}

			### Parameters:
			{parameters}

			### Output Constraints
			- ALWAYS return a valid JSON object with exactly three keys: "data", "is_success", and "reason"
			- For successful extraction:
			  - "is_success": true
			  - "data": contains all defined parameters with extracted values (null for missing values)
			  - "reason": "success"
			- For unsuccessful extraction (e.g., input is empty, completely irrelevant, or missing all required parameters):
			  - "is_success": false
			  - "data": null
			  - "reason": brief explanation of failure (e.g., "input is empty", "no relevant parameters found")
			- DO NOT include any explanation, markdown, or preamble
			- Output must be directly parsable as JSON
			- Ensure the JSON is properly formatted and escaped
			""";

	private static final String PARAMETER_PARSING_USER_PROMPT_1 = """
			{ "input_text": "[Instruction: Please help me check the paper] paper number: 2405.10739 .",
			"Parameters":{"name": "paper_num", "type": "String", "description": "paper number"}}
			""";

	private static final String PARAMETER_PARSING_ASSISTANT_PROMPT_1 = """
			{"is_success": true, "data": {"paper_num": "2405.10739"}, "reason": "success"}

			""";

	private static final String PARAMETER_PARSING_USER_PROMPT_2 = """
			{ "input_text": null,
			"Parameters":{"name": "array_of_story_outlines", "type": "List","description": "the story outlines"}}
			""";

	private static final String PARAMETER_PARSING_ASSISTANT_PROMPT_2 = """
			{"is_success": false, "data": null, "reason": "input_text is null."}

			""";

	private static final SystemPromptTemplate SYSTEM_PROMPT_TEMPLATE = new SystemPromptTemplate(
			PARAMETER_PARSING_PROMPT_TEMPLATE);

	private static final Pattern VAR_TEMPLATE_PATTERN = Pattern.compile("\\{(\\w+)}");

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	private final ChatClient chatClient;

	private final String inputText;

	private final String inputTextKey;

	private final List<Param> parameters;

	private final String successKey;

	private final String dataKey;

	private final String reasonKey;

	// 由用户提供的一个指令，可以有{}占位符号
	private final String instruction;

	public ParameterParsingNode(ChatClient chatClient, String inputText, String inputTextKey, List<Param> parameters,
			String instruction, String successKey, String dataKey, String reasonKey) {
		if (chatClient == null || !StringUtils.hasText(successKey) || !StringUtils.hasText(dataKey)
				|| !StringUtils.hasText(reasonKey)) {
			throw new IllegalArgumentException("There are some empty fields");
		}
		this.chatClient = chatClient;
		this.inputText = inputText;
		this.inputTextKey = inputTextKey;
		this.parameters = parameters;
		this.instruction = instruction;
		this.successKey = successKey;
		this.dataKey = dataKey;
		this.reasonKey = reasonKey;
	}

	private String renderTemplate(OverAllState state, String template) {
		Map<String, Object> params = Stream.of(template)
			.map(VAR_TEMPLATE_PATTERN::matcher)
			.map(Matcher::results)
			.map(results -> results.collect(Collectors.toUnmodifiableMap(r -> r.group(1),
					r -> state.value(r.group(1)).orElse(""), (a, b) -> b)))
			.findFirst()
			.orElseThrow();
		return new PromptTemplate(template).render(params);
	}

	@Override
	public Map<String, Object> apply(OverAllState state) throws Exception {
		try {
			String currentInputText = this.inputText;
			if (StringUtils.hasText(inputTextKey)) {
				currentInputText = (String) state.value(inputTextKey).orElse(currentInputText);
			}
			if (!StringUtils.hasText(currentInputText)) {
				throw new IllegalArgumentException("inputText is empty.");
			}

			Map<String, Object> promptInput = new HashMap<>();
			promptInput.put("inputText",
					String.format("[Instruction: %s] %s", renderTemplate(state, instruction), currentInputText));
			promptInput.put("parameters", OBJECT_MAPPER.writeValueAsString(parameters));

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
				.system(SYSTEM_PROMPT_TEMPLATE.render(promptInput))
				.user(currentInputText)
				.messages(messages)
				.call()
				.chatResponse();

			String rawJson = Optional.ofNullable(response)
				.orElseThrow(() -> new RuntimeException("chat response is null"))
				.getResult()
				.getOutput()
				.getText();
			// 去掉Markdown标记
			if (rawJson != null) {
				rawJson = rawJson.replace("```json", "").replace("```", "").trim();
			}

			Map<String, Object> result = new HashMap<>();
			Response responseJson;
			try {
				responseJson = OBJECT_MAPPER.readValue(rawJson, Response.class);
			}
			catch (JsonProcessingException e) {
				throw new RuntimeException("ChatClient successfully returned, but the returned json is invalid.");
			}

			if (responseJson.isSuccess()) {
				if (responseJson.data() == null) {
					throw new RuntimeException("ChatClient successfully returned, but the returned data is invalid.");
				}
				result.put(successKey, true);
				result.put(dataKey, responseJson.data());
				result.put(reasonKey, "success");
			}
			else {
				result.put(successKey, false);
				result.put(reasonKey, Optional.ofNullable(responseJson.reason()).orElse("reason is empty"));
			}

			return result;
		}
		catch (Exception e) {
			return Map.of(successKey, false, reasonKey, e.getMessage());
		}
	}

	public record Param(String name, String type, String description) {

	}

	private record Response(@JsonProperty("is_success") boolean isSuccess, Map<String, Object> data, String reason) {

	}

	public static Param param(String name, String type, String description) {
		return new Param(name, type, description);
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {

		private String inputText = "";

		private String inputTextKey;

		private ChatClient chatClient;

		private List<Param> parameters;

		private String successKey = "is_success";

		private String dataKey = "data";

		private String reasonKey = "reason";

		private String instruction = "";

		public Builder inputText(String inputText) {
			this.inputText = inputText;
			return this;
		}

		public Builder inputTextKey(String input) {
			this.inputTextKey = input;
			return this;
		}

		public Builder chatClient(ChatClient chatClient) {
			this.chatClient = chatClient;
			return this;
		}

		public Builder parameters(List<Param> parameters) {
			this.parameters = parameters;
			return this;
		}

		public Builder successKey(String successKey) {
			this.successKey = successKey;
			return this;
		}

		public Builder dataKey(String dataKey) {
			this.dataKey = dataKey;
			return this;
		}

		public Builder reasonKey(String reasonKey) {
			this.reasonKey = reasonKey;
			return this;
		}

		public Builder instruction(String instruction) {
			this.instruction = instruction;
			return this;
		}

		public ParameterParsingNode build() {
			return new ParameterParsingNode(chatClient, inputText, inputTextKey, parameters, instruction, successKey,
					dataKey, reasonKey);
		}

	}

}
