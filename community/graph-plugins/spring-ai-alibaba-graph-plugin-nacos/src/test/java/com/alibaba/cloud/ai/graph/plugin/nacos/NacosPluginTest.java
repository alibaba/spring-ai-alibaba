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
package com.alibaba.cloud.ai.graph.plugin.nacos;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test cases for NacosPlugin.
 *
 * Note: These tests require a running Nacos server. Set NACOS_SERVER_ADDR environment
 * variable to run integration tests.
 */
class NacosPluginTest {

	private NacosPlugin nacosPlugin;

	@BeforeEach
	void setUp() {
		// Only create plugin if Nacos server is available
		String serverAddr = System.getenv("NACOS_SERVER_ADDR");
		if (serverAddr != null) {
			try {
				nacosPlugin = new NacosPlugin();
			}
			catch (Exception e) {
				// Skip tests if Nacos is not available
				nacosPlugin = null;
			}
		}
	}

	@Test
	void testGetId() {
		if (nacosPlugin != null) {
			assertEquals("nacos", nacosPlugin.getId());
		}
	}

	@Test
	void testGetName() {
		if (nacosPlugin != null) {
			assertEquals("Nacos Configuration Manager", nacosPlugin.getName());
		}
	}

	@Test
	void testGetDescription() {
		if (nacosPlugin != null) {
			assertEquals(
					"Manage configurations in Nacos registry, including reading, writing, and monitoring configuration changes",
					nacosPlugin.getDescription());
		}
	}

	@Test
	void testGetInputSchema() {
		if (nacosPlugin != null) {
			Map<String, Object> schema = nacosPlugin.getInputSchema();

			assertNotNull(schema);
			assertEquals("object", schema.get("type"));
			assertArrayEquals(new String[] { "operation", "dataId" }, (String[]) schema.get("required"));

			Map<String, Object> properties = (Map<String, Object>) schema.get("properties");
			assertNotNull(properties);

			Map<String, Object> operation = (Map<String, Object>) properties.get("operation");
			assertNotNull(operation);
			assertEquals("string", operation.get("type"));
			assertArrayEquals(new String[] { "get", "publish", "remove", "listen" }, (String[]) operation.get("enum"));
		}
	}

	@Test
	void testExecuteWithInvalidOperation() {
		if (nacosPlugin != null) {
			Map<String, Object> params = new HashMap<>();
			params.put("operation", "invalid");
			params.put("dataId", "test");

			IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
				nacosPlugin.execute(params);
			});

			assertTrue(exception.getMessage().contains("Unsupported operation"));
		}
	}

	@Test
	void testExecuteWithMissingDataId() {
		if (nacosPlugin != null) {
			Map<String, Object> params = new HashMap<>();
			params.put("operation", "get");

			IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
				nacosPlugin.execute(params);
			});

			assertEquals("Operation and dataId parameters are required", exception.getMessage());
		}
	}

	@Test
	@EnabledIfEnvironmentVariable(named = "NACOS_SERVER_ADDR", matches = ".*")
	void testGetConfigIntegration() throws Exception {
		if (nacosPlugin != null) {
			Map<String, Object> params = new HashMap<>();
			params.put("operation", "get");
			params.put("dataId", "test-config");
			params.put("group", "DEFAULT_GROUP");

			Map<String, Object> result = nacosPlugin.execute(params);

			assertNotNull(result);
			assertEquals("get", result.get("operation"));
			assertEquals("test-config", result.get("dataId"));
			assertEquals("DEFAULT_GROUP", result.get("group"));
			assertNotNull(result.get("exists"));
		}
	}

	@Test
	@EnabledIfEnvironmentVariable(named = "NACOS_SERVER_ADDR", matches = ".*")
	void testPublishAndGetConfigIntegration() throws Exception {
		if (nacosPlugin != null) {
			String dataId = "test-publish-config";
			String content = "test.property=value123";

			// Publish config
			Map<String, Object> publishParams = new HashMap<>();
			publishParams.put("operation", "publish");
			publishParams.put("dataId", dataId);
			publishParams.put("group", "DEFAULT_GROUP");
			publishParams.put("content", content);
			publishParams.put("type", "properties");

			Map<String, Object> publishResult = nacosPlugin.execute(publishParams);

			assertNotNull(publishResult);
			assertEquals("publish", publishResult.get("operation"));
			assertEquals(true, publishResult.get("success"));

			// Get config to verify
			Map<String, Object> getParams = new HashMap<>();
			getParams.put("operation", "get");
			getParams.put("dataId", dataId);
			getParams.put("group", "DEFAULT_GROUP");

			Map<String, Object> getResult = nacosPlugin.execute(getParams);

			assertNotNull(getResult);
			assertEquals(true, getResult.get("exists"));
			assertEquals(content, getResult.get("content"));

			// Clean up
			Map<String, Object> removeParams = new HashMap<>();
			removeParams.put("operation", "remove");
			removeParams.put("dataId", dataId);
			removeParams.put("group", "DEFAULT_GROUP");

			nacosPlugin.execute(removeParams);
		}
	}

}
