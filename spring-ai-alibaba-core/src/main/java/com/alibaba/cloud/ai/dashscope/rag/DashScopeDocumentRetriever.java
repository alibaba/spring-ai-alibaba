package com.alibaba.cloud.ai.dashscope.rag;

import java.util.List;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.common.DashScopeException;

import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentRetriever;
import org.springframework.util.Assert;

/**
 * @author nuocheng.lxm
 * @since 2024/8/5 14:42
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
