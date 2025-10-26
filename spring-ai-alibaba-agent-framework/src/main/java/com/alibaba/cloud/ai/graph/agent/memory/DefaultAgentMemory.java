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
import com.alibaba.cloud.ai.graph.store.StoreItem;
import com.alibaba.cloud.ai.graph.store.StoreSearchRequest;
import com.alibaba.cloud.ai.graph.store.StoreSearchResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;

/**
 * Default implementation of AgentMemory using the Store interface.
 *
 * <p>This implementation stores agent memory in a hierarchical Store with the following namespace
 * structure: - agents/{agentId}/conversation/{sessionId} - conversation messages -
 * agents/{agentId}/context/{sessionId} - context data - agents/{agentId}/facts/{sessionId} - facts
 * - agents/{agentId}/tools - global tools information
 *
 * @author Spring AI Alibaba
 * @since 1.1.0.0
 */
public class DefaultAgentMemory implements AgentMemory {

  private final Store store;

  private final String agentId;

  private final ObjectMapper objectMapper;

  private static final String CONVERSATION_TYPE = "conversation";

  private static final String CONTEXT_TYPE = "context";

  private static final String FACTS_TYPE = "facts";

  private static final String TOOLS_TYPE = "tools";

  /**
   * Constructor for DefaultAgentMemory.
   *
   * @param store the underlying Store instance
   * @param agentId the agent ID
   */
  public DefaultAgentMemory(Store store, String agentId) {
    this.store = store;
    this.agentId = agentId;
    this.objectMapper = new ObjectMapper();
  }

  // Conversation Message Methods

  @Override
  public void storeConversationMessage(String sessionId, Message message) {
    storeConversationMessages(sessionId, Collections.singletonList(message));
  }

  @Override
  public void storeConversationMessages(String sessionId, List<Message> messages) {
    if (messages == null || messages.isEmpty()) {
      return;
    }

    // Store each message with a timestamp-based key
    for (Message message : messages) {
      long timestamp = System.currentTimeMillis();
      String messageKey =
          String.format("msg_%d_%s", timestamp, UUID.randomUUID().toString().substring(0, 8));

      Map<String, Object> messageData = new HashMap<>();
      messageData.put("role", message.getMessageType().name());
      messageData.put("content", message.getText());
      messageData.put("timestamp", timestamp);
      messageData.put("headers", message.getMetadata());

      StoreItem item = StoreItem.of(buildConversationNamespace(sessionId), messageKey, messageData);
      store.putItem(item);
    }
  }

  @Override
  public List<Message> getConversationHistory(String sessionId) {
    return getConversationHistory(sessionId, 100, 0);
  }

  @Override
  public List<Message> getConversationHistory(String sessionId, int limit, int offset) {
    StoreSearchRequest searchRequest =
        StoreSearchRequest.builder()
            .namespace(buildConversationNamespace(sessionId))
            .limit(limit)
            .offset(offset)
            .sortFields(Collections.singletonList("createdAt"))
            .ascending(true)
            .build();

    StoreSearchResult result = store.searchItems(searchRequest);
    List<Message> messages = new ArrayList<>();

    for (StoreItem item : result.getItems()) {
      try {
        Message message = reconstructMessage(item.getValue());
        if (message != null) {
          messages.add(message);
        }
      } catch (Exception e) {
        // Log and skip invalid messages
        System.err.println("Failed to reconstruct message from item: " + item.getKey());
      }
    }

    return messages;
  }

  @Override
  public void clearConversationHistory(String sessionId) {
    List<Message> history = getConversationHistory(sessionId, Integer.MAX_VALUE, 0);
    StoreSearchRequest searchRequest =
        StoreSearchRequest.builder()
            .namespace(buildConversationNamespace(sessionId))
            .limit(Integer.MAX_VALUE)
            .build();

    StoreSearchResult result = store.searchItems(searchRequest);
    for (StoreItem item : result.getItems()) {
      store.deleteItem(item.getNamespace(), item.getKey());
    }
  }

  // Context Methods

  @Override
  public void storeContext(String sessionId, String contextKey, Map<String, Object> contextData) {
    StoreItem item = StoreItem.of(buildContextNamespace(sessionId), contextKey, contextData);
    store.putItem(item);
  }

  @Override
  public Optional<Map<String, Object>> getContext(String sessionId, String contextKey) {
    Optional<StoreItem> item = store.getItem(buildContextNamespace(sessionId), contextKey);
    return item.map(StoreItem::getValue);
  }

  @Override
  public Map<String, Map<String, Object>> getAllContext(String sessionId) {
    StoreSearchRequest searchRequest =
        StoreSearchRequest.builder()
            .namespace(buildContextNamespace(sessionId))
            .limit(Integer.MAX_VALUE)
            .build();

    StoreSearchResult result = store.searchItems(searchRequest);
    Map<String, Map<String, Object>> contextMap = new HashMap<>();

    for (StoreItem item : result.getItems()) {
      contextMap.put(item.getKey(), item.getValue());
    }

    return contextMap;
  }

  @Override
  public void updateContext(String sessionId, String contextKey, Map<String, Object> contextData) {
    storeContext(sessionId, contextKey, contextData);
  }

  @Override
  public boolean deleteContext(String sessionId, String contextKey) {
    return store.deleteItem(buildContextNamespace(sessionId), contextKey);
  }

  @Override
  public void clearContext(String sessionId) {
    StoreSearchRequest searchRequest =
        StoreSearchRequest.builder()
            .namespace(buildContextNamespace(sessionId))
            .limit(Integer.MAX_VALUE)
            .build();

    StoreSearchResult result = store.searchItems(searchRequest);
    for (StoreItem item : result.getItems()) {
      store.deleteItem(item.getNamespace(), item.getKey());
    }
  }

  // Facts Methods

  @Override
  public void storeFact(String sessionId, String factKey, Map<String, Object> factData) {
    StoreItem item = StoreItem.of(buildFactsNamespace(sessionId), factKey, factData);
    store.putItem(item);
  }

  @Override
  public Optional<Map<String, Object>> getFact(String sessionId, String factKey) {
    Optional<StoreItem> item = store.getItem(buildFactsNamespace(sessionId), factKey);
    return item.map(StoreItem::getValue);
  }

  @Override
  public Map<String, Map<String, Object>> getAllFacts(String sessionId) {
    StoreSearchRequest searchRequest =
        StoreSearchRequest.builder()
            .namespace(buildFactsNamespace(sessionId))
            .limit(Integer.MAX_VALUE)
            .build();

    StoreSearchResult result = store.searchItems(searchRequest);
    Map<String, Map<String, Object>> factsMap = new HashMap<>();

    for (StoreItem item : result.getItems()) {
      factsMap.put(item.getKey(), item.getValue());
    }

    return factsMap;
  }

  @Override
  public boolean deleteFact(String sessionId, String factKey) {
    return store.deleteItem(buildFactsNamespace(sessionId), factKey);
  }

  @Override
  public void clearFacts(String sessionId) {
    StoreSearchRequest searchRequest =
        StoreSearchRequest.builder()
            .namespace(buildFactsNamespace(sessionId))
            .limit(Integer.MAX_VALUE)
            .build();

    StoreSearchResult result = store.searchItems(searchRequest);
    for (StoreItem item : result.getItems()) {
      store.deleteItem(item.getNamespace(), item.getKey());
    }
  }

  // Tools Methods

  @Override
  public void storeToolMetadata(String toolName, Map<String, Object> toolMetadata) {
    StoreItem item = StoreItem.of(buildToolsNamespace(), toolName, toolMetadata);
    store.putItem(item);
  }

  @Override
  public Optional<Map<String, Object>> getToolMetadata(String toolName) {
    Optional<StoreItem> item = store.getItem(buildToolsNamespace(), toolName);
    return item.map(StoreItem::getValue);
  }

  @Override
  public Map<String, Map<String, Object>> getAllToolsMetadata() {
    StoreSearchRequest searchRequest =
        StoreSearchRequest.builder()
            .namespace(buildToolsNamespace())
            .limit(Integer.MAX_VALUE)
            .build();

    StoreSearchResult result = store.searchItems(searchRequest);
    Map<String, Map<String, Object>> toolsMap = new HashMap<>();

    for (StoreItem item : result.getItems()) {
      toolsMap.put(item.getKey(), item.getValue());
    }

    return toolsMap;
  }

  // Search Methods

  @Override
  public MemorySearchResult search(String sessionId, String query) {
    return search(sessionId, MemorySearchOptions.builder().query(query).build());
  }

  @Override
  public MemorySearchResult search(String sessionId, MemorySearchOptions searchOptions) {
    List<MemorySearchOptions.MemoryType> memoryTypes = searchOptions.getMemoryTypes();

    // If no specific types selected, search all
    if (memoryTypes.isEmpty()) {
      memoryTypes =
          Arrays.asList(
              MemorySearchOptions.MemoryType.CONVERSATION,
              MemorySearchOptions.MemoryType.CONTEXT,
              MemorySearchOptions.MemoryType.FACTS,
              MemorySearchOptions.MemoryType.TOOLS);
    }

    List<MemorySearchResult.MemoryItem> conversationResults = new ArrayList<>();
    List<MemorySearchResult.MemoryItem> contextResults = new ArrayList<>();
    List<MemorySearchResult.MemoryItem> factResults = new ArrayList<>();
    List<MemorySearchResult.MemoryItem> toolResults = new ArrayList<>();

    for (MemorySearchOptions.MemoryType type : memoryTypes) {
      List<MemorySearchResult.MemoryItem> results = searchByType(sessionId, searchOptions, type);

      switch (type) {
        case CONVERSATION:
          conversationResults.addAll(results);
          break;
        case CONTEXT:
          contextResults.addAll(results);
          break;
        case FACTS:
          factResults.addAll(results);
          break;
        case TOOLS:
          toolResults.addAll(results);
          break;
      }
    }

    return new MemorySearchResult(conversationResults, contextResults, factResults, toolResults);
  }

  private List<MemorySearchResult.MemoryItem> searchByType(
      String sessionId, MemorySearchOptions searchOptions, MemorySearchOptions.MemoryType type) {
    List<String> namespace;
    MemorySearchOptions.MemoryType itemType = type;

    switch (type) {
      case CONVERSATION:
        namespace = buildConversationNamespace(sessionId);
        break;
      case CONTEXT:
        namespace = buildContextNamespace(sessionId);
        break;
      case FACTS:
        namespace = buildFactsNamespace(sessionId);
        break;
      case TOOLS:
        namespace = buildToolsNamespace();
        break;
      default:
        return new ArrayList<>();
    }

    StoreSearchRequest storeRequest =
        StoreSearchRequest.builder()
            .namespace(namespace)
            .query(searchOptions.getQuery())
            .limit(searchOptions.getLimit())
            .offset(searchOptions.getOffset())
            .ascending(!searchOptions.isSortByRecency())
            .sortFields(Collections.singletonList("createdAt"))
            .build();

    StoreSearchResult result = store.searchItems(storeRequest);

    return result.getItems().stream()
        .map(
            item ->
                new MemorySearchResult.MemoryItem(
                    item.getKey(), item.getValue(), item.getUpdatedAt(), itemType))
        .collect(Collectors.toList());
  }

  // Statistics Methods

  @Override
  public MemoryStatistics getStatistics(String sessionId) {
    long conversationCount = countItemsInNamespace(buildConversationNamespace(sessionId));
    long contextCount = countItemsInNamespace(buildContextNamespace(sessionId));
    long factCount = countItemsInNamespace(buildFactsNamespace(sessionId));
    long toolCount = countItemsInNamespace(buildToolsNamespace());

    long estimatedSize = estimateMemorySize(sessionId);

    return new MemoryStatistics(
        conversationCount, contextCount, factCount, toolCount, estimatedSize);
  }

  private long countItemsInNamespace(List<String> namespace) {
    StoreSearchRequest searchRequest =
        StoreSearchRequest.builder().namespace(namespace).limit(Integer.MAX_VALUE).build();

    StoreSearchResult result = store.searchItems(searchRequest);
    return result.getTotalCount();
  }

  private long estimateMemorySize(String sessionId) {
    // Estimate by counting total items and assuming average size per item
    long totalItems =
        countItemsInNamespace(buildConversationNamespace(sessionId))
            + countItemsInNamespace(buildContextNamespace(sessionId))
            + countItemsInNamespace(buildFactsNamespace(sessionId));

    // Rough estimate: 1KB average per item
    return totalItems * 1024;
  }

  // Clear All Methods

  @Override
  public void clearAllMemory(String sessionId) {
    clearConversationHistory(sessionId);
    clearContext(sessionId);
    clearFacts(sessionId);
  }

  // Helper Methods

  private List<String> buildConversationNamespace(String sessionId) {
    return List.of("agents", agentId, CONVERSATION_TYPE, sessionId);
  }

  private List<String> buildContextNamespace(String sessionId) {
    return List.of("agents", agentId, CONTEXT_TYPE, sessionId);
  }

  private List<String> buildFactsNamespace(String sessionId) {
    return List.of("agents", agentId, FACTS_TYPE, sessionId);
  }

  private List<String> buildToolsNamespace() {
    return List.of("agents", agentId, TOOLS_TYPE);
  }

  private Message reconstructMessage(Map<String, Object> messageData) {
    try {
      String role = (String) messageData.get("role");
      String content = (String) messageData.get("content");
      @SuppressWarnings("unchecked")
      Map<String, String> headers = (Map<String, String>) messageData.get("headers");

      if (role == null || content == null) {
        return null;
      }

      // Reconstruct message based on role type
      switch (role) {
        case "USER":
          return new UserMessage(content);
        case "ASSISTANT":
          return new AssistantMessage(content);
        default:
          // For other types, default to UserMessage
          return new UserMessage(content);
      }
    } catch (Exception e) {
      return null;
    }
  }

  @Override
  public Store getStore() {
    return store;
  }

  @Override
  public String getAgentId() {
    return agentId;
  }
}
