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
package com.alibaba.cloud.ai.graph.plugin.weather;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test cases for WeatherPlugin.
 */
class WeatherPluginTest {

	private WeatherPlugin weatherPlugin;

	@BeforeEach
	void setUp() {
		weatherPlugin = new WeatherPlugin();
	}

	@Test
	void testGetId() {
		assertEquals("weather", weatherPlugin.getId());
	}

	@Test
	void testGetName() {
		assertEquals("Weather Service", weatherPlugin.getName());
	}

	@Test
	void testGetDescription() {
		assertEquals("Get real-time weather information for a location using WeatherAPI.com",
				weatherPlugin.getDescription());
	}

	@Test
	void testGetInputSchema() {
		Map<String, Object> schema = weatherPlugin.getInputSchema();

		assertNotNull(schema);
		assertEquals("object", schema.get("type"));
		assertArrayEquals(new String[] { "location" }, (String[]) schema.get("required"));

		Map<String, Object> properties = (Map<String, Object>) schema.get("properties");
		assertNotNull(properties);

		Map<String, Object> location = (Map<String, Object>) properties.get("location");
		assertNotNull(location);
		assertEquals("string", location.get("type"));
		assertNotNull(location.get("description"));
	}

	@Test
	void testExecuteWithNullLocation() {
		Map<String, Object> params = new HashMap<>();
		params.put("location", null);

		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
			weatherPlugin.execute(params);
		});

		assertEquals("Location parameter is required", exception.getMessage());
	}

	@Test
	void testExecuteWithEmptyLocation() {
		Map<String, Object> params = new HashMap<>();
		params.put("location", "");

		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
			weatherPlugin.execute(params);
		});

		assertEquals("Location parameter is required", exception.getMessage());
	}

	// Note: Actual API tests would require a valid API key and network connection
	// Those should be integration tests, not unit tests

}