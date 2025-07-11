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

import com.alibaba.cloud.ai.dbconnector.DbConfig;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.cloud.ai.schema.ExecutionStep;
import com.alibaba.cloud.ai.schema.Plan;
import com.alibaba.cloud.ai.schema.SchemaDTO;
import com.alibaba.cloud.ai.service.base.BaseNl2SqlService;
import com.alibaba.cloud.ai.util.StateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.core.ParameterizedTypeReference;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.alibaba.cloud.ai.constant.Constant.*;
import static com.alibaba.cloud.ai.graph.StateGraph.END;

/**
 * SQL生成节点
 *
 * @author zhangshenghang
 */
public class SqlGenerateNode implements NodeAction {

	private static final Logger logger = LoggerFactory.getLogger(SqlGenerateNode.class);

	private static final int MAX_RETRY_COUNT = 3;

	private final ChatClient chatClient;

	private final DbConfig dbConfig;

	private final BaseNl2SqlService baseNl2SqlService;

	private final BeanOutputConverter<Plan> converter;

	public SqlGenerateNode(ChatClient.Builder chatClientBuilder, BaseNl2SqlService baseNl2SqlService,
			DbConfig dbConfig) {
		this.chatClient = chatClientBuilder.build();
		this.baseNl2SqlService = baseNl2SqlService;
		this.dbConfig = dbConfig;
		this.converter = new BeanOutputConverter<>(new ParameterizedTypeReference<Plan>() {
		});
	}

	@Override
	public Map<String, Object> apply(OverAllState state) throws Exception {
		logger.info("进入 {} 节点", this.getClass().getSimpleName());

		// 获取必要的输入参数
		String plannerNodeOutput = StateUtils.getStringValue(state, PLANNER_NODE_OUTPUT);
		Plan plan = converter.convert(plannerNodeOutput);
		Integer currentStep = StateUtils.getObjectValue(state, PLAN_CURRENT_STEP, Integer.class, 1);

		List<ExecutionStep> executionPlan = plan.getExecutionPlan();
		ExecutionStep executionStep = executionPlan.get(currentStep - 1);
		ExecutionStep.ToolParameters toolParameters = executionStep.getToolParameters();

		// 检查是否需要重新生成SQL
		if (StateUtils.hasValue(state, SQL_EXECUTE_NODE_EXCEPTION_OUTPUT)) {
			return handleSqlExecutionException(state, plan, toolParameters);
		}

		if (isSemanticConsistencyFailed(state)) {
			return handleSemanticConsistencyFailure(state, toolParameters);
		}

		throw new IllegalStateException("SQL生成节点被意外调用");
	}

	/**
	 * 处理SQL执行异常
	 */
	private Map<String, Object> handleSqlExecutionException(OverAllState state, Plan plan,
			ExecutionStep.ToolParameters toolParameters) throws Exception {
		String sqlException = StateUtils.getStringValue(state, SQL_EXECUTE_NODE_EXCEPTION_OUTPUT);
		logger.info("检测到SQL执行异常，开始重新生成SQL: {}", sqlException);

		List<String> evidenceList = StateUtils.getListValue(state, EVIDENCES);
		SchemaDTO schemaDTO = StateUtils.getObjectValue(state, TABLE_RELATION_OUTPUT, SchemaDTO.class);

		String newSql = regenerateSql(state, toolParameters.toJsonStr(), evidenceList, schemaDTO,
				SQL_EXECUTE_NODE_EXCEPTION_OUTPUT, toolParameters.getSqlQuery());

		toolParameters.setSqlQuery(newSql);

		return Map.of(SQL_GENERATE_OUTPUT, SQL_EXECUTE_NODE, PLANNER_NODE_OUTPUT, plan.toJsonStr());
	}

	/**
	 * 处理语义一致性校验失败
	 */
	private Map<String, Object> handleSemanticConsistencyFailure(OverAllState state,
			ExecutionStep.ToolParameters toolParameters) throws Exception {
		logger.info("语义一致性校验未通过，开始重新生成SQL");

		List<String> evidenceList = StateUtils.getListValue(state, EVIDENCES);
		SchemaDTO schemaDTO = StateUtils.getObjectValue(state, TABLE_RELATION_OUTPUT, SchemaDTO.class);

		String newSql = regenerateSql(state, toolParameters.toJsonStr(), evidenceList, schemaDTO,
				SEMANTIC_CONSISTENC_NODE_RECOMMEND_OUTPUT, toolParameters.getSqlQuery());

		return Map.of(SQL_GENERATE_OUTPUT, newSql, RESULT, newSql);
	}

	/**
	 * 检查语义一致性是否失败
	 */
	private boolean isSemanticConsistencyFailed(OverAllState state) {
		return StateUtils.getObjectValue(state, SEMANTIC_CONSISTENC_NODE_OUTPUT, Boolean.class, true) == false;
	}

	/**
	 * 重新生成SQL
	 */
	private String regenerateSql(OverAllState state, String input, List<String> evidenceList, SchemaDTO schemaDTO,
			String exceptionOutputKey, String originalSql) throws Exception {
		String exceptionMessage = StateUtils.getStringValue(state, exceptionOutputKey);

		String newSql = baseNl2SqlService.generateSql(evidenceList, input, schemaDTO, originalSql, exceptionMessage);
		logger.info("重新生成的SQL: {}", newSql);

		return newSql;
	}

	/**
	 * 处理不满足需求的召回信息
	 */
	private Map<String, Object> handleUnsatisfiedRecallInfo(OverAllState state, String recallInfoSatisfyRequirement) {
		int sqlGenerateCount = StateUtils.getObjectValue(state, SQL_GENERATE_COUNT, Integer.class, 0) + 1;

		logger.info(sqlGenerateCount == 1 ? "首次生成SQL" : "SQL生成次数: {}", sqlGenerateCount);

		if (sqlGenerateCount <= MAX_RETRY_COUNT) {
			return buildRetryResult(state, recallInfoSatisfyRequirement, sqlGenerateCount);
		}
		else {
			logger.info("召回信息不满足需求，重试次数已达上限，结束SQL生成");
			return Map.of(RESULT, recallInfoSatisfyRequirement, SQL_GENERATE_OUTPUT, END, SQL_GENERATE_COUNT, 0);
		}
	}

	/**
	 * 构建重试结果
	 */
	private Map<String, Object> buildRetryResult(OverAllState state, String recallInfoSatisfyRequirement,
			int sqlGenerateCount) {
		logger.info("召回信息不满足需求，开始重新生成SQL");

		Map<String, Object> result = new HashMap<>();
		result.put(SQL_GENERATE_COUNT, sqlGenerateCount);
		result.put(SQL_GENERATE_OUTPUT, SQL_GENERATE_SCHEMA_MISSING);

		String newAdvice = StateUtils.getStringValue(state, SQL_GENERATE_SCHEMA_MISSING_ADVICE, "")
				+ (StateUtils.hasValue(state, SQL_GENERATE_SCHEMA_MISSING_ADVICE) ? "\n" : "")
				+ recallInfoSatisfyRequirement;

		result.put(SQL_GENERATE_SCHEMA_MISSING_ADVICE, newAdvice);

		if (!StateUtils.hasValue(state, SQL_GENERATE_SCHEMA_MISSING_ADVICE)) {
			logger.info("召回信息不满足需求，需要补充Schema信息");
		}

		return result;
	}

}
