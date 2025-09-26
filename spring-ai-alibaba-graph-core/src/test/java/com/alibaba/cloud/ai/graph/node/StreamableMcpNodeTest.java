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
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class StreamableMcpNodeTest {

    @Mock
    private McpNode mockMcpNode;
    
    private MockWebServer mockServer;
    private AutoCloseable mocks;

    @BeforeEach
    void setUp() throws Exception {
        mocks = MockitoAnnotations.openMocks(this);
        mockServer = new MockWebServer();
        mockServer.start();
    }

    @AfterEach
    void tearDown() throws Exception {
        mockServer.shutdown();
        mocks.close();
    }

    @Test
    void testBuilderValidation() {
        assertThrows(IllegalArgumentException.class, () -> {
            StreamableMcpNode.builder().build();
        });
    }

    @Test
    void testBuilderWithAllOptions() {
        StreamableMcpNode node = StreamableMcpNode.builder()
            .mcpNode(mockMcpNode)
            .streamUrl("http://localhost:8080/stream")
            .format(StreamableMcpNode.StreamFormat.JSON_LINES)
            .build();

        assertNotNull(node);
    }

    @Test
    void testStreamFormatEnum() {
        assertEquals("text/event-stream", StreamableMcpNode.StreamFormat.SSE.getContentType());
        assertEquals("application/x-ndjson", StreamableMcpNode.StreamFormat.JSON_LINES.getContentType());
        assertEquals("text/plain", StreamableMcpNode.StreamFormat.TEXT_PLAIN.getContentType());
    }

    @Test
    void testApplyWithoutStreamUrl() throws Exception {
        // Mock MCP结果
        Map<String, Object> mcpResult = Map.of(
            "messages", List.of("test message"),
            "result", "success"
        );
        when(mockMcpNode.apply(any(OverAllState.class))).thenReturn(mcpResult);

        StreamableMcpNode node = StreamableMcpNode.builder()
            .mcpNode(mockMcpNode)
            .build();

        OverAllState state = new OverAllState();
        CompletableFuture<Map<String, Object>> result = node.apply(state);
        
        Map<String, Object> response = result.get();
        assertEquals("success", response.get("result"));
        assertEquals(List.of("test message"), response.get("messages"));
        assertFalse(response.containsKey("stream_response"));
    }

    @Test
    void testApplyWithStreamUrlSuccess() throws Exception {
        // Mock MCP结果
        Map<String, Object> mcpResult = Map.of("mcp_result", "success");
        when(mockMcpNode.apply(any(OverAllState.class))).thenReturn(mcpResult);

        // Mock HTTP响应
        mockServer.enqueue(new MockResponse()
            .setBody("stream data")
            .setHeader("Content-Type", "text/plain"));

        String streamUrl = mockServer.url("/stream").toString();
        StreamableMcpNode node = StreamableMcpNode.builder()
            .mcpNode(mockMcpNode)
            .streamUrl(streamUrl)
            .format(StreamableMcpNode.StreamFormat.TEXT_PLAIN)
            .build();

        OverAllState state = new OverAllState();
        CompletableFuture<Map<String, Object>> result = node.apply(state);
        
        Map<String, Object> response = result.get();
        assertEquals("success", response.get("mcp_result"));
        assertEquals("stream data", response.get("stream_response"));
    }

    @Test
    void testApplyWithStreamUrlHttpError() throws Exception {
        // Mock MCP结果
        Map<String, Object> mcpResult = Map.of("mcp_result", "success");
        when(mockMcpNode.apply(any(OverAllState.class))).thenReturn(mcpResult);

        // Mock HTTP错误响应
        mockServer.enqueue(new MockResponse().setResponseCode(500));

        String streamUrl = mockServer.url("/stream").toString();
        StreamableMcpNode node = StreamableMcpNode.builder()
            .mcpNode(mockMcpNode)
            .streamUrl(streamUrl)
            .build();

        OverAllState state = new OverAllState();
        CompletableFuture<Map<String, Object>> result = node.apply(state);
        
        Map<String, Object> response = result.get();
        assertTrue(response.containsKey("error"));
        assertTrue(response.get("error").toString().contains("HTTP 500"));
    }

    @Test
    void testApplyWithMcpNodeException() throws Exception {
        // Mock MCP异常
        when(mockMcpNode.apply(any(OverAllState.class)))
            .thenThrow(new RuntimeException("MCP error"));

        StreamableMcpNode node = StreamableMcpNode.builder()
            .mcpNode(mockMcpNode)
            .build();

        OverAllState state = new OverAllState();
        CompletableFuture<Map<String, Object>> result = node.apply(state);
        
        Map<String, Object> response = result.get();
        assertTrue(response.containsKey("error"));
        assertTrue(response.get("error").toString().contains("MCP error"));
    }

    @Test
    void testApplyWithDifferentStreamFormats() throws Exception {
        // Mock MCP结果
        Map<String, Object> mcpResult = Map.of("data", "test");
        when(mockMcpNode.apply(any(OverAllState.class))).thenReturn(mcpResult);

        // 测试SSE格式
        mockServer.enqueue(new MockResponse()
            .setBody("data: sse content")
            .setHeader("Content-Type", "text/event-stream"));

        String streamUrl = mockServer.url("/sse").toString();
        StreamableMcpNode sseNode = StreamableMcpNode.builder()
            .mcpNode(mockMcpNode)
            .streamUrl(streamUrl)
            .format(StreamableMcpNode.StreamFormat.SSE)
            .build();

        OverAllState state = new OverAllState();
        CompletableFuture<Map<String, Object>> result = sseNode.apply(state);
        
        Map<String, Object> response = result.get();
        assertEquals("test", response.get("data"));
        assertEquals("data: sse content", response.get("stream_response"));
    }

    @Test
    void testBuilderWithInvalidStreamUrl() {
        assertThrows(IllegalArgumentException.class, () -> {
            StreamableMcpNode.builder()
                .mcpNode(mockMcpNode)
                .streamUrl("invalid-url")
                .build();
        });
    }

    @Test
    void testApplyWithValidStreamUrl() throws Exception {
        // Mock MCP结果
        Map<String, Object> mcpResult = Map.of("result", "ok");
        when(mockMcpNode.apply(any(OverAllState.class))).thenReturn(mcpResult);

        // Mock HTTP响应
        mockServer.enqueue(new MockResponse()
            .setBody("valid response")
            .setHeader("Content-Type", "text/plain"));

        String streamUrl = mockServer.url("/valid").toString();
        StreamableMcpNode node = StreamableMcpNode.builder()
            .mcpNode(mockMcpNode)
            .streamUrl(streamUrl)
            .build();

        OverAllState state = new OverAllState();
        CompletableFuture<Map<String, Object>> result = node.apply(state);
        
        Map<String, Object> response = result.get();
        assertEquals("ok", response.get("result"));
        assertEquals("valid response", response.get("stream_response"));
    }


}
