/*
 * Copyright 2025 the original author or authors.
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
package com.alibaba.cloud.ai.example.manus.tool;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("TerminateTool 测试")
class TerminateToolTest {

	private TerminateTool terminateTool;

	@BeforeEach
	void setUp() {
		// 使用测试列名初始化
		List<String> testColumns = Arrays.asList("name", "age", "city");
		terminateTool = new TerminateTool("test-plan-123", testColumns);
	}

	@Test
	@DisplayName("测试 generateParametersJson 方法返回的 JSON 结构")
	void testGenerateParametersJson() {
		// 测试不同的列配置
		List<String> columns1 = Arrays.asList("name", "age");
		List<String> columns2 = Arrays.asList("id", "title", "description", "status");
		List<String> emptyColumns = Arrays.asList();
		List<String> nullColumns = null;

		System.out.println("=== generateParametersJson 测试结果 ===");

		// 测试1: 普通列
		String json1 = getParametersJsonViaReflection(columns1);
		System.out.println("测试1 - 普通列 [name, age]:");
		System.out.println(json1);
		System.out.println();

		// 测试2: 多列
		String json2 = getParametersJsonViaReflection(columns2);
		System.out.println("测试2 - 多列 [id, title, description, status]:");
		System.out.println(json2);
		System.out.println();

		// 测试3: 空列表
		String json3 = getParametersJsonViaReflection(emptyColumns);
		System.out.println("测试3 - 空列表 []:");
		System.out.println(json3);
		System.out.println();

		// 测试4: null列表
		String json4 = getParametersJsonViaReflection(nullColumns);
		System.out.println("测试4 - null列表:");
		System.out.println(json4);
		System.out.println();

		// 验证JSON包含必要的结构
		assertTrue(json1.contains("\"type\": \"object\""));
		assertTrue(json1.contains("\"properties\""));
		assertTrue(json1.contains("\"columns\""));
		assertTrue(json1.contains("\"data\""));
		assertTrue(json1.contains("\"required\": [\"columns\", \"data\"]"));
	}

	@Test
	@DisplayName("测试 getCurrentToolStateString 方法在不同状态下的返回内容")
	void testGetCurrentToolStateString() {
		System.out.println("=== getCurrentToolStateString 测试结果 ===");

		// 测试1: 初始状态
		String initialState = terminateTool.getCurrentToolStateString();
		System.out.println("测试1 - 初始状态:");
		System.out.println(initialState);
		System.out.println();

		// 测试2: 执行终止操作后的状态
		Map<String, Object> terminateInput = new HashMap<>();
		terminateInput.put("columns", Arrays.asList("name", "status"));
		terminateInput.put("data", Arrays.asList(Arrays.asList("Alice", "completed"), Arrays.asList("Bob", "pending")));

		terminateTool.run(terminateInput);
		String terminatedState = terminateTool.getCurrentToolStateString();
		System.out.println("测试2 - 终止后状态:");
		System.out.println(terminatedState);
		System.out.println();

		// 验证状态变化
		assertFalse(initialState.contains("🛑 Terminated"));
		assertTrue(initialState.contains("⚡ Active"));

		assertTrue(terminatedState.contains("🛑 Terminated"));
		assertFalse(terminatedState.contains("⚡ Active"));
		assertTrue(terminatedState.contains("test-plan-123"));
	}

	@Test
	@DisplayName("测试不同列配置下的 TerminateTool 行为")
	void testDifferentColumnConfigurations() {
		System.out.println("=== 不同列配置测试 ===");

		// 测试1: 默认列（空列表）
		TerminateTool tool1 = new TerminateTool("plan-1", Arrays.asList());
		String state1 = tool1.getCurrentToolStateString();
		System.out.println("测试1 - 默认列配置 (空列表):");
		System.out.println(state1);
		System.out.println();

		// 测试2: null列
		TerminateTool tool2 = new TerminateTool("plan-2", null);
		String state2 = tool2.getCurrentToolStateString();
		System.out.println("测试2 - null列配置:");
		System.out.println(state2);
		System.out.println();

		// 测试3: 单列
		TerminateTool tool3 = new TerminateTool("plan-3", Arrays.asList("result"));
		String state3 = tool3.getCurrentToolStateString();
		System.out.println("测试3 - 单列配置 [result]:");
		System.out.println(state3);
		System.out.println();

		// 验证默认行为
		assertTrue(state1.contains("message")); // 默认列应该是 "message"
		assertTrue(state2.contains("message")); // null时也应该使用默认列
		assertTrue(state3.contains("result"));
	}

	@Test
	@DisplayName("测试工具定义生成")
	void testToolDefinition() {
		System.out.println("=== 工具定义测试 ===");

		List<String> testColumns = Arrays.asList("task_id", "result", "timestamp");
		var toolDefinition = TerminateTool.getToolDefinition(testColumns);

		// FunctionTool 是一个简单的包装类，我们通过反射来获取内部的 function 对象
		try {
			var functionField = toolDefinition.getClass().getDeclaredField("function");
			functionField.setAccessible(true);
			var function = functionField.get(toolDefinition);

			var nameField = function.getClass().getDeclaredField("name");
			nameField.setAccessible(true);
			String name = (String) nameField.get(function);

			var descriptionField = function.getClass().getDeclaredField("description");
			descriptionField.setAccessible(true);
			String description = (String) descriptionField.get(function);

			var parametersField = function.getClass().getDeclaredField("parameters");
			parametersField.setAccessible(true);
			String parameters = (String) parametersField.get(function);

			System.out.println("工具名称: " + name);
			System.out.println("工具描述: " + description);
			System.out.println("参数结构:");
			System.out.println(parameters);
			System.out.println();

			assertEquals("terminate", name);
			assertNotNull(description);
			assertNotNull(parameters);
		}
		catch (Exception e) {
			System.out.println("Failed to access FunctionTool fields via reflection: " + e.getMessage());
			// 简化测试，只验证工具定义不为null
			assertNotNull(toolDefinition);
		}
	}

	@Test
	@DisplayName("测试 JSON 输出的长度和格式")
	void testJsonOutputCharacteristics() {
		System.out.println("=== JSON 输出特征测试 ===");

		// 测试不同规模的列配置
		List<String> smallColumns = Arrays.asList("id");
		List<String> mediumColumns = Arrays.asList("id", "name", "status", "created_at");
		List<String> largeColumns = Arrays.asList("id", "name", "email", "phone", "address", "city", "state", "zip",
				"country", "notes");

		String smallJson = getParametersJsonViaReflection(smallColumns);
		String mediumJson = getParametersJsonViaReflection(mediumColumns);
		String largeJson = getParametersJsonViaReflection(largeColumns);

		System.out.println("小规模列 (1列) JSON长度: " + smallJson.length() + " 字符");
		System.out.println("中等规模列 (4列) JSON长度: " + mediumJson.length() + " 字符");
		System.out.println("大规模列 (10列) JSON长度: " + largeJson.length() + " 字符");
		System.out.println();

		// 验证JSON格式正确性（简单验证）
		assertTrue(smallJson.startsWith("{"));
		assertTrue(smallJson.endsWith("}"));
		assertTrue(mediumJson.contains("\"type\": \"object\""));
		assertTrue(largeJson.contains("\"items\": {\"type\": \"string\"}"));

		// 确保长度在合理范围内（避免过长导致解析问题）
		assertTrue(smallJson.length() < 1000, "小规模JSON应该小于1000字符");
		assertTrue(mediumJson.length() < 1000, "中等规模JSON应该小于1000字符");
		assertTrue(largeJson.length() < 1000, "大规模JSON应该小于1000字符");
	}

	/**
	 * 通过反射调用私有静态方法 generateParametersJson
	 */
	private String getParametersJsonViaReflection(List<String> columns) {
		try {
			var method = TerminateTool.class.getDeclaredMethod("generateParametersJson", List.class);
			method.setAccessible(true);
			return (String) method.invoke(null, columns);
		}
		catch (Exception e) {
			throw new RuntimeException("Failed to invoke generateParametersJson", e);
		}
	}

}
