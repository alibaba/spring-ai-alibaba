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

package com.alibaba.cloud.ai.graph;

import com.alibaba.cloud.ai.agent.loader.AgentLoader;
import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.graph.agent.BaseAgent;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;

import org.springframework.ai.chat.model.ChatModel;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.annotation.Nonnull;

import static java.util.Arrays.stream;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toUnmodifiableMap;

/**
 * Static Agent Loader for programmatically provided agents.
 *
 * <p>This loader takes a static list of pre-created agent instances and makes them available
 * through the AgentLoader interface. Perfect for cases where you already have agent instances and
 * just need a convenient way to wrap them in an AgentLoader.
 *
 * <p>This class is not a Spring component by itself - instances are created programmatically and
 * then registered as beans via factory methods.
 */
@Component
class AgentStaticLoader implements AgentLoader {

  private Map<String, BaseAgent> agents = new ConcurrentHashMap<>();

  public AgentStaticLoader() throws GraphStateException {
    // Create DashScopeApi instance using the API key from environment variable
    DashScopeApi dashScopeApi = DashScopeApi.builder().apiKey(System.getenv("AI_DASHSCOPE_API_KEY")).build();
    // Create DashScope ChatModel instance
    ChatModel chatModel = DashScopeChatModel.builder().dashScopeApi(dashScopeApi).build();

    ReactAgent agent = ReactAgent.builder()
            .name("single_agent")
            .model(chatModel)
            .saver(new MemorySaver())
            .tools(PoetTool.createPoetToolCallback())
            .build();
    this.agents.put("single_agent", agent);
  }

  public AgentStaticLoader(BaseAgent... agents) {
    this.agents = stream(agents).collect(toUnmodifiableMap(BaseAgent::name, identity()));
  }

  @Override
  @Nonnull
  public List<String> listAgents() {
    return agents.keySet().stream().toList();
  }

  @Override
  public BaseAgent loadAgent(String name) {
    if (name == null || name.trim().isEmpty()) {
      throw new IllegalArgumentException("Agent name cannot be null or empty");
    }

    BaseAgent agent = agents.get(name);
    if (agent == null) {
      throw new NoSuchElementException("Agent not found: " + name);
    }

    return agent;
  }
}
