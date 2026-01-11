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

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.tool.execution.ToolCallResultConverter;
import org.springframework.ai.util.json.JsonParser;

import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

import java.lang.reflect.Type;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MessageToolCallResultConverter implements ToolCallResultConverter {

	private static final Logger logger = LoggerFactory.getLogger(MessageToolCallResultConverter.class);

	/**
	 * Currently Spring AI ToolResponseMessage only supports text type, that's why the return type of this method is String.
	 * More types like image/audio/video/file can be supported in the future.
	 */
	public String convert(@Nullable Object result, @Nullable Type returnType) {
		if (returnType == Void.TYPE) {
			logger.debug("The tool has no return type. Converting to conventional response.");
			return JsonParser.toJson("Done");
		} else if (result instanceof AssistantMessage assistantMessage) {
			if (StringUtils.hasLength(assistantMessage.getText())) {
				return assistantMessage.getText();
			} else if (CollectionUtils.isNotEmpty(assistantMessage.getMedia())) {
				throw new UnsupportedOperationException("Currently Spring AI ToolResponseMessage only supports text type, that's why the return type of this method is String. More types like image/audio/video/file can be supported in the future.");
			}
			logger.warn("The tool returned an empty AssistantMessage. Converting to conventional response.");
			return JsonParser.toJson("Done");
		} else {
			logger.debug("Converting tool result to JSON.");
			return JsonParser.toJson(result);
		}
	}
}
