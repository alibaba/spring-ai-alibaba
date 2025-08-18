
package com.alibaba.cloud.ai.service.milvus;

import com.alibaba.cloud.ai.annotation.ConditionalOnMilvusEnabled;
import com.alibaba.cloud.ai.request.SearchRequest;
import com.alibaba.cloud.ai.service.base.BaseVectorStoreService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.milvus.MilvusVectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
@ConditionalOnMilvusEnabled
public class MilvusVectorStoreService extends BaseVectorStoreService {

	private static final Logger log = LoggerFactory.getLogger(MilvusVectorStoreService.class);

	@Autowired
	private EmbeddingModel embeddingModel;

	@Autowired
	private MilvusVectorStore milvusVectorStore;

	@Override
	protected EmbeddingModel getEmbeddingModel() {
		return embeddingModel;
	}

	@Override
	public List<Document> searchWithVectorType(SearchRequest searchRequestDTO) {
		String filter = String.format("vectorType == '%s'", searchRequestDTO.getVectorType());
		return executeQuery(searchRequestDTO, filter);
	}

	@Override
	public List<Document> searchWithFilter(SearchRequest searchRequestDTO) {
		return executeQuery(searchRequestDTO, searchRequestDTO.getFilterFormatted());
	}

	private List<Document> executeQuery(SearchRequest searchRequestDTO, String filter) {
		if (searchRequestDTO.getQuery() == null || searchRequestDTO.getQuery().isEmpty()) {
			return Collections.emptyList();
		}

		var springAiSearchRequest = org.springframework.ai.vectorstore.SearchRequest.builder()
			.query(searchRequestDTO.getQuery())
			.filterExpression(filter)
			.topK(searchRequestDTO.getTopK())
			.build();

		List<Document> documents = milvusVectorStore.doSimilaritySearch(springAiSearchRequest);
		log.debug("Search completed. Found {} documents for SearchRequest: {}", documents.size(),
				searchRequestDTO.toString());
		return documents;
	}

}
