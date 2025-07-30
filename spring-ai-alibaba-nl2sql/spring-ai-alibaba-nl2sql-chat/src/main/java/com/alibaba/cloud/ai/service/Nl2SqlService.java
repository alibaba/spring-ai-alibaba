/*
 * Copyright 2024-2025 the original author or authors.
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
package com.alibaba.cloud.ai.service;

import com.alibaba.cloud.ai.constant.Constant;
import com.alibaba.cloud.ai.dto.schema.SchemaDTO;
import com.alibaba.cloud.ai.service.base.BaseNl2SqlService;
import com.alibaba.cloud.ai.service.base.BaseSchemaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * NL2SQL接口预留
 *
 * @author vlsmb
 * @since 2025/7/27
 */
@Service
public class Nl2SqlService {

	private static final Logger log = LoggerFactory.getLogger(Nl2SqlService.class);

	private final BaseNl2SqlService baseNl2SqlService;

	private final BaseSchemaService baseSchemaService;

	public Nl2SqlService(@Qualifier("nl2SqlServiceImpl") BaseNl2SqlService baseNl2SqlService,
			@Qualifier("schemaServiceImpl") BaseSchemaService baseSchemaService) {
		this.baseNl2SqlService = baseNl2SqlService;
		this.baseSchemaService = baseSchemaService;
	}

	/**
	 * 根据Nl2Sql-Graph的定义，抽取其自然语言转化为sql的功能代码
	 * @param query 自然语言
	 * @return sql语言
	 */
	public String apply(String query) throws Exception {
		// 1. query rewrite
		query = baseNl2SqlService.rewrite(query);
		if (Constant.INTENT_UNCLEAR.equals(query) || Constant.SMALL_TALK_REJECT.equals(query)) {
			throw new IllegalArgumentException("输入的自然语言属于【".concat(query).concat("】，无法转为SQL语言"));
		}
		log.info("问题重写结果：{}", query);

		// 2. keyword extract
		List<String> expandedQuestions = baseNl2SqlService.expandQuestion(query);
		log.info("问题扩展结果: {}", expandedQuestions);
		List<String> evidences = baseNl2SqlService.extractEvidences(query);
		List<String> keywords = baseNl2SqlService.extractKeywords(query, evidences);
		log.info("增强提取结果 - 证据: {}, 关键词: {}", evidences, keywords);

		// 3. schema recall
		List<Document> tableDocuments = baseSchemaService.getTableDocuments(query);
		List<List<Document>> columnDocumentsByKeywords = baseSchemaService.getColumnDocumentsByKeywords(keywords);
		log.info("Schema recall results - table documents count: {}, keyword-related column document groups: {}",
				tableDocuments.size(), columnDocumentsByKeywords.size());

		// 4. table relation
		SchemaDTO schemaDTO = new SchemaDTO();
		baseSchemaService.extractDatabaseName(schemaDTO);
		baseSchemaService.buildSchemaFromDocuments(columnDocumentsByKeywords, tableDocuments, schemaDTO);
		log.info("Executing regular schema selection");
		schemaDTO = baseNl2SqlService.fineSelect(schemaDTO, query, evidences);
		log.info("Schema result: {}", schemaDTO);

		// 5. nl2sql
		return baseNl2SqlService.generateSql(evidences, query, schemaDTO);
	}

}
