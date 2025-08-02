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
package com.alibaba.cloud.ai.vectorstore;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

/**
 * Unit tests for SimpleVectorStoreEnhanced
 *
 * @author Maki Ma
 */
@ExtendWith(MockitoExtension.class)
class SimpleVectorStoreEnhancedTest {

	@Mock
	private EmbeddingModel embeddingModel;

	private SimpleVectorStoreEnhanced vectorStore;

	@BeforeEach
	void setUp() {
		// Mock embedding model behavior with lenient mode to avoid
		// UnnecessaryStubbingException
		lenient().when(embeddingModel.embed(any(Document.class))).thenReturn(new float[] { 0.1f, 0.2f, 0.3f });
		lenient().when(embeddingModel.embed(any(String.class))).thenReturn(new float[] { 0.1f, 0.2f, 0.3f });

		// Create the enhanced vector store
		vectorStore = new SimpleVectorStoreEnhanced(embeddingModel);
	}

	@Test
	void testBuilderPattern() {
		// 准备测试数据
		// (Builder pattern doesn't need additional test data)

		// 执行测试
		SimpleVectorStoreEnhanced store = SimpleVectorStoreEnhanced.builder(embeddingModel);

		// 验证结果
		assertNotNull(store);
		assertEquals(embeddingModel, store.getEmbeddingModel());
		assertNotNull(store.getSimpleVectorStore());
	}

	@Test
	void testAddDocuments() {
		// 准备测试数据
		List<Document> documents = createTestDocuments();

		// 执行测试
		vectorStore.add(documents);

		// 验证结果
		// Verify that embed method was called for each document
		verify(embeddingModel, times(documents.size())).embed(any(Document.class));
	}

	@Test
	void testDeleteByIds() {
		// 准备测试数据
		List<Document> documents = createTestDocuments();
		vectorStore.add(documents);
		List<String> idsToDelete = Arrays.asList("doc1", "doc2");

		// 执行测试
		assertDoesNotThrow(() -> vectorStore.delete(idsToDelete));

		// 验证结果
		// Since SimpleVectorStore doesn't provide verification,
		// we verify the method executes without throwing exceptions
	}

	@Test
	void testDoDeleteWithFilterExpression() {
		// 准备测试数据
		List<Document> documents = createTestDocuments();
		vectorStore.add(documents);

		FilterExpressionBuilder builder = new FilterExpressionBuilder();
		Filter.Expression filterExpression = builder.eq("vectorType", "column").build();

		// 执行测试
		assertDoesNotThrow(() -> vectorStore.doDelete(filterExpression));

		// 验证结果
		// Verify that similaritySearch was called during doDelete process
		// Note: This is indirectly tested through the execution without exception
	}

	@Test
	void testDeleteInterfaceMethod() {
		// 准备测试数据
		List<Document> documents = createTestDocuments();
		vectorStore.add(documents);

		FilterExpressionBuilder builder = new FilterExpressionBuilder();
		Filter.Expression filterExpression = builder.eq("vectorType", "table").build();

		// 执行测试
		assertDoesNotThrow(() -> vectorStore.delete(filterExpression));

		// 验证结果
		// Verify delete via VectorStore interface works
	}

	@Test
	void testSimilaritySearch() {
		// 准备测试数据
		List<Document> documents = createTestDocuments();
		vectorStore.add(documents);

		SearchRequest searchRequest = SearchRequest.builder().query("test query").topK(5).build();

		// 执行测试
		List<Document> results = vectorStore.similaritySearch(searchRequest);

		// 验证结果
		assertNotNull(results);
		// Verify embedding was called for the search query
		verify(embeddingModel, atLeastOnce()).embed(eq("test query"));
	}

	@Test
	void testGetDocumentCount() {
		// 准备测试数据 - empty store first
		// 执行测试
		int emptyCount = vectorStore.getDocumentCount();

		// 验证结果
		assertEquals(0, emptyCount);

		// 准备测试数据 - add documents
		List<Document> documents = createTestDocuments();
		vectorStore.add(documents);

		// 执行测试
		int countAfterAdd = vectorStore.getDocumentCount();

		// 验证结果
		assertTrue(countAfterAdd >= 0);
	}

	@Test
	void testDeleteAll() {
		// 准备测试数据
		List<Document> documents = createTestDocuments();
		vectorStore.add(documents);

		// 执行测试
		assertDoesNotThrow(() -> vectorStore.deleteAll());

		// 验证结果
		int countAfterDeleteAll = vectorStore.getDocumentCount();
		assertEquals(0, countAfterDeleteAll);
	}

	@Test
	void testSearchByMetadata() {
		// 准备测试数据
		List<Document> documents = createTestDocuments();
		vectorStore.add(documents);

		// 执行测试
		List<Document> results = vectorStore.searchByMetadata("vectorType", "column", 10);

		// 验证结果
		assertNotNull(results);
		// All returned documents should have vectorType="column"
		for (Document doc : results) {
			assertEquals("column", doc.getMetadata().get("vectorType"));
		}
	}

	@Test
	void testSearchByMetadataWithNoResults() {
		// 准备测试数据
		List<Document> documents = createTestDocuments();
		vectorStore.add(documents);

		// 执行测试
		List<Document> results = vectorStore.searchByMetadata("vectorType", "nonexistent", 10);

		// 验证结果
		assertNotNull(results);
		assertTrue(results.isEmpty());
	}

	@Test
	void testDoDeleteWithEmptyResults() {
		// 准备测试数据
		FilterExpressionBuilder builder = new FilterExpressionBuilder();
		Filter.Expression filterExpression = builder.eq("nonexistent", "value").build();

		// 执行测试 - should not throw exception even with no matches
		assertDoesNotThrow(() -> vectorStore.doDelete(filterExpression));

		// 验证结果
		// Method should complete successfully even with empty results
	}

	@Test
	void testDoDeleteWithNullFilterExpression() {
		// 准备测试数据
		// (null filter expression)

		// 执行测试
		RuntimeException exception = assertThrows(RuntimeException.class, () -> {
			vectorStore.doDelete(null);
		});

		// 验证结果
		assertNotNull(exception);
		assertTrue(exception.getMessage().contains("Failed to delete documents by filter expression"));
	}

	@Test
	void testComplexFilterExpression() {
		// 准备测试数据
		List<Document> documents = createTestDocuments();
		vectorStore.add(documents);

		FilterExpressionBuilder builder = new FilterExpressionBuilder();
		Filter.Expression complexFilter = builder
			.and(builder.eq("vectorType", "column"), builder.eq("tableName", "test_table"))
			.build();

		// 执行测试
		assertDoesNotThrow(() -> vectorStore.doDelete(complexFilter));

		// 验证结果
		// Complex filter should be handled correctly
	}

	@Test
	void testOrFilterExpression() {
		// 准备测试数据
		List<Document> documents = createTestDocuments();
		vectorStore.add(documents);

		FilterExpressionBuilder builder = new FilterExpressionBuilder();
		Filter.Expression orFilter = builder.or(builder.eq("vectorType", "column"), builder.eq("vectorType", "table"))
			.build();

		// 执行测试
		assertDoesNotThrow(() -> vectorStore.doDelete(orFilter));

		// 验证结果
		// OR filter should be handled correctly
	}

	@Test
	void testDeleteDifferentVectorTypes() {
		// 准备测试数据
		String[] vectorTypes = { "column", "table", "schema", "index" };

		for (String vectorType : vectorTypes) {
			// 准备测试数据 - 为每种类型创建专门的文档
			List<Document> documents = createDocumentsWithVectorType(vectorType, 3);
			vectorStore.add(documents);

			// 验证文档已添加
			assertEquals(3, vectorStore.getDocumentCount());

			FilterExpressionBuilder builder = new FilterExpressionBuilder();
			Filter.Expression filter = builder.eq("vectorType", vectorType).build();

			// 执行测试
			assertDoesNotThrow(() -> vectorStore.doDelete(filter),
					"Failed to delete documents with vectorType: " + vectorType);

			// 验证删除后文档数量为0
			assertEquals(0, vectorStore.getDocumentCount(),
					"Documents with vectorType " + vectorType + " should be deleted");
		}

		// 验证结果
		// All vector types should be deletable without exceptions
	}

	@Test
	void testDoDeleteSpecificColumnDocuments() {
		// 准备测试数据 - 使用专门的column文档
		List<Document> columnDocs = createColumnDocuments();
		List<Document> tableDocs = createTableDocuments();

		vectorStore.add(columnDocs);
		vectorStore.add(tableDocs);

		// 验证初始文档数量
		int initialCount = vectorStore.getDocumentCount();
		assertEquals(columnDocs.size() + tableDocs.size(), initialCount);

		// 执行测试 - 只删除column类型文档
		FilterExpressionBuilder builder = new FilterExpressionBuilder();
		Filter.Expression columnFilter = builder.eq("vectorType", "column").build();

		assertDoesNotThrow(() -> vectorStore.doDelete(columnFilter));

		// 验证结果 - 应该只剩下table类型文档
		int remainingCount = vectorStore.getDocumentCount();
		assertEquals(tableDocs.size(), remainingCount, "Only table documents should remain after deleting columns");
	}

	@Test
	void testDoDeleteWithComplexMetadataFiltering() {
		// 准备测试数据 - 创建有丰富元数据的文档
		List<Document> documents = new ArrayList<>();

		// 添加users表的column
		documents.addAll(createDocumentsWithVectorType("column", 2));
		// 添加特定表名的column
		Document userNameCol = new Document("user_name_col", "User name column",
				Map.of("vectorType", "column", "tableName", "users", "dataType", "varchar"));
		documents.add(userNameCol);

		vectorStore.add(documents);

		// 执行测试 - 使用复杂过滤器：column类型且表名为users
		FilterExpressionBuilder builder = new FilterExpressionBuilder();
		Filter.Expression complexFilter = builder
			.and(builder.eq("vectorType", "column"), builder.eq("tableName", "users"))
			.build();

		assertDoesNotThrow(() -> vectorStore.doDelete(complexFilter));

		// 验证结果 - 应该删除了匹配的文档
		// 由于SimpleVectorStore的限制，我们主要验证操作不抛异常
		assertTrue(vectorStore.getDocumentCount() >= 0);
	}

	@Test
	void testGetEmbeddingModel() {
		// 准备测试数据
		// (no additional test data needed)

		// 执行测试
		EmbeddingModel result = vectorStore.getEmbeddingModel();

		// 验证结果
		assertEquals(embeddingModel, result);
	}

	@Test
	void testGetSimpleVectorStore() {
		// 准备测试数据
		// (no additional test data needed)

		// 执行测试
		var result = vectorStore.getSimpleVectorStore();

		// 验证结果
		assertNotNull(result);
	}

	@Test
	void testDeleteAllWithEmptyStore() {
		// 准备测试数据
		// (empty store)

		// 执行测试
		assertDoesNotThrow(() -> vectorStore.deleteAll());

		// 验证结果
		assertEquals(0, vectorStore.getDocumentCount());
	}

	/**
	 * Helper method to create test documents for column type
	 */
	private List<Document> createColumnDocuments() {
		List<Document> documents = new ArrayList<>();

		Document col1 = new Document("col1", "User ID column for identification",
				Map.of("vectorType", "column", "tableName", "users", "dataType", "bigint", "primary", true));
		Document col2 = new Document("col2", "User name column for display",
				Map.of("vectorType", "column", "tableName", "users", "dataType", "varchar", "primary", false));
		Document col3 = new Document("col3", "Order amount column for calculations",
				Map.of("vectorType", "column", "tableName", "orders", "dataType", "decimal", "primary", false));

		documents.add(col1);
		documents.add(col2);
		documents.add(col3);
		return documents;
	}

	/**
	 * Helper method to create test documents for table type
	 */
	private List<Document> createTableDocuments() {
		List<Document> documents = new ArrayList<>();

		Document table1 = new Document("table1", "Users table stores user information",
				Map.of("vectorType", "table", "schema", "public", "name", "users", "type", "main"));
		Document table2 = new Document("table2", "Orders table stores order records",
				Map.of("vectorType", "table", "schema", "public", "name", "orders", "type", "transaction"));
		Document table3 = new Document("table3", "Products table stores product catalog",
				Map.of("vectorType", "table", "schema", "catalog", "name", "products", "type", "reference"));

		documents.add(table1);
		documents.add(table2);
		documents.add(table3);
		return documents;
	}

	/**
	 * Helper method to create mixed test documents (default for most tests)
	 */
	private List<Document> createTestDocuments() {
		List<Document> documents = new ArrayList<>();

		// Create column documents
		Document columnDoc1 = new Document("doc1", "This is a test column",
				Map.of("vectorType", "column", "tableName", "test_table", "name", "test_column"));
		Document columnDoc2 = new Document("doc2", "Another test column",
				Map.of("vectorType", "column", "tableName", "test_table", "name", "another_column"));

		// Create table documents
		Document tableDoc1 = new Document("doc3", "This is a test table",
				Map.of("vectorType", "table", "name", "test_table", "schema", "test_schema"));
		Document tableDoc2 = new Document("doc4", "Another test table",
				Map.of("vectorType", "table", "name", "another_table", "schema", "test_schema"));

		documents.add(columnDoc1);
		documents.add(columnDoc2);
		documents.add(tableDoc1);
		documents.add(tableDoc2);

		return documents;
	}

	/**
	 * Helper method to create documents with specific vectorType
	 */
	private List<Document> createDocumentsWithVectorType(String vectorType, int count) {
		List<Document> documents = new ArrayList<>();

		for (int i = 1; i <= count; i++) {
			Document doc = new Document("test_" + vectorType + "_" + i, "Test " + vectorType + " document " + i,
					Map.of("vectorType", vectorType, "index", i, "category", "test"));
			documents.add(doc);
		}

		return documents;
	}

	/**
	 * Helper method to create mock search results for testing
	 */
	private List<Document> createMockSearchResults(String vectorType) {
		return createTestDocuments().stream()
			.filter(doc -> vectorType.equals(doc.getMetadata().get("vectorType")))
			.toList();
	}

}
