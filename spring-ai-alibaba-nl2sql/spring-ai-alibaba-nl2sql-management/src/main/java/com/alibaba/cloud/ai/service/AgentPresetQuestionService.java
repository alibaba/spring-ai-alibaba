/**
 * Copyright 2024-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alibaba.cloud.ai.service;

import com.alibaba.cloud.ai.entity.AgentPresetQuestion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;

@Service
public class AgentPresetQuestionService {

	@Autowired
	private JdbcTemplate jdbcTemplate;

	private static final String SELECT_BY_AGENT_ID = """
			SELECT * FROM agent_preset_question
			WHERE agent_id = ? AND is_active = 1
			ORDER BY sort_order ASC, id ASC
			""";

	private static final String INSERT = """
			INSERT INTO agent_preset_question (agent_id, question, sort_order, is_active, create_time, update_time)
			VALUES (?, ?, ?, ?, NOW(), NOW())
			""";

	private static final String UPDATE = """
			UPDATE agent_preset_question
			SET question = ?, sort_order = ?, is_active = ?, update_time = NOW()
			WHERE id = ?
			""";

	private static final String DELETE = """
			DELETE FROM agent_preset_question WHERE id = ?
			""";

	private static final String DELETE_BY_AGENT_ID = """
			DELETE FROM agent_preset_question WHERE agent_id = ?
			""";

	/**
	 * Get preset question list by agent ID
	 */
	public List<AgentPresetQuestion> findByAgentId(Long agentId) {
		return jdbcTemplate.query(SELECT_BY_AGENT_ID, new BeanPropertyRowMapper<>(AgentPresetQuestion.class), agentId);
	}

	/**
	 * Create preset question
	 */
	public AgentPresetQuestion create(AgentPresetQuestion question) {
		KeyHolder keyHolder = new GeneratedKeyHolder();
		jdbcTemplate.update(connection -> {
			PreparedStatement ps = connection.prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS);
			ps.setLong(1, question.getAgentId());
			ps.setString(2, question.getQuestion());
			ps.setInt(3, question.getSortOrder() != null ? question.getSortOrder() : 0);
			ps.setBoolean(4, question.getIsActive() != null ? question.getIsActive() : true);
			return ps;
		}, keyHolder);
		question.setId(keyHolder.getKey().longValue());
		return question;
	}

	/**
	 * Update preset question
	 */
	public void update(Long id, AgentPresetQuestion question) {
		jdbcTemplate.update(UPDATE, question.getQuestion(), question.getSortOrder(), question.getIsActive(), id);
	}

	/**
	 * Delete preset question
	 */
	public void deleteById(Long id) {
		jdbcTemplate.update(DELETE, id);
	}

	/**
	 * Delete all preset questions of agent
	 */
	public void deleteByAgentId(Long agentId) {
		jdbcTemplate.update(DELETE_BY_AGENT_ID, agentId);
	}

	/**
	 * Batch save preset questions (delete first then insert)
	 */
	public void batchSave(Long agentId, List<AgentPresetQuestion> questions) {
		// First delete all preset questions of the agent
		deleteByAgentId(agentId);
		// Batch insert new preset questions
		for (int i = 0; i < questions.size(); i++) {
			AgentPresetQuestion question = questions.get(i);
			question.setAgentId(agentId);
			question.setSortOrder(i);
			question.setIsActive(true);
			create(question);
		}
	}

}
