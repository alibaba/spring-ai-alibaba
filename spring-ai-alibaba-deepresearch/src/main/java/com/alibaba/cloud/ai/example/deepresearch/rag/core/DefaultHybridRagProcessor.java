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

package com.alibaba.cloud.ai.example.deepresearch.rag.core;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.TermQuery;
import com.alibaba.cloud.ai.example.deepresearch.config.rag.RagProperties;
import com.alibaba.cloud.ai.example.deepresearch.rag.post.DocumentSelectFirstProcess;
import com.alibaba.cloud.ai.example.deepresearch.rag.retriever.RrfHybridElasticsearchRetriever;
import com.alibaba.cloud.ai.example.deepresearch.rag.strategy.RrfFusionStrategy;
import org.elasticsearch.client.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.rag.preretrieval.query.expansion.MultiQueryExpander;
import org.springframework.ai.rag.preretrieval.query.transformation.TranslationQueryTransformer;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 统一的RAG处理器实现，整合RagAdvisorConfiguration中的前后处理逻辑 和RrfHybridElasticsearchRetriever的混合查询能力
 *
 * @author hupei
 */
@Component
@ConditionalOnProperty(prefix = "spring.ai.alibaba.deepresearch.rag", name = "enabled", havingValue = "true")
public class DefaultHybridRagProcessor implements HybridRagProcessor {

	private static final Logger logger = LoggerFactory.getLogger(DefaultHybridRagProcessor.class);

	private final VectorStore vectorStore;

	private final RrfHybridElasticsearchRetriever hybridRetriever;

	private final MultiQueryExpander queryExpander;

	private final TranslationQueryTransformer queryTransformer;

	private final DocumentSelectFirstProcess documentPostProcessor;

	private final RrfFusionStrategy rrfFusionStrategy;

	private final RagProperties ragProperties;

	public DefaultHybridRagProcessor(@Qualifier("ragVectorStore") VectorStore vectorStore, RestClient restClient,
			EmbeddingModel embeddingModel, ChatClient.Builder chatClientBuilder, RagProperties ragProperties,
			RrfFusionStrategy rrfFusionStrategy) {
		this.vectorStore = vectorStore;
		this.ragProperties = ragProperties;
		this.rrfFusionStrategy = rrfFusionStrategy;

		// 初始化混合检索器
		if (ragProperties.getVectorStoreType().equalsIgnoreCase("elasticsearch")
				&& ragProperties.getElasticsearch().getHybrid().isEnabled()) {
			this.hybridRetriever = new RrfHybridElasticsearchRetriever(restClient, embeddingModel,
					ragProperties.getElasticsearch().getIndexName(), ragProperties.getElasticsearch().getHybrid());
		}
		else {
			this.hybridRetriever = null;
		}

		// 初始化查询处理器
		this.queryExpander = ragProperties.getPipeline().isQueryExpansionEnabled()
				? MultiQueryExpander.builder().chatClientBuilder(chatClientBuilder).build() : null;

		this.queryTransformer = ragProperties.getPipeline().isQueryTranslationEnabled()
				? TranslationQueryTransformer.builder()
					.chatClientBuilder(chatClientBuilder)
					.targetLanguage(ragProperties.getPipeline().getQueryTranslationLanguage())
					.build()
				: null;

		// 初始化文档后处理器
		this.documentPostProcessor = ragProperties.getPipeline().isPostProcessingSelectFirstEnabled()
				? new DocumentSelectFirstProcess() : null;
	}

	@Override
	public List<Document> process(org.springframework.ai.rag.Query query, Map<String, Object> options) {
		logger.debug("Starting RAG processing for query: {}", query.text());

		// 1. 查询前处理
		List<org.springframework.ai.rag.Query> processedQueries = preProcess(query, options);

		// 2. 构建过滤表达式
		Query filterExpression = buildFilterExpression(options);

		// 3. 执行混合检索
		List<Document> documents = hybridRetrieve(processedQueries, filterExpression, options);

		// 4. 文档后处理
		List<Document> finalDocuments = postProcess(documents, options);

		logger.debug("RAG processing completed. Retrieved {} documents", finalDocuments.size());
		return finalDocuments;
	}

	@Override
	public List<org.springframework.ai.rag.Query> preProcess(org.springframework.ai.rag.Query query,
			Map<String, Object> options) {
		List<org.springframework.ai.rag.Query> queries = new ArrayList<>();
		queries.add(query);

		// 查询翻译
		if (queryTransformer != null) {
			queries = queries.stream().flatMap(q -> {
				org.springframework.ai.rag.Query transformed = queryTransformer.transform(q);
				return transformed != null ? Stream.of(transformed) : Stream.empty();
			}).collect(Collectors.toList());
		}

		// 查询扩展
		if (queryExpander != null) {
			queries = queries.stream().flatMap(q -> queryExpander.expand(q).stream()).collect(Collectors.toList());
		}

		return queries;
	}

	@Override
	public List<Document> hybridRetrieve(List<org.springframework.ai.rag.Query> queries, Query filterExpression,
			Map<String, Object> options) {
		List<Document> allDocuments = new ArrayList<>();

		for (org.springframework.ai.rag.Query query : queries) {
			// 如果配置了ES混合查询且可用，使用混合检索器
			if (hybridRetriever != null) {
				List<Document> hybridResults = hybridRetriever.retrieve(query, filterExpression);
				allDocuments.addAll(hybridResults);
			}
			else {
				// 否则使用标准向量搜索
				List<Document> vectorResults = performVectorSearch(query, options);
				allDocuments.addAll(vectorResults);
			}
		}

		// 去重（基于文档ID或内容）
		return deduplicateDocuments(allDocuments);
	}

	@Override
	public List<Document> postProcess(List<Document> documents, Map<String, Object> options) {
		// 优先使用RRF rerank，如果启用的话
		if (ragProperties.getPipeline().isRerankEnabled()) {
			org.springframework.ai.rag.Query query = new org.springframework.ai.rag.Query(
					options.getOrDefault("query", "").toString());
			return rrfFusionStrategy.process(query, documents);
		}

		// 否则使用传统的后处理器
		if (documentPostProcessor != null) {
			return documentPostProcessor.process(null, documents);
		}

		return documents;
	}

	@Override
	public Query buildFilterExpression(Map<String, Object> options) {
		if (options == null || options.isEmpty()) {
			return null;
		}

		BoolQuery.Builder boolQueryBuilder = new BoolQuery.Builder();
		boolean hasConditions = false;

		// 按照VectorStoreDataIngestionService中的元数据逻辑构建过滤条件
		if (options.containsKey("source_type")) {
			boolQueryBuilder
				.must(TermQuery.of(t -> t.field("metadata.source_type").value(options.get("source_type").toString()))
					._toQuery());
			hasConditions = true;
		}

		if (options.containsKey("session_id")) {
			boolQueryBuilder
				.must(TermQuery.of(t -> t.field("metadata.session_id").value(options.get("session_id").toString()))
					._toQuery());
			hasConditions = true;
		}

		if (options.containsKey("user_id")) {
			boolQueryBuilder.must(
					TermQuery.of(t -> t.field("metadata.user_id").value(options.get("user_id").toString()))._toQuery());
			hasConditions = true;
		}

		return hasConditions ? boolQueryBuilder.build()._toQuery() : null;
	}

	private List<Document> performVectorSearch(org.springframework.ai.rag.Query query, Map<String, Object> options) {
		var filterBuilder = new FilterExpressionBuilder();
		var searchRequestBuilder = SearchRequest.builder().query(query.text());

		// 构建向量搜索的过滤表达式
		if (options.containsKey("source_type")) {
			var filterExpression = filterBuilder.eq("source_type", options.get("source_type").toString());

			if (options.containsKey("session_id")) {
				filterExpression = filterBuilder.and(filterExpression,
						filterBuilder.eq("session_id", options.get("session_id").toString()));
			}

			if (options.containsKey("user_id")) {
				filterExpression = filterBuilder.and(filterExpression,
						filterBuilder.eq("user_id", options.get("user_id").toString()));
			}

			searchRequestBuilder.filterExpression(filterExpression.build());
		}

		SearchRequest searchRequest = searchRequestBuilder.topK(ragProperties.getPipeline().getTopK())
			.similarityThreshold(ragProperties.getPipeline().getSimilarityThreshold())
			.build();

		return vectorStore.similaritySearch(searchRequest);
	}

	private List<Document> deduplicateDocuments(List<Document> documents) {
		if (!ragProperties.getPipeline().isDeduplicationEnabled()) {
			return documents;
		}

		Map<String, Document> uniqueDocuments = new LinkedHashMap<>();

		for (Document doc : documents) {
			String key = doc.getId() != null ? doc.getId() : doc.getText();
			if (!uniqueDocuments.containsKey(key)) {
				uniqueDocuments.put(key, doc);
			}
		}

		return new ArrayList<>(uniqueDocuments.values());
	}

}
