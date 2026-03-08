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
import com.sun.net.httpserver.HttpServer;
import io.agentscope.runtime.sandbox.box.BaseSandbox;
import io.agentscope.runtime.sandbox.box.Sandbox;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.tool.ToolCallback;
import io.agentscope.runtime.sandbox.manager.ManagerConfig;
import io.agentscope.runtime.sandbox.manager.SandboxService;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;

class WebFetchToolTest {

    private HttpServer httpServer;

    @AfterEach
    void tearDown() {
        if (httpServer != null) {
            httpServer.stop(0);
        }
    }

    @Test
    void shouldFetchAndTruncateContent() throws Exception {
        startServer("hello-web-fetch", false);
        ToolCallback toolCallback = ToolkitInit.WebFetchTool(createSandbox());

        String input = "{\"url\":\"" + localUrl("/content") + "\",\"maxBytes\":5,\"format\":\"text\"}";
        String output = toolCallback.call(input);
        System.out.println("shouldFetchAndTruncateContent output: " + output);

        assertTrue(output.contains("\"status\":200"), output);
        assertTrue(output.contains("\"truncated\":true"), output);
        assertTrue(output.contains("hello"), output);
    }

    @Test
    void shouldFetchRealContentFromBaidu() {
        ToolCallback toolCallback = ToolkitInit.WebFetchTool(createSandbox());

        String input = "{\"url\":\"https://www.baidu.com\",\"followRedirects\":true,\"timeoutMs\":20000,\"maxBytes\":400000}";
        String output = toolCallback.call(input);
        System.out.println("shouldFetchRealContentFromBaidu output: " + output.substring(0, Math.min(output.length(), 500)));

        String lower = output.toLowerCase(Locale.ROOT);
        assertTrue(lower.contains("\"status\":200") || lower.contains("\"status\":301") || lower.contains("\"status\":302"), output);
        assertTrue(lower.contains("baidu") || output.contains("百度"), output);
    }

    @Test
    void shouldFetchRealContentFromAliyun() {
        ToolCallback toolCallback = ToolkitInit.WebFetchTool(createSandbox());

        String input = "{\"url\":\"https://www.aliyun.com\",\"followRedirects\":true,\"timeoutMs\":20000,\"maxBytes\":400000}";
        String output = toolCallback.call(input);
        System.out.println("shouldFetchRealContentFromAliyun output: " + output.substring(0, Math.min(output.length(), 500)));

        String lower = output.toLowerCase(Locale.ROOT);
        assertTrue(lower.contains("\"status\":200") || lower.contains("\"status\":301") || lower.contains("\"status\":302"), output);
        assertTrue(lower.contains("aliyun") || output.contains("阿里云"), output);
    }

    @Test
    void shouldReturnRedirectBlockedWhenFollowRedirectsDisabled() throws Exception {
        startServer("redirect-target", true);
        ToolCallback toolCallback = ToolkitInit.WebFetchTool(createSandbox());

        String input = "{\"url\":\"" + localUrl("/redirect") + "\",\"followRedirects\":false}";
        String output = toolCallback.call(input);
        System.out.println("shouldReturnRedirectBlockedWhenFollowRedirectsDisabled output: " + output);

        assertTrue(output.contains("REDIRECT_BLOCKED"), output);
    }

    @Test
    void shouldRejectInvalidUrl() {
        ToolCallback toolCallback = ToolkitInit.WebFetchTool(createSandbox());
        String output = toolCallback.call("{\"url\":\"not-a-url\"}");
        System.out.println("shouldRejectInvalidUrl output: " + output);
        assertTrue(output.contains("INVALID_URL"), output);
    }

    @Test
    void shouldRejectUnsupportedScheme() {
        ToolCallback toolCallback = ToolkitInit.WebFetchTool(createSandbox());
        String output = toolCallback.call("{\"url\":\"ftp://example.com/file.txt\"}");
        System.out.println("shouldRejectUnsupportedScheme output: " + output);
        assertTrue(output.contains("INVALID_URL"), output);
    }

    @Test
    void shouldBlockPrivateNetworkAddress() {
        ToolCallback toolCallback = ToolkitInit.WebFetchTool(createSandbox());
        String output = toolCallback.call("{\"url\":\"http://10.10.10.10\"}");
        System.out.println("shouldBlockPrivateNetworkAddress output: " + output);
        assertTrue(output.contains("POLICY_BLOCKED"), output);
    }

    @Test
    void shouldTimeoutWhenServerTooSlow() throws Exception {
        startServer("slow-content", false);
        ToolCallback toolCallback = ToolkitInit.WebFetchTool(createSandbox());

        String input = "{\"url\":\"" + localUrl("/slow") + "\",\"timeoutMs\":10,\"followRedirects\":true}";
        String output = toolCallback.call(input);
        System.out.println("shouldTimeoutWhenServerTooSlow output: " + output);

        assertTrue(output.contains("READ_TIMEOUT") || output.contains("FETCH_FAILED"), output);
    }

    @Test
    void shouldFailWhenRedirectExceedsMaxLimit() throws Exception {
        startServer("redirect-target", true);
        ToolCallback toolCallback = ToolkitInit.WebFetchTool(createSandbox());

        String input = "{\"url\":\"" + localUrl("/redirect")
                + "\",\"followRedirects\":true,\"maxRedirects\":0}";
        String output = toolCallback.call(input);
        System.out.println("shouldFailWhenRedirectExceedsMaxLimit output: " + output);

        assertTrue(output.contains("TOO_MANY_REDIRECTS"), output);
    }

    @Test
    void shouldRejectNonTextContentType() throws Exception {
        startServer("binary", false);
        ToolCallback toolCallback = ToolkitInit.WebFetchTool(createSandbox());

        String input = "{\"url\":\"" + localUrl("/binary") + "\",\"followRedirects\":true}";
        String output = toolCallback.call(input);
        System.out.println("shouldRejectNonTextContentType output: " + output);

        assertTrue(output.contains("UNSUPPORTED_CONTENT_TYPE"), output);
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "AI_DASHSCOPE_API_KEY", matches = ".+")
    void shouldUseReactAgentToFetchAndAnalyzeBaiduHotList() throws Exception {
        String apiKey = System.getenv("AI_DASHSCOPE_API_KEY");
        Assumptions.assumeTrue(apiKey != null && !apiKey.isBlank(), "AI_DASHSCOPE_API_KEY not configured");

        SandboxService sandboxService = new SandboxService(ManagerConfig.builder().build());
        sandboxService.start();

        try {
            DashScopeApi dashScopeApi = DashScopeApi.builder().apiKey(apiKey).build();
            DashScopeChatModel chatModel = DashScopeChatModel.builder().dashScopeApi(dashScopeApi).build();

            BaseSandbox baseSandbox = new BaseSandbox(sandboxService, "testUser", "testSession");
            ReactAgent agent = ReactAgent.builder()
                    .name("web-fetch-baidu-hot-agent")
                    .model(chatModel)
                    .instruction("You must call web_fetch to retrieve pages and then answer in Chinese with concise analysis.")
                    .tools(List.of(ToolkitInit.WebFetchTool(baseSandbox)))
                    .build();

            Optional<OverAllState> result = agent.invoke(new UserMessage(
                    "请使用 web_fetch 抓取 https://top.baidu.com/board?tab=realtime 的内容，提取并总结百度热榜前5条话题，给出简短分析。"));

            assertTrue(result.isPresent(), "Agent result should be present");
            Object messages = result.get().value("messages").orElse("");
            String messagesText = String.valueOf(messages);
            String allMessages = messagesText.toLowerCase(Locale.ROOT);

            System.out.println("========== SANDBOX WEB_FETCH REACT AGENT TRACE ==========");
            System.out.println("Messages: " + messages);
            System.out.println("==========================================================");

            int fetchIndex = allMessages.indexOf("web_fetch");
            if (fetchIndex >= 0) {
                int endIndex = Math.min(allMessages.length(), fetchIndex + 800);
                System.out.println("web_fetch call snippet: " + allMessages.substring(fetchIndex, endIndex));
            }
            int resultIndex = allMessages.indexOf("\"response\":{\"status\":");
            if (resultIndex >= 0) {
                int endIndex = Math.min(allMessages.length(), resultIndex + 800);
                System.out.println("web_fetch result snippet: " + allMessages.substring(resultIndex, endIndex));
            }

            String finalAnswer = extractFinalAssistantAnswer(messagesText);
            System.out.println("FINAL_REACT_AGENT_ANSWER: " + finalAnswer);

            assertTrue(allMessages.contains("web_fetch"), messagesText);
            assertTrue(allMessages.contains("top.baidu.com") || allMessages.contains("百度") || allMessages.contains("热榜"), messagesText);
            assertTrue(allMessages.contains("\"status\":200") || allMessages.contains("\"status\":301") || allMessages.contains("\"status\":302"),
                    messagesText);
            assertTrue(allMessages.contains("前5") || allMessages.contains("top 5") || allMessages.contains("1.") || allMessages.contains("一、"),
                    messagesText);
            assertTrue(finalAnswer != null && !finalAnswer.isBlank(), messagesText);
        }
        finally {
            sandboxService.cleanupAllSandboxes();
        }
    }

    private String extractFinalAssistantAnswer(String messagesText) {
        int textIndex = messagesText.lastIndexOf("textContent=");
        if (textIndex < 0) {
            return "";
        }
        int start = textIndex + "textContent=".length();
        int end = messagesText.indexOf(", metadata=", start);
        if (end < 0) {
            end = messagesText.length();
        }
        return messagesText.substring(start, end).trim();
    }

    private Sandbox createSandbox() {
        return new Sandbox("sandbox-web-fetch", "user", "session", "base", null, Map.of(), false);
    }

    private String localUrl(String path) {
        return "http://127.0.0.1:" + httpServer.getAddress().getPort() + path;
    }

    private void startServer(String body, boolean withRedirect) throws IOException {
        httpServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        httpServer.createContext("/content", exchange -> {
            byte[] payload = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
            exchange.sendResponseHeaders(200, payload.length);
            exchange.getResponseBody().write(payload);
            exchange.close();
        });
        httpServer.createContext("/slow", exchange -> {
            try {
                Thread.sleep(200L);
            }
            catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            byte[] payload = "slow-response".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
            exchange.sendResponseHeaders(200, payload.length);
            exchange.getResponseBody().write(payload);
            exchange.close();
        });
        httpServer.createContext("/binary", exchange -> {
            byte[] payload = new byte[] { 1, 2, 3, 4, 5 };
            exchange.getResponseHeaders().set("Content-Type", "image/png");
            exchange.sendResponseHeaders(200, payload.length);
            exchange.getResponseBody().write(payload);
            exchange.close();
        });
        if (withRedirect) {
            httpServer.createContext("/redirect", exchange -> {
                exchange.getResponseHeaders().set("Location", localUrl("/content"));
                exchange.sendResponseHeaders(302, -1);
                exchange.close();
            });
        }
        httpServer.start();
    }
}
