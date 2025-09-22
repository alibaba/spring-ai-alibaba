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

import com.alibaba.cloud.ai.entity.ChatSession;
import com.alibaba.cloud.ai.mapper.ChatSessionMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Chat Session Service Class
 */
@Service
public class ChatSessionService {

	private static final Logger log = LoggerFactory.getLogger(ChatSessionService.class);

	@Autowired
	private ChatSessionMapper chatSessionMapper;

	/**
	 * Get session list by agent ID
	 */
	public List<ChatSession> findByAgentId(Integer agentId) {
		return chatSessionMapper.selectByAgentId(agentId);
	}

	/**
	 * Get session by ID
	 */
	public ChatSession findById(String sessionId) {
		return chatSessionMapper.selectBySessionId(sessionId);
	}

	/**
	 * Create a new session
	 */
	public ChatSession createSession(Integer agentId, String title, Long userId) {
		String sessionId = UUID.randomUUID().toString();

		ChatSession session = new ChatSession(sessionId, agentId, title != null ? title : "新对话", "active", userId);
		chatSessionMapper.insert(session);

		log.info("Created new chat session: {} for agent: {}", sessionId, agentId);
		return session;
	}

	/**
	 * Update session
	 */
	public ChatSession updateSession(ChatSession session) {
		chatSessionMapper.updateById(session);
		log.info("Updated chat session: {}", session.getId());
		return session;
	}

	/**
	 * Clear all sessions for an agent
	 */
	public void clearSessionsByAgentId(Integer agentId) {
		LocalDateTime now = LocalDateTime.now();
		int updated = chatSessionMapper.softDeleteByAgentId(agentId, now);
		log.info("Cleared {} sessions for agent: {}", updated, agentId);
	}

	/**
	 * Update the last activity time of a session
	 */
	public void updateSessionTime(String sessionId) {
		LocalDateTime now = LocalDateTime.now();
		chatSessionMapper.updateSessionTime(sessionId, now);
	}

	/**
	 * 置顶/取消置顶会话
	 */
	public void pinSession(String sessionId, boolean isPinned) {
		LocalDateTime now = LocalDateTime.now();
		chatSessionMapper.updatePinStatus(sessionId, isPinned, now);
		log.info("Updated pin status for session: {} to: {}", sessionId, isPinned);
	}

	/**
	 * Rename session
	 */
	public void renameSession(String sessionId, String newTitle) {
		LocalDateTime now = LocalDateTime.now();
		chatSessionMapper.updateTitle(sessionId, newTitle, now);
		log.info("Renamed session: {} to: {}", sessionId, newTitle);
	}

	/**
	 * Delete a single session
	 */
	public void deleteSession(String sessionId) {
		LocalDateTime now = LocalDateTime.now();
		chatSessionMapper.softDeleteById(sessionId, now);
		log.info("Deleted session: {}", sessionId);
	}

}
