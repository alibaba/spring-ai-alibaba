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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.core.ParameterizedTypeReference;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.alibaba.cloud.ai.constant.Constant.*;

/**
 * @author zhangshenghang
 */
public class ReportGeneratorNode implements NodeAction {

	private static final Logger logger = LoggerFactory.getLogger(ReportGeneratorNode.class);

	private final ChatClient chatClient;

	private final BeanOutputConverter<Plan> converter;

	public ReportGeneratorNode(ChatClient.Builder chatClientBuilder) {
		this.chatClient = chatClientBuilder.build();
		this.converter = new BeanOutputConverter<>(new ParameterizedTypeReference<>() {
		});
	}

	@Override
	public Map<String, Object> apply(OverAllState state) throws Exception {
		logger.info("进入 {} 节点", this.getClass().getSimpleName());

		String plannerNodeOutput = (String) state.value(PLANNER_NODE_OUTPUT)
			.orElseThrow(() -> new IllegalStateException("计划节点输出为空"));
		logger.info("plannerNodeOutput: {}", plannerNodeOutput);

		Map<String, Object> updated = new HashMap<>();
		Plan plan = converter.convert(plannerNodeOutput);
		Integer currentStep = state.value(PLAN_CURRENT_STEP, 1);
		List<ExecutionStep> executionPlan = plan.getExecutionPlan();

		// 添加空值检查
		if (executionPlan == null || executionPlan.isEmpty()) {
			throw new IllegalStateException("执行计划为空");
		}

		ExecutionStep executionStep = executionPlan.get(currentStep - 1);
		ExecutionStep.ToolParameters toolParameters = executionStep.getToolParameters();
		String summaryAndRecommendations = toolParameters.getSummaryAndRecommendations();
		HashMap<String, String> executionResults = state.value(SQL_EXECUTE_NODE_OUTPUT, new HashMap<>());

		// 获取用户原始输入
		String userInput = (String) state.value(INPUT_KEY)
			.orElseThrow(() -> new IllegalStateException("用户输入为空"));

		// 构建用户需求和计划描述
		String userRequirementsAndPlan = buildUserRequirementsAndPlan(userInput, plan);

		// 构建分析步骤和数据结果描述
		String analysisStepsAndData = buildAnalysisStepsAndData(plan, executionResults);

		// 使用PromptHelper构建报告生成提示词
		String reportPrompt = PromptHelper.buildReportGeneratorPrompt(
			userRequirementsAndPlan,
			analysisStepsAndData,
			summaryAndRecommendations
		);

		String content = chatClient.prompt()
			.user(reportPrompt)
			.call()
			.content();

		logger.info("生成的报告内容: {}", content);

		updated.put(RESULT, content);
		updated.put(SQL_EXECUTE_NODE_OUTPUT, null);
		updated.put(PLAN_CURRENT_STEP, null);
		updated.put(PLANNER_NODE_OUTPUT, null);
		return updated;
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
	 * 构建分析步骤和数据结果描述
	 */
	private String buildAnalysisStepsAndData(Plan plan, HashMap<String, String> executionResults) {
		StringBuilder sb = new StringBuilder();
		sb.append("## 数据执行结果\n");

		if (executionResults.isEmpty()) {
			sb.append("暂无执行结果数据\n");
		} else {
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
								sb.append("**执行SQL**: \n```sql\n").append(step.getToolParameters().getSqlQuery()).append("\n```\n");
							}
						}
					}
				} catch (NumberFormatException e) {
					// 忽略解析错误
				}

				sb.append("**执行结果**: \n```json\n").append(stepResult).append("\n```\n\n");
			}
		}

		return sb.toString();
	}

}
