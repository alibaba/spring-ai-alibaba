package com.alibaba.cloud.ai.graph.node.rag;

import com.alibaba.cloud.ai.graph.GraphRunnerException;
import com.alibaba.cloud.ai.graph.NodeActionDescriptor;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.cloud.ai.graph.node.AbstractNode;
import com.alibaba.cloud.ai.graph.state.NodeState;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * DocumentRetrieverNodeAction is responsible for retrieving documents based on a query.
 *
 * @author Dolphin
 * @version V1.0
 * @date 2024/12/3 15:51
 */
public class DocumentRetrieverNodeAction extends AbstractNode implements NodeAction {

	public static final String QUERY_KEY = "query";

	public static final String RETRIEVED_DOCUMENTS_KEY = "retrievedDocuments";

	private final VectorStoreDocumentRetriever documentRetriever;

	private NodeActionDescriptor nodeActionDescriptor;

	public DocumentRetrieverNodeAction(VectorStore vectorStore, Double similarityThreshold, Integer topK,
			Supplier<Filter.Expression> filterExpression, NodeActionDescriptor nodeActionDescriptor) {
		this.documentRetriever = new VectorStoreDocumentRetriever(vectorStore, similarityThreshold, topK,
				filterExpression);
		this.nodeActionDescriptor = nodeActionDescriptor;
	}

	@Override
	public Map<String, Object> apply(OverAllState state) throws Exception {
		Optional<Object> queryOptional = state.value(QUERY_KEY);
		if (!queryOptional.isPresent()) {
			throw new GraphRunnerException("Query cannot be null");
		}

		String queryStr = queryOptional.map(Object::toString).orElse(null);
		Query query = new Query(queryStr);

		List<Document> retrievedDocuments = documentRetriever.retrieve(query);

		return Map.of(RETRIEVED_DOCUMENTS_KEY, retrievedDocuments);
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {

		private VectorStore vectorStore;

		private Double similarityThreshold;

		private Integer topK;

		private Supplier<Filter.Expression> filterExpression;

		private DocumentRetrieverNodeActionDescriptor nodeActionDescriptor;

		public Builder() {
			this.nodeActionDescriptor = new DocumentRetrieverNodeActionDescriptor();
		}

		public Builder withVectorStore(VectorStore vectorStore) {
			this.vectorStore = vectorStore;
			return this;
		}

		public Builder withSimilarityThreshold(Double similarityThreshold) {
			this.similarityThreshold = similarityThreshold;
			this.nodeActionDescriptor.setSimilarityThreshold(similarityThreshold);
			return this;
		}

		public Builder withTopK(Integer topK) {
			this.topK = topK;
			this.nodeActionDescriptor.setTopK(topK);
			return this;
		}

		public Builder withFilterExpression(Supplier<Filter.Expression> filterExpression) {
			this.filterExpression = filterExpression;
			this.nodeActionDescriptor.setFilterExpression(filterExpression);
			return this;
		}

		public DocumentRetrieverNodeAction build() {
			return new DocumentRetrieverNodeAction(vectorStore, similarityThreshold, topK, filterExpression,
					nodeActionDescriptor);
		}

	}

}
