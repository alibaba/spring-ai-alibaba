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
