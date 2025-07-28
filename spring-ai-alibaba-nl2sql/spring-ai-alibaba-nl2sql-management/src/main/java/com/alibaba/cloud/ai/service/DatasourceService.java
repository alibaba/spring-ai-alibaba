package com.alibaba.cloud.ai.service;

import com.alibaba.cloud.ai.entity.Datasource;
import com.alibaba.cloud.ai.entity.AgentDatasource;
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
 * 数据源服务类
 *
 * @author Alibaba Cloud AI
 */
@Service
public class DatasourceService {

	@Autowired
	private JdbcTemplate jdbcTemplate;

	/**
	 * 获取所有数据源列表
	 */
	public List<Datasource> getAllDatasources() {
		String sql = "SELECT * FROM datasource ORDER BY create_time DESC";
		return jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(Datasource.class));
	}

	/**
	 * 根据状态获取数据源列表
	 */
	public List<Datasource> getDatasourcesByStatus(String status) {
		String sql = "SELECT * FROM datasource WHERE status = ? ORDER BY create_time DESC";
		return jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(Datasource.class), status);
	}

	/**
	 * 根据类型获取数据源列表
	 */
	public List<Datasource> getDatasourcesByType(String type) {
		String sql = "SELECT * FROM datasource WHERE type = ? ORDER BY create_time DESC";
		return jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(Datasource.class), type);
	}

	/**
	 * 根据ID获取数据源详情
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
	 * 创建数据源
	 */
	public Datasource createDatasource(Datasource datasource) {
		// 生成连接URL
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
			ps.setString(7, datasource.getPassword()); // 注意：实际应用中需要加密
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
	 * 更新数据源
	 */
	public Datasource updateDatasource(Integer id, Datasource datasource) {
		// 重新生成连接URL
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
	 * 删除数据源
	 */
	@Transactional
	public void deleteDatasource(Integer id) {
		// 先删除关联关系
		String deleteRelationSql = "DELETE FROM agent_datasource WHERE datasource_id = ?";
		jdbcTemplate.update(deleteRelationSql, id);

		// 再删除数据源
		String deleteDatasourceSql = "DELETE FROM datasource WHERE id = ?";
		jdbcTemplate.update(deleteDatasourceSql, id);
	}

	/**
	 * 更新数据源测试状态
	 */
	public void updateTestStatus(Integer id, String testStatus) {
		String sql = "UPDATE datasource SET test_status = ? WHERE id = ?";
		jdbcTemplate.update(sql, testStatus, id);
	}

	/**
	 * 测试数据源连接
	 */
	public boolean testConnection(Integer id) {
		Datasource datasource = getDatasourceById(id);
		if (datasource == null) {
			return false;
		}

		try {
			// 这里应该实际测试数据库连接
			// 为了简化，这里只是模拟测试
			boolean connectionSuccess = simulateConnectionTest(datasource);

			// 更新测试状态
			updateTestStatus(id, connectionSuccess ? "success" : "failed");

			return connectionSuccess;
		}
		catch (Exception e) {
			updateTestStatus(id, "failed");
			return false;
		}
	}

	/**
	 * 模拟连接测试（实际应用中应该真正连接数据库）
	 */
	private boolean simulateConnectionTest(Datasource datasource) {
		// 模拟连接测试逻辑
		// 实际应该根据数据源类型创建相应的数据库连接
		return !"192.168.1.102".equals(datasource.getHost()); // 模拟某个主机连接失败
	}

	/**
	 * 获取智能体关联的数据源列表
	 */
	public List<AgentDatasource> getAgentDatasources(Integer agentId) {
		String sql = "SELECT ad.*, d.name, d.type, d.host, d.port, d.database_name, "
				+ "d.connection_url, d.status, d.test_status, d.description " + "FROM agent_datasource ad "
				+ "LEFT JOIN datasource d ON ad.datasource_id = d.id " + "WHERE ad.agent_id = ? "
				+ "ORDER BY ad.create_time DESC";

		return jdbcTemplate.query(sql, (rs, rowNum) -> {
			AgentDatasource agentDatasource = new AgentDatasource();
			agentDatasource.setId(rs.getInt("id"));
			agentDatasource.setAgentId(rs.getInt("agent_id"));
			agentDatasource.setDatasourceId(rs.getInt("datasource_id"));
			agentDatasource.setIsActive(rs.getInt("is_active"));
			agentDatasource.setCreateTime(rs.getTimestamp("create_time").toLocalDateTime());
			agentDatasource.setUpdateTime(rs.getTimestamp("update_time").toLocalDateTime());

			// 填充数据源信息
			Datasource datasource = new Datasource();
			datasource.setId(rs.getInt("datasource_id"));
			datasource.setName(rs.getString("name"));
			datasource.setType(rs.getString("type"));
			datasource.setHost(rs.getString("host"));
			datasource.setPort(rs.getInt("port"));
			datasource.setDatabaseName(rs.getString("database_name"));
			datasource.setConnectionUrl(rs.getString("connection_url"));
			datasource.setStatus(rs.getString("status"));
			datasource.setTestStatus(rs.getString("test_status"));
			datasource.setDescription(rs.getString("description"));

			agentDatasource.setDatasource(datasource);
			return agentDatasource;
		}, agentId);
	}

	/**
	 * 为智能体添加数据源
	 */
	@Transactional
	public AgentDatasource addDatasourceToAgent(Integer agentId, Integer datasourceId) {
		// 先禁用该智能体的其他数据源（一个智能体只能启用一个数据源）
		String disableOthersSql = "UPDATE agent_datasource SET is_active = 0 WHERE agent_id = ?";
		jdbcTemplate.update(disableOthersSql, agentId);

		// 检查是否已存在关联
		String checkSql = "SELECT COUNT(*) FROM agent_datasource WHERE agent_id = ? AND datasource_id = ?";
		Integer count = jdbcTemplate.queryForObject(checkSql, Integer.class, agentId, datasourceId);

		if (count != null && count > 0) {
			// 如果已存在，则激活该关联
			String activateSql = "UPDATE agent_datasource SET is_active = 1 WHERE agent_id = ? AND datasource_id = ?";
			jdbcTemplate.update(activateSql, agentId, datasourceId);

			// 查询并返回更新后的关联
			String selectSql = "SELECT * FROM agent_datasource WHERE agent_id = ? AND datasource_id = ?";
			return jdbcTemplate.queryForObject(selectSql, new BeanPropertyRowMapper<>(AgentDatasource.class), agentId,
					datasourceId);
		}
		else {
			// 如果不存在，则创建新关联
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
	 * 移除智能体的数据源关联
	 */
	public void removeDatasourceFromAgent(Integer agentId, Integer datasourceId) {
		String sql = "DELETE FROM agent_datasource WHERE agent_id = ? AND datasource_id = ?";
		jdbcTemplate.update(sql, agentId, datasourceId);
	}

	/**
	 * 启用/禁用智能体的数据源
	 */
	public AgentDatasource toggleDatasourceForAgent(Integer agentId, Integer datasourceId, Boolean isActive) {
		// 如果要启用数据源，先检查是否已有其他启用的数据源
		if (isActive) {
			String checkSql = "SELECT COUNT(*) FROM agent_datasource WHERE agent_id = ? AND is_active = 1 AND datasource_id != ?";
			Integer activeCount = jdbcTemplate.queryForObject(checkSql, Integer.class, agentId, datasourceId);
			if (activeCount != null && activeCount > 0) {
				throw new RuntimeException("同一智能体下只能启用一个数据源，请先禁用其他数据源后再启用此数据源");
			}
		}

		// 更新数据源状态
		Integer activeValue = isActive ? 1 : 0;
		String updateSql = "UPDATE agent_datasource SET is_active = ? WHERE agent_id = ? AND datasource_id = ?";
		int updated = jdbcTemplate.update(updateSql, activeValue, agentId, datasourceId);

		if (updated == 0) {
			throw new RuntimeException("未找到相关的数据源关联记录");
		}

		// 返回更新后的关联记录
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
	 * 获取数据源统计信息
	 */
	public Map<String, Object> getDatasourceStats() {
		Map<String, Object> stats = new HashMap<>();

		// 总数统计
		String totalSql = "SELECT COUNT(*) FROM datasource";
		Integer total = jdbcTemplate.queryForObject(totalSql, Integer.class);
		stats.put("total", total);

		// 按状态统计
		String statusSql = "SELECT status, COUNT(*) as count FROM datasource GROUP BY status";
		List<Map<String, Object>> statusStats = jdbcTemplate.queryForList(statusSql);
		stats.put("byStatus", statusStats);

		// 按类型统计
		String typeSql = "SELECT type, COUNT(*) as count FROM datasource GROUP BY type";
		List<Map<String, Object>> typeStats = jdbcTemplate.queryForList(typeSql);
		stats.put("byType", typeStats);

		// 连接状态统计
		String testStatusSql = "SELECT test_status, COUNT(*) as count FROM datasource GROUP BY test_status";
		List<Map<String, Object>> testStats = jdbcTemplate.queryForList(testStatusSql);
		stats.put("byTestStatus", testStats);

		return stats;
	}

}
