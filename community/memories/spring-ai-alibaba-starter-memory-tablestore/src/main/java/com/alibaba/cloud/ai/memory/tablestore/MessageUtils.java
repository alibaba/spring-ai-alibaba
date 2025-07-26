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
package com.alibaba.cloud.ai.memory.tablestore;

import com.aliyun.openservices.tablestore.agent.model.Metadata;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.NullSerializer;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.AbstractMessage;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.content.Media;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class MessageUtils {

	private static final Logger log = LoggerFactory.getLogger(MessageUtils.class);

	private static final ObjectMapper MAPPER;

	public static final String MESSAGE_TYPE = AbstractMessage.MESSAGE_TYPE;

	public static final String MESSAGE_MEDIA = "_spring_ai_message_media";

	public static final String MESSAGE_TOOL_CALLS = "_spring_ai_message_tool_calls";

	public static final String MESSAGE_TOOL_RESPONSE = "_spring_ai_message_tool_response";

	static {
		MAPPER = new ObjectMapper();
		SimpleModule module = new SimpleModule();
		module.addSerializer(byte[].class, NullSerializer.instance);
		MAPPER.registerModule(module);
	}

	private static final Set<Class<?>> SUPPORTED_VALUE_TYPES = new LinkedHashSet<>();

	static {
		SUPPORTED_VALUE_TYPES.add(String.class);

		SUPPORTED_VALUE_TYPES.add(int.class);
		SUPPORTED_VALUE_TYPES.add(Integer.class);

		SUPPORTED_VALUE_TYPES.add(long.class);
		SUPPORTED_VALUE_TYPES.add(Long.class);

		SUPPORTED_VALUE_TYPES.add(float.class);
		SUPPORTED_VALUE_TYPES.add(Float.class);

		SUPPORTED_VALUE_TYPES.add(double.class);
		SUPPORTED_VALUE_TYPES.add(Double.class);

		SUPPORTED_VALUE_TYPES.add(short.class);
		SUPPORTED_VALUE_TYPES.add(Short.class);

		SUPPORTED_VALUE_TYPES.add(boolean.class);
		SUPPORTED_VALUE_TYPES.add(Boolean.class);

		SUPPORTED_VALUE_TYPES.add(byte[].class);
	}

	public static String getMD5UserId(String conversationId) {
		return DigestUtils.md5Hex(conversationId);
	}

	public static Message toSpringMessage(com.aliyun.openservices.tablestore.agent.model.Message tablestoreMessage) {
		Metadata metadata = tablestoreMessage.getMetadata();
		Long createTime = tablestoreMessage.getCreateTime();
		metadata.put("createTime", createTime);
		String sessionId = tablestoreMessage.getSessionId();
		metadata.put("sessionId", sessionId);
		String messageId = tablestoreMessage.getMessageId();
		metadata.put("messageId", messageId);
		String content = tablestoreMessage.getContent();
		Map<String, Object> metadataMap = metadata.toMap();
		try {
			MessageType messageType = MessageType.fromValue(metadata.getString(MESSAGE_TYPE));
			metadataMap.put(AbstractMessage.MESSAGE_TYPE, messageType);
			switch (messageType) {
				case USER: {
					String mediaJson = metadata.getString(MESSAGE_MEDIA);
					List<Media> media = Collections.emptyList();
					if (mediaJson != null) {
						media = MAPPER.readValue(mediaJson, new TypeReference<List<Media>>() {
						});
					}
					return UserMessage.builder().text(content).media(media).metadata(metadataMap).build();
				}
				case ASSISTANT: {
					String mediaJson = metadata.getString(MESSAGE_MEDIA);
					List<Media> media = Collections.emptyList();
					if (mediaJson != null) {
						media = MAPPER.readValue(mediaJson, new TypeReference<List<Media>>() {
						});
					}

					String toolCallsJson = metadata.getString(MESSAGE_TOOL_CALLS);
					List<AssistantMessage.ToolCall> toolCalls = Collections.emptyList();
					if (toolCallsJson != null) {
						toolCalls = MAPPER.readValue(mediaJson, new TypeReference<List<AssistantMessage.ToolCall>>() {
						});
					}

					return new AssistantMessage(content, metadataMap, toolCalls, media);
				}
				case SYSTEM: {
					return SystemMessage.builder().text(content).metadata(metadataMap).build();
				}
				case TOOL: {
					String toolResponseJson = metadata.getString(MESSAGE_TOOL_RESPONSE);
					List<ToolResponseMessage.ToolResponse> toolResponse = Collections.emptyList();
					if (toolResponseJson != null) {
						toolResponse = MAPPER.readValue(toolResponseJson,
								new TypeReference<List<ToolResponseMessage.ToolResponse>>() {
								});
					}
					return new ToolResponseMessage(toolResponse, metadataMap);
				}
				default:
					throw new UnsupportedOperationException("MessageType " + messageType + " is not supported");
			}
		}
		catch (Exception e) {
			log.error("Error converting message", e);
			return new UserMessage("Error: " + e.getMessage());
		}
	}

	public static com.aliyun.openservices.tablestore.agent.model.Message toTablestoreMessage(String conversationId,
			Message springMessage) {
		String messageId = UUID.randomUUID().toString();
		MessageType messageType = springMessage.getMessageType();
		String text = springMessage.getText();
		com.aliyun.openservices.tablestore.agent.model.Message message = new com.aliyun.openservices.tablestore.agent.model.Message(
				conversationId, messageId);
		try {
			Metadata metadata = new Metadata();
			for (Map.Entry<String, Object> entry : springMessage.getMetadata().entrySet()) {
				if (SUPPORTED_VALUE_TYPES.contains(entry.getValue().getClass())) {
					metadata.putObject(entry.getKey(), entry.getValue());
				}
				else {
					metadata.put(entry.getKey(), MAPPER.writeValueAsString(entry.getValue()));
				}
			}
			metadata.put(MESSAGE_TYPE, messageType.getValue());
			switch (messageType) {
				case USER: {
					UserMessage userMessage = (UserMessage) springMessage;
					String mediaJson = MAPPER.writeValueAsString(userMessage.getMedia());
					metadata.put(MESSAGE_MEDIA, mediaJson);
					break;
				}
				case ASSISTANT: {
					AssistantMessage assistantMessage = (AssistantMessage) springMessage;
					String mediaJson = MAPPER.writeValueAsString(assistantMessage.getMedia());
					metadata.put(MESSAGE_MEDIA, mediaJson);
					String toolCallsJson = MAPPER.writeValueAsString(assistantMessage.getToolCalls());
					metadata.put(MESSAGE_TOOL_CALLS, toolCallsJson);
					break;
				}
				case SYSTEM: {
					break;
				}
				case TOOL: {
					ToolResponseMessage toolResponseMessage = (ToolResponseMessage) springMessage;
					String responsesJson = MAPPER.writeValueAsString(toolResponseMessage.getResponses());
					metadata.put(MESSAGE_TOOL_RESPONSE, responsesJson);
					break;
				}
				default:
					throw new UnsupportedOperationException("MessageType " + messageType + " is not supported");
			}

			message.setContent(text);
			message.setMetadata(metadata);
		}
		catch (Exception e) {
			log.error("Error converting message", e);
			throw new RuntimeException("Error converting message", e);
		}
		return message;
	}

}
