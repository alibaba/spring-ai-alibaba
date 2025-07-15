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

import com.alibaba.cloud.ai.dbconnector.DbAccessor;
import com.alibaba.cloud.ai.dbconnector.DbConfig;
import com.alibaba.cloud.ai.dbconnector.bo.DbQueryParameter;
import com.alibaba.cloud.ai.dbconnector.bo.ResultSetBO;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.schema.ExecutionStep;
import com.alibaba.cloud.ai.util.ChatResponseUtil;
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
 * SQL执行节点
 *
 * @author zhangshenghang
 */
public class SqlExecuteNode extends AbstractPlanBasedNode {

	private static final Logger logger = LoggerFactory.getLogger(SqlExecuteNode.class);

	private final DbConfig dbConfig;

	private final DbAccessor dbAccessor;

	public SqlExecuteNode(ChatClient.Builder chatClientBuilder, DbAccessor dbAccessor, DbConfig dbConfig) {
		super();
		this.dbAccessor = dbAccessor;
		this.dbConfig = dbConfig;
	}

	@Override
	public Map<String, Object> apply(OverAllState state) throws Exception {
		logNodeEntry();

		ExecutionStep executionStep = getCurrentExecutionStep(state);
		Integer currentStep = getCurrentStepNumber(state);

		ExecutionStep.ToolParameters toolParameters = executionStep.getToolParameters();
		String sqlQuery = toolParameters.getSqlQuery();

		logger.info("执行SQL查询: {}", sqlQuery);
		logger.info("步骤描述: {}", toolParameters.getDescription());

		return executeSqlQuery(state, currentStep, sqlQuery);
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> executeSqlQuery(OverAllState state, Integer currentStep, String sqlQuery) {
		// 先执行业务逻辑
		DbQueryParameter dbQueryParameter = new DbQueryParameter();
		dbQueryParameter.setSql(sqlQuery);

		try {
			// 执行SQL查询，获取结果
			ResultSetBO resultSetBO = dbAccessor.executeSqlAndReturnObject(dbConfig, dbQueryParameter);
			String jsonStr = resultSetBO.toJsonStr();

			Map<String, String> existingResults = StateUtils.getObjectValue(state, SQL_EXECUTE_NODE_OUTPUT, Map.class,
					new HashMap());
			Map<String, String> updatedResults = StepResultUtils.addStepResult(existingResults, currentStep, jsonStr);

			logger.info("SQL执行成功，结果记录数: {}", resultSetBO.getData() != null ? resultSetBO.getData().size() : 0);

			Map<String, Object> result = Map.of(SQL_EXECUTE_NODE_OUTPUT, updatedResults,
					SQL_EXECUTE_NODE_EXCEPTION_OUTPUT, "");

			// 创建显示流，仅用于用户体验
			Flux<ChatResponse> displayFlux = Flux.create(emitter -> {
				emitter.next(ChatResponseUtil.createCustomStatusResponse("开始执行SQL..."));
				emitter.next(ChatResponseUtil.createCustomStatusResponse("执行SQL查询"));
				emitter.next(ChatResponseUtil.createCustomStatusResponse("```" + sqlQuery + "```"));
				emitter.next(ChatResponseUtil.createCustomStatusResponse("执行SQL完成"));
				emitter.complete();
			});

			// 使用工具类创建生成器，直接返回业务逻辑计算的结果
			var generator = StreamingChatGeneratorUtil.createStreamingGeneratorWithMessages(this.getClass(), state,
					v -> result, displayFlux);

			return Map.of(SQL_EXECUTE_NODE_OUTPUT, generator);
		}
		catch (Exception e) {
			String errorMessage = e.getMessage();
			logger.error("SQL执行失败 - SQL: [{}] ", sqlQuery, e);

			Map<String, Object> errorResult = Map.of(SQL_EXECUTE_NODE_EXCEPTION_OUTPUT, errorMessage);

			// 创建错误显示流
			Flux<ChatResponse> errorDisplayFlux = Flux.create(emitter -> {
				emitter.next(ChatResponseUtil.createCustomStatusResponse("开始执行SQL..."));
				emitter.next(ChatResponseUtil.createCustomStatusResponse("执行SQL查询"));
				emitter.next(ChatResponseUtil.createCustomStatusResponse("SQL执行失败: " + errorMessage));
				emitter.complete();
			});

			// 使用工具类创建错误生成器
			var generator = StreamingChatGeneratorUtil.createStreamingGeneratorWithMessages(this.getClass(), state,
					v -> errorResult, errorDisplayFlux);

			return Map.of(SQL_EXECUTE_NODE_EXCEPTION_OUTPUT, generator);
		}
	}

}
