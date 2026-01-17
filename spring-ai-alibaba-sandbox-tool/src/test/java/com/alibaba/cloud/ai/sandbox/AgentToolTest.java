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
package com.alibaba.cloud.ai.sandbox;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import io.agentscope.runtime.sandbox.box.BaseSandbox;
import io.agentscope.runtime.sandbox.box.BrowserSandbox;
import io.agentscope.runtime.sandbox.box.FilesystemSandbox;
import io.agentscope.runtime.sandbox.manager.ManagerConfig;
import io.agentscope.runtime.sandbox.manager.SandboxService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@EnabledIfDockerAvailable
@Testcontainers
@EnabledIfEnvironmentVariable(named = "AI_DASHSCOPE_API_KEY", matches = ".+")
class AgentToolTest {

    private static ChatModel chatModel;

    private static SandboxService sandboxService;

    @BeforeAll
    static void setUp() {
        // Create DashScopeApi instance using the API key from environment variable
        DashScopeApi dashScopeApi = DashScopeApi.builder().apiKey(System.getenv("AI_DASHSCOPE_API_KEY")).build();

        // Create DashScope ChatModel instance
        chatModel = DashScopeChatModel.builder().dashScopeApi(dashScopeApi).build();

        // Initialize SandboxService for agent tool execution
        ManagerConfig managerConfig = ManagerConfig.builder().build();
        sandboxService = new SandboxService(managerConfig);
        sandboxService.start();
    }

    @AfterAll
    static void tearDown() {
        if (sandboxService != null) {
            sandboxService.cleanupAllSandboxes();
        }
    }

    @Test
    public void testBaseSandboxTools() throws Exception{
        BaseSandbox sandbox = new BaseSandbox(sandboxService, "testUser", "testSession");

        ReactAgent baseSandboxToolAgent = ReactAgent.builder()
                .name("tool_test_agent")
                .model(chatModel)
                .description("An agent that can use tools to answer questions.")
                .instruction("You are a intelligent agent that can use tools to perform tasks.")
                .tools(List.of(ToolkitInit.RunPythonCodeTool(sandbox), ToolkitInit.RunShellCommandTool(sandbox)))
                .build();

        try {
            Optional<OverAllState> pythonResult = baseSandboxToolAgent
                    .invoke(new UserMessage("Use the python tool to print 'Hello, World!' and return the message."));

            assertTrue(pythonResult.isPresent(), "Result should be present");

            OverAllState pythonState = pythonResult.get();

            assertTrue(pythonState.value("messages").isPresent(), "Messages should be present in state");

            Object messages = pythonState.value("messages").get();
            assertNotNull(messages, "Messages should not be null");

            System.out.println("=== Python Sandbox Agent Tool Test ===");
            System.out.println(pythonResult.get());


            Optional<OverAllState> shellResult = baseSandboxToolAgent
                    .invoke(new UserMessage("Use the shell tool to execute 'echo Hello from Shell' and return the message."));

            assertTrue(shellResult.isPresent(), "Result should be present");

            OverAllState shellState = shellResult.get();

            assertTrue(shellState.value("messages").isPresent(), "Messages should be present in state");

            Object shellMessages = shellState.value("messages").get();
            assertNotNull(messages, "Messages should not be null");

            System.out.println("=== Shell Sandbox Agent Tool Test ===");
            System.out.println(shellResult.get());
        } catch (java.util.concurrent.CompletionException e) {
            fail("Agent tool execution failed: " + e.getMessage());
        }
    }


    @Test
    public void testBrowserSandboxTools() throws Exception{
        BrowserSandbox sandbox = new BrowserSandbox(sandboxService, "testUser", "testSession");

        ReactAgent browserSandboxToolAgent = ReactAgent.builder()
                .name("tool_test_agent")
                .model(chatModel)
                .description("An agent that can use tools to answer questions.")
                .instruction("You are a intelligent agent that can use tools to perform tasks.")
                .tools(List.of(ToolkitInit.BrowserNavigateTool(sandbox)))
                .build();

        try {
            Optional<OverAllState> browserNavigateResult = browserSandboxToolAgent
                    .invoke(new UserMessage("Use the browser navigate tool to go to 'https://www.baidu.com' and return the page title."));

            assertTrue(browserNavigateResult.isPresent(), "Result should be present");

            OverAllState browserNavigateState = browserNavigateResult.get();

            assertTrue(browserNavigateState.value("messages").isPresent(), "Messages should be present in state");

            Object messages = browserNavigateState.value("messages").get();
            assertNotNull(messages, "Messages should not be null");

            System.out.println("=== Browser Navigate Sandbox Agent Tool Test ===");
            System.out.println(browserNavigateResult.get());

        } catch (java.util.concurrent.CompletionException e) {
            fail("Agent tool execution failed: " + e.getMessage());
        }
    }


    @Test
    public void testFsSandboxTools() throws Exception{
        FilesystemSandbox sandbox = new FilesystemSandbox(sandboxService, "testUser", "testSession");

        ReactAgent fsSandboxToolAgent = ReactAgent.builder()
                .name("tool_test_agent")
                .model(chatModel)
                .description("An agent that can use tools to answer questions.")
                .instruction("You are a intelligent agent that can use tools to perform tasks.")
                .tools(List.of(ToolkitInit.WriteFileTool(sandbox)))
                .build();

        try {
            Optional<OverAllState> writeFileResult = fsSandboxToolAgent
                    .invoke(new UserMessage("Use the write file tool to create a file named 'test.txt' with content 'Hello, Filesystem Sandbox!' and return the file path."));

            assertTrue(writeFileResult.isPresent(), "Result should be present");

            OverAllState writeFileState = writeFileResult.get();

            assertTrue(writeFileState.value("messages").isPresent(), "Messages should be present in state");

            Object messages = writeFileState.value("messages").get();
            assertNotNull(messages, "Messages should not be null");

            System.out.println("=== Write File Sandbox Agent Tool Test ===");
            System.out.println(writeFileResult.get());

        } catch (java.util.concurrent.CompletionException e) {
            fail("Agent tool execution failed: " + e.getMessage());
        }
    }
}
