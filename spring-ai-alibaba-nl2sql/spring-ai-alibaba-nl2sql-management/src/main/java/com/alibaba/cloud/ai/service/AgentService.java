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

import com.alibaba.cloud.ai.entity.Agent;
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
 * 智能体服务类
 */
@Service
public class AgentService {

	@Autowired
	private JdbcTemplate jdbcTemplate;

	private static final String SELECT_ALL = """
			SELECT * FROM agent ORDER BY create_time DESC
			""";

	private static final String SELECT_BY_ID = """
			SELECT * FROM agent WHERE id = ?
			""";

	private static final String SELECT_BY_STATUS = """
			SELECT * FROM agent WHERE status = ? ORDER BY create_time DESC
			""";

	private static final String SEARCH_BY_KEYWORD = """
			SELECT * FROM agent 
			WHERE (name LIKE ? OR description LIKE ? OR tags LIKE ?) 
			ORDER BY create_time DESC
			""";

	private static final String INSERT = """
			INSERT INTO agent (name, description, avatar, status, prompt, category, admin_id, tags, create_time, update_time)
			VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
			""";

	private static final String UPDATE = """
			UPDATE agent SET name = ?, description = ?, avatar = ?, status = ?, prompt = ?, 
			category = ?, admin_id = ?, tags = ?, update_time = ? WHERE id = ?
			""";

	private static final String DELETE = """
			DELETE FROM agent WHERE id = ?
			""";

	public List<Agent> findAll() {
		return jdbcTemplate.query(SELECT_ALL, new BeanPropertyRowMapper<>(Agent.class));
	}

	public Agent findById(Long id) {
		List<Agent> results = jdbcTemplate.query(SELECT_BY_ID, new BeanPropertyRowMapper<>(Agent.class), id);
		return results.isEmpty() ? null : results.get(0);
	}

	public List<Agent> findByStatus(String status) {
		return jdbcTemplate.query(SELECT_BY_STATUS, new BeanPropertyRowMapper<>(Agent.class), status);
	}

	public List<Agent> search(String keyword) {
		String searchPattern = "%" + keyword + "%";
		return jdbcTemplate.query(SEARCH_BY_KEYWORD, new BeanPropertyRowMapper<>(Agent.class), searchPattern,
				searchPattern, searchPattern);
	}

	public Agent save(Agent agent) {
		LocalDateTime now = LocalDateTime.now();

		if (agent.getId() == null) {
			// 新增
			agent.setCreateTime(now);
			agent.setUpdateTime(now);

			KeyHolder keyHolder = new GeneratedKeyHolder();
			jdbcTemplate.update(connection -> {
				PreparedStatement ps = connection.prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS);
				ps.setString(1, agent.getName());
				ps.setString(2, agent.getDescription());
				ps.setString(3, agent.getAvatar());
				ps.setString(4, agent.getStatus());
				ps.setString(5, agent.getPrompt());
				ps.setString(6, agent.getCategory());
				ps.setObject(7, agent.getAdminId());
				ps.setString(8, agent.getTags());
				ps.setObject(9, agent.getCreateTime());
				ps.setObject(10, agent.getUpdateTime());
				return ps;
			}, keyHolder);

			Number key = keyHolder.getKey();
			if (key != null) {
				agent.setId(key.longValue());
			}
		}
		else {
			// 更新
			agent.setUpdateTime(now);
			jdbcTemplate.update(UPDATE, agent.getName(), agent.getDescription(), agent.getAvatar(),
					agent.getStatus(), agent.getPrompt(), agent.getCategory(), agent.getAdminId(), agent.getTags(),
					agent.getUpdateTime(), agent.getId());
		}

		return agent;
	}

	public void deleteById(Long id) {
		jdbcTemplate.update(DELETE, id);
	}

}
