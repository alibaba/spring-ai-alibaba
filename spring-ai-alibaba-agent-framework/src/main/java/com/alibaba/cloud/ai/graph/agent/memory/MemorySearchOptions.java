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
package com.alibaba.cloud.ai.graph.agent.memory;

import java.util.ArrayList;
import java.util.List;

/**
 * Options for advanced memory search operations.
 *
 * <p>Allows filtering and controlling which memory types to search across.
 *
 * @author Spring AI Alibaba
 * @since 1.1.0.0
 */
public class MemorySearchOptions {

  /** The search query string. */
  private String query;

  /** Types of memory to search in. If empty, searches all types. */
  private List<MemoryType> memoryTypes = new ArrayList<>();

  /** Maximum number of results to return. */
  private int limit = 50;

  /** Offset for pagination. */
  private int offset = 0;

  /** Whether to sort by recency (most recent first). */
  private boolean sortByRecency = true;

  /** Default constructor. */
  public MemorySearchOptions() {}

  /**
   * Constructor with query.
   *
   * @param query the search query
   */
  public MemorySearchOptions(String query) {
    this.query = query;
  }

  /**
   * Create a new builder.
   *
   * @return a new builder instance
   */
  public static Builder builder() {
    return new Builder();
  }

  /** Enum for different memory types. */
  public enum MemoryType {
    CONVERSATION,
    CONTEXT,
    FACTS,
    TOOLS
  }

  // Getters and Setters

  public String getQuery() {
    return query;
  }

  public void setQuery(String query) {
    this.query = query;
  }

  public List<MemoryType> getMemoryTypes() {
    return memoryTypes;
  }

  public void setMemoryTypes(List<MemoryType> memoryTypes) {
    this.memoryTypes = memoryTypes != null ? memoryTypes : new ArrayList<>();
  }

  public int getLimit() {
    return limit;
  }

  public void setLimit(int limit) {
    this.limit = Math.max(1, limit);
  }

  public int getOffset() {
    return offset;
  }

  public void setOffset(int offset) {
    this.offset = Math.max(0, offset);
  }

  public boolean isSortByRecency() {
    return sortByRecency;
  }

  public void setSortByRecency(boolean sortByRecency) {
    this.sortByRecency = sortByRecency;
  }

  /** Builder for MemorySearchOptions. */
  public static class Builder {

    private final MemorySearchOptions options = new MemorySearchOptions();

    /**
     * Set the search query.
     *
     * @param query the query string
     * @return this builder
     */
    public Builder query(String query) {
      options.setQuery(query);
      return this;
    }

    /**
     * Add a memory type to search.
     *
     * @param type the memory type
     * @return this builder
     */
    public Builder addMemoryType(MemoryType type) {
      options.memoryTypes.add(type);
      return this;
    }

    /**
     * Add multiple memory types.
     *
     * @param types the memory types
     * @return this builder
     */
    public Builder memoryTypes(List<MemoryType> types) {
      options.setMemoryTypes(types);
      return this;
    }

    /**
     * Set the result limit.
     *
     * @param limit max results
     * @return this builder
     */
    public Builder limit(int limit) {
      options.setLimit(limit);
      return this;
    }

    /**
     * Set the pagination offset.
     *
     * @param offset items to skip
     * @return this builder
     */
    public Builder offset(int offset) {
      options.setOffset(offset);
      return this;
    }

    /**
     * Set whether to sort by recency.
     *
     * @param sortByRecency true to sort by recency
     * @return this builder
     */
    public Builder sortByRecency(boolean sortByRecency) {
      options.setSortByRecency(sortByRecency);
      return this;
    }

    /**
     * Build the options.
     *
     * @return configured options
     */
    public MemorySearchOptions build() {
      return options;
    }
  }

  @Override
  public String toString() {
    return "MemorySearchOptions{"
        + "query='"
        + query
        + '\''
        + ", memoryTypes="
        + memoryTypes
        + ", limit="
        + limit
        + ", offset="
        + offset
        + ", sortByRecency="
        + sortByRecency
        + '}';
  }
}
