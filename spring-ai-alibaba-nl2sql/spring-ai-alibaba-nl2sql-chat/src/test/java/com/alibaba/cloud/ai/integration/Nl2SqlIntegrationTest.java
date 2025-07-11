/*
 * Copyright 2025 the original author or authors.
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

package com.alibaba.cloud.ai.integration;

import com.alibaba.cloud.ai.dbconnector.DbConfig;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.node.ReportGeneratorNode;
import com.alibaba.cloud.ai.request.SchemaInitRequest;
import com.alibaba.cloud.ai.schema.ExecutionStep;
import com.alibaba.cloud.ai.schema.Plan;
import com.alibaba.cloud.ai.service.LlmService;
import com.alibaba.cloud.ai.service.base.BaseNl2SqlService;
import com.alibaba.cloud.ai.service.base.BaseSchemaService;
import com.alibaba.cloud.ai.service.base.BaseVectorStoreService;
import com.alibaba.cloud.ai.service.simple.SimpleNl2SqlService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.*;

import static com.alibaba.cloud.ai.constant.Constant.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * NL2SQL集成测试示例
 *
 * 该测试类展示了如何使用NL2SQL服务的核心功能
 *
 * @author Makoto
 */
@SpringBootTest
@ActiveProfiles("test")
public class Nl2SqlIntegrationTest {

	@MockBean
	private LlmService llmService;

	@Autowired
	private BaseVectorStoreService vectorStoreService;

	@Autowired
	private BaseSchemaService schemaService;

	@Autowired
	private BaseNl2SqlService nl2SqlService;

	@Autowired
	private DbConfig dbConfig;

	/**
     * 测试数据准备
     */
    @BeforeEach
    void setup() throws Exception {
        // 配置LLM服务模拟行为
        when(llmService.call(anyString())).thenReturn(
            "需求类型：《数据分析》\n需求内容：查询最近一个月的销售数据\n\n" +
            "```sql\nSELECT * FROM sales WHERE create_time >= DATE_SUB(NOW(), INTERVAL 1 MONTH);\n```"
        );

        when(llmService.callWithSystemPrompt(anyString(), anyString())).thenReturn(
            "```sql\nSELECT * FROM sales WHERE create_time >= DATE_SUB(NOW(), INTERVAL 1 MONTH);\n```"
        );

        // 初始化Schema
        SchemaInitRequest schemaInitRequest = new SchemaInitRequest();
        schemaInitRequest.setDbConfig(dbConfig);
        schemaInitRequest.setTables(Arrays.asList("sales", "products", "customers"));
        vectorStoreService.schema(schemaInitRequest);
    }

	/**
	 * 测试NL2SQL转换功能
	 */
	@Test
	void testNl2SqlConversion() throws Exception {
		// 准备测试数据
		String userQuery = "分析最近一个月的销售数据";

		// 执行NL2SQL转换
		String sql = nl2SqlService.nl2sql(userQuery);

		// 验证结果
		System.out.println("生成的SQL: " + sql);
		assertThat(sql).contains("SELECT");
		assertThat(sql).contains("FROM sales");
		assertThat(sql).contains("INTERVAL 1 MONTH");
	}

	/**
	 * 测试完整的查询执行流程
	 */
	@Test
	void testQueryExecution() throws Exception {
		// 准备测试数据
		String sql = "SELECT * FROM sales WHERE create_time >= DATE_SUB(NOW(), INTERVAL 1 MONTH)";

		// 执行SQL
		String result = nl2SqlService.executeSql(sql);

		// 验证结果
		System.out.println("SQL执行结果: " + result);
		assertThat(result).isNotBlank();
		assertThat(result).contains("|"); // Markdown格式的表格
	}

	/**
	 * 测试报告生成节点
	 */
	@Test
	void testReportGeneration() throws Exception {
		// 创建模拟的ReportGeneratorNode
		ReportGeneratorNode reportGeneratorNode = Mockito.mock(ReportGeneratorNode.class);

		// 准备模拟的结果
		String mockReport = "# 销售数据分析报告\n\n## 摘要\n销售数据分析显示，过去一个月共有2条销售记录，总销售额为37,001.25元。手机和电脑是主要销售产品。\n\n## 详细分析\n1. 产品销售情况：手机销售额12,000.50元，电脑销售额25,000.75元\n2. 销售时间分布：5月上旬和5月中旬各有一笔销售\n\n## 建议\n1. 增加产品多样性\n2. 加强营销力度，提高销售频次";
		Map<String, Object> mockResult = new HashMap<>();
		mockResult.put(RESULT, mockReport);

		// 设置模拟行为
		when(reportGeneratorNode.apply(Mockito.any(OverAllState.class))).thenReturn(mockResult);

		// 准备测试数据 - 创建模拟的OverAllState
		OverAllState state = new OverAllState();

		// 添加用户输入
		Map<String, Object> inputData = new HashMap<>();
		inputData.put(INPUT_KEY, "分析最近一个月的销售数据，生成销售报表");
		state.updateState(inputData);

		// 添加计划节点输出
		Map<String, Object> plannerOutput = new HashMap<>();
		plannerOutput.put(PLANNER_NODE_OUTPUT,
				"{\"thoughtProcess\":\"需要分析最近一个月的销售数据，首先查询销售表，然后生成报表\",\"executionPlan\":[{\"toolToUse\":\"sql_execute_node\",\"toolParameters\":{\"description\":\"查询最近一个月的销售数据\",\"sqlQuery\":\"SELECT * FROM sales WHERE create_time >= DATE_SUB(NOW(), INTERVAL 1 MONTH)\"}},{\"toolToUse\":\"report_generator_node\",\"toolParameters\":{\"description\":\"根据销售数据生成分析报表\",\"summaryAndRecommendations\":\"销售数据显示电脑销量高于手机，建议增加电脑库存\"}}]}");
		state.updateState(plannerOutput);

		// 添加SQL执行结果
		HashMap<String, String> executionResults = new HashMap<>();
		executionResults.put("step_1",
				"| product_id | product_name | sales_amount | create_time |\n| --- | --- | --- | --- |\n| 1 | 手机 | 12000.50 | 2023-05-01 10:20:30 |\n| 2 | 电脑 | 25000.75 | 2023-05-15 14:35:22 |");

		Map<String, Object> sqlOutput = new HashMap<>();
		sqlOutput.put(SQL_EXECUTE_NODE_OUTPUT, executionResults);
		state.updateState(sqlOutput);

		// 添加当前步骤
		Map<String, Object> stepData = new HashMap<>();
		stepData.put(PLAN_CURRENT_STEP, 2); // 第二步，即报告生成步骤
		state.updateState(stepData);

		// 执行ReportGeneratorNode
		Map<String, Object> result = reportGeneratorNode.apply(state);

		// 验证结果
		assertThat(result).containsKey(RESULT);
		String reportContent = (String) result.get(RESULT);
		System.out.println("生成的报告: \n" + reportContent);

		// 验证报告内容
		assertThat(reportContent).contains("销售数据分析");
		assertThat(reportContent).contains("手机");
		assertThat(reportContent).contains("电脑");
	}

	/**
	 * 测试配置
	 */
	@Configuration
	@Import({})
	static class TestConfig {

		@Bean
		public DbConfig dbConfig() {
			DbConfig dbConfig = new DbConfig();
			dbConfig.setUrl("jdbc:mysql://localhost:3306/test");
			dbConfig.setUsername("test");
			dbConfig.setPassword("test");
			dbConfig.setSchema("test");
			dbConfig.setConnectionType("jdbc");
			dbConfig.setDialectType("mysql");
			return dbConfig;
		}

		@Bean
		public BaseVectorStoreService simpleVectorStoreService() {
			return Mockito.mock(BaseVectorStoreService.class);
		}

		@Bean
		public BaseSchemaService simpleSchemaService(BaseVectorStoreService vectorStoreService) {
			return Mockito.mock(BaseSchemaService.class);
		}

		@Bean
		public BaseNl2SqlService nl2SqlServiceImpl(BaseVectorStoreService vectorStoreService,
				BaseSchemaService schemaService, LlmService llmService) {
			BaseNl2SqlService service = Mockito.mock(SimpleNl2SqlService.class);
			try {
				when(service.nl2sql(anyString()))
					.thenReturn("SELECT * FROM sales WHERE create_time >= DATE_SUB(NOW(), INTERVAL 1 MONTH)");
				when(service.executeSql(anyString()))
					.thenReturn("| product_id | product_name | sales_amount | create_time |\n"
							+ "| --- | --- | --- | --- |\n" + "| 1 | 手机 | 12000.50 | 2023-05-01 10:20:30 |\n"
							+ "| 2 | 电脑 | 25000.75 | 2023-05-15 14:35:22 |");
			}
			catch (Exception e) {
				throw new RuntimeException("初始化模拟服务失败", e);
			}
			return service;
		}

	}

}