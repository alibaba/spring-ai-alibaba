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

import com.alibaba.cloud.ai.dashscope.embedding.DashScopeEmbeddingOptions;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test cases for DashScopeStoreOptions. Tests cover constructor, getters/setters, and
 * option configurations.
 *
 * @author yuluo
 * @author <a href="mailto:yuluo08290126@gmail.com">yuluo</a>
 * @author brianxiadong
 * @since 1.0.0-M5.1
 */
class DashScopeStoreOptionsTests {

	// Test constants for validation
	private static final String TEST_INDEX_NAME = "test-index";

	private static final String TEST_EMBEDDING_MODEL = "text-embedding-v2";

	private static final String TEST_RERANK_MODEL = "gte-rerank-hybrid";

	/**
	 * Test constructor with index name. Verifies that the index name is correctly set in
	 * the constructor.
	 */
	@Test
	void testConstructorWithIndexName() {
		DashScopeStoreOptions options = new DashScopeStoreOptions(TEST_INDEX_NAME);

		// Verify index name is set correctly
		assertThat(options.getIndexName()).isEqualTo(TEST_INDEX_NAME);

		// Verify other options are null by default
		assertThat(options.getTransformerOptions()).isNull();
		assertThat(options.getEmbeddingOptions()).isNull();
		assertThat(options.getRetrieverOptions()).isNull();
	}

	/**
	 * Test setters and getters for all options. Verifies that all options can be set and
	 * retrieved correctly.
	 */
	@Test
	void testSettersAndGetters() {
		DashScopeStoreOptions options = new DashScopeStoreOptions(TEST_INDEX_NAME);

		// Create and set transformer options
		DashScopeDocumentTransformerOptions transformerOptions = new DashScopeDocumentTransformerOptions();
		transformerOptions.setChunkSize(1000);
		options.setTransformerOptions(transformerOptions);

		// Create and set embedding options
		DashScopeEmbeddingOptions embeddingOptions = DashScopeEmbeddingOptions.builder()
			.withModel(TEST_EMBEDDING_MODEL)
			.build();
		options.setEmbeddingOptions(embeddingOptions);

		// Create and set retriever options
		DashScopeDocumentRetrieverOptions retrieverOptions = DashScopeDocumentRetrieverOptions.builder()
			.withRerankModelName(TEST_RERANK_MODEL)
			.build();
		options.setRetrieverOptions(retrieverOptions);

		// Verify all options are set correctly
		assertThat(options.getIndexName()).isEqualTo(TEST_INDEX_NAME);
		assertThat(options.getTransformerOptions()).isEqualTo(transformerOptions);
		assertThat(options.getEmbeddingOptions()).isEqualTo(embeddingOptions);
		assertThat(options.getRetrieverOptions()).isEqualTo(retrieverOptions);
	}

	/**
	 * Test setting index name after construction. Verifies that the index name can be
	 * modified after object creation.
	 */
	@Test
	void testSetIndexName() {
		String newIndexName = "new-test-index";
		DashScopeStoreOptions options = new DashScopeStoreOptions(TEST_INDEX_NAME);

		options.setIndexName(newIndexName);

		assertThat(options.getIndexName()).isEqualTo(newIndexName);
	}

	/**
	 * Test setting transformer options with default values. Verifies that transformer
	 * options with default values are handled correctly.
	 */
	@Test
	void testTransformerOptionsWithDefaults() {
		DashScopeStoreOptions options = new DashScopeStoreOptions(TEST_INDEX_NAME);
		DashScopeDocumentTransformerOptions transformerOptions = new DashScopeDocumentTransformerOptions();

		options.setTransformerOptions(transformerOptions);

		// Verify default values
		assertThat(options.getTransformerOptions().getChunkSize()).isEqualTo(500);
		assertThat(options.getTransformerOptions().getOverlapSize()).isEqualTo(100);
		assertThat(options.getTransformerOptions().getLanguage()).isEqualTo("cn");
	}

	/**
	 * Test setting embedding options with custom configuration. Verifies that embedding
	 * options can be customized and retrieved correctly.
	 */
	@Test
	void testEmbeddingOptionsWithCustomConfig() {
		DashScopeStoreOptions options = new DashScopeStoreOptions(TEST_INDEX_NAME);
		DashScopeEmbeddingOptions embeddingOptions = DashScopeEmbeddingOptions.builder()
			.withModel(TEST_EMBEDDING_MODEL)
			.withTextType("document")
			.withDimensions(1536)
			.build();

		options.setEmbeddingOptions(embeddingOptions);

		// Verify custom values
		assertThat(options.getEmbeddingOptions().getModel()).isEqualTo(TEST_EMBEDDING_MODEL);
		assertThat(options.getEmbeddingOptions().getTextType()).isEqualTo("document");
		assertThat(options.getEmbeddingOptions().getDimensions()).isEqualTo(1536);
	}

	/**
	 * Test setting retriever options with custom configuration. Verifies that retriever
	 * options can be customized and retrieved correctly.
	 */
	@Test
	void testRetrieverOptionsWithCustomConfig() {
		DashScopeStoreOptions options = new DashScopeStoreOptions(TEST_INDEX_NAME);
		DashScopeDocumentRetrieverOptions retrieverOptions = DashScopeDocumentRetrieverOptions.builder()
			.withDenseSimilarityTopK(50)
			.withSparseSimilarityTopK(30)
			.withEnableReranking(true)
			.withRerankModelName(TEST_RERANK_MODEL)
			.build();

		options.setRetrieverOptions(retrieverOptions);

		// Verify custom values
		assertThat(options.getRetrieverOptions().getDenseSimilarityTopK()).isEqualTo(50);
		assertThat(options.getRetrieverOptions().getSparseSimilarityTopK()).isEqualTo(30);
		assertThat(options.getRetrieverOptions().isEnableReranking()).isTrue();
		assertThat(options.getRetrieverOptions().getRerankModelName()).isEqualTo(TEST_RERANK_MODEL);
	}

}
