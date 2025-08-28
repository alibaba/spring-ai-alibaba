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
 * Request parameters for listing namespaces in the Store.
 * <p>
 * Provides filtering and pagination capabilities for namespace discovery. Useful for
 * exploring the hierarchical structure of stored data.
 * </p>
 *
 * <h2>Usage Examples</h2> <pre>{@code
 * // List all top-level namespaces
 * NamespaceListRequest request = NamespaceListRequest.builder()
 *     .build();
 *
 * // List namespaces under "users" prefix
 * NamespaceListRequest request = NamespaceListRequest.builder()
 *     .namespace("users")
 *     .maxDepth(2)
 *     .build();
 *
 * // Paginated namespace listing
 * NamespaceListRequest request = NamespaceListRequest.builder()
 *     .offset(20)
 *     .limit(10)
 *     .build();
 * }</pre>
 *
 * @author Spring AI Alibaba
 * @since 1.0.0.3
 */
public class NamespaceListRequest {

	/**
	 * Namespace prefix filter. Only namespaces starting with this prefix will be
	 * returned. Empty list means no prefix filter (list all namespaces).
	 */
	private List<String> namespace = Collections.emptyList();

	/**
	 * Maximum depth of namespaces to return. -1 means unlimited depth.
	 */
	private int maxDepth = -1;

	/**
	 * Offset for pagination (number of namespaces to skip).
	 */
	private int offset = 0;

	/**
	 * Maximum number of namespaces to return.
	 */
	private int limit = 1000;

	/**
	 * Default constructor.
	 */
	public NamespaceListRequest() {
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

	public int getMaxDepth() {
		return maxDepth;
	}

	public void setMaxDepth(int maxDepth) {
		this.maxDepth = maxDepth;
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
	 * Builder class for creating NamespaceListRequest instances.
	 */
	public static class Builder {

		private final NamespaceListRequest request = new NamespaceListRequest();

		/**
		 * Set namespace prefix filter using varargs.
		 * @param namespace namespace components
		 * @return this builder
		 */
		public Builder namespace(String... namespace) {
			request.setNamespace(List.of(namespace));
			return this;
		}

		/**
		 * Set namespace prefix filter using a list.
		 * @param namespace namespace path prefix
		 * @return this builder
		 */
		public Builder namespace(List<String> namespace) {
			request.setNamespace(namespace);
			return this;
		}

		/**
		 * Set maximum depth of namespaces to return.
		 * @param maxDepth maximum depth (-1 for unlimited)
		 * @return this builder
		 */
		public Builder maxDepth(int maxDepth) {
			request.setMaxDepth(maxDepth);
			return this;
		}

		/**
		 * Set pagination offset.
		 * @param offset number of namespaces to skip
		 * @return this builder
		 */
		public Builder offset(int offset) {
			request.setOffset(offset);
			return this;
		}

		/**
		 * Set maximum number of results.
		 * @param limit maximum namespaces to return
		 * @return this builder
		 */
		public Builder limit(int limit) {
			request.setLimit(limit);
			return this;
		}

		/**
		 * Build the NamespaceListRequest.
		 * @return configured namespace list request
		 */
		public NamespaceListRequest build() {
			return request;
		}

	}

	@Override
	public String toString() {
		return "NamespaceListRequest{" + "namespace=" + namespace + ", maxDepth=" + maxDepth + ", offset=" + offset
				+ ", limit=" + limit + '}';
	}

}
