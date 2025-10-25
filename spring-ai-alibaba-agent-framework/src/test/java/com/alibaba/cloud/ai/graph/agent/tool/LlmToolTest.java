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

package com.alibaba.cloud.ai.graph.agent.tool;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ToolContext;

import java.util.Map;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

@EnabledIfEnvironmentVariable(named = "AI_DASHSCOPE_API_KEY", matches = ".+")
public class LlmToolTest {

    private ChatModel chatModel;

    private static final Logger log = LoggerFactory.getLogger(LlmToolTest.class);

    private final Consumer<String> emptyConsumer = (s) -> {};

    private final Consumer<String> printConsumer = System.out::println;

    @BeforeEach
    void setUp() {
        DashScopeApi dashScopeApi = DashScopeApi.builder().apiKey(System.getenv("AI_DASHSCOPE_API_KEY")).build();
        this.chatModel = DashScopeChatModel.builder().dashScopeApi(dashScopeApi).build();
    }

    @Test
    public void testBase() {
        LlmTool tool = new LlmTool(Executors.newSingleThreadExecutor(), ChatClient.builder(chatModel).build(), printConsumer);
        ToolContext toolContext = new ToolContext(Map.of(BaseTool.AGENT_NAME, "llm_agent"));
        String result = tool.apply("帮我写一个Hello World程序", toolContext);
        log.info("result: {}", result);
        assert result != null && !result.isEmpty();
    }

    @Test
    public void testException() {
        DashScopeApi dashScopeApi = DashScopeApi.builder().apiKey("sk-invalid").build();
        ChatModel chatModel = DashScopeChatModel.builder().dashScopeApi(dashScopeApi).build();
        LlmTool tool = new LlmTool(Executors.newSingleThreadExecutor(), ChatClient.builder(chatModel).build(), emptyConsumer);
        ToolContext toolContext = new ToolContext(Map.of(BaseTool.AGENT_NAME, "llm_agent"));
        String result = tool.apply("帮我写一个Hello World程序", toolContext);
        log.info("result: {}", result);
        assert result != null && result.startsWith("LLM工具无法使用，原因：") && result.contains("401");
    }

    @Test
    public void testCancel() {
        LlmTool tool = new LlmTool(Executors.newSingleThreadExecutor(), ChatClient.builder(chatModel).build(), emptyConsumer);
        ToolContext toolContext = new ToolContext(Map.of(BaseTool.AGENT_NAME, "llm_agent"));
        new Thread(() -> {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            tool.cancel(toolContext);
        }).start();
        String result = tool.apply("帮我写一个Hello World程序", toolContext);
        log.info("result: {}", result);
        assert result != null && result.startsWith("LLM工具无法使用，原因：") && result.contains("CancellationException");
    }

    @Test
    public void testBaseInAgent() throws Exception {
        LlmTool tool = new LlmTool(Executors.newSingleThreadExecutor(), ChatClient.builder(chatModel).build(), printConsumer);
        ReactAgent agent = ReactAgent.builder()
                .name("llm_agent")
                .model(chatModel)
                .description("LLM Agent")
                .instruction("重要：你的**所有请求**都要直接转发给llm工具")
                .tools(tool.toolCallback())
                .build();
        AssistantMessage message = agent.call("帮我写一个Hello World程序");
        String text = message.getText();
        log.info("result: {}", text);
    }

    @Test
    public void testExceptionInAgent() throws Exception {
        DashScopeApi dashScopeApi = DashScopeApi.builder().apiKey("sk-invalid").build();
        ChatModel chatModel = DashScopeChatModel.builder().dashScopeApi(dashScopeApi).build();
        LlmTool tool = new LlmTool(Executors.newSingleThreadExecutor(), ChatClient.builder(chatModel).build(), emptyConsumer);
        ReactAgent agent = ReactAgent.builder()
                .name("llm_agent")
                .model(this.chatModel)
                .description("LLM Agent")
                .instruction("重要：你的**所有请求**都要直接转发给llm工具")
                .tools(tool.toolCallback())
                .build();
        AssistantMessage message = agent.call("帮我写一个Hello World程序");
        String text = message.getText();
        log.info("result: {}", text);
    }

    @Test
    public void testCancelInAgent() throws Exception {
        LlmTool tool = new LlmTool(Executors.newSingleThreadExecutor(), ChatClient.builder(chatModel).build(), emptyConsumer);
        ReactAgent agent = ReactAgent.builder()
                .name("llm_agent")
                .model(this.chatModel)
                .description("LLM Agent")
                .instruction("重要：你的**所有请求**都要直接转发给llm工具")
                .tools(tool.toolCallback())
                .build();
        new Thread(() -> {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            tool.cancel(new ToolContext(Map.of(BaseTool.AGENT_NAME, "llm_agent")));
        }).start();
        AssistantMessage message = agent.call("帮我写一个Hello World程序");
        String text = message.getText();
        log.info("result: {}", text);
    }

}
