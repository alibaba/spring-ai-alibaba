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

import com.alibaba.cloud.ai.constant.StreamResponseType;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.cloud.ai.model.execution.ExecutionStep;
import com.alibaba.cloud.ai.model.execution.Plan;
import com.alibaba.cloud.ai.prompt.PromptHelper;
import com.alibaba.cloud.ai.util.StateUtils;
import com.alibaba.cloud.ai.util.StreamingChatGeneratorUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.core.ParameterizedTypeReference;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.alibaba.cloud.ai.constant.Constant.*;

/**
 * Report generation node that creates comprehensive analysis reports based on execution
 * results.
 *
 * This node is responsible for: - Generating detailed analysis reports from SQL execution
 * results - Summarizing data insights and findings - Providing comprehensive answers to
 * user queries - Creating structured final output for users
 *
 * @author zhangshenghang
 */
public class ReportGeneratorNode implements NodeAction {

	private static final Logger logger = LoggerFactory.getLogger(ReportGeneratorNode.class);

	private final ChatClient chatClient;

	private final BeanOutputConverter<Plan> converter;

	public ReportGeneratorNode(ChatClient.Builder chatClientBuilder) {
		this.chatClient = chatClientBuilder.build();
		this.converter = new BeanOutputConverter<>(new ParameterizedTypeReference<Plan>() {
		});
	}

	@Override
	public Map<String, Object> apply(OverAllState state) throws Exception {
		logger.info("Entering {} node", this.getClass().getSimpleName());

		// Get necessary input parameters
		String plannerNodeOutput = StateUtils.getStringValue(state, PLANNER_NODE_OUTPUT);
		String userInput = StateUtils.getStringValue(state, INPUT_KEY);
		Integer currentStep = StateUtils.getObjectValue(state, PLAN_CURRENT_STEP, Integer.class, 1);
		@SuppressWarnings("unchecked")
		HashMap<String, String> executionResults = StateUtils.getObjectValue(state, SQL_EXECUTE_NODE_OUTPUT,
				HashMap.class, new HashMap<>());

		logger.info("Planner node output: {}", plannerNodeOutput);

		// Parse plan and get current step
		Plan plan = converter.convert(plannerNodeOutput);
		ExecutionStep executionStep = getCurrentExecutionStep(plan, currentStep);
		String summaryAndRecommendations = executionStep.getToolParameters().getSummaryAndRecommendations();

		// Generate report streaming flux
		Flux<ChatResponse> reportGenerationFlux = generateReport(userInput, plan, executionResults,
				summaryAndRecommendations);

		// Use utility class to create streaming generator with content collection
		var generator = StreamingChatGeneratorUtil.createStreamingGeneratorWithMessages(this.getClass(), state,
				"开始生成报告...", "报告生成完成！", reportContent -> {
					logger.info("Generated report content: {}", reportContent);
					Map<String, Object> result = new HashMap<>();
					result.put(RESULT, reportContent);
					result.put(SQL_EXECUTE_NODE_OUTPUT, null);
					result.put(PLAN_CURRENT_STEP, null);
					result.put(PLANNER_NODE_OUTPUT, null);
					return result;
				}, reportGenerationFlux, StreamResponseType.OUTPUT_REPORT);

		return Map.of(RESULT, generator);
	}

	/**
	 * Gets the current execution step from the plan.
	 */
	private ExecutionStep getCurrentExecutionStep(Plan plan, Integer currentStep) {
		List<ExecutionStep> executionPlan = plan.getExecutionPlan();
		if (executionPlan == null || executionPlan.isEmpty()) {
			throw new IllegalStateException("Execution plan is empty");
		}

		int stepIndex = currentStep - 1;
		if (stepIndex < 0 || stepIndex >= executionPlan.size()) {
			throw new IllegalStateException("Current step index out of range: " + stepIndex);
		}

		return executionPlan.get(stepIndex);
	}

	/**
	 * Generates the analysis report.
	 */
	private Flux<ChatResponse> generateReport(String userInput, Plan plan, HashMap<String, String> executionResults,
			String summaryAndRecommendations) {
		// Build user requirements and plan description
		String userRequirementsAndPlan = buildUserRequirementsAndPlan(userInput, plan);

		// Build analysis steps and data results description
		String analysisStepsAndData = buildAnalysisStepsAndData(plan, executionResults);

		// Use PromptHelper to build report generation prompt
		String reportPrompt = PromptHelper.buildReportGeneratorPrompt(userRequirementsAndPlan, analysisStepsAndData,
				summaryAndRecommendations);

		return chatClient.prompt().user(reportPrompt).stream().chatResponse();
	}

	/**
	 * Builds user requirements and plan description.
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
			sb.append("### 步骤 ").append(i + 1).append(": 步骤编号 ").append(step.getStep()).append("\n");
			sb.append("**工具**: ").append(step.getToolToUse()).append("\n");
			if (step.getToolParameters() != null) {
				sb.append("**参数描述**: ").append(step.getToolParameters().getDescription()).append("\n");
			}
			sb.append("\n");
		}

		return sb.toString();
	}

	/**
	 * Builds analysis steps and data results description.
	 */
	private String buildAnalysisStepsAndData(Plan plan, HashMap<String, String> executionResults) {
		StringBuilder sb = new StringBuilder();
		sb.append("## 数据执行结果\n");

		if (executionResults.isEmpty()) {
			sb.append("暂无执行结果数据\n");
		}
		else {
			List<ExecutionStep> executionPlan = plan.getExecutionPlan();
			for (Map.Entry<String, String> entry : executionResults.entrySet()) {
				String stepKey = entry.getKey();
				String stepResult = entry.getValue();

				sb.append("### ").append(stepKey).append("\n");

				// Try to get corresponding step description
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
					// Ignore parsing errors
				}

				sb.append("**执行结果**: \n```json\n").append(stepResult).append("\n```\n\n");
			}
		}

		return sb.toString();
	}

}
