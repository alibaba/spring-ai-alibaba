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
 * 数据源服务类
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
	 * 获取所有数据源列表
	 */
	public List<Datasource> getAllDatasources() {
		return datasourceMapper.selectList(Wrappers.<Datasource>lambdaQuery().orderByDesc(Datasource::getCreateTime));
	}

	/**
	 * 根据状态获取数据源列表
	 */
	public List<Datasource> getDatasourcesByStatus(String status) {
		return datasourceMapper.selectByStatus(status);
	}

	/**
	 * 根据类型获取数据源列表
	 */
	public List<Datasource> getDatasourcesByType(String type) {
		return datasourceMapper.selectByType(type);
	}

	/**
	 * 根据ID获取数据源详情
	 */
	public Datasource getDatasourceById(Integer id) {
		return datasourceMapper.selectById(id);
	}

	/**
	 * 创建数据源
	 */
	public Datasource createDatasource(Datasource datasource) {
		// 生成连接URL
		datasource.generateConnectionUrl();

		// 设置默认值
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
	 * 更新数据源
	 */
	public Datasource updateDatasource(Integer id, Datasource datasource) {
		// 重新生成连接URL
		datasource.generateConnectionUrl();
		datasource.setId(id);

		datasourceMapper.updateById(datasource);
		return datasource;
	}

	/**
	 * 删除数据源
	 */
	@Transactional
	public void deleteDatasource(Integer id) {
		// 先删除关联关系
		agentDatasourceMapper.delete(Wrappers.<AgentDatasource>lambdaQuery().eq(AgentDatasource::getDatasourceId, id));

		// 再删除数据源
		datasourceMapper.deleteById(id);
	}

	/**
	 * 更新数据源测试状态
	 */
	public void updateTestStatus(Integer id, String testStatus) {
		datasourceMapper.update(null,
				Wrappers.<Datasource>lambdaUpdate()
					.eq(Datasource::getId, id)
					.set(Datasource::getTestStatus, testStatus));
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
			// ping测试
			boolean connectionSuccess = realConnectionTest(datasource);
			log.info(datasource.getName() + " test connection result: " + connectionSuccess);
			// 更新测试状态
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
	 * 实际的连接测试方法
	 */
	private boolean realConnectionTest(Datasource datasource) {
		// 把 Datasource 转成 DbConfig
		DbConfig config = new DbConfig();
		String originalUrl = datasource.getConnectionUrl();

		// 检查 URL 是否含有 serverTimezone 参数，如果没有则添加默认时区，否则会抛异常
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

			// 检查是否含有 useSSL 参数，如果没有则添加 useSSL=false
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
	 * 获取智能体关联的数据源列表
	 */
	public List<AgentDatasource> getAgentDatasources(Integer agentId) {
		List<AgentDatasource> agentDatasources = agentDatasourceMapper.selectByAgentIdWithDatasource(agentId);

		// 手动填充数据源信息（因为 MyBatis Plus 不直接支持复杂的联表查询结果映射）
		for (AgentDatasource agentDatasource : agentDatasources) {
			if (agentDatasource.getDatasourceId() != null) {
				Datasource datasource = datasourceMapper.selectById(agentDatasource.getDatasourceId());
				agentDatasource.setDatasource(datasource);
			}
		}

		return agentDatasources;
	}

	/**
	 * 为智能体添加数据源
	 */
	@Transactional
	public AgentDatasource addDatasourceToAgent(Integer agentId, Integer datasourceId) {
		// 先禁用该智能体的其他数据源（一个智能体只能启用一个数据源）
		agentDatasourceMapper.disableAllByAgentId(agentId);

		// 检查是否已存在关联
		AgentDatasource existing = agentDatasourceMapper.selectByAgentIdAndDatasourceId(agentId, datasourceId);

		if (existing != null) {
			// 如果已存在，则激活该关联
			agentDatasourceMapper.update(null,
					Wrappers.<AgentDatasource>lambdaUpdate()
						.eq(AgentDatasource::getAgentId, agentId)
						.eq(AgentDatasource::getDatasourceId, datasourceId)
						.set(AgentDatasource::getIsActive, 1));

			// 查询并返回更新后的关联
			return agentDatasourceMapper.selectByAgentIdAndDatasourceId(agentId, datasourceId);
		}
		else {
			// 如果不存在，则创建新关联
			AgentDatasource agentDatasource = new AgentDatasource(agentId, datasourceId);
			agentDatasource.setIsActive(1);
			agentDatasourceMapper.insert(agentDatasource);
			return agentDatasource;
		}
	}

	/**
	 * 移除智能体的数据源关联
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
		// 如果要启用数据源，先检查是否已有其他启用的数据源
		if (isActive) {
			int activeCount = agentDatasourceMapper.countActiveByAgentIdExcluding(agentId, datasourceId);
			if (activeCount > 0) {
				throw new RuntimeException("同一智能体下只能启用一个数据源，请先禁用其他数据源后再启用此数据源");
			}
		}

		// 更新数据源状态
		int updated = agentDatasourceMapper.update(null,
				Wrappers.<AgentDatasource>lambdaUpdate()
					.eq(AgentDatasource::getAgentId, agentId)
					.eq(AgentDatasource::getDatasourceId, datasourceId)
					.set(AgentDatasource::getIsActive, isActive ? 1 : 0));

		if (updated == 0) {
			throw new RuntimeException("未找到相关的数据源关联记录");
		}

		// 返回更新后的关联记录
		return agentDatasourceMapper.selectByAgentIdAndDatasourceId(agentId, datasourceId);
	}

	/**
	 * 获取数据源统计信息
	 */
	public Map<String, Object> getDatasourceStats() {
		Map<String, Object> stats = new HashMap<>();

		// 总数统计
		Long total = datasourceMapper.selectCount(null);
		stats.put("total", total);

		// 按状态统计
		List<Map<String, Object>> statusStats = datasourceMapper.selectStatusStats();
		stats.put("byStatus", statusStats);

		// 按类型统计
		List<Map<String, Object>> typeStats = datasourceMapper.selectTypeStats();
		stats.put("byType", typeStats);

		// 连接状态统计
		List<Map<String, Object>> testStats = datasourceMapper.selectTestStatusStats();
		stats.put("byTestStatus", testStats);

		return stats;
	}

}
