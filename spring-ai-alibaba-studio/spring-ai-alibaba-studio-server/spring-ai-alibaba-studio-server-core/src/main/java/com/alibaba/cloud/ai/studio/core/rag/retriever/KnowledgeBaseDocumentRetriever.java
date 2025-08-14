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

package com.alibaba.cloud.ai.studio.core.rag.retriever;

import com.alibaba.cloud.ai.studio.core.model.reranker.dashscope.DashscopeReranker;
import com.alibaba.cloud.ai.studio.runtime.exception.BizException;
import com.alibaba.cloud.ai.studio.runtime.enums.ErrorCode;
import com.alibaba.cloud.ai.studio.runtime.domain.app.FileSearchOptions;
import com.alibaba.cloud.ai.studio.runtime.domain.knowledgebase.KnowledgeBase;
import com.alibaba.cloud.ai.studio.core.model.llm.ModelFactory;
import com.alibaba.cloud.ai.studio.core.rag.vectorstore.VectorStoreFactory;
import com.alibaba.cloud.ai.studio.core.utils.LogUtils;
import com.alibaba.cloud.ai.studio.core.utils.concurrent.ThreadPoolUtils;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.SearchType;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static com.alibaba.cloud.ai.studio.core.rag.RagConstants.*;
import static com.alibaba.cloud.ai.studio.core.utils.LogUtils.FAIL;
import static com.alibaba.cloud.ai.studio.core.utils.LogUtils.SUCCESS;

/**
 * A document retriever that searches across multiple vector stores. It supports parallel
 * retrieval from different knowledge bases and optional document reranking.
 *
 * @since 1.0.0.3
 */

@RequiredArgsConstructor
public class KnowledgeBaseDocumentRetriever implements DocumentRetriever {

	/** List of knowledge bases to search from */
	private final List<KnowledgeBase> knowledgeBases;

	/** Factory for creating vector store instances */
	private final VectorStoreFactory vectorStoreFactory;

	/** Factory for creating model instances */
	private final ModelFactory modelFactory;

	/** Configuration options for document search */
	private final FileSearchOptions searchOptions;

	/**
	 * Retrieves relevant documents from all knowledge bases based on the query. Documents
	 * are retrieved in parallel and then merged, sorted, and filtered.
	 * @param query The search query containing text and context
	 * @return List of relevant documents sorted by score
	 */
	@NotNull
	@Override
	public List<Document> retrieve(@NotNull Query query) {
		Assert.notNull(query, "query cannot be null");
		Assert.notNull(query.context(), "query context can not be null");

		long start = System.currentTimeMillis();
		List<CompletableFuture<List<Document>>> futureList = new ArrayList<>();
		for (KnowledgeBase knowledgeBase : knowledgeBases) {
			CompletableFuture<List<Document>> textFuture = CompletableFuture
				.supplyAsync(() -> retrieve(knowledgeBase, query), ThreadPoolUtils.DEFAULT_TASK_EXECUTOR);
			futureList.add(textFuture);
		}

		try {
			List<Document> documents = new ArrayList<>();
			for (CompletableFuture<List<Document>> future : futureList) {
				documents.addAll(future.get(SEARCH_TIMEOUT, TimeUnit.SECONDS));
			}

			List<Document> results = documents.stream()
				.sorted(Comparator.comparing(Document::getScore, Comparator.nullsLast(Comparator.reverseOrder())))
				.filter(x -> x.getScore() != null && x.getScore() > searchOptions.getSimilarityThreshold())
				.limit(searchOptions.getTopK())
				.toList();

			LogUtils.monitor("DocumentRetriever", "retrieve", start, SUCCESS, query.text(), results.size());
			return results;
		}
		catch (BizException e) {
			LogUtils.monitor("DocumentRetriever", "retrieve", start, FAIL, query.text(), null);
			throw e;
		}
		catch (InterruptedException | ExecutionException e) {
			LogUtils.monitor("DocumentRetriever", "retrieve", start, FAIL, query.text(), null);
			throw new BizException(ErrorCode.DOCUMENT_RETRIEVAL_ERROR.toError(), e);
		}
		catch (TimeoutException e) {
			LogUtils.monitor("DocumentRetriever", "retrieve", start, FAIL, query.text(), null);
			throw new BizException(ErrorCode.DOCUMENT_RETRIEVAL_TIMEOUT.toError(), e);
		}
	}

	/**
	 * Retrieves documents from a single knowledge base using vector similarity search.
	 * Optionally applies reranking if enabled in search options.
	 * @param knowledgeBase The knowledge base to search in
	 * @param query The search query
	 * @return List of retrieved documents
	 */
	private List<Document> retrieve(KnowledgeBase knowledgeBase, Query query) {
		VectorStore vectorStore = vectorStoreFactory.getVectorStoreService()
			.getVectorStore(knowledgeBase.getIndexConfig());
		var b = new FilterExpressionBuilder();
		var exp = b.and(b.eq(KEY_WORKSPACE_ID, knowledgeBase.getWorkspaceId()), b.eq(KEY_ENABLED, true)).build();

		FileSearchOptions searchOptions = knowledgeBase.getSearchConfig();
		SearchType searchType = SearchType.valueOf(searchOptions.getSearchType().toUpperCase());

		SearchRequest.Builder searchRequestBuilder = SearchRequest.builder()
			.query(query.text())
			.filterExpression(exp)
			.searchType(searchType);
		if (searchOptions.getSimilarityThreshold() != null) {
			searchRequestBuilder.similarityThreshold(searchOptions.getSimilarityThreshold());
		}
		if (searchOptions.getTopK() != null) {
			searchRequestBuilder.topK(searchOptions.getTopK());
		}
		if (searchOptions.getHybridWeight() != null) {
			searchRequestBuilder.hybridWeight(searchOptions.getHybridWeight());
		}

		List<Document> documents = vectorStore.similaritySearch(searchRequestBuilder.build());
		if (searchOptions.getEnableRerank()) {
			documents = rerankDocuments(searchOptions, query, documents);
		}

		return documents;
	}

	/**
	 * Reranks the retrieved documents using a document ranker model.
	 * @param searchOptions Search configuration options
	 * @param query The original search query
	 * @param documents List of documents to rerank
	 * @return Reranked list of documents
	 */
	private List<Document> rerankDocuments(FileSearchOptions searchOptions, Query query, List<Document> documents) {
		long start = System.currentTimeMillis();
		DashscopeReranker documentRanker = modelFactory.getDocumentRanker(searchOptions);
		List<Document> results = documentRanker.process(query, documents);

		LogUtils.monitor("DocumentRetriever", "rerank", start, SUCCESS, query.text(), results.size());
		return results;
	}

}
