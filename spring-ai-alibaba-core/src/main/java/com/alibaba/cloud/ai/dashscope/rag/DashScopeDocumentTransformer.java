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
package com.alibaba.cloud.ai.dashscope.rag;

import java.util.ArrayList;
import java.util.List;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.common.DashScopeException;
import com.alibaba.cloud.ai.dashscope.common.ErrorCodeEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentTransformer;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

/**
 * @author nuocheng.lxm
 * @since 2024/8/6 16:19
 */
public class DashScopeDocumentTransformer implements DocumentTransformer {

	private static final Logger logger = LoggerFactory.getLogger(DashScopeDocumentTransformer.class);

	private final DashScopeApi dashScopeApi;

	private final DashScopeDocumentTransformerOptions options;

	public DashScopeDocumentTransformer(DashScopeApi dashScopeApi) {
		this(dashScopeApi, DashScopeDocumentTransformerOptions.builder().build());
	}

	public DashScopeDocumentTransformer(DashScopeApi dashScopeApi, DashScopeDocumentTransformerOptions options) {
		Assert.notNull(dashScopeApi, "DashScopeApi must not be null");
		Assert.notNull(options, "DashScopeDocumentTransformerOptions must not be null");
		this.dashScopeApi = dashScopeApi;
		this.options = options;
	}

	private List<Document> doSplitDocuments(List<Document> documents) {
		validateDocuments(documents);
		Document document = documents.get(0);

		ResponseEntity<DashScopeApi.DocumentSplitResponse> splitResponseEntity = dashScopeApi.documentSplit(document,
				options);
		validateSplitResponse(splitResponseEntity);

		DashScopeApi.DocumentSplitResponse splitResponse = splitResponseEntity.getBody();
		validateChunkResult(splitResponse);

		List<DashScopeApi.DocumentSplitResponse.DocumentChunk> chunkList = splitResponse.chunkService().chunkResult();
		List<Document> documentList = new ArrayList<>();

		chunkList.forEach(e -> {
			Document chunk = new Document(document.getId() + "_" + e.chunkId(), e.content(), document.getMetadata());
			documentList.add(chunk);
		});

		return documentList;
	}

	private void validateDocuments(List<Document> documents) {
		if (CollectionUtils.isEmpty(documents)) {
			throw new RuntimeException("Documents must not be null");
		}
		if (documents.size() > 1) {
			throw new RuntimeException("Just support one Document");
		}
	}

	private void validateSplitResponse(ResponseEntity<DashScopeApi.DocumentSplitResponse> splitResponseEntity) {
		if (splitResponseEntity == null) {
			throw new DashScopeException(ErrorCodeEnum.SPLIT_DOCUMENT_ERROR);
		}
	}

	private void validateChunkResult(DashScopeApi.DocumentSplitResponse splitResponse) {
		if (splitResponse == null || splitResponse.chunkService() == null
				|| CollectionUtils.isEmpty(splitResponse.chunkService().chunkResult())) {
			logger.error("DashScopeDocumentTransformer NoSplitResult");
			throw new DashScopeException(ErrorCodeEnum.SPLIT_DOCUMENT_ERROR);
		}
	}

	@Override
	public List<Document> apply(List<Document> documents) {
		return this.doSplitDocuments(documents);
	}

}
