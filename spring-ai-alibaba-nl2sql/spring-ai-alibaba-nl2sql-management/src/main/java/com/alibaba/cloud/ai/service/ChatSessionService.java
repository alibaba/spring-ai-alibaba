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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 聊天会话服务类
 */
@Service
public class ChatSessionService {

	private static final Logger log = LoggerFactory.getLogger(ChatSessionService.class);

	@Autowired
	private JdbcTemplate jdbcTemplate;

	private static final String SELECT_BY_AGENT_ID = """
			SELECT * FROM chat_session WHERE agent_id = ? AND status = 'active' ORDER BY is_pinned DESC, update_time DESC
			""";

	private static final String SELECT_BY_ID = """
			SELECT * FROM chat_session WHERE id = ?
			""";

	private static final String INSERT = """
			INSERT INTO chat_session (id, agent_id, title, status, is_pinned, user_id, create_time, update_time)
			VALUES (?, ?, ?, ?, ?, ?, ?, ?)
			""";

	private static final String UPDATE = """
			UPDATE chat_session SET title = ?, status = ?, update_time = ? WHERE id = ?
			""";

	private static final String DELETE_BY_AGENT_ID = """
			UPDATE chat_session SET status = 'deleted', update_time = ? WHERE agent_id = ? AND status = 'active'
			""";

	private static final String UPDATE_TIME = """
			UPDATE chat_session SET update_time = ? WHERE id = ?
			""";

	private static final String UPDATE_PIN_STATUS = """
			UPDATE chat_session SET is_pinned = ?, update_time = ? WHERE id = ?
			""";

	private static final String UPDATE_TITLE = """
			UPDATE chat_session SET title = ?, update_time = ? WHERE id = ?
			""";

	private static final String DELETE_SESSION = """
			UPDATE chat_session SET status = 'deleted', update_time = ? WHERE id = ?
			""";

	/**
	 * 根据智能体ID获取会话列表
	 */
	public List<ChatSession> findByAgentId(Integer agentId) {
		return jdbcTemplate.query(SELECT_BY_AGENT_ID, new BeanPropertyRowMapper<>(ChatSession.class), agentId);
	}

	/**
	 * 根据ID获取会话
	 */
	public ChatSession findById(String sessionId) {
		List<ChatSession> results = jdbcTemplate.query(SELECT_BY_ID, new BeanPropertyRowMapper<>(ChatSession.class),
				sessionId);
		return results.isEmpty() ? null : results.get(0);
	}

	/**
	 * 创建新会话
	 */
	public ChatSession createSession(Integer agentId, String title, Long userId) {
		String sessionId = UUID.randomUUID().toString();
		LocalDateTime now = LocalDateTime.now();

		ChatSession session = new ChatSession(sessionId, agentId, title != null ? title : "新对话", "active", userId);
		session.setCreateTime(now);
		session.setUpdateTime(now);

		jdbcTemplate.update(INSERT, session.getId(), session.getAgentId(), session.getTitle(), session.getStatus(),
				session.getIsPinned(), session.getUserId(), session.getCreateTime(), session.getUpdateTime());

		log.info("Created new chat session: {} for agent: {}", sessionId, agentId);
		return session;
	}

	/**
	 * 更新会话
	 */
	public ChatSession updateSession(ChatSession session) {
		LocalDateTime now = LocalDateTime.now();
		session.setUpdateTime(now);

		jdbcTemplate.update(UPDATE, session.getTitle(), session.getStatus(), session.getUpdateTime(), session.getId());

		log.info("Updated chat session: {}", session.getId());
		return session;
	}

	/**
	 * 清空智能体的所有会话
	 */
	public void clearSessionsByAgentId(Integer agentId) {
		LocalDateTime now = LocalDateTime.now();
		int updated = jdbcTemplate.update(DELETE_BY_AGENT_ID, now, agentId);
		log.info("Cleared {} sessions for agent: {}", updated, agentId);
	}

	/**
	 * 更新会话的最后活动时间
	 */
	public void updateSessionTime(String sessionId) {
		LocalDateTime now = LocalDateTime.now();
		jdbcTemplate.update(UPDATE_TIME, now, sessionId);
	}

	/**
	 * 置顶/取消置顶会话
	 */
	public void pinSession(String sessionId, boolean isPinned) {
		LocalDateTime now = LocalDateTime.now();
		jdbcTemplate.update(UPDATE_PIN_STATUS, isPinned, now, sessionId);
		log.info("Updated pin status for session: {} to: {}", sessionId, isPinned);
	}

	/**
	 * 重命名会话
	 */
	public void renameSession(String sessionId, String newTitle) {
		LocalDateTime now = LocalDateTime.now();
		jdbcTemplate.update(UPDATE_TITLE, newTitle, now, sessionId);
		log.info("Renamed session: {} to: {}", sessionId, newTitle);
	}

	/**
	 * 删除单个会话
	 */
	public void deleteSession(String sessionId) {
		LocalDateTime now = LocalDateTime.now();
		jdbcTemplate.update(DELETE_SESSION, now, sessionId);
		log.info("Deleted session: {}", sessionId);
	}

}
