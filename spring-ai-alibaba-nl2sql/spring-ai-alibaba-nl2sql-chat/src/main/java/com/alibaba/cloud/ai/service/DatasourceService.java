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
import com.alibaba.cloud.ai.mapper.DatasourceMapper;
import com.alibaba.cloud.ai.mapper.AgentDatasourceMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
	private DatasourceMapper datasourceMapper;

	@Autowired
	private AgentDatasourceMapper agentDatasourceMapper;

	@Autowired
	private DBConnectionPoolContext dbConnectionPoolContext;

	/**
	 * Get all data source list
	 */
	public List<Datasource> getAllDatasources() {
		return datasourceMapper.selectList(Wrappers.<Datasource>lambdaQuery().orderByDesc(Datasource::getCreateTime));
	}

	/**
	 * Get data source list by status
	 */
	public List<Datasource> getDatasourcesByStatus(String status) {
		return datasourceMapper.selectByStatus(status);
	}

	/**
	 * Get data source list by type
	 */
	public List<Datasource> getDatasourcesByType(String type) {
		return datasourceMapper.selectByType(type);
	}

	/**
	 * Get data source details by ID
	 */
	public Datasource getDatasourceById(Integer id) {
		return datasourceMapper.selectById(id);
	}

	/**
	 * Create data source
	 */
	public Datasource createDatasource(Datasource datasource) {
		// Generate connection URL
		datasource.generateConnectionUrl();

		// Set default values
		if (datasource.getStatus() == null) {
			datasource.setStatus("active");
		}
		if (datasource.getTestStatus() == null) {
			datasource.setTestStatus("unknown");
		}

		datasourceMapper.insert(datasource);
		return datasource;
	}

	/**
	 * Update data source
	 */
	public Datasource updateDatasource(Integer id, Datasource datasource) {
		// Regenerate connection URL
		datasource.generateConnectionUrl();
		datasource.setId(id);

		datasourceMapper.updateById(datasource);
		return datasource;
	}

	/**
	 * Delete data source
	 */
	@Transactional
	public void deleteDatasource(Integer id) {
		// First, delete the associations
		agentDatasourceMapper.delete(Wrappers.<AgentDatasource>lambdaQuery().eq(AgentDatasource::getDatasourceId, id));

		// Then, delete the data source
		datasourceMapper.deleteById(id);
	}

	/**
	 * Update data source test status
	 */
	public void updateTestStatus(Integer id, String testStatus) {
		datasourceMapper.update(null,
				Wrappers.<Datasource>lambdaUpdate()
					.eq(Datasource::getId, id)
					.set(Datasource::getTestStatus, testStatus));
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
		List<AgentDatasource> agentDatasources = agentDatasourceMapper.selectByAgentIdWithDatasource(agentId);

		// Manually fill in the data source information (since MyBatis Plus does not
		// directly support complex join query result mapping)
		for (AgentDatasource agentDatasource : agentDatasources) {
			if (agentDatasource.getDatasourceId() != null) {
				Datasource datasource = datasourceMapper.selectById(agentDatasource.getDatasourceId());
				agentDatasource.setDatasource(datasource);
			}
		}

		return agentDatasources;
	}

	/**
	 * Add data source to agent
	 */
	@Transactional
	public AgentDatasource addDatasourceToAgent(Integer agentId, Integer datasourceId) {
		// First, disable other data sources for this agent (an agent can only have one
		// enabled data source)
		agentDatasourceMapper.disableAllByAgentId(agentId);

		// Check if an association already exists
		AgentDatasource existing = agentDatasourceMapper.selectByAgentIdAndDatasourceId(agentId, datasourceId);

		if (existing != null) {
			// If it exists, activate the association
			agentDatasourceMapper.update(null,
					Wrappers.<AgentDatasource>lambdaUpdate()
						.eq(AgentDatasource::getAgentId, agentId)
						.eq(AgentDatasource::getDatasourceId, datasourceId)
						.set(AgentDatasource::getIsActive, 1));

			// Query and return the updated association
			return agentDatasourceMapper.selectByAgentIdAndDatasourceId(agentId, datasourceId);
		}
		else {
			// If it does not exist, create a new association
			AgentDatasource agentDatasource = new AgentDatasource(agentId, datasourceId);
			agentDatasource.setIsActive(1);
			agentDatasourceMapper.insert(agentDatasource);
			return agentDatasource;
		}
	}

	/**
	 * Remove data source association from agent
	 */
	public void removeDatasourceFromAgent(Integer agentId, Integer datasourceId) {
		agentDatasourceMapper.delete(Wrappers.<AgentDatasource>lambdaQuery()
			.eq(AgentDatasource::getAgentId, agentId)
			.eq(AgentDatasource::getDatasourceId, datasourceId));
	}

	/**
	 * 启用/禁用智能体的数据源
	 */
	public AgentDatasource toggleDatasourceForAgent(Integer agentId, Integer datasourceId, Boolean isActive) {
		// If enabling data source, first check if there are other enabled data sources
		if (isActive) {
			int activeCount = agentDatasourceMapper.countActiveByAgentIdExcluding(agentId, datasourceId);
			if (activeCount > 0) {
				throw new RuntimeException("同一智能体下只能启用一个数据源，请先禁用其他数据源后再启用此数据源");
			}
		}

		// Update data source status
		int updated = agentDatasourceMapper.update(null,
				Wrappers.<AgentDatasource>lambdaUpdate()
					.eq(AgentDatasource::getAgentId, agentId)
					.eq(AgentDatasource::getDatasourceId, datasourceId)
					.set(AgentDatasource::getIsActive, isActive ? 1 : 0));

		if (updated == 0) {
			throw new RuntimeException("未找到相关的数据源关联记录");
		}

		// Return the updated association record
		return agentDatasourceMapper.selectByAgentIdAndDatasourceId(agentId, datasourceId);
	}

	/**
	 * Get data source statistics
	 */
	public Map<String, Object> getDatasourceStats() {
		Map<String, Object> stats = new HashMap<>();

		// Total count statistics
		Long total = datasourceMapper.selectCount(null);
		stats.put("total", total);

		// Statistics by status
		List<Map<String, Object>> statusStats = datasourceMapper.selectStatusStats();
		stats.put("byStatus", statusStats);

		// Statistics by type
		List<Map<String, Object>> typeStats = datasourceMapper.selectTypeStats();
		stats.put("byType", typeStats);

		// Connection status statistics
		List<Map<String, Object>> testStats = datasourceMapper.selectTestStatusStats();
		stats.put("byTestStatus", testStats);

		return stats;
	}

}
