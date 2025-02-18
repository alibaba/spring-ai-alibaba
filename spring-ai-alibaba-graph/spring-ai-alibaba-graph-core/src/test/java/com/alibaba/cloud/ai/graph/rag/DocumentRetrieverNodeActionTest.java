package com.alibaba.cloud.ai.graph.rag;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.rag.DashScopeCloudStore;
import com.alibaba.cloud.ai.dashscope.rag.DashScopeDocumentCloudReader;
import com.alibaba.cloud.ai.dashscope.rag.DashScopeStoreOptions;
import com.alibaba.cloud.ai.graph.GraphRunnerException;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.node.rag.DocumentRetrieverNodeAction;
import com.alibaba.cloud.ai.graph.state.NodeState;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.internal.verification.Times;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.util.Assert;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.ai.vectorstore.filter.Filter.ExpressionType.EQ;

class DocumentRetrieverNodeActionTest {


	@Test
	void testApplyWithValidQuery() throws Exception {

		OverAllState overAllState = new OverAllState().inputs(Map.of(DocumentRetrieverNodeAction.QUERY_KEY, "test query"))
				.addKeyAndStrategy(DocumentRetrieverNodeAction.QUERY_KEY, null);

		var mockVectorStore = mock(VectorStore.class);
		DocumentRetrieverNodeAction nodeAction = DocumentRetrieverNodeAction.builder()
			.withVectorStore(mockVectorStore)
			.withSimilarityThreshold(0.7)
			.withTopK(3)
			.build();
		Map<String, Object> result = nodeAction.apply(overAllState);
		assertEquals(1, result.size());
		for (String outputKey : result.keySet()) {
			assertEquals("retrievedDocuments", outputKey);
		}
		System.out.println(result);
	}

	@Test
	void testApplyWithDashScope() throws Exception {
		DashScopeApi dashScopeApi = new DashScopeApi("sk-ee4f3c63b69348958a824cc7aecefdd6");
		String filePath = "C:\\Users\\dolphin\\AppData\\Local\\Temp\\06- Spring AI 1.0.0 M5 升级.pdf";
		DashScopeDocumentCloudReader cloudReader = new DashScopeDocumentCloudReader(filePath, dashScopeApi, null);
		List<Document> documentList = cloudReader.get();

		DashScopeCloudStore dashScopeCloudStore = new DashScopeCloudStore(dashScopeApi,
				new DashScopeStoreOptions("test index"));
		dashScopeCloudStore.add(documentList);

		OverAllState overAllState = new OverAllState().inputs(Map.of(DocumentRetrieverNodeAction.QUERY_KEY, "M5"))
				.addKeyAndStrategy(DocumentRetrieverNodeAction.QUERY_KEY, null);
		DocumentRetrieverNodeAction nodeAction = DocumentRetrieverNodeAction.builder()
			.withVectorStore(dashScopeCloudStore)
			.withSimilarityThreshold(0.7)
			.withTopK(3)
			.build();
		Map<String, Object> result = nodeAction.apply(overAllState);
		for (String outputKey : result.keySet()) {
			assertEquals("retrievedDocuments", outputKey);
		}
		System.out.println(result);
	}

	@Test
	void testApplyWithInvalidQuery() {
		OverAllState overAllState = new OverAllState();
		var mockVectorStore = mock(VectorStore.class);

		DocumentRetrieverNodeAction nodeAction = DocumentRetrieverNodeAction.builder()
			.withVectorStore(mockVectorStore)
			.withSimilarityThreshold(0.7)
			.withTopK(3)
			.build();

		Exception exception = assertThrows(GraphRunnerException.class, () -> nodeAction.apply(overAllState));
		assertThat(exception.getMessage()).isEqualTo("Query cannot be null");
	}

	@Test
	void testBuilderWithSimilarityThreshold() {
		assertThatThrownBy(() -> DocumentRetrieverNodeAction.builder()
			.withSimilarityThreshold(-0.1)
			.withVectorStore(mock(VectorStore.class))
			.build()).isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("similarityThreshold must be equal to or greater than 0.0");
	}

	@Test
	void testBuilderWithFilterExpressions() throws Exception {
		var mockVectorStore = mock(VectorStore.class);
		var nodeAction = DocumentRetrieverNodeAction.builder()
			.withVectorStore(mockVectorStore)
			.withFilterExpression(
					() -> new FilterExpressionBuilder().eq("tenantId", TenantContextHolder.getTenantIdentifier())
						.build())
			.build();

		OverAllState overAllState = new OverAllState().inputs(Map.of(DocumentRetrieverNodeAction.QUERY_KEY, "query"))
				.addKeyAndStrategy(DocumentRetrieverNodeAction.QUERY_KEY, null);

		TenantContextHolder.setTenantIdentifier("tenant1");
		nodeAction.apply(overAllState);
		TenantContextHolder.clear();

		TenantContextHolder.setTenantIdentifier("tenant2");
		nodeAction.apply(overAllState);
		TenantContextHolder.clear();

		var searchRequestCaptor = ArgumentCaptor.forClass(SearchRequest.class);

		verify(mockVectorStore, new Times(2)).similaritySearch(searchRequestCaptor.capture());

		var searchRequest1 = searchRequestCaptor.getAllValues().get(0);
		assertThat(searchRequest1.getFilterExpression())
			.isEqualTo(new Filter.Expression(EQ, new Filter.Key("tenantId"), new Filter.Value("tenant1")));

		var searchRequest2 = searchRequestCaptor.getAllValues().get(1);
		assertThat(searchRequest2.getFilterExpression())
			.isEqualTo(new Filter.Expression(EQ, new Filter.Key("tenantId"), new Filter.Value("tenant2")));
	}

	static final class TenantContextHolder {

		private static final ThreadLocal<String> TENANT_IDENTIFIER = new ThreadLocal<>();

		private TenantContextHolder() {
		}

		public static void setTenantIdentifier(String tenant) {
			Assert.hasText(tenant, "tenant cannot be null or empty");
			TENANT_IDENTIFIER.set(tenant);
		}

		public static String getTenantIdentifier() {
			return TENANT_IDENTIFIER.get();
		}

		public static void clear() {
			TENANT_IDENTIFIER.remove();
		}

	}

}