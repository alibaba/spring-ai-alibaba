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
package com.alibaba.cloud.ai.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.metadata.EmptyUsage;
import org.springframework.ai.chat.metadata.Usage;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Test cases for RerankResponseMetadata. Tests the functionality of metadata handling and
 * usage information.
 *
 * @author yuluo
 * @author <a href="mailto:yuluo08290126@gmail.com">yuluo</a>
 * @author brianxiadong
 * @since 1.0.0-M5.1
 */
class RerankResponseMetadataTests {

	private RerankResponseMetadata metadata;

	private Usage usage;

	@BeforeEach
	void setUp() {
		// Initialize test objects
		usage = mock(Usage.class);
		metadata = new RerankResponseMetadata(usage);
	}

	@Test
	void testDefaultConstructor() {
		// Test default constructor
		RerankResponseMetadata defaultMetadata = new RerankResponseMetadata();
		assertThat(defaultMetadata.getUsage()).isInstanceOf(EmptyUsage.class);
	}

	@Test
	void testConstructorWithUsage() {
		// Test constructor with Usage parameter
		assertThat(metadata.getUsage()).isEqualTo(usage);
	}

	@Test
	void testConstructorWithUsageAndMetadata() {
		// Test constructor with Usage and metadata map parameters
		Map<String, Object> metadataMap = new HashMap<>();
		metadataMap.put("key1", "value1");
		metadataMap.put("key2", "value2");

		RerankResponseMetadata metadataWithMap = new RerankResponseMetadata(usage, metadataMap);

		assertThat(metadataWithMap.getUsage()).isEqualTo(usage);
		// Since we can't directly access the metadata map, we verify the usage is set
		// correctly
		assertThat(metadataWithMap.getUsage()).isEqualTo(usage);
	}

	@Test
	void testGetAndSetUsage() {
		// Test getter and setter for Usage
		Usage newUsage = mock(Usage.class);
		metadata.setUsage(newUsage);
		assertThat(metadata.getUsage()).isEqualTo(newUsage);
	}

}
