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

	private final ChatClient chatClient;
	private final BeanOutputConverter<Plan> converter;

	public PythonExecuteNode(ChatClient.Builder chatClientBuilder) {
		this.converter = new BeanOutputConverter<>(new ParameterizedTypeReference<Plan>() {});

		this.chatClient = chatClientBuilder.build();
	}

	@Override
	public Map<String, Object> apply(OverAllState state) throws Exception {
		String plannerNodeOutput = (String) state.value(PLANNER_NODE_OUTPUT).orElseThrow();
		logger.info("plannerNodeOutput: {}", plannerNodeOutput);
		Map<String, Object> updated = new HashMap<>();
		Plan plan = converter.convert(plannerNodeOutput);
		Integer planCurrentStep = state.value(PLAN_CURRENT_STEP, 1);
		List<ExecutionStep> executionPlan = plan.getExecutionPlan();
		ExecutionStep executionStep = executionPlan.get(planCurrentStep - 1);
		ExecutionStep.ToolParameters toolParameters = executionStep.getToolParameters();
		String instruction = toolParameters.getInstruction();
		String description = toolParameters.getDescription();
		HashMap<String, String> value = state.value(SQL_EXECUTE_NODE_OUTPUT, new HashMap<String, String>());

		String content = chatClient.prompt("你是一个模拟Python的执行器，我将给你需求和数据，请帮我分析，并给出详细的数据结果").user(
				"## 整体执行计划（仅当无法理解需求时参考整体执行计划）：" + plan.toJsonStr() +
						"## instruction：" + instruction + "\n## description：" + description + "\n## 数据：" + value + "\n请给出结果。"
		).call().content();
		HashMap<String, String> value2 = state.value(SQL_EXECUTE_NODE_OUTPUT, new HashMap<String, String>());
		value2.put("步骤"+planCurrentStep+"结果", content);
		updated.put(SQL_EXECUTE_NODE_OUTPUT, value2);
		updated.put(PLAN_CURRENT_STEP, planCurrentStep + 1);
		return updated;
	}

}
