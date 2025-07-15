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
import com.alibaba.cloud.ai.graph.streaming.StreamingChatGenerator;
import com.alibaba.cloud.ai.prompt.PromptHelper;
import com.alibaba.cloud.ai.schema.ExecutionStep;
import com.alibaba.cloud.ai.schema.SchemaDTO;
import com.alibaba.cloud.ai.service.base.BaseNl2SqlService;
import com.alibaba.cloud.ai.util.ChatResponseUtil;
import com.alibaba.cloud.ai.util.StateUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

import static com.alibaba.cloud.ai.constant.Constant.*;

/**
 * 语义一致性校验节点
 *
 * @author zhangshenghang
 */
public class SemanticConsistencyNode extends AbstractPlanBasedNode {

	private static final Logger logger = LoggerFactory.getLogger(SemanticConsistencyNode.class);

	private final BaseNl2SqlService baseNl2SqlService;

	public SemanticConsistencyNode(ChatClient.Builder chatClientBuilder, BaseNl2SqlService baseNl2SqlService,
								   DbConfig dbConfig) {
		super();
		this.baseNl2SqlService = baseNl2SqlService;
	}

	@Override
	public Map<String, Object> apply(OverAllState state) throws Exception {
		logNodeEntry();

		// 获取必要的输入参数
		List<String> evidenceList = StateUtils.getListValue(state, EVIDENCES);
		SchemaDTO schemaDTO = StateUtils.getObjectValue(state, TABLE_RELATION_OUTPUT, SchemaDTO.class);

		// 获取当前执行步骤和SQL查询
		ExecutionStep executionStep = getCurrentExecutionStep(state);
		Integer currentStep = getCurrentStepNumber(state);
		ExecutionStep.ToolParameters toolParameters = executionStep.getToolParameters();
		String sqlQuery = toolParameters.getSqlQuery();

		logger.info("开始语义一致性校验 - SQL: {}", sqlQuery);
		logger.info("步骤描述: {}", toolParameters.getDescription());

		// 创建开始校验状态流
		Flux<ChatResponse> startValidationFlux = Flux.just(ChatResponseUtil.createCustomStatusResponse("开始语义一致性校验"));

		// 通过流式API获取语义验证结果
		Flux<ChatResponse> validationResultFlux = performSemanticValidationStream(schemaDTO, evidenceList,
				toolParameters, sqlQuery)
			.doOnComplete(() -> {
				logger.info("语义一致性校验流式处理完成");
			})
			.doOnError(error -> {
				logger.error("语义一致性校验出错", error);
			});

		// 处理验证结果
		var chatGenerator = StreamingChatGenerator.builder()
			.startingNode(this.getClass().getSimpleName())
			.startingState(state)
			.mapResult(response -> {
				String validationResult = response.getResult().getOutput().getText();
				boolean isPassed = !validationResult.startsWith("不通过");
				return buildValidationResult(isPassed, validationResult, currentStep);
			})
			.build(Flux.concat(startValidationFlux, validationResultFlux));

		return Map.of(SEMANTIC_CONSISTENC_NODE_OUTPUT, chatGenerator);
	}

	/**
	 * 执行语义一致性验证
	 */
	private String performSemanticValidation(SchemaDTO schemaDTO, List<String> evidenceList,
			ExecutionStep.ToolParameters toolParameters, String sqlQuery) throws Exception {
		// 构建验证上下文
		String schema = PromptHelper.buildMixMacSqlDbPrompt(schemaDTO, true);
		String evidence = StringUtils.join(evidenceList, ";\n");
		String context = String.join("\n", schema, evidence, toolParameters.getDescription());

		// 执行语义一致性检查
		return baseNl2SqlService.semanticConsistency(sqlQuery, context);
	}

	/**
	 * 执行语义一致性验证
	 */
	private Flux<ChatResponse> performSemanticValidationStream(SchemaDTO schemaDTO, List<String> evidenceList,
			ExecutionStep.ToolParameters toolParameters, String sqlQuery) throws Exception {
		// 构建验证上下文
		String schema = PromptHelper.buildMixMacSqlDbPrompt(schemaDTO, true);
		String evidence = StringUtils.join(evidenceList, ";\n");
		String context = String.join("\n", schema, evidence, toolParameters.getDescription());

		// 执行语义一致性检查
		return baseNl2SqlService.semanticConsistencyStream(sqlQuery, context);
	}

	/**
	 * 构建验证结果
	 */
	private Map<String, Object> buildValidationResult(boolean passed, String validationResult, Integer currentStep) {
		if (passed) {
			return Map.of(SEMANTIC_CONSISTENC_NODE_OUTPUT, true, PLAN_CURRENT_STEP, currentStep + 1);
		}
		else {
			return Map.of(SEMANTIC_CONSISTENC_NODE_OUTPUT, false, SEMANTIC_CONSISTENC_NODE_RECOMMEND_OUTPUT,
					validationResult);
		}
	}

}
