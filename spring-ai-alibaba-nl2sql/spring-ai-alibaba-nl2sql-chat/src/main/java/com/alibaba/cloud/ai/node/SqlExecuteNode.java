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
import com.alibaba.cloud.ai.util.StateUtils;
import com.alibaba.cloud.ai.util.StepResultUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;

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
		DbQueryParameter dbQueryParameter = new DbQueryParameter();
		dbQueryParameter.setSql(sqlQuery);

		try {
			ResultSetBO resultSetBO = dbAccessor.executeSqlAndReturnObject(dbConfig, dbQueryParameter);
			String jsonStr = resultSetBO.toJsonStr();

			Map<String, String> existingResults = StateUtils.getObjectValue(state, SQL_EXECUTE_NODE_OUTPUT, Map.class,
					new HashMap());
			Map<String, String> updatedResults = StepResultUtils.addStepResult(existingResults, currentStep, jsonStr);

			logger.info("SQL执行成功，结果记录数: {}", resultSetBO.getData() != null ? resultSetBO.getData().size() : 0);

			return Map.of(SQL_EXECUTE_NODE_OUTPUT, updatedResults, SQL_EXECUTE_NODE_EXCEPTION_OUTPUT, "");
		}
		catch (Exception e) {
			String errorMessage = e.getMessage();
			logger.error("SQL执行失败 - SQL: [{}] ", sqlQuery, e);
			return Map.of(SQL_EXECUTE_NODE_EXCEPTION_OUTPUT, errorMessage);
		}
	}

}
