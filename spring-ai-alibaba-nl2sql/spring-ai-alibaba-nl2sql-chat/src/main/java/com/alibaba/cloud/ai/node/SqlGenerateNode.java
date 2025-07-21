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
import com.alibaba.cloud.ai.util.ChatResponseUtil;
import com.alibaba.cloud.ai.util.StateUtils;
import com.alibaba.cloud.ai.util.StreamingChatGeneratorUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.core.ParameterizedTypeReference;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.alibaba.cloud.ai.constant.Constant.*;
import static com.alibaba.cloud.ai.graph.StateGraph.END;

/**
 * SQL generation node that handles SQL query regeneration based on errors or validation
 * failures.
 *
 * This node is responsible for: - Regenerating SQL queries when execution exceptions
 * occur - Handling semantic consistency validation failures - Managing retry logic with
 * schema advice - Providing streaming feedback during regeneration process
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
		logger.info("Entering {} node", this.getClass().getSimpleName());

		// Get necessary input parameters
		String plannerNodeOutput = StateUtils.getStringValue(state, PLANNER_NODE_OUTPUT);
		Plan plan = converter.convert(plannerNodeOutput);
		Integer currentStep = StateUtils.getObjectValue(state, PLAN_CURRENT_STEP, Integer.class, 1);

		List<ExecutionStep> executionPlan = plan.getExecutionPlan();
		ExecutionStep executionStep = executionPlan.get(currentStep - 1);
		ExecutionStep.ToolParameters toolParameters = executionStep.getToolParameters();

		// Execute business logic first - determine what needs to be regenerated
		Map<String, Object> result;
		String displayMessage;

		if (StateUtils.hasValue(state, SQL_EXECUTE_NODE_EXCEPTION_OUTPUT)) {
			displayMessage = "检测到SQL执行异常，开始重新生成SQL...";
			String newSql = handleSqlExecutionException(state, plan, toolParameters);
			toolParameters.setSqlQuery(newSql);
			result = Map.of(SQL_GENERATE_OUTPUT, SQL_EXECUTE_NODE, PLANNER_NODE_OUTPUT, plan.toJsonStr());
			logger.info("[{}] Regenerated SQL due to execution exception: {}", this.getClass().getSimpleName(), newSql);
		}
		else if (isSemanticConsistencyFailed(state)) {
			displayMessage = "语义一致性校验未通过，开始重新生成SQL...";
			String newSql = handleSemanticConsistencyFailure(state, toolParameters);
			result = Map.of(SQL_GENERATE_OUTPUT, newSql, RESULT, newSql);
			logger.info("[{}] Regenerated SQL due to semantic consistency failure: {}", this.getClass().getSimpleName(),
					newSql);
		}
		else {
			throw new IllegalStateException("SQL generation node was called unexpectedly");
		}

		// Create display flux for user experience only
		Flux<ChatResponse> displayFlux = Flux.create(emitter -> {
			emitter.next(ChatResponseUtil.createCustomStatusResponse(displayMessage));
			if (result.containsKey(RESULT)) {
				emitter.next(ChatResponseUtil.createCustomStatusResponse("重新生成的SQL: " + result.get(RESULT)));
			}
			else if (result.containsKey(SQL_GENERATE_OUTPUT)
					&& result.get(SQL_GENERATE_OUTPUT).equals(SQL_EXECUTE_NODE)) {
				emitter.next(ChatResponseUtil.createCustomStatusResponse("SQL重新生成完成，准备执行"));
			}
			emitter.complete();
		});

		var generator = StreamingChatGeneratorUtil.createStreamingGeneratorWithMessages(this.getClass(), state,
				v -> result, displayFlux);

		return Map.of(SQL_GENERATE_OUTPUT, generator);
	}

	/**
	 * Handle SQL execution exception
	 */
	private String handleSqlExecutionException(OverAllState state, Plan plan,
			ExecutionStep.ToolParameters toolParameters) throws Exception {
		String sqlException = StateUtils.getStringValue(state, SQL_EXECUTE_NODE_EXCEPTION_OUTPUT);
		logger.info("Detected SQL execution exception, starting to regenerate SQL: {}", sqlException);

		List<String> evidenceList = StateUtils.getListValue(state, EVIDENCES);
		SchemaDTO schemaDTO = StateUtils.getObjectValue(state, TABLE_RELATION_OUTPUT, SchemaDTO.class);

		return regenerateSql(state, toolParameters.toJsonStr(), evidenceList, schemaDTO,
				SQL_EXECUTE_NODE_EXCEPTION_OUTPUT, toolParameters.getSqlQuery());
	}

	/**
	 * Handle semantic consistency validation failure
	 */
	private String handleSemanticConsistencyFailure(OverAllState state, ExecutionStep.ToolParameters toolParameters)
			throws Exception {
		logger.info("Semantic consistency validation failed, starting to regenerate SQL");

		List<String> evidenceList = StateUtils.getListValue(state, EVIDENCES);
		SchemaDTO schemaDTO = StateUtils.getObjectValue(state, TABLE_RELATION_OUTPUT, SchemaDTO.class);

		return regenerateSql(state, toolParameters.toJsonStr(), evidenceList, schemaDTO,
				SEMANTIC_CONSISTENCY_NODE_RECOMMEND_OUTPUT, toolParameters.getSqlQuery());
	}

	/**
	 * Check if semantic consistency validation failed
	 */
	private boolean isSemanticConsistencyFailed(OverAllState state) {
		return StateUtils.getObjectValue(state, SEMANTIC_CONSISTENCY_NODE_OUTPUT, Boolean.class, true) == false;
	}

	/**
	 * 重新生成SQL
	 */
	private String regenerateSql(OverAllState state, String input, List<String> evidenceList, SchemaDTO schemaDTO,
			String exceptionOutputKey, String originalSql) throws Exception {
		String exceptionMessage = StateUtils.getStringValue(state, exceptionOutputKey);

		String newSql = baseNl2SqlService.generateSql(evidenceList, input, schemaDTO, originalSql, exceptionMessage);
		logger.info("Regenerated SQL: {}", newSql);

		return newSql;
	}

	/**
	 * Handle unsatisfied recall information
	 */
	private Map<String, Object> handleUnsatisfiedRecallInfo(OverAllState state, String recallInfoSatisfyRequirement) {
		int sqlGenerateCount = StateUtils.getObjectValue(state, SQL_GENERATE_COUNT, Integer.class, 0) + 1;

		logger.info(sqlGenerateCount == 1 ? "First time generating SQL" : "SQL generation count: {}", sqlGenerateCount);

		if (sqlGenerateCount <= MAX_RETRY_COUNT) {
			return buildRetryResult(state, recallInfoSatisfyRequirement, sqlGenerateCount);
		}
		else {
			logger.info("Recall information doesn't satisfy requirements, retry limit reached, ending SQL generation");
			return Map.of(RESULT, recallInfoSatisfyRequirement, SQL_GENERATE_OUTPUT, END, SQL_GENERATE_COUNT, 0);
		}
	}

	/**
	 * Build retry result
	 */
	private Map<String, Object> buildRetryResult(OverAllState state, String recallInfoSatisfyRequirement,
			int sqlGenerateCount) {
		logger.info("Recall information doesn't satisfy requirements, starting to regenerate SQL");

		Map<String, Object> result = new HashMap<>();
		result.put(SQL_GENERATE_COUNT, sqlGenerateCount);
		result.put(SQL_GENERATE_OUTPUT, SQL_GENERATE_SCHEMA_MISSING);

		String newAdvice = StateUtils.getStringValue(state, SQL_GENERATE_SCHEMA_MISSING_ADVICE, "")
				+ (StateUtils.hasValue(state, SQL_GENERATE_SCHEMA_MISSING_ADVICE) ? "\n" : "")
				+ recallInfoSatisfyRequirement;

		result.put(SQL_GENERATE_SCHEMA_MISSING_ADVICE, newAdvice);

		if (!StateUtils.hasValue(state, SQL_GENERATE_SCHEMA_MISSING_ADVICE)) {
			logger.info("Recall information doesn't satisfy requirements, need to supplement Schema information");
		}

		return result;
	}

}
