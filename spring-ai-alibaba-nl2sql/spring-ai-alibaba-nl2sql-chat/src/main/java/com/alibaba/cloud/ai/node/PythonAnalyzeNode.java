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

import com.alibaba.cloud.ai.enums.StreamResponseType;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.cloud.ai.prompt.PromptConstant;
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

import static com.alibaba.cloud.ai.constant.Constant.PLAN_CURRENT_STEP;
import static com.alibaba.cloud.ai.constant.Constant.PYTHON_ANALYSIS_NODE_OUTPUT;
import static com.alibaba.cloud.ai.constant.Constant.PYTHON_EXECUTE_NODE_OUTPUT;
import static com.alibaba.cloud.ai.constant.Constant.QUERY_REWRITE_NODE_OUTPUT;
import static com.alibaba.cloud.ai.constant.Constant.SQL_EXECUTE_NODE_OUTPUT;

/**
 * 根据Python代码的运行结果做总结分析
 *
 * @author vlsmb
 * @since 2025/7/30
 */
public class PythonAnalyzeNode extends AbstractPlanBasedNode implements NodeAction {

	private static final Logger log = LoggerFactory.getLogger(PythonAnalyzeNode.class);

	private final ChatClient chatClient;

	public PythonAnalyzeNode(ChatClient.Builder chatClientBuilder) {
		super();
		this.chatClient = chatClientBuilder.build();
	}

	@Override
	public Map<String, Object> apply(OverAllState state) throws Exception {
		this.logNodeEntry();

		// Get context
		String userQuery = StateUtils.getStringValue(state, QUERY_REWRITE_NODE_OUTPUT);
		String pythonOutput = StateUtils.getStringValue(state, PYTHON_EXECUTE_NODE_OUTPUT);
		int currentStep = this.getCurrentStepNumber(state);
		@SuppressWarnings("unchecked")
		Map<String, String> sqlExecuteResult = StateUtils.getObjectValue(state, SQL_EXECUTE_NODE_OUTPUT, Map.class,
				new HashMap());

		// Load Python code generation template
		String systemPrompt = PromptConstant.getPythonAnalyzePromptTemplate()
			.render(Map.of("python_output", pythonOutput, "user_query", userQuery));

		Flux<ChatResponse> pythonAnalyzeFlux = chatClient.prompt().system(systemPrompt).stream().chatResponse();

		var generator = StreamingChatGeneratorUtil.createStreamingGeneratorWithMessages(this.getClass(), state,
				"正在分析代码运行结果...\n", "\n结果分析完成。", aiResponse -> {
					Map<String, String> updatedSqlResult = StepResultUtils.addStepResult(sqlExecuteResult, currentStep,
							aiResponse);
					this.logNodeOutput("python_analysis_result", aiResponse);
					return Map.of(SQL_EXECUTE_NODE_OUTPUT, updatedSqlResult, PLAN_CURRENT_STEP, currentStep + 1);
				}, pythonAnalyzeFlux, StreamResponseType.PYTHON_ANALYSIS);

		return Map.of(PYTHON_ANALYSIS_NODE_OUTPUT, generator);
	}

}
