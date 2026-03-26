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
package com.alibaba.cloud.ai.graph.agent;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.graph.agent.hook.returndirect.ReturnDirectModelHook;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test case to reproduce issue #4462:
 * In Spring AI Alibaba, @Tool's returnDirect parameter doesn't work (true/false has no difference),
 * but it works in Spring AI.
 *
 * This test verifies that returnDirect works WITHOUT manually adding ReturnDirectModelHook.
 */
@EnabledIfEnvironmentVariable(named = "AI_DASHSCOPE_API_KEY", matches = ".+")
public class Issue4462ReturnDirectReproTest {

    private ChatModel chatModel;

    @BeforeEach
    void setUp() {
        DashScopeApi dashScopeApi = DashScopeApi.builder()
                .apiKey(System.getenv("AI_DASHSCOPE_API_KEY"))
                .build();

        this.chatModel = DashScopeChatModel.builder()
                .dashScopeApi(dashScopeApi)
                .build();
    }

    /**
     * Test that returnDirect=true works WITHOUT manually adding ReturnDirectModelHook.
     * This should reproduce issue #4462.
     *
     * Expected behavior when returnDirect=true:
     * - Tool result is returned directly without another LLM call
     * - Output should contain "RAW_RESULT=42" (raw tool result)
     *
     * Expected behavior when returnDirect=false:
     * - Tool result is processed and explained by LLM
     * - Output should contain explanation like "结果是42" or "和是42"
     */
    @Test
    public void testReturnDirectWithoutHook() throws GraphRunnerException {
        var saver = new MemorySaver();

        // Create agent WITHOUT adding ReturnDirectModelHook
        // This is the bug - returnDirect should work automatically
        var react = ReactAgent.builder()
                .name("testReturnDirectAgent")
                .model(chatModel)
                .saver(saver)
                .tools(ToolCallbacks.from(new CalculatorTools()))
                .systemPrompt("你是一个计算器助手")
                .build();

        System.out.println("\n=== Test: returnDirect=true WITHOUT manually adding ReturnDirectModelHook ===");

        String output = react.call("请调用工具计算 12 + 30").getText();
        System.out.println("ReactAgent Output (returnDirect=true): " + output);

        assertNotNull(output, "Output should not be null");

        // With returnDirect=true, the tool result should be returned directly
        // Expected: "RAW_RESULT=42" or similar raw result
        // Bug: Without ReturnDirectModelHook, LLM will always explain the result
        boolean hasRawResult = output.contains("RAW_RESULT=42");
        boolean hasExplanation = output.contains("42") && !output.startsWith("RAW_RESULT");

        System.out.println("Has raw result (RAW_RESULT=42): " + hasRawResult);
        System.out.println("Has explanation: " + hasExplanation);

        // This assertion will FAIL if the bug exists
        // After fix, it should PASS
        assertTrue(hasRawResult,
            "With returnDirect=true, output should contain RAW_RESULT=42 (direct tool result), " +
            "but got: " + output);

        System.out.println("✓ Test passed: returnDirect works without manual hook");
    }

    /**
     * Test that returnDirect=false works correctly.
     * LLM should explain the result instead of returning raw result.
     */
    @Test
    public void testReturnDirectFalse() throws GraphRunnerException {
        var saver = new MemorySaver();

        // Create agent with returnDirect=false
        var react = ReactAgent.builder()
                .name("testReturnDirectFalseAgent")
                .model(chatModel)
                .saver(saver)
                .tools(ToolCallbacks.from(new CalculatorToolsFalse()))
                .systemPrompt("你是一个计算器助手")
                .build();

        System.out.println("\n=== Test: returnDirect=false ===");

        String output = react.call("请调用工具计算 100 + 200").getText();
        System.out.println("ReactAgent Output (returnDirect=false): " + output);

        assertNotNull(output, "Output should not be null");

        // With returnDirect=false, LLM should explain the result
        assertTrue(output.contains("300") || output.contains("一百"),
            "With returnDirect=false, output should contain explanation, but got: " + output);

        System.out.println("✓ Test passed: returnDirect=false works correctly");
    }

    /**
     * Test with hook - this should always work (baseline test)
     */
    @Test
    public void testReturnDirectWithHook() throws GraphRunnerException {
        var saver = new MemorySaver();

        // Create agent WITH ReturnDirectModelHook (manual workaround)
        var react = ReactAgent.builder()
                .name("testReturnDirectWithHookAgent")
                .model(chatModel)
                .saver(saver)
                .tools(ToolCallbacks.from(new CalculatorTools()))
                .hooks(new ReturnDirectModelHook())
                .systemPrompt("你是一个计算器助手")
                .build();

        System.out.println("\n=== Test: returnDirect=true WITH manually adding ReturnDirectModelHook ===");

        String output = react.call("请调用工具计算 50 + 50").getText();
        System.out.println("ReactAgent Output (with hook): " + output);

        assertNotNull(output, "Output should not be null");

        // With hook, should return direct result
        assertTrue(output.contains("RAW_RESULT=100"),
            "With ReturnDirectModelHook, output should contain RAW_RESULT=100, but got: " + output);

        System.out.println("✓ Test passed: returnDirect works with manual hook (baseline)");
    }

    /**
     * Tools with returnDirect=true
     */
    static class CalculatorTools {
        @Tool(name = "my_add", description = "实现两个整数相加", returnDirect = true)
        public String add(@ToolParam(required = true, description = "必须是整数") int first,
                         @ToolParam(required = true, description = "必须是整数") int second) {
            System.out.println("my_add 被调用了，first=" + first + ",second=" + second);
            return "RAW_RESULT=" + (first + second);
        }
    }

    /**
     * Tools with returnDirect=false
     */
    static class CalculatorToolsFalse {
        @Tool(name = "my_add_false", description = "实现两个整数相加", returnDirect = false)
        public String add(@ToolParam(required = true, description = "必须是整数") int first,
                         @ToolParam(required = true, description = "必须是整数") int second) {
            System.out.println("my_add_false 被调用了，first=" + first + ",second=" + second);
            return "RAW_RESULT=" + (first + second);
        }
    }
}
