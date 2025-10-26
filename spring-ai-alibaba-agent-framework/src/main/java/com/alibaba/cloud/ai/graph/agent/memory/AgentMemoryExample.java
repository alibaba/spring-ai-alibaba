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

import com.alibaba.cloud.ai.graph.store.stores.InMemoryStore;
import java.util.*;
import org.springframework.ai.chat.messages.*;

/**
 * Example demonstrating comprehensive Agent Memory usage.
 *
 * <p>This example shows how to use the Agent Memory API for storing and retrieving different types
 * of information across agent conversations.
 *
 * @author Spring AI Alibaba
 */
public class AgentMemoryExample {

  public static void main(String[] args) {
    demonstrateMemoryUsage();
  }

  public static void demonstrateMemoryUsage() {
    // Initialize memory manager with an in-memory store
    InMemoryStore store = new InMemoryStore();
    MemoryManager memoryManager = new MemoryManager(store);

    // Get memory for specific agent
    String agentId = "customer_service_agent";
    AgentMemory agentMemory = memoryManager.getAgentMemory(agentId);

    // Example session
    String sessionId = "user_session_12345";

    // 1. Store conversation history
    System.out.println("=== Storing Conversation History ===");
    storeConversationHistory(agentMemory, sessionId);

    // 2. Store and retrieve context
    System.out.println("\n=== Managing Context ===");
    manageContext(agentMemory, sessionId);

    // 3. Store and retrieve facts
    System.out.println("\n=== Managing Facts ===");
    manageFacts(agentMemory, sessionId);

    // 4. Store tool metadata
    System.out.println("\n=== Managing Tool Metadata ===");
    manageTools(agentMemory);

    // 5. Search memories
    System.out.println("\n=== Searching Memories ===");
    searchMemories(agentMemory, sessionId);

    // 6. Get statistics
    System.out.println("\n=== Memory Statistics ===");
    printStatistics(agentMemory, sessionId);

    // 7. Demonstrate multi-agent scenario
    System.out.println("\n=== Multi-Agent Scenario ===");
    demonstrateMultiAgentScenario(memoryManager);
  }

  private static void storeConversationHistory(AgentMemory agentMemory, String sessionId) {
    List<Message> messages =
        Arrays.asList(
            new UserMessage("你好，我想咨询一下我的订单状态"),
            new AssistantMessage("您好！欢迎联系我们的客服。请提供您的订单号。"),
            new UserMessage("我的订单号是 ORD-2025-001"),
            new AssistantMessage("感谢。让我为您查询订单状态..."));

    agentMemory.storeConversationMessages(sessionId, messages);
    System.out.println("✓ Stored " + messages.size() + " conversation messages");

    // Retrieve and display
    List<Message> history = agentMemory.getConversationHistory(sessionId);
    System.out.println("✓ Retrieved " + history.size() + " messages from history");
  }

  private static void manageContext(AgentMemory agentMemory, String sessionId) {
    // Store user context
    Map<String, Object> userContext =
        Map.of(
            "user_id", "user_123",
            "customer_level", "VIP",
            "preferred_language", "Chinese",
            "contact", Map.of("email", "user@example.com", "phone", "+86-1234567890"));

    agentMemory.storeContext(sessionId, "user_context", userContext);
    System.out.println("✓ Stored user context");

    // Store order context
    Map<String, Object> orderContext =
        Map.of(
            "order_id", "ORD-2025-001", "status", "processing", "amount", 999.99, "items_count", 3);

    agentMemory.storeContext(sessionId, "order_context", orderContext);
    System.out.println("✓ Stored order context");

    // Retrieve specific context
    Optional<Map<String, Object>> userCtx = agentMemory.getContext(sessionId, "user_context");
    if (userCtx.isPresent()) {
      System.out.println("✓ Retrieved user context: " + userCtx.get().get("customer_level"));
    }

    // Retrieve all context
    Map<String, Map<String, Object>> allContext = agentMemory.getAllContext(sessionId);
    System.out.println("✓ Total context items: " + allContext.size());
  }

  private static void manageFacts(AgentMemory agentMemory, String sessionId) {
    // Extract and store facts
    Map<String, Object> fact1 =
        Map.of(
            "type", "extracted_info",
            "value", "用户拥有VIP会员资格",
            "confidence", 0.98,
            "source", "system_record");

    agentMemory.storeFact(sessionId, "user_vip_status", fact1);
    System.out.println("✓ Stored fact: user VIP status");

    Map<String, Object> fact2 =
        Map.of(
            "type", "problem_identified",
            "description", "订单延迟交货问题",
            "severity", "high",
            "identified_at", System.currentTimeMillis());

    agentMemory.storeFact(sessionId, "order_issue", fact2);
    System.out.println("✓ Stored fact: order issue");

    // Retrieve all facts
    Map<String, Map<String, Object>> allFacts = agentMemory.getAllFacts(sessionId);
    System.out.println("✓ Total facts: " + allFacts.size());
  }

  private static void manageTools(AgentMemory agentMemory) {
    // Store tool metadata
    Map<String, Object> searchToolMeta =
        Map.of(
            "name",
            "order_search",
            "description",
            "Search for customer orders in the system",
            "parameters",
            List.of("order_id", "date_range"),
            "response_time_ms",
            100);

    agentMemory.storeToolMetadata("order_search", searchToolMeta);
    System.out.println("✓ Stored tool metadata: order_search");

    Map<String, Object> notificationToolMeta =
        Map.of(
            "name",
            "send_notification",
            "description",
            "Send notification to customer",
            "parameters",
            List.of("user_id", "message", "channel"),
            "channels",
            List.of("email", "sms", "push"));

    agentMemory.storeToolMetadata("send_notification", notificationToolMeta);
    System.out.println("✓ Stored tool metadata: send_notification");

    // Retrieve all tools
    Map<String, Map<String, Object>> allTools = agentMemory.getAllToolsMetadata();
    System.out.println("✓ Total tools available: " + allTools.size());
  }

  private static void searchMemories(AgentMemory agentMemory, String sessionId) {
    // Search with simple query
    MemorySearchResult result = agentMemory.search(sessionId, "订单");
    System.out.println("✓ Search results for '订单':");
    System.out.println("  - Conversation results: " + result.getConversationResults().size());
    System.out.println("  - Context results: " + result.getContextResults().size());
    System.out.println("  - Facts results: " + result.getFactResults().size());

    // Advanced search with options
    MemorySearchOptions options =
        MemorySearchOptions.builder()
            .query("VIP")
            .addMemoryType(MemorySearchOptions.MemoryType.FACTS)
            .limit(10)
            .sortByRecency(true)
            .build();

    MemorySearchResult advancedResult = agentMemory.search(sessionId, options);
    System.out.println("✓ Advanced search results: " + advancedResult.getTotalCount());
  }

  private static void printStatistics(AgentMemory agentMemory, String sessionId) {
    MemoryStatistics stats = agentMemory.getStatistics(sessionId);

    System.out.println("Memory Usage Statistics:");
    System.out.println("  - Conversation messages: " + stats.getConversationCount());
    System.out.println("  - Context items: " + stats.getContextCount());
    System.out.println("  - Facts: " + stats.getFactCount());
    System.out.println("  - Tools: " + stats.getToolCount());
    System.out.println("  - Total items: " + stats.getTotalItemCount());
    System.out.println("  - Estimated size: " + stats.getFormattedSize());
  }

  private static void demonstrateMultiAgentScenario(MemoryManager memoryManager) {
    // Create memory for different agents
    AgentMemory analysisAgentMemory = memoryManager.getAgentMemory("analysis_agent");
    AgentMemory executionAgentMemory = memoryManager.getAgentMemory("execution_agent");
    AgentMemory verificationAgentMemory = memoryManager.getAgentMemory("verification_agent");

    String commonSessionId = "workflow_session_456";

    // Analysis agent stores findings
    Map<String, Object> analysisResult =
        Map.of(
            "status", "completed",
            "findings", "Order requires expedited shipping",
            "recommendation", "Apply VIP discount");
    analysisAgentMemory.storeContext(commonSessionId, "analysis_result", analysisResult);
    System.out.println("✓ Analysis agent stored findings");

    // Execution agent stores actions
    Map<String, Object> executionAction =
        Map.of(
            "action",
            "expedited_shipping_applied",
            "timestamp",
            System.currentTimeMillis(),
            "success",
            true);
    executionAgentMemory.storeContext(commonSessionId, "execution_action", executionAction);
    System.out.println("✓ Execution agent stored actions");

    // Verification agent stores verification
    Map<String, Object> verification =
        Map.of(
            "verified",
            true,
            "verification_time",
            System.currentTimeMillis(),
            "verified_by",
            "verification_agent");
    verificationAgentMemory.storeContext(commonSessionId, "verification", verification);
    System.out.println("✓ Verification agent stored verification");

    System.out.println("✓ Multi-agent workflow completed with separate memory contexts");
  }
}
