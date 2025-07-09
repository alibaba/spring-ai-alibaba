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
import com.alibaba.cloud.ai.schema.ExecutionStep;
import com.alibaba.cloud.ai.schema.Plan;
import com.alibaba.cloud.ai.util.StepResultUtils;
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
public class PythonExecuteNode implements NodeAction {

	private static final Logger logger = LoggerFactory.getLogger(PythonExecuteNode.class);

	private static final String SYSTEM_PROMPT = "你是一个模拟Python的执行���，我将给你需求和数据，请帮我分析，并给出详细的数据结果";

	private final ChatClient chatClient;
	private final BeanOutputConverter<Plan> converter;

	public PythonExecuteNode(ChatClient.Builder chatClientBuilder) {
		this.converter = new BeanOutputConverter<>(new ParameterizedTypeReference<Plan>() {});
		this.chatClient = chatClientBuilder.build();
	}

	@Override
	public Map<String, Object> apply(OverAllState state) throws Exception {
		// 获取计划节点输出并解析
		String plannerNodeOutput = (String) state.value(PLANNER_NODE_OUTPUT)
			.orElseThrow(() -> new IllegalStateException("计划节点输出为空"));
		logger.info("plannerNodeOutput: {}", plannerNodeOutput);

		Plan plan = converter.convert(plannerNodeOutput);
		Integer currentStep = state.value(PLAN_CURRENT_STEP, 1);

		// 获取当前执行步骤信息
		List<ExecutionStep> executionPlan = plan.getExecutionPlan();
		if (executionPlan == null || executionPlan.isEmpty()) {
			throw new IllegalStateException("执行计划为空");
		}

		int stepIndex = currentStep - 1;
		if (stepIndex < 0 || stepIndex >= executionPlan.size()) {
			throw new IllegalStateException("当前步骤索引超出范围: " + stepIndex);
		}

		ExecutionStep executionStep = executionPlan.get(stepIndex);
		ExecutionStep.ToolParameters toolParameters = executionStep.getToolParameters();
		String instruction = toolParameters.getInstruction();
		String description = toolParameters.getDescription();

		// 获取SQL执行结果
		Map<String, String> sqlExecuteResult = state.value(SQL_EXECUTE_NODE_OUTPUT, new HashMap<>());

		// 构建用户消息并调用AI
		String userMessage = String.format(
			"## 整体执行计划（仅当无法理解需求时参考整体执行计划）：%s## instruction：%s\n## description：%s\n## 数据：%s\n请给出结果。",
			plan.toJsonStr(), instruction, description, sqlExecuteResult
		);

		String aiResponse = chatClient.prompt(SYSTEM_PROMPT)
			.user(userMessage)
			.call()
			.content();

		// 构建返回结果
		Map<String, Object> updated = new HashMap<>();
		Map<String, String> updatedSqlResult = StepResultUtils.addStepResult(sqlExecuteResult, currentStep, aiResponse);

		updated.put(SQL_EXECUTE_NODE_OUTPUT, updatedSqlResult);
		updated.put(PLAN_CURRENT_STEP, currentStep + 1);

		return updated;
	}
}
