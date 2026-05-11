/*
 * Copyright 2025-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.cloud.ai.agent.agentscope;

import io.agentscope.core.agent.Event;
import io.agentscope.core.agent.EventType;
import io.agentscope.core.message.ContentBlock;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.message.ThinkingBlock;
import io.agentscope.core.message.ToolResultBlock;
import io.agentscope.core.message.ToolUseBlock;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.util.json.JsonParser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility for converting between Spring AI {@link Message} and AgentScope {@link Msg} / {@link Event}.
 * <p>
 * EventType mapping:
 * <ul>
 *   <li>REASONING, AGENT_RESULT → {@link AssistantMessage}</li>
 *   <li>TOOL_RESULT → {@link ToolResponseMessage}</li>
 * </ul>
 * <p>
 * Content block mapping for AssistantMessage:
 * <ul>
 *   <li>REASONING: ThinkingBlock → metadata (key {@value #REASONING_CONTENT_KEY});
 *       ToolUseBlock → toolCalls; TextBlock is not used as text content.</li>
 *   <li>AGENT_RESULT: TextBlock → AssistantMessage text content;
 *       ThinkingBlock and ToolUseBlock handled as above if present.</li>
 * </ul>
 * <p>
 * Reverse mapping (Spring AI → AgentScope) in {@link #toAgentScopeMessage(Message)}:
 * <ul>
 *   <li>AssistantMessage: metadata {@value #REASONING_CONTENT_KEY} → ThinkingBlock;
 *       toolCalls → ToolUseBlock; text content → TextBlock.</li>
 *   <li>ToolResponseMessage: each ToolResponse → ToolResultBlock (id, name, output as TextBlock).</li>
 * </ul>
 */
public final class AgentScopeMessageUtils {

	/** Metadata key for reasoning/thinking content in AssistantMessage. */
	public static final String REASONING_CONTENT_KEY = "reasoning_content";

	private AgentScopeMessageUtils() {
	}

	/**
	 * Convert Spring AI messages to AgentScope Msg list.
	 * @param messages Spring AI messages (UserMessage, AssistantMessage, etc.)
	 * @return List of AgentScope Msg
	 */
	public static List<Msg> toAgentScopeMessages(List<Message> messages) {
		if (messages == null || messages.isEmpty()) {
			return List.of();
		}
		List<Msg> result = new ArrayList<>(messages.size());
		for (Message m : messages) {
			Msg msg = toAgentScopeMessage(m);
			if (msg != null) {
				result.add(msg);
			}
		}
		return result;
	}

	/**
	 * Convert a single Spring AI Message to AgentScope Msg.
	 * AssistantMessage: reasoning_content metadata → ThinkingBlock; toolCalls → ToolUseBlock;
	 * text → TextBlock. ToolResponseMessage: each response → ToolResultBlock.
	 */
	public static Msg toAgentScopeMessage(Message message) {
		if (message == null) {
			return null;
		}
		String text = message.getText();
		if (message instanceof UserMessage) {
			return Msg.builder()
					.name("user")
					.role(MsgRole.USER)
					.textContent(text != null ? text : "")
					.build();
		}
		if (message instanceof AssistantMessage assistantMessage) {
			return toAgentScopeMsgFromAssistantMessage(assistantMessage);
		}
		if (message instanceof ToolResponseMessage toolResponseMessage) {
			return toAgentScopeMsgFromToolResponseMessage(toolResponseMessage);
		}
		// System and other: treat as user for compatibility
		return Msg.builder()
				.name("user")
				.role(MsgRole.USER)
				.textContent(text != null ? text : "")
				.build();
	}

	/**
	 * Convert AssistantMessage to AgentScope Msg with ContentBlocks.
	 * reasoning_content in metadata → ThinkingBlock; toolCalls → ToolUseBlock; text → TextBlock.
	 */
	private static Msg toAgentScopeMsgFromAssistantMessage(AssistantMessage assistantMessage) {
		List<ContentBlock> content = new ArrayList<>();
		Map<String, Object> metadata = assistantMessage.getMetadata();
		if (metadata != null && metadata.containsKey(REASONING_CONTENT_KEY)) {
			Object val = metadata.get(REASONING_CONTENT_KEY);
			String reasoning = val != null ? val.toString() : "";
			content.add(ThinkingBlock.builder().thinking(reasoning).build());
		}
		if (assistantMessage.hasToolCalls()) {
			for (AssistantMessage.ToolCall tc : assistantMessage.getToolCalls()) {
				Map<String, Object> input = parseToolCallArguments(tc.arguments());
				content.add(new ToolUseBlock(
						tc.id() != null ? tc.id() : "",
						tc.name() != null ? tc.name() : "",
						input));
			}
		}
		String textContent = assistantMessage.getText();
		if (textContent != null && !textContent.isEmpty()) {
			content.add(TextBlock.builder().text(textContent).build());
		}
		if (content.isEmpty()) {
			return Msg.builder()
					.name("assistant")
					.role(MsgRole.ASSISTANT)
					.textContent("")
					.build();
		}
		return Msg.builder()
				.name("assistant")
				.role(MsgRole.ASSISTANT)
				.content(content)
				.build();
	}

	/**
	 * Convert ToolResponseMessage to AgentScope Msg with ToolResultBlock content.
	 */
	private static Msg toAgentScopeMsgFromToolResponseMessage(ToolResponseMessage toolResponseMessage) {
		List<ToolResponseMessage.ToolResponse> responses = toolResponseMessage.getResponses();
		if (responses == null || responses.isEmpty()) {
			return Msg.builder()
					.name("tool")
					.role(MsgRole.TOOL)
					.textContent("")
					.build();
		}
		List<ContentBlock> blocks = new ArrayList<>();
		for (ToolResponseMessage.ToolResponse r : responses) {
			String id = r.id() != null ? r.id() : "";
			String name = r.name() != null ? r.name() : "";
			String contentStr = r.responseData() != null ? r.responseData() : "";
			ContentBlock outputBlock = TextBlock.builder().text(contentStr).build();
			blocks.add(ToolResultBlock.of(id, name, outputBlock));
		}
		return Msg.builder()
				.name("tool")
				.role(MsgRole.TOOL)
				.content(blocks)
				.build();
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Object> parseToolCallArguments(String arguments) {
		if (arguments == null || arguments.isBlank()) {
			return Map.of();
		}
		try {
			Object parsed = JsonParser.fromJson(arguments, Map.class);
			return parsed instanceof Map ? (Map<String, Object>) parsed : Map.of();
		}
		catch (Exception e) {
			return Map.of();
		}
	}

	/**
	 * Convert an AgentScope Event to Spring AI Message by event type.
	 * REASONING and AGENT_RESULT → AssistantMessage; TOOL_RESULT → ToolResponseMessage.
	 * Content mapping: REASONING uses ThinkingBlock/ToolUseBlock only (no text from TextBlock);
	 * AGENT_RESULT uses TextBlock for text content plus ThinkingBlock/ToolUseBlock if present.
	 */
	public static Message toMessage(Event event) {
		if (event == null) {
			return null;
		}
		Msg msg = event.getMessage();
		if (msg == null) {
			return null;
		}
		EventType type = event.getType();
		if (type == EventType.TOOL_RESULT) {
			return toToolResponseMessage(msg);
		}
		return toAssistantMessage(msg, type);
	}

	/**
	 * Convert an AgentScope Msg to Spring AI Message by role (legacy).
	 * For REASONING/AGENT_RESULT content use {@link #toAssistantMessage(Msg)} which parses ContentBlocks.
	 */
	public static Message toMessage(Msg msg) {
		if (msg == null) {
			return null;
		}
		return switch (msg.getRole()) {
			case ASSISTANT -> toAssistantMessage(msg);
			case TOOL -> toToolResponseMessage(msg);
			default -> new UserMessage(msg.getTextContent() != null ? msg.getTextContent() : "");
		};
	}

	/**
	 * Convert an AgentScope Msg to Spring AI AssistantMessage (no event type).
	 * Behaves like AGENT_RESULT: TextBlock → text content, ThinkingBlock → metadata, ToolUseBlock → toolCalls.
	 */
	public static AssistantMessage toAssistantMessage(Msg msg) {
		return toAssistantMessage(msg, EventType.AGENT_RESULT);
	}

	/**
	 * Convert an AgentScope Msg to Spring AI AssistantMessage by event type and ContentBlocks.
	 * <ul>
	 *   <li>ThinkingBlock → metadata key {@value #REASONING_CONTENT_KEY}</li>
	 *   <li>ToolUseBlock → AssistantMessage toolCalls</li>
	 *   <li>TextBlock → AssistantMessage text content only when eventType is AGENT_RESULT</li>
	 * </ul>
	 * For REASONING, TextBlock is not used as main text content.
	 */
	public static AssistantMessage toAssistantMessage(Msg msg, EventType eventType) {
		if (msg == null) {
			return null;
		}
		List<ThinkingBlock> thinkingBlocks = msg.getContentBlocks(ThinkingBlock.class);
		List<ToolUseBlock> toolUseBlocks = msg.getContentBlocks(ToolUseBlock.class);
		List<TextBlock> textBlocks = msg.getContentBlocks(TextBlock.class);

		// Only AGENT_RESULT uses TextBlock as AssistantMessage text content
		String textContent = (eventType == EventType.AGENT_RESULT)
				? textBlocks.stream()
						.map(TextBlock::getText)
						.reduce("", (a, b) -> a.isEmpty() ? b : a + "\n" + b)
				: "";

		Map<String, Object> metadata = new HashMap<>();
		if (!thinkingBlocks.isEmpty()) {
			String reasoningContent = thinkingBlocks.stream()
					.map(ThinkingBlock::getThinking)
					.reduce("", (a, b) -> a.isEmpty() ? b : a + "\n" + b);
			metadata.put(REASONING_CONTENT_KEY, reasoningContent);
		}

		List<AssistantMessage.ToolCall> toolCalls = new ArrayList<>();
		for (ToolUseBlock block : toolUseBlocks) {
			String arguments = block.getInput() != null && !block.getInput().isEmpty()
					? JsonParser.toJson(block.getInput())
					: "{}";
			toolCalls.add(new AssistantMessage.ToolCall(
					block.getId() != null ? block.getId() : "",
					"function",
					block.getName() != null ? block.getName() : "",
					arguments));
		}

		var builder = AssistantMessage.builder().content(textContent != null ? textContent : "");
		if (!metadata.isEmpty()) {
			builder.properties(metadata);
		}
		if (!toolCalls.isEmpty()) {
			builder.toolCalls(toolCalls);
		}
		return builder.build();
	}

	/**
	 * Convert an AgentScope Msg (TOOL role with ToolResultBlock content) to Spring AI ToolResponseMessage.
	 */
	public static ToolResponseMessage toToolResponseMessage(Msg msg) {
		if (msg == null) {
			return null;
		}
		List<ToolResultBlock> resultBlocks = msg.getContentBlocks(ToolResultBlock.class);
		if (resultBlocks.isEmpty()) {
			String fallback = msg.getTextContent();
			return ToolResponseMessage.builder()
					.responses(List.of(new ToolResponseMessage.ToolResponse("", "", fallback != null ? fallback : "")))
					.build();
		}
		List<ToolResponseMessage.ToolResponse> responses = new ArrayList<>();
		for (ToolResultBlock block : resultBlocks) {
			String content = contentBlocksToText(block.getOutput());
			responses.add(new ToolResponseMessage.ToolResponse(
					block.getId() != null ? block.getId() : "",
					block.getName() != null ? block.getName() : "",
					content));
		}
		return ToolResponseMessage.builder().responses(responses).build();
	}

	private static String contentBlocksToText(List<io.agentscope.core.message.ContentBlock> output) {
		if (output == null || output.isEmpty()) {
			return "";
		}
		StringBuilder sb = new StringBuilder();
		for (io.agentscope.core.message.ContentBlock block : output) {
			if (block instanceof TextBlock textBlock) {
				if (sb.length() > 0) {
					sb.append("\n");
				}
				sb.append(textBlock.getText());
			}
		}
		return sb.toString();
	}
}
