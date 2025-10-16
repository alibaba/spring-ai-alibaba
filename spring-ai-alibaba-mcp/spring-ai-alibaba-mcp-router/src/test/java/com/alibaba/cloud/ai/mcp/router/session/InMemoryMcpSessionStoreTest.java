/*
 * Copyright 2025-2026 the original author or authors.
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

package com.alibaba.cloud.ai.mcp.router.session;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Libres-coder
 * @since 2025.10.16
 */
class InMemoryMcpSessionStoreTest {

	private McpSessionStore sessionStore;

	@BeforeEach
	void setUp() {
		sessionStore = new InMemoryMcpSessionStore();
	}

	@Test
	void testPutAndGet() {
		String serviceName = "test-service";
		String sessionData = "test-data";

		sessionStore.put(serviceName, sessionData);
		String result = sessionStore.get(serviceName);

		assertEquals(sessionData, result);
	}

	@Test
	void testGetNonExistent() {
		String result = sessionStore.get("non-existent");
		assertNull(result);
	}

	@Test
	void testRemove() {
		String serviceName = "test-service";
		String sessionData = "test-data";

		sessionStore.put(serviceName, sessionData);
		sessionStore.remove(serviceName);

		assertFalse(sessionStore.contains(serviceName));
		assertNull(sessionStore.get(serviceName));
	}

	@Test
	void testContains() {
		String serviceName = "test-service";
		assertFalse(sessionStore.contains(serviceName));

		sessionStore.put(serviceName, "data");
		assertTrue(sessionStore.contains(serviceName));
	}

	@Test
	void testClear() {
		sessionStore.put("service1", "data1");
		sessionStore.put("service2", "data2");
		sessionStore.put("service3", "data3");

		assertEquals(3, sessionStore.size());

		sessionStore.clear();

		assertEquals(0, sessionStore.size());
		assertFalse(sessionStore.contains("service1"));
		assertFalse(sessionStore.contains("service2"));
		assertFalse(sessionStore.contains("service3"));
	}

	@Test
	void testSize() {
		assertEquals(0, sessionStore.size());

		sessionStore.put("service1", "data1");
		assertEquals(1, sessionStore.size());

		sessionStore.put("service2", "data2");
		assertEquals(2, sessionStore.size());

		sessionStore.remove("service1");
		assertEquals(1, sessionStore.size());
	}

	@Test
	void testGetAll() {
		sessionStore.put("service1", "data1");
		sessionStore.put("service2", "data2");
		sessionStore.put("service3", "data3");

		Map<String, Object> allSessions = sessionStore.getAll();

		assertEquals(3, allSessions.size());
		assertEquals("data1", allSessions.get("service1"));
		assertEquals("data2", allSessions.get("service2"));
		assertEquals("data3", allSessions.get("service3"));
	}

	@Test
	void testPutNullServiceName() {
		sessionStore.put(null, "data");
		assertEquals(0, sessionStore.size());
	}

	@Test
	void testGetNullServiceName() {
		Object result = sessionStore.get(null);
		assertNull(result);
	}

	@Test
	void testContainsNullServiceName() {
		assertFalse(sessionStore.contains(null));
	}

	@Test
	void testUpdateExistingSession() {
		String serviceName = "test-service";
		sessionStore.put(serviceName, "data1");
		sessionStore.put(serviceName, "data2");

		assertEquals("data2", sessionStore.get(serviceName));
		assertEquals(1, sessionStore.size());
	}

}

