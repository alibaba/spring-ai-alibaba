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

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.cloud.ai.prompt.PromptHelper;
import com.alibaba.cloud.ai.schema.ExecutionStep;
import com.alibaba.cloud.ai.schema.Plan;
import com.alibaba.cloud.ai.util.ChartUtils;
import com.alibaba.cloud.ai.util.StateUtils;
import com.alibaba.cloud.ai.util.TableUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.core.ParameterizedTypeReference;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static com.alibaba.cloud.ai.constant.Constant.*;

/**
 * 报告生成节点
 *
 * @author zhangshenghang
 */
public class ReportGeneratorNode implements NodeAction {

	private static final Logger logger = LoggerFactory.getLogger(ReportGeneratorNode.class);

	private static final Pattern CHART_PATH_PATTERN = Pattern.compile("!\\[.*?\\]\\((.*?)\\)");

	private static final Pattern TABLE_PATTERN = Pattern.compile("\\|.*?\\|[\\s\\S]*?\\|.*?\\|");

	private final ChatClient chatClient;

	private final BeanOutputConverter<Plan> converter;

	// 默认图表存储目录
	private static final String DEFAULT_CHARTS_DIR = "charts";

	// 报告生成时间格式
	private static final DateTimeFormatter REPORT_TIMESTAMP_FORMATTER = DateTimeFormatter
		.ofPattern("yyyy-MM-dd HH:mm:ss");

	public ReportGeneratorNode(ChatClient.Builder chatClientBuilder) {
		this.chatClient = chatClientBuilder.build();
		this.converter = new BeanOutputConverter<>(new ParameterizedTypeReference<Plan>() {
		});
	}

	@Override
	public Map<String, Object> apply(OverAllState state) throws Exception {
		logger.info("进入 {} 节点", this.getClass().getSimpleName());

		// 获取必要的输入参数
		String plannerNodeOutput = StateUtils.getStringValue(state, PLANNER_NODE_OUTPUT);
		String userInput = StateUtils.getStringValue(state, INPUT_KEY);
		Integer currentStep = StateUtils.getObjectValue(state, PLAN_CURRENT_STEP, Integer.class, 1);
		@SuppressWarnings("unchecked")
		HashMap<String, String> executionResults = StateUtils.getObjectValue(state, SQL_EXECUTE_NODE_OUTPUT,
				HashMap.class, new HashMap<>());

		String sqlException = StateUtils.getStringValue(state, SQL_EXECUTE_NODE_EXCEPTION_OUTPUT, "");

		String pythonResults = StateUtils.getStringValue(state, PYTHON_EXECUTE_NODE_OUTPUT, "");

		logger.info("计划节点输出: {}", plannerNodeOutput);

		// 解析计划并获取当前步骤
		Plan plan = converter.convert(plannerNodeOutput);

		Map<String, List<String>> mediaContent = collectAndProcessMediaContent(executionResults);

		ExecutionStep executionStep = getCurrentExecutionStep(plan, currentStep);
		String summaryAndRecommendations = (executionStep != null && executionStep.getToolParameters() != null)
				? executionStep.getToolParameters().getSummaryAndRecommendations() : "";

		// 构建报告
		String reportContent = generateReport(userInput, plan, executionResults, mediaContent,
				summaryAndRecommendations, sqlException, pythonResults);

		logger.info("生成的报告内容: {}", reportContent);

		return buildFinalResult(reportContent);
	}

	/**
	 * 收集并处理媒体内容（图表、表格等）
	 */
	private Map<String, List<String>> collectAndProcessMediaContent(HashMap<String, String> executionResults) {
		Map<String, List<String>> mediaContent = new HashMap<>();
		mediaContent.put("charts", new ArrayList<>());
		mediaContent.put("tables", new ArrayList<>());
		mediaContent.put("formattedTables", new ArrayList<>());

		for (Map.Entry<String, String> entry : executionResults.entrySet()) {
			String result = entry.getValue();
			if (result == null)
				continue;

			List<String> chartPaths = ChartUtils.extractChartPaths(result);
			if (!chartPaths.isEmpty()) {
				mediaContent.get("charts").addAll(chartPaths);
			}

			List<String> tables = TableUtils.extractTables(result);
			if (!tables.isEmpty()) {
				mediaContent.get("tables").addAll(tables);

				for (String table : tables) {
					String formattedTable = TableUtils.formatTable(table);
					if (!formattedTable.isEmpty()) {
						mediaContent.get("formattedTables").add(formattedTable);
					}
				}
			}

			// TODO：提取Mermaid图表定义并处理
			List<String> mermaidCharts = ChartUtils.extractMermaidCharts(result);
			if (!mermaidCharts.isEmpty()) {
				// TODO：可以在这里处理Mermaid图表，如将其转换为图片等
			}
		}

		return mediaContent;
	}

	/**
	 * 获取当前执行步骤
	 */
	private ExecutionStep getCurrentExecutionStep(Plan plan, Integer currentStep) {
		List<ExecutionStep> executionPlan = plan.getExecutionPlan();
		if (executionPlan == null || executionPlan.isEmpty()) {
			throw new IllegalStateException("执行计划为空");
		}

		int stepIndex = currentStep - 1;
		if (stepIndex < 0 || stepIndex >= executionPlan.size()) {
			throw new IllegalStateException("当前步骤索引超出范围: " + stepIndex);
		}

		return executionPlan.get(stepIndex);
	}

	/**
	 * 生成报告
	 */
	private String generateReport(String userInput, Plan plan, HashMap<String, String> executionResults,
			Map<String, List<String>> mediaContent, String summaryAndRecommendations, String sqlException,
			String pythonResults) {

		String userRequirementsAndPlan = buildUserRequirementsAndPlan(userInput, plan);

		String analysisStepsAndData = buildAnalysisStepsAndData(plan, executionResults, mediaContent, sqlException,
				pythonResults);

		// 添加报告元数据
		String metaInfo = generateReportMetaInfo();

		// 使用PromptHelper构建报告生成提示词
		String reportPrompt = PromptHelper.buildReportGeneratorPrompt(userRequirementsAndPlan,
				analysisStepsAndData + "\n\n" + metaInfo, summaryAndRecommendations);

		return chatClient.prompt().user(reportPrompt).call().content();
	}

	/**
	 * 生成报告元数据信息
	 */
	private String generateReportMetaInfo() {
		StringBuilder meta = new StringBuilder();
		meta.append("## 报告元数据\n");
		meta.append("- **生成时间**: ").append(LocalDateTime.now().format(REPORT_TIMESTAMP_FORMATTER)).append("\n");
		meta.append("- **报告版本**: 1.0\n");
		return meta.toString();
	}

	/**
	 * 构建用户需求和计划描述
	 */
	private String buildUserRequirementsAndPlan(String userInput, Plan plan) {
		StringBuilder sb = new StringBuilder();
		sb.append("## 用户原始需求\n");
		sb.append(userInput).append("\n\n");

		sb.append("## 执行计划概述\n");
		sb.append("**思考过程**: ").append(plan.getThoughtProcess()).append("\n\n");

		sb.append("## 详细执行步骤\n");
		List<ExecutionStep> executionPlan = plan.getExecutionPlan();
		for (int i = 0; i < executionPlan.size(); i++) {
			ExecutionStep step = executionPlan.get(i);
			sb.append("### 步骤 ").append(i + 1).append(": ").append(step.getStep()).append("\n");
			sb.append("**工具**: ").append(step.getToolToUse()).append("\n");
			if (step.getToolParameters() != null) {
				sb.append("**参数描述**: ").append(step.getToolParameters().getDescription()).append("\n");

				if (step.getToolParameters().getSqlQuery() != null
						&& !step.getToolParameters().getSqlQuery().trim().isEmpty()) {
					sb.append("**SQL查询**: \n```sql\n").append(step.getToolParameters().getSqlQuery()).append("\n```\n");
				}

				if (step.getToolParameters().getInstruction() != null
						&& !step.getToolParameters().getInstruction().trim().isEmpty()) {
					sb.append("**执行指令**: ").append(step.getToolParameters().getInstruction()).append("\n");
				}

				if (step.getToolParameters().getInputDataDescription() != null
						&& !step.getToolParameters().getInputDataDescription().trim().isEmpty()) {
					sb.append("**输入数据描述**: ").append(step.getToolParameters().getInputDataDescription()).append("\n");
				}
			}
			sb.append("\n");
		}

		return sb.toString();
	}

	/**
	 * 构建分析步骤和数据结果描述
	 */
	private String buildAnalysisStepsAndData(Plan plan, HashMap<String, String> executionResults,
			Map<String, List<String>> mediaContent, String sqlException, String pythonResults) {

		StringBuilder sb = new StringBuilder();
		sb.append("## 数据执行结果\n");

		if (executionResults.isEmpty() && sqlException.isEmpty() && pythonResults.isEmpty()) {
			sb.append("暂无执行结果数据\n");
		}
		else {
			if (!sqlException.isEmpty()) {
				sb.append("### SQL执行异常\n");
				sb.append("```\n").append(sqlException).append("\n```\n\n");
			}

			if (!pythonResults.isEmpty()) {
				sb.append("### Python执行结果\n");
				sb.append("```\n").append(pythonResults).append("\n```\n\n");
			}

			List<ExecutionStep> executionPlan = plan.getExecutionPlan();
			for (Map.Entry<String, String> entry : executionResults.entrySet()) {
				String stepKey = entry.getKey();
				String stepResult = entry.getValue();

				sb.append("### ").append(stepKey).append("\n");

				// 尝试获取对应步骤的描述
				try {
					int stepIndex = Integer.parseInt(stepKey.replace("step_", "")) - 1;
					if (stepIndex >= 0 && stepIndex < executionPlan.size()) {
						ExecutionStep step = executionPlan.get(stepIndex);
						sb.append("**步骤编号**: ").append(step.getStep()).append("\n");
						sb.append("**使用工具**: ").append(step.getToolToUse()).append("\n");
						if (step.getToolParameters() != null) {
							sb.append("**参数描述**: ").append(step.getToolParameters().getDescription()).append("\n");
							if (step.getToolParameters().getSqlQuery() != null) {
								sb.append("**执行SQL**: \n```sql\n")
									.append(step.getToolParameters().getSqlQuery())
									.append("\n```\n");
							}
						}
					}
				}
				catch (NumberFormatException e) {
					// 忽略解析错误
					logger.debug("无法解析步骤编号: {}", stepKey);
				}

				if (stepResult.contains("![")) {
					sb.append("**执行结果**: \n").append(stepResult).append("\n\n");
				}
				else {
					List<String> tables = TableUtils.extractTables(stepResult);
					if (!tables.isEmpty()) {
						String processedResult = stepResult;
						for (String table : tables) {
							String formattedTable = TableUtils.formatTable(table);
							processedResult = processedResult.replace(table, formattedTable);
						}
						sb.append("**执行结果**: \n").append(processedResult).append("\n\n");
					}
					else {
						sb.append("**执行结果**: \n```\n").append(stepResult).append("\n```\n\n");
					}
				}
			}

			// 汇总图表内容
			if (!mediaContent.get("charts").isEmpty()) {
				sb.append("## 图表汇总\n");
				for (String chartPath : mediaContent.get("charts")) {
					sb.append(ChartUtils.createChartReference(chartPath, "数据图表")).append("\n\n");
				}
			}

			// 汇总表格内容，使用格式化后的表格
			if (!mediaContent.get("formattedTables").isEmpty()) {
				sb.append("## 数据表格汇总\n");
				String mergedTable = TableUtils.mergeTables(mediaContent.get("formattedTables"));
				if (!mergedTable.isEmpty()) {
					sb.append("### 合并数据表\n");
					sb.append(mergedTable).append("\n\n");
				}

				if (mergedTable.isEmpty()) {
					for (int i = 0; i < mediaContent.get("formattedTables").size(); i++) {
						String table = mediaContent.get("formattedTables").get(i);
						sb.append("### 数据表 ").append(i + 1).append("\n");
						sb.append(table).append("\n\n");

						sb.append(TableUtils.generateTableSummary(table, 3)).append("\n\n");
					}
				}
			}
		}

		return sb.toString();
	}

	/**
	 * 构建最终结果
	 */
	private Map<String, Object> buildFinalResult(String reportContent) {
		Map<String, Object> result = new HashMap<>();
		result.put(RESULT, reportContent);
		// 清理临时状态
		result.put(SQL_EXECUTE_NODE_OUTPUT, null);
		result.put(SQL_EXECUTE_NODE_EXCEPTION_OUTPUT, null);
		result.put(PYTHON_EXECUTE_NODE_OUTPUT, null);
		result.put(PLAN_CURRENT_STEP, null);
		result.put(PLANNER_NODE_OUTPUT, null);
		return result;
	}

}
