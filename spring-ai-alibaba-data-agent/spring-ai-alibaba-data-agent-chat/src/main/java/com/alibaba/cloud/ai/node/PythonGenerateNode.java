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

import com.alibaba.cloud.ai.config.CodeExecutorProperties;
import com.alibaba.cloud.ai.dto.schema.SchemaDTO;
import com.alibaba.cloud.ai.enums.StreamResponseType;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.cloud.ai.model.execution.ExecutionStep;
import com.alibaba.cloud.ai.prompt.PromptConstant;
import com.alibaba.cloud.ai.util.MarkdownParser;
import com.alibaba.cloud.ai.util.StateUtils;
import com.alibaba.cloud.ai.util.StreamingChatGeneratorUtil;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

import static com.alibaba.cloud.ai.constant.Constant.PYTHON_EXECUTE_NODE_OUTPUT;
import static com.alibaba.cloud.ai.constant.Constant.PYTHON_GENERATE_NODE_OUTPUT;
import static com.alibaba.cloud.ai.constant.Constant.PYTHON_IS_SUCCESS;
import static com.alibaba.cloud.ai.constant.Constant.PYTHON_TRIES_COUNT;
import static com.alibaba.cloud.ai.constant.Constant.QUERY_REWRITE_NODE_OUTPUT;
import static com.alibaba.cloud.ai.constant.Constant.SQL_RESULT_LIST_MEMORY;
import static com.alibaba.cloud.ai.constant.Constant.TABLE_RELATION_OUTPUT;

/**
 * 生成Python代码的节点
 *
 * @author vlsmb
 * @since 2025/7/30
 */
public class PythonGenerateNode extends AbstractPlanBasedNode implements NodeAction {

	private static final Logger log = LoggerFactory.getLogger(PythonGenerateNode.class);

	private static final int SAMPLE_DATA_NUMBER = 5;

	private static final int MAX_TRIES_COUNT = 5;

	private final ObjectMapper objectMapper;

	private final CodeExecutorProperties codeExecutorProperties;

	private final ChatClient chatClient;

	public PythonGenerateNode(CodeExecutorProperties codeExecutorProperties, ChatClient.Builder chatClientBuilder) {
		super();
		this.codeExecutorProperties = codeExecutorProperties;
		this.chatClient = chatClientBuilder.build();
		this.objectMapper = new ObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL);
	}

	@Override
	public Map<String, Object> apply(OverAllState state) throws Exception {
		this.logNodeEntry();

		// Get context
		SchemaDTO schemaDTO = StateUtils.getObjectValue(state, TABLE_RELATION_OUTPUT, SchemaDTO.class);
		List<Map<String, String>> sqlResults = StateUtils.getListValue(state, SQL_RESULT_LIST_MEMORY);
		boolean codeRunSuccess = StateUtils.getObjectValue(state, PYTHON_IS_SUCCESS, Boolean.class, true);
		int triesCount = StateUtils.getObjectValue(state, PYTHON_TRIES_COUNT, Integer.class, MAX_TRIES_COUNT);

		String userPrompt = StateUtils.getStringValue(state, QUERY_REWRITE_NODE_OUTPUT);
		if (!codeRunSuccess) {
			// Last generated Python code failed to run, inform AI model of this
			// information
			String lastCode = StateUtils.getStringValue(state, PYTHON_GENERATE_NODE_OUTPUT);
			String lastError = StateUtils.getStringValue(state, PYTHON_EXECUTE_NODE_OUTPUT);
			userPrompt += String.format("""
					上次尝试生成的Python代码运行失败，请你重新生成符合要求的Python代码。
					【上次生成代码】
					```python
					%s
					```
					【运行错误信息】
					```
					%s
					```
					""", lastCode, lastError);
		}

		ExecutionStep executionStep = this.getCurrentExecutionStep(state);

		ExecutionStep.ToolParameters toolParameters = executionStep.getToolParameters();

		// Load Python code generation template
		String systemPrompt = PromptConstant.getPythonGeneratorPromptTemplate()
			.render(Map.of("python_memory", codeExecutorProperties.getLimitMemory().toString(), "python_timeout",
					codeExecutorProperties.getCodeTimeout(), "database_schema",
					objectMapper.writeValueAsString(schemaDTO), "sample_input",
					objectMapper.writeValueAsString(sqlResults.stream().limit(SAMPLE_DATA_NUMBER).toList()),
					"plan_description", objectMapper.writeValueAsString(toolParameters)));

		Flux<ChatResponse> pythonGenerateFlux = chatClient.prompt()
			.system(systemPrompt)
			.user(userPrompt)
			.stream()
			.chatResponse();

		var generator = StreamingChatGeneratorUtil.createStreamingGeneratorWithMessages(this.getClass(), state, "", "",
				aiResponse -> {
					// Some AI models still output Markdown markup (even though Prompt has
					// emphasized this)
					aiResponse = MarkdownParser.extractRawText(aiResponse);
					log.info("Python Generate Code: {}", aiResponse);
					return Map.of(PYTHON_GENERATE_NODE_OUTPUT, aiResponse, PYTHON_TRIES_COUNT, triesCount - 1);
				}, pythonGenerateFlux, StreamResponseType.PYTHON_GENERATE);

		return Map.of(PYTHON_GENERATE_NODE_OUTPUT, generator);
	}

}
