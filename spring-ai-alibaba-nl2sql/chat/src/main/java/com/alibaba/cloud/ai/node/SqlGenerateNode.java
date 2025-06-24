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
import com.alibaba.cloud.ai.schema.SchemaDTO;
import com.alibaba.cloud.ai.service.base.BaseNl2SqlService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.alibaba.cloud.ai.constant.Constant.*;
import static com.alibaba.cloud.ai.graph.StateGraph.END;

/**
 * 生成 SQL 语句
 *
 * @author zhangshenghang
 */
public class SqlGenerateNode implements NodeAction {

	private static final Logger logger = LoggerFactory.getLogger(SqlGenerateNode.class);

	private final ChatClient chatClient;

	private final DbConfig dbConfig;

	private final BaseNl2SqlService baseNl2SqlService;

	public SqlGenerateNode(ChatClient.Builder chatClientBuilder, BaseNl2SqlService baseNl2SqlService,
			DbConfig dbConfig) {
		this.chatClient = chatClientBuilder.build();
		this.baseNl2SqlService = baseNl2SqlService;
		this.dbConfig = dbConfig;
	}

	@Override
	public Map<String, Object> apply(OverAllState state) throws Exception {
		logger.info("进入 {} 节点", this.getClass().getSimpleName());
		String input = (String) state.value(INPUT_KEY).orElseThrow();
		List<String> evidenceList = (List<String>) state.value(EVIDENCES).orElseThrow();
		SchemaDTO schemaDTO = (SchemaDTO) state.value(TABLE_RELATION_OUTPUT).orElseThrow();

		// 检查SQL语法验证结果
		boolean isValidationFailed = state.value(SQL_VALIDATE_NODE_OUTPUT).map(v -> !(Boolean) v).orElse(false);

		if (isValidationFailed) {
			logger.info("SQL 语法校验未通过，开始重新生成SQL");
			return regenerateSql(state, input, evidenceList, schemaDTO, SQL_VALIDATE_EXCEPTION_OUTPUT);
		}

		// 检查语义一致性验证结果
		boolean isSemanticConsistencyFailed = state.value(SEMANTIC_CONSISTENC_NODE_OUTPUT)
			.map(v -> !(Boolean) v)
			.orElse(false);

		if (isSemanticConsistencyFailed) {
			logger.info("语义一致性校验未通过，开始重新生成SQL");
			return regenerateSql(state, input, evidenceList, schemaDTO, SEMANTIC_CONSISTENC_NODE_RECOMMEND_OUTPUT);
		}

		// 检查召回信息是否满足需求
		String recallInfoSatisfyRequirement = baseNl2SqlService.isRecallInfoSatisfyRequirement(input, schemaDTO,
				evidenceList);
		logger.info("召回信息是否满足需求：{}", recallInfoSatisfyRequirement);

		if (recallInfoSatisfyRequirement.startsWith("否") || recallInfoSatisfyRequirement.contains("**否")) {
			return handleUnsatisfiedRecallInfo(state, recallInfoSatisfyRequirement);
		}

		// 生成SQL
		logger.info("开始生成SQL");
		String sql = baseNl2SqlService.generateSql(evidenceList, input, schemaDTO);
		logger.info("生成的SQL为：{}", sql);

		Map<String, Object> result = Map.of(SQL_GENERATE_OUTPUT, sql, RESULT, sql);

		logger.info("{} 节点执行完成", this.getClass().getSimpleName());
		return result;
	}

	/**
	 * 重新生成SQL
	 */
	private Map<String, Object> regenerateSql(OverAllState state, String input, List<String> evidenceList,
			SchemaDTO schemaDTO, String exceptionOutputKey) throws Exception {
		String exceptionMessage = state.value(exceptionOutputKey)
			.map(String.class::cast)
			.orElseThrow(() -> new IllegalStateException("Exception message not found"));

		String originalSql = state.value(SQL_GENERATE_OUTPUT)
			.map(String.class::cast)
			.orElseThrow(() -> new IllegalStateException("Original SQL not found"));

		String newSql = baseNl2SqlService.generateSql(evidenceList, input, schemaDTO, originalSql, exceptionMessage);
		logger.info("重新生成的SQL为：{}", newSql);

		return Map.of(SQL_GENERATE_OUTPUT, newSql, RESULT, newSql);
	}

	/**
	 * 处理不满足需求的召回信息
	 */
	private Map<String, Object> handleUnsatisfiedRecallInfo(OverAllState state, String recallInfoSatisfyRequirement) {
		int sqlGenerateCount = state.value(SQL_GENERATE_COUNT).map(v -> (Integer) v + 1).orElse(1);

		logger.info(sqlGenerateCount == 1 ? "首次生成SQL" : "SQL生成次数增加到: {}", sqlGenerateCount);

		if (sqlGenerateCount <= 3) {
			logger.info("召回信息不满足需求，开始重新生成SQL");
			Map<String, Object> updated = new HashMap<>();
			updated.put(SQL_GENERATE_COUNT, sqlGenerateCount);
			updated.put(SQL_GENERATE_OUTPUT, SQL_GENERATE_SCHEMA_MISSING);

			String newAdvice = state.value(SQL_GENERATE_SCHEMA_MISSING_ADVICE)
				.map(v -> v + "\n" + recallInfoSatisfyRequirement)
				.orElse(recallInfoSatisfyRequirement);

			updated.put(SQL_GENERATE_SCHEMA_MISSING_ADVICE, newAdvice);

			if (!state.value(SQL_GENERATE_SCHEMA_MISSING_ADVICE).isPresent()) {
				logger.info("召回信息不满足需求，需要补充Schema信息");
			}

			return updated;
		}
		else {
			logger.info("召回信息不满足需求，尝试重新生成SQL失败，结束生成SQL");
			return Map.of(RESULT, recallInfoSatisfyRequirement, SQL_GENERATE_OUTPUT, END, SQL_GENERATE_COUNT, 0);
		}
	}

}
