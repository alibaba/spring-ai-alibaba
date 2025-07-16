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
import org.springframework.ai.document.Document;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test cases for RerankRequest. Tests cover constructors, getters, and interface methods.
 *
 * @author yuluo
 * @author <a href="mailto:yuluo08290126@gmail.com">yuluo</a>
 * @author brianxiadong
 * @since 1.0.0-M5.1
 */
class RerankRequestTests {

	// Test constants
	private static final String TEST_QUERY = "test query";

	private static final String TEST_DOC_ID = "test-doc-1";

	private static final String TEST_DOC_TEXT = "This is a test document";

	private List<Document> testDocuments;

	private RerankOptions testOptions;

	@BeforeEach
	void setUp() {
		// Initialize test documents
		testDocuments = new ArrayList<>();
		Map<String, Object> metadata = new HashMap<>();
		metadata.put("id", TEST_DOC_ID);
		testDocuments.add(new Document(TEST_DOC_TEXT, metadata));

		// Initialize test options
		testOptions = new RerankOptions() {
			@Override
			public String getModel() {
				return "test-model";
			}

			@Override
			public Integer getTopN() {
				return 10;
			}
		};
	}

	/**
	 * Test constructor with query and documents. Verifies that a RerankRequest can be
	 * created with just query and documents.
	 */
	@Test
	void testConstructorWithQueryAndDocuments() {
		RerankRequest request = new RerankRequest(TEST_QUERY, testDocuments);

		assertThat(request.getQuery()).isEqualTo(TEST_QUERY);
		assertThat(request.getInstructions()).isEqualTo(testDocuments);
		assertThat(request.getOptions()).isNull();
	}

	/**
	 * Test constructor with query, documents, and options. Verifies that a RerankRequest
	 * can be created with all parameters.
	 */
	@Test
	void testConstructorWithAllParameters() {
		RerankRequest request = new RerankRequest(TEST_QUERY, testDocuments, testOptions);

		assertThat(request.getQuery()).isEqualTo(TEST_QUERY);
		assertThat(request.getInstructions()).isEqualTo(testDocuments);
		assertThat(request.getOptions()).isEqualTo(testOptions);
	}

	/**
	 * Test getInstructions method. Verifies that getInstructions returns the correct
	 * document list.
	 */
	@Test
	void testGetInstructions() {
		RerankRequest request = new RerankRequest(TEST_QUERY, testDocuments);
		assertThat(request.getInstructions()).isEqualTo(testDocuments);
	}

	/**
	 * Test getOptions method. Verifies that getOptions returns the correct options
	 * object.
	 */
	@Test
	void testGetOptions() {
		RerankRequest request = new RerankRequest(TEST_QUERY, testDocuments, testOptions);
		assertThat(request.getOptions()).isEqualTo(testOptions);
	}

	/**
	 * Test getQuery method. Verifies that getQuery returns the correct query string.
	 */
	@Test
	void testGetQuery() {
		RerankRequest request = new RerankRequest(TEST_QUERY, testDocuments);
		assertThat(request.getQuery()).isEqualTo(TEST_QUERY);
	}

}
