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
package com.alibaba.cloud.ai.dashscope.rag;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test cases for DashScopeDocumentTransformerOptions. Tests cover default values, builder
 * pattern, setters/getters, and partial value settings.
 *
 * @author yuluo
 * @author <a href="mailto:yuluo08290126@gmail.com">yuluo</a>
 * @author brianxiadong
 * @since 1.0.0-M5.1
 */
class DashScopeDocumentTransformerOptionsTests {

	// Test constants for validation
	private static final int TEST_CHUNK_SIZE = 1000;

	private static final int TEST_OVERLAP_SIZE = 200;

	private static final String TEST_SEPARATOR = "|,|，|。|？|！|\\n";

	private static final String TEST_FILE_TYPE = "txt";

	private static final String TEST_LANGUAGE = "en";

	/**
	 * Test default values when creating a new instance. Verifies that all properties have
	 * their expected default values.
	 */
	@Test
	void testDefaultValues() {
		DashScopeDocumentTransformerOptions options = new DashScopeDocumentTransformerOptions();

		// Verify default values
		assertThat(options.getChunkSize()).isEqualTo(500);
		assertThat(options.getOverlapSize()).isEqualTo(100);
		assertThat(options.getSeparator()).isEqualTo("|,|，|。|？|！|\\n|\\\\?|\\\\!");
		assertThat(options.getFileType()).isEqualTo("idp");
		assertThat(options.getLanguage()).isEqualTo("cn");
	}

	/**
	 * Test the builder pattern for setting all properties. Verifies that all properties
	 * can be set using the builder pattern.
	 */
	@Test
	void testBuilderPattern() {
		DashScopeDocumentTransformerOptions options = DashScopeDocumentTransformerOptions.builder()
			.withChunkSize(TEST_CHUNK_SIZE)
			.withOverlapSize(TEST_OVERLAP_SIZE)
			.withSeparator(TEST_SEPARATOR)
			.withFileType(TEST_FILE_TYPE)
			.withLanguage(TEST_LANGUAGE)
			.build();

		// Verify all properties are set correctly
		assertThat(options.getChunkSize()).isEqualTo(TEST_CHUNK_SIZE);
		assertThat(options.getOverlapSize()).isEqualTo(TEST_OVERLAP_SIZE);
		assertThat(options.getSeparator()).isEqualTo(TEST_SEPARATOR);
		assertThat(options.getFileType()).isEqualTo(TEST_FILE_TYPE);
		assertThat(options.getLanguage()).isEqualTo(TEST_LANGUAGE);
	}

	/**
	 * Test all setter and getter methods. Verifies that all properties can be set and
	 * retrieved correctly using setters and getters.
	 */
	@Test
	void testSettersAndGetters() {
		DashScopeDocumentTransformerOptions options = new DashScopeDocumentTransformerOptions();

		// Set values using setters
		options.setChunkSize(TEST_CHUNK_SIZE);
		options.setOverlapSize(TEST_OVERLAP_SIZE);
		options.setSeparator(TEST_SEPARATOR);
		options.setFileType(TEST_FILE_TYPE);
		options.setLanguage(TEST_LANGUAGE);

		// Verify values using getters
		assertThat(options.getChunkSize()).isEqualTo(TEST_CHUNK_SIZE);
		assertThat(options.getOverlapSize()).isEqualTo(TEST_OVERLAP_SIZE);
		assertThat(options.getSeparator()).isEqualTo(TEST_SEPARATOR);
		assertThat(options.getFileType()).isEqualTo(TEST_FILE_TYPE);
		assertThat(options.getLanguage()).isEqualTo(TEST_LANGUAGE);
	}

	/**
	 * Test builder with partial values set. Verifies that when only some properties are
	 * set, others retain their default values.
	 */
	@Test
	void testBuilderWithPartialValues() {
		DashScopeDocumentTransformerOptions options = DashScopeDocumentTransformerOptions.builder()
			.withChunkSize(TEST_CHUNK_SIZE)
			.withLanguage(TEST_LANGUAGE)
			.build();

		// Verify set values
		assertThat(options.getChunkSize()).isEqualTo(TEST_CHUNK_SIZE);
		assertThat(options.getLanguage()).isEqualTo(TEST_LANGUAGE);

		// Verify default values for unset properties
		assertThat(options.getOverlapSize()).isEqualTo(100);
		assertThat(options.getSeparator()).isEqualTo("|,|，|。|？|！|\\n|\\\\?|\\\\!");
		assertThat(options.getFileType()).isEqualTo("idp");
	}

}
