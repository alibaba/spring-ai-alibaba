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
import com.alibaba.cloud.ai.dto.schema.SchemaDTO;
import com.alibaba.cloud.ai.service.LlmService;
import com.alibaba.cloud.ai.util.DateTimeUtil;
import com.alibaba.cloud.ai.util.MarkdownParser;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.document.Document;
import reactor.core.publisher.Flux;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.alibaba.cloud.ai.constant.Constant.INTENT_UNCLEAR;
import static com.alibaba.cloud.ai.constant.Constant.SMALL_TALK_REJECT;
import static com.alibaba.cloud.ai.prompt.PromptHelper.buildMixMacSqlDbPrompt;
import static com.alibaba.cloud.ai.prompt.PromptHelper.buildMixSelectorPrompt;
import static com.alibaba.cloud.ai.prompt.PromptConstant.getQuestionExpansionPromptTemplate;

public class BaseNl2SqlService {

	private static final Logger logger = LoggerFactory.getLogger(BaseNl2SqlService.class);

	protected final BaseVectorStoreService vectorStoreService;

	protected final BaseSchemaService schemaService;

	public final LlmService aiService;

	protected final DbAccessor dbAccessor;

	protected final DbConfig dbConfig;

	public BaseNl2SqlService(BaseVectorStoreService vectorStoreService, BaseSchemaService schemaService,
			LlmService aiService, DbAccessor dbAccessor, DbConfig dbConfig) {
		logger.info(
				"Initializing BaseNl2SqlService with components: vectorStoreService={}, schemaService={}, aiService={}, dbAccessor={}, dbConfig={}",
				vectorStoreService.getClass().getSimpleName(), schemaService.getClass().getSimpleName(),
				aiService.getClass().getSimpleName(), dbAccessor.getClass().getSimpleName(),
				dbConfig != null ? dbConfig.getClass().getSimpleName() : "null");
		this.vectorStoreService = vectorStoreService;
		this.schemaService = schemaService;
		this.aiService = aiService;
		this.dbAccessor = dbAccessor;
		this.dbConfig = dbConfig;
		logger.info("BaseNl2SqlService initialized successfully");
	}

	public Flux<ChatResponse> rewriteStream(String query) throws Exception {
		logger.info("Starting rewriteStream for query: {}", query);
		List<String> evidences = extractEvidences(query);
		logger.debug("Extracted {} evidences for rewriteStream", evidences.size());
		SchemaDTO schemaDTO = select(query, evidences);
		String prompt = PromptHelper.buildRewritePrompt(query, schemaDTO, evidences);
		logger.debug("Built rewrite prompt for streaming");
		Flux<ChatResponse> result = aiService.streamCall(prompt);
		logger.info("RewriteStream completed for query: {}", query);
		return result;
	}

	public String rewrite(String query) throws Exception {
		logger.info("Starting rewrite for query: {}", query);
		List<String> evidences = extractEvidences(query);
		logger.debug("Extracted {} evidences for rewrite", evidences.size());
		SchemaDTO schemaDTO = select(query, evidences);
		String prompt = PromptHelper.buildRewritePrompt(query, schemaDTO, evidences);
		logger.debug("Built rewrite prompt, calling LLM");
		String responseContent = aiService.call(prompt);
		String[] splits = responseContent.split("\n");
		logger.debug("Processing LLM response with {} lines", splits.length);
		for (String line : splits) {
			if (line.startsWith("需求类型：")) {
				String content = line.substring(5).trim();
				logger.debug("Detected request type: {}", content);
				if ("《自由闲聊》".equals(content)) {
					logger.info("Rewrite result: SMALL_TALK_REJECT for query: {}", query);
					return SMALL_TALK_REJECT;
				}
				else if ("《需要澄清》".equals(content)) {
					logger.info("Rewrite result: INTENT_UNCLEAR for query: {}", query);
					return INTENT_UNCLEAR;
				}
			}
			else if (line.startsWith("需求内容：")) {
				query = line.substring(5);
				logger.debug("Extracted rewritten query: {}", query);
			}
		}
		logger.info("Rewrite completed successfully for query: {}", query);
		return query;
	}

	public String nl2sql(String query) throws Exception {
		logger.info("Starting nl2sql conversion for query: {}", query);
		List<String> evidences = extractEvidences(query);
		logger.debug("Extracted {} evidences for nl2sql", evidences.size());
		SchemaDTO schemaDTO = select(query, evidences);
		String sql = generateSql(evidences, query, schemaDTO);
		logger.info("Nl2sql conversion completed. Generated SQL: {}", sql);
		return sql;
	}

	public String executeSql(String sql) throws Exception {
		logger.info("Executing SQL: {}", sql);
		try {
			DbQueryParameter param = DbQueryParameter.from(dbConfig).setSql(sql);
			logger.debug("Created DbQueryParameter for SQL execution");
			ResultSetBO resultSet = dbAccessor.executeSqlAndReturnObject(dbConfig, param);
			logger.debug("SQL executed successfully, generating table format");
			String result = MdTableGenerator.generateTable(resultSet);
			logger.info("SQL execution completed successfully, result rows: {}",
					resultSet.getData() != null ? resultSet.getData().size() : 0);
			return result;
		}
		catch (Exception e) {
			logger.error("Failed to execute SQL: {}", sql, e);
			throw e;
		}
	}

	@Deprecated
	public String semanticConsistency(String sql, String queryPrompt) throws Exception {
		String semanticConsistencyPrompt = PromptHelper.buildSemanticConsistenPrompt(queryPrompt, sql);
		String call = aiService.call(semanticConsistencyPrompt);
		return call;
	}

	public Flux<ChatResponse> semanticConsistencyStream(String sql, String queryPrompt) throws Exception {
		String semanticConsistencyPrompt = PromptHelper.buildSemanticConsistenPrompt(queryPrompt, sql);
		logger.info("semanticConsistencyPrompt = {}", semanticConsistencyPrompt);
		return aiService.streamCall(semanticConsistencyPrompt);
	}

	/**
	 * 将问题扩展为多个不同表述的问题变体
	 * @param query 原始问题
	 * @return 包含原始问题和扩展问题的列表
	 */
	public List<String> expandQuestion(String query) {
		logger.info("Starting question expansion for query: {}", query);
		try {
			// 构建问题扩展提示词
			Map<String, Object> params = new HashMap<>();
			params.put("question", query);
			String prompt = getQuestionExpansionPromptTemplate().render(params);

			// 调用LLM获取扩展问题
			logger.debug("Calling LLM for question expansion");
			String content = aiService.call(prompt);

			// 解析JSON响应
			List<String> expandedQuestions = new Gson().fromJson(content, new TypeToken<List<String>>() {
			}.getType());

			if (expandedQuestions == null || expandedQuestions.isEmpty()) {
				logger.warn("No expanded questions generated, returning original query");
				return Collections.singletonList(query);
			}

			logger.info("Question expansion completed successfully: {} questions generated", expandedQuestions.size());
			logger.debug("Expanded questions: {}", expandedQuestions);
			return expandedQuestions;
		}
		catch (Exception e) {
			logger.warn("Question expansion failed, returning original query: {}", e.getMessage());
			return Collections.singletonList(query);
		}
	}

	/**
	 * 抽取证据
	 */
	public List<String> extractEvidences(String query) {
		logger.debug("Extracting evidences for query: {}", query);
		List<Document> evidenceDocuments = vectorStoreService.getDocuments(query, "evidence");
		List<String> evidences = evidenceDocuments.stream().map(Document::getText).collect(Collectors.toList());
		logger.debug("Extracted {} evidences: {}", evidences.size(), evidences);
		return evidences;
	}

	public List<String> extractKeywords(String query, List<String> evidenceList) {
		logger.debug("Extracting keywords from query: {} with {} evidences", query, evidenceList.size());
		StringBuilder queryBuilder = new StringBuilder(query);
		for (String evidence : evidenceList) {
			queryBuilder.append(evidence).append("。");
		}
		query = queryBuilder.toString();

		String prompt = PromptHelper.buildQueryToKeywordsPrompt(query);
		logger.debug("Calling LLM for keyword extraction");
		String content = aiService.call(prompt);

		List<String> keywords = new Gson().fromJson(content, new TypeToken<List<String>>() {
		}.getType());
		logger.debug("Extracted {} keywords: {}", keywords != null ? keywords.size() : 0, keywords);
		return keywords;
	}

	public SchemaDTO select(String query, List<String> evidenceList) throws Exception {
		logger.debug("Starting schema selection for query: {} with {} evidences", query, evidenceList.size());
		List<String> keywords = extractKeywords(query, evidenceList);
		logger.debug("Using {} keywords for schema selection", keywords != null ? keywords.size() : 0);
		SchemaDTO schemaDTO = schemaService.mixRag(query, keywords);
		logger.debug("Retrieved schema with {} tables", schemaDTO.getTable() != null ? schemaDTO.getTable().size() : 0);
		SchemaDTO result = fineSelect(schemaDTO, query, evidenceList);
		logger.debug("Fine selection completed, final schema has {} tables",
				result.getTable() != null ? result.getTable().size() : 0);
		return result;
	}

	public String isRecallInfoSatisfyRequirement(String query, SchemaDTO schemaDTO, List<String> evidenceList) {
		logger.debug("Checking if recall info satisfies requirement for query: {}", query);
		String prompt = PromptHelper.mixSqlGeneratorSystemCheckPrompt(query, dbConfig, schemaDTO, evidenceList);
		logger.debug("Calling LLM for requirement satisfaction check");
		String result = aiService.call(prompt);
		logger.debug("Requirement satisfaction check result: {}", result);
		return result;
	}

	public String generateSql(List<String> evidenceList, String query, SchemaDTO schemaDTO, String sql,
			String exceptionMessage) throws Exception {
		logger.info("Generating SQL for query: {}, hasExistingSql: {}", query, sql != null && !sql.isEmpty());

		// TODO 时间处理暂时未应用
		String dateTimeExtractPrompt = PromptHelper.buildDateTimeExtractPrompt(query);
		logger.debug("Extracting datetime expressions");
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
		logger.debug("Processed {} datetime expressions", dateTimeList.size());

		String newSql = "";
		if (sql != null && !sql.isEmpty()) {
			// 使用专业的SQL错误修复提示词
			logger.debug("Using SQL error fixer for existing SQL: {}", sql);
			String errorFixerPrompt = PromptHelper.buildSqlErrorFixerPrompt(query, dbConfig, schemaDTO, evidenceList,
					sql, exceptionMessage);
			newSql = aiService.call(errorFixerPrompt);
			logger.info("SQL error fixing completed");
		}
		else {
			// 正常的SQL生成流程
			logger.debug("Generating new SQL from scratch");
			List<String> prompts = PromptHelper.buildMixSqlGeneratorPrompt(query, dbConfig, schemaDTO, evidenceList);
			newSql = aiService.callWithSystemPrompt(prompts.get(0), prompts.get(1));
			logger.info("New SQL generation completed");
		}

		String result = MarkdownParser.extractRawText(newSql).trim();
		logger.info("Final generated SQL: {}", result);
		return result;
	}

	public String generateSql(List<String> evidenceList, String query, SchemaDTO schemaDTO) throws Exception {
		return generateSql(evidenceList, query, schemaDTO, null, null);
	}

	public Set<String> fineSelect(SchemaDTO schemaDTO, String sqlGenerateSchemaMissingAdvice) {
		logger.debug("Fine selecting tables based on advice: {}", sqlGenerateSchemaMissingAdvice);
		String schemaInfo = buildMixMacSqlDbPrompt(schemaDTO, true);
		String prompt = " 建议：" + sqlGenerateSchemaMissingAdvice
				+ " \n 请按照建议进行返回相关表的名称，只返回建议中提到的表名，返回格式为：[\"a\",\"b\",\"c\"] \n " + schemaInfo;
		logger.debug("Calling LLM for table selection with advice");
		String content = aiService.call(prompt);
		if (content != null && !content.trim().isEmpty()) {
			String jsonContent = MarkdownParser.extractText(content);
			List<String> tableList;
			try {
				tableList = new Gson().fromJson(jsonContent, new TypeToken<List<String>>() {
				}.getType());
			}
			catch (Exception e) {
				logger.error("Failed to parse table selection response: {}", jsonContent, e);
				throw new IllegalStateException(jsonContent);
			}
			if (tableList != null && !tableList.isEmpty()) {
				Set<String> selectedTables = tableList.stream().map(String::toLowerCase).collect(Collectors.toSet());
				logger.debug("Selected {} tables based on advice: {}", selectedTables.size(), selectedTables);
				return selectedTables;
			}
		}
		logger.debug("No tables selected based on advice");
		return new HashSet<>();
	}

	public SchemaDTO fineSelect(SchemaDTO schemaDTO, String query, List<String> evidenceList) {
		return fineSelect(schemaDTO, query, evidenceList, null);
	}

	public SchemaDTO fineSelect(SchemaDTO schemaDTO, String query, List<String> evidenceList,
			String sqlGenerateSchemaMissingAdvice) {
		logger.debug("Fine selecting schema for query: {} with {} evidences", query, evidenceList.size());
		String prompt = buildMixSelectorPrompt(evidenceList, query, schemaDTO);
		logger.debug("Calling LLM for schema fine selection");
		String content = aiService.call(prompt);
		Set<String> selectedTables = new HashSet<>();

		if (sqlGenerateSchemaMissingAdvice != null) {
			logger.debug("Adding tables from schema missing advice");
			selectedTables.addAll(this.fineSelect(schemaDTO, sqlGenerateSchemaMissingAdvice));
		}

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
				logger.error("Failed to parse fine selection response: {}", jsonContent, e);
				throw new IllegalStateException(jsonContent);
			}
			if (tableList != null && !tableList.isEmpty()) {
				selectedTables.addAll(tableList.stream().map(String::toLowerCase).collect(Collectors.toSet()));
				int originalTableCount = schemaDTO.getTable() != null ? schemaDTO.getTable().size() : 0;
				schemaDTO.getTable().removeIf(table -> !selectedTables.contains(table.getName().toLowerCase()));
				int finalTableCount = schemaDTO.getTable() != null ? schemaDTO.getTable().size() : 0;
				logger.debug("Fine selection completed: {} -> {} tables, selected tables: {}", originalTableCount,
						finalTableCount, selectedTables);
			}
		}
		return schemaDTO;
	}

}
