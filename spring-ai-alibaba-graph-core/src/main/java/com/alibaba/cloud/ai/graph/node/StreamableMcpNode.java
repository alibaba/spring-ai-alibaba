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

package com.alibaba.cloud.ai.graph.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;

/**
 * Streamable MCP Node
 */
public class StreamableMcpNode implements AsyncNodeAction {

    private static final Logger log = LoggerFactory.getLogger(StreamableMcpNode.class);

    private final McpNode mcpNode;
    private final String streamUrl;
    private final StreamFormat format;
    private final HttpClient httpClient;

    private StreamableMcpNode(Builder builder) {
        this.mcpNode = builder.mcpNode;
        this.streamUrl = builder.streamUrl;
        this.format = builder.format;
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .executor(ForkJoinPool.commonPool())
            .build();
    }

    @Override
    public CompletableFuture<Map<String, Object>> apply(OverAllState state) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return mcpNode.apply(state);
            } catch (Exception e) {
                log.error("[StreamableMcpNode] MCP call failed", e);
                Map<String, Object> errorResult = new HashMap<>();
                errorResult.put("error", e.getMessage());
                errorResult.put("exception_type", e.getClass().getSimpleName());
                return errorResult;
            }
        }).thenCompose(mcpResult -> {
            if (mcpResult.containsKey("error") || streamUrl == null) {
                return CompletableFuture.completedFuture(mcpResult);
            }
            return executeStreamRequestAsync(mcpResult);
        });
    }

    private CompletableFuture<Map<String, Object>> executeStreamRequestAsync(Map<String, Object> mcpResult) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(streamUrl))
                .header("Accept", format.getContentType())
                .timeout(Duration.ofSeconds(60))
                .GET()
                .build();

            return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() < 200 || response.statusCode() >= 300) {
                        log.error("[StreamableMcpNode] HTTP error: {} {}", response.statusCode(), response.body());
                        Map<String, Object> errorResult = new HashMap<>(mcpResult);
                        errorResult.put("error", "HTTP " + response.statusCode() + ": " + response.body());
                        return errorResult;
                    }

                    Map<String, Object> result = new HashMap<>();
                    mcpResult.forEach(result::put);
                    result.put("stream_response", response.body());
                    return result;
                })
                .exceptionally(throwable -> {
                    log.error("[StreamableMcpNode] Stream request failed", throwable);
                    Map<String, Object> errorResult = new HashMap<>(mcpResult);
                    errorResult.put("error", throwable.getMessage());
                    errorResult.put("exception_type", throwable.getClass().getSimpleName());
                    return errorResult;
                });
        } catch (Exception e) {
            log.error("[StreamableMcpNode] Failed to create stream request", e);
            Map<String, Object> errorResult = new HashMap<>(mcpResult);
            errorResult.put("error", e.getMessage());
            errorResult.put("exception_type", e.getClass().getSimpleName());
            return CompletableFuture.completedFuture(errorResult);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private McpNode mcpNode;
        private String streamUrl;
        private StreamFormat format = StreamFormat.SSE;

        public Builder mcpNode(McpNode mcpNode) {
            this.mcpNode = mcpNode;
            return this;
        }

        public Builder streamUrl(String streamUrl) {
            this.streamUrl = streamUrl;
            return this;
        }

        public Builder format(StreamFormat format) {
            this.format = format;
            return this;
        }

        public StreamableMcpNode build() {
            if (mcpNode == null) {
                throw new IllegalArgumentException("McpNode is required");
            }
            if (streamUrl != null && !isValidUrl(streamUrl)) {
                throw new IllegalArgumentException("Invalid streamUrl format: " + streamUrl);
            }
            return new StreamableMcpNode(this);
        }

        private boolean isValidUrl(String url) {
            try {
                URI.create(url);
                return url.startsWith("http://") || url.startsWith("https://");
            } catch (Exception e) {
                return false;
            }
        }
    }

    public enum StreamFormat {
        SSE("text/event-stream"),
        JSON_LINES("application/x-ndjson"),
        TEXT_PLAIN("text/plain");

        private final String contentType;

        StreamFormat(String contentType) {
            this.contentType = contentType;
        }

        public String getContentType() {
            return contentType;
        }
    }
}
