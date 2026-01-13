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
package com.alibaba.cloud.ai.graph.agent;

import com.alibaba.cloud.ai.graph.agent.tool.ToolResult;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.content.Media;
import org.springframework.ai.tool.execution.ToolCallResultConverter;
import org.springframework.ai.util.json.JsonParser;

import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Converter for tool call results that supports text, multimodal content, and ToolResult.
 *
 * <p>Handles the following result types:</p>
 * <ul>
 *   <li>{@link ToolResult} - Rich results with text and/or media</li>
 *   <li>{@link Media} - Single media content</li>
 *   <li>{@link Collection} of {@link Media} - Multiple media content</li>
 *   <li>{@link AssistantMessage} - Text or media content</li>
 *   <li>Other objects - Serialized to JSON</li>
 * </ul>
 *
 * @author disaster
 * @since 1.0.0
 */
public class MessageToolCallResultConverter implements ToolCallResultConverter {

	private static final Logger logger = LoggerFactory.getLogger(MessageToolCallResultConverter.class);

	/**
	 * Converts tool result to a string representation.
	 * Supports ToolResult, Media, and other types.
	 */
	public String convert(@Nullable Object result, @Nullable Type returnType) {
		if (returnType == Void.TYPE) {
			logger.debug("The tool has no return type. Converting to conventional response.");
			return JsonParser.toJson("Done");
		}

		if (result == null) {
			return "";
		}

		if (result instanceof String str) {
			return str;
		}

		// Handle ToolResult - rich result model
		if (result instanceof ToolResult toolResult) {
			return toolResult.toStringResult();
		}

		// Handle single Media
		if (result instanceof Media media) {
			return serializeMedia(media);
		}

		// Handle collection of Media
		if (result instanceof Collection<?> collection && !collection.isEmpty()) {
			Object first = collection.iterator().next();
			if (first instanceof Media) {
				@SuppressWarnings("unchecked")
				List<Media> mediaList = new ArrayList<>((Collection<Media>) collection);
				return ToolResult.media(mediaList).toStringResult();
			}
		}

		// Handle AssistantMessage
		if (result instanceof AssistantMessage assistantMessage) {
			if (StringUtils.hasLength(assistantMessage.getText())) {
				// Check if there's also media content
				if (CollectionUtils.isNotEmpty(assistantMessage.getMedia())) {
					return ToolResult.mixed(assistantMessage.getText(), assistantMessage.getMedia()).toStringResult();
				}
				return assistantMessage.getText();
			}
			else if (CollectionUtils.isNotEmpty(assistantMessage.getMedia())) {
				// Media-only result
				return ToolResult.media(assistantMessage.getMedia()).toStringResult();
			}
			logger.warn("The tool returned an empty AssistantMessage. Converting to conventional response.");
			return JsonParser.toJson("Done");
		}

		// Default: try JSON serialization
		logger.debug("Converting tool result to JSON.");
		return JsonParser.toJson(result);
	}

	/**
	 * Serializes a single Media to ToolResult format.
	 */
	private String serializeMedia(Media media) {
		return ToolResult.media(List.of(media)).toStringResult();
	}

}
