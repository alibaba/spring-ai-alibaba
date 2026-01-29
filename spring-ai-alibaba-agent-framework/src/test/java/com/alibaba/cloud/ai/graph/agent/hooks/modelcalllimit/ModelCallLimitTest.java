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
package com.alibaba.cloud.ai.graph.agent.hooks.modelcalllimit;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.hook.modelcalllimit.ModelCallLimitExceededException;
import com.alibaba.cloud.ai.graph.agent.hook.modelcalllimit.ModelCallLimitHook;
import com.alibaba.cloud.ai.graph.agent.tools.WeatherTool;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.ai.chat.model.ChatModel;

import java.util.List;
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
    public void testThreadLimitWithErrorBehavior() throws Exception {
        ModelCallLimitHook hook = ModelCallLimitHook.builder()
                .threadLimit(1)
                .exitBehavior(ModelCallLimitHook.ExitBehavior.ERROR)
                .build();

        ReactAgent agent = createAgent(hook, "test-agent", chatModel);

        // 第一次调用，执行第二次推理时报错
        assertThrows(ModelCallLimitExceededException.class, () -> {
            agent.invoke("你好，帮我分两次调用weather工具，查询北京和上海的天气");
        }, "第一次调用应该抛出ModelCallLimitExceededException异常");

        // 第二次调用，正常执行，不受之前影响
        Optional<OverAllState> result2 = agent.invoke("你好");
        assertTrue(result2.isPresent(), "第二次调用应该返回结果而不是抛出异常");
    }

    @Test
    public void testRunLimitWithEndBehavior() throws Exception {
        ModelCallLimitHook hook = ModelCallLimitHook.builder()
                .runLimit(1)
                .exitBehavior(ModelCallLimitHook.ExitBehavior.END)
                .build();

        ReactAgent agent = createAgent(hook, "test-agent", chatModel);

        // END 之前实现是添加 AssistantMessage 提示模型去中止, 此处用例不验证
        // 第一次调用，正常执行，不受之前影响
        Optional<OverAllState> result1 = agent.invoke("你好");
        assertTrue(result1.isPresent(), "第一次调用应该返回结果而不是抛出异常");

        // 第二次调用，正常执行，不会导致异常
        assertDoesNotThrow(() -> {
            agent.invoke("你好，调用weather工具，查询北京的天气");
        });

        // 第三次调用，正常执行，不会导致异常
        assertDoesNotThrow(() -> {
            agent.invoke("你好，调用weather工具，查询上海的天气");
        });
    }

    public ReactAgent createAgent(ModelCallLimitHook hook, String name, ChatModel model) throws GraphStateException {
        return ReactAgent.builder()
                .name(name)
                .model(model)
                .hooks(List.of(hook))
                .tools(WeatherTool.createWeatherTool("weather_tool", new WeatherTool()))
                .enableLogging(true)
                .saver(new MemorySaver())
                .build();
    }

}
