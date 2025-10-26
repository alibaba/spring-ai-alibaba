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

import static org.junit.jupiter.api.Assertions.*;

import com.alibaba.cloud.ai.graph.store.stores.InMemoryStore;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for MemoryManager.
 *
 * @author Spring AI Alibaba
 */
class MemoryManagerTest {

  private MemoryManager memoryManager;

  @BeforeEach
  void setUp() {
    InMemoryStore store = new InMemoryStore();
    memoryManager = new MemoryManager(store);
  }

  @Test
  void testGetAgentMemoryCreatesInstance() {
    // Given
    String agentId = "agent_1";

    // When
    AgentMemory memory = memoryManager.getAgentMemory(agentId);

    // Then
    assertNotNull(memory, "Should create AgentMemory instance");
    assertEquals(agentId, memory.getAgentId(), "Agent ID should match");
  }

  @Test
  void testGetAgentMemoryCachesInstance() {
    // Given
    String agentId = "agent_1";

    // When
    AgentMemory memory1 = memoryManager.getAgentMemory(agentId);
    AgentMemory memory2 = memoryManager.getAgentMemory(agentId);

    // Then
    assertSame(memory1, memory2, "Should return cached instance");
  }

  @Test
  void testCreateAgentMemoryNoCaching() {
    // Given
    String agentId = "agent_1";

    // When
    AgentMemory memory1 = memoryManager.createAgentMemory(agentId);
    AgentMemory memory2 = memoryManager.createAgentMemory(agentId);

    // Then
    assertNotSame(memory1, memory2, "Should create separate instances");
  }

  @Test
  void testRemoveAgentMemoryFromCache() {
    // Given
    String agentId = "agent_1";
    memoryManager.getAgentMemory(agentId);
    assertEquals(1, memoryManager.getCacheSize(), "Cache should have one entry");

    // When
    memoryManager.removeAgentMemory(agentId);

    // Then
    assertEquals(0, memoryManager.getCacheSize(), "Cache should be empty");
  }

  @Test
  void testClearCache() {
    // Given
    memoryManager.getAgentMemory("agent_1");
    memoryManager.getAgentMemory("agent_2");
    memoryManager.getAgentMemory("agent_3");
    assertEquals(3, memoryManager.getCacheSize(), "Cache should have three entries");

    // When
    memoryManager.clearCache();

    // Then
    assertEquals(0, memoryManager.getCacheSize(), "Cache should be empty");
  }

  @Test
  void testGetStoreReturnsUnderlyingStore() {
    // Given
    InMemoryStore expectedStore = new InMemoryStore();
    MemoryManager manager = new MemoryManager(expectedStore);

    // When
    var store = manager.getStore();

    // Then
    assertNotNull(store, "Should return Store instance");
  }

  @Test
  void testMultipleAgentsIndependentMemories() {
    // Given
    String agentId1 = "agent_1";
    String agentId2 = "agent_2";

    // When
    AgentMemory memory1 = memoryManager.getAgentMemory(agentId1);
    AgentMemory memory2 = memoryManager.getAgentMemory(agentId2);

    // Store different data
    memory1.storeContext("session1", "ctx", Map.of("agent", "1"));
    memory2.storeContext("session1", "ctx", Map.of("agent", "2"));

    // Then
    var ctx1 = memory1.getContext("session1", "ctx");
    var ctx2 = memory2.getContext("session1", "ctx");

    assertTrue(ctx1.isPresent());
    assertTrue(ctx2.isPresent());
    assertEquals("1", ctx1.get().get("agent"), "Agent 1's context should have value 1");
    assertEquals("2", ctx2.get().get("agent"), "Agent 2's context should have value 2");
  }

  @Test
  void testNullStoreThrowsException() {
    // When & Then
    assertThrows(
        NullPointerException.class,
        () -> new MemoryManager(null),
        "Should throw NullPointerException for null store");
  }

  @Test
  void testCacheSizeAccurate() {
    // When
    memoryManager.getAgentMemory("agent_1");
    assertEquals(1, memoryManager.getCacheSize());

    memoryManager.getAgentMemory("agent_2");
    assertEquals(2, memoryManager.getCacheSize());

    memoryManager.getAgentMemory("agent_3");
    assertEquals(3, memoryManager.getCacheSize());

    memoryManager.removeAgentMemory("agent_2");
    assertEquals(2, memoryManager.getCacheSize());

    // Then
    assertTrue(memoryManager.getCacheSize() >= 0, "Cache size should be non-negative");
  }
}
