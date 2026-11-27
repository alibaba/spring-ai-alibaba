/*
 * Copyright 2024-2026 the original author or authors.
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
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.decision.DecisionModel;
import org.springframework.ai.decision.DecisionPrompt;
import org.springframework.ai.decision.question.ChoiceQuestion;
import org.springframework.util.Assert;
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

	private static final Pattern VAR_TEMPLATE_PATTERN = Pattern.compile("\\{(\\w+)}");

	private final SystemPromptTemplate systemPromptTemplate;

	private final ChatClient chatClient;

	private final DecisionModel decisionModel;

	// Categories may contain {} placeholder variables that need to be processed
	private final Map<String, String> categories;

	// Classification instructions may contain {} placeholder variables that need to be processed
	private final List<String> classificationInstructions;

	private final String inputTextKey;

	private final String outputKey;

	public QuestionClassifierNode(ChatClient chatClient, String inputTextKey, Map<String, String> categories,
			List<String> classificationInstructions, String outputKey) {
		this(chatClient, null, inputTextKey, categories, classificationInstructions, outputKey);
	}

	private QuestionClassifierNode(ChatClient chatClient, DecisionModel decisionModel, String inputTextKey,
			Map<String, String> categories, List<String> classificationInstructions, String outputKey) {
		this.chatClient = chatClient;
		this.decisionModel = decisionModel;
		this.inputTextKey = inputTextKey;
		this.categories = categories;
		this.classificationInstructions = classificationInstructions == null ? List.of() : List.copyOf(classificationInstructions);
		this.systemPromptTemplate = new SystemPromptTemplate(CLASSIFIER_PROMPT_TEMPLATE);
		this.outputKey = outputKey;
	}

	private String renderTemplate(OverAllState state, String template) {
		if (!StringUtils.hasText(template)) {
			return template;
		}
		Map<String, Object> params = Stream.of(template)
			.map(VAR_TEMPLATE_PATTERN::matcher)
			.map(Matcher::results)
			.map(results -> results.collect(Collectors.toUnmodifiableMap(r -> r.group(1),
					r -> state.value(r.group(1)).orElse(""), (a, b) -> b)))
			.findFirst()
			.orElseThrow();
		return new PromptTemplate(template).render(params);
	}

	private List<String> renderTemplates(OverAllState state, List<String> templates) {
		return templates.stream().map(template -> renderTemplate(state, template)).toList();
	}

	@Override
	public Map<String, Object> apply(OverAllState state) throws Exception {
		String inputText = state.<String>value(inputTextKey)
				.orElseThrow(() -> new IllegalArgumentException("Missing classifier input: " + inputTextKey));
		Map<String, String> renderedCategories = categories.entrySet().stream()
				.collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, e -> renderTemplate(state, e.getValue())));
		List<String> renderedInstructions = renderTemplates(state, classificationInstructions);
		if (decisionModel != null) {
			return classifyWithDecisionModel(state, inputText, renderedCategories, renderedInstructions);
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

		List<String> categoriesList = renderedCategories.values().stream().toList();

		ChatResponse response = chatClient.prompt()
			.system(systemPromptTemplate.render(Map.of("inputText", inputText, "categories", categoriesList,
					"classificationInstructions", renderedInstructions)))
			.user(inputText)
			.messages(messages)
			.call()
			.chatResponse();

		Map<String, Object> updatedState = new HashMap<>();
		String output = Optional
			.ofNullable(Optional.ofNullable(response)
				.orElseThrow(() -> new RuntimeException("chat response is null"))
				.getResult()
				.getOutput()
				.getText())
			.orElseThrow(() -> new RuntimeException("chat response text is null"));
		String result = renderedCategories.entrySet()
			.stream()
			.filter(entry -> output.contains(entry.getValue()))
			.map(Map.Entry::getKey)
			.findFirst()
			.orElseThrow(() -> new RuntimeException(
					"chatClient returns [" + output + "], but it does not belong to the given category."));

		updatedState.put(outputKey, result);
		if (state.value("messages").isPresent()) {
			updatedState.put("messages", response.getResult().getOutput());
		}

		return updatedState;
	}

	private Map<String, Object> classifyWithDecisionModel(OverAllState state, String inputText,
			Map<String, String> categories, List<String> instructions) {
		String questionName = "category";
		String questionText = String.join("\n", instructions);
		var question = ChoiceQuestion.builder().text("Which category best matches the input?\n" + questionText);
		categories.forEach(question::option);
		var prompt = DecisionPrompt.builder().text(inputText).question(questionName, question.build()).build();
		var response = decisionModel.call(prompt);
		if (response == null || !response.hasAnswer(questionName)) {
			throw new IllegalStateException("Decision model returned no classification answer");
		}

		String selected = response.getChoice(questionName).getValue();
		if (!categories.containsKey(selected)) {
			throw new IllegalStateException("Decision model returned an unknown category ID: " + selected);
		}
		if (state.value("messages").isPresent()) {
			return Map.of(outputKey, selected, "messages", new AssistantMessage(categories.get(selected)));
		}
		return Map.of(outputKey, selected);
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {

		private String inputTextKey;

		private ChatClient chatClient;

		private DecisionModel decisionModel;

		private Map<String, String> categories;

		private List<String> classificationInstructions;

		private String outputKey;

		public Builder inputTextKey(String input) {
			this.inputTextKey = input;
			return this;
		}

		public Builder chatClient(ChatClient chatClient) {
			this.chatClient = chatClient;
			return this;
		}

		/**
		 * Selects a category through Spring AI's typed choice API. Provide at least two
		 * categories and use their stable graph IDs as the option names.
		 */
		public Builder decisionModel(DecisionModel decisionModel) {
			this.decisionModel = decisionModel;
			return this;
		}

		/**
		 * Requires a Map object where key is category ID and value is the specific category name.
		 * Category names can contain variable placeholders like {@code {var_name}}, which will be replaced with values from {@code OverAllState}.
		 */
		public Builder categories(Map<String, String> categories) {
			this.categories = categories;
			return this;
		}

		// Compatible with old version
		@Deprecated
		public Builder categories(List<String> categories) {
			this.categories = categories.stream().collect(Collectors.toMap(k -> k, k -> k, (a, b) -> b));
			return this;
		}

		/**
		 * List of classification instructions. Instructions can contain variable placeholders like {@code {var_name}}, which will be replaced with values from {@code OverAllState}.
		 */
		public Builder classificationInstructions(List<String> classificationInstructions) {
			this.classificationInstructions = classificationInstructions;
			return this;
		}

		public Builder outputKey(String outputKey) {
			this.outputKey = outputKey;
			return this;
		}

		public QuestionClassifierNode build() {
			Assert.isTrue((chatClient == null) != (decisionModel == null),
					"Provide exactly one classifier model: ChatClient or DecisionModel");
			Assert.isTrue(decisionModel == null || (categories != null && categories.size() >= 2),
					"DecisionModel classification needs at least two categories");
			return new QuestionClassifierNode(chatClient, decisionModel, inputTextKey, categories,
					classificationInstructions, outputKey);
		}

	}

}
