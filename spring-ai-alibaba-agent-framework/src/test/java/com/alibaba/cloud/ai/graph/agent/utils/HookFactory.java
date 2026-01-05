package com.alibaba.cloud.ai.graph.agent.utils;
/*
 * Copyright 2024-2026 the original author or authors.
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

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.hook.AgentHook;
import org.springframework.ai.chat.messages.AssistantMessage;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Simple HookFactory to create common hooks
 */
public class HookFactory {
    /**
     * Create a LogAgentHook that logs agent execution before and after
     * @return an AgentHook that logs agent execution
     */
    public static AgentHook createLogAgentHook() {
        return new AgentHook() {
            @Override
            public String getName() {
                return "log_agent_hook";
            }

            @Override
            public CompletableFuture<Map<String, Object>> beforeAgent(OverAllState state, RunnableConfig config) {
                System.out.println("╔════════════════════════════════════════════════════════════════╗");
                System.out.println("║  [LOG HOOK] BEFORE AGENT EXECUTION                             ║");
                System.out.println("╠════════════════════════════════════════════════════════════════╣");
                System.out.println("║  Agent Name: " + (getAgentName() != null ? getAgentName() : "N/A"));
                if (state.value("input").isPresent()) {
                    String input = state.value("input").get().toString();
                    if (input.length() > 50) {
                        input = input.substring(0, 50) + "...";
                    }
                    System.out.println("║  Input: " + input);
                }
                System.out.println("╚════════════════════════════════════════════════════════════════╝");
                return CompletableFuture.completedFuture(java.util.Map.of());
            }

            @Override
            public CompletableFuture<java.util.Map<String, Object>> afterAgent(
                    OverAllState state, RunnableConfig config) {
                System.out.println("╔════════════════════════════════════════════════════════════════╗");
                System.out.println("║  [LOG HOOK] AFTER AGENT EXECUTION                              ║");
                System.out.println("╠════════════════════════════════════════════════════════════════╣");
                System.out.println("║  Agent Name: " + (getAgentName() != null ? getAgentName() : "N/A"));
                // Try to get the output from the agent
                state.data().keySet().stream()
                        .filter(key -> key.endsWith("_output"))
                        .findFirst().flatMap(state::value).ifPresent(output -> {
                            if (output instanceof AssistantMessage) {
                                String response = ((AssistantMessage) output).getText();
                                if (response.length() > 50) {
                                    response = response.substring(0, 50) + "...";
                                }
                                System.out.println("║  Agent Response: " + response);
                            }
                        });
                System.out.println("╚════════════════════════════════════════════════════════════════╝");
                return CompletableFuture.completedFuture(java.util.Map.of());
            }
        };
    }
}
