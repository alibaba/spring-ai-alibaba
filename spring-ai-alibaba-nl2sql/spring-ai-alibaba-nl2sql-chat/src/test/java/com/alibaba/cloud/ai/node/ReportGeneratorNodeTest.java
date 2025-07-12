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

package com.alibaba.cloud.ai.node;

import com.alibaba.cloud.ai.dbconnector.bo.ResultSetBO;
import com.alibaba.cloud.ai.schema.ExecutionStep;
import com.alibaba.cloud.ai.schema.Plan;
import com.alibaba.cloud.ai.util.StepResultUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.alibaba.cloud.ai.constant.Constant.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 报告生成节点测试 注：本测试类主要用于展示ReportGeneratorNode的数据准备和汇总逻辑， 不包含实际的AI调用，因此没有实际生成报告内容。
 */
class ReportGeneratorNodeTest {

	private final ObjectMapper objectMapper = new ObjectMapper();

	@TempDir
	Path tempDir;

	@Test
	void testReportDataPreparation() throws Exception {
		// 准备测试数据，包括SQL查询结果和Python分析结果
		// 模拟输入数据
		String userQuery = "分析销售数据在各地区的分布情况和趋势";
		Plan plan = createTestPlan();
		Map<String, String> results = createTestResults();

		// 打印模拟数据内容（代替实际报告生成）
		System.out.println("===== 报告数据准备示例 =====");
		System.out.println("用户问题: " + userQuery);
		System.out.println("\n执行计划思路: " + plan.getThoughtProcess());

		// 打印执行步骤
		System.out.println("\n执行步骤:");
		for (ExecutionStep step : plan.getExecutionPlan()) {
			System.out.println("步骤 " + step.getStep() + ": " + step.getToolParameters().getDescription() + " ["
					+ step.getToolToUse() + "]");
		}

		// 打印结果示例
		System.out.println("\n结果数据示例:");
		for (Map.Entry<String, String> entry : results.entrySet()) {
			String key = entry.getKey();
			String value = entry.getValue();
			System.out.println(key + ": " + (value.length() > 100 ? value.substring(0, 100) + "..." : value));
		}

		// 打印图表路径
		List<String> chartPaths = extractChartPaths(results);
		System.out.println("\n检测到的图表路径:");
		for (String path : chartPaths) {
			System.out.println("- " + path);
		}

		// 验证数据完整性
		assertFalse(plan.getExecutionPlan().isEmpty(), "执行计划不应为空");
		assertFalse(results.isEmpty(), "结果数据不应为空");
		assertFalse(chartPaths.isEmpty(), "应检测到至少一个图表路径");
	}

	/**
	 * 从结果中提取图表路径
	 */
	private List<String> extractChartPaths(Map<String, String> results) {
		List<String> chartPaths = new ArrayList<>();

		for (String value : results.values()) {
			if (value.contains(".png") || value.contains(".jpg") || value.contains("![")
					|| value.contains("data:image")) {

				// 简化版的图表路径提取逻辑
				if (value.contains("图表已保存至")) {
					int startIdx = value.indexOf("图表已保存至") + "图表已保存至".length();
					int endIdx = value.indexOf(".png", startIdx) + 4;
					if (endIdx > startIdx) {
						chartPaths.add(value.substring(startIdx, endIdx).trim());
					}
				}

				// 提取Markdown图片格式
				if (value.contains("![")) {
					int startIdx = value.indexOf("![");
					int endIdx = value.indexOf(")", startIdx) + 1;
					if (endIdx > startIdx) {
						chartPaths.add(value.substring(startIdx, endIdx).trim());
					}
				}
			}
		}

		return chartPaths;
	}

	/**
	 * 创建测试用的执行计划
	 */
	private Plan createTestPlan() {
		List<ExecutionStep> executionPlan = new ArrayList<>();

		// 步骤1：SQL查询 - 获取各地区销售数据
		ExecutionStep step1 = new ExecutionStep();
		step1.setStep(1);
		step1.setToolToUse(SQL_EXECUTE_NODE);
		ExecutionStep.ToolParameters params1 = new ExecutionStep.ToolParameters();
		params1.setDescription("获取各地区销售数据");
		params1.setSqlQuery(
				"SELECT region, SUM(sales) AS total_sales FROM sales_data GROUP BY region ORDER BY total_sales DESC");
		step1.setToolParameters(params1);
		executionPlan.add(step1);

		// 步骤2：SQL查询 - 获取各地区月度销售趋势
		ExecutionStep step2 = new ExecutionStep();
		step2.setStep(2);
		step2.setToolToUse(SQL_EXECUTE_NODE);
		ExecutionStep.ToolParameters params2 = new ExecutionStep.ToolParameters();
		params2.setDescription("获取各地区月度销售趋势");
		params2.setSqlQuery(
				"SELECT region, month, SUM(sales) AS monthly_sales FROM sales_data GROUP BY region, month ORDER BY region, month");
		step2.setToolParameters(params2);
		executionPlan.add(step2);

		// 步骤3：Python分析 - 生成销售分布饼图
		ExecutionStep step3 = new ExecutionStep();
		step3.setStep(3);
		step3.setToolToUse(PYTHON_EXECUTE_NODE);
		ExecutionStep.ToolParameters params3 = new ExecutionStep.ToolParameters();
		params3.setDescription("生成销售分布饼图");
		params3.setInstruction("使用步骤1的数据生成一个饼图，展示各地区销售额占比");
		params3.setInputDataDescription("步骤1中的区域销售总额数据");
		step3.setToolParameters(params3);
		executionPlan.add(step3);

		// 步骤4：Python分析 - 生成月度销售趋势图
		ExecutionStep step4 = new ExecutionStep();
		step4.setStep(4);
		step4.setToolToUse(PYTHON_EXECUTE_NODE);
		ExecutionStep.ToolParameters params4 = new ExecutionStep.ToolParameters();
		params4.setDescription("生成月度销售趋势图");
		params4.setInstruction("使用步骤2的数据生成一个折线图，展示各地区月度销售额趋势");
		params4.setInputDataDescription("步骤2中的区域月度销售数据");
		step4.setToolParameters(params4);
		executionPlan.add(step4);

		// 步骤5：汇总报告
		ExecutionStep step5 = new ExecutionStep();
		step5.setStep(5);
		step5.setToolToUse(REPORT_GENERATOR_NODE);
		ExecutionStep.ToolParameters params5 = new ExecutionStep.ToolParameters();
		params5.setDescription("生成销售数据分析报告");
		params5.setSummaryAndRecommendations("根据分析结果，华东地区销售额最高，建议加大对华东地区的市场投入；西南地区增长速度最快，应密切关注并开发潜力市场。");
		step5.setToolParameters(params5);
		executionPlan.add(step5);

		// 创建计划
		Plan plan = new Plan();
		plan.setThoughtProcess("首先分析各地区销售总额分布，再分析各地区销售月度趋势，然后生成可视化图表，最后汇总形成分析报告。");
		plan.setExecutionPlan(executionPlan);

		return plan;
	}

	/**
	 * 创建测试用的结果数据
	 */
	private Map<String, String> createTestResults() throws Exception {
		Map<String, String> results = new HashMap<>();

		// 步骤1的SQL结果 - 各地区销售数据
		ResultSetBO resultSet1 = new ResultSetBO();
		List<String> columns1 = List.of("region", "total_sales");
		resultSet1.setColumn(columns1);

		List<Map<String, String>> data1 = new ArrayList<>();
		data1.add(createRow("华东", "1500000"));
		data1.add(createRow("华南", "1200000"));
		data1.add(createRow("华北", "950000"));
		data1.add(createRow("西南", "780000"));
		data1.add(createRow("西北", "420000"));
		resultSet1.setData(data1);

		// 步骤2的SQL结果 - 各地区月度销售趋势
		ResultSetBO resultSet2 = new ResultSetBO();
		List<String> columns2 = List.of("region", "month", "monthly_sales");
		resultSet2.setColumn(columns2);

		List<Map<String, String>> data2 = new ArrayList<>();
		// 华东地区数据
		data2.add(createRow(Map.of("region", "华东", "month", "1", "monthly_sales", "105000")));
		data2.add(createRow(Map.of("region", "华东", "month", "2", "monthly_sales", "118000")));
		data2.add(createRow(Map.of("region", "华东", "month", "3", "monthly_sales", "132000")));
		// 华南地区数据
		data2.add(createRow(Map.of("region", "华南", "month", "1", "monthly_sales", "95000")));
		data2.add(createRow(Map.of("region", "华南", "month", "2", "monthly_sales", "105000")));
		data2.add(createRow(Map.of("region", "华南", "month", "3", "monthly_sales", "112000")));
		resultSet2.setData(data2);

		// 保存SQL结果到步骤结果中
		results = StepResultUtils.addStepResult(results, 1, objectMapper.writeValueAsString(resultSet1));
		results = StepResultUtils.addStepResult(results, 2, objectMapper.writeValueAsString(resultSet2));

		// 步骤3的Python分析结果 - 销售分布饼图
		// 创建一个临时文件作为图表
		Path chartPath1 = createTempChartFile("sales_distribution_pie.png");
		String pythonResult1 = "分析结果：\n" + "根据销售数据分析，华东地区占总销售额的30.9%，是销售额最高的地区；" + "其次是华南地区，占总销售额的24.7%；华北地区占19.6%；"
				+ "西南和西北地区分别占16.1%和8.7%。\n" + "图表已保存至：" + chartPath1.toString();
		results = StepResultUtils.addStepResult(results, 3, pythonResult1);

		// 步骤4的Python分析结果 - 月度销售趋势图
		Path chartPath2 = createTempChartFile("monthly_sales_trend.png");
		String pythonResult2 = "分析结果：\n" + "从月度销售趋势来看，所有地区的销售额在1月到3月期间均呈上升趋势。" + "华东地区的增长速率最高，三个月增长了25.7%；"
				+ "华南地区三个月增长了17.9%；\n" + "以下是具体的月度同比增长率：\n" + "- 华东：1月到2月增长12.4%，2月到3月增长11.9%\n"
				+ "- 华南：1月到2月增长10.5%，2月到3月增长6.7%\n" + "\n月度销售趋势图已生成：" + chartPath2.toString() + "\n"
				+ "![月度销售趋势图](data:image/png;base64,iVBORw0KGgoAAA...省略base64数据...)";
		results = StepResultUtils.addStepResult(results, 4, pythonResult2);

		return results;
	}

	/**
	 * 创建一个测试用的临时图表文件
	 */
	private Path createTempChartFile(String fileName) throws IOException {
		Path filePath = tempDir.resolve(fileName);
		// 创建一个空文件
		Files.createFile(filePath);
		return filePath;
	}

	/**
	 * 创建一行数据
	 */
	private Map<String, String> createRow(String key, String value) {
		Map<String, String> row = new HashMap<>();
		row.put("region", key);
		row.put("total_sales", value);
		return row;
	}

	/**
	 * 创建一行数据（多列）
	 */
	private Map<String, String> createRow(Map<String, String> values) {
		return new HashMap<>(values);
	}

}