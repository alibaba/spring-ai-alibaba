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

import com.alibaba.cloud.ai.connector.DBConnectionPool;
import com.alibaba.cloud.ai.connector.DBConnectionPoolContext;
import com.alibaba.cloud.ai.connector.config.DbConfig;
import com.alibaba.cloud.ai.entity.Datasource;
import com.alibaba.cloud.ai.entity.AgentDatasource;
import com.alibaba.cloud.ai.enums.ErrorCodeEnum;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

/**
 * Data Source Service Class
 *
 * @author Alibaba Cloud AI
 */
@Service
public class DatasourceService {

	private static final Logger log = LoggerFactory.getLogger(DatasourceService.class);

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private DBConnectionPoolContext dbConnectionPoolContext;

	/**
	 * Get all data source list
	 */
	public List<Datasource> getAllDatasources() {
		String sql = "SELECT * FROM datasource ORDER BY create_time DESC";
		return jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(Datasource.class));
	}

	/**
	 * Get data source list by status
	 */
	public List<Datasource> getDatasourcesByStatus(String status) {
		String sql = "SELECT * FROM datasource WHERE status = ? ORDER BY create_time DESC";
		return jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(Datasource.class), status);
	}

	/**
	 * Get data source list by type
	 */
	public List<Datasource> getDatasourcesByType(String type) {
		String sql = "SELECT * FROM datasource WHERE type = ? ORDER BY create_time DESC";
		return jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(Datasource.class), type);
	}

	/**
	 * Get data source details by ID
	 */
	public Datasource getDatasourceById(Integer id) {
		try {
			String sql = "SELECT * FROM datasource WHERE id = ?";
			return jdbcTemplate.queryForObject(sql, new BeanPropertyRowMapper<>(Datasource.class), id);
		}
		catch (EmptyResultDataAccessException e) {
			return null;
		}
	}

	/**
	 * Create data source
	 */
	public Datasource createDatasource(Datasource datasource) {
		// Generate connection URL
		datasource.generateConnectionUrl();

		String sql = "INSERT INTO datasource (name, type, host, port, database_name, username, password, "
				+ "connection_url, status, test_status, description, creator_id) "
				+ "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

		KeyHolder keyHolder = new GeneratedKeyHolder();
		jdbcTemplate.update(connection -> {
			PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
			ps.setString(1, datasource.getName());
			ps.setString(2, datasource.getType());
			ps.setString(3, datasource.getHost());
			ps.setInt(4, datasource.getPort());
			ps.setString(5, datasource.getDatabaseName());
			ps.setString(6, datasource.getUsername());
			ps.setString(7, datasource.getPassword()); // Note: Encryption is needed in
														// actual applications
			ps.setString(8, datasource.getConnectionUrl());
			ps.setString(9, datasource.getStatus() != null ? datasource.getStatus() : "active");
			ps.setString(10, datasource.getTestStatus() != null ? datasource.getTestStatus() : "unknown");
			ps.setString(11, datasource.getDescription());
			ps.setObject(12, datasource.getCreatorId());
			return ps;
		}, keyHolder);

		Number key = keyHolder.getKey();
		if (key != null) {
			datasource.setId(key.intValue());
		}
		return datasource;
	}

	/**
	 * Update data source
	 */
	public Datasource updateDatasource(Integer id, Datasource datasource) {
		// Regenerate connection URL
		datasource.generateConnectionUrl();

		String sql = "UPDATE datasource SET name = ?, type = ?, host = ?, port = ?, database_name = ?, "
				+ "username = ?, password = ?, connection_url = ?, status = ?, description = ? " + "WHERE id = ?";

		jdbcTemplate.update(sql, datasource.getName(), datasource.getType(), datasource.getHost(), datasource.getPort(),
				datasource.getDatabaseName(), datasource.getUsername(), datasource.getPassword(),
				datasource.getConnectionUrl(), datasource.getStatus(), datasource.getDescription(), id);

		datasource.setId(id);
		return datasource;
	}

	/**
	 * Delete data source
	 */
	@Transactional
	public void deleteDatasource(Integer id) {
		// First delete the association
		String deleteRelationSql = "DELETE FROM agent_datasource WHERE datasource_id = ?";
		jdbcTemplate.update(deleteRelationSql, id);

		// Then delete the data source
		String deleteDatasourceSql = "DELETE FROM datasource WHERE id = ?";
		jdbcTemplate.update(deleteDatasourceSql, id);
	}

	/**
	 * Update data source test status
	 */
	public void updateTestStatus(Integer id, String testStatus) {
		String sql = "UPDATE datasource SET test_status = ? WHERE id = ?";
		jdbcTemplate.update(sql, testStatus, id);
	}

	/**
	 * Test data source connection
	 */
	public boolean testConnection(Integer id) {
		Datasource datasource = getDatasourceById(id);
		if (datasource == null) {
			return false;
		}
		try {
			// ping测试
			boolean connectionSuccess = realConnectionTest(datasource);
			log.info(datasource.getName() + " test connection result: " + connectionSuccess);
			// Update test status
			updateTestStatus(id, connectionSuccess ? "success" : "failed");

			return connectionSuccess;
		}
		catch (Exception e) {
			updateTestStatus(id, "failed");
			log.error("Error testing connection for datasource ID " + id + ": " + e.getMessage(), e);
			return false;
		}
	}

	/**
	 * Actual connection test method
	 */
	private boolean realConnectionTest(Datasource datasource) {
		// Convert Datasource to DbConfig
		DbConfig config = new DbConfig();
		String originalUrl = datasource.getConnectionUrl();

		// Check if URL contains serverTimezone parameter, add default timezone if not,
		// otherwise it will throw an exception
		if (StringUtils.isNotBlank(originalUrl)) {
			String lowerUrl = originalUrl.toLowerCase();

			if (!lowerUrl.contains("servertimezone=")) {
				if (originalUrl.contains("?")) {
					originalUrl += "&serverTimezone=Asia/Shanghai";
				}
				else {
					originalUrl += "?serverTimezone=Asia/Shanghai";
				}
			}

			// Check if it contains useSSL parameter, add useSSL=false if not
			if (!lowerUrl.contains("usessl=")) {
				if (originalUrl.contains("?")) {
					originalUrl += "&useSSL=false";
				}
				else {
					originalUrl += "?useSSL=false";
				}
			}
		}
		config.setUrl(originalUrl);
		config.setUsername(datasource.getUsername());
		config.setPassword(datasource.getPassword());

		DBConnectionPool pool = dbConnectionPoolContext.getPoolByType(datasource.getType());
		if (pool == null) {
			return false;
		}

		ErrorCodeEnum result = pool.ping(config);
		return result == ErrorCodeEnum.SUCCESS;

	}

	/**
	 * Get data source list associated with agent
	 */
	public List<AgentDatasource> getAgentDatasources(Integer agentId) {
		String sql = "SELECT ad.*, d.name, d.type, d.host, d.port, d.database_name, "
				+ "d.connection_url, d.username, d.password, d.status, d.test_status, d.description "
				+ "FROM agent_datasource ad " + "LEFT JOIN datasource d ON ad.datasource_id = d.id "
				+ "WHERE ad.agent_id = ? " + "ORDER BY ad.create_time DESC";

		return jdbcTemplate.query(sql, (rs, rowNum) -> {
			AgentDatasource agentDatasource = new AgentDatasource();
			agentDatasource.setId(rs.getInt("id"));
			agentDatasource.setAgentId(rs.getInt("agent_id"));
			agentDatasource.setDatasourceId(rs.getInt("datasource_id"));
			agentDatasource.setIsActive(rs.getInt("is_active"));
			agentDatasource.setCreateTime(rs.getTimestamp("create_time").toLocalDateTime());
			agentDatasource.setUpdateTime(rs.getTimestamp("update_time").toLocalDateTime());

			// Fill data source information
			Datasource datasource = new Datasource();
			datasource.setId(rs.getInt("datasource_id"));
			datasource.setName(rs.getString("name"));
			datasource.setType(rs.getString("type"));
			datasource.setHost(rs.getString("host"));
			datasource.setPort(rs.getInt("port"));
			datasource.setDatabaseName(rs.getString("database_name"));
			datasource.setConnectionUrl(rs.getString("connection_url"));
			datasource.setUsername(rs.getString("username"));
			datasource.setPassword(rs.getString("password"));
			datasource.setStatus(rs.getString("status"));
			datasource.setTestStatus(rs.getString("test_status"));
			datasource.setDescription(rs.getString("description"));

			agentDatasource.setDatasource(datasource);
			return agentDatasource;
		}, agentId);
	}

	/**
	 * Add data source for agent
	 */
	@Transactional
	public AgentDatasource addDatasourceToAgent(Integer agentId, Integer datasourceId) {
		// First disable other data sources of the agent (an agent can only enable one
		// data source)
		String disableOthersSql = "UPDATE agent_datasource SET is_active = 0 WHERE agent_id = ?";
		jdbcTemplate.update(disableOthersSql, agentId);

		// Check if association already exists
		String checkSql = "SELECT COUNT(*) FROM agent_datasource WHERE agent_id = ? AND datasource_id = ?";
		Integer count = jdbcTemplate.queryForObject(checkSql, Integer.class, agentId, datasourceId);

		if (count != null && count > 0) {
			// If exists, activate the association
			String activateSql = "UPDATE agent_datasource SET is_active = 1 WHERE agent_id = ? AND datasource_id = ?";
			jdbcTemplate.update(activateSql, agentId, datasourceId);

			// Query and return the updated association
			String selectSql = "SELECT * FROM agent_datasource WHERE agent_id = ? AND datasource_id = ?";
			return jdbcTemplate.queryForObject(selectSql, new BeanPropertyRowMapper<>(AgentDatasource.class), agentId,
					datasourceId);
		}
		else {
			// If not exists, create new association
			String insertSql = "INSERT INTO agent_datasource (agent_id, datasource_id, is_active) VALUES (?, ?, 1)";
			KeyHolder keyHolder = new GeneratedKeyHolder();

			jdbcTemplate.update(connection -> {
				PreparedStatement ps = connection.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS);
				ps.setInt(1, agentId);
				ps.setInt(2, datasourceId);
				return ps;
			}, keyHolder);

			AgentDatasource agentDatasource = new AgentDatasource(agentId, datasourceId);
			Number key = keyHolder.getKey();
			if (key != null) {
				agentDatasource.setId(key.intValue());
			}
			return agentDatasource;
		}
	}

	/**
	 * Remove data source association from agent
	 */
	public void removeDatasourceFromAgent(Integer agentId, Integer datasourceId) {
		String sql = "DELETE FROM agent_datasource WHERE agent_id = ? AND datasource_id = ?";
		jdbcTemplate.update(sql, agentId, datasourceId);
	}

	/**
	 * 启用/禁用智能体的数据源
	 */
	public AgentDatasource toggleDatasourceForAgent(Integer agentId, Integer datasourceId, Boolean isActive) {
		// If enabling data source, first check if there are other enabled data sources
		if (isActive) {
			String checkSql = "SELECT COUNT(*) FROM agent_datasource WHERE agent_id = ? AND is_active = 1 AND datasource_id != ?";
			Integer activeCount = jdbcTemplate.queryForObject(checkSql, Integer.class, agentId, datasourceId);
			if (activeCount != null && activeCount > 0) {
				throw new RuntimeException("同一智能体下只能启用一个数据源，请先禁用其他数据源后再启用此数据源");
			}
		}

		// Update data source status
		Integer activeValue = isActive ? 1 : 0;
		String updateSql = "UPDATE agent_datasource SET is_active = ? WHERE agent_id = ? AND datasource_id = ?";
		int updated = jdbcTemplate.update(updateSql, activeValue, agentId, datasourceId);

		if (updated == 0) {
			throw new RuntimeException("未找到相关的数据源关联记录");
		}

		// Return the updated association record
		String selectSql = "SELECT id, agent_id, datasource_id, is_active, create_time "
				+ "FROM agent_datasource WHERE agent_id = ? AND datasource_id = ?";
		return jdbcTemplate.queryForObject(selectSql, (rs, rowNum) -> {
			AgentDatasource agentDatasource = new AgentDatasource(rs.getInt("agent_id"), rs.getInt("datasource_id"));
			agentDatasource.setId(rs.getInt("id"));
			agentDatasource.setIsActive(rs.getInt("is_active"));
			if (rs.getTimestamp("create_time") != null) {
				agentDatasource.setCreateTime(rs.getTimestamp("create_time").toLocalDateTime());
			}
			return agentDatasource;
		}, agentId, datasourceId);
	}

	/**
	 * Get data source statistics
	 */
	public Map<String, Object> getDatasourceStats() {
		Map<String, Object> stats = new HashMap<>();

		// Total count statistics
		String totalSql = "SELECT COUNT(*) FROM datasource";
		Integer total = jdbcTemplate.queryForObject(totalSql, Integer.class);
		stats.put("total", total);

		// Statistics by status
		String statusSql = "SELECT status, COUNT(*) as count FROM datasource GROUP BY status";
		List<Map<String, Object>> statusStats = jdbcTemplate.queryForList(statusSql);
		stats.put("byStatus", statusStats);

		// Statistics by type
		String typeSql = "SELECT type, COUNT(*) as count FROM datasource GROUP BY type";
		List<Map<String, Object>> typeStats = jdbcTemplate.queryForList(typeSql);
		stats.put("byType", typeStats);

		// Connection status statistics
		String testStatusSql = "SELECT test_status, COUNT(*) as count FROM datasource GROUP BY test_status";
		List<Map<String, Object>> testStats = jdbcTemplate.queryForList(testStatusSql);
		stats.put("byTestStatus", testStats);

		return stats;
	}

}
