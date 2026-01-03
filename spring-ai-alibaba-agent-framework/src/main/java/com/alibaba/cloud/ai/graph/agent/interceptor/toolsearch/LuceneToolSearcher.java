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
package com.alibaba.cloud.ai.graph.agent.interceptor.toolsearch;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.Directory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

import java.io.IOException;
import java.util.*;

/**
 * 基于Lucene的工具搜索器实现
 */
public class LuceneToolSearcher implements ToolSearcher {

	private static final Logger log = LoggerFactory.getLogger(LuceneToolSearcher.class);

	private final Directory indexDirectory;

	private final Analyzer analyzer;

	private final ObjectMapper objectMapper;

	private final Map<String, Float> fieldBoosts;

	private final List<String> indexFields;

	private IndexSearcher indexSearcher;

	private final Map<String, ToolCallback> toolCallbackMap = new HashMap<>();


	public LuceneToolSearcher() {
		this(builder());
	}

	private LuceneToolSearcher(Builder builder) {
		this.indexDirectory = builder.indexDirectory != null ? builder.indexDirectory : new ByteBuffersDirectory();
		this.analyzer = builder.analyzer != null ? builder.analyzer : new StandardAnalyzer();
		this.objectMapper = new ObjectMapper();
		this.fieldBoosts = new HashMap<>(builder.fieldBoosts);
		this.indexFields = new ArrayList<>(builder.indexFields);
	}


	public static Builder builder() {
		return new Builder();
	}

	@Override
	public void indexTools(List<ToolCallback> tools) {
		try {
			IndexWriterConfig config = new IndexWriterConfig(analyzer);
			IndexWriter indexWriter = new IndexWriter(indexDirectory, config);

			for (ToolCallback tool : tools) {
				ToolDefinition definition = tool.getToolDefinition();
				Document doc = new Document();

				for (String fieldName : indexFields) {
					String fieldValue = getFieldValue(definition, fieldName);
					if (fieldValue != null && !fieldValue.isEmpty()) {
						doc.add(new TextField(fieldName, fieldValue, Field.Store.YES));
					}
				}

				// 存储完整的Schema
				doc.add(new StoredField("schema", getToolSchema(tool)));

				indexWriter.addDocument(doc);

				// 缓存 ToolCallback
				toolCallbackMap.put(definition.name(), tool);
			}

			indexWriter.commit();
			indexWriter.close();

			// 创建搜索器
			DirectoryReader indexReader = DirectoryReader.open(indexDirectory);
			this.indexSearcher = new IndexSearcher(indexReader);

			log.info("Successfully indexed {} tools with fields: {}", tools.size(), indexFields);
		}
		catch (IOException e) {
			throw new RuntimeException("Failed to index tools", e);
		}
	}

	/**
	 * 从 ToolDefinition 中获取指定字段的值
	 */
	private String getFieldValue(ToolDefinition definition, String fieldName) {
		switch (fieldName) {
			case "name":
				return definition.name();
			case "description":
				return definition.description();
			case "parameters":
				return definition.inputSchema();
			default:
				return null;
		}
	}

	@Override
	public List<ToolCallback> search(String query, int maxResults) {
		if (indexSearcher == null) {
			throw new IllegalStateException("Tools not indexed yet. Call indexTools() first.");
		}

		try {
			// 使用配置的字段和权重构建多字段查询
			String[] fields = indexFields.toArray(new String[0]);
			MultiFieldQueryParser parser = new MultiFieldQueryParser(fields, analyzer, fieldBoosts);

			// 转义特殊字符
			String escapedQuery = QueryParser.escape(query);
			Query luceneQuery = parser.parse(escapedQuery);

			// 执行搜索
			TopDocs topDocs = indexSearcher.search(luceneQuery, maxResults);

			// 转换为 ToolCallback
			List<ToolCallback> results = new ArrayList<>();
			for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
				Document doc = indexSearcher.doc(scoreDoc.doc);
				String toolName = doc.get("name");

				// 从缓存中获取 ToolCallback
				ToolCallback tool = toolCallbackMap.get(toolName);
				if (tool != null) {
					results.add(tool);
				}
			}

			log.debug("Search query '{}' found {} tools", query, results.size());
			return results;
		}
		catch (Exception e) {
			log.error("Failed to search tools for query: {}", query, e);
			return Collections.emptyList();
		}
	}

	@Override
	public String getToolSchema(ToolCallback tool) {
		try {
			ToolDefinition definition = tool.getToolDefinition();

			// 构建 JSON Schema
			Map<String, Object> schema = new HashMap<>();
			schema.put("type", "function");

			Map<String, Object> function = new HashMap<>();
			function.put("name", definition.name());
			function.put("description", definition.description());

			// 尝试解析参数 Schema
			String inputTypeSchema = definition.inputSchema();
			if (inputTypeSchema != null && !inputTypeSchema.isEmpty()) {
				try {
					Object parameters = objectMapper.readValue(inputTypeSchema, Object.class);
					function.put("parameters", parameters);
				}
				catch (Exception e) {
					log.warn("Failed to parse input schema for tool {}: {}", definition.name(), e.getMessage());
					function.put("parameters", Collections.emptyMap());
				}
			}
			else {
				function.put("parameters", Collections.emptyMap());
			}

			schema.put("function", function);

			return objectMapper.writeValueAsString(schema);
		}
		catch (Exception e) {
			log.error("Failed to generate schema for tool", e);
			return "{}";
		}
	}


	public static class Builder {

		private Directory indexDirectory;

		private Analyzer analyzer;

		private final Map<String, Float> fieldBoosts = new HashMap<>();

		private final List<String> indexFields = new ArrayList<>();

		public Builder() {
			indexFields.add("name");
			indexFields.add("description");
			indexFields.add("parameters");

			fieldBoosts.put("name", 3.0f);
			fieldBoosts.put("description", 2.0f);
			fieldBoosts.put("parameters", 1.0f);
		}


		public Builder indexDirectory(Directory indexDirectory) {
			this.indexDirectory = indexDirectory;
			return this;
		}


		public Builder analyzer(Analyzer analyzer) {
			this.analyzer = analyzer;
			return this;
		}


		public Builder fieldBoost(String fieldName, float boost) {
			this.fieldBoosts.put(fieldName, boost);
			return this;
		}


		public Builder fieldBoosts(Map<String, Float> boosts) {
			this.fieldBoosts.putAll(boosts);
			return this;
		}


		public Builder addIndexField(String fieldName) {
			if (!this.indexFields.contains(fieldName)) {
				this.indexFields.add(fieldName);
			}
			return this;
		}


		public Builder addIndexField(String fieldName, float boost) {
			addIndexField(fieldName);
			fieldBoost(fieldName, boost);
			return this;
		}


		public Builder clearIndexFields() {
			this.indexFields.clear();
			this.fieldBoosts.clear();
			return this;
		}

		public LuceneToolSearcher build() {
			if (indexFields.isEmpty()) {
				throw new IllegalStateException("At least one index field must be configured");
			}
			return new LuceneToolSearcher(this);
		}

	}

}
