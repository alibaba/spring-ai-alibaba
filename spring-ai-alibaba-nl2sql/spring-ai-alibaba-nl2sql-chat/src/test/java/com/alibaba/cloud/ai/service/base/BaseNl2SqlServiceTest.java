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
package com.alibaba.cloud.ai.service.base;

import com.alibaba.cloud.ai.dbconnector.DbAccessor;
import com.alibaba.cloud.ai.dbconnector.DbConfig;
import com.alibaba.cloud.ai.dbconnector.bo.DbQueryParameter;
import com.alibaba.cloud.ai.dbconnector.bo.ResultSetBO;
import com.alibaba.cloud.ai.dto.schema.ColumnDTO;
import com.alibaba.cloud.ai.dto.schema.SchemaDTO;
import com.alibaba.cloud.ai.dto.schema.TableDTO;
import com.alibaba.cloud.ai.service.LlmService;
import com.google.gson.Gson;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.document.Document;
import reactor.core.publisher.Flux;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * BaseNl2SqlService 测试类
 *
 * @author test
 */
@ExtendWith(MockitoExtension.class)
class BaseNl2SqlServiceTest {

	private static final Logger log = LoggerFactory.getLogger(BaseNl2SqlServiceTest.class);

	@Mock
	private BaseVectorStoreService vectorStoreService;

	@Mock
	private BaseSchemaService schemaService;

	@Mock
	private LlmService aiService;

	@Mock
	private DbAccessor dbAccessor;

	@Mock
	private DbConfig dbConfig;

	@Mock
	private ChatResponse chatResponse;

	private BaseNl2SqlService baseNl2SqlService;

	private Gson gson;

	@BeforeEach
	void setUp() {
		log.info("Setting up BaseNl2SqlServiceTest");
		gson = new Gson();
		baseNl2SqlService = new BaseNl2SqlService(vectorStoreService, schemaService, aiService, dbAccessor, dbConfig);
	}

	@Test
	void testConstructor() {
		log.info("Testing constructor");

		assertNotNull(baseNl2SqlService);
		assertNotNull(baseNl2SqlService.aiService);

		log.info("Constructor test passed");
	}

	@Test
	void testRewrite() throws Exception {
		log.info("Testing rewrite method");

		String query = "查询用户信息";
		String expectedRewrittenQuery = "查询所有用户的详细信息";

		// Mock 依赖方法的返回值
		List<String> mockEvidences = Arrays.asList("evidence1", "evidence2");
		when(vectorStoreService.getDocuments(eq(query), eq("evidence"))).thenReturn(createMockDocuments(mockEvidences));

		SchemaDTO mockSchemaDTO = createMockSchemaDTO();
		when(schemaService.mixRag(anyString(), anyList())).thenReturn(mockSchemaDTO);
		when(aiService.call(anyString())).thenReturn("[\"keyword1\", \"keyword2\"]")
			.thenReturn("[]")
			.thenReturn("需求类型：正常查询\n需求内容：" + expectedRewrittenQuery);

		// 执行测试
		String result = baseNl2SqlService.rewrite(query);

		// 验证结果
		assertEquals(expectedRewrittenQuery, result);

		// 验证方法调用
		verify(vectorStoreService, times(1)).getDocuments(eq(query), eq("evidence"));
		verify(aiService, atLeastOnce()).call(anyString());

		log.info("Rewrite test passed with result: {}", result);
	}

	@Test
	void testRewriteWithSmallTalk() throws Exception {
		log.info("Testing rewrite method with small talk");

		String query = "你好";

		// Mock 依赖方法的返回值
		List<String> mockEvidences = Arrays.asList("evidence1");
		when(vectorStoreService.getDocuments(eq(query), eq("evidence"))).thenReturn(createMockDocuments(mockEvidences));

		SchemaDTO mockSchemaDTO = createMockSchemaDTO();
		when(schemaService.mixRag(anyString(), anyList())).thenReturn(mockSchemaDTO);
		when(aiService.call(anyString())).thenReturn("[\"greeting\"]") // extractKeywords
			.thenReturn("[]") // fineSelect
			.thenReturn("需求类型：《自由闲聊》\n需求内容：用户问好"); // buildRewritePrompt

		// 执行测试
		String result = baseNl2SqlService.rewrite(query);

		// 验证结果
		assertEquals("闲聊拒识", result);

		log.info("Small talk rewrite test passed");
	}

	@Test
	void testRewriteWithUnclearIntent() throws Exception {
		log.info("Testing rewrite method with unclear intent");

		String query = "模糊的查询";

		// Mock 依赖方法的返回值
		List<String> mockEvidences = Arrays.asList("evidence1");
		when(vectorStoreService.getDocuments(eq(query), eq("evidence"))).thenReturn(createMockDocuments(mockEvidences));

		SchemaDTO mockSchemaDTO = createMockSchemaDTO();
		when(schemaService.mixRag(anyString(), anyList())).thenReturn(mockSchemaDTO);
		when(aiService.call(anyString())).thenReturn("[\"unclear\"]") // extractKeywords
			.thenReturn("[]") // fineSelect
			.thenReturn("需求类型：《需要澄清》\n需求内容：需要更多信息"); // buildRewritePrompt

		// 执行测试
		String result = baseNl2SqlService.rewrite(query);

		// 验证结果
		assertEquals("意图模糊需要澄清", result);

		log.info("Unclear intent rewrite test passed");
	}

	@Test
	void testRewriteStream() throws Exception {
		log.info("Testing rewriteStream method");

		String query = "查询用户信息";

		// Mock 依赖方法的返回值
		List<String> mockEvidences = Arrays.asList("evidence1", "evidence2");
		when(vectorStoreService.getDocuments(eq(query), eq("evidence"))).thenReturn(createMockDocuments(mockEvidences));

		SchemaDTO mockSchemaDTO = createMockSchemaDTO();
		when(schemaService.mixRag(anyString(), anyList())).thenReturn(mockSchemaDTO);
		when(aiService.call(anyString())).thenReturn("[\"keyword1\", \"keyword2\"]");

		Flux<ChatResponse> mockFlux = Flux.just(chatResponse);
		when(aiService.streamCall(anyString())).thenReturn(mockFlux);

		// 执行测试
		Flux<ChatResponse> result = baseNl2SqlService.rewriteStream(query);

		// 验证结果
		assertNotNull(result);
		// 验证流式结果（简化验证，因为无法使用 StepVerifier）
		assertFalse(result.toIterable().iterator().hasNext() == false);

		// 验证方法调用
		verify(aiService, times(1)).streamCall(anyString());

		log.info("RewriteStream test passed");
	}

	@Test
	void testNl2sql() throws Exception {
		log.info("Testing nl2sql method");

		String query = "查询用户表中年龄大于18的用户";
		String expectedSql = "SELECT * FROM users WHERE age > 18";

		// 配置 DbConfig Mock
		when(dbConfig.getDialectType()).thenReturn("mysql");

		// Mock 依赖方法的返回值
		List<String> mockEvidences = Arrays.asList("用户表包含用户信息", "年龄字段为age");
		when(vectorStoreService.getDocuments(eq(query), eq("evidence"))).thenReturn(createMockDocuments(mockEvidences));

		SchemaDTO mockSchemaDTO = createMockSchemaDTO();
		when(schemaService.mixRag(anyString(), anyList())).thenReturn(mockSchemaDTO);

		// Mock LLM 调用
		when(aiService.call(anyString())).thenReturn("[\"用户\", \"年龄\"]") // extractKeywords
			.thenReturn("[\"2024-01-01\"]") // buildDateTimeExtractPrompt
			.thenReturn("[]"); // buildMixSelectorPrompt

		when(aiService.callWithSystemPrompt(anyString(), anyString())).thenReturn("```sql\n" + expectedSql + "\n```");

		// 执行测试
		String result = baseNl2SqlService.nl2sql(query);

		// 验证结果
		assertEquals(expectedSql, result);

		// 验证方法调用
		verify(vectorStoreService, times(1)).getDocuments(eq(query), eq("evidence"));
		verify(schemaService, times(1)).mixRag(anyString(), anyList());
		verify(aiService, atLeastOnce()).call(anyString());

		log.info("Nl2sql test passed with result: {}", result);
	}

	@Test
	void testExecuteSql() throws Exception {
		log.info("Testing executeSql method");

		String sql = "SELECT * FROM users";

		// Mock 数据库执行结果
		ResultSetBO mockResultSet = new ResultSetBO();
		mockResultSet.setColumn(Arrays.asList("id", "name", "age"));

		// 创建测试数据
		List<Map<String, String>> data = new ArrayList<>();
		Map<String, String> row = new HashMap<>();
		row.put("id", "1");
		row.put("name", "John");
		row.put("age", "25");
		data.add(row);
		mockResultSet.setData(data);

		when(dbAccessor.executeSqlAndReturnObject(any(DbConfig.class), any(DbQueryParameter.class)))
			.thenReturn(mockResultSet);

		// 由于 MdTableGenerator.generateTable 是静态方法，我们主要验证流程
		String result = baseNl2SqlService.executeSql(sql);

		// 验证方法调用
		verify(dbAccessor, times(1)).executeSqlAndReturnObject(any(DbConfig.class), any(DbQueryParameter.class));
		assertNotNull(result);

		log.info("ExecuteSql test passed");
	}

	@Test
	void testSemanticConsistencyStream() throws Exception {
		log.info("Testing semanticConsistencyStream method");

		String sql = "SELECT * FROM users";
		String queryPrompt = "查询所有用户";

		Flux<ChatResponse> mockFlux = Flux.just(chatResponse);
		when(aiService.streamCall(anyString())).thenReturn(mockFlux);

		// 执行测试
		Flux<ChatResponse> result = baseNl2SqlService.semanticConsistencyStream(sql, queryPrompt);

		// 验证结果
		assertNotNull(result);
		// 验证流式结果（简化验证）
		assertFalse(result.toIterable().iterator().hasNext() == false);

		// 验证方法调用
		verify(aiService, times(1)).streamCall(anyString());

		log.info("SemanticConsistencyStream test passed");
	}

	@Test
	void testExpandQuestion() {
		log.info("Testing expandQuestion method");

		String query = "查询用户信息";
		List<String> expectedExpansions = Arrays.asList("查询用户信息", "获取用户数据", "显示用户详情");

		// Mock LLM 返回的 JSON 响应
		when(aiService.call(anyString())).thenReturn(gson.toJson(expectedExpansions));

		// 执行测试
		List<String> result = baseNl2SqlService.expandQuestion(query);

		// 验证结果
		assertNotNull(result);
		assertEquals(expectedExpansions.size(), result.size());
		assertTrue(result.contains("查询用户信息"));

		// 验证方法调用
		verify(aiService, times(1)).call(anyString());

		log.info("ExpandQuestion test passed with {} expanded questions", result.size());
	}

	@Test
	void testExpandQuestionWithException() {
		log.info("Testing expandQuestion method with exception");

		String query = "查询用户信息";

		// Mock LLM 抛出异常
		when(aiService.call(anyString())).thenThrow(new RuntimeException("LLM error"));

		// 执行测试
		List<String> result = baseNl2SqlService.expandQuestion(query);

		// 验证结果 - 应该返回原始问题
		assertNotNull(result);
		assertEquals(1, result.size());
		assertEquals(query, result.get(0));

		log.info("ExpandQuestion exception test passed");
	}

	@Test
	void testExtractEvidences() {
		log.info("Testing extractEvidences method");

		String query = "查询用户信息";
		List<String> expectedEvidences = Arrays.asList("evidence1", "evidence2");

		when(vectorStoreService.getDocuments(eq(query), eq("evidence")))
			.thenReturn(createMockDocuments(expectedEvidences));

		// 执行测试
		List<String> result = baseNl2SqlService.extractEvidences(query);

		// 验证结果
		assertNotNull(result);
		assertEquals(expectedEvidences.size(), result.size());
		assertEquals(expectedEvidences, result);

		// 验证方法调用
		verify(vectorStoreService, times(1)).getDocuments(eq(query), eq("evidence"));

		log.info("ExtractEvidences test passed with {} evidences", result.size());
	}

	@Test
	void testExtractKeywords() {
		log.info("Testing extractKeywords method");

		String query = "查询用户信息";
		List<String> evidenceList = Arrays.asList("用户表", "信息字段");
		List<String> expectedKeywords = Arrays.asList("用户", "信息", "查询");

		when(aiService.call(anyString())).thenReturn(gson.toJson(expectedKeywords));

		// 执行测试
		List<String> result = baseNl2SqlService.extractKeywords(query, evidenceList);

		// 验证结果
		assertNotNull(result);
		assertEquals(expectedKeywords.size(), result.size());
		assertEquals(expectedKeywords, result);

		// 验证方法调用
		verify(aiService, times(1)).call(anyString());

		log.info("ExtractKeywords test passed with {} keywords", result.size());
	}

	@Test
	void testSelect() throws Exception {
		log.info("Testing select method");

		String query = "查询用户信息";
		List<String> evidenceList = Arrays.asList("evidence1", "evidence2");

		SchemaDTO mockSchemaDTO = createMockSchemaDTO();
		when(aiService.call(anyString())).thenReturn("[\"keyword1\", \"keyword2\"]") // extractKeywords
			.thenReturn("[]"); // fineSelect
		when(schemaService.mixRag(anyString(), anyList())).thenReturn(mockSchemaDTO);

		// 执行测试
		SchemaDTO result = baseNl2SqlService.select(query, evidenceList);

		// 验证结果
		assertNotNull(result);
		assertEquals(mockSchemaDTO.getName(), result.getName());

		// 验证方法调用
		verify(schemaService, times(1)).mixRag(anyString(), anyList());
		verify(aiService, atLeastOnce()).call(anyString());

		log.info("Select test passed");
	}

	@Test
	void testIsRecallInfoSatisfyRequirement() {
		log.info("Testing isRecallInfoSatisfyRequirement method");

		String query = "查询用户信息";
		SchemaDTO schemaDTO = createMockSchemaDTO();
		List<String> evidenceList = Arrays.asList("evidence1", "evidence2");
		String expectedResponse = "信息满足要求";

		// 配置 DbConfig Mock
		when(dbConfig.getDialectType()).thenReturn("mysql");

		when(aiService.call(anyString())).thenReturn(expectedResponse);

		// 执行测试
		String result = baseNl2SqlService.isRecallInfoSatisfyRequirement(query, schemaDTO, evidenceList);

		// 验证结果
		assertEquals(expectedResponse, result);

		// 验证方法调用
		verify(aiService, times(1)).call(anyString());

		log.info("IsRecallInfoSatisfyRequirement test passed");
	}

	@Test
	void testGenerateSqlWithoutExistingSql() throws Exception {
		log.info("Testing generateSql method without existing SQL");

		String query = "查询用户信息";
		List<String> evidenceList = Arrays.asList("evidence1", "evidence2");
		SchemaDTO schemaDTO = createMockSchemaDTO();
		String expectedSql = "SELECT * FROM users";

		// 配置 DbConfig Mock
		when(dbConfig.getDialectType()).thenReturn("mysql");

		when(aiService.call(anyString())).thenReturn("[\"2024-01-01\"]");
		when(aiService.callWithSystemPrompt(anyString(), anyString())).thenReturn("```sql\n" + expectedSql + "\n```");

		// 执行测试
		String result = baseNl2SqlService.generateSql(evidenceList, query, schemaDTO);

		// 验证结果
		assertEquals(expectedSql, result);

		// 验证方法调用
		verify(aiService, times(1)).call(anyString());
		verify(aiService, times(1)).callWithSystemPrompt(anyString(), anyString());

		log.info("GenerateSql test passed with result: {}", result);
	}

	@Test
	void testGenerateSqlWithExistingSql() throws Exception {
		log.info("Testing generateSql method with existing SQL");

		String query = "查询用户信息";
		List<String> evidenceList = Arrays.asList("evidence1", "evidence2");
		SchemaDTO schemaDTO = createMockSchemaDTO();
		String existingSql = "SELECT * FROM user";
		String exceptionMessage = "Table 'user' not found";
		String expectedSql = "SELECT * FROM users";

		// 配置 DbConfig Mock
		when(dbConfig.getDialectType()).thenReturn("mysql");

		when(aiService.call(anyString())).thenReturn("[\"2024-01-01\"]") // buildDateTimeExtractPrompt
			.thenReturn("```sql\n" + expectedSql + "\n```"); // buildSqlErrorFixerPrompt

		// 执行测试
		String result = baseNl2SqlService.generateSql(evidenceList, query, schemaDTO, existingSql, exceptionMessage);

		// 验证结果
		assertEquals(expectedSql, result);

		// 验证方法调用
		verify(aiService, times(2)).call(anyString());

		log.info("GenerateSql with existing SQL test passed with result: {}", result);
	}

	@Test
	void testFineSelectWithAdvice() {
		log.info("Testing fineSelect method with advice");

		SchemaDTO schemaDTO = createMockSchemaDTO();
		String advice = "建议使用用户表和订单表";
		Set<String> expectedTables = Set.of("users", "orders");

		when(aiService.call(anyString())).thenReturn("[\"users\", \"orders\"]");

		// 执行测试
		Set<String> result = baseNl2SqlService.fineSelect(schemaDTO, advice);

		// 验证结果
		assertNotNull(result);
		assertEquals(expectedTables.size(), result.size());
		assertTrue(result.contains("users"));
		assertTrue(result.contains("orders"));

		// 验证方法调用
		verify(aiService, times(1)).call(anyString());

		log.info("FineSelect with advice test passed with {} tables", result.size());
	}

	@Test
	void testFineSelectWithQuery() {
		log.info("Testing fineSelect method with query");

		SchemaDTO schemaDTO = createMockSchemaDTO();
		String query = "查询用户信息";
		List<String> evidenceList = Arrays.asList("evidence1", "evidence2");

		when(aiService.call(anyString())).thenReturn("[\"users\"]");

		// 执行测试
		SchemaDTO result = baseNl2SqlService.fineSelect(schemaDTO, query, evidenceList);

		// 验证结果
		assertNotNull(result);

		// 验证方法调用
		verify(aiService, times(1)).call(anyString());

		log.info("FineSelect with query test passed");
	}

	// Helper methods for creating mock objects
	private List<Document> createMockDocuments(List<String> texts) {
		List<Document> documents = new ArrayList<>();
		for (int i = 0; i < texts.size(); i++) {
			Map<String, Object> metadata = new HashMap<>();
			metadata.put("id", "doc_" + i);
			documents.add(new Document("doc_" + i, texts.get(i), metadata));
		}
		return documents;
	}

	private SchemaDTO createMockSchemaDTO() {
		SchemaDTO schemaDTO = new SchemaDTO();
		schemaDTO.setName("test_db");

		List<TableDTO> tables = new ArrayList<>();

		// 创建用户表
		TableDTO userTable = new TableDTO();
		userTable.setName("users");
		userTable.setDescription("用户表");

		List<ColumnDTO> userColumns = new ArrayList<>();

		ColumnDTO idColumn = new ColumnDTO();
		idColumn.setName("id");
		idColumn.setType("INT");
		idColumn.setDescription("用户ID");
		userColumns.add(idColumn);

		ColumnDTO nameColumn = new ColumnDTO();
		nameColumn.setName("name");
		nameColumn.setType("VARCHAR");
		nameColumn.setDescription("用户姓名");
		userColumns.add(nameColumn);

		ColumnDTO ageColumn = new ColumnDTO();
		ageColumn.setName("age");
		ageColumn.setType("INT");
		ageColumn.setDescription("用户年龄");
		userColumns.add(ageColumn);

		userTable.setColumn(userColumns);
		tables.add(userTable);

		schemaDTO.setTable(tables);
		return schemaDTO;
	}

}
