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
import com.alibaba.cloud.ai.prompt.PromptHelper;
import com.alibaba.cloud.ai.schema.ExecutionStep;
import com.alibaba.cloud.ai.schema.SchemaDTO;
import com.alibaba.cloud.ai.service.base.BaseNl2SqlService;
import com.alibaba.cloud.ai.util.StateUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;

import java.util.List;
import java.util.Map;

import static com.alibaba.cloud.ai.constant.Constant.*;

/**
 * 语义一致性校验节点
 *
 * @author zhangshenghang
 */
public class SemanticConsistencNode extends AbstractPlanBasedNode {

	private static final Logger logger = LoggerFactory.getLogger(SemanticConsistencNode.class);

	private final BaseNl2SqlService baseNl2SqlService;

	public SemanticConsistencNode(ChatClient.Builder chatClientBuilder, BaseNl2SqlService baseNl2SqlService,
			DbConfig dbConfig) {
		super();
		this.baseNl2SqlService = baseNl2SqlService;
	}

	@Override
	public Map<String, Object> apply(OverAllState state) throws Exception {
		logNodeEntry();

		// 获取必要的输入参数
		String input = StateUtils.getStringValue(state, INPUT_KEY);
		List<String> evidenceList = StateUtils.getListValue(state, EVIDENCES);
		SchemaDTO schemaDTO = StateUtils.getObjectValue(state, TABLE_RELATION_OUTPUT, SchemaDTO.class);

		// 获取当前执行步骤和SQL查询
		ExecutionStep executionStep = getCurrentExecutionStep(state);
		Integer currentStep = getCurrentStepNumber(state);
		ExecutionStep.ToolParameters toolParameters = executionStep.getToolParameters();
		String sqlQuery = toolParameters.getSqlQuery();

		logger.info("开始语义一致性校验 - SQL: {}", sqlQuery);
		logger.info("步骤描述: {}", toolParameters.getDescription());

		// 执行语义一致性检查
		String validationResult = performSemanticValidation(schemaDTO, evidenceList, toolParameters, sqlQuery);

		// 判断校验结果
		boolean passed = !validationResult.startsWith("不通过");
		logger.info("语义一致性校验结果: {}", passed ? "通过" : "未通过");

		if (!passed) {
			logger.info("语义一致性校验详情: {}", validationResult);
		}

		return buildValidationResult(passed, validationResult, currentStep);
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
