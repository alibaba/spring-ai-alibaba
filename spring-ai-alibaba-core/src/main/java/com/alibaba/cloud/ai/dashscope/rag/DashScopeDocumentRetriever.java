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

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.common.DashScopeException;

import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentRetriever;
import org.springframework.util.Assert;

/**
 * @author nuocheng.lxm
 * @date 2024/8/5 14:42
 */
public class DashScopeDocumentRetriever implements DocumentRetriever {

	private final DashScopeDocumentRetrieverOptions options;

	private final DashScopeApi dashScopeApi;

	public DashScopeDocumentRetriever(DashScopeApi dashScopeApi, DashScopeDocumentRetrieverOptions options) {
		Assert.notNull(options, "RetrieverOptions must not be null");
		Assert.notNull(options.getIndexName(), "IndexName must not be null");
		this.options = options;
		this.dashScopeApi = dashScopeApi;
	}

	@Override
	public List<Document> retrieve(String query) {
		String pipelineId = dashScopeApi.getPipelineIdByName(options.getIndexName());
		if (pipelineId == null) {
			throw new DashScopeException("Index:" + options.getIndexName() + " NotExist");
		}
		List<Document> documentList = dashScopeApi.retriever(pipelineId, query, options);
		return documentList;
	}

}
