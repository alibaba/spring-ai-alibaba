/*
 * Copyright 2025 the original author or authors.
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

package com.alibaba.cloud.ai.studio.core.model.reranker;

import org.springframework.lang.Nullable;

/**
 * Interface defining options for reranking results.
 *
 * @since 1.0.0.3
 */
public interface RerankerOptions {

	/**
	 * Gets the name of the reranker model to use
	 * @return model name
	 */
	@Nullable
	String getModel();

	/**
	 * Gets the number of top results to return after reranking
	 * @return number of top results
	 */
	@Nullable
	Integer getTopN();

}
