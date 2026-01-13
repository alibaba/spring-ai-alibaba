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
package com.alibaba.cloud.ai.graph.agent.interceptors;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.interceptor.ToolCallHandler;
import com.alibaba.cloud.ai.graph.agent.interceptor.ToolCallRequest;
import com.alibaba.cloud.ai.graph.agent.interceptor.ToolCallResponse;
import com.alibaba.cloud.ai.graph.agent.interceptor.ToolInterceptor;
import com.alibaba.cloud.ai.graph.agent.node.AgentToolNode;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ToolExecutionContextPropagationTest {

    @Test
    void toolInterceptorShouldSeeThreadIdAndState() throws Exception {
        // Build an AssistantMessage with one tool call.
        AssistantMessage.ToolCall toolCall = new AssistantMessage.ToolCall("call-1", "tool", "demo_tool", "{}");
        Message assistantMessage = AssistantMessage.builder().content("").toolCalls(List.of(toolCall)).build();

        // Create initial data with messages (data() returns unmodifiable map, so we initialize via constructor)
        Map<String, Object> initialData = new HashMap<>();
        initialData.put("messages", new ArrayList<>(List.of(assistantMessage)));
        OverAllState state = new OverAllState(initialData);

        RunnableConfig config = RunnableConfig.builder().threadId("t-1").build();

        CapturingToolInterceptor interceptor = new CapturingToolInterceptor();

        // Configure AgentToolNode with a dummy tool resolver (we won't reach tool execution).
        AgentToolNode node = AgentToolNode.builder().agentName("test").enableActingLog(false).build();
        node.setToolCallbacks(List.of());
        node.setToolInterceptors(List.of(interceptor));

        try {
            node.apply(state, config);
        } catch (IllegalStateException ex) {
            // Expected. There's no ToolCallback for demo_tool; we only care about interceptor seeing the context.
        }

        ToolCallRequest captured = interceptor.captured;
        assertNotNull(captured);
        assertTrue(captured.getExecutionContext().isPresent());
        assertEquals("t-1", captured.getExecutionContext().flatMap(ctx -> ctx.threadId()).orElse(null));
        assertTrue(captured.getExecutionContext().map(ctx -> ctx.state() == state).orElse(false));
    }

    static class CapturingToolInterceptor extends ToolInterceptor {
        ToolCallRequest captured;

        @Override
        public String getName() {
            return "capturing-tool-interceptor";
        }

        @Override
        public ToolCallResponse interceptToolCall(ToolCallRequest request, ToolCallHandler handler) {
            this.captured = request;
            return handler.call(request);
        }
    }
}
