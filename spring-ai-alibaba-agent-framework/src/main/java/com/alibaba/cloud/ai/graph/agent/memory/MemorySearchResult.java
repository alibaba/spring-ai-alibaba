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

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Result container for agent memory search operations.
 *
 * <p>Contains search results organized by memory type along with metadata.
 *
 * @author Spring AI Alibaba
 * @since 1.1.0.0
 */
public class MemorySearchResult {

  /** Conversation memory search results. */
  private List<MemoryItem> conversationResults = Collections.emptyList();

  /** Context memory search results. */
  private List<MemoryItem> contextResults = Collections.emptyList();

  /** Facts memory search results. */
  private List<MemoryItem> factResults = Collections.emptyList();

  /** Tools memory search results. */
  private List<MemoryItem> toolResults = Collections.emptyList();

  /** Total number of results matching the search. */
  private long totalCount = 0;

  /** Default constructor. */
  public MemorySearchResult() {}

  /**
   * Constructor with all results.
   *
   * @param conversationResults conversation search results
   * @param contextResults context search results
   * @param factResults facts search results
   * @param toolResults tools search results
   */
  public MemorySearchResult(
      List<MemoryItem> conversationResults,
      List<MemoryItem> contextResults,
      List<MemoryItem> factResults,
      List<MemoryItem> toolResults) {
    this.conversationResults =
        conversationResults != null ? conversationResults : Collections.emptyList();
    this.contextResults = contextResults != null ? contextResults : Collections.emptyList();
    this.factResults = factResults != null ? factResults : Collections.emptyList();
    this.toolResults = toolResults != null ? toolResults : Collections.emptyList();
    this.totalCount =
        this.conversationResults.size()
            + this.contextResults.size()
            + this.factResults.size()
            + this.toolResults.size();
  }

  /**
   * Get total results from all memory types.
   *
   * @return total number of results
   */
  public List<MemoryItem> getAllResults() {
    List<MemoryItem> all = new java.util.ArrayList<>();
    all.addAll(conversationResults);
    all.addAll(contextResults);
    all.addAll(factResults);
    all.addAll(toolResults);
    return all;
  }

  /**
   * Check if there are any results.
   *
   * @return true if any results found
   */
  public boolean hasResults() {
    return totalCount > 0;
  }

  // Getters and Setters

  public List<MemoryItem> getConversationResults() {
    return conversationResults;
  }

  public void setConversationResults(List<MemoryItem> conversationResults) {
    this.conversationResults =
        conversationResults != null ? conversationResults : Collections.emptyList();
  }

  public List<MemoryItem> getContextResults() {
    return contextResults;
  }

  public void setContextResults(List<MemoryItem> contextResults) {
    this.contextResults = contextResults != null ? contextResults : Collections.emptyList();
  }

  public List<MemoryItem> getFactResults() {
    return factResults;
  }

  public void setFactResults(List<MemoryItem> factResults) {
    this.factResults = factResults != null ? factResults : Collections.emptyList();
  }

  public List<MemoryItem> getToolResults() {
    return toolResults;
  }

  public void setToolResults(List<MemoryItem> toolResults) {
    this.toolResults = toolResults != null ? toolResults : Collections.emptyList();
  }

  public long getTotalCount() {
    return totalCount;
  }

  public void setTotalCount(long totalCount) {
    this.totalCount = Math.max(0, totalCount);
  }

  @Override
  public String toString() {
    return "MemorySearchResult{"
        + "conversationResults="
        + conversationResults.size()
        + ", contextResults="
        + contextResults.size()
        + ", factResults="
        + factResults.size()
        + ", toolResults="
        + toolResults.size()
        + ", totalCount="
        + totalCount
        + '}';
  }

  /** Represents a single memory item in search results. */
  public static class MemoryItem {

    private String key;

    private Map<String, Object> value;

    private long timestamp;

    private MemorySearchOptions.MemoryType type;

    /** Default constructor. */
    public MemoryItem() {}

    /**
     * Constructor with all fields.
     *
     * @param key the item key
     * @param value the item value
     * @param timestamp the timestamp
     * @param type the memory type
     */
    public MemoryItem(
        String key,
        Map<String, Object> value,
        long timestamp,
        MemorySearchOptions.MemoryType type) {
      this.key = key;
      this.value = value;
      this.timestamp = timestamp;
      this.type = type;
    }

    // Getters and Setters

    public String getKey() {
      return key;
    }

    public void setKey(String key) {
      this.key = key;
    }

    public Map<String, Object> getValue() {
      return value;
    }

    public void setValue(Map<String, Object> value) {
      this.value = value;
    }

    public long getTimestamp() {
      return timestamp;
    }

    public void setTimestamp(long timestamp) {
      this.timestamp = timestamp;
    }

    public MemorySearchOptions.MemoryType getType() {
      return type;
    }

    public void setType(MemorySearchOptions.MemoryType type) {
      this.type = type;
    }

    @Override
    public String toString() {
      return "MemoryItem{"
          + "key='"
          + key
          + '\''
          + ", type="
          + type
          + ", timestamp="
          + timestamp
          + '}';
    }
  }
}
