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
import com.alibaba.cloud.ai.service.base.BaseSchemaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;

import java.util.List;
import java.util.Map;

import static com.alibaba.cloud.ai.constant.Constant.*;

/**
 * 根据关键词和意图，召回相关表、字段、关系等数据库 Schema 信息。
 *
 * @author zhangshenghang
 */
public class SchemaRecallNode implements NodeAction {

	private static final Logger logger = LoggerFactory.getLogger(SchemaRecallNode.class);

	private final ChatClient chatClient;

	private final BaseSchemaService baseSchemaService;

	public SchemaRecallNode(ChatClient.Builder chatClientBuilder, BaseSchemaService baseSchemaService) {
		this.chatClient = chatClientBuilder.build();
		this.baseSchemaService = baseSchemaService;
	}

	@Override
	public Map<String, Object> apply(OverAllState state) throws Exception {
		logger.info("进入 {} 节点", this.getClass().getSimpleName());

		// 获取必要的输入参数
		String input = state.value(INPUT_KEY)
			.map(String.class::cast)
			.orElseThrow(() -> new IllegalStateException("Input key not found"));

		List<String> keywords = state.value(KEYWORD_EXTRACT_NODE_OUTPUT)
			.map(v -> (List<String>) v)
			.orElseThrow(() -> new IllegalStateException("Keywords not found"));

		// 获取表和列的文档信息
		List<Document> tableDocuments = baseSchemaService.getTableDocuments(input);
		List<List<Document>> columnDocumentsByKeywords = baseSchemaService.getColumnDocumentsByKeywords(keywords);

		// 记录处理结果
		logger.info("[{}] Schema召回结果 - 表文档数量: {}, 关键词相关列文档组数: {}", this.getClass().getSimpleName(),
				tableDocuments.size(), columnDocumentsByKeywords.size());

		// 返回处理结果
		return Map.of(TABLE_DOCUMENTS_FOR_SCHEMA_OUTPUT, tableDocuments, COLUMN_DOCUMENTS_BY_KEYWORDS_OUTPUT,
				columnDocumentsByKeywords);
	}

}
