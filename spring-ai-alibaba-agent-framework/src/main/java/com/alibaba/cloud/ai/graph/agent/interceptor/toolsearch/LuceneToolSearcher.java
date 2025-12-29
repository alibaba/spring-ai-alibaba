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

	private IndexSearcher indexSearcher;

	private final Map<String, ToolCallback> toolCallbackMap = new HashMap<>();

	public LuceneToolSearcher() {
		this.indexDirectory = new ByteBuffersDirectory();
		this.analyzer = new StandardAnalyzer();
		this.objectMapper = new ObjectMapper();
	}

	@Override
	public void indexTools(List<ToolCallback> tools) {
		try {
			IndexWriterConfig config = new IndexWriterConfig(analyzer);
			IndexWriter indexWriter = new IndexWriter(indexDirectory, config);

			for (ToolCallback tool : tools) {
				ToolDefinition definition = tool.getToolDefinition();
				Document doc = new Document();

				// 索引工具名称
				doc.add(new TextField("name", definition.name(), Field.Store.YES));

				// 索引工具描述
				String description = definition.description();
				if (description != null && !description.isEmpty()) {
					doc.add(new TextField("description", description, Field.Store.YES));
				}

				// 索引工具参数信息
				String inputSchema = definition.inputSchema();
				if (inputSchema != null && !inputSchema.isEmpty()) {
					doc.add(new TextField("parameters", inputSchema, Field.Store.YES));
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

			log.info("Successfully indexed {} tools", tools.size());
		}
		catch (IOException e) {
			throw new RuntimeException("Failed to index tools", e);
		}
	}

	@Override
	public List<ToolCallback> search(String query, int maxResults) {
		if (indexSearcher == null) {
			throw new IllegalStateException("Tools not indexed yet. Call indexTools() first.");
		}

		try {
			// 构建多字段查询
			Map<String, Float> boosts = new HashMap<>();
			boosts.put("name", 3.0f);
			boosts.put("description", 2.0f);
			boosts.put("parameters", 1.0f);

			MultiFieldQueryParser parser = new MultiFieldQueryParser(
					new String[] { "name", "description", "parameters" }, analyzer, boosts);

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

}

