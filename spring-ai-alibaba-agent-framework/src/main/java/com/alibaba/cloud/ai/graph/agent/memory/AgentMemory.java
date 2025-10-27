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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.ai.chat.messages.Message;

/**
 * Interface for agent memory management based on the Store layer.
 *
 * <p>AgentMemory provides a high-level API for agents to manage different types of memory: -
 * Conversation history: stores past interactions and dialogue - Context: stores current task
 * context and relevant information - Facts: stores extracted knowledge and facts - Tools: stores
 * information about available tools and their usage
 *
 * <p>Each agent instance has its own memory namespace, with sub-namespaces for different memory
 * types.
 *
 * <h2>Memory Structure</h2>
 *
 * The hierarchical namespace structure is:
 *
 * <ul>
 *   <li>agents/{agentId}/conversation - conversation history
 *   <li>agents/{agentId}/context - task context
 *   <li>agents/{agentId}/facts - extracted facts
 *   <li>agents/{agentId}/tools - tool information
 * </ul>
 *
 * @author Spring AI Alibaba
 * @since 1.1.0.0
 */
public interface AgentMemory {

  /**
   * Store a conversation message in memory.
   *
   * @param sessionId the session ID or thread ID
   * @param message the message to store
   */
  void storeConversationMessage(String sessionId, Message message);

  /**
   * Store multiple conversation messages in memory.
   *
   * @param sessionId the session ID or thread ID
   * @param messages the messages to store
   */
  void storeConversationMessages(String sessionId, List<Message> messages);

  /**
   * Retrieve conversation history for a specific session.
   *
   * @param sessionId the session ID or thread ID
   * @return list of conversation messages, empty list if none found
   */
  List<Message> getConversationHistory(String sessionId);

  /**
   * Retrieve conversation history with pagination.
   *
   * @param sessionId the session ID or thread ID
   * @param limit maximum number of messages to retrieve
   * @param offset number of messages to skip
   * @return list of conversation messages
   */
  List<Message> getConversationHistory(String sessionId, int limit, int offset);

  /**
   * Clear conversation history for a specific session.
   *
   * @param sessionId the session ID or thread ID
   */
  void clearConversationHistory(String sessionId);

  /**
   * Store context information for a task or session.
   *
   * @param sessionId the session ID or thread ID
   * @param contextKey the context key (e.g., "task_context", "user_profile")
   * @param contextData the context data as a map
   */
  void storeContext(String sessionId, String contextKey, Map<String, Object> contextData);

  /**
   * Retrieve context information for a specific key.
   *
   * @param sessionId the session ID or thread ID
   * @param contextKey the context key
   * @return optional containing the context data if found
   */
  Optional<Map<String, Object>> getContext(String sessionId, String contextKey);

  /**
   * Retrieve all context for a session.
   *
   * @param sessionId the session ID or thread ID
   * @return map of all context data
   */
  Map<String, Map<String, Object>> getAllContext(String sessionId);

  /**
   * Update context information.
   *
   * @param sessionId the session ID or thread ID
   * @param contextKey the context key
   * @param contextData the updated context data
   */
  void updateContext(String sessionId, String contextKey, Map<String, Object> contextData);

  /**
   * Delete context information.
   *
   * @param sessionId the session ID or thread ID
   * @param contextKey the context key to delete
   * @return true if deleted, false if not found
   */
  boolean deleteContext(String sessionId, String contextKey);

  /**
   * Clear all context for a session.
   *
   * @param sessionId the session ID or thread ID
   */
  void clearContext(String sessionId);

  /**
   * Store a fact or extracted knowledge.
   *
   * @param sessionId the session ID or thread ID
   * @param factKey the fact key (e.g., "user_name", "project_deadline")
   * @param factData the fact data as a map
   */
  void storeFact(String sessionId, String factKey, Map<String, Object> factData);

  /**
   * Retrieve a stored fact.
   *
   * @param sessionId the session ID or thread ID
   * @param factKey the fact key
   * @return optional containing the fact data if found
   */
  Optional<Map<String, Object>> getFact(String sessionId, String factKey);

  /**
   * Retrieve all facts for a session.
   *
   * @param sessionId the session ID or thread ID
   * @return map of all facts
   */
  Map<String, Map<String, Object>> getAllFacts(String sessionId);

  /**
   * Delete a stored fact.
   *
   * @param sessionId the session ID or thread ID
   * @param factKey the fact key to delete
   * @return true if deleted, false if not found
   */
  boolean deleteFact(String sessionId, String factKey);

  /**
   * Clear all facts for a session.
   *
   * @param sessionId the session ID or thread ID
   */
  void clearFacts(String sessionId);

  /**
   * Store tool information or metadata.
   *
   * @param toolName the name of the tool
   * @param toolMetadata the tool metadata as a map
   */
  void storeToolMetadata(String toolName, Map<String, Object> toolMetadata);

  /**
   * Retrieve tool metadata.
   *
   * @param toolName the name of the tool
   * @return optional containing the tool metadata if found
   */
  Optional<Map<String, Object>> getToolMetadata(String toolName);

  /**
   * Retrieve all available tools metadata.
   *
   * @return map of all tool metadata
   */
  Map<String, Map<String, Object>> getAllToolsMetadata();

  /**
   * Search memories across different memory types.
   *
   * @param sessionId the session ID or thread ID
   * @param query the search query
   * @return search results
   */
  MemorySearchResult search(String sessionId, String query);

  /**
   * Search memories with advanced options.
   *
   * @param sessionId the session ID or thread ID
   * @param searchOptions the search options
   * @return search results
   */
  MemorySearchResult search(String sessionId, MemorySearchOptions searchOptions);

  /**
   * Get memory statistics for a session.
   *
   * @param sessionId the session ID or thread ID
   * @return memory statistics
   */
  MemoryStatistics getStatistics(String sessionId);

  /**
   * Clear all memory for a session.
   *
   * @param sessionId the session ID or thread ID
   */
  void clearAllMemory(String sessionId);

  /**
   * Get the underlying Store instance.
   *
   * @return the Store instance
   */
  Store getStore();

  /**
   * Get the agent ID associated with this memory.
   *
   * @return the agent ID
   */
  String getAgentId();
}
