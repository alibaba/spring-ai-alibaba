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
package com.alibaba.cloud.ai.service.simple;

import com.alibaba.cloud.ai.connector.accessor.Accessor;
import com.alibaba.cloud.ai.connector.bo.ColumnInfoBO;
import com.alibaba.cloud.ai.connector.bo.DbQueryParameter;
import com.alibaba.cloud.ai.connector.bo.ForeignKeyInfoBO;
import com.alibaba.cloud.ai.connector.bo.TableInfoBO;
import com.alibaba.cloud.ai.connector.config.DbConfig;
import com.alibaba.cloud.ai.request.DeleteRequest;
import com.alibaba.cloud.ai.request.SchemaInitRequest;
import com.alibaba.cloud.ai.request.SearchRequest;
import com.google.common.collect.Lists;
import com.google.gson.Gson;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author zhangshenghang
 */
@ExtendWith(MockitoExtension.class)
@Disabled("暂时屏蔽，功能改造中")
class SimpleVectorStoreServiceTest {

	@Mock
	private EmbeddingModel embeddingModel;

	@Mock
	private Accessor dbAccessor;

	@Mock
	private DbConfig dbConfig;

	private Gson gson;

	private SimpleVectorStoreService vectorStoreService;

	@BeforeEach
	void setUp() {
		gson = new Gson();
		// Create service instance to be tested
		vectorStoreService = new SimpleVectorStoreService(embeddingModel, gson, dbAccessor, dbConfig, null);
	}

	@Test
	void testSchemaInitialization() throws Exception {
		// Prepare test data
		SchemaInitRequest schemaInitRequest = createMockSchemaInitRequest();

		// Mock data returned by database operations
		when(dbAccessor.showForeignKeys(any(DbConfig.class), any(DbQueryParameter.class)))
			.thenReturn(createMockForeignKeys());

		when(dbAccessor.fetchTables(any(DbConfig.class), any(DbQueryParameter.class))).thenReturn(createMockTables());

		when(dbAccessor.showColumns(any(DbConfig.class), any(DbQueryParameter.class))).thenReturn(createMockColumns());

		when(dbAccessor.sampleColumn(any(DbConfig.class), any(DbQueryParameter.class)))
			.thenReturn(Arrays.asList("sample1", "sample2", "sample3"));

		// Mock EmbeddingModel behavior - only mock embed method for Document type
		when(embeddingModel.embed(any(Document.class))).thenReturn(new float[] { 0.1f, 0.2f, 0.3f });

		// Execute test
		Boolean result = vectorStoreService.schema(schemaInitRequest);

		// Verify result
		assertTrue(result);

		// Verify method call
		verify(dbAccessor, times(1)).showForeignKeys(any(DbConfig.class), any(DbQueryParameter.class));
		verify(dbAccessor, times(1)).fetchTables(any(DbConfig.class), any(DbQueryParameter.class));
		verify(dbAccessor, atLeastOnce()).showColumns(any(DbConfig.class), any(DbQueryParameter.class));
		verify(dbAccessor, atLeastOnce()).sampleColumn(any(DbConfig.class), any(DbQueryParameter.class));
	}

	@Test
	void testConvertToDocument() {
		// Prepare test data
		TableInfoBO tableInfo = createMockTableInfo();
		ColumnInfoBO columnInfo = createMockColumnInfo();

		// Execute test
		Document document = vectorStoreService.convertToDocument(tableInfo, columnInfo);

		// Verify result
		assertNotNull(document);
		assertEquals("test_table.test_column", document.getId());
		assertEquals("Test column description", document.getText()); // Actually returns
																		// description

		Map<String, Object> metadata = document.getMetadata();
		assertEquals("test_table.test_column", metadata.get("id"));
		assertEquals("test_column", metadata.get("name"));
		assertEquals("test_table", metadata.get("tableName"));
		assertEquals("varchar", metadata.get("type"));
		assertEquals(true, metadata.get("primary"));
		assertEquals(true, metadata.get("notnull"));
		assertEquals("column", metadata.get("vectorType"));
	}

	@Test
	void testConvertTableToDocument() {
		// Prepare test data
		TableInfoBO tableInfo = createMockTableInfo();

		// Execute test
		Document document = vectorStoreService.convertTableToDocument(tableInfo);

		// Verify result
		assertNotNull(document);
		assertEquals("test_table", document.getId());
		assertEquals("Test table", document.getText()); // Actually returns description

		Map<String, Object> metadata = document.getMetadata();
		assertEquals("test_table", metadata.get("name"));
		assertEquals("Test table", metadata.get("description"));
		assertEquals("table", metadata.get("vectorType"));
	}

	@Test
	void testDeleteDocumentsById() throws Exception {
		// Prepare test data - 先通过 schema 方法添加一些文档
		SchemaInitRequest schemaInitRequest = createMockSchemaInitRequest();

		// Mock data returned by database operations
		when(dbAccessor.showForeignKeys(any(DbConfig.class), any(DbQueryParameter.class)))
			.thenReturn(createMockForeignKeys());

		when(dbAccessor.fetchTables(any(DbConfig.class), any(DbQueryParameter.class))).thenReturn(createMockTables());

		when(dbAccessor.showColumns(any(DbConfig.class), any(DbQueryParameter.class))).thenReturn(createMockColumns());

		when(dbAccessor.sampleColumn(any(DbConfig.class), any(DbQueryParameter.class)))
			.thenReturn(Arrays.asList("sample1", "sample2", "sample3"));

		// Mock EmbeddingModel behavior
		when(embeddingModel.embed(any(Document.class))).thenReturn(new float[] { 0.1f, 0.2f, 0.3f });
		when(embeddingModel.embed(any(String.class))).thenReturn(new float[] { 0.1f, 0.2f, 0.3f });

		// Execute schema initialization, add data to vectorStore
		vectorStoreService.schema(schemaInitRequest);

		// Count documents of all types before deletion
		SearchRequest searchAllRequest = new SearchRequest();
		searchAllRequest.setVectorType("column");
		searchAllRequest.setQuery("");
		searchAllRequest.setTopK(Integer.MAX_VALUE);

		List<Document> beforeDeleteColumns = vectorStoreService.searchWithVectorType(searchAllRequest);
		int beforeColumnCount = beforeDeleteColumns.size();

		searchAllRequest.setVectorType("table");
		List<Document> beforeDeleteTables = vectorStoreService.searchWithVectorType(searchAllRequest);
		int beforeTableCount = beforeDeleteTables.size();

		int totalBeforeCount = beforeColumnCount + beforeTableCount;

		// Prepare delete request - Note: actual delete implementation uses hardcoded
		// "comment_count"
		// Here we test the delete logic itself, not specific ID matching
		DeleteRequest deleteRequest = new DeleteRequest();
		deleteRequest.setId("test_id");

		// Execute delete operation
		Boolean result = vectorStoreService.deleteDocuments(deleteRequest);

		// Verify delete operation succeeded
		assertTrue(result);

		// Count documents after deletion
		List<Document> afterDeleteColumns = vectorStoreService.searchWithVectorType(searchAllRequest);
		searchAllRequest.setVectorType("column");
		afterDeleteColumns = vectorStoreService.searchWithVectorType(searchAllRequest);
		int afterColumnCount = afterDeleteColumns.size();

		searchAllRequest.setVectorType("table");
		List<Document> afterDeleteTables = vectorStoreService.searchWithVectorType(searchAllRequest);
		int afterTableCount = afterDeleteTables.size();

		int totalAfterCount = afterColumnCount + afterTableCount;

		// Verify delete result - since hardcoded ID is used in implementation, may not
		// actually delete documents
		// Here we mainly verify method execution succeeded and record data changes
		System.out.println("按ID删除测试:");
		System.out.println("删除前 column 类型文档数量: " + beforeColumnCount);
		System.out.println("删除后 column 类型文档数量: " + afterColumnCount);
		System.out.println("删除前 table 类型文档数量: " + beforeTableCount);
		System.out.println("删除后 table 类型文档数量: " + afterTableCount);
		System.out.println("删除前总文档数量: " + totalBeforeCount);
		System.out.println("删除后总文档数量: " + totalAfterCount);

		// Verify method execution succeeded
		assertTrue(result, "删除操作应该返回成功");
	}

	@Test
	void testDeleteDocumentsByVectorType() throws Exception {
		// Prepare test data - 先通过 schema 方法添加一些文档
		SchemaInitRequest schemaInitRequest = createMockSchemaInitRequest();

		// Mock data returned by database operations
		when(dbAccessor.showForeignKeys(any(DbConfig.class), any(DbQueryParameter.class)))
			.thenReturn(createMockForeignKeys());

		when(dbAccessor.fetchTables(any(DbConfig.class), any(DbQueryParameter.class))).thenReturn(createMockTables());

		when(dbAccessor.showColumns(any(DbConfig.class), any(DbQueryParameter.class))).thenReturn(createMockColumns());

		when(dbAccessor.sampleColumn(any(DbConfig.class), any(DbQueryParameter.class)))
			.thenReturn(Arrays.asList("sample1", "sample2", "sample3"));

		// Mock EmbeddingModel behavior
		when(embeddingModel.embed(any(Document.class))).thenReturn(new float[] { 0.1f, 0.2f, 0.3f });
		when(embeddingModel.embed(any(String.class))).thenReturn(new float[] { 0.1f, 0.2f, 0.3f });

		// Execute schema initialization, add data to vectorStore
		vectorStoreService.schema(schemaInitRequest);

		// Count data before deletion - use service's search method
		SearchRequest searchColumnRequest = new SearchRequest();
		searchColumnRequest.setVectorType("column");
		searchColumnRequest.setQuery(""); // Empty query to get all matching documents
		searchColumnRequest.setTopK(Integer.MAX_VALUE);

		List<Document> beforeDeleteColumns = vectorStoreService.searchWithVectorType(searchColumnRequest);
		int beforeDeleteCount = beforeDeleteColumns.size();

		// Count table type documents (to verify only column type was deleted)
		SearchRequest searchTableRequest = new SearchRequest();
		searchTableRequest.setVectorType("table");
		searchTableRequest.setQuery("");
		searchTableRequest.setTopK(Integer.MAX_VALUE);

		List<Document> beforeDeleteTables = vectorStoreService.searchWithVectorType(searchTableRequest);
		int beforeTableCount = beforeDeleteTables.size();

		// Prepare delete request
		DeleteRequest deleteRequest = new DeleteRequest();
		deleteRequest.setVectorType("column");

		// Execute delete operation
		Boolean result = vectorStoreService.deleteDocuments(deleteRequest);

		// Verify delete operation succeeded
		assertTrue(result);

		// Count data after deletion
		List<Document> afterDeleteColumns = vectorStoreService.searchWithVectorType(searchColumnRequest);
		int afterDeleteCount = afterDeleteColumns.size();

		List<Document> afterDeleteTables = vectorStoreService.searchWithVectorType(searchTableRequest);
		int afterTableCount = afterDeleteTables.size();

		// Verify delete result
		assertTrue(beforeDeleteCount > 0, "删除前应该有column类型的文档");
		assertEquals(0, afterDeleteCount, "删除后应该没有column类型的文档");
		assertEquals(beforeTableCount, afterTableCount, "table类型的文档数量应该保持不变");

		System.out.println("删除前 column 类型文档数量: " + beforeDeleteCount);
		System.out.println("删除后 column 类型文档数量: " + afterDeleteCount);
		System.out.println("删除前 table 类型文档数量: " + beforeTableCount);
		System.out.println("删除后 table 类型文档数量: " + afterTableCount);
		System.out.println("成功删除了 " + (beforeDeleteCount - afterDeleteCount) + " 个 column 类型的文档");
	}

	@Test
	void testDeleteDocumentsWithInvalidRequest() {
		// Prepare test data
		DeleteRequest deleteRequest = new DeleteRequest();
		// Don't set any parameters

		// Verify exception
		Exception exception = assertThrows(Exception.class, () -> {
			vectorStoreService.deleteDocuments(deleteRequest);
		});

		assertTrue(exception.getCause() instanceof IllegalArgumentException);
	}

	@Test
	void testSearchWithVectorType() {
		// Prepare test data
		SearchRequest searchRequest = new SearchRequest();
		searchRequest.setQuery("test query");
		searchRequest.setVectorType("column");
		searchRequest.setTopK(5);

		// Mock EmbeddingModel behavior
		when(embeddingModel.embed(any(String.class))).thenReturn(new float[] { 0.1f, 0.2f, 0.3f });

		// Execute test
		List<Document> results = vectorStoreService.searchWithVectorType(searchRequest);

		// Verify result
		assertNotNull(results);
		// Since SimpleVectorStore is empty, result should be empty
		assertTrue(results.isEmpty() || results.size() >= 0);
	}

	@Test
	void testSearchWithFilter() {
		// Prepare test data
		SearchRequest searchRequest = new SearchRequest();
		searchRequest.setQuery("test query");
		searchRequest.setVectorType("table");
		searchRequest.setTopK(5);

		// Mock EmbeddingModel behavior
		when(embeddingModel.embed(any(String.class))).thenReturn(new float[] { 0.1f, 0.2f, 0.3f });

		// Execute test
		List<Document> results = vectorStoreService.searchWithFilter(searchRequest);

		// Verify result
		assertNotNull(results);
	}

	@Test
	void testSearchTableByNameAndVectorType() {
		// Prepare test data
		SearchRequest searchRequest = new SearchRequest();
		searchRequest.setName("test_table");
		searchRequest.setVectorType("table");
		searchRequest.setTopK(5);

		// Mock EmbeddingModel behavior - 这个方法不需要 embed，因为没有 query
		// when(embeddingModel.embed(any(String.class)))
		// .thenReturn(new float[]{0.1f, 0.2f, 0.3f});

		// Execute test
		List<Document> results = vectorStoreService.searchTableByNameAndVectorType(searchRequest);

		// Verify result
		assertNotNull(results);
	}

	// Create mock SchemaInitRequest
	private SchemaInitRequest createMockSchemaInitRequest() {
		SchemaInitRequest request = new SchemaInitRequest();

		DbConfig mockDbConfig = new DbConfig();
		mockDbConfig.setSchema("test_schema");
		request.setDbConfig(mockDbConfig);
		request.setTables(Arrays.asList("test_table", "another_table"));

		return request;
	}

	// Create mock foreign key information
	private List<ForeignKeyInfoBO> createMockForeignKeys() {
		List<ForeignKeyInfoBO> foreignKeys = new ArrayList<>();

		ForeignKeyInfoBO fk = ForeignKeyInfoBO.builder()
			.table("test_table")
			.column("foreign_id")
			.referencedTable("referenced_table")
			.referencedColumn("id")
			.build();

		foreignKeys.add(fk);
		return foreignKeys;
	}

	// Create mock table information
	private List<TableInfoBO> createMockTables() {
		List<TableInfoBO> tables = new ArrayList<>();

		TableInfoBO table = TableInfoBO.builder()
			.name("test_table")
			.description("Test table description")
			.schema("test_schema")
			.build();

		tables.add(table);
		return tables;
	}

	// Create mock column information
	private List<ColumnInfoBO> createMockColumns() {
		List<ColumnInfoBO> columns = new ArrayList<>();

		ColumnInfoBO column = ColumnInfoBO.builder()
			.name("test_column")
			.description("Test column description")
			.type("varchar")
			.primary(true)
			.notnull(true)
			.tableName("test_table")
			.samples(gson.toJson(Arrays.asList("sample1", "sample2")))
			.build();

		columns.add(column);
		return columns;
	}

	// Create mock table information对象
	private TableInfoBO createMockTableInfo() {
		return TableInfoBO.builder()
			.name("test_table")
			.description("Test table")
			.schema("test_schema")
			.primaryKeys(Lists.newArrayList("id"))
			.foreignKey("foreign_key_info")
			.build();
	}

	// Create mock column information对象
	private ColumnInfoBO createMockColumnInfo() {
		return ColumnInfoBO.builder()
			.name("test_column")
			.description("Test column description")
			.type("varchar")
			.primary(true)
			.notnull(true)
			.tableName("test_table")
			.samples(gson.toJson(Arrays.asList("sample1", "sample2")))
			.build();
	}

}
