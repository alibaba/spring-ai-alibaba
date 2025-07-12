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
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.schema.ExecutionStep;
import com.alibaba.cloud.ai.schema.Plan;
import com.alibaba.cloud.ai.util.ChartUtils;
import com.alibaba.cloud.ai.util.ReportUtils;
import com.alibaba.cloud.ai.util.StateUtils;
import com.alibaba.cloud.ai.util.StepResultUtils;
import com.alibaba.cloud.ai.util.TableUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.alibaba.cloud.ai.constant.Constant.*;

/**
 * 报告生成节点
 *
 * @author Makoto
 */
public class ReportGeneratorNode extends AbstractPlanBasedNode {

	private static final Logger logger = LoggerFactory.getLogger(ReportGeneratorNode.class);

	private static final String PROMPT_FILE = "prompts/report-generator.txt";

	private final ChatClient chatClient;

	private final ObjectMapper objectMapper;

	public ReportGeneratorNode(ChatClient.Builder chatClientBuilder) {
		super();
		this.chatClient = chatClientBuilder.build();
		this.objectMapper = new ObjectMapper();
	}

	@Override
	public Map<String, Object> apply(OverAllState state) throws Exception {
		logNodeEntry();

		Plan plan = getPlan(state);

		Map<String, String> sqlExecuteResults = StateUtils.getObjectValue(state, SQL_EXECUTE_NODE_OUTPUT, Map.class,
				new HashMap<>());

		String userQuery = StateUtils.getStringValue(state, INPUT_KEY);

		String report = generateReport(state, plan, sqlExecuteResults, userQuery);

		logger.info("报告生成完成，长度: {}", report.length());

		return Map.of(RESULT, report);
	}

	/**
	 * 生成分析报告
	 */
	private String generateReport(OverAllState state, Plan plan, Map<String, String> sqlExecuteResults,
			String userQuery) {
		try {
			StringBuilder reportData = ReportUtils.createReportStructure(userQuery, plan);

			List<ExecutionStep> executionPlan = plan.getExecutionPlan();
			reportData.append("## 数据分析过程\n\n");

			List<String> chartPaths = new ArrayList<>();

			// 记录Python分析结果
			Map<Integer, String> pythonResults = new HashMap<>();

			for (int i = 0; i < executionPlan.size(); i++) {
				ExecutionStep step = executionPlan.get(i);
				int stepNumber = step.getStep();
				String toolToUse = step.getToolToUse();
				ExecutionStep.ToolParameters toolParameters = step.getToolParameters();

				reportData.append("### 分析步骤 ")
					.append(stepNumber)
					.append(": ")
					.append(toolParameters.getDescription())
					.append("\n\n");

				if (SQL_EXECUTE_NODE.equals(toolToUse)) {
					String sqlQuery = toolParameters.getSqlQuery();
					reportData.append("**数据查询**:\n```sql\n").append(sqlQuery).append("\n```\n\n");

					String resultJson = StepResultUtils.getStepResult(sqlExecuteResults, stepNumber);
					if (resultJson != null) {
						try {
							ResultSetBO resultSetBO = objectMapper.readValue(resultJson, ResultSetBO.class);
							TableUtils.formatSqlResult(reportData, resultSetBO);
						}
						catch (Exception e) {
							reportData.append("**查询结果**:\n```\n").append(resultJson).append("\n```\n\n");
						}
					}
				}
				else if (PYTHON_EXECUTE_NODE.equals(toolToUse)) {
					String instruction = toolParameters.getInstruction();
					String inputDataDescription = toolParameters.getInputDataDescription();
					reportData.append("**分析指令**:\n").append(instruction).append("\n\n");
					if (inputDataDescription != null && !inputDataDescription.isEmpty()) {
						reportData.append("**输入数据**:\n").append(inputDataDescription).append("\n\n");
					}

					String analysisResult = StepResultUtils.getStepResult(sqlExecuteResults, stepNumber);
					if (analysisResult != null) {
						pythonResults.put(stepNumber, analysisResult);

						List<String> extractedChartPaths = ChartUtils.extractChartPaths(analysisResult);
						if (!extractedChartPaths.isEmpty()) {
							chartPaths.addAll(extractedChartPaths);

							reportData.append("**数据可视化分析**:\n");
							String cleanedResult = ChartUtils.removeChartPaths(analysisResult, extractedChartPaths);
							reportData.append(cleanedResult).append("\n\n");

							reportData.append("**数据图表**:\n");
							for (int j = 0; j < extractedChartPaths.size(); j++) {
								String chartPath = extractedChartPaths.get(j);
								reportData.append(
										ChartUtils.generateChartContainer(chartPath, chartPaths.size(), stepNumber));
							}
						}
						else {
							reportData.append("**分析结果**:\n").append(analysisResult).append("\n\n");
						}
					}
				}
				else if (REPORT_GENERATOR_NODE.equals(toolToUse)) {
					String summaryRecommendations = toolParameters.getSummaryAndRecommendations();
					if (summaryRecommendations != null && !summaryRecommendations.isEmpty()) {
						reportData.append("**总结与建议**:\n").append(summaryRecommendations).append("\n\n");
					}
				}
			}

			// 添加关键发现与建议章节
			ReportUtils.addFindingsAndRecommendations(reportData);
			ReportUtils.addChartSummary(reportData, chartPaths);
			ReportUtils.addCrossDimensionalAnalysis(reportData, pythonResults);

			return generateFinalReport(userQuery, reportData.toString());

		}
		catch (Exception e) {
			logger.error("生成报告时发生错误", e);
			return "生成报告时发生错误: " + e.getMessage();
		}
	}

	/**
	 * 使用AI生成最终格式化的报告
	 */
	private String generateFinalReport(String userQuery, String reportData) {
		reportData = ReportUtils.replaceBasicPlaceholders(reportData);
		String userMessage = ReportUtils.buildAIPrompt(userQuery, reportData);
		String systemPrompt = ReportUtils.readPromptFile(PROMPT_FILE);
		return chatClient.prompt().system(systemPrompt).user(userMessage).call().content();
	}

}