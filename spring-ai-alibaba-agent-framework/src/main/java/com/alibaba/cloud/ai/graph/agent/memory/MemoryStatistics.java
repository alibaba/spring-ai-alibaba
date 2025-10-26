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

/**
 * Statistics about agent memory usage.
 *
 * <p>Provides metrics for memory consumption and storage information.
 *
 * @author Spring AI Alibaba
 * @since 1.1.0.0
 */
public class MemoryStatistics {

  /** Total number of conversation messages. */
  private long conversationCount = 0;

  /** Total number of context items. */
  private long contextCount = 0;

  /** Total number of facts. */
  private long factCount = 0;

  /** Total number of tools. */
  private long toolCount = 0;

  /** Approximate total memory size in bytes. */
  private long estimatedSizeBytes = 0;

  /** Timestamp of the last update. */
  private long lastUpdated = System.currentTimeMillis();

  /** Default constructor. */
  public MemoryStatistics() {}

  /**
   * Constructor with all fields.
   *
   * @param conversationCount conversation count
   * @param contextCount context count
   * @param factCount fact count
   * @param toolCount tool count
   * @param estimatedSizeBytes estimated size
   */
  public MemoryStatistics(
      long conversationCount,
      long contextCount,
      long factCount,
      long toolCount,
      long estimatedSizeBytes) {
    this.conversationCount = conversationCount;
    this.contextCount = contextCount;
    this.factCount = factCount;
    this.toolCount = toolCount;
    this.estimatedSizeBytes = estimatedSizeBytes;
  }

  /**
   * Get total count of all memory items.
   *
   * @return total item count
   */
  public long getTotalItemCount() {
    return conversationCount + contextCount + factCount + toolCount;
  }

  /**
   * Convert size to human-readable format.
   *
   * @return formatted size string
   */
  public String getFormattedSize() {
    if (estimatedSizeBytes < 1024) {
      return estimatedSizeBytes + " B";
    } else if (estimatedSizeBytes < 1024 * 1024) {
      return String.format("%.2f KB", estimatedSizeBytes / 1024.0);
    } else if (estimatedSizeBytes < 1024 * 1024 * 1024) {
      return String.format("%.2f MB", estimatedSizeBytes / (1024.0 * 1024.0));
    } else {
      return String.format("%.2f GB", estimatedSizeBytes / (1024.0 * 1024.0 * 1024.0));
    }
  }

  // Getters and Setters

  public long getConversationCount() {
    return conversationCount;
  }

  public void setConversationCount(long conversationCount) {
    this.conversationCount = conversationCount;
  }

  public long getContextCount() {
    return contextCount;
  }

  public void setContextCount(long contextCount) {
    this.contextCount = contextCount;
  }

  public long getFactCount() {
    return factCount;
  }

  public void setFactCount(long factCount) {
    this.factCount = factCount;
  }

  public long getToolCount() {
    return toolCount;
  }

  public void setToolCount(long toolCount) {
    this.toolCount = toolCount;
  }

  public long getEstimatedSizeBytes() {
    return estimatedSizeBytes;
  }

  public void setEstimatedSizeBytes(long estimatedSizeBytes) {
    this.estimatedSizeBytes = Math.max(0, estimatedSizeBytes);
  }

  public long getLastUpdated() {
    return lastUpdated;
  }

  public void setLastUpdated(long lastUpdated) {
    this.lastUpdated = lastUpdated;
  }

  @Override
  public String toString() {
    return "MemoryStatistics{"
        + "conversationCount="
        + conversationCount
        + ", contextCount="
        + contextCount
        + ", factCount="
        + factCount
        + ", toolCount="
        + toolCount
        + ", estimatedSize="
        + getFormattedSize()
        + ", lastUpdated="
        + lastUpdated
        + '}';
  }
}
