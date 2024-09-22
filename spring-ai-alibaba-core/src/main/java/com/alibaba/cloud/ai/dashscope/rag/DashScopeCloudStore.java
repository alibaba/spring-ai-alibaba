/*
 * Copyright 2023-2024 the original author or authors.
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

package com.alibaba.cloud.ai.dashscope.rag;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.common.DashScopeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

/**
 * @author nuocheng.lxm
 * @since 2024/8/6 15:42
 */
public class DashScopeCloudStore implements VectorStore {

	private final DashScopeStoreOptions options;

	private final DashScopeApi dashScopeApi;

	public DashScopeCloudStore(DashScopeApi dashScopeApi, DashScopeStoreOptions options) {
		Assert.notNull(options, "DashScopeStoreOptions must not be null");
		Assert.notNull(options.getIndexName(), "IndexName must not be null");
		this.options = options;
		this.dashScopeApi = dashScopeApi;
	}

	/**
	 * @param documents the list of documents to store. Current document must be
	 * DashScopeDocumentReader's Result
	 *
	 */
	@Override
	public void add(List<Document> documents) {
		if (CollectionUtils.isEmpty(documents)) {
			throw new DashScopeException("documents must not be null");
		}
		List<String> documentIdList = documents.stream()
			.filter(e -> e.getId() != null)
			.map(Document::getId)
			.collect(Collectors.toList());
		if (documentIdList == null || documentIdList.isEmpty()) {
			throw new DashScopeException("document's id must not be null");
		}
		dashScopeApi.upsertPipeline(documents, options);
	}

	@Override
	public Optional<Boolean> delete(List<String> idList) {
		String pipelineId = dashScopeApi.getPipelineIdByName(options.getIndexName());
		if (pipelineId == null) {
			throw new DashScopeException("Index:" + options.getIndexName() + " NotExist");
		}
		return Optional.of(dashScopeApi.deletePipelineDocument(pipelineId, idList));
	}

	@Override
	public List<Document> similaritySearch(String query) {

		return similaritySearch(SearchRequest.query(query));

	}

	@Override
	public List<Document> similaritySearch(SearchRequest request) {
		String pipelineId = dashScopeApi.getPipelineIdByName(options.getIndexName());
		if (pipelineId == null) {
			throw new DashScopeException("Index:" + options.getIndexName() + " NotExist");
		}
		DashScopeDocumentRetrieverOptions searchOption = options.getRetrieverOptions();
		if (searchOption == null) {
			searchOption = new DashScopeDocumentRetrieverOptions();
		}
		searchOption.setRerankTopN(request.getTopK());
		return dashScopeApi.retriever(pipelineId, request.query, searchOption);
	}

}
