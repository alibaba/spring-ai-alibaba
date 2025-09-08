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

package com.alibaba.cloud.ai.example.deepresearch.rag.strategy;

import org.springframework.ai.document.Document;

import java.util.List;

/**
 * Defines a strategy interface for merging multiple retrieval result lists.
 */
public interface FusionStrategy {

	/**
	 * Merges multiple ranked document lists into a single, re-ranked list.
	 * @param results A list containing multiple retrieval result lists.
	 * @return A single merged and re-ranked document list.
	 */
	List<Document> fuse(List<List<Document>> results);

	/**
	 * Returns the unique name of this strategy.
	 * @return The strategy name.
	 */
	String getStrategyName();

}
