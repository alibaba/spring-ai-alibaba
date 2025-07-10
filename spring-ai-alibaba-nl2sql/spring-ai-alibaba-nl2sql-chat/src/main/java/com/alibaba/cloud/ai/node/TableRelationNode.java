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

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.cloud.ai.schema.SchemaDTO;
import com.alibaba.cloud.ai.service.base.BaseNl2SqlService;
import com.alibaba.cloud.ai.service.base.BaseSchemaService;
import com.alibaba.cloud.ai.util.StateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;

import java.util.List;
import java.util.Map;

import static com.alibaba.cloud.ai.constant.Constant.*;

/**
 * 推理表与字段间的关系，自动补全 Join、外键等复杂结构。
 *
 * @author zhangshenghang
 */
public class TableRelationNode implements NodeAction {

	private static final Logger logger = LoggerFactory.getLogger(TableRelationNode.class);

	private final BaseSchemaService baseSchemaService;

	private final BaseNl2SqlService baseNl2SqlService;

	public TableRelationNode(ChatClient.Builder chatClientBuilder, BaseSchemaService baseSchemaService,
			BaseNl2SqlService baseNl2SqlService) {
		this.baseSchemaService = baseSchemaService;
		this.baseNl2SqlService = baseNl2SqlService;
	}

	@Override
	public Map<String, Object> apply(OverAllState state) throws Exception {
		logger.info("进入 {} 节点", this.getClass().getSimpleName());

		// 获取必要的输入参数
		String input = StateUtils.getStringValue(state, INPUT_KEY);
		List<String> evidenceList = StateUtils.getListValue(state, EVIDENCES);
		List<Document> tableDocuments = StateUtils.getDocumentList(state, TABLE_DOCUMENTS_FOR_SCHEMA_OUTPUT);
		List<List<Document>> columnDocumentsByKeywords = StateUtils.getDocumentListList(state,
				COLUMN_DOCUMENTS_BY_KEYWORDS_OUTPUT);

		// 构建和处理Schema
		SchemaDTO schemaDTO = buildInitialSchema(columnDocumentsByKeywords, tableDocuments);
		SchemaDTO result = processSchemaSelection(schemaDTO, input, evidenceList, state);

		logger.info("[{}] Schema处理结果: {}", this.getClass().getSimpleName(), result);
		return Map.of(TABLE_RELATION_OUTPUT, result);
	}

	/**
	 * 构建初始Schema
	 */
	private SchemaDTO buildInitialSchema(List<List<Document>> columnDocumentsByKeywords,
			List<Document> tableDocuments) {
		SchemaDTO schemaDTO = new SchemaDTO();
		baseSchemaService.extractDatabaseName(schemaDTO);
		baseSchemaService.buildSchemaFromDocuments(columnDocumentsByKeywords, tableDocuments, schemaDTO);
		return schemaDTO;
	}

	/**
	 * 处理Schema选择
	 */
	private SchemaDTO processSchemaSelection(SchemaDTO schemaDTO, String input, List<String> evidenceList,
			OverAllState state) {
		String schemaAdvice = StateUtils.getStringValue(state, SQL_GENERATE_SCHEMA_MISSING_ADVICE, null);

		if (schemaAdvice != null) {
			logger.info("[{}] 使用Schema补充建议处理: {}", this.getClass().getSimpleName(), schemaAdvice);
			return baseNl2SqlService.fineSelect(schemaDTO, input, evidenceList, schemaAdvice);
		}
		else {
			logger.info("[{}] 执行常规Schema选择", this.getClass().getSimpleName());
			return baseNl2SqlService.fineSelect(schemaDTO, input, evidenceList);
		}
	}

}
