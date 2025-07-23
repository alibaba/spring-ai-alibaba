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
import com.alibaba.cloud.ai.model.execution.ExecutionStep;
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
public class PythonExecuteNode extends AbstractPlanBasedNode {

	private static final Logger logger = LoggerFactory.getLogger(PythonExecuteNode.class);

	private static final String SYSTEM_PROMPT = """
			你将模拟Python的执行，根据我提供的需求和数据进行详细分析，并给出最终的数据结果。
			在进行分析时，请按照以下要求操作：
			1. 仔细理解需求和数据的内容。
			2. 运用类似于Python的逻辑和方法进行分析。
			3. 给出详细的分析过程和推理依据。
			4. 输出详细、全面的数据结果。
			""";

	private final ChatClient chatClient;

	public PythonExecuteNode(ChatClient.Builder chatClientBuilder) {
		super();
		this.chatClient = chatClientBuilder.build();
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

		// Create streaming output
		String prompt = String.format(
				"## 整体执行计划（仅当无法理解需求时参考整体执行计划）：%s## instruction：%s\n## description：%s\n## 数据：%s\n请给出结果。",
				getPlan(state).toJsonStr(), instruction, description, sqlExecuteResult);

		Flux<ChatResponse> pythonExecutionFlux = chatClient.prompt()
			.system(SYSTEM_PROMPT)
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
				}, pythonExecutionFlux, StreamResponseType.PYTHON_ANALYSIS);

		return Map.of(PYTHON_EXECUTE_NODE_OUTPUT, generator);
	}

}
