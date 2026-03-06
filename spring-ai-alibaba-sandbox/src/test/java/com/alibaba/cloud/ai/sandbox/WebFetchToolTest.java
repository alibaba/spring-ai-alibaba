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

import com.sun.net.httpserver.HttpServer;
import io.agentscope.runtime.sandbox.box.Sandbox;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;

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

        assertTrue(output.contains("\"status\":200"), output);
        assertTrue(output.contains("\"truncated\":true"), output);
        assertTrue(output.contains("hello"), output);
    }

    @Test
    void shouldReturnRedirectBlockedWhenFollowRedirectsDisabled() throws Exception {
        startServer("redirect-target", true);
        ToolCallback toolCallback = ToolkitInit.WebFetchTool(createSandbox());

        String input = "{\"url\":\"" + localUrl("/redirect") + "\",\"followRedirects\":false}";
        String output = toolCallback.call(input);

        assertTrue(output.contains("REDIRECT_BLOCKED"), output);
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
