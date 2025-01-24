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
package com.alibaba.cloud.ai.reader.arxiv.client;

/**
 * ArxivSortOrder indicates the order in which search results are sorted according to the
 * specified ArxivSortCriterion
 *
 * @see <a href="https://arxiv.org/help/api/user-manual#sort">arXiv API User's Manual:
 * sort order</a>
 * @author brianxiadong
 */
public enum ArxivSortOrder {

	/**
	 * Sort in ascending order
	 */
	ASCENDING("ascending"),

	/**
	 * Sort in descending order
	 */
	DESCENDING("descending");

	private final String value;

	ArxivSortOrder(String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}

}
