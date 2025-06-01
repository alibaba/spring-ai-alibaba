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

import com.alibaba.cloud.ai.dbconnector.DbAccessor;
import com.alibaba.cloud.ai.dbconnector.DbConfig;
import com.alibaba.cloud.ai.dbconnector.MdTableGenerator;
import com.alibaba.cloud.ai.dbconnector.bo.DbQueryParameter;
import com.alibaba.cloud.ai.dbconnector.bo.ResultSetBO;
import com.alibaba.cloud.ai.prompt.PromptHelper;
import com.alibaba.cloud.ai.schema.SchemaDTO;
import com.alibaba.cloud.ai.util.DateTimeUtil;
import com.alibaba.cloud.ai.util.MarkdownParser;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static com.alibaba.cloud.ai.prompt.PromptHelper.buildMixSelectorPrompt;

@Service
public class Nl2SqlService {

	@Autowired
	private VectorStoreService vectorStoreService;

	@Autowired
	private SchemaService schemaService;

	@Autowired
	public LlmService aiService;

	@Autowired
	private DbAccessor dbAccessor;

	@Autowired
	private DbConfig dbConfig;

	public String nl2sql(String query) throws Exception {
		List<Document> evidenceDocuments = vectorStoreService.getDocuments(query, "evidence");
		List<String> evidences = evidenceDocuments.stream().map(Document::getText).collect(Collectors.toList());
		SchemaDTO schemaDTO = select(query, evidences);
		return generateSql(evidences, query, schemaDTO);
	}

	public String executeSql(String sql) throws Exception {
		DbQueryParameter param = DbQueryParameter.from(dbConfig).setSql(sql);
		ResultSetBO resultSet = dbAccessor.executeSqlAndReturnObject(dbConfig, param);
		return MdTableGenerator.generateTable(resultSet);
	}

	public SchemaDTO select(String query, List<String> evidenceList) throws Exception {
		StringBuilder queryBuilder = new StringBuilder(query);
		for (String evidence : evidenceList) {
			queryBuilder.append(evidence).append("。");
		}
		query = queryBuilder.toString();

		String prompt = PromptHelper.buildQueryToKeywordsPrompt(query);
		String content = aiService.call(prompt);

		List<String> keywords = new Gson().fromJson(content, new TypeToken<List<String>>() {
		}.getType());
		SchemaDTO schemaDTO = schemaService.mixRag(query, keywords);
		return fineSelect(schemaDTO, query, evidenceList);
	}

	public String generateSql(List<String> evidenceList, String query, SchemaDTO schemaDTO) throws Exception {
		String dateTimeExtractPrompt = PromptHelper.buildDateTimeExtractPrompt(query);
		String content = aiService.call(dateTimeExtractPrompt);
		List<String> dateTimeList = new ArrayList<>();
		LocalDate now = LocalDate.now();
		List<String> expressionList = new Gson().fromJson(content, new TypeToken<List<String>>() {
		}.getType());
		List<String> dateTimeExpressions = DateTimeUtil.buildDateExpressions(expressionList, now);
		for (String dateTimeExpression : dateTimeExpressions) {
			if (dateTimeExpression.endsWith("=")) {
				continue;
			}
			dateTimeList.add(dateTimeExpression.replace("=", "指的是"));
		}
		expressionList.addAll(dateTimeList);

		List<String> prompts = PromptHelper.buildMixSqlGeneratorPrompt(query, dbConfig, schemaDTO, evidenceList);
		return MarkdownParser.extractRawText(aiService.callWithSystemPrompt(prompts.get(0), prompts.get(1))).trim();
	}

	public SchemaDTO fineSelect(SchemaDTO schemaDTO, String query, List<String> evidenceList) {
		String prompt = buildMixSelectorPrompt(evidenceList, query, schemaDTO);
		String content = aiService.call(prompt);

		if (content != null && !content.trim().isEmpty()) {
			String jsonContent = MarkdownParser.extractText(content);
			List<String> tableList = new Gson().fromJson(jsonContent, new TypeToken<List<String>>() {
			}.getType());

			Set<String> selectedTables = tableList.stream().map(String::toLowerCase).collect(Collectors.toSet());

			schemaDTO.getTable().removeIf(table -> !selectedTables.contains(table.getName().toLowerCase()));
		}
		return schemaDTO;
	}

}
