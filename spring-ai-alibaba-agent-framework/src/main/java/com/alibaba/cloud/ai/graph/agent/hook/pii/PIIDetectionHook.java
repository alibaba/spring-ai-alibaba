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
package com.alibaba.cloud.ai.graph.agent.hook.pii;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.hook.HookPosition;
import com.alibaba.cloud.ai.graph.agent.hook.HookPositions;
import com.alibaba.cloud.ai.graph.agent.hook.JumpTo;
import com.alibaba.cloud.ai.graph.agent.hook.ModelHook;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Detect and handle Personally Identifiable Information (PII) in agent conversations.
 *
 * This hook detects common PII types and applies configurable strategies
 * to handle them. It can detect emails, credit cards, IP addresses,
 * MAC addresses, and URLs in both user input and agent output.
 *
 * Example:
 * PIIDetectionHook pii = PIIDetectionHook.builder()
 *     .piiType(PIIType.EMAIL)
 *     .strategy(RedactionStrategy.REDACT)
 *     .applyToInput(true)
 *     .build();
 */
@HookPositions({HookPosition.BEFORE_MODEL, HookPosition.AFTER_MODEL})
public class PIIDetectionHook extends ModelHook {

	private final PIIType piiType;
	private final RedactionStrategy strategy;
	private final PIIDetector detector;
	private final boolean applyToInput;
	private final boolean applyToOutput;
	private final boolean applyToToolResults;

	private PIIDetectionHook(Builder builder) {
		this.piiType = builder.piiType;
		this.strategy = builder.strategy;
		this.detector = builder.detector != null ? builder.detector : getDefaultDetector(piiType);
		this.applyToInput = builder.applyToInput;
		this.applyToOutput = builder.applyToOutput;
		this.applyToToolResults = builder.applyToToolResults;
	}

	public static Builder builder() {
		return new Builder();
	}

	@Override
	public CompletableFuture<Map<String, Object>> beforeModel(OverAllState state, RunnableConfig config) {
		List<Message> messages = (List<Message>) state.value("messages").orElse(List.of());
		List<Message> processedMessages = new ArrayList<>();
		boolean hasChanges = false;

		for (Message message : messages) {
			Message processed = processMessage(message);
			processedMessages.add(processed);
			if (processed != message) {
				hasChanges = true;
			}
		}

		if (hasChanges) {
			Map<String, Object> updates = new HashMap<>();
			updates.put("messages", processedMessages);
			return CompletableFuture.completedFuture(updates);
		}

		return CompletableFuture.completedFuture(Map.of());
	}

	@Override
	public CompletableFuture<Map<String, Object>> afterModel(OverAllState state, RunnableConfig config) {
		// Only process if applyToOutput is enabled
		if (!applyToOutput) {
			return CompletableFuture.completedFuture(Map.of());
		}

		List<Message> messages = (List<Message>) state.value("messages").orElse(List.of());

		if (messages.isEmpty()) {
			return CompletableFuture.completedFuture(Map.of());
		}

		// Find the last AI message
		AssistantMessage aiMessage = null;
		int lastIndex = -1;
		for (int i = messages.size() - 1; i >= 0; i--) {
			if (messages.get(i) instanceof AssistantMessage am) {
				aiMessage = am;
				lastIndex = i;
				break;
			}
		}

		if (aiMessage == null) {
			return CompletableFuture.completedFuture(Map.of());
		}

		String content = aiMessage.getText();

		if (content == null || content.isEmpty()) {
			return CompletableFuture.completedFuture(Map.of());
		}

		// Detect PII
		ProcessResult result = processText(content);

		if (!result.hasMatches) {
			return CompletableFuture.completedFuture(Map.of());
		}

		// Apply strategy
		if (result.hasMatches && strategy == RedactionStrategy.BLOCK) {
			throw new PIIDetectionException(piiType.name(), result.matches);
		}

		if (result.redactedText.equals(content)) {
			return CompletableFuture.completedFuture(Map.of());
		}

		// Create updated message
		AssistantMessage updatedMessage = new AssistantMessage(
			result.redactedText,
			aiMessage.getMetadata(),
			aiMessage.getToolCalls(),
			aiMessage.getMedia()
		);

		List<Message> updatedMessages = new ArrayList<>(messages);
		updatedMessages.set(lastIndex, updatedMessage);

		Map<String, Object> updates = new HashMap<>();
		updates.put("messages", updatedMessages);
		return CompletableFuture.completedFuture(updates);
	}

	private Message processMessage(Message message) {
		if (applyToInput && message instanceof UserMessage) {
			return processContent((UserMessage) message);
		}
		else if (applyToOutput && message instanceof AssistantMessage) {
			return processContent((AssistantMessage) message);
		}
		else if (applyToToolResults && message instanceof ToolResponseMessage) {
			return processToolResponse((ToolResponseMessage) message);
		}
		return message;
	}

	private UserMessage processContent(UserMessage message) {
		String content = message.getText();
		ProcessResult result = processText(content);

		if (result.hasMatches && strategy == RedactionStrategy.BLOCK) {
			throw new PIIDetectionException(piiType.name(), result.matches);
		}

		if (result.redactedText.equals(content)) {
			return message;
		}

		return UserMessage.builder().text(result.redactedText).metadata(message.getMetadata()).build();
	}

	private AssistantMessage processContent(AssistantMessage message) {
		String content = message.getText();
		ProcessResult result = processText(content);

		if (result.hasMatches && strategy == RedactionStrategy.BLOCK) {
			throw new PIIDetectionException(piiType.name(), result.matches);
		}

		if (result.redactedText.equals(content)) {
			return message;
		}

		return new AssistantMessage(result.redactedText, message.getMetadata(),
				message.getToolCalls(), message.getMedia());
	}

	private ToolResponseMessage processToolResponse(ToolResponseMessage message) {
		// Process each tool response
		List<ToolResponseMessage.ToolResponse> responses = new ArrayList<>();
		boolean hasChanges = false;

		for (ToolResponseMessage.ToolResponse response : message.getResponses()) {
			String content = response.responseData();
			ProcessResult result = processText(content);

			if (result.hasMatches && strategy == RedactionStrategy.BLOCK) {
				throw new PIIDetectionException(piiType.name(), result.matches);
			}

			if (!result.redactedText.equals(content)) {
				responses.add(new ToolResponseMessage.ToolResponse(
						response.id(), response.name(), result.redactedText));
				hasChanges = true;
			}
			else {
				responses.add(response);
			}
		}

		return hasChanges ? new ToolResponseMessage(responses, message.getMetadata()) : message;
	}

	private ProcessResult processText(String text) {
		List<PIIMatch> matches = detector.detect(text);

		if (matches.isEmpty()) {
			return new ProcessResult(text, false, matches);
		}

		String redacted = applyStrategy(text, matches);
		return new ProcessResult(redacted, true, matches);
	}

	private String applyStrategy(String text, List<PIIMatch> matches) {
		if (matches.isEmpty()) {
			return text;
		}

		StringBuilder result = new StringBuilder();
		int lastEnd = 0;

		// Sort matches by start position
		matches.sort(Comparator.comparingInt(m -> m.start));

		for (PIIMatch match : matches) {
			result.append(text, lastEnd, match.start);

			switch (strategy) {
			case REDACT:
				result.append("[REDACTED_").append(piiType.name()).append("]");
				break;
			case MASK:
				result.append(maskValue(match.value));
				break;
			case HASH:
				result.append(hashValue(match.value));
				break;
			case BLOCK:
				// Already handled in processText
				break;
			}

			lastEnd = match.end;
		}

		result.append(text.substring(lastEnd));
		return result.toString();
	}

	private String maskValue(String value) {
		if (value.length() <= 4) {
			return "****";
		}
		int visibleChars = 4;
		String masked = "*".repeat(value.length() - visibleChars);
		return masked + value.substring(value.length() - visibleChars);
	}

	private String hashValue(String value) {
		int hash = value.hashCode();
		return String.format("<%s_hash:%08x>", piiType.name().toLowerCase(), hash);
	}

	private PIIDetector getDefaultDetector(PIIType type) {
		switch (type) {
		case EMAIL:
			return PIIDetectors.emailDetector();
		case CREDIT_CARD:
			return PIIDetectors.creditCardDetector();
		case IP:
			return PIIDetectors.ipDetector();
		case MAC_ADDRESS:
			return PIIDetectors.macAddressDetector();
		case URL:
			return PIIDetectors.urlDetector();
		default:
			throw new IllegalArgumentException("No default detector for PII type: " + type);
		}
	}

	@Override
	public String getName() {
		return "PIIDetection[" + piiType.name() + "]";
	}

	@Override
	public List<JumpTo> canJumpTo() {
		return List.of();
	}

	private static class ProcessResult {
		final String redactedText;
		final boolean hasMatches;
		final List<PIIMatch> matches;

		ProcessResult(String redactedText, boolean hasMatches, List<PIIMatch> matches) {
			this.redactedText = redactedText;
			this.hasMatches = hasMatches;
			this.matches = matches;
		}
	}

	public static class Builder {
		private PIIType piiType;
		private RedactionStrategy strategy = RedactionStrategy.REDACT;
		private PIIDetector detector;
		private boolean applyToInput = true;
		private boolean applyToOutput = false;
		private boolean applyToToolResults = false;

		public Builder piiType(PIIType piiType) {
			this.piiType = piiType;
			return this;
		}

		public Builder strategy(RedactionStrategy strategy) {
			this.strategy = strategy;
			return this;
		}

		public Builder detector(PIIDetector detector) {
			this.detector = detector;
			return this;
		}

		public Builder applyToInput(boolean applyToInput) {
			this.applyToInput = applyToInput;
			return this;
		}

		public Builder applyToOutput(boolean applyToOutput) {
			this.applyToOutput = applyToOutput;
			return this;
		}

		public Builder applyToToolResults(boolean applyToToolResults) {
			this.applyToToolResults = applyToToolResults;
			return this;
		}

		public PIIDetectionHook build() {
			if (piiType == null) {
				throw new IllegalArgumentException("piiType must be specified");
			}
			return new PIIDetectionHook(this);
		}
	}
}

