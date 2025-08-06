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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 聊天消息服务类
 */
@Service
public class ChatMessageService {

	private static final Logger log = LoggerFactory.getLogger(ChatMessageService.class);

	@Autowired
	private JdbcTemplate jdbcTemplate;

	private static final String SELECT_BY_SESSION_ID = """
			SELECT * FROM chat_message WHERE session_id = ? ORDER BY create_time ASC
			""";

	private static final String INSERT = """
			INSERT INTO chat_message (session_id, role, content, message_type, metadata, create_time)
			VALUES (?, ?, ?, ?, ?, ?)
			""";

	/**
	 * 根据会话ID获取消息列表
	 */
	public List<ChatMessage> findBySessionId(String sessionId) {
		return jdbcTemplate.query(SELECT_BY_SESSION_ID, new BeanPropertyRowMapper<>(ChatMessage.class), sessionId);
	}

	/**
	 * 保存消息
	 */
	public ChatMessage saveMessage(ChatMessage message) {
		LocalDateTime now = LocalDateTime.now();
		message.setCreateTime(now);

		KeyHolder keyHolder = new GeneratedKeyHolder();
		jdbcTemplate.update(connection -> {
			PreparedStatement ps = connection.prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS);
			ps.setString(1, message.getSessionId());
			ps.setString(2, message.getRole());
			ps.setString(3, message.getContent());
			ps.setString(4, message.getMessageType());
			ps.setString(5, message.getMetadata());
			ps.setObject(6, message.getCreateTime());
			return ps;
		}, keyHolder);

		Number key = keyHolder.getKey();
		if (key != null) {
			message.setId(key.longValue());
		}

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