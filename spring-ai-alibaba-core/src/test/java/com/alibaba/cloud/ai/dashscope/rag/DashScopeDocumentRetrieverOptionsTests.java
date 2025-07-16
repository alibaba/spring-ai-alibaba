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
 * Test cases for DashScopeDocumentRetrieverOptions. Tests cover builder pattern,
 * getters/setters, and default values.
 *
 * @author yuluo
 * @author <a href="mailto:yuluo08290126@gmail.com">yuluo</a>
 * @author brianxiadong
 * @since 1.0.0-M5.1
 */
class DashScopeDocumentRetrieverOptionsTests {

	// Test constants
	private static final String TEST_INDEX_NAME = "test-index";

	private static final int TEST_DENSE_TOP_K = 50;

	private static final int TEST_SPARSE_TOP_K = 30;

	private static final String TEST_REWRITE_MODEL = "test-rewrite-model";

	private static final String TEST_RERANK_MODEL = "test-rerank-model";

	private static final float TEST_RERANK_MIN_SCORE = 0.5f;

	private static final int TEST_RERANK_TOP_N = 10;

	@Test
	void testDefaultValues() {
		// Test default constructor and default values
		DashScopeDocumentRetrieverOptions options = new DashScopeDocumentRetrieverOptions();

		// Verify default values
		assertThat(options.getIndexName()).isNull();
		assertThat(options.getDenseSimilarityTopK()).isEqualTo(100);
		assertThat(options.getSparseSimilarityTopK()).isEqualTo(100);
		assertThat(options.isEnableRewrite()).isFalse();
		assertThat(options.getRewriteModelName()).isEqualTo("conv-rewrite-qwen-1.8b");
		assertThat(options.isEnableReranking()).isTrue();
		assertThat(options.getRerankModelName()).isEqualTo("gte-rerank-hybrid");
		assertThat(options.getRerankMinScore()).isEqualTo(0.01f);
		assertThat(options.getRerankTopN()).isEqualTo(5);
	}

	@Test
	void testBuilderPattern() {
		// Test builder pattern with all properties set
		DashScopeDocumentRetrieverOptions options = DashScopeDocumentRetrieverOptions.builder()
			.withIndexName(TEST_INDEX_NAME)
			.withDenseSimilarityTopK(TEST_DENSE_TOP_K)
			.withSparseSimilarityTopK(TEST_SPARSE_TOP_K)
			.withEnableRewrite(true)
			.withRewriteModelName(TEST_REWRITE_MODEL)
			.withEnableReranking(false)
			.withRerankModelName(TEST_RERANK_MODEL)
			.withRerankMinScore(TEST_RERANK_MIN_SCORE)
			.withRerankTopN(TEST_RERANK_TOP_N)
			.build();

		// Verify all properties are set correctly
		assertThat(options.getIndexName()).isEqualTo(TEST_INDEX_NAME);
		assertThat(options.getDenseSimilarityTopK()).isEqualTo(TEST_DENSE_TOP_K);
		assertThat(options.getSparseSimilarityTopK()).isEqualTo(TEST_SPARSE_TOP_K);
		assertThat(options.isEnableRewrite()).isTrue();
		assertThat(options.getRewriteModelName()).isEqualTo(TEST_REWRITE_MODEL);
		assertThat(options.isEnableReranking()).isFalse();
		assertThat(options.getRerankModelName()).isEqualTo(TEST_RERANK_MODEL);
		assertThat(options.getRerankMinScore()).isEqualTo(TEST_RERANK_MIN_SCORE);
		assertThat(options.getRerankTopN()).isEqualTo(TEST_RERANK_TOP_N);
	}

	@Test
	void testSettersAndGetters() {
		// Test setters and getters
		DashScopeDocumentRetrieverOptions options = new DashScopeDocumentRetrieverOptions();

		// Set values using setters
		options.setIndexName(TEST_INDEX_NAME);
		options.setDenseSimilarityTopK(TEST_DENSE_TOP_K);
		options.setSparseSimilarityTopK(TEST_SPARSE_TOP_K);
		options.setEnableRewrite(true);
		options.setRewriteModelName(TEST_REWRITE_MODEL);
		options.setEnableReranking(false);
		options.setRerankModelName(TEST_RERANK_MODEL);
		options.setRerankMinScore(TEST_RERANK_MIN_SCORE);
		options.setRerankTopN(TEST_RERANK_TOP_N);

		// Verify values using getters
		assertThat(options.getIndexName()).isEqualTo(TEST_INDEX_NAME);
		assertThat(options.getDenseSimilarityTopK()).isEqualTo(TEST_DENSE_TOP_K);
		assertThat(options.getSparseSimilarityTopK()).isEqualTo(TEST_SPARSE_TOP_K);
		assertThat(options.isEnableRewrite()).isTrue();
		assertThat(options.getRewriteModelName()).isEqualTo(TEST_REWRITE_MODEL);
		assertThat(options.isEnableReranking()).isFalse();
		assertThat(options.getRerankModelName()).isEqualTo(TEST_RERANK_MODEL);
		assertThat(options.getRerankMinScore()).isEqualTo(TEST_RERANK_MIN_SCORE);
		assertThat(options.getRerankTopN()).isEqualTo(TEST_RERANK_TOP_N);
	}

	@Test
	void testBuilderWithPartialValues() {
		// Test builder with only some properties set
		DashScopeDocumentRetrieverOptions options = DashScopeDocumentRetrieverOptions.builder()
			.withIndexName(TEST_INDEX_NAME)
			.withDenseSimilarityTopK(TEST_DENSE_TOP_K)
			.build();

		// Verify set values
		assertThat(options.getIndexName()).isEqualTo(TEST_INDEX_NAME);
		assertThat(options.getDenseSimilarityTopK()).isEqualTo(TEST_DENSE_TOP_K);

		// Verify unset values remain at defaults
		assertThat(options.getSparseSimilarityTopK()).isEqualTo(100);
		assertThat(options.isEnableRewrite()).isFalse();
		assertThat(options.getRewriteModelName()).isEqualTo("conv-rewrite-qwen-1.8b");
		assertThat(options.isEnableReranking()).isTrue();
		assertThat(options.getRerankModelName()).isEqualTo("gte-rerank-hybrid");
		assertThat(options.getRerankMinScore()).isEqualTo(0.01f);
		assertThat(options.getRerankTopN()).isEqualTo(5);
	}

}
