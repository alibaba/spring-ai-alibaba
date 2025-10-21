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
import com.alibaba.cloud.ai.graph.agent.hook.AfterModelHook;
import com.alibaba.cloud.ai.graph.agent.hook.JumpTo;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * After-model hook that detects and handles PII in model output (AI messages).
 *
 * This hook processes AI messages after the model generates them, applying
 * the configured PII detection and redaction strategy.
 *
 * Example:
 * <pre>
 * PIIDetectionAfterModelHook hook = PIIDetectionAfterModelHook.builder()
 *     .piiType(PIIType.EMAIL)
 *     .strategy(RedactionStrategy.REDACT)
 *     .build();
 * </pre>
 */
public class PIIDetectionAfterModelHook extends AfterModelHook {

	private final PIIType piiType;
	private final RedactionStrategy strategy;
	private final PIIDetector detector;

	private PIIDetectionAfterModelHook(Builder builder) {
		this.piiType = builder.piiType;
		this.strategy = builder.strategy;
		this.detector = builder.detector != null ? builder.detector : PIIDetectors.getDetector(piiType);
	}

	public static Builder builder() {
		return new Builder();
	}

	@Override
	@SuppressWarnings("unchecked")
	public CompletableFuture<Map<String, Object>> apply(OverAllState state, RunnableConfig config) {
		List<Message> messages = (List<Message>) state.value("messages").orElse(new ArrayList<>());

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
		List<PIIMatch> matches = detector.detect(content);

		if (matches.isEmpty()) {
			return CompletableFuture.completedFuture(Map.of());
		}

		// Apply strategy
		if (strategy == RedactionStrategy.BLOCK) {
			throw new PIIDetectionException(piiType.name(), matches);
		}

		String redactedContent = applyStrategy(content, matches);

		if (redactedContent.equals(content)) {
			return CompletableFuture.completedFuture(Map.of());
		}

		// Create updated message
		AssistantMessage updatedMessage = new AssistantMessage(
			redactedContent,
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
				// Already handled above
				throw new PIIDetectionException(piiType.name(), matches);
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
		// Show last 4 characters
		return "****" + value.substring(value.length() - 4);
	}

	private String hashValue(String value) {
		int hash = value.hashCode();
		return String.format("<%s_hash:%08x>", piiType.name().toLowerCase(), hash);
	}

	@Override
	public String getName() {
		return "PIIDetectionAfter[" + piiType.name() + "]";
	}

	@Override
	public List<JumpTo> canJumpTo() {
		return List.of();
	}

	public static class Builder {
		private PIIType piiType;
		private RedactionStrategy strategy = RedactionStrategy.REDACT;
		private PIIDetector detector;

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

		public PIIDetectionAfterModelHook build() {
			if (piiType == null) {
				throw new IllegalArgumentException("piiType is required");
			}
			return new PIIDetectionAfterModelHook(this);
		}
	}
}
