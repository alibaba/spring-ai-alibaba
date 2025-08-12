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
package com.alibaba.cloud.ai.service;

import com.alibaba.cloud.ai.entity.ChatMessage;
import com.alibaba.cloud.ai.mapper.ChatMessageMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 聊天消息服务类
 */
@Service
public class ChatMessageService {

	private static final Logger log = LoggerFactory.getLogger(ChatMessageService.class);

	@Autowired
	private ChatMessageMapper chatMessageMapper;

	/**
	 * 根据会话ID获取消息列表
	 */
	public List<ChatMessage> findBySessionId(String sessionId) {
		return chatMessageMapper.selectBySessionId(sessionId);
	}

	/**
	 * 保存消息
	 */
	public ChatMessage saveMessage(ChatMessage message) {
		chatMessageMapper.insert(message);
		log.info("Saved message: {} for session: {}", message.getId(), message.getSessionId());
		return message;
	}

	/**
	 * 保存用户消息
	 */
	public ChatMessage saveUserMessage(String sessionId, String content) {
		ChatMessage message = new ChatMessage(sessionId, "user", content, "text");
		return saveMessage(message);
	}

	/**
	 * 保存助手消息
	 */
	public ChatMessage saveAssistantMessage(String sessionId, String content, String messageType, String metadata) {
		ChatMessage message = new ChatMessage(sessionId, "assistant", content, messageType, metadata);
		return saveMessage(message);
	}

}
