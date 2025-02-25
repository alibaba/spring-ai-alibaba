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
package com.alibaba.cloud.ai.dashscope.common;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test cases for RequestIdGenerator. Tests include: 1. Verify generated ID follows UUID
 * format 2. Verify uniqueness of generated IDs 3. Test ID generation with different
 * parameters
 *
 * @author yuluo
 * @author <a href="mailto:yuluo08290126@gmail.com">yuluo</a>
 * @author brianxiadong
 * @since 1.0.0-M5.1
 */
class RequestIdGeneratorTests {

	private RequestIdGenerator generator;

	@BeforeEach
	void setUp() {
		// Initialize RequestIdGenerator instance
		generator = new RequestIdGenerator();
	}

	@Test
	void testGenerateIdFormat() {
		// Test if generated ID follows UUID format
		String id = generator.generateId();

		// Verify ID is not null
		assertThat(id).isNotNull();

		// Verify ID length is 36 (standard UUID string length)
		assertThat(id).hasSize(36);

		// Verify ID can be parsed as valid UUID
		UUID uuid = UUID.fromString(id);
		assertThat(uuid).isNotNull();
	}

	@Test
	void testGenerateIdUniqueness() {
		// Test uniqueness of multiple generated IDs
		int count = 1000;
		Set<String> ids = new HashSet<>();

		// Generate multiple IDs
		for (int i = 0; i < count; i++) {
			ids.add(generator.generateId());
		}

		// Verify all generated IDs are unique
		assertThat(ids).hasSize(count);
	}

	@Test
	void testGenerateIdWithParameters() {
		// Test ID generation with different parameters
		String id1 = generator.generateId("test");
		String id2 = generator.generateId("test", 123);
		String id3 = generator.generateId();

		// Verify IDs generated with different parameters are unique
		assertThat(id1).isNotEqualTo(id2).isNotEqualTo(id3);

		// Verify all generated IDs are valid UUIDs
		assertThat(UUID.fromString(id1)).isNotNull();
		assertThat(UUID.fromString(id2)).isNotNull();
		assertThat(UUID.fromString(id3)).isNotNull();
	}

	@Test
	void testGenerateIdWithNullParameters() {
		// Test ID generation with null parameters
		String id1 = generator.generateId(null);
		String id2 = generator.generateId(null, null);

		// Verify IDs generated with null parameters are not null
		assertThat(id1).isNotNull();
		assertThat(id2).isNotNull();

		// Verify generated IDs are unique
		assertThat(id1).isNotEqualTo(id2);

		// Verify generated IDs are valid UUIDs
		assertThat(UUID.fromString(id1)).isNotNull();
		assertThat(UUID.fromString(id2)).isNotNull();
	}

}
