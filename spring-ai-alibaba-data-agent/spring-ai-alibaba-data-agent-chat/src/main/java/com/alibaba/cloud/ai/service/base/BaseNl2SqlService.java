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

import com.alibaba.cloud.ai.connector.MdTableGenerator;
import com.alibaba.cloud.ai.connector.accessor.Accessor;
import com.alibaba.cloud.ai.connector.bo.DbQueryParameter;
import com.alibaba.cloud.ai.connector.bo.ResultSetBO;
import com.alibaba.cloud.ai.connector.config.DbConfig;
import com.alibaba.cloud.ai.dto.schema.ColumnDTO;
import com.alibaba.cloud.ai.dto.schema.SchemaDTO;
import com.alibaba.cloud.ai.dto.schema.TableDTO;
import com.alibaba.cloud.ai.prompt.PromptHelper;
import com.alibaba.cloud.ai.service.LlmService;

import com.alibaba.cloud.ai.util.MarkdownParser;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.document.Document;
import reactor.core.publisher.Flux;

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
import static com.alibaba.cloud.ai.prompt.PromptConstant.getQuestionExpansionPromptTemplate;
import static com.alibaba.cloud.ai.prompt.PromptHelper.buildMixMacSqlDbPrompt;
import static com.alibaba.cloud.ai.prompt.PromptHelper.buildMixSelectorPrompt;

public class BaseNl2SqlService {

	private static final Logger logger = LoggerFactory.getLogger(BaseNl2SqlService.class);

	protected final BaseVectorStoreService vectorStoreService;

	protected final BaseSchemaService schemaService;

	public final LlmService aiService;

	protected final Accessor dbAccessor;

	protected final DbConfig dbConfig;

	public BaseNl2SqlService(BaseVectorStoreService vectorStoreService, BaseSchemaService schemaService,
			LlmService aiService, Accessor dbAccessor, DbConfig dbConfig) {
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
		return rewriteStream(query, null);
	}

	public Flux<ChatResponse> rewriteStream(String query, String agentId) throws Exception {
		logger.info("Starting rewriteStream for query: {} with agentId: {}", query, agentId);

		// 处理时间表达式 - 将相对时间转换为具体时间
		String timeRewrittenQuery = processTimeExpressions(query);
		logger.debug("Time rewritten query: {} -> {}", query, timeRewrittenQuery);

		List<String> evidences = extractEvidences(timeRewrittenQuery, agentId);
		logger.debug("Extracted {} evidences for rewriteStream", evidences.size());
		SchemaDTO schemaDTO = select(timeRewrittenQuery, evidences, agentId);
		String prompt = PromptHelper.buildRewritePrompt(timeRewrittenQuery, schemaDTO, evidences);
		logger.debug("Built rewrite prompt for streaming");
		Flux<ChatResponse> result = aiService.streamCall(prompt);
		logger.info("RewriteStream completed for query: {}", query);
		return result;
	}

	public String rewrite(String query) throws Exception {
		logger.info("Starting rewrite for query: {}", query);

		// 处理时间表达式 - 将相对时间转换为具体时间
		String timeRewrittenQuery = processTimeExpressions(query);
		logger.debug("Time rewritten query: {} -> {}", query, timeRewrittenQuery);

		List<String> evidences = extractEvidences(timeRewrittenQuery);
		logger.debug("Extracted {} evidences for rewrite", evidences.size());
		SchemaDTO schemaDTO = select(timeRewrittenQuery, evidences);
		String prompt = PromptHelper.buildRewritePrompt(timeRewrittenQuery, schemaDTO, evidences);
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

	/**
	 * 处理查询中的时间表达式，将相对时间转换为具体时间
	 * @param query 原始查询
	 * @return 处理后的查询
	 */
	public String processTimeExpressions(String query) {
		try {
			logger.debug("Processing time expressions in query: {}", query);

			// 使用统一管理的提示词构建时间转换提示
			String timeConversionPrompt = PromptHelper.buildTimeConversionPrompt(query);

			// 调用模型进行时间转换
			String convertedQuery = aiService.call(timeConversionPrompt);

			if (!convertedQuery.equals(query)) {
				logger.info("Time expression conversion: {} -> {}", query, convertedQuery);
			}
			else {
				logger.debug("No time expressions found or converted in query: {}", query);
			}

			return convertedQuery;

		}
		catch (Exception e) {
			logger.warn("Failed to process time expressions using AI, using original query: {}", e.getMessage());
			return query;
		}
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
	 * Expand question into multiple differently expressed question variants
	 * @param query original question
	 * @return list containing original question and expanded questions
	 */
	public List<String> expandQuestion(String query) {
		logger.info("Starting question expansion for query: {}", query);
		try {
			// Build question expansion prompt
			Map<String, Object> params = new HashMap<>();
			params.put("question", query);
			String prompt = getQuestionExpansionPromptTemplate().render(params);

			// Call LLM to get expanded questions
			logger.debug("Calling LLM for question expansion");
			String content = aiService.call(prompt);

			// Parse JSON response
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
	 * Extract evidence
	 */
	public List<String> extractEvidences(String query) {
		return extractEvidences(query, null);
	}

	/**
	 * Extract evidence - supports agent isolation
	 */
	public List<String> extractEvidences(String query, String agentId) {
		logger.debug("Extracting evidences for query: {} with agentId: {}", query, agentId);
		List<Document> evidenceDocuments;
		if (agentId != null && !agentId.trim().isEmpty()) {
			evidenceDocuments = vectorStoreService.getDocumentsForAgent(agentId, query, "evidence");
		}
		else {
			evidenceDocuments = vectorStoreService.getDocuments(query, "evidence");
		}
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
		return select(query, evidenceList, null);
	}

	public SchemaDTO select(String query, List<String> evidenceList, String agentId) throws Exception {
		logger.debug("Starting schema selection for query: {} with {} evidences and agentId: {}", query,
				evidenceList.size(), agentId);
		List<String> keywords = extractKeywords(query, evidenceList);
		logger.debug("Using {} keywords for schema selection", keywords != null ? keywords.size() : 0);
		SchemaDTO schemaDTO;
		if (agentId != null) {
			schemaDTO = schemaService.mixRagForAgent(agentId, query, keywords);
		}
		else {
			schemaDTO = schemaService.mixRag(query, keywords);
		}
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

		// 时间处理已经在查询重写阶段完成，这里不再需要处理
		logger.debug("Time expressions already processed in rewrite phase");

		String newSql = "";
		if (sql != null && !sql.isEmpty()) {
			// Use professional SQL error repair prompt
			logger.debug("Using SQL error fixer for existing SQL: {}", sql);
			String errorFixerPrompt = PromptHelper.buildSqlErrorFixerPrompt(query, dbConfig, schemaDTO, evidenceList,
					sql, exceptionMessage);
			newSql = aiService.call(errorFixerPrompt);
			logger.info("SQL error fixing completed");
		}
		else {
			// Normal SQL generation process
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
		return fineSelect(schemaDTO, query, evidenceList, sqlGenerateSchemaMissingAdvice, null);
	}

	public SchemaDTO fineSelect(SchemaDTO schemaDTO, String query, List<String> evidenceList,
			String sqlGenerateSchemaMissingAdvice, DbConfig specificDbConfig) {
		logger.debug("Fine selecting schema for query: {} with {} evidences and specificDbConfig: {}", query,
				evidenceList.size(), specificDbConfig != null ? specificDbConfig.getUrl() : "default");

		// 增加具体的样例数据，让模型根据样例数据进行选择
		SchemaDTO enrichedSchema = enrichSchemaWithSampleData(schemaDTO, specificDbConfig);
		logger.debug("Schema enriched with sample data for {} tables",
				enrichedSchema.getTable() != null ? enrichedSchema.getTable().size() : 0);

		String prompt = buildMixSelectorPrompt(evidenceList, query, enrichedSchema);
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
				// Some scenarios may prompt exceptions, such as:
				// java.lang.IllegalStateException:
				// Please provide database schema information so I can filter relevant
				// tables based on your question.
				// TODO 目前异常接口直接返回500，未返回异常信息，后续优化将异常返回给用户
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

	/**
	 * 为Schema中的表和列添加样例数据，以帮助模型更好地理解数据内容和结构
	 * @param schemaDTO 原始的数据库模式信息
	 * @param specificDbConfig 特定的数据库配置，如果为null则使用默认配置
	 * @return 包含样例数据的Schema副本
	 */
	private SchemaDTO enrichSchemaWithSampleData(SchemaDTO schemaDTO, DbConfig specificDbConfig) {
		if (schemaDTO == null || schemaDTO.getTable() == null || schemaDTO.getTable().isEmpty()) {
			logger.debug("Schema is null or empty, skipping sample data enrichment");
			return schemaDTO;
		}

		// 使用传入的特定数据库配置，如果为null则使用默认配置
		DbConfig targetDbConfig = specificDbConfig != null ? specificDbConfig : dbConfig;
		logger.debug("Using database config: {}", targetDbConfig != null ? targetDbConfig.getUrl() : "null");

		// 检查数据库配置是否有效
		if (!isDatabaseConfigValid(targetDbConfig)) {
			logger.info("Database configuration is invalid, skipping sample data enrichment for all tables");
			return schemaDTO;
		}

		try {
			// 创建SchemaDTO的深拷贝以避免修改原始对象
			SchemaDTO enrichedSchema = copySchemaDTO(schemaDTO);

			// 为每个表获取样例数据
			for (TableDTO tableDTO : enrichedSchema.getTable()) {
				enrichTableWithSampleData(tableDTO, targetDbConfig);
			}

			logger.info("Successfully enriched schema with sample data for {} tables",
					enrichedSchema.getTable().size());
			return enrichedSchema;

		}
		catch (Exception e) {
			logger.warn("Failed to enrich schema with sample data, using original schema: {}", e.getMessage());
			return schemaDTO;
		}
	}

	/**
	 * 为单个表添加样例数据
	 * @param tableDTO 表信息对象
	 * @param dbConfig 数据库配置
	 */
	private void enrichTableWithSampleData(TableDTO tableDTO, DbConfig dbConfig) {
		if (tableDTO == null || tableDTO.getColumn() == null || tableDTO.getColumn().isEmpty()) {
			return;
		}

		logger.debug("Enriching table '{}' with sample table data for {} columns", tableDTO.getName(),
				tableDTO.getColumn().size());

		try {
			// 获取表的样例数据
			ResultSetBO tableData = getSampleDataForTable(tableDTO.getName(), dbConfig);
			if (tableData != null && tableData.getData() != null && !tableData.getData().isEmpty()) {
				// 将整行数据分配给对应的列
				distributeTableDataToColumns(tableDTO, tableData);
				logger.info("Successfully enriched table '{}' with {} sample rows", tableDTO.getName(),
						tableData.getData().size());
			}
			else {
				logger.debug("No sample data found for table '{}'", tableDTO.getName());
			}

		}
		catch (Exception e) {
			logger.warn("Failed to get sample data for table '{}': {}", tableDTO.getName(), e.getMessage());
		}
	}

	/**
	 * 获取表的样例数据
	 * @param tableName 表名
	 * @param dbConfig 数据库配置
	 * @return 表样例数据
	 */
	private ResultSetBO getSampleDataForTable(String tableName, DbConfig dbConfig) throws Exception {
		DbQueryParameter param = DbQueryParameter.from(dbConfig).setTable(tableName);
		return dbAccessor.scanTable(dbConfig, param);
	}

	/**
	 * 将表数据分配给对应的列
	 * @param tableDTO 表信息
	 * @param tableData 表样例数据
	 */
	private void distributeTableDataToColumns(TableDTO tableDTO, ResultSetBO tableData) {
		List<String> columnHeaders = tableData.getColumn();
		List<Map<String, String>> rows = tableData.getData();

		// 为每个列创建样例数据映射
		Map<String, List<String>> columnSamples = new HashMap<>();

		// 遍历每一行数据
		for (Map<String, String> row : rows) {
			for (String columnName : columnHeaders) {
				String value = row.get(columnName);

				if (value != null && !value.trim().isEmpty()) {
					columnSamples.computeIfAbsent(columnName, k -> new ArrayList<>()).add(value);
				}
			}
		}

		// 将样例数据分配给对应的列
		for (ColumnDTO columnDTO : tableDTO.getColumn()) {
			String columnName = columnDTO.getName();
			List<String> samples = columnSamples.get(columnName);

			if (samples != null && !samples.isEmpty()) {
				// 去重并限制样例数量
				List<String> filteredSamples = samples.stream()
					.filter(sample -> sample != null && !sample.trim().isEmpty())
					.distinct()
					.limit(5) // 最多保留5个样例值
					.collect(Collectors.toList());

				if (!filteredSamples.isEmpty()) {
					columnDTO.setSamples(filteredSamples);
					logger.debug("Added {} sample values for column '{}.{}': {}", filteredSamples.size(),
							tableDTO.getName(), columnName, filteredSamples);
				}
			}
		}
	}

	/**
	 * 检查数据库配置是否有效
	 * @param dbConfig 数据库配置
	 * @return true如果配置有效，false otherwise
	 */
	private boolean isDatabaseConfigValid(DbConfig dbConfig) {
		if (dbConfig == null) {
			logger.debug("dbConfig is null");
			return false;
		}

		if (dbAccessor == null) {
			logger.debug("dbAccessor is null");
			return false;
		}

		// 检查基本的连接信息
		boolean hasBasicInfo = dbConfig.getUrl() != null && !dbConfig.getUrl().trim().isEmpty()
				&& dbConfig.getUsername() != null && !dbConfig.getUsername().trim().isEmpty();

		if (!hasBasicInfo) {
			logger.debug("dbConfig missing basic connection info - url: {}, username: {}",
					dbConfig.getUrl() != null ? "present" : "null",
					dbConfig.getUsername() != null ? "present" : "null");
			return false;
		}

		return true;
	}

	/**
	 * 创建SchemaDTO的深拷贝
	 * @param originalSchema 原始Schema
	 * @return Schema的深拷贝
	 */
	private SchemaDTO copySchemaDTO(SchemaDTO originalSchema) {
		SchemaDTO copy = new SchemaDTO();
		copy.setName(originalSchema.getName());
		copy.setDescription(originalSchema.getDescription());
		copy.setTableCount(originalSchema.getTableCount());
		copy.setForeignKeys(originalSchema.getForeignKeys());

		if (originalSchema.getTable() != null) {
			List<TableDTO> copiedTables = originalSchema.getTable()
				.stream()
				.map(this::copyTableDTO)
				.collect(Collectors.toList());
			copy.setTable(copiedTables);
		}

		return copy;
	}

	/**
	 * 创建TableDTO的深拷贝
	 * @param originalTable 原始表
	 * @return 表的深拷贝
	 */
	private TableDTO copyTableDTO(TableDTO originalTable) {
		TableDTO copy = new TableDTO();
		copy.setName(originalTable.getName());
		copy.setDescription(originalTable.getDescription());
		copy.setPrimaryKeys(originalTable.getPrimaryKeys());

		if (originalTable.getColumn() != null) {
			List<ColumnDTO> copiedColumns = originalTable.getColumn()
				.stream()
				.map(this::copyColumnDTO)
				.collect(Collectors.toList());
			copy.setColumn(copiedColumns);
		}

		return copy;
	}

	/**
	 * 创建ColumnDTO的深拷贝
	 * @param originalColumn 原始列
	 * @return 列的深拷贝
	 */
	private ColumnDTO copyColumnDTO(ColumnDTO originalColumn) {
		ColumnDTO copy = new ColumnDTO();
		copy.setName(originalColumn.getName());
		copy.setDescription(originalColumn.getDescription());
		copy.setEnumeration(originalColumn.getEnumeration());
		copy.setRange(originalColumn.getRange());
		copy.setType(originalColumn.getType());
		copy.setMapping(originalColumn.getMapping());

		// 复制现有的样例数据
		if (originalColumn.getSamples() != null) {
			copy.setSamples(new ArrayList<>(originalColumn.getSamples()));
		}
		if (originalColumn.getData() != null) {
			copy.setData(new ArrayList<>(originalColumn.getData()));
		}

		return copy;
	}

}
