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
package com.alibaba.cloud.ai.graph.agent.interceptor.toolsearch;

import org.apache.lucene.analysis.core.SimpleAnalyzer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;


class LuceneToolSearcherTest {

	private LuceneToolSearcher toolSearcher;

	private List<ToolCallback> testTools;

	@BeforeEach
	void setUp() {
		toolSearcher = new LuceneToolSearcher();
		testTools = createTestTools();
		toolSearcher.indexTools(testTools);
	}

	@Test
	void testSearchByName() {
		List<ToolCallback> results = toolSearcher.search("weather", 10);

		assertNotNull(results);
		assertFalse(results.isEmpty());
		assertTrue(results.stream().anyMatch(tool -> tool.getToolDefinition().name().contains("weather")));
	}

	@Test
	void testSearchByDescription() {
		List<ToolCallback> results = toolSearcher.search("database", 10);

		assertNotNull(results);
		assertFalse(results.isEmpty());
		assertTrue(results.stream()
			.anyMatch(tool -> tool.getToolDefinition().description().contains("database")));
	}

	@Test
	void testSearchNoResults() {
		List<ToolCallback> results = toolSearcher.search("nonexistent_xyz", 10);

		assertNotNull(results);
		assertTrue(results.isEmpty());
	}

	@Test
	void testSearchWithMaxResults() {
		List<ToolCallback> results = toolSearcher.search("get", 2);

		assertNotNull(results);
		assertTrue(results.size() <= 2);
	}

	@Test
	void testGetToolSchema() {
		ToolCallback tool = testTools.get(0);
		String schema = toolSearcher.getToolSchema(tool);

		assertNotNull(schema);
		assertFalse(schema.isEmpty());
		assertTrue(schema.contains("\"type\":\"function\""));
		assertTrue(schema.contains("\"name\":\"" + tool.getToolDefinition().name() + "\""));
	}

	@Test
	void testSearchRelevance() {
		List<ToolCallback> results = toolSearcher.search("weather", 10);

		assertNotNull(results);
		assertFalse(results.isEmpty());
		assertEquals("get_weather", results.get(0).getToolDefinition().name());
	}

	@Test
	void testBuilderWithCustomAnalyzer() {
		LuceneToolSearcher customSearcher = LuceneToolSearcher.builder()
			.analyzer(new SimpleAnalyzer())
			.build();

		customSearcher.indexTools(testTools);
		List<ToolCallback> results = customSearcher.search("weather", 10);

		assertNotNull(results);
		assertFalse(results.isEmpty());
	}

	@Test
	void testBuilderWithCustomBoosts() {
		Map<String, Float> customBoosts = new HashMap<>();
		customBoosts.put("name", 1.0f);
		customBoosts.put("description", 5.0f);
		customBoosts.put("parameters", 1.0f);

		LuceneToolSearcher customSearcher = LuceneToolSearcher.builder()
			.fieldBoosts(customBoosts)
			.build();

		customSearcher.indexTools(testTools);
		List<ToolCallback> results = customSearcher.search("database", 10);

		assertNotNull(results);
		assertFalse(results.isEmpty());
		assertTrue(results.get(0).getToolDefinition().description().contains("database"));
	}

	@Test
	void testBuilderClearAndCustomFields() {
		LuceneToolSearcher customSearcher = LuceneToolSearcher.builder()
			.clearIndexFields()
			.addIndexField("name", 5.0f)
			.addIndexField("description", 3.0f)
			.build();

		customSearcher.indexTools(testTools);
		List<ToolCallback> results = customSearcher.search("weather", 10);

		assertNotNull(results);
		assertFalse(results.isEmpty());
	}

	@Test
	void testBuilderValidation() {
		assertThrows(IllegalStateException.class, () -> {
			LuceneToolSearcher.builder()
				.clearIndexFields()
				.build();
		});
	}


	private List<ToolCallback> createTestTools() {
		List<ToolCallback> tools = new ArrayList<>();

		// 天气查询工具
		tools.add(FunctionToolCallback.builder("get_weather", (Function<WeatherRequest, String>) request -> {
			return "Weather in " + request.city + ": Sunny, 25°C";
		}).description("Get current weather information for a specific city").inputType(WeatherRequest.class).build());

		// 数据库查询工具
		tools.add(FunctionToolCallback.builder("query_database", (Function<DatabaseRequest, String>) request -> {
			return "Query result: " + request.sql;
		}).description("Execute SQL queries against the database").inputType(DatabaseRequest.class).build());

		// 文件操作工具
		tools.add(FunctionToolCallback.builder("read_file", (Function<FileRequest, String>) request -> {
			return "File content: " + request.path;
		}).description("Read file content from filesystem").inputType(FileRequest.class).build());

		// 计算工具
		tools.add(FunctionToolCallback.builder("calculate", (Function<CalculateRequest, String>) request -> {
			return "Result: " + (request.a + request.b);
		}).description("Perform arithmetic calculations").inputType(CalculateRequest.class).build());

		// 邮件发送工具
		tools.add(FunctionToolCallback.builder("send_email", (Function<EmailRequest, String>) request -> {
			return "Email sent to: " + request.to;
		}).description("Send email to specified recipients").inputType(EmailRequest.class).build());

		return tools;
	}

	// 测试用的请求类

	public record WeatherRequest(String city) {
	}

	public record DatabaseRequest(String sql) {
	}

	public record FileRequest(String path) {
	}

	public record CalculateRequest(int a, int b) {
	}

	public record EmailRequest(String to, String subject, String body) {
	}

}
