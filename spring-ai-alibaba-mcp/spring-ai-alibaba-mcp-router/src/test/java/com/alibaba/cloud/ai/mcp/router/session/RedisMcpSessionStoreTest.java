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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * @author Libres-coder
 * @since 2025.10.16
 */
@ExtendWith(MockitoExtension.class)
class RedisMcpSessionStoreTest {

	@Mock
	private StringRedisTemplate stringRedisTemplate;

	@Mock
	private ValueOperations<String, String> valueOperations;

	private ObjectMapper objectMapper;

	private McpSessionStore sessionStore;

	private static final String KEY_PREFIX = "test:session:";

	private static final long TTL_SECONDS = 600;

	@BeforeEach
	void setUp() {
		objectMapper = new ObjectMapper();
		sessionStore = new RedisMcpSessionStore(stringRedisTemplate, objectMapper, KEY_PREFIX, TTL_SECONDS);
	}

	@Test
	void testPut() throws Exception {
		String serviceName = "test-service";
		String sessionData = "test-data";
		String key = KEY_PREFIX + serviceName;
		String value = objectMapper.writeValueAsString(sessionData);

		when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
		sessionStore.put(serviceName, sessionData);

		verify(valueOperations).set(key, value, TTL_SECONDS, TimeUnit.SECONDS);
	}

	@Test
	void testGet() throws Exception {
		String serviceName = "test-service";
		String sessionData = "test-data";
		String key = KEY_PREFIX + serviceName;
		String value = objectMapper.writeValueAsString(sessionData);

		when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
		when(valueOperations.get(key)).thenReturn(value);

		Object result = sessionStore.get(serviceName);

		assertNotNull(result);
		verify(valueOperations).get(key);
	}

	@Test
	void testGetNonExistent() {
		String serviceName = "non-existent";
		String key = KEY_PREFIX + serviceName;

		when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
		when(valueOperations.get(key)).thenReturn(null);

		Object result = sessionStore.get(serviceName);

		assertNull(result);
		verify(valueOperations).get(key);
	}

	@Test
	void testRemove() {
		String serviceName = "test-service";
		String key = KEY_PREFIX + serviceName;

		when(stringRedisTemplate.delete(key)).thenReturn(true);

		sessionStore.remove(serviceName);

		verify(stringRedisTemplate).delete(key);
	}

	@Test
	void testContains() {
		String serviceName = "test-service";
		String key = KEY_PREFIX + serviceName;

		when(stringRedisTemplate.hasKey(key)).thenReturn(true);

		boolean result = sessionStore.contains(serviceName);

		assertTrue(result);
		verify(stringRedisTemplate).hasKey(key);
	}

	@Test
	void testClear() {
		String pattern = KEY_PREFIX + "*";
		Set<String> keys = Set.of(KEY_PREFIX + "service1", KEY_PREFIX + "service2", KEY_PREFIX + "service3");

		when(stringRedisTemplate.keys(pattern)).thenReturn(keys);
		when(stringRedisTemplate.delete(keys)).thenReturn(3L);

		sessionStore.clear();

		verify(stringRedisTemplate).keys(pattern);
		verify(stringRedisTemplate).delete(keys);
	}

	@Test
	void testSize() {
		String pattern = KEY_PREFIX + "*";
		Set<String> keys = Set.of(KEY_PREFIX + "service1", KEY_PREFIX + "service2");

		when(stringRedisTemplate.keys(pattern)).thenReturn(keys);

		int size = sessionStore.size();

		assertEquals(2, size);
		verify(stringRedisTemplate).keys(pattern);
	}

	@Test
	void testPutNullServiceName() {
		sessionStore.put(null, "data");

		verify(valueOperations, never()).set(anyString(), anyString(), anyLong(), any(TimeUnit.class));
	}

	@Test
	void testGetNullServiceName() {
		Object result = sessionStore.get(null);

		assertNull(result);
		verify(valueOperations, never()).get(anyString());
	}

	@Test
	void testContainsNullServiceName() {
		boolean result = sessionStore.contains(null);

		assertFalse(result);
		verify(stringRedisTemplate, never()).hasKey(anyString());
	}

}

