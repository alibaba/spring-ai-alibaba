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
import com.alibaba.cloud.ai.prompt.PromptConstant;
import com.alibaba.cloud.ai.model.execution.ExecutionStep;
import com.alibaba.cloud.ai.tool.PythonExecutorTool;
import com.alibaba.cloud.ai.util.StateUtils;
import com.alibaba.cloud.ai.util.StepResultUtils;
import com.alibaba.cloud.ai.util.StreamingChatGeneratorUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.Map;

import static com.alibaba.cloud.ai.constant.Constant.*;

/**
 * Python execution simulation node - currently simulates execution, needs integration
 * with Python tools.
 *
 * This node is responsible for: - Simulating Python data analysis execution - Processing
 * SQL execution results through AI analysis - Updating step results with analysis
 * outcomes - Providing streaming feedback during analysis process
 *
 * TODO: Replace simulation with actual Python tool integration
 *
 * @author zhangshenghang
 */
public class PythonExecuteNodeV2 extends AbstractPlanBasedNode {

	private static final Logger logger = LoggerFactory.getLogger(PythonExecuteNodeV2.class);

	private final ChatClient chatClient;

	public PythonExecuteNodeV2(ChatClient.Builder chatClientBuilder, PythonExecutorTool pythonExecutorTool) {
		super();
		this.chatClient = chatClientBuilder.defaultTools(pythonExecutorTool).build();
	}

	@Override
	public Map<String, Object> apply(OverAllState state) throws Exception {
		logNodeEntry();

		ExecutionStep executionStep = getCurrentExecutionStep(state);
		Integer currentStep = getCurrentStepNumber(state);

		ExecutionStep.ToolParameters toolParameters = executionStep.getToolParameters();
		String instruction = toolParameters.getInstruction();
		String description = toolParameters.getDescription();

		@SuppressWarnings("unchecked")
		Map<String, String> sqlExecuteResult = StateUtils.getObjectValue(state, SQL_EXECUTE_NODE_OUTPUT, Map.class,
				new HashMap());

		String systemPrompt = PromptConstant.getPythonExecutorPromptTemplate().render();
		// Create streaming output
		String prompt = String.format(
				"## 整体执行计划（仅当无法理解需求时参考整体执行计划）：%s## instruction：%s\n## description：%s\n## 数据：%s\n请给出结果。",
				getPlan(state).toJsonStr(), instruction, description, sqlExecuteResult);

		Flux<ChatResponse> pythonExecutionFlux = chatClient.prompt()
			.system(systemPrompt)
			.user(prompt)
			.stream()
			.chatResponse();

		// Use utility class to create generator for streaming content collection
		var generator = StreamingChatGeneratorUtil.createStreamingGeneratorWithMessages(this.getClass(), state,
				"开始执行Python分析", "Python分析执行完成", aiResponse -> {
					Map<String, String> updatedSqlResult = StepResultUtils.addStepResult(sqlExecuteResult, currentStep,
							aiResponse);
					logNodeOutput("analysis_result", aiResponse);
					return Map.of(SQL_EXECUTE_NODE_OUTPUT, updatedSqlResult, PLAN_CURRENT_STEP, currentStep + 1);
				}, pythonExecutionFlux);

		return Map.of(PYTHON_EXECUTE_NODE_OUTPUT, generator);
	}

}
