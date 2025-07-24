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
import com.alibaba.cloud.ai.prompt.PromptHelper;
import com.alibaba.cloud.ai.model.execution.ExecutionStep;
import com.alibaba.cloud.ai.dto.schema.SchemaDTO;
import com.alibaba.cloud.ai.service.base.BaseNl2SqlService;
import com.alibaba.cloud.ai.util.StateUtils;
import com.alibaba.cloud.ai.util.StreamingChatGeneratorUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatResponse;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

import static com.alibaba.cloud.ai.constant.Constant.*;

/**
 * Semantic consistency validation node that checks SQL query semantic consistency.
 *
 * This node is responsible for: - Validating SQL query semantic consistency against
 * schema and evidence - Providing validation results for query refinement - Handling
 * validation failures with recommendations - Managing step progression in execution plan
 *
 * @author zhangshenghang
 */
public class SemanticConsistencyNode extends AbstractPlanBasedNode {

	private static final Logger logger = LoggerFactory.getLogger(SemanticConsistencyNode.class);

	private final BaseNl2SqlService baseNl2SqlService;

	public SemanticConsistencyNode(BaseNl2SqlService baseNl2SqlService) {
		super();
		this.baseNl2SqlService = baseNl2SqlService;
	}

	@Override
	public Map<String, Object> apply(OverAllState state) throws Exception {
		logNodeEntry();

		// Get necessary input parameters
		List<String> evidenceList = StateUtils.getListValue(state, EVIDENCES);
		SchemaDTO schemaDTO = StateUtils.getObjectValue(state, TABLE_RELATION_OUTPUT, SchemaDTO.class);

		// Get current execution step and SQL query
		ExecutionStep executionStep = getCurrentExecutionStep(state);
		Integer currentStep = getCurrentStepNumber(state);
		ExecutionStep.ToolParameters toolParameters = executionStep.getToolParameters();
		String sqlQuery = toolParameters.getSqlQuery();

		logger.info("Starting semantic consistency validation - SQL: {}", sqlQuery);
		logger.info("Step description: {}", toolParameters.getDescription());

		Flux<ChatResponse> validationResultFlux = performSemanticValidationStream(schemaDTO, evidenceList,
				toolParameters, sqlQuery);

		var generator = StreamingChatGeneratorUtil.createStreamingGeneratorWithMessages(this.getClass(), state,
				"开始语义一致性校验", "语义一致性校验完成", validationResult -> {
					boolean isPassed = !validationResult.startsWith("不通过");
					Map<String, Object> result = buildValidationResult(isPassed, validationResult, currentStep);
					logger.info("[{}] Semantic consistency validation result: {}, passed: {}",
							this.getClass().getSimpleName(), validationResult, isPassed);
					return result;
				}, validationResultFlux, StreamResponseType.VALIDATION);

		return Map.of(SEMANTIC_CONSISTENCY_NODE_OUTPUT, generator);
	}

	/**
	 * Perform semantic consistency validation
	 */
	private String performSemanticValidation(SchemaDTO schemaDTO, List<String> evidenceList,
			ExecutionStep.ToolParameters toolParameters, String sqlQuery) throws Exception {
		// Build validation context
		String schema = PromptHelper.buildMixMacSqlDbPrompt(schemaDTO, true);
		String evidence = StringUtils.join(evidenceList, ";\n");
		String context = String.join("\n", schema, evidence, toolParameters.getDescription());

		// Execute semantic consistency check
		return baseNl2SqlService.semanticConsistency(sqlQuery, context);
	}

	/**
	 * Perform streaming semantic consistency validation
	 */
	private Flux<ChatResponse> performSemanticValidationStream(SchemaDTO schemaDTO, List<String> evidenceList,
			ExecutionStep.ToolParameters toolParameters, String sqlQuery) throws Exception {
		// Build validation context
		String schema = PromptHelper.buildMixMacSqlDbPrompt(schemaDTO, true);
		String evidence = StringUtils.join(evidenceList, ";\n");
		String context = String.join("\n", schema, evidence, toolParameters.getDescription());

		// Execute semantic consistency check
		return baseNl2SqlService.semanticConsistencyStream(sqlQuery, context);
	}

	/**
	 * Build validation result
	 */
	private Map<String, Object> buildValidationResult(boolean passed, String validationResult, Integer currentStep) {
		if (passed) {
			return Map.of(SEMANTIC_CONSISTENCY_NODE_OUTPUT, true, PLAN_CURRENT_STEP, currentStep + 1);
		}
		else {
			return Map.of(SEMANTIC_CONSISTENCY_NODE_OUTPUT, false, SEMANTIC_CONSISTENCY_NODE_RECOMMEND_OUTPUT,
					validationResult);
		}
	}

}
