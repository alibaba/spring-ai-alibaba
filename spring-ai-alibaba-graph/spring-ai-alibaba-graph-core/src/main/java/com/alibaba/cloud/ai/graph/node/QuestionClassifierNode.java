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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.lang.model.type.ArrayType;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.util.StringUtils;

public class QuestionClassifierNode implements NodeAction {

	private static final String CLASSIFIER_PROMPT_TEMPLATE = """
				### Job Description',
				You are a text classification engine that analyzes text data and assigns categories based on user input or automatically determined categories.
				### Task
				Your task is to assign one category ONLY to the input text and only one category can be  returned in the output. Additionally, you need to extract the key words from the text that are related to the classification.
				### Format
				The input text is: {inputText}. Categories are specified as a category list: {categories}. Classification instructions may be included to improve the classification accuracy: {classificationInstructions}.
				### Constraint
				DO NOT include anything other than the JSON array in your response.
			""";

	private static final String QUESTION_CLASSIFIER_USER_PROMPT_1 = """
				{ "input_text": ["I recently had a great experience with your company. The service was prompt and the staff was very friendly."],
				"categories": ["Customer Service", "Satisfaction", "Sales", "Product"],
				"classification_instructions": ["classify the text based on the feedback provided by customer"]}
			""";

	private static final String QUESTION_CLASSIFIER_ASSISTANT_PROMPT_1 = """
				```json
					{"keywords": ["recently", "great experience", "company", "service", "prompt", "staff", "friendly"]
					"category_name": "Customer Service"}
				```
			""";

	private static final String QUESTION_CLASSIFIER_USER_PROMPT_2 = """
				{"input_text": ["bad service, slow to bring the food"],
				"categories": ["Food Quality", "Experience", "Price"],
				"classification_instructions": []}
			""";

	private static final String QUESTION_CLASSIFIER_ASSISTANT_PROMPT_2 = """
				```json
					{"keywords": ["bad service", "slow", "food", "tip", "terrible", "waitresses"],
					"category_name": "Experience"}
				```
			""";

	private SystemPromptTemplate systemPromptTemplate;

	private ChatClient chatClient;

	private String inputText;

	private List<String> categories;

	private List<String> classificationInstructions;

	private String inputTextKey;

	public QuestionClassifierNode(ChatClient chatClient, String inputTextKey, List<String> categories,
			List<String> classificationInstructions) {
		this.chatClient = chatClient;
		this.inputTextKey = inputTextKey;
		this.categories = categories;
		this.classificationInstructions = classificationInstructions;
		this.systemPromptTemplate = new SystemPromptTemplate(CLASSIFIER_PROMPT_TEMPLATE);
	}

	@Override
	public Map<String, Object> apply(OverAllState state) throws Exception {
		if (StringUtils.hasLength(inputTextKey)) {
			this.inputText = (String) state.value(inputTextKey).orElse(this.inputText);
		}

		List<Message> messages = new ArrayList<>();
		UserMessage userMessage1 = new UserMessage(QUESTION_CLASSIFIER_USER_PROMPT_1);
		AssistantMessage assistantMessage1 = new AssistantMessage(QUESTION_CLASSIFIER_ASSISTANT_PROMPT_1);
		UserMessage userMessage2 = new UserMessage(QUESTION_CLASSIFIER_USER_PROMPT_2);
		AssistantMessage assistantMessage2 = new AssistantMessage(QUESTION_CLASSIFIER_ASSISTANT_PROMPT_2);
		messages.add(userMessage1);
		messages.add(assistantMessage1);
		messages.add(userMessage2);
		messages.add(assistantMessage2);

		ChatResponse response = chatClient.prompt()
			.system(systemPromptTemplate.render(Map.of("inputText", inputText, "categories", categories,
					"classificationInstructions", classificationInstructions)))
			.user(inputText)
			.messages(messages)
			.call()
			.chatResponse();

		Map<String, Object> updatedState = new HashMap<>();
		updatedState.put("classifier_output", response.getResult().getOutput().getText());
		if (state.value("messages").isPresent()) {
			updatedState.put("messages", response.getResult().getOutput());
		}

		return updatedState;
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {

		private String inputTextKey;

		private ChatClient chatClient;

		private List<String> categories;

		private List<String> classificationInstructions;

		public Builder inputTextKey(String input) {
			this.inputTextKey = input;
			return this;
		}

		public Builder chatClient(ChatClient chatClient) {
			this.chatClient = chatClient;
			return this;
		}

		public Builder categories(List<String> categories) {
			this.categories = categories;
			return this;
		}

		public Builder classificationInstructions(List<String> classificationInstructions) {
			this.classificationInstructions = classificationInstructions;
			return this;
		}

		public QuestionClassifierNode build() {
			return new QuestionClassifierNode(chatClient, inputTextKey, categories, classificationInstructions);
		}

	}

}
