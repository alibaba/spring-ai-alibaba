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
import java.util.Map;

/**
 * Request parameters for searching items in the Store.
 * <p>
 * Provides flexible search capabilities including namespace filtering, text queries,
 * custom filters, sorting, and pagination.
 * </p>
 *
 * <h2>Usage Examples</h2> <pre>{@code
 * // Basic namespace search
 * StoreSearchRequest request = StoreSearchRequest.builder()
 *     .namespace("users", "user123")
 *     .build();
 *
 * // Text search with filters
 * StoreSearchRequest request = StoreSearchRequest.builder()
 *     .query("machine learning")
 *     .filter(Map.of("category", "research", "status", "active"))
 *     .limit(20)
 *     .build();
 *
 * // Sorted search with pagination
 * StoreSearchRequest request = StoreSearchRequest.builder()
 *     .namespace("documents")
 *     .sortFields(List.of("createdAt", "title"))
 *     .ascending(false)
 *     .offset(40)
 *     .limit(20)
 *     .build();
 * }</pre>
 *
 * @author Spring AI Alibaba
 * @since 1.0.0.3
 */
public class StoreSearchRequest {

	/**
	 * Namespace filter - search within specific namespace path. Empty list means search
	 * across all namespaces.
	 */
	private List<String> namespace = Collections.emptyList();

	/**
	 * Text query to search in keys and values.
	 */
	private String query;

	/**
	 * Custom filters to apply on item values. Key-value pairs that must match in the
	 * item's value Map.
	 */
	private Map<String, Object> filter = Collections.emptyMap();

	/**
	 * Fields to sort by. Can include "createdAt", "updatedAt", "key", etc.
	 */
	private List<String> sortFields = Collections.emptyList();

	/**
	 * Sort order. True for ascending, false for descending.
	 */
	private boolean ascending = true;

	/**
	 * Offset for pagination (number of items to skip).
	 */
	private int offset = 0;

	/**
	 * Maximum number of items to return.
	 */
	private int limit = 100;

	/**
	 * Default constructor.
	 */
	public StoreSearchRequest() {
	}

	/**
	 * Returns a new builder instance.
	 * @return a new builder
	 */
	public static Builder builder() {
		return new Builder();
	}

	// Getters and Setters

	public List<String> getNamespace() {
		return namespace;
	}

	public void setNamespace(List<String> namespace) {
		this.namespace = namespace != null ? namespace : Collections.emptyList();
	}

	public String getQuery() {
		return query;
	}

	public void setQuery(String query) {
		this.query = query;
	}

	public Map<String, Object> getFilter() {
		return filter;
	}

	public void setFilter(Map<String, Object> filter) {
		this.filter = filter != null ? filter : Collections.emptyMap();
	}

	public List<String> getSortFields() {
		return sortFields;
	}

	public void setSortFields(List<String> sortFields) {
		this.sortFields = sortFields != null ? sortFields : Collections.emptyList();
	}

	public boolean isAscending() {
		return ascending;
	}

	public void setAscending(boolean ascending) {
		this.ascending = ascending;
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

	/**
	 * Builder class for creating StoreSearchRequest instances.
	 */
	public static class Builder {

		private final StoreSearchRequest request = new StoreSearchRequest();

		/**
		 * Set namespace filter using varargs.
		 * @param namespace namespace components
		 * @return this builder
		 */
		public Builder namespace(String... namespace) {
			request.setNamespace(List.of(namespace));
			return this;
		}

		/**
		 * Set namespace filter using a list.
		 * @param namespace namespace path
		 * @return this builder
		 */
		public Builder namespace(List<String> namespace) {
			request.setNamespace(namespace);
			return this;
		}

		/**
		 * Set text query.
		 * @param query search query
		 * @return this builder
		 */
		public Builder query(String query) {
			request.setQuery(query);
			return this;
		}

		/**
		 * Set custom filters.
		 * @param filter filter map
		 * @return this builder
		 */
		public Builder filter(Map<String, Object> filter) {
			request.setFilter(filter);
			return this;
		}

		/**
		 * Set sort fields.
		 * @param sortFields fields to sort by
		 * @return this builder
		 */
		public Builder sortFields(List<String> sortFields) {
			request.setSortFields(sortFields);
			return this;
		}

		/**
		 * Set sort order.
		 * @param ascending true for ascending, false for descending
		 * @return this builder
		 */
		public Builder ascending(boolean ascending) {
			request.setAscending(ascending);
			return this;
		}

		/**
		 * Set pagination offset.
		 * @param offset number of items to skip
		 * @return this builder
		 */
		public Builder offset(int offset) {
			request.setOffset(offset);
			return this;
		}

		/**
		 * Set maximum number of results.
		 * @param limit maximum items to return
		 * @return this builder
		 */
		public Builder limit(int limit) {
			request.setLimit(limit);
			return this;
		}

		/**
		 * Build the StoreSearchRequest.
		 * @return configured search request
		 */
		public StoreSearchRequest build() {
			return request;
		}

	}

	@Override
	public String toString() {
		return "StoreSearchRequest{" + "namespace=" + namespace + ", query='" + query + '\'' + ", filter=" + filter
				+ ", sortFields=" + sortFields + ", ascending=" + ascending + ", offset=" + offset + ", limit=" + limit
				+ '}';
	}

}
