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
import com.alibaba.cloud.ai.prompt.PromptHelper;
import com.alibaba.cloud.ai.schema.SchemaDTO;
import com.alibaba.cloud.ai.service.base.BaseNl2SqlService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;

import java.util.List;
import java.util.Map;

import static com.alibaba.cloud.ai.constant.Constant.*;

/**
 * 语义一致性校验
 *
 * @author zhangshenghang
 */
public class SemanticConsistencNode implements NodeAction {

	private static final Logger logger = LoggerFactory.getLogger(SemanticConsistencNode.class);

	private final ChatClient chatClient;

	private final DbConfig dbConfig;

	private final BaseNl2SqlService baseNl2SqlService;

	public SemanticConsistencNode(ChatClient.Builder chatClientBuilder, BaseNl2SqlService baseNl2SqlService,
			DbConfig dbConfig) {
		this.chatClient = chatClientBuilder.build();
		this.dbConfig = dbConfig;
		this.baseNl2SqlService = baseNl2SqlService;
	}

	@Override
	public Map<String, Object> apply(OverAllState state) throws Exception {
		logger.info("进入 {} 节点", this.getClass().getSimpleName());

		// 获取必要的输入参数
		String input = state.value(INPUT_KEY)
			.map(String.class::cast)
			.orElseThrow(() -> new IllegalStateException("Input key not found"));

		List<String> evidenceList = state.value(EVIDENCES)
			.map(v -> (List<String>) v)
			.orElseThrow(() -> new IllegalStateException("Evidence list not found"));

		SchemaDTO schemaDTO = state.value(TABLE_RELATION_OUTPUT)
			.map(SchemaDTO.class::cast)
			.orElseThrow(() -> new IllegalStateException("Schema DTO not found"));

		String sql = state.value(SQL_GENERATE_OUTPUT)
			.map(String.class::cast)
			.orElseThrow(() -> new IllegalStateException("SQL not found"));

		// 构建和执行语义一致性检查
		List<String> prompts = PromptHelper.buildMixSqlGeneratorPrompt(input, dbConfig, schemaDTO, evidenceList);
		String semanticConsistency = baseNl2SqlService.semanticConsistency(sql,
				String.join("\n", prompts.get(0), prompts.get(1)));

		logger.info("语义一致性校验结果详情: {}", semanticConsistency);
		boolean passed = !semanticConsistency.startsWith("不通过");
		logger.info("语义一致性校验结果: {}", passed);

		// 根据校验结果返回相应的状态
		return passed ? Map.of(SEMANTIC_CONSISTENC_NODE_OUTPUT, true) : Map.of(SEMANTIC_CONSISTENC_NODE_OUTPUT, false,
				SEMANTIC_CONSISTENC_NODE_RECOMMEND_OUTPUT, semanticConsistency);
	}

}
