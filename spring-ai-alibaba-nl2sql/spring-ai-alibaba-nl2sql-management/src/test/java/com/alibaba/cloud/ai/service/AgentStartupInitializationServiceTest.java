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
import com.alibaba.cloud.ai.entity.AgentDatasource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.ApplicationArguments;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * AgentStartupInitializationService 测试类
 */
@ExtendWith(MockitoExtension.class)
class AgentStartupInitializationServiceTest {

	@Mock
	private AgentService agentService;

	@Mock
	private AgentVectorService agentVectorService;

	@Mock
	private DatasourceService datasourceService;

	@Mock
	private ApplicationArguments applicationArguments;

	@InjectMocks
	private AgentStartupInitializationService agentStartupInitializationService;

	@Test
    void testRunWithNoPublishedAgents() throws Exception {
        // 配置没有已发布的智能体
        when(agentService.findByStatus("published")).thenReturn(new ArrayList<>());

        // 执行
        agentStartupInitializationService.run(applicationArguments);

        // 等待异步任务完成
        Thread.sleep(500);

        // 验证调用了查询方法
        verify(agentService).findByStatus("published");
    }

	@Test
	void testRunWithPublishedAgents() throws Exception {
		// 创建测试数据
		Agent agent1 = createTestAgent(1L, "Test Agent 1");
		Agent agent2 = createTestAgent(2L, "Test Agent 2");
		List<Agent> publishedAgents = List.of(agent1, agent2);

		// 配置mock行为
		when(agentService.findByStatus("published")).thenReturn(publishedAgents);

		// 配置agent1已有数据，应该跳过
		Map<String, Object> stats1 = new HashMap<>();
		stats1.put("hasData", true);
		stats1.put("documentCount", 10);
		when(agentVectorService.getVectorStatistics(1L)).thenReturn(stats1);

		// 配置agent2没有数据，需要初始化
		Map<String, Object> stats2 = new HashMap<>();
		stats2.put("hasData", false);
		stats2.put("documentCount", 0);
		when(agentVectorService.getVectorStatistics(2L)).thenReturn(stats2);

		// 配置agent2的数据源
		AgentDatasource agentDatasource = createTestAgentDatasource(2, 1);
		when(datasourceService.getAgentDatasources(2)).thenReturn(List.of(agentDatasource));

		// 配置数据源的表
		when(agentVectorService.getDatasourceTables(1)).thenReturn(List.of("table1", "table2"));

		// 配置初始化成功
		when(agentVectorService.initializeSchemaForAgentWithDatasource(2L, 1, List.of("table1", "table2")))
			.thenReturn(true);

		// 执行
		agentStartupInitializationService.run(applicationArguments);

		// 等待异步任务完成
		Thread.sleep(1000);

		// 验证调用
		verify(agentService).findByStatus("published");
		verify(agentVectorService).getVectorStatistics(1L);
		verify(agentVectorService).getVectorStatistics(2L);
		verify(datasourceService).getAgentDatasources(2);
		verify(agentVectorService).getDatasourceTables(1);
		verify(agentVectorService).initializeSchemaForAgentWithDatasource(2L, 1, List.of("table1", "table2"));
	}

	private Agent createTestAgent(Long id, String name) {
		Agent agent = new Agent();
		agent.setId(id);
		agent.setName(name);
		agent.setStatus("published");
		return agent;
	}

	private AgentDatasource createTestAgentDatasource(Integer agentId, Integer datasourceId) {
		AgentDatasource agentDatasource = new AgentDatasource();
		agentDatasource.setId(1);
		agentDatasource.setAgentId(agentId);
		agentDatasource.setDatasourceId(datasourceId);
		agentDatasource.setIsActive(1);
		return agentDatasource;
	}

}
