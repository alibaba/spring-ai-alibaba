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
public class SqlExecuteNode implements NodeAction {

	private static final Logger logger = LoggerFactory.getLogger(SqlExecuteNode.class);

	private final BeanOutputConverter<Plan> converter;

	private final DbConfig dbConfig;

	private final DbAccessor dbAccessor;

	public SqlExecuteNode(ChatClient.Builder chatClientBuilder, DbAccessor dbAccessor, DbConfig dbConfig) {
		this.converter = new BeanOutputConverter<>(new ParameterizedTypeReference<Plan>() {
		});
		this.dbAccessor = dbAccessor;
		this.dbConfig = dbConfig;
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
		logger.info(toolParameters.getDescription());
		String sqlQuery = toolParameters.getSqlQuery();
		DbQueryParameter dbQueryParameter = new DbQueryParameter();
		dbQueryParameter.setSql(sqlQuery);
		try {
			ResultSetBO resultSetBO = dbAccessor.executeSqlAndReturnObject(dbConfig, dbQueryParameter);
			String jsonStr = resultSetBO.toJsonStr();
			HashMap<String, String> value = state.value(SQL_EXECUTE_NODE_OUTPUT, new HashMap<String, String>());
			value.put("步骤" + planCurrentStep + "结果", jsonStr);
			updated.put(SQL_EXECUTE_NODE_OUTPUT, value);
			updated.put(PLAN_CURRENT_STEP, planCurrentStep + 1);
			updated.put(SQL_EXECUTE_NODE_EXCEPTION_OUTPUT, null);
			return updated;
		}
		catch (Exception e) {
			// 处理验证失败情况
			String errorMessage = e.getMessage();
			logger.error("[{}] SQL执行失败 - 原因: {}", this.getClass().getSimpleName(), errorMessage);
			// 失败，重新生成SQL
			return Map.of(SQL_EXECUTE_NODE_EXCEPTION_OUTPUT, errorMessage);
		}
	}

}
