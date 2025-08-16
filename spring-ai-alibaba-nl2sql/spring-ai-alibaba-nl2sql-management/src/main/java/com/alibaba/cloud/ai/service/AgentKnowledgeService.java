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

import com.alibaba.cloud.ai.entity.AgentKnowledge;
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
 * Agent Knowledge Service Class
 */
@Service
public class AgentKnowledgeService {

	@Autowired
	private JdbcTemplate jdbcTemplate;

	/**
	 * Query knowledge list by agent ID
	 */
	public List<AgentKnowledge> getKnowledgeByAgentId(Integer agentId) {
		String sql = "SELECT * FROM agent_knowledge WHERE agent_id = ? ORDER BY create_time DESC";
		return jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(AgentKnowledge.class), agentId);
	}

	/**
	 * Query knowledge details by ID
	 */
	public AgentKnowledge getKnowledgeById(Integer id) {
		String sql = "SELECT * FROM agent_knowledge WHERE id = ?";
		List<AgentKnowledge> results = jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(AgentKnowledge.class), id);
		return results.isEmpty() ? null : results.get(0);
	}

	/**
	 * Create knowledge
	 */
	public AgentKnowledge createKnowledge(AgentKnowledge knowledge) {
		String sql = "INSERT INTO agent_knowledge (agent_id, title, content, type, category, tags, status, "
				+ "source_url, file_path, file_size, file_type, embedding_status, creator_id, create_time, update_time) "
				+ "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

		KeyHolder keyHolder = new GeneratedKeyHolder();
		LocalDateTime now = LocalDateTime.now();

		jdbcTemplate.update(connection -> {
			PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
			ps.setObject(1, knowledge.getAgentId());
			ps.setString(2, knowledge.getTitle());
			ps.setString(3, knowledge.getContent());
			ps.setString(4, knowledge.getType() != null ? knowledge.getType() : "document");
			ps.setString(5, knowledge.getCategory());
			ps.setString(6, knowledge.getTags());
			ps.setString(7, knowledge.getStatus() != null ? knowledge.getStatus() : "active");
			ps.setString(8, knowledge.getSourceUrl());
			ps.setString(9, knowledge.getFilePath());
			ps.setObject(10, knowledge.getFileSize());
			ps.setString(11, knowledge.getFileType());
			ps.setString(12, knowledge.getEmbeddingStatus() != null ? knowledge.getEmbeddingStatus() : "pending");
			ps.setObject(13, knowledge.getCreatorId());
			ps.setObject(14, now);
			ps.setObject(15, now);
			return ps;
		}, keyHolder);

		Integer generatedId = keyHolder.getKey().intValue();
		knowledge.setId(generatedId);
		knowledge.setCreateTime(now);
		knowledge.setUpdateTime(now);

		return knowledge;
	}

	/**
	 * Update knowledge
	 */
	public AgentKnowledge updateKnowledge(Integer id, AgentKnowledge knowledge) {
		String sql = "UPDATE agent_knowledge SET title = ?, content = ?, type = ?, category = ?, tags = ?, "
				+ "status = ?, source_url = ?, file_path = ?, file_size = ?, file_type = ?, "
				+ "embedding_status = ?, update_time = ? WHERE id = ?";

		LocalDateTime now = LocalDateTime.now();

		int updatedRows = jdbcTemplate.update(sql, knowledge.getTitle(), knowledge.getContent(), knowledge.getType(),
				knowledge.getCategory(), knowledge.getTags(), knowledge.getStatus(), knowledge.getSourceUrl(),
				knowledge.getFilePath(), knowledge.getFileSize(), knowledge.getFileType(),
				knowledge.getEmbeddingStatus(), now, id);

		if (updatedRows > 0) {
			knowledge.setId(id);
			knowledge.setUpdateTime(now);
			return knowledge;
		}
		return null;
	}

	/**
	 * Delete knowledge
	 */
	public boolean deleteKnowledge(Integer id) {
		String sql = "DELETE FROM agent_knowledge WHERE id = ?";
		int deletedRows = jdbcTemplate.update(sql, id);
		return deletedRows > 0;
	}

	/**
	 * Query knowledge list by type
	 */
	public List<AgentKnowledge> getKnowledgeByType(Integer agentId, String type) {
		String sql = "SELECT * FROM agent_knowledge WHERE agent_id = ? AND type = ? ORDER BY create_time DESC";
		return jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(AgentKnowledge.class), agentId, type);
	}

	/**
	 * Query knowledge list by status
	 */
	public List<AgentKnowledge> getKnowledgeByStatus(Integer agentId, String status) {
		String sql = "SELECT * FROM agent_knowledge WHERE agent_id = ? AND status = ? ORDER BY create_time DESC";
		return jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(AgentKnowledge.class), agentId, status);
	}

	/**
	 * Search knowledge
	 */
	public List<AgentKnowledge> searchKnowledge(Integer agentId, String keyword) {
		String sql = "SELECT * FROM agent_knowledge WHERE agent_id = ? AND "
				+ "(title LIKE ? OR content LIKE ? OR tags LIKE ?) ORDER BY create_time DESC";
		String searchPattern = "%" + keyword + "%";
		return jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(AgentKnowledge.class), agentId, searchPattern,
				searchPattern, searchPattern);
	}

	/**
	 * Batch update knowledge status
	 */
	public int batchUpdateStatus(List<Integer> ids, String status) {
		String sql = "UPDATE agent_knowledge SET status = ?, update_time = ? WHERE id = ?";
		LocalDateTime now = LocalDateTime.now();

		int totalUpdated = 0;
		for (Integer id : ids) {
			totalUpdated += jdbcTemplate.update(sql, status, now, id);
		}
		return totalUpdated;
	}

	/**
	 * Count agent knowledge
	 */
	public int countKnowledgeByAgent(Integer agentId) {
		String sql = "SELECT COUNT(*) FROM agent_knowledge WHERE agent_id = ?";
		return jdbcTemplate.queryForObject(sql, Integer.class, agentId);
	}

	/**
	 * Count knowledge by types
	 */
	public List<Object[]> countKnowledgeByType(Integer agentId) {
		String sql = "SELECT type, COUNT(*) as count FROM agent_knowledge WHERE agent_id = ? GROUP BY type";
		return jdbcTemplate.query(sql, (rs, rowNum) -> new Object[] { rs.getString("type"), rs.getInt("count") },
				agentId);
	}

}
