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
package com.alibaba.cloud.ai.service.base;

import com.alibaba.cloud.ai.dbconnector.DbAccessor;
import com.alibaba.cloud.ai.dbconnector.DbConfig;
import com.alibaba.cloud.ai.dbconnector.MdTableGenerator;
import com.alibaba.cloud.ai.dbconnector.bo.DbQueryParameter;
import com.alibaba.cloud.ai.dbconnector.bo.ResultSetBO;
import com.alibaba.cloud.ai.prompt.PromptHelper;
import com.alibaba.cloud.ai.schema.SchemaDTO;
import com.alibaba.cloud.ai.service.LlmService;
import com.alibaba.cloud.ai.util.DateTimeUtil;
import com.alibaba.cloud.ai.util.MarkdownParser;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.springframework.ai.document.Document;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.alibaba.cloud.ai.prompt.PromptHelper.buildMixSelectorPrompt;

public class BaseNl2SqlService {

	protected final BaseVectorStoreService vectorStoreService;

	protected final BaseSchemaService schemaService;

	public final LlmService aiService;

	protected final DbAccessor dbAccessor;

	protected final DbConfig dbConfig;

	public BaseNl2SqlService(BaseVectorStoreService vectorStoreService, BaseSchemaService schemaService,
			LlmService aiService, DbAccessor dbAccessor, DbConfig dbConfig) {
		this.vectorStoreService = vectorStoreService;
		this.schemaService = schemaService;
		this.aiService = aiService;
		this.dbAccessor = dbAccessor;
		this.dbConfig = dbConfig;
	}

	public String rewrite(String query) throws Exception {
		List<Document> evidenceDocuments = vectorStoreService.getDocuments(query, "evidence");
		List<String> evidences = evidenceDocuments.stream().map(Document::getText).collect(Collectors.toList());
		SchemaDTO schemaDTO = select(query, evidences);
		String prompt = PromptHelper.buildRewritePrompt(query, schemaDTO, evidences);
		String responseContent = aiService.call(prompt);
		String[] splits = responseContent.split("\\n");
		for (String line : splits) {
			if (line.startsWith("需求类型：")) {
				String content = line.substring(5).trim();
				if ("《自由闲聊》".equals(content)) {
					return "闲聊拒识";
				}
				else if ("《需要澄清》".equals(content)) {
					return "意图模糊需要澄清";
				}
			}
			else if (line.startsWith("需求内容：")) {
				query = line.substring(5);
			}
		}
		return query;
	}

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
		// TODO 时间处理暂时未应用
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
			List<String> tableList;
			try {
				tableList = new Gson().fromJson(jsonContent, new TypeToken<List<String>>() {
				}.getType());
			}
			catch (Exception e) {
				// 某些场景会提示异常，如：java.lang.IllegalStateException:
				// 请提供数据库schema信息以便我能够根据您的问题筛选出相关的表。
				// TODO 目前异常接口直接返回500，未返回向异常常信息，后续优化将异常返回给用户
				throw new IllegalStateException(jsonContent);
			}
			if (tableList != null && !tableList.isEmpty()) {
				Set<String> selectedTables = tableList.stream().map(String::toLowerCase).collect(Collectors.toSet());
				schemaDTO.getTable().removeIf(table -> !selectedTables.contains(table.getName().toLowerCase()));
			}
		}
		return schemaDTO;
	}

}
