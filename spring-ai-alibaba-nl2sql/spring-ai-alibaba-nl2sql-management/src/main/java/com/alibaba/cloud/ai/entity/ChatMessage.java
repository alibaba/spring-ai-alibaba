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
package com.alibaba.cloud.ai.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;

/**
 * Chat Message Entity Class
 */
@TableName("chat_message")
public class ChatMessage {

	@TableId(value = "id", type = IdType.AUTO)
	private Long id;

	@TableField("session_id")
	private String sessionId;

	@TableField("role")
	private String role; // user, assistant, system

	@TableField("content")
	private String content;

	@TableField("message_type")
	private String messageType; // text, sql, result, error

	@TableField("metadata")
	private String metadata; // JSON格式的元数据

	@TableField(value = "create_time", fill = FieldFill.INSERT)
	private LocalDateTime createTime;

	public ChatMessage() {
	}

	public ChatMessage(String sessionId, String role, String content, String messageType) {
		this.sessionId = sessionId;
		this.role = role;
		this.content = content;
		this.messageType = messageType;
	}

	public ChatMessage(String sessionId, String role, String content, String messageType, String metadata) {
		this.sessionId = sessionId;
		this.role = role;
		this.content = content;
		this.messageType = messageType;
		this.metadata = metadata;
	}

	// Getters and Setters
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getSessionId() {
		return sessionId;
	}

	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}

	public String getRole() {
		return role;
	}

	public void setRole(String role) {
		this.role = role;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public String getMessageType() {
		return messageType;
	}

	public void setMessageType(String messageType) {
		this.messageType = messageType;
	}

	public String getMetadata() {
		return metadata;
	}

	public void setMetadata(String metadata) {
		this.metadata = metadata;
	}

	public LocalDateTime getCreateTime() {
		return createTime;
	}

	public void setCreateTime(LocalDateTime createTime) {
		this.createTime = createTime;
	}

	@Override
	public String toString() {
		return "ChatMessage{" + "id=" + id + ", sessionId='" + sessionId + '\'' + ", role='" + role + '\''
				+ ", content='" + content + '\'' + ", messageType='" + messageType + '\'' + ", metadata='" + metadata
				+ '\'' + ", createTime=" + createTime + '}';
	}

}
