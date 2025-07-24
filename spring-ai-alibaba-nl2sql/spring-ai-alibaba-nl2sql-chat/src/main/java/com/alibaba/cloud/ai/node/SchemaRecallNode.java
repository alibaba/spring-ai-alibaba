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
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.cloud.ai.service.base.BaseSchemaService;
import com.alibaba.cloud.ai.util.ChatResponseUtil;
import com.alibaba.cloud.ai.util.StateUtils;
import com.alibaba.cloud.ai.util.StreamingChatGeneratorUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.document.Document;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

import static com.alibaba.cloud.ai.constant.Constant.*;

/**
 * Schema recall node that retrieves relevant database schema information based on
 * keywords and intent.
 *
 * This node is responsible for: - Recalling relevant tables based on user input -
 * Retrieving column documents based on extracted keywords - Organizing schema information
 * for subsequent processing - Providing streaming feedback during recall process
 *
 * @author zhangshenghang
 */
public class SchemaRecallNode implements NodeAction {

	private static final Logger logger = LoggerFactory.getLogger(SchemaRecallNode.class);

	private final BaseSchemaService baseSchemaService;

	public SchemaRecallNode(BaseSchemaService baseSchemaService) {
		this.baseSchemaService = baseSchemaService;
	}

	@Override
	public Map<String, Object> apply(OverAllState state) throws Exception {
		logger.info("Entering {} node", this.getClass().getSimpleName());

		String input = StateUtils.getStringValue(state, INPUT_KEY);
		List<String> keywords = StateUtils.getListValue(state, KEYWORD_EXTRACT_NODE_OUTPUT);

		// Execute business logic first - recall schema information immediately
		List<Document> tableDocuments = baseSchemaService.getTableDocuments(input);
		List<List<Document>> columnDocumentsByKeywords = baseSchemaService.getColumnDocumentsByKeywords(keywords);

		logger.info(
				"[{}] Schema recall results - table documents count: {}, keyword-related column document groups: {}",
				this.getClass().getSimpleName(), tableDocuments.size(), columnDocumentsByKeywords.size());

		Flux<ChatResponse> displayFlux = Flux.create(emitter -> {
			emitter.next(ChatResponseUtil.createCustomStatusResponse("开始召回Schema信息..."));
			emitter.next(ChatResponseUtil.createCustomStatusResponse("表信息召回完成，数量: " + tableDocuments.size()));
			emitter
				.next(ChatResponseUtil.createCustomStatusResponse("列信息召回完成，数量: " + columnDocumentsByKeywords.size()));
			emitter.next(ChatResponseUtil.createCustomStatusResponse("Schema信息召回完成."));
			emitter.complete();
		});

		var generator = StreamingChatGeneratorUtil.createStreamingGeneratorWithMessages(this.getClass(), state,
				currentState -> {
					logger.info("Table document details: {}", tableDocuments);
					logger.info("Keyword-related column document details: {}", columnDocumentsByKeywords);
					return Map.of(TABLE_DOCUMENTS_FOR_SCHEMA_OUTPUT, tableDocuments,
							COLUMN_DOCUMENTS_BY_KEYWORDS_OUTPUT, columnDocumentsByKeywords);
				}, displayFlux, StreamResponseType.SCHEMA_RECALL);

		// Return the processing result
		return Map.of(SCHEMA_RECALL_NODE_OUTPUT, generator);
	}

}
