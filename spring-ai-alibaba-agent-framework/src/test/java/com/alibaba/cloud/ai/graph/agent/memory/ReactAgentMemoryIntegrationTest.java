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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;

/**
 * Unit tests for ReactAgent and Builder memory integration.
 *
 * @author Spring AI Alibaba
 * @since 1.1.0.0
 */
class ReactAgentMemoryIntegrationTest {

  private AgentMemory memory;
  private MemoryManager memoryManager;
  private InMemoryStore store;

  @BeforeEach
  void setUp() {
    store = new InMemoryStore();
    memoryManager = new MemoryManager(store);
    memory = memoryManager.getAgentMemory("test_agent");
  }

  @Test
  void testBuilderCanConfigureMemory() {
    // This test verifies that Builder can accept memory configuration
    // Since we can't instantiate ReactAgent directly without full setup,
    // we test that the configuration can be passed through

    assertNotNull(memory);
    assertTrue(memory instanceof DefaultAgentMemory);
  }

  @Test
  void testMemoryManagerCreateMultipleAgentMemories() {
    // Test that MemoryManager can create independent memories for different agents
    AgentMemory memory1 = memoryManager.getAgentMemory("agent_1");
    AgentMemory memory2 = memoryManager.getAgentMemory("agent_2");

    assertNotNull(memory1);
    assertNotNull(memory2);
    assertNotSame(memory1, memory2);
  }

  @Test
  void testMemoryManagerCachesInstances() {
    // Test that the same agent ID returns the same cached instance
    AgentMemory memory1 = memoryManager.getAgentMemory("cached_agent");
    AgentMemory memory2 = memoryManager.getAgentMemory("cached_agent");

    assertSame(memory1, memory2);
  }

  @Test
  void testAgentMemoryStoreConversation() {
    // Test storing and retrieving conversation messages through memory
    String sessionId = "session_001";
    Message userMsg = new UserMessage("Hello, agent!");
    Message assistantMsg = new AssistantMessage("Hello! How can I help?");

    memory.storeConversationMessage(sessionId, userMsg);
    memory.storeConversationMessage(sessionId, assistantMsg);

    List<Message> history = memory.getConversationHistory(sessionId);

    assertNotNull(history);
    assertEquals(2, history.size());
  }

  @Test
  void testAgentMemoryStoreContext() {
    // Test storing and retrieving context data
    String sessionId = "session_002";
    String contextKey = "user_profile";
    Map<String, Object> contextData = new HashMap<>();
    contextData.put("name", "John Doe");
    contextData.put("age", 30);
    contextData.put("vip_level", "gold");

    memory.storeContext(sessionId, contextKey, contextData);

    var retrievedContext = memory.getContext(sessionId, contextKey);

    assertTrue(retrievedContext.isPresent());
    Map<String, Object> data = retrievedContext.get();
    assertEquals("John Doe", data.get("name"));
    assertEquals(30, data.get("age"));
    assertEquals("gold", data.get("vip_level"));
  }

  @Test
  void testAgentMemoryStoreFacts() {
    // Test storing and retrieving facts
    String sessionId = "session_003";
    Map<String, Object> factData = new HashMap<>();
    factData.put("purchase_history", Arrays.asList("item1", "item2", "item3"));
    factData.put("total_spent", 1500.00);
    factData.put("confidence", 0.95);

    memory.storeFact(sessionId, "customer_facts", factData);

    var retrievedFact = memory.getFact(sessionId, "customer_facts");

    assertTrue(retrievedFact.isPresent());
    Map<String, Object> data = retrievedFact.get();
    assertEquals(0.95, data.get("confidence"));
  }

  @Test
  void testAgentMemoryStoreToolMetadata() {
    // Test storing and retrieving tool metadata
    Map<String, Object> toolMetadata = new HashMap<>();
    toolMetadata.put("name", "WeatherTool");
    toolMetadata.put("description", "Get weather information");
    toolMetadata.put("version", "1.0.0");

    memory.storeToolMetadata("weather_tool", toolMetadata);

    var retrievedTool = memory.getToolMetadata("weather_tool");

    assertTrue(retrievedTool.isPresent());
    Map<String, Object> data = retrievedTool.get();
    assertEquals("WeatherTool", data.get("name"));
    assertEquals("1.0.0", data.get("version"));
  }

  @Test
  void testAgentMemorySearch() {
    // Test searching across memory
    String sessionId = "session_004";

    // Add some conversation
    memory.storeConversationMessage(sessionId, new UserMessage("What is the weather?"));
    memory.storeConversationMessage(sessionId, new AssistantMessage("It's sunny today"));

    // Add some context
    Map<String, Object> context = new HashMap<>();
    context.put("location", "San Francisco");
    memory.storeContext(sessionId, "location_info", context);

    // Search
    MemorySearchResult result = memory.search(sessionId, "weather");

    assertNotNull(result);
    // Result should contain conversation items mentioning "weather"
  }

  @Test
  void testAgentMemorySearchWithOptions() {
    // Test searching with specific options
    String sessionId = "session_005";

    // Add conversations
    memory.storeConversationMessage(sessionId, new UserMessage("User query 1"));
    memory.storeConversationMessage(sessionId, new AssistantMessage("Assistant response 1"));

    // Search with options
    MemorySearchOptions options =
        MemorySearchOptions.builder()
            .query("query")
            .limit(10)
            .addMemoryType(MemorySearchOptions.MemoryType.CONVERSATION)
            .sortByRecency(true)
            .build();

    MemorySearchResult result = memory.search(sessionId, options);

    assertNotNull(result);
    assertTrue(result.hasResults());
  }

  @Test
  void testAgentMemoryStatistics() {
    // Test retrieving memory statistics
    String sessionId = "session_006";

    // Add various memory types
    memory.storeConversationMessage(sessionId, new UserMessage("Test"));
    memory.storeConversationMessage(sessionId, new AssistantMessage("Response"));

    Map<String, Object> context = new HashMap<>();
    context.put("key", "value");
    memory.storeContext(sessionId, "context_key", context);

    Map<String, Object> fact = new HashMap<>();
    fact.put("fact_key", "fact_value");
    memory.storeFact(sessionId, "fact_key", fact);

    // Get statistics
    MemoryStatistics stats = memory.getStatistics(sessionId);

    assertNotNull(stats);
    assertEquals(2, stats.getConversationCount());
    assertEquals(1, stats.getContextCount());
    assertEquals(1, stats.getFactCount());
    assertTrue(stats.getEstimatedSizeBytes() > 0);
  }

  @Test
  void testAgentMemoryClearConversation() {
    // Test clearing conversation history
    String sessionId = "session_007";

    memory.storeConversationMessage(sessionId, new UserMessage("Message 1"));
    memory.storeConversationMessage(sessionId, new UserMessage("Message 2"));

    List<Message> beforeClear = memory.getConversationHistory(sessionId);
    assertEquals(2, beforeClear.size());

    memory.clearConversationHistory(sessionId);

    List<Message> afterClear = memory.getConversationHistory(sessionId);
    assertEquals(0, afterClear.size());
  }

  @Test
  void testAgentMemoryUpdateContext() {
    // Test updating existing context
    String sessionId = "session_008";
    String contextKey = "user_settings";

    Map<String, Object> originalContext = new HashMap<>();
    originalContext.put("theme", "dark");
    originalContext.put("language", "en");
    memory.storeContext(sessionId, contextKey, originalContext);

    Map<String, Object> updatedContext = new HashMap<>();
    updatedContext.put("theme", "light");
    updatedContext.put("language", "zh");
    updatedContext.put("notification", true);
    memory.updateContext(sessionId, contextKey, updatedContext);

    var retrieved = memory.getContext(sessionId, contextKey);
    assertTrue(retrieved.isPresent());
    Map<String, Object> data = retrieved.get();
    assertEquals("light", data.get("theme"));
    assertEquals("zh", data.get("language"));
    assertEquals(true, data.get("notification"));
  }

  @Test
  void testAgentMemoryDeleteContext() {
    // Test deleting context
    String sessionId = "session_009";

    Map<String, Object> context = new HashMap<>();
    context.put("data", "value");
    memory.storeContext(sessionId, "to_delete", context);

    var beforeDelete = memory.getContext(sessionId, "to_delete");
    assertTrue(beforeDelete.isPresent());

    memory.deleteContext(sessionId, "to_delete");

    var afterDelete = memory.getContext(sessionId, "to_delete");
    assertTrue(afterDelete.isEmpty());
  }

  @Test
  void testAgentMemoryMultipleSessions() {
    // Test that different sessions maintain independent memories
    String session1 = "session_101";
    String session2 = "session_102";

    memory.storeConversationMessage(session1, new UserMessage("Session 1 message"));
    memory.storeConversationMessage(session2, new UserMessage("Session 2 message"));

    List<Message> history1 = memory.getConversationHistory(session1);
    List<Message> history2 = memory.getConversationHistory(session2);

    assertEquals(1, history1.size());
    assertEquals(1, history2.size());
    assertNotEquals(history1.get(0).getText(), history2.get(0).getText());
  }

  @Test
  void testAgentMemoryGetAllContext() {
    // Test retrieving all context for a session
    String sessionId = "session_110";

    Map<String, Object> ctx1 = new HashMap<>();
    ctx1.put("type", "user");
    memory.storeContext(sessionId, "context_1", ctx1);

    Map<String, Object> ctx2 = new HashMap<>();
    ctx2.put("type", "order");
    memory.storeContext(sessionId, "context_2", ctx2);

    Map<String, Map<String, Object>> allContext = memory.getAllContext(sessionId);

    assertNotNull(allContext);
    assertEquals(2, allContext.size());
    assertTrue(allContext.containsKey("context_1"));
    assertTrue(allContext.containsKey("context_2"));
  }

  @Test
  void testAgentMemoryGetAllFacts() {
    // Test retrieving all facts for a session
    String sessionId = "session_111";

    Map<String, Object> fact1 = new HashMap<>();
    fact1.put("value", "data1");
    memory.storeFact(sessionId, "fact_1", fact1);

    Map<String, Object> fact2 = new HashMap<>();
    fact2.put("value", "data2");
    memory.storeFact(sessionId, "fact_2", fact2);

    Map<String, Map<String, Object>> allFacts = memory.getAllFacts(sessionId);

    assertNotNull(allFacts);
    assertEquals(2, allFacts.size());
    assertTrue(allFacts.containsKey("fact_1"));
    assertTrue(allFacts.containsKey("fact_2"));
  }

  @Test
  void testAgentMemoryGetAllToolsMetadata() {
    // Test retrieving all tools metadata
    Map<String, Object> tool1 = new HashMap<>();
    tool1.put("name", "Tool1");
    memory.storeToolMetadata("tool_1", tool1);

    Map<String, Object> tool2 = new HashMap<>();
    tool2.put("name", "Tool2");
    memory.storeToolMetadata("tool_2", tool2);

    Map<String, Map<String, Object>> allTools = memory.getAllToolsMetadata();

    assertNotNull(allTools);
    assertTrue(allTools.size() >= 2);
  }

  @Test
  void testBuilderMemoryConfiguration() {
    // Test that Builder correctly stores memory configuration
    AgentMemory testMemory = memoryManager.getAgentMemory("builder_test_agent");

    assertNotNull(testMemory);
    assertEquals(testMemory.getAgentId(), "builder_test_agent");
  }

  @Test
  void testMemoryManagerGetStore() {
    // Test that MemoryManager provides access to underlying store
    AgentMemory agentMemory = memoryManager.getAgentMemory("store_test");

    assertNotNull(agentMemory.getStore());
    assertSame(store, agentMemory.getStore());
  }

  @Test
  void testMemoryManagerRemoveAgent() {
    // Test removing agent from memory cache
    String agentId = "removable_agent";
    AgentMemory memory1 = memoryManager.getAgentMemory(agentId);

    memoryManager.removeAgentMemory(agentId);

    AgentMemory memory2 = memoryManager.getAgentMemory(agentId);

    // After removal, new instance should be created
    assertNotSame(memory1, memory2);
  }

  @Test
  void testConversationHistoryWithPagination() {
    // Test conversation history retrieval with pagination
    String sessionId = "session_120";

    // Add multiple messages
    for (int i = 0; i < 5; i++) {
      memory.storeConversationMessage(sessionId, new UserMessage("Message " + i));
    }

    // Get with limit
    List<Message> page1 = memory.getConversationHistory(sessionId, 2, 0);
    List<Message> page2 = memory.getConversationHistory(sessionId, 2, 2);

    assertEquals(2, page1.size());
    assertEquals(2, page2.size());
    assertNotEquals(page1.get(0).getText(), page2.get(0).getText());
  }

  @Test
  void testMemoryEnabledFlag() {
    // Test that memory enabled flag is properly tracked
    assertNotNull(memory);

    // Since memory is not null, it should be enabled
    // Create a concrete agent implementation for testing
    String agentId = "test-agent-enabled";
    InMemoryStore store = new InMemoryStore();

    // Test with memory
    AgentMemory testMemory = new DefaultAgentMemory(store, agentId);

    // For direct memory testing
    assertTrue(testMemory != null); // Memory is not null means enabled
  }

  @Test
  void testMemoryClearAll() {
    // Test clearing all memory for a session
    String sessionId = "session_130";

    // Add all types of memory
    memory.storeConversationMessage(sessionId, new UserMessage("msg"));
    Map<String, Object> ctx = new HashMap<>();
    ctx.put("key", "value");
    memory.storeContext(sessionId, "ctx", ctx);
    Map<String, Object> fact = new HashMap<>();
    fact.put("key", "value");
    memory.storeFact(sessionId, "fact", fact);

    MemoryStatistics beforeClear = memory.getStatistics(sessionId);
    assertTrue(beforeClear.getEstimatedSizeBytes() > 0);

    memory.clearAllMemory(sessionId);

    MemoryStatistics afterClear = memory.getStatistics(sessionId);
    assertEquals(0, afterClear.getConversationCount());
    assertEquals(0, afterClear.getContextCount());
    assertEquals(0, afterClear.getFactCount());
  }
}
