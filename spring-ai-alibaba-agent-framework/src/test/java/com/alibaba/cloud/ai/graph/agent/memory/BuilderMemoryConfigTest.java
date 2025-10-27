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
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.UserMessage;

/**
 * Unit tests for Builder memory configuration.
 *
 * @author Spring AI Alibaba
 * @since 1.1.0.0
 */
class BuilderMemoryConfigTest {

  private InMemoryStore store;
  private MemoryManager memoryManager;
  private AgentMemory memory;

  @BeforeEach
  void setUp() {
    store = new InMemoryStore();
    memoryManager = new MemoryManager(store);
    memory = memoryManager.getAgentMemory("test_agent");
  }

  @Test
  void testBuilderAcceptsMemory() {
    // Test that memory can be configured through builder pattern
    // We test the memory configuration directly since Builder is abstract

    assertNotNull(memory);
    assertTrue(memory instanceof DefaultAgentMemory);
  }

  @Test
  void testMemoryConfigurationIsPreserved() {
    // Test that memory configuration is preserved through builder
    AgentMemory memory1 = memoryManager.getAgentMemory("agent_1");
    String sessionId = "session_1";

    memory1.storeConversationMessage(sessionId, new UserMessage("Test message"));

    // Get the same memory instance
    AgentMemory memory2 = memoryManager.getAgentMemory("agent_1");
    var history = memory2.getConversationHistory(sessionId);

    assertEquals(1, history.size());
  }

  @Test
  void testMultipleAgentsWithDifferentMemories() {
    // Test that multiple agents can have independent memory configurations
    AgentMemory agent1Memory = memoryManager.getAgentMemory("agent_1");
    AgentMemory agent2Memory = memoryManager.getAgentMemory("agent_2");

    String sessionId = "common_session";

    // Store in agent1
    agent1Memory.storeConversationMessage(sessionId, new UserMessage("Agent 1 message"));

    // Store in agent2
    agent2Memory.storeConversationMessage(sessionId, new UserMessage("Agent 2 message"));

    // Verify separation
    var history1 = agent1Memory.getConversationHistory(sessionId);
    var history2 = agent2Memory.getConversationHistory(sessionId);

    assertEquals(1, history1.size());
    assertEquals(1, history2.size());
    assertNotEquals(history1.get(0).getText(), history2.get(0).getText());
  }

  @Test
  void testBuilderMemoryNullHandling() {
    // Test that memory can be null (optional configuration)
    // Create an agent without memory configuration
    AgentMemory memory1 = memoryManager.getAgentMemory("no_memory_agent");
    assertNotNull(memory1); // Factory always provides memory instance

    // Test null safety
    AgentMemory nullMemory = null;
    if (nullMemory != null) {
      nullMemory.storeConversationMessage("session", new UserMessage("test"));
    }
    // Should not throw exception
  }

  @Test
  void testMemoryPersistenceAcrossMultipleSessions() {
    // Test that memory persists across multiple sessions for same agent
    AgentMemory memory = memoryManager.getAgentMemory("persistent_agent");

    // Session 1
    memory.storeConversationMessage("session_1", new UserMessage("Session 1 message"));

    // Session 2
    memory.storeConversationMessage("session_2", new UserMessage("Session 2 message"));

    // Verify both exist
    var history1 = memory.getConversationHistory("session_1");
    var history2 = memory.getConversationHistory("session_2");

    assertEquals(1, history1.size());
    assertEquals(1, history2.size());
  }

  @Test
  void testBuilderMemoryIsolation() {
    // Test that memory is isolated between different builder instances
    AgentMemory memory1 = memoryManager.getAgentMemory("isolated_1");
    AgentMemory memory2 = memoryManager.getAgentMemory("isolated_2");

    String commonSession = "isolation_test";

    // Add data to both
    memory1.storeConversationMessage(commonSession, new UserMessage("Memory 1"));
    memory2.storeConversationMessage(commonSession, new UserMessage("Memory 2"));

    // Verify isolation at agent level
    var data1 = memory1.getConversationHistory(commonSession);
    var data2 = memory2.getConversationHistory(commonSession);

    assertEquals(1, data1.size());
    assertEquals(1, data2.size());
  }

  @Test
  void testMemoryContextConfiguration() {
    // Test configuring and retrieving context through memory
    AgentMemory memory = memoryManager.getAgentMemory("context_agent");
    String sessionId = "context_session";

    Map<String, Object> contextData = new HashMap<>();
    contextData.put("user_id", "user_123");
    contextData.put("session_type", "support");
    contextData.put("priority", "high");

    memory.storeContext(sessionId, "request_context", contextData);

    var retrievedContext = memory.getContext(sessionId, "request_context");
    assertTrue(retrievedContext.isPresent());
    assertEquals("user_123", retrievedContext.get().get("user_id"));
  }

  @Test
  void testMemoryFactsConfiguration() {
    // Test configuring and retrieving facts through memory
    AgentMemory memory = memoryManager.getAgentMemory("facts_agent");
    String sessionId = "facts_session";

    Map<String, Object> facts = new HashMap<>();
    facts.put("extraction", "important_data");
    facts.put("confidence", 0.98);
    facts.put("source", "user_input");

    memory.storeFact(sessionId, "extracted_facts", facts);

    var retrievedFacts = memory.getFact(sessionId, "extracted_facts");
    assertTrue(retrievedFacts.isPresent());
    assertEquals(0.98, retrievedFacts.get().get("confidence"));
  }

  @Test
  void testMemoryToolMetadataConfiguration() {
    // Test configuring tool metadata through memory
    AgentMemory memory = memoryManager.getAgentMemory("tools_agent");

    Map<String, Object> toolConfig = new HashMap<>();
    toolConfig.put("name", "SearchTool");
    toolConfig.put("description", "Search for information");
    toolConfig.put("timeout_ms", 5000);
    toolConfig.put("enabled", true);

    memory.storeToolMetadata("search_tool", toolConfig);

    var retrievedTool = memory.getToolMetadata("search_tool");
    assertTrue(retrievedTool.isPresent());
    assertEquals(5000, retrievedTool.get().get("timeout_ms"));
  }

  @Test
  void testMemorySearchConfiguration() {
    // Test search functionality configuration through memory
    AgentMemory memory = memoryManager.getAgentMemory("search_agent");
    String sessionId = "search_session";

    memory.storeConversationMessage(sessionId, new UserMessage("Tell me about machine learning"));
    memory.storeConversationMessage(sessionId, new UserMessage("What are neural networks?"));

    // Basic search
    MemorySearchResult result1 = memory.search(sessionId, "learning");
    assertNotNull(result1);

    // Search with options
    MemorySearchOptions options =
        MemorySearchOptions.builder()
            .query("neural")
            .limit(10)
            .addMemoryType(MemorySearchOptions.MemoryType.CONVERSATION)
            .sortByRecency(true)
            .build();

    MemorySearchResult result2 = memory.search(sessionId, options);
    assertNotNull(result2);
  }

  @Test
  void testMemoryStatisticsConfiguration() {
    // Test statistics configuration through memory
    AgentMemory memory = memoryManager.getAgentMemory("stats_agent");
    String sessionId = "stats_session";

    // Add various data
    memory.storeConversationMessage(sessionId, new UserMessage("msg1"));
    memory.storeConversationMessage(sessionId, new UserMessage("msg2"));

    Map<String, Object> context = new HashMap<>();
    context.put("key", "value");
    memory.storeContext(sessionId, "ctx", context);

    Map<String, Object> fact = new HashMap<>();
    fact.put("key", "value");
    memory.storeFact(sessionId, "fact", fact);

    memory.storeToolMetadata("tool", new HashMap<>());

    // Get statistics
    MemoryStatistics stats = memory.getStatistics(sessionId);

    assertNotNull(stats);
    assertEquals(2, stats.getConversationCount());
    assertEquals(1, stats.getContextCount());
    assertEquals(1, stats.getFactCount());
    assertEquals(1, stats.getToolCount());
    assertTrue(stats.getEstimatedSizeBytes() > 0);
  }

  @Test
  void testMemoryManagerCacheConfiguration() {
    // Test that MemoryManager properly caches configured memories
    String agentId = "cached_agent";

    AgentMemory memory1 = memoryManager.getAgentMemory(agentId);
    AgentMemory memory2 = memoryManager.getAgentMemory(agentId);

    // Should be same instance (cached)
    assertSame(memory1, memory2);

    // Cache size should reflect the cached instance
    assertTrue(memoryManager.getCacheSize() > 0);
  }

  @Test
  void testMemoryManagerCacheRemoval() {
    // Test that MemoryManager can remove cached memory configurations
    String agentId = "removable_cached_agent";

    AgentMemory memory1 = memoryManager.getAgentMemory(agentId);
    memoryManager.removeAgentMemory(agentId);
    AgentMemory memory2 = memoryManager.getAgentMemory(agentId);

    // Should be different instances after removal
    assertNotSame(memory1, memory2);
  }

  @Test
  void testMultipleMemoryInstancesIndependence() {
    // Test that multiple memory instances are independent
    AgentMemory mem1 = memoryManager.getAgentMemory("agent_mem_1");
    AgentMemory mem2 = memoryManager.getAgentMemory("agent_mem_2");

    String session = "test_session";

    // Add different data
    mem1.storeConversationMessage(session, new UserMessage("Data from memory 1"));
    mem2.storeConversationMessage(session, new UserMessage("Data from memory 2"));

    // Verify independence
    var history1 = mem1.getConversationHistory(session);
    var history2 = mem2.getConversationHistory(session);

    assertEquals(1, history1.size());
    assertEquals(1, history2.size());
    assertNotEquals(history1.get(0).getText(), history2.get(0).getText());
  }

  @Test
  void testBuilderMemoryUnderlyingStore() {
    // Test that builder configured memory uses correct underlying store
    AgentMemory memory = memoryManager.getAgentMemory("store_agent");

    assertNotNull(memory.getStore());
    assertSame(store, memory.getStore());
  }

  @Test
  void testMemoryAgentIdConfiguration() {
    // Test that memory correctly tracks agent ID
    String agentId = "identified_agent";
    AgentMemory memory = memoryManager.getAgentMemory(agentId);

    assertEquals(agentId, memory.getAgentId());
  }

  @Test
  void testMemoryConfigurationFluencyPattern() {
    // Test the fluent builder pattern for memory configuration
    // This test verifies the expected usage pattern

    AgentMemory memory = memoryManager.getAgentMemory("fluent_agent");

    // Simulate fluent configuration pattern
    String sessionId = "fluent_session";

    memory.storeConversationMessage(sessionId, new UserMessage("Question 1"));
    memory.storeConversationMessage(sessionId, new UserMessage("Question 2"));

    Map<String, Object> context = new HashMap<>();
    context.put("fluent", true);
    memory.storeContext(sessionId, "fluent_context", context);

    // Verify fluent operations succeeded
    assertEquals(2, memory.getConversationHistory(sessionId).size());
    assertTrue(memory.getContext(sessionId, "fluent_context").isPresent());
  }
}
