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
package com.alibaba.cloud.ai.graph.agent.hooks.modelcalllimit;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.graph.CompileConfig;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.hook.modelcalllimit.ModelCallLimitExceededException;
import com.alibaba.cloud.ai.graph.agent.hook.modelcalllimit.ModelCallLimitHook;
import com.alibaba.cloud.ai.graph.checkpoint.config.SaverConfig;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.ai.chat.model.ChatModel;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@EnabledIfEnvironmentVariable(named = "AI_DASHSCOPE_API_KEY", matches = ".+")
public class ModelCallLimitTest {

    private ChatModel chatModel;

    @BeforeEach
    void setUp() {
        // Create DashScopeApi instance using the API key from environment variable
        DashScopeApi dashScopeApi = DashScopeApi.builder().apiKey(System.getenv("AI_DASHSCOPE_API_KEY")).build();

        // Create DashScope ChatModel instance
        this.chatModel = DashScopeChatModel.builder().dashScopeApi(dashScopeApi).build();
    }

    @Test
    public void testThreadLimitWithEndBehavior() throws Exception {
        // 限制 1 次
        ModelCallLimitHook hook = ModelCallLimitHook.builder()
                .threadLimit(1)
                .exitBehavior(ModelCallLimitHook.ExitBehavior.END)
                .build();

        ReactAgent agent = createAgent(hook, "test-agent", chatModel);

        // 第一次调用应该成功
        Optional<OverAllState> result1 = agent.invoke("你好，帮我写一个简单的问候语");
        assertTrue(result1.isPresent(), "第一次调用应该成功");

        // 验证返回的消息中包含限制信息
        Object messagesObj = result1.get().value("messages").get();
        assertNotNull(messagesObj);
        assertTrue(messagesObj instanceof java.util.List);
        
        // 第二次调用应该因为达到限制而结束
        Optional<OverAllState> result2 = agent.invoke("再写一个问候语");
        assertTrue(result2.isPresent(), "第二次调用应该返回结果而不是抛出异常");
    }

    @Test
    public void testRunLimitWithErrorBehavior() {
        // 创建一个限制为1次运行调用的hook，使用ERROR行为
        ModelCallLimitHook hook = ModelCallLimitHook.builder()
                .runLimit(1)
                .exitBehavior(ModelCallLimitHook.ExitBehavior.ERROR)
                .build();

        String threadId = "test-run-error-thread";
        RunnableConfig config = RunnableConfig.builder().threadId(threadId).build();

        try {
            ReactAgent agent = createAgent(hook, "test-agent", chatModel);
            // 第一次调用应该成功
            agent.invoke("你好", config);
            
            // 第二次调用应该抛出异常
            assertThrows(ModelCallLimitExceededException.class, () -> {
                agent.invoke("再一次说你好", config);
            });
        } catch (Exception e) {
            fail("hook异常: " + e.getMessage());
        }
    }

    @Test
    public void testCombinedLimits() throws Exception {
        // 同时设置线程和运行限制
        ModelCallLimitHook hook = ModelCallLimitHook.builder()
                .threadLimit(2)
                .runLimit(3)
                .exitBehavior(ModelCallLimitHook.ExitBehavior.END)
                .build();

        ReactAgent agent = ReactAgent.builder()
                .name("test-agent-combined")
                .model(chatModel)
                .hooks(java.util.Arrays.asList(hook))
                .saver(new MemorySaver())
                .build();

        // 第一次调用
        Optional<OverAllState> result1 = agent.invoke("写一个关于春天的句子");
        assertTrue(result1.isPresent(), "第一次调用应该成功");

        // 第二次调用
        Optional<OverAllState> result2 = agent.invoke("再写一个关于夏天的句子");
        assertTrue(result2.isPresent(), "第二次调用应该成功");

        // 第三次调用应该达到线程限制
        Optional<OverAllState> result3 = agent.invoke("再写一个关于秋天的句子");
        assertTrue(result3.isPresent(), "第三次调用应该返回结果");
    }

    @Test
    public void testNormalExecutionWithoutLimits() throws Exception {
        // true case
        ReactAgent agent = createAgent(ModelCallLimitHook.builder().threadLimit(2).runLimit(2).build(),"test-agent",chatModel);
        Optional<OverAllState> result = agent.invoke("帮我写一个简单的问候语");
        assertTrue(result.isPresent(), "没有限制时应该正常执行");
        assertNotNull(result.get().value("messages").get(), "应该返回消息");
    }

    public ReactAgent createAgent(ModelCallLimitHook hook,String name,ChatModel model) throws GraphStateException {
        return ReactAgent.builder()
            .name(name)
            .model(model)
            .hooks(List.of(hook))
            .saver(new MemorySaver())
            .build();
    }

    private static CompileConfig getCompileConfig() {
        SaverConfig saverConfig = SaverConfig.builder()
            .register(new MemorySaver())
            .build();
        return CompileConfig.builder().saverConfig(saverConfig).build();
    }
}
