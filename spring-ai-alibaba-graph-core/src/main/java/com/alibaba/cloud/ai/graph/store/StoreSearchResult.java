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
package com.alibaba.cloud.ai.graph.store;

import java.util.Collections;
import java.util.List;

/**
 * Result container for Store search operations.
 * <p>
 * Encapsulates the search results along with pagination information and metadata about
 * the search operation.
 * </p>
 *
 * <h2>Usage Example</h2> <pre>{@code
 * StoreSearchResult result = store.searchItems(searchRequest);
 *
 * // Check if there are results
 * if (!result.getItems().isEmpty()) {
 *     // Process the items
 *     for (StoreItem item : result.getItems()) {
 *         System.out.println("Found: " + item.getKey());
 *     }
 *
 *     // Check pagination
 *     if (result.hasMore()) {
 *         System.out.println("Total: " + result.getTotalCount());
 *         System.out.println("More results available");
 *     }
 * }
 * }</pre>
 *
 * @author Spring AI Alibaba
 * @since 1.0.0.3
 */
public class StoreSearchResult {

	/**
	 * The list of items found in the search.
	 */
	private List<StoreItem> items = Collections.emptyList();

	/**
	 * Total number of items that match the search criteria (before pagination).
	 */
	private long totalCount = 0;

	/**
	 * The offset used in the search request.
	 */
	private int offset = 0;

	/**
	 * The limit used in the search request.
	 */
	private int limit = 100;

	/**
	 * Default constructor.
	 */
	public StoreSearchResult() {
	}

	/**
	 * Constructor with all parameters.
	 * @param items the list of found items
	 * @param totalCount total matching items count
	 * @param offset search offset
	 * @param limit search limit
	 */
	public StoreSearchResult(List<StoreItem> items, long totalCount, int offset, int limit) {
		this.items = items != null ? items : Collections.emptyList();
		this.totalCount = totalCount;
		this.offset = offset;
		this.limit = limit;
	}

	/**
	 * Static factory method to create a search result.
	 * @param items the list of found items
	 * @param totalCount total matching items count
	 * @param offset search offset
	 * @param limit search limit
	 * @return a new StoreSearchResult instance
	 */
	public static StoreSearchResult of(List<StoreItem> items, long totalCount, int offset, int limit) {
		return new StoreSearchResult(items, totalCount, offset, limit);
	}

	/**
	 * Static factory method for empty results.
	 * @return an empty search result
	 */
	public static StoreSearchResult empty() {
		return new StoreSearchResult(Collections.emptyList(), 0, 0, 100);
	}

	/**
	 * Check if there are more results available beyond the current page.
	 * @return true if more results are available
	 */
	public boolean hasMore() {
		return (offset + items.size()) < totalCount;
	}

	/**
	 * Check if this is the first page of results.
	 * @return true if this is the first page
	 */
	public boolean isFirstPage() {
		return offset == 0;
	}

	/**
	 * Check if this is the last page of results.
	 * @return true if this is the last page
	 */
	public boolean isLastPage() {
		return !hasMore();
	}

	/**
	 * Get the number of pages based on the limit.
	 * @return total number of pages
	 */
	public long getTotalPages() {
		if (limit <= 0) {
			return 1;
		}
		return (totalCount + limit - 1) / limit;
	}

	/**
	 * Get the current page number (1-based).
	 * @return current page number
	 */
	public long getCurrentPage() {
		if (limit <= 0) {
			return 1;
		}
		return (offset / limit) + 1;
	}

	// Getters and Setters

	public List<StoreItem> getItems() {
		return items;
	}

	public void setItems(List<StoreItem> items) {
		this.items = items != null ? items : Collections.emptyList();
	}

	public long getTotalCount() {
		return totalCount;
	}

	public void setTotalCount(long totalCount) {
		this.totalCount = Math.max(0, totalCount);
	}

	public int getOffset() {
		return offset;
	}

	public void setOffset(int offset) {
		this.offset = Math.max(0, offset);
	}

	public int getLimit() {
		return limit;
	}

	public void setLimit(int limit) {
		this.limit = Math.max(1, limit);
	}

	@Override
	public String toString() {
		return "StoreSearchResult{" + "itemsCount=" + items.size() + ", totalCount=" + totalCount + ", offset=" + offset
				+ ", limit=" + limit + ", hasMore=" + hasMore() + '}';
	}

}
