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
package com.alibaba.cloud.ai.graph.agent.interceptor.toolselection;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.List;

/**
 * {@link ToolSelectionStrategy} that asks a chat model to select relevant tools.
 */
public final class LlmToolSelectionStrategy implements ToolSelectionStrategy {

	static final String DEFAULT_SYSTEM_PROMPT =
			"Your goal is to select the most relevant tools for answering the user's query.";

	private final ChatModel selectionModel;

	private final String systemPrompt;

	private final ObjectMapper objectMapper = new ObjectMapper();

	public LlmToolSelectionStrategy(ChatModel selectionModel) {
		this(selectionModel, DEFAULT_SYSTEM_PROMPT);
	}

	public LlmToolSelectionStrategy(ChatModel selectionModel, String systemPrompt) {
		if (selectionModel == null) {
			throw new IllegalArgumentException("selectionModel must not be null");
		}
		this.selectionModel = selectionModel;
		this.systemPrompt = systemPrompt != null ? systemPrompt : DEFAULT_SYSTEM_PROMPT;
	}

	@Override
	public List<String> select(ToolSelectionRequest request) throws JsonProcessingException {
		StringBuilder toolList = new StringBuilder();
		for (ToolMetadata tool : request.tools()) {
			toolList.append("- ").append(tool.name());
			if (tool.description() != null && !tool.description().isEmpty()) {
				toolList.append(": ").append(tool.description());
			}
			toolList.append("\n");
		}

		String maxToolsInstruction = request.maxTools() != null
				? "\nIMPORTANT: List the tool names in order of relevance. Select at most "
						+ request.maxTools() + " tools."
				: "";

		List<Message> selectionMessages = List.of(
				new SystemMessage(systemPrompt + maxToolsInstruction),
				new UserMessage("Available tools:\n" + toolList + "\nUser query: " + request.query()
						+ "\n\nRespond with a JSON object containing a 'tools' array with the selected tool names: "
						+ "{\"tools\": [\"tool1\", \"tool2\"]}"));

		var response = selectionModel.call(new Prompt(selectionMessages));
		String responseText = response.getResult().getOutput().getText();
		return parseToolSelection(responseText);
	}

	private List<String> parseToolSelection(String responseText) throws JsonProcessingException {
		ToolSelectionResponse response = objectMapper.readValue(responseText, ToolSelectionResponse.class);
		return response.tools;
	}

	private static class ToolSelectionResponse {

		private final List<String> tools;

		@JsonCreator
		ToolSelectionResponse(@JsonProperty(value = "tools", required = true) List<String> tools) {
			if (tools == null) {
				throw new IllegalArgumentException("tools must not be null");
			}
			this.tools = tools;
		}
	}
}
