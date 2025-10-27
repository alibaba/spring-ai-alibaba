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

import com.alibaba.cloud.ai.graph.store.Store;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manager for creating and managing AgentMemory instances.
 *
 * <p>Provides factory methods to create AgentMemory instances with a shared Store backend, and
 * manages cached instances for efficient reuse.
 *
 * @author Spring AI Alibaba
 * @since 1.1.0.0
 */
public class MemoryManager {

  private final Store store;

  private final Map<String, AgentMemory> memoryCache = new ConcurrentHashMap<>();

  /**
   * Constructor for MemoryManager.
   *
   * @param store the underlying Store instance to use for all memories
   */
  public MemoryManager(Store store) {
    this.store = Objects.requireNonNull(store, "Store cannot be null");
  }

  /**
   * Get or create an AgentMemory instance for the specified agent.
   *
   * <p>Memory instances are cached to avoid recreating them repeatedly.
   *
   * @param agentId the agent ID
   * @return an AgentMemory instance for the specified agent
   */
  public AgentMemory getAgentMemory(String agentId) {
    return memoryCache.computeIfAbsent(agentId, id -> new DefaultAgentMemory(store, id));
  }

  /**
   * Create a new AgentMemory instance without caching.
   *
   * @param agentId the agent ID
   * @return a new AgentMemory instance
   */
  public AgentMemory createAgentMemory(String agentId) {
    return new DefaultAgentMemory(store, agentId);
  }

  /**
   * Remove a cached AgentMemory instance.
   *
   * @param agentId the agent ID
   */
  public void removeAgentMemory(String agentId) {
    memoryCache.remove(agentId);
  }

  /** Clear all cached AgentMemory instances. */
  public void clearCache() {
    memoryCache.clear();
  }

  /**
   * Get the number of cached AgentMemory instances.
   *
   * @return cache size
   */
  public int getCacheSize() {
    return memoryCache.size();
  }

  /**
   * Get the underlying Store instance.
   *
   * @return the Store instance
   */
  public Store getStore() {
    return store;
  }
}
