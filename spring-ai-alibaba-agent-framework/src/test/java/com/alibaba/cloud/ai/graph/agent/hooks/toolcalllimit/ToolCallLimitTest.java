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
package com.alibaba.cloud.ai.graph.agent.hooks.toolcalllimit;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.hook.toolcalllimit.ToolCallLimitExceededException;
import com.alibaba.cloud.ai.graph.agent.hook.toolcalllimit.ToolCallLimitHook;
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
public class ToolCallLimitTest {

    private ChatModel chatModel;

    @BeforeEach
    void setUp() {
        DashScopeApi dashScopeApi = DashScopeApi.builder().apiKey(System.getenv("AI_DASHSCOPE_API_KEY")).build();
        this.chatModel = DashScopeChatModel.builder().dashScopeApi(dashScopeApi).build();
    }

    @Test
    public void testThreadLimitWithErrorBehavior() throws Exception {
        // 限制 1 次
        ToolCallLimitHook hook = ToolCallLimitHook.builder()
                .threadLimit(2)
                .exitBehavior(ToolCallLimitHook.ExitBehavior.ERROR)
                .build();

        ReactAgent agent = createAgent(hook, "test-agent", chatModel);

        // 第一次调用，执行第二次工具时报错
        assertThrows(ToolCallLimitExceededException.class, () -> {
            agent.invoke("你好，帮我分别调用几次weather工具，查询北京、上海、杭州的天气");
        }, "第一次调用应该抛出ModelCallLimitExceededException异常");

        // 第二次调用，正常执行，不受之前影响
        Optional<OverAllState> result2 = agent.invoke("帮我查询成都天气");
        assertTrue(result2.isPresent(), "第二次调用应该返回结果而不是抛出异常");
    }

    @Test
    public void testRunLimitWithEndBehavior() throws Exception {
        ToolCallLimitHook hook = ToolCallLimitHook.builder()
                .runLimit(1)
                .exitBehavior(ToolCallLimitHook.ExitBehavior.END)
                .build();

        ReactAgent agent = createAgent(hook, "test-agent", chatModel);

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

    public ReactAgent createAgent(ToolCallLimitHook hook, String name, ChatModel model) throws GraphStateException {
        return ReactAgent.builder()
                .name(name)
                .model(model)
                .hooks(List.of(hook))
                .tools(WeatherTool.createWeatherTool("weather_tool", new WeatherTool()))
                .saver(new MemorySaver())
                .build();
    }

    
}
