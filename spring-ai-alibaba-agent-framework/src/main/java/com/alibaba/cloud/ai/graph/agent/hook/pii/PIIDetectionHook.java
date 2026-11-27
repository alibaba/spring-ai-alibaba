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
package com.alibaba.cloud.ai.graph.agent.hook.pii;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.hook.HookPosition;
import com.alibaba.cloud.ai.graph.agent.hook.HookPositions;
import com.alibaba.cloud.ai.graph.agent.hook.JumpTo;
import com.alibaba.cloud.ai.graph.agent.hook.messages.AgentCommand;
import com.alibaba.cloud.ai.graph.agent.hook.messages.MessagesModelHook;
import com.alibaba.cloud.ai.graph.agent.hook.messages.UpdatePolicy;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.messages.part.MessagePart;
import org.springframework.ai.chat.messages.part.TextPart;
import org.springframework.ai.chat.messages.part.ToolResultPart;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

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
public class PIIDetectionHook extends MessagesModelHook {

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
	public List<UnaryOperator<AssistantMessage>> getStreamingOutputFilters() {
		return applyToOutput ? List.of(this::processContent) : List.of();
	}

	@Override
	public AgentCommand beforeModel(List<Message> previousMessages, RunnableConfig config) {
		List<Message> processedMessages = new ArrayList<>();
		boolean hasChanges = false;

		for (Message message : previousMessages) {
			Message processed = processMessage(message);
			processedMessages.add(processed);
			if (processed != message) {
				hasChanges = true;
			}
		}

		if (hasChanges) {
			return new AgentCommand(processedMessages, UpdatePolicy.REPLACE);
		}

		return new AgentCommand(previousMessages);
	}

	@Override
	public AgentCommand afterModel(List<Message> previousMessages, RunnableConfig config) {
		// Only process if applyToOutput is enabled
		if (!applyToOutput) {
			return new AgentCommand(previousMessages);
		}

		if (previousMessages.isEmpty()) {
			return new AgentCommand(previousMessages);
		}

		// Find the last AI message
		AssistantMessage aiMessage = null;
		int lastIndex = -1;
		for (int i = previousMessages.size() - 1; i >= 0; i--) {
			if (previousMessages.get(i) instanceof AssistantMessage am) {
				aiMessage = am;
				lastIndex = i;
				break;
			}
		}

		if (aiMessage == null) {
			return new AgentCommand(previousMessages);
		}

		AssistantMessage updatedMessage = processContent(aiMessage);
		if (updatedMessage == aiMessage) {
			return new AgentCommand(previousMessages);
		}

		List<Message> updatedMessages = new ArrayList<>(previousMessages);
		updatedMessages.set(lastIndex, updatedMessage);
		return new AgentCommand(updatedMessages, UpdatePolicy.REPLACE);
	}

	@Override
	protected Map<String, Object> afterModelStateUpdates(OverAllState state, List<Message> previous,
			List<Message> updated) {
		if (previous == updated || getAgent() == null) {
			return Map.of();
		}
		String key = getAgent().getOutputKey();
		if (key != null && !key.isEmpty() && state.value(key).orElse(null) instanceof AssistantMessage output) {
			int index = previous.lastIndexOf(output);
			if (index >= 0 && updated.get(index) != previous.get(index)) {
				return Map.of(key, updated.get(index));
			}
		}
		return Map.of();
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
		var parts = redactTextParts(message.getParts());
		return parts == message.getParts() ? message : UserMessage.builder().parts(parts)
				.metadata(message.getMetadata()).build();
	}

	private AssistantMessage processContent(AssistantMessage message) {
		var parts = redactTextParts(message.getParts());
		return parts == message.getParts() ? message : AssistantMessage.builder().parts(parts)
				.properties(message.getMetadata()).build();
	}

	private ToolResponseMessage processToolResponse(ToolResponseMessage message) {
		List<MessagePart> parts = new ArrayList<>();
		boolean edited = false;
		for (MessagePart part : redactTextParts(message.getParts())) {
			if (part instanceof ToolResultPart result) {
				var content = redactTextParts(result.content());
				if (content != result.content()) {
					part = new ToolResultPart(result.id(), result.name(), content, result.isError(),
							null, result.attributes());
					edited = true;
				}
			}
			parts.add(part);
		}
		return edited || !parts.equals(message.getParts())
				? ToolResponseMessage.builder().parts(parts).metadata(message.getMetadata()).build() : message;
	}

	private List<MessagePart> redactTextParts(List<MessagePart> parts) {
		String text = parts.stream().filter(TextPart.class::isInstance).map(TextPart.class::cast)
				.map(TextPart::text).collect(Collectors.joining());
		if (text.isEmpty()) {
			return parts;
		}

		List<PIIMatch> matches = detector.detect(text).stream()
				.sorted(Comparator.comparingInt(match -> match.start)).toList();
		if (matches.isEmpty()) {
			return parts;
		}
		if (strategy == RedactionStrategy.BLOCK) {
			throw new PIIDetectionException(piiType.name(), matches);
		}

		// Detect on the joined text so a match split across parts is still redacted.
		// Place its replacement in the part where it began and retain the other parts.
		List<MessagePart> redacted = new ArrayList<>(parts.size());
		int offset = 0;
		int matchIndex = 0;
		for (MessagePart part : parts) {
			if (!(part instanceof TextPart textPart) || textPart.text().isEmpty()) {
				redacted.add(part);
				continue;
			}
			int end = offset + textPart.text().length();
			int cursor = offset;
			StringBuilder replacement = new StringBuilder();
			while (matchIndex < matches.size() && matches.get(matchIndex).start < end) {
				PIIMatch match = matches.get(matchIndex);
				if (match.start >= cursor) {
					replacement.append(text, cursor, match.start).append(replacementFor(match));
				}
				cursor = Math.max(cursor, Math.min(match.end, end));
				if (match.end > end) {
					break;
				}
				matchIndex++;
			}
			replacement.append(text, cursor, end);
			String updatedText = replacement.toString();
			redacted.add(updatedText.equals(textPart.text()) ? part
					: new TextPart(updatedText, null, textPart.attributes()));
			offset = end;
		}
		return redacted.equals(parts) ? parts : redacted;
	}

	private String replacementFor(PIIMatch match) {
		return switch (strategy) {
			case REDACT -> "[REDACTED_" + piiType.name() + "]";
			case MASK -> maskValue(match.value);
			case HASH -> hashValue(match.value);
			case BLOCK -> throw new IllegalStateException("Blocked matches must not be rewritten");
		};
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

