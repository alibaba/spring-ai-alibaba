package com.alibaba.cloud.ai.service;

import com.alibaba.cloud.ai.connector.accessor.Accessor;
import com.alibaba.cloud.ai.connector.bo.ColumnInfoBO;
import com.alibaba.cloud.ai.connector.bo.TableInfoBO;
import com.alibaba.cloud.ai.connector.config.DbConfig;
import com.alibaba.cloud.ai.request.DeleteRequest;
import com.alibaba.cloud.ai.request.EvidenceRequest;
import com.alibaba.cloud.ai.request.SchemaInitRequest;
import com.alibaba.cloud.ai.request.SearchRequest;
import com.google.gson.Gson;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.milvus.MilvusVectorStore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MilvusVectorStoreManagementServiceTest {

	@Mock
	private EmbeddingModel embeddingModel;

	@Mock
	private MilvusVectorStore milvusVectorStore;

	@Mock
	private Accessor dbAccessor;

	@Mock
	private Gson gson;

	@InjectMocks
	private MilvusVectorStoreManagementService milvusVectorStoreManagementService;

	@BeforeEach
	void setUp() {
		// This setup is handled by @InjectMocks and @Mock annotations
	}

	@Test
	void addEvidence() {
		EvidenceRequest request = new EvidenceRequest();
		request.setContent("test content");
		request.setType(0);
		List<EvidenceRequest> requests = Collections.singletonList(request);

		Boolean result = milvusVectorStoreManagementService.addEvidence(requests);

		assertTrue(result);
		ArgumentCaptor<List<Document>> captor = ArgumentCaptor.forClass(List.class);
		verify(milvusVectorStore).add(captor.capture());
		List<Document> capturedDocs = captor.getValue();
		assertEquals(1, capturedDocs.size());
		assertEquals("test content", capturedDocs.get(0).getText());
		assertEquals(0, capturedDocs.get(0).getMetadata().get("evidenceType"));
		assertEquals("evidence", capturedDocs.get(0).getMetadata().get("vectorType"));
	}

	@Test
	void embed() {
		String text = "sample text";
		float[] embedding = { 0.1f, 0.2f, 0.3f };
		when(embeddingModel.embed(text)).thenReturn(embedding);

		List<Double> result = milvusVectorStoreManagementService.embed(text);

		assertNotNull(result);
		assertEquals(3, result.size());

		// 定义一个非常小的误差范围
		double delta = 0.00001;
		assertEquals(0.1, result.get(0), delta);
		assertEquals(0.2, result.get(1), delta);
		assertEquals(0.3, result.get(2), delta);
	}

	@Test
	void search() {
		SearchRequest searchRequest = new SearchRequest();
		searchRequest.setQuery("test query");
		searchRequest.setTopK(5);
		searchRequest.setVectorType("evidence");

		List<Document> expectedDocs = Collections.singletonList(new Document("result"));
		when(milvusVectorStore.similaritySearch(any(org.springframework.ai.vectorstore.SearchRequest.class)))
			.thenReturn(expectedDocs);

		List<Document> result = milvusVectorStoreManagementService.search(searchRequest);

		assertEquals(expectedDocs, result);
		ArgumentCaptor<org.springframework.ai.vectorstore.SearchRequest> captor = ArgumentCaptor
			.forClass(org.springframework.ai.vectorstore.SearchRequest.class);
		verify(milvusVectorStore).similaritySearch(captor.capture());
		org.springframework.ai.vectorstore.SearchRequest capturedRequest = captor.getValue();
		assertEquals("test query", capturedRequest.getQuery());
		assertEquals(5, capturedRequest.getTopK());
		assertNotNull(capturedRequest.getFilterExpression());
	}

	@Test
	void deleteDocumentsById() throws Exception {
		DeleteRequest deleteRequest = new DeleteRequest();
		deleteRequest.setId("doc123");

		Boolean result = milvusVectorStoreManagementService.deleteDocuments(deleteRequest);

		assertTrue(result);
		verify(milvusVectorStore).delete(Collections.singletonList("doc123"));
	}

	@Test
	void deleteDocumentsByVectorType() throws Exception {
		DeleteRequest deleteRequest = new DeleteRequest();
		deleteRequest.setVectorType("column");

		Boolean result = milvusVectorStoreManagementService.deleteDocuments(deleteRequest);

		assertTrue(result);
		ArgumentCaptor<Filter.Expression> captor = ArgumentCaptor.forClass(Filter.Expression.class);
		verify(milvusVectorStore).delete(captor.capture());
		// Cannot easily inspect the content of the expression, but we can verify the call
		// was made
		assertNotNull(captor.getValue());
	}

	@Test
	void deleteDocuments_shouldThrowException_whenNoIdOrVectorType() {
		DeleteRequest deleteRequest = new DeleteRequest();
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> milvusVectorStoreManagementService.deleteDocuments(deleteRequest));
		assertEquals("Either id or vectorType must be specified.", exception.getMessage());
	}

	@Test
	void deleteDocuments_shouldThrowException_onFailure() {
		DeleteRequest deleteRequest = new DeleteRequest();
		deleteRequest.setId("doc123");
		doThrow(new RuntimeException("Milvus error")).when(milvusVectorStore).delete(anyList());

		Exception exception = assertThrows(Exception.class,
				() -> milvusVectorStoreManagementService.deleteDocuments(deleteRequest));
		assertTrue(exception.getMessage().contains("Failed to delete documents"));
	}

	@Test
	void schema() throws Exception {
		SchemaInitRequest request = new SchemaInitRequest();
		DbConfig dbConfig = new DbConfig();
		dbConfig.setSchema("test_schema");
		request.setDbConfig(dbConfig);
		request.setTables(Collections.singletonList("test_table"));

		TableInfoBO tableInfo = new TableInfoBO();
		tableInfo.setName("test_table");
		tableInfo.setDescription("Test Table");

		ColumnInfoBO columnInfo = new ColumnInfoBO();
		columnInfo.setName("id");
		columnInfo.setDescription("Primary Key");
		columnInfo.setPrimary(true);

		when(dbAccessor.fetchTables(any(), any())).thenReturn(Collections.singletonList(tableInfo));
		when(dbAccessor.showColumns(any(), any())).thenReturn(Collections.singletonList(columnInfo));
		when(dbAccessor.sampleColumn(any(), any())).thenReturn(new ArrayList<>());
		when(gson.toJson(any(List.class))).thenReturn("[]");

		Boolean result = milvusVectorStoreManagementService.schema(request);

		assertTrue(result);
		verify(milvusVectorStore, times(2)).delete(any(Filter.Expression.class));
		verify(milvusVectorStore, times(2)).add(anyList());

		ArgumentCaptor<List<Document>> captor = ArgumentCaptor.forClass(List.class);
		verify(milvusVectorStore, times(2)).add(captor.capture());

		List<Document> columnDocs = captor.getAllValues().get(0);
		assertEquals(1, columnDocs.size());
		assertEquals("test_table.id", columnDocs.get(0).getId());
		assertEquals("Primary Key", columnDocs.get(0).getText());
		assertEquals("column", columnDocs.get(0).getMetadata().get("vectorType"));

		List<Document> tableDocs = captor.getAllValues().get(1);
		assertEquals(1, tableDocs.size());
		assertEquals("test_table", tableDocs.get(0).getId());
		assertEquals("Test Table", tableDocs.get(0).getText());
		assertEquals("table", tableDocs.get(0).getMetadata().get("vectorType"));
	}

	@Test
	void convertToDocument() {
		TableInfoBO tableInfo = new TableInfoBO();
		tableInfo.setName("users");

		ColumnInfoBO columnInfo = new ColumnInfoBO();
		columnInfo.setName("user_id");
		columnInfo.setDescription("The user's unique identifier");
		columnInfo.setType("INT");
		columnInfo.setPrimary(true);
		columnInfo.setNotnull(true);
		columnInfo.setSamples("[\"1\", \"2\", \"3\"]");

		Document doc = milvusVectorStoreManagementService.convertToDocument(tableInfo, columnInfo);

		assertEquals("users.user_id", doc.getId());
		assertEquals("The user's unique identifier", doc.getText());
		Map<String, Object> metadata = doc.getMetadata();
		assertEquals("user_id", metadata.get("name"));
		assertEquals("users", metadata.get("tableName"));
		assertEquals("The user's unique identifier", metadata.get("description"));
		assertEquals("INT", metadata.get("type"));
		assertTrue((Boolean) metadata.get("primary"));
		assertTrue((Boolean) metadata.get("notnull"));
		assertEquals("column", metadata.get("vectorType"));
		assertEquals("[\"1\", \"2\", \"3\"]", metadata.get("samples"));
	}

	@Test
	void convertTableToDocument() {
		TableInfoBO tableInfo = new TableInfoBO();
		tableInfo.setSchema("public");
		tableInfo.setName("products");
		tableInfo.setDescription("Contains all product information");
		tableInfo.setForeignKey("category_id=categories.id");
		tableInfo.setPrimaryKey("product_id");

		Document doc = milvusVectorStoreManagementService.convertTableToDocument(tableInfo);

		assertEquals("products", doc.getId());
		assertEquals("Contains all product information", doc.getText());
		Map<String, Object> metadata = doc.getMetadata();
		assertEquals("public", metadata.get("schema"));
		assertEquals("products", metadata.get("name"));
		assertEquals("Contains all product information", metadata.get("description"));
		assertEquals("category_id=categories.id", metadata.get("foreignKey"));
		assertEquals("product_id", metadata.get("primaryKey"));
		assertEquals("table", metadata.get("vectorType"));
	}

}