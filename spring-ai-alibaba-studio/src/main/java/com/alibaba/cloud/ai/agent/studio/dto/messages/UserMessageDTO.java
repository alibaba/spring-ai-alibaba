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
package com.alibaba.cloud.ai.agent.studio.dto.messages;

import org.springframework.ai.chat.messages.UserMessage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Data Transfer Object for UserMessage.
 * Provides serialization-friendly representation with default constructor and getters/setters.
 */
public class UserMessageDTO implements MessageDTO {

	@JsonProperty("messageType")
	private String messageType = "user";

	@JsonProperty("content")
	private String content;

	@JsonProperty("metadata")
	private Map<String, Object> metadata;

	@JsonProperty("media")
	private List<MediaDTO> media;

	/**
	 * Default constructor for deserialization.
	 */
	public UserMessageDTO() {
		this.metadata = new HashMap<>();
		this.media = new ArrayList<>();
	}

	/**
	 * Constructor with content.
	 */
	public UserMessageDTO(String content) {
		this();
		this.content = content;
	}

	/**
	 * Constructor from Spring AI UserMessage.
	 */
	public UserMessageDTO(UserMessage message) {
		this();
		this.content = message.getText();
		this.metadata = new HashMap<>(message.getMetadata());

		// Note: Media extraction is not currently supported
		// Spring AI's Media API is not directly accessible in this version
	}

	/**
	 * Convert to Spring AI UserMessage.
	 */
	public UserMessage toUserMessage() {
		// UserMessage constructor just takes content as String
		return new UserMessage(this.content);
	}

	// Getters and Setters

	public String getMessageType() {
		return messageType;
	}

	public void setMessageType(String messageType) {
		this.messageType = messageType;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public Map<String, Object> getMetadata() {
		return metadata;
	}

	public void setMetadata(Map<String, Object> metadata) {
		this.metadata = metadata;
	}

	public List<MediaDTO> getMedia() {
		return media;
	}

	public void setMedia(List<MediaDTO> media) {
		this.media = media;
	}

	/**
	 * DTO for Media within UserMessage.
	 * Placeholder for future media support.
	 */
	public static class MediaDTO {
		@JsonProperty("mimeType")
		private String mimeType;

		@JsonProperty("data")
		private Object data;

		public MediaDTO() {
		}

		// Getters and Setters

		public String getMimeType() {
			return mimeType;
		}

		public void setMimeType(String mimeType) {
			this.mimeType = mimeType;
		}

		public Object getData() {
			return data;
		}

		public void setData(Object data) {
			this.data = data;
		}
	}
}
