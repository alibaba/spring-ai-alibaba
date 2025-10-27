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
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;

/**
 * Unit tests for DefaultAgentMemory implementation.
 *
 * @author Spring AI Alibaba
 */
class DefaultAgentMemoryTest {

  private DefaultAgentMemory agentMemory;

  private final String AGENT_ID = "test_agent";

  private final String SESSION_ID = "test_session_1";

  @BeforeEach
  void setUp() {
    InMemoryStore store = new InMemoryStore();
    agentMemory = new DefaultAgentMemory(store, AGENT_ID);
  }

  // Conversation Tests

  @Test
  void testStoreAndRetrieveConversationMessage() {
    // Given
    Message userMessage = new UserMessage("Hello, agent!");

    // When
    agentMemory.storeConversationMessage(SESSION_ID, userMessage);

    // Then
    List<Message> history = agentMemory.getConversationHistory(SESSION_ID);
    assertNotNull(history);
    assertTrue(history.size() > 0, "Should have at least one message");
  }

  @Test
  void testStoreMultipleConversationMessages() {
    // Given
    List<Message> messages =
        Arrays.asList(
            new UserMessage("First message"),
            new AssistantMessage("First response"),
            new UserMessage("Second message"));

    // When
    agentMemory.storeConversationMessages(SESSION_ID, messages);

    // Then
    List<Message> history = agentMemory.getConversationHistory(SESSION_ID);
    assertEquals(3, history.size(), "Should have all three messages");
  }

  @Test
  void testClearConversationHistory() {
    // Given
    agentMemory.storeConversationMessage(SESSION_ID, new UserMessage("Test message"));

    // When
    agentMemory.clearConversationHistory(SESSION_ID);

    // Then
    List<Message> history = agentMemory.getConversationHistory(SESSION_ID);
    assertTrue(history.isEmpty(), "Conversation history should be empty");
  }

  // Context Tests

  @Test
  void testStoreAndRetrieveContext() {
    // Given
    String contextKey = "task_context";
    Map<String, Object> contextData =
        Map.of(
            "task_id", "task_123",
            "priority", "high",
            "deadline", "2025-12-31");

    // When
    agentMemory.storeContext(SESSION_ID, contextKey, contextData);

    // Then
    Optional<Map<String, Object>> retrieved = agentMemory.getContext(SESSION_ID, contextKey);
    assertTrue(retrieved.isPresent(), "Context should be found");
    assertEquals(contextData, retrieved.get(), "Context data should match");
  }

  @Test
  void testGetAllContext() {
    // Given
    String contextKey1 = "context_1";
    String contextKey2 = "context_2";
    Map<String, Object> contextData1 = Map.of("key", "value1");
    Map<String, Object> contextData2 = Map.of("key", "value2");

    // When
    agentMemory.storeContext(SESSION_ID, contextKey1, contextData1);
    agentMemory.storeContext(SESSION_ID, contextKey2, contextData2);

    // Then
    Map<String, Map<String, Object>> allContext = agentMemory.getAllContext(SESSION_ID);
    assertEquals(2, allContext.size(), "Should have two context items");
    assertTrue(allContext.containsKey(contextKey1), "Should contain first context");
    assertTrue(allContext.containsKey(contextKey2), "Should contain second context");
  }

  @Test
  void testUpdateContext() {
    // Given
    String contextKey = "user_profile";
    Map<String, Object> initialData = Map.of("name", "John", "age", 30);
    Map<String, Object> updatedData = Map.of("name", "John", "age", 31);

    // When
    agentMemory.storeContext(SESSION_ID, contextKey, initialData);
    agentMemory.updateContext(SESSION_ID, contextKey, updatedData);

    // Then
    Optional<Map<String, Object>> retrieved = agentMemory.getContext(SESSION_ID, contextKey);
    assertTrue(retrieved.isPresent());
    assertEquals(31, retrieved.get().get("age"), "Age should be updated");
  }

  @Test
  void testDeleteContext() {
    // Given
    String contextKey = "temporary_context";
    agentMemory.storeContext(SESSION_ID, contextKey, Map.of("data", "value"));

    // When
    boolean deleted = agentMemory.deleteContext(SESSION_ID, contextKey);

    // Then
    assertTrue(deleted, "Context should be deleted successfully");
    Optional<Map<String, Object>> retrieved = agentMemory.getContext(SESSION_ID, contextKey);
    assertFalse(retrieved.isPresent(), "Deleted context should not be found");
  }

  @Test
  void testClearContext() {
    // Given
    agentMemory.storeContext(SESSION_ID, "context1", Map.of("data", "value1"));
    agentMemory.storeContext(SESSION_ID, "context2", Map.of("data", "value2"));

    // When
    agentMemory.clearContext(SESSION_ID);

    // Then
    Map<String, Map<String, Object>> allContext = agentMemory.getAllContext(SESSION_ID);
    assertTrue(allContext.isEmpty(), "All context should be cleared");
  }

  // Facts Tests

  @Test
  void testStoreAndRetrieveFact() {
    // Given
    String factKey = "user_name";
    Map<String, Object> factData = Map.of("first_name", "John", "last_name", "Doe");

    // When
    agentMemory.storeFact(SESSION_ID, factKey, factData);

    // Then
    Optional<Map<String, Object>> retrieved = agentMemory.getFact(SESSION_ID, factKey);
    assertTrue(retrieved.isPresent(), "Fact should be found");
    assertEquals(factData, retrieved.get(), "Fact data should match");
  }

  @Test
  void testGetAllFacts() {
    // Given
    agentMemory.storeFact(SESSION_ID, "fact1", Map.of("data", "value1"));
    agentMemory.storeFact(SESSION_ID, "fact2", Map.of("data", "value2"));
    agentMemory.storeFact(SESSION_ID, "fact3", Map.of("data", "value3"));

    // When
    Map<String, Map<String, Object>> allFacts = agentMemory.getAllFacts(SESSION_ID);

    // Then
    assertEquals(3, allFacts.size(), "Should have three facts");
  }

  @Test
  void testDeleteFact() {
    // Given
    String factKey = "to_delete";
    agentMemory.storeFact(SESSION_ID, factKey, Map.of("data", "value"));

    // When
    boolean deleted = agentMemory.deleteFact(SESSION_ID, factKey);

    // Then
    assertTrue(deleted, "Fact should be deleted successfully");
    Optional<Map<String, Object>> retrieved = agentMemory.getFact(SESSION_ID, factKey);
    assertFalse(retrieved.isPresent(), "Deleted fact should not be found");
  }

  @Test
  void testClearFacts() {
    // Given
    agentMemory.storeFact(SESSION_ID, "fact1", Map.of("data", "value1"));
    agentMemory.storeFact(SESSION_ID, "fact2", Map.of("data", "value2"));

    // When
    agentMemory.clearFacts(SESSION_ID);

    // Then
    Map<String, Map<String, Object>> allFacts = agentMemory.getAllFacts(SESSION_ID);
    assertTrue(allFacts.isEmpty(), "All facts should be cleared");
  }

  // Tools Tests

  @Test
  void testStoreAndRetrieveToolMetadata() {
    // Given
    String toolName = "web_search";
    Map<String, Object> toolMetadata =
        Map.of(
            "description", "Search the web for information", "params", List.of("query", "limit"));

    // When
    agentMemory.storeToolMetadata(toolName, toolMetadata);

    // Then
    Optional<Map<String, Object>> retrieved = agentMemory.getToolMetadata(toolName);
    assertTrue(retrieved.isPresent(), "Tool metadata should be found");
    assertEquals(toolMetadata, retrieved.get(), "Tool metadata should match");
  }

  @Test
  void testGetAllToolsMetadata() {
    // Given
    agentMemory.storeToolMetadata("tool1", Map.of("desc", "Tool 1"));
    agentMemory.storeToolMetadata("tool2", Map.of("desc", "Tool 2"));

    // When
    Map<String, Map<String, Object>> allTools = agentMemory.getAllToolsMetadata();

    // Then
    assertEquals(2, allTools.size(), "Should have two tools");
  }

  // Search Tests

  @Test
  void testSearchMemory() {
    // Given
    agentMemory.storeConversationMessage(SESSION_ID, new UserMessage("Hello world"));
    agentMemory.storeContext(SESSION_ID, "context", Map.of("topic", "greeting"));
    agentMemory.storeFact(SESSION_ID, "greeting_time", Map.of("time", "morning"));

    // When
    MemorySearchResult result = agentMemory.search(SESSION_ID, "hello");

    // Then
    assertNotNull(result);
    assertTrue(
        result.hasResults() || result.getTotalCount() == 0, "Search should complete without error");
  }

  @Test
  void testSearchMemoryWithOptions() {
    // Given
    agentMemory.storeContext(SESSION_ID, "context_key", Map.of("data", "test_data"));
    MemorySearchOptions options =
        MemorySearchOptions.builder()
            .query("test")
            .addMemoryType(MemorySearchOptions.MemoryType.CONTEXT)
            .limit(10)
            .build();

    // When
    MemorySearchResult result = agentMemory.search(SESSION_ID, options);

    // Then
    assertNotNull(result);
    assertEquals(0, result.getConversationResults().size(), "Should not have conversation results");
  }

  // Statistics Tests

  @Test
  void testGetMemoryStatistics() {
    // Given
    agentMemory.storeConversationMessage(SESSION_ID, new UserMessage("Test"));
    agentMemory.storeContext(SESSION_ID, "ctx", Map.of("data", "value"));
    agentMemory.storeFact(SESSION_ID, "fact", Map.of("data", "value"));

    // When
    MemoryStatistics stats = agentMemory.getStatistics(SESSION_ID);

    // Then
    assertNotNull(stats);
    assertTrue(stats.getTotalItemCount() > 0, "Should have items in memory");
    assertTrue(stats.getEstimatedSizeBytes() > 0, "Should have estimated size");
  }

  @Test
  void testFormattedMemorySize() {
    // Given
    MemoryStatistics stats = new MemoryStatistics();
    stats.setEstimatedSizeBytes(1024);

    // When
    String formatted = stats.getFormattedSize();

    // Then
    assertNotNull(formatted);
    assertTrue(formatted.contains("KB"), "Should be formatted in KB");
  }

  // Clear All Tests

  @Test
  void testClearAllMemory() {
    // Given
    agentMemory.storeConversationMessage(SESSION_ID, new UserMessage("Test"));
    agentMemory.storeContext(SESSION_ID, "ctx", Map.of("data", "value"));
    agentMemory.storeFact(SESSION_ID, "fact", Map.of("data", "value"));

    // When
    agentMemory.clearAllMemory(SESSION_ID);

    // Then
    List<Message> history = agentMemory.getConversationHistory(SESSION_ID);
    Map<String, Map<String, Object>> context = agentMemory.getAllContext(SESSION_ID);
    Map<String, Map<String, Object>> facts = agentMemory.getAllFacts(SESSION_ID);

    assertTrue(history.isEmpty(), "Conversation should be cleared");
    assertTrue(context.isEmpty(), "Context should be cleared");
    assertTrue(facts.isEmpty(), "Facts should be cleared");
  }

  // Helper Tests

  @Test
  void testMultipleSessionsIndependent() {
    // Given
    String sessionId2 = "test_session_2";
    agentMemory.storeContext(SESSION_ID, "key1", Map.of("session", "1"));
    agentMemory.storeContext(sessionId2, "key2", Map.of("session", "2"));

    // When
    Map<String, Map<String, Object>> context1 = agentMemory.getAllContext(SESSION_ID);
    Map<String, Map<String, Object>> context2 = agentMemory.getAllContext(sessionId2);

    // Then
    assertEquals(1, context1.size(), "Session 1 should have one context");
    assertEquals(1, context2.size(), "Session 2 should have one context");
    assertTrue(context1.containsKey("key1"));
    assertTrue(context2.containsKey("key2"));
  }
}
