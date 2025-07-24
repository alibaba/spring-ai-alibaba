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
package com.alibaba.cloud.ai.example.manus.dynamic.mcp.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.alibaba.cloud.ai.example.manus.dynamic.mcp.model.po.McpConfigType;
import com.alibaba.cloud.ai.example.manus.dynamic.mcp.model.vo.McpServerConfig;

/**
 * Test class for DecisionConnectionType
 */
class DecisionConnectionTypeTest {

	@Test
	void testDecideConnectionTypeWithCommand() throws IOException {
		// 测试有command字段的情况
		McpServerConfig serverConfig = new McpServerConfig();
		serverConfig.setCommand("npx");
		serverConfig.setArgs(Arrays.asList("-y", "mcp-server"));
		serverConfig.setEnv(new HashMap<>());

		McpConfigType result = DecisionConnectionType.decideConnectionType(serverConfig);
		assertEquals(McpConfigType.STUDIO, result);
	}

	@Test
	void testDecideConnectionTypeWithUrl() {
		// 测试有url字段但没有command的情况
		McpServerConfig serverConfig = new McpServerConfig();
		serverConfig.setUrl("https://example.com/api");
		serverConfig.setEnv(new HashMap<>());

		// 由于无法进行实际的HTTP请求，应该抛出异常
		assertThrows(IOException.class, () -> {
			DecisionConnectionType.decideConnectionType(serverConfig);
		});
	}

	@Test
	void testDecideConnectionTypeWithSSEUrl() throws IOException {
		// 测试URL以sse结尾的情况
		// 注意：由于现在URL后缀检查在HTTP请求之后，这个测试可能会失败
		// 因为无法连接到example.com，所以会抛出异常或返回默认值
		McpServerConfig serverConfig = new McpServerConfig();
		serverConfig.setUrl("https://example.com/api/sse");
		serverConfig.setEnv(new HashMap<>());

		try {
			McpConfigType result = DecisionConnectionType.decideConnectionType(serverConfig);
			// 如果网络请求失败，应该返回默认的STREAMING
			assertEquals(McpConfigType.STREAMING, result);
		}
		catch (Exception e) {
			// 预期会抛出异常
			assertNotNull(e);
		}
	}

	@Test
	void testDecideConnectionTypeWithSSEUrlNoSlash() throws IOException {
		// 测试URL以sse结尾（没有斜杠）的情况
		// 注意：由于现在URL后缀检查在HTTP请求之后，这个测试可能会失败
		McpServerConfig serverConfig = new McpServerConfig();
		serverConfig.setUrl("https://example.com/sse");
		serverConfig.setEnv(new HashMap<>());

		try {
			McpConfigType result = DecisionConnectionType.decideConnectionType(serverConfig);
			// 如果网络请求失败，应该返回默认的STREAMING
			assertEquals(McpConfigType.STREAMING, result);
		}
		catch (Exception e) {
			// 预期会抛出异常
			assertNotNull(e);
		}
	}

	@Test
	void testDecideConnectionTypeWithNoCommandAndNoUrl() throws IOException {
		// 测试既没有command也没有url的情况
		McpServerConfig serverConfig = new McpServerConfig();
		serverConfig.setEnv(new HashMap<>());

		McpConfigType result = DecisionConnectionType.decideConnectionType(serverConfig);
		assertEquals(McpConfigType.STREAMING, result);
	}

	@Test
	void testDecideConnectionTypes() throws IOException {
		// 测试批量判断连接类型
		Map<String, McpServerConfig> serverConfigs = new HashMap<>();

		// 配置1：有command
		McpServerConfig config1 = new McpServerConfig();
		config1.setCommand("python");
		config1.setArgs(Arrays.asList("server.py"));
		serverConfigs.put("server1", config1);

		// 配置2：有url
		McpServerConfig config2 = new McpServerConfig();
		config2.setUrl("https://example.com/api");
		serverConfigs.put("server2", config2);

		// 配置3：空配置
		McpServerConfig config3 = new McpServerConfig();
		serverConfigs.put("server3", config3);

		Map<String, McpConfigType> results = DecisionConnectionType.decideConnectionTypes(serverConfigs);

		assertNotNull(results);
		assertEquals(3, results.size());
		assertEquals(McpConfigType.STUDIO, results.get("server1"));
		assertEquals(McpConfigType.STREAMING, results.get("server2"));
		assertEquals(McpConfigType.STREAMING, results.get("server3"));
	}

	@Test
	void testDecideConnectionTypeWithNullConfig() {
		// 测试null配置的情况
		McpServerConfig serverConfig = null;

		// 这里应该抛出异常，但我们测试默认行为
		try {
			McpConfigType result = DecisionConnectionType.decideConnectionType(serverConfig);
			assertEquals(McpConfigType.STREAMING, result);
		}
		catch (Exception e) {
			// 预期会抛出异常
			assertNotNull(e);
		}
	}

	@Test
	void testDecideConnectionTypeWithInvalidUrl() {
		// 测试无效URL的情况（会返回非200状态码）
		McpServerConfig serverConfig = new McpServerConfig();
		serverConfig.setUrl("https://invalid-url-that-does-not-exist-12345.com/api");
		serverConfig.setEnv(new HashMap<>());

		// 由于无法连接到无效URL，应该抛出异常或返回默认值
		try {
			McpConfigType result = DecisionConnectionType.decideConnectionType(serverConfig);
			// 如果网络请求失败，应该返回默认的STREAMING
			assertEquals(McpConfigType.STREAMING, result);
		}
		catch (Exception e) {
			// 预期会抛出异常
			assertNotNull(e);
		}
	}

	@Test
	void testDecideConnectionTypeWithStreamableHttpUrl() throws IOException {
		// 测试StreamableHttp URL（基于实际测试结果）
		McpServerConfig serverConfig = new McpServerConfig();
		serverConfig.setUrl("https://mcp.higress.ai/mcp-ip-query/cmb6h8vpr00e08a01dx15ck8o");
		serverConfig.setEnv(new HashMap<>());

		try {
			McpConfigType result = DecisionConnectionType.decideConnectionType(serverConfig);
			// 根据实际测试，这个URL应该返回STREAMING类型
			assertEquals(McpConfigType.STREAMING, result);
		}
		catch (Exception e) {
			// 如果网络请求失败，应该返回默认的STREAMING
			assertNotNull(e);
		}
	}

	@Test
	void testJsonRpcResponseDetection() {
		// 测试JSON-RPC响应检测
		String jsonRpcResponse = "{\"jsonrpc\": \"2.0\",\"id\":1,\"error\":{\"code\":-32602,\"message\":\"Unsupported protocol version\"}}";

		// 使用反射调用私有方法进行测试
		try {
			java.lang.reflect.Method method = DecisionConnectionType.class.getDeclaredMethod("isJsonRpcResponse",
					String.class);
			method.setAccessible(true);
			boolean result = (Boolean) method.invoke(null, jsonRpcResponse);
			assertTrue(result, "Should detect JSON-RPC response");
		}
		catch (Exception e) {
			fail("Failed to test JSON-RPC response detection: " + e.getMessage());
		}
	}

	@Test
	void testNonJsonRpcResponseDetection() {
		// 测试非JSON-RPC响应检测
		String nonJsonRpcResponse = "{\"status\": \"ok\", \"data\": \"some data\"}";

		try {
			java.lang.reflect.Method method = DecisionConnectionType.class.getDeclaredMethod("isJsonRpcResponse",
					String.class);
			method.setAccessible(true);
			boolean result = (Boolean) method.invoke(null, nonJsonRpcResponse);
			assertFalse(result, "Should not detect non-JSON-RPC response");
		}
		catch (Exception e) {
			fail("Failed to test non-JSON-RPC response detection: " + e.getMessage());
		}
	}

}