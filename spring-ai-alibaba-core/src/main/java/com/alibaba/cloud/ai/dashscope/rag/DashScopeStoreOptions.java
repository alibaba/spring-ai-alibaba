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

import com.alibaba.cloud.ai.dashscope.embedding.DashScopeEmbeddingOptions;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author nuocheng.lxm
 * @since 2024/8/9 10:00
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DashScopeStoreOptions {

	private @JsonProperty("index_name") String indexName;

	/**
	 * 文档索引切分相关配置
	 */
	private @JsonProperty("transformer_options") DashScopeDocumentTransformerOptions transformerOptions;

	/**
	 * 文档索引向量化相关配置
	 */
	private @JsonProperty("embedding_options") DashScopeEmbeddingOptions embeddingOptions;

	/**
	 * 文档检索相关配置
	 */
	private @JsonProperty("retriever_options") DashScopeDocumentRetrieverOptions retrieverOptions;

	public DashScopeStoreOptions(String indexName) {
		this.indexName = indexName;
	}

	public String getIndexName() {
		return indexName;
	}

	public void setIndexName(String indexName) {
		this.indexName = indexName;
	}

	public DashScopeDocumentTransformerOptions getTransformerOptions() {
		return transformerOptions;
	}

	public void setTransformerOptions(DashScopeDocumentTransformerOptions transformerOptions) {
		this.transformerOptions = transformerOptions;
	}

	public DashScopeEmbeddingOptions getEmbeddingOptions() {
		return embeddingOptions;
	}

	public void setEmbeddingOptions(DashScopeEmbeddingOptions embeddingOptions) {
		this.embeddingOptions = embeddingOptions;
	}

	public DashScopeDocumentRetrieverOptions getRetrieverOptions() {
		return retrieverOptions;
	}

	public void setRetrieverOptions(DashScopeDocumentRetrieverOptions retrieverOptions) {
		this.retrieverOptions = retrieverOptions;
	}

}
