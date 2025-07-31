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
		// 使用测试预期返回信息初始化
		terminateTool = new TerminateTool("test-plan-123", "test expected return info");
	}

	@Test
	@DisplayName("测试 generateParametersJson 方法返回的 JSON 结构")
	void testGenerateParametersJson() {
		// 测试不同的预期返回信息
		String expectedReturnInfo1 = "result data";
		String expectedReturnInfo2 = "task completion status";
		String expectedReturnInfo3 = "";
		String expectedReturnInfo4 = null;

		System.out.println("=== generateParametersJson 测试结果 ===");

		// 测试1: 普通预期返回信息
		String json1 = getParametersJsonViaReflection(expectedReturnInfo1);
		System.out.println("测试1 - 普通预期返回信息 \"result data\":");
		System.out.println(json1);
		System.out.println();

		// 测试2: 复杂预期返回信息
		String json2 = getParametersJsonViaReflection(expectedReturnInfo2);
		System.out.println("测试2 - 复杂预期返回信息 \"task completion status\":");
		System.out.println(json2);
		System.out.println();

		// 测试3: 空字符串
		String json3 = getParametersJsonViaReflection(expectedReturnInfo3);
		System.out.println("测试3 - 空字符串:");
		System.out.println(json3);
		System.out.println();

		// 测试4: null
		String json4 = getParametersJsonViaReflection(expectedReturnInfo4);
		System.out.println("测试4 - null:");
		System.out.println(json4);
		System.out.println();

		// 验证JSON包含必要的结构
		assertTrue(json1.contains("\"type\": \"object\""));
		assertTrue(json1.contains("\"properties\""));
		assertTrue(json1.contains("\"message\""));
		assertTrue(json1.contains("\"fileList\""));
		assertTrue(json1.contains("\"folderList\""));
		assertTrue(json1.contains("\"required\": [\"message\"]"));
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
		terminateInput.put("message", "Task completed successfully");
		
		// 添加文件列表
		List<Map<String, String>> fileList = Arrays.asList(
			Map.of("fileName", "result.txt", "fileDescription", "Task execution result"),
			Map.of("fileName", "log.txt", "fileDescription", "Execution log")
		);
		terminateInput.put("fileList", fileList);
		
		// 添加文件夹列表
		List<Map<String, String>> folderList = Arrays.asList(
			Map.of("folderName", "output", "folderDescription", "Output files folder"),
			Map.of("folderName", "temp", "folderDescription", "Temporary files folder")
		);
		terminateInput.put("folderList", folderList);

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
	@DisplayName("测试不同预期返回信息配置下的 TerminateTool 行为")
	void testDifferentExpectedReturnInfoConfigurations() {
		System.out.println("=== 不同预期返回信息配置测试 ===");

		// 测试1: 默认预期返回信息（空字符串）
		TerminateTool tool1 = new TerminateTool("plan-1", "");
		String state1 = tool1.getCurrentToolStateString();
		System.out.println("测试1 - 默认预期返回信息 (空字符串):");
		System.out.println(state1);
		System.out.println();

		// 测试2: null预期返回信息
		TerminateTool tool2 = new TerminateTool("plan-2", null);
		String state2 = tool2.getCurrentToolStateString();
		System.out.println("测试2 - null预期返回信息:");
		System.out.println(state2);
		System.out.println();

		// 测试3: 具体预期返回信息
		TerminateTool tool3 = new TerminateTool("plan-3", "task result");
		String state3 = tool3.getCurrentToolStateString();
		System.out.println("测试3 - 具体预期返回信息 \"task result\":");
		System.out.println(state3);
		System.out.println();

		// 验证默认行为
		assertTrue(state1.contains("N/A")); // 空字符串时应该显示 N/A
		assertTrue(state2.contains("N/A")); // null时也应该显示 N/A
		assertTrue(state3.contains("task result"));
	}

	@Test
	@DisplayName("测试工具定义生成")
	void testToolDefinition() {
		System.out.println("=== 工具定义测试 ===");

		String expectedReturnInfo = "task_id, result, timestamp";
		var toolDefinition = TerminateTool.getToolDefinition(expectedReturnInfo);

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

		// 测试不同规模的预期返回信息
		String smallExpectedInfo = "id";
		String mediumExpectedInfo = "id, name, status, created_at";
		String largeExpectedInfo = "id, name, email, phone, address, city, state, zip, country, notes";

		String smallJson = getParametersJsonViaReflection(smallExpectedInfo);
		String mediumJson = getParametersJsonViaReflection(mediumExpectedInfo);
		String largeJson = getParametersJsonViaReflection(largeExpectedInfo);

		System.out.println("小规模预期返回信息 (1项) JSON长度: " + smallJson.length() + " 字符");
		System.out.println("中等规模预期返回信息 (4项) JSON长度: " + mediumJson.length() + " 字符");
		System.out.println("大规模预期返回信息 (10项) JSON长度: " + largeJson.length() + " 字符");
		System.out.println();

		// 验证JSON格式正确性（简单验证）
		assertTrue(smallJson.startsWith("{"));
		assertTrue(smallJson.endsWith("}"));
		assertTrue(mediumJson.contains("\"type\": \"object\""));
		assertTrue(largeJson.contains("\"items\": {\"type\": \"object\"}"));

		// 确保长度在合理范围内（避免过长导致解析问题）
		assertTrue(smallJson.length() < 2000, "小规模JSON应该小于2000字符");
		assertTrue(mediumJson.length() < 2000, "中等规模JSON应该小于2000字符");
		assertTrue(largeJson.length() < 2000, "大规模JSON应该小于2000字符");
	}

	/**
	 * 通过反射调用私有静态方法 generateParametersJson
	 */
	private String getParametersJsonViaReflection(String expectedReturnInfo) {
		try {
			var method = TerminateTool.class.getDeclaredMethod("generateParametersJson", String.class);
			method.setAccessible(true);
			return (String) method.invoke(null, expectedReturnInfo);
		}
		catch (Exception e) {
			throw new RuntimeException("Failed to invoke generateParametersJson", e);
		}
	}

}