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
package com.alibaba.cloud.ai.graph.agent.hook.summarization;

import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.hook.messages.AgentCommand;
import com.alibaba.cloud.ai.graph.agent.hook.messages.AppendPolicy;
import com.alibaba.cloud.ai.graph.agent.hook.HookPosition;
import com.alibaba.cloud.ai.graph.agent.hook.HookPositions;
import com.alibaba.cloud.ai.graph.agent.hook.JumpTo;
import com.alibaba.cloud.ai.graph.agent.hook.messages.MessagesModelHook;
import com.alibaba.cloud.ai.graph.agent.hook.TokenCounter;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Hook that summarizes conversation history when token limits are approached.
 *
 * This hook monitors message token counts and automatically summarizes older
 * messages when a threshold is reached, preserving recent messages and maintaining
 * context continuity.
 *
 * Example:
 * SummarizationHook summarizer = SummarizationHook.builder()
 *     .model(chatModel)
 *     .maxTokensBeforeSummary(4000)
 *     .messagesToKeep(20)
 *     .build();
 */
@HookPositions({HookPosition.BEFORE_MODEL})
public class SummarizationHook extends MessagesModelHook {

	private static final Logger log = LoggerFactory.getLogger(SummarizationHook.class);

	private static final String DEFAULT_SUMMARY_PROMPT =
			"<role>\nContext Extraction Assistant\n</role>\n\n" +
					"<primary_objective>\n" +
					"Your sole objective in this task is to extract the highest quality/most relevant context " +
					"from the conversation history below.\n</primary_objective>\n\n" +
					"<instructions>\n" +
					"The conversation history below will be replaced with the context you extract in this step. " +
					"Extract and record all of the most important context from the conversation history.\n" +
					"Respond ONLY with the extracted context. Do not include any additional information.\n" +
					"</instructions>\n\n" +
					"<messages>\nMessages to summarize:\n%s\n</messages>";

	private static final String SUMMARY_PREFIX = "## Previous conversation summary:";
	private static final int DEFAULT_MESSAGES_TO_KEEP = 20;

	private final ChatModel model;
	private final Integer maxTokensBeforeSummary;
	private final int messagesToKeep;
	private final TokenCounter tokenCounter;
	private final String summaryPrompt;
	private final String summaryPrefix;

	private SummarizationHook(Builder builder) {
		this.model = builder.model;
		this.maxTokensBeforeSummary = builder.maxTokensBeforeSummary;
		this.messagesToKeep = builder.messagesToKeep;
		this.tokenCounter = builder.tokenCounter;
		this.summaryPrompt = builder.summaryPrompt;
		this.summaryPrefix = builder.summaryPrefix;
	}

	public static Builder builder() {
		return new Builder();
	}

	@Override
	public AgentCommand beforeModel(List<Message> previousMessages, RunnableConfig config) {
		if (maxTokensBeforeSummary == null) {
			return new AgentCommand(previousMessages);
		}

		int totalTokens = tokenCounter.countTokens(previousMessages);

		if (totalTokens < maxTokensBeforeSummary) {
			return new AgentCommand(previousMessages);
		}

		log.info("Token count {} exceeds threshold {}, triggering summarization",
				totalTokens, maxTokensBeforeSummary);

		int cutoffIndex = findSafeCutoff(previousMessages);

		if (cutoffIndex <= 0) {
			log.warn("Cannot find safe cutoff point for summarization");
			return new AgentCommand(previousMessages);
		}

		List<Message> toSummarize = previousMessages.subList(0, cutoffIndex);
		List<Message> toPreserve = previousMessages.subList(cutoffIndex, previousMessages.size());

		String summary = createSummary(toSummarize);

		List<Message> newMessages = new ArrayList<>();
		newMessages.add(new UserMessage(
				"Here is a summary of the conversation to date:\n\n" + summary));
		// Add preserved messages
		newMessages.addAll(toPreserve);

		log.info("Summarized {} messages, keeping {} recent messages",
				toSummarize.size(), toPreserve.size());

		return new AgentCommand(newMessages, AppendPolicy.REPLACE);
	}

	private int findSafeCutoff(List<Message> messages) {
		// Find a safe cutoff point, preserving recent messages
		int targetCutoff = Math.max(0, messages.size() - messagesToKeep);

		// Ensure we don't split AI/Tool message pairs
		for (int i = targetCutoff; i < messages.size(); i++) {
			Message msg = messages.get(i);
			if (msg instanceof UserMessage) {
				return i;
			}
		}

		return targetCutoff;
	}

	private String createSummary(List<Message> messages) {
		if (messages.isEmpty()) {
			return "No previous conversation.";
		}

		StringBuilder messageText = new StringBuilder();
		for (Message msg : messages) {
			String role = getRoleName(msg);
			messageText.append(role).append(": ").append(msg.getText()).append("\n");
		}

		String prompt = String.format(summaryPrompt, messageText.toString());

		try {
			Prompt summaryPromptObj = new Prompt(List.of(new UserMessage(prompt)));
			var response = model.call(summaryPromptObj);
			return response.getResult().getOutput().getText();
		}
		catch (Exception e) {
			log.error("Failed to create summary: {}", e.getMessage());
			return "Summary generation failed: " + e.getMessage();
		}
	}

	private String getRoleName(Message message) {
		if (message instanceof UserMessage) {
			return "Human";
		}
		else if (message instanceof AssistantMessage) {
			return "Assistant";
		}
		else if (message instanceof SystemMessage) {
			return "System";
		}
		else if (message instanceof ToolResponseMessage) {
			return "Tool";
		}
		else {
			return "Unknown";
		}
	}

	@Override
	public String getName() {
		return "Summarization";
	}

	@Override
	public List<JumpTo> canJumpTo() {
		return List.of();
	}

	public static class Builder {
		private ChatModel model;
		private Integer maxTokensBeforeSummary;
		private int messagesToKeep = DEFAULT_MESSAGES_TO_KEEP;
		private TokenCounter tokenCounter = TokenCounter.approximateMsgCounter();
		private String summaryPrompt = DEFAULT_SUMMARY_PROMPT;
		private String summaryPrefix = SUMMARY_PREFIX;

		public Builder model(ChatModel model) {
			this.model = model;
			return this;
		}

		public Builder maxTokensBeforeSummary(Integer maxTokens) {
			this.maxTokensBeforeSummary = maxTokens;
			return this;
		}

		public Builder messagesToKeep(int count) {
			this.messagesToKeep = count;
			return this;
		}

		public Builder summaryPrompt(String prompt) {
			this.summaryPrompt = prompt;
			return this;
		}

		public Builder summaryPrefix(String prefix) {
			this.summaryPrefix = prefix;
			return this;
		}

		public Builder tokenCounter(TokenCounter counter) {
			this.tokenCounter = counter;
			return this;
		}

		public SummarizationHook build() {
			if (model == null) {
				throw new IllegalArgumentException("model must be specified");
			}
			return new SummarizationHook(this);
		}
	}
}
