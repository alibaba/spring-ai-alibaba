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

package com.alibaba.cloud.ai.agent.nacos;

import com.alibaba.cloud.ai.agent.nacos.utils.ChatOptionsProxy;
import com.alibaba.cloud.ai.agent.nacos.vo.AgentVO;
import com.alibaba.cloud.ai.agent.nacos.vo.McpServersVO;
import com.alibaba.cloud.ai.agent.nacos.vo.ModelVO;
import com.alibaba.cloud.ai.agent.nacos.vo.PromptVO;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.hook.Hook;
import com.alibaba.cloud.ai.graph.agent.interceptor.Interceptor;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelCallHandler;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelInterceptor;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelRequest;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelResponse;
import com.alibaba.cloud.ai.graph.agent.interceptor.ToolCallHandler;
import com.alibaba.cloud.ai.graph.agent.interceptor.ToolCallRequest;
import com.alibaba.cloud.ai.graph.agent.interceptor.ToolCallResponse;
import com.alibaba.cloud.ai.graph.agent.interceptor.ToolInterceptor;
import com.alibaba.cloud.ai.graph.agent.node.AgentLlmNode;
import com.alibaba.cloud.ai.graph.agent.node.AgentToolNode;
import com.alibaba.cloud.ai.mcp.nacos.service.NacosMcpOperationService;
import com.alibaba.cloud.ai.observation.model.ObservationMetadataAwareOptions;
import com.alibaba.fastjson.JSON;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.client.config.NacosConfigService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

/**
 * Unit tests to verify that interceptors provided via interceptors() method are correctly separated into
 * modelInterceptors and toolInterceptors when using NacosReactAgentBuilder.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NacosReactAgentBuilderInterceptorTest {
    
    @Mock
    private NacosConfigService nacosConfigService;
    
    @Mock
    private NacosMcpOperationService mcpOperationService;
    
    private NacosOptions nacosOptions;
    
    private MockedStatic<ChatOptionsProxy> chatOptionsProxyMockedStatic;
    
    /**
     * Simple ModelInterceptor implementation for testing.
     */
    static class TestModelInterceptor extends ModelInterceptor {
        
        private final String name;
        
        private final List<ToolCallback> tools;
        
        TestModelInterceptor(String name) {
            this.name = name;
            this.tools = Collections.emptyList();
        }
        
        TestModelInterceptor(String name, ToolCallback... tools) {
            this.name = name;
            this.tools = List.of(tools);
        }
        
        @Override
        public String getName() {
            return name;
        }
        
        @Override
        public ModelResponse interceptModel(ModelRequest request, ModelCallHandler handler) {
            return handler.call(request);
        }
        
        @Override
        public List<ToolCallback> getTools() {
            return tools;
        }
    }
    
    /**
     * Simple ToolInterceptor implementation for testing.
     */
    static class TestToolInterceptor extends ToolInterceptor {
        
        private final String name;
        
        TestToolInterceptor(String name) {
            this.name = name;
        }
        
        @Override
        public String getName() {
            return name;
        }
        
        @Override
        public ToolCallResponse interceptToolCall(ToolCallRequest request, ToolCallHandler handler) {
            return handler.call(request);
        }
    }
    
    /**
     * Simple Hook implementation for testing that provides tools.
     */
    static class TestHookWithTools implements Hook {
        
        private final String name;
        
        private final List<ToolCallback> tools;
        
        private String agentName;
        
        private ReactAgent agent;
        
        TestHookWithTools(String name, ToolCallback... tools) {
            this.name = name;
            this.tools = List.of(tools);
        }
        
        @Override
        public String getName() {
            return name;
        }
        
        @Override
        public void setAgentName(String agentName) {
            this.agentName = agentName;
        }
        
        @Override
        public String getAgentName() {
            return agentName;
        }
        
        @Override
        public ReactAgent getAgent() {
            return agent;
        }
        
        @Override
        public void setAgent(ReactAgent agent) {
            this.agent = agent;
        }
        
        @Override
        public List<ToolCallback> getTools() {
            return tools;
        }
        
        @Override
        public int getOrder() {
            return 0;
        }
    }
    
    /**
     * Simple function for creating FunctionToolCallback.
     */
    static class EchoFunction implements Function<String, String> {
        
        @Override
        public String apply(String input) {
            return "Echo: " + input;
        }
    }
    
    /**
     * Create a simple tool callback for testing.
     */
    private static ToolCallback createTestToolCallback(String name, String description) {
        return FunctionToolCallback.builder(name, new EchoFunction()).description(description).inputType(String.class)
                .build();
    }
    
    /**
     * Create mock NacosOptions with mocked services.
     */
    private NacosOptions createMockNacosOptions() throws Exception {
        NacosOptions options = mock(NacosOptions.class);
        when(options.getNacosConfigService()).thenReturn(nacosConfigService);
        when(options.getMcpOperationService()).thenReturn(mcpOperationService);
        when(options.getAgentName()).thenReturn("test-agent");
        when(options.isAgentBaseEncrypted()).thenReturn(false);
        when(options.isPromptEncrypted()).thenReturn(false);
        when(options.isModelEncrypted()).thenReturn(false);
        when(options.isMcpServersEncrypted()).thenReturn(false);
        return options;
    }
    
    /**
     * Setup mock Nacos config responses.
     */
    private void setupMockNacosConfigs() throws NacosException {
        // Mock AgentVO config
        AgentVO agentVO = new AgentVO();
        agentVO.setPromptKey("test-prompt");
        agentVO.setDescription("Test agent description");
        when(nacosConfigService.getConfig(eq("agent-base.json"), anyString(), anyLong())).thenReturn(
                JSON.toJSONString(agentVO));
        
        // Mock PromptVO config
        PromptVO promptVO = new PromptVO();
        promptVO.setPromptKey("test-prompt");
        promptVO.setVersion("1.0");
        promptVO.setTemplate("You are a test assistant");
        when(nacosConfigService.getConfig(eq("prompt-test-prompt.json"), anyString(), anyLong())).thenReturn(
                JSON.toJSONString(promptVO));
        
        // Mock ModelVO config
        ModelVO modelVO = new ModelVO();
        modelVO.setBaseUrl("https://api.openai.com/v1");
        modelVO.setApiKey("test-api-key");
        modelVO.setModel("gpt-4");
        modelVO.setTemperature("0.7");
        when(nacosConfigService.getConfig(eq("model.json"), anyString(), anyLong())).thenReturn(
                JSON.toJSONString(modelVO));
        
        // Mock McpServersVO config - empty MCP servers
        McpServersVO mcpServersVO = new McpServersVO();
        mcpServersVO.setMcpServers(Collections.emptyList());
        when(nacosConfigService.getConfig(eq("mcp-servers.json"), anyString(), anyLong())).thenReturn(
                JSON.toJSONString(mcpServersVO));
    }
    
    @BeforeEach
    void setUp() throws Exception {
        nacosOptions = createMockNacosOptions();
        setupMockNacosConfigs();
        
        // Mock the static ChatOptionsProxy.createProxy method to avoid CGLIB issues
        chatOptionsProxyMockedStatic = mockStatic(ChatOptionsProxy.class);
        chatOptionsProxyMockedStatic.when(() -> ChatOptionsProxy.createProxy(any(ChatOptions.class), anyMap()))
                .thenAnswer(invocation -> {
                    // Return a mock that implements both interfaces
                    OpenAiChatOptions mockOptions = mock(OpenAiChatOptions.class,
                            withSettings().extraInterfaces(ObservationMetadataAwareOptions.class));
                    
                    // Setup basic behavior
                    when(mockOptions.getModel()).thenReturn("gpt-4");
                    when(mockOptions.getTemperature()).thenReturn(0.7);
                    
                    // Setup ObservationMetadataAwareOptions behavior
                    Map<String, String> metadata = new HashMap<>();
                    ObservationMetadataAwareOptions observationOptions = (ObservationMetadataAwareOptions) mockOptions;
                    when(observationOptions.getObservationMetadata()).thenReturn(metadata);
                    
                    return mockOptions;
                });
    }
    
    @AfterEach
    void tearDown() {
        if (chatOptionsProxyMockedStatic != null) {
            chatOptionsProxyMockedStatic.close();
        }
    }
    
    /**
     * Helper method to get llmNode from ReactAgent using reflection.
     */
    private AgentLlmNode getLlmNode(ReactAgent agent) throws Exception {
        Field llmNodeField = ReactAgent.class.getDeclaredField("llmNode");
        llmNodeField.setAccessible(true);
        return (AgentLlmNode) llmNodeField.get(agent);
    }
    
    /**
     * Helper method to get toolNode from ReactAgent using reflection.
     */
    private AgentToolNode getToolNode(ReactAgent agent) throws Exception {
        Field toolNodeField = ReactAgent.class.getDeclaredField("toolNode");
        toolNodeField.setAccessible(true);
        return (AgentToolNode) toolNodeField.get(agent);
    }
    
    /**
     * Helper method to get toolCallbacks from AgentLlmNode using reflection.
     */
    private List<ToolCallback> getToolCallbacks(AgentLlmNode llmNode) throws Exception {
        Field toolCallbacksField = AgentLlmNode.class.getDeclaredField("toolCallbacks");
        toolCallbacksField.setAccessible(true);
        @SuppressWarnings("unchecked") List<ToolCallback> toolCallbacks = (List<ToolCallback>) toolCallbacksField.get(
                llmNode);
        return toolCallbacks;
    }
    
    /**
     * Helper method to get modelInterceptors from Builder using reflection.
     */
    @SuppressWarnings("unchecked")
    private List<ModelInterceptor> getModelInterceptors(NacosReactAgentBuilder builder) throws Exception {
        Field field = builder.getClass().getSuperclass().getSuperclass().getSuperclass()
                .getDeclaredField("modelInterceptors");
        field.setAccessible(true);
        return (List<ModelInterceptor>) field.get(builder);
    }
    
    /**
     * Helper method to get toolInterceptors from Builder using reflection.
     */
    @SuppressWarnings("unchecked")
    private List<ToolInterceptor> getToolInterceptors(NacosReactAgentBuilder builder) throws Exception {
        Field field = builder.getClass().getSuperclass().getSuperclass().getSuperclass()
                .getDeclaredField("toolInterceptors");
        field.setAccessible(true);
        return (List<ToolInterceptor>) field.get(builder);
    }
    
    /**
     * Test case: Using interceptors() with ModelInterceptor only - should be separated correctly
     */
    @Test
    void testWithModelInterceptorOnly_interceptorsSeparatedCorrectly() throws Exception {
        TestModelInterceptor modelInterceptor = new TestModelInterceptor("testModelInterceptor");
        
        NacosReactAgentBuilder builder = new NacosReactAgentBuilder();
        ReactAgent agent = builder.nacosOptions(nacosOptions).name("agent_with_model_interceptor")
                .interceptors(modelInterceptor).build();
        
        // Verify interceptors are separated correctly
        List<ModelInterceptor> modelInterceptors = getModelInterceptors(builder);
        List<ToolInterceptor> toolInterceptors = getToolInterceptors(builder);
        
        assertNotNull(modelInterceptors, "modelInterceptors should not be null");
        assertEquals(1, modelInterceptors.size(), "Should have 1 model interceptor");
        assertEquals("testModelInterceptor", modelInterceptors.get(0).getName(), "Model interceptor name should match");
        
        assertNotNull(toolInterceptors, "toolInterceptors should not be null");
        assertTrue(toolInterceptors.isEmpty(), "Should have 0 tool interceptors");
    }
    
    /**
     * Test case: Using interceptors() with ToolInterceptor only - should be separated correctly
     */
    @Test
    void testWithToolInterceptorOnly_interceptorsSeparatedCorrectly() throws Exception {
        TestToolInterceptor toolInterceptor = new TestToolInterceptor("testToolInterceptor");
        
        NacosReactAgentBuilder builder = new NacosReactAgentBuilder();
        ReactAgent agent = builder.nacosOptions(nacosOptions).name("agent_with_tool_interceptor")
                .interceptors(toolInterceptor).build();
        
        // Verify interceptors are separated correctly
        List<ModelInterceptor> modelInterceptors = getModelInterceptors(builder);
        List<ToolInterceptor> toolInterceptors = getToolInterceptors(builder);
        
        assertNotNull(modelInterceptors, "modelInterceptors should not be null");
        assertTrue(modelInterceptors.isEmpty(), "Should have 0 model interceptors");
        
        assertNotNull(toolInterceptors, "toolInterceptors should not be null");
        assertEquals(1, toolInterceptors.size(), "Should have 1 tool interceptor");
        assertEquals("testToolInterceptor", toolInterceptors.get(0).getName(), "Tool interceptor name should match");
    }
    
    /**
     * Test case: Using interceptors() with both ModelInterceptor and ToolInterceptor - should be separated correctly
     */
    @Test
    void testWithMixedInterceptors_interceptorsSeparatedCorrectly() throws Exception {
        TestModelInterceptor modelInterceptor1 = new TestModelInterceptor("modelInterceptor1");
        TestModelInterceptor modelInterceptor2 = new TestModelInterceptor("modelInterceptor2");
        TestToolInterceptor toolInterceptor1 = new TestToolInterceptor("toolInterceptor1");
        TestToolInterceptor toolInterceptor2 = new TestToolInterceptor("toolInterceptor2");
        
        NacosReactAgentBuilder builder = new NacosReactAgentBuilder();
        ReactAgent agent = builder.nacosOptions(nacosOptions).name("agent_with_mixed_interceptors")
                .interceptors(modelInterceptor1, toolInterceptor1, modelInterceptor2, toolInterceptor2).build();
        
        // Verify interceptors are separated correctly
        List<ModelInterceptor> modelInterceptors = getModelInterceptors(builder);
        List<ToolInterceptor> toolInterceptors = getToolInterceptors(builder);
        
        assertNotNull(modelInterceptors, "modelInterceptors should not be null");
        assertEquals(2, modelInterceptors.size(), "Should have 2 model interceptors");
        assertTrue(modelInterceptors.stream().anyMatch(i -> "modelInterceptor1".equals(i.getName())),
                "Should contain modelInterceptor1");
        assertTrue(modelInterceptors.stream().anyMatch(i -> "modelInterceptor2".equals(i.getName())),
                "Should contain modelInterceptor2");
        
        assertNotNull(toolInterceptors, "toolInterceptors should not be null");
        assertEquals(2, toolInterceptors.size(), "Should have 2 tool interceptors");
        assertTrue(toolInterceptors.stream().anyMatch(i -> "toolInterceptor1".equals(i.getName())),
                "Should contain toolInterceptor1");
        assertTrue(toolInterceptors.stream().anyMatch(i -> "toolInterceptor2".equals(i.getName())),
                "Should contain toolInterceptor2");
    }
    
    /**
     * Test case: Using interceptors() with List parameter - should work correctly
     */
    @Test
    void testWithInterceptorsList_interceptorsSeparatedCorrectly() throws Exception {
        TestModelInterceptor modelInterceptor = new TestModelInterceptor("listModelInterceptor");
        TestToolInterceptor toolInterceptor = new TestToolInterceptor("listToolInterceptor");
        
        List<Interceptor> interceptorList = List.of(modelInterceptor, toolInterceptor);
        
        NacosReactAgentBuilder builder = new NacosReactAgentBuilder();
        ReactAgent agent = builder.nacosOptions(nacosOptions).name("agent_with_interceptors_list")
                .interceptors(interceptorList).build();
        
        // Verify interceptors are separated correctly
        List<ModelInterceptor> modelInterceptors = getModelInterceptors(builder);
        List<ToolInterceptor> toolInterceptors = getToolInterceptors(builder);
        
        assertNotNull(modelInterceptors, "modelInterceptors should not be null");
        assertEquals(1, modelInterceptors.size(), "Should have 1 model interceptor");
        assertEquals("listModelInterceptor", modelInterceptors.get(0).getName(), "Model interceptor name should match");
        
        assertNotNull(toolInterceptors, "toolInterceptors should not be null");
        assertEquals(1, toolInterceptors.size(), "Should have 1 tool interceptor");
        assertEquals("listToolInterceptor", toolInterceptors.get(0).getName(), "Tool interceptor name should match");
    }
    
    /**
     * Test case: ModelInterceptor with tools - tools should be included in agent
     */
    @Test
    void testWithModelInterceptorTools_toolsAreIncluded() throws Exception {
        ToolCallback interceptorTool = createTestToolCallback("interceptorTool", "Interceptor tool");
        TestModelInterceptor modelInterceptor = new TestModelInterceptor("interceptorWithTools", interceptorTool);
        
        NacosReactAgentBuilder builder = new NacosReactAgentBuilder();
        ReactAgent agent = builder.nacosOptions(nacosOptions).name("agent_with_interceptor_tools")
                .interceptors(modelInterceptor).build();
        
        AgentLlmNode llmNode = getLlmNode(agent);
        AgentToolNode toolNode = getToolNode(agent);
        
        // Verify llmNode has interceptor tools
        List<ToolCallback> llmToolCallbacks = getToolCallbacks(llmNode);
        assertNotNull(llmToolCallbacks, "llmNode toolCallbacks should not be null");
        assertEquals(1, llmToolCallbacks.size(), "llmNode should have 1 tool from interceptor");
        assertEquals("interceptorTool", llmToolCallbacks.get(0).getToolDefinition().name(),
                "llmNode should contain interceptorTool");
        
        // Verify toolNode has interceptor tools
        List<ToolCallback> toolNodeCallbacks = toolNode.getToolCallbacks();
        assertNotNull(toolNodeCallbacks, "toolNode toolCallbacks should not be null");
        assertEquals(1, toolNodeCallbacks.size(), "toolNode should have 1 tool from interceptor");
        assertEquals("interceptorTool", toolNodeCallbacks.get(0).getToolDefinition().name(),
                "toolNode should contain interceptorTool");
    }
    
    /**
     * Test case: Combining interceptors with tools and direct tools - all tools should be merged
     */
    @Test
    void testWithInterceptorToolsAndDirectTools_toolsAreMerged() throws Exception {
        ToolCallback interceptorTool = createTestToolCallback("interceptorTool", "Interceptor tool");
        TestModelInterceptor modelInterceptor = new TestModelInterceptor("interceptorWithTools", interceptorTool);
        ToolCallback directTool = createTestToolCallback("directTool", "Direct tool");
        
        NacosReactAgentBuilder builder = new NacosReactAgentBuilder();
        ReactAgent agent = builder.nacosOptions(nacosOptions).name("agent_with_interceptor_and_direct_tools")
                .interceptors(modelInterceptor).tools(directTool).build();
        
        AgentLlmNode llmNode = getLlmNode(agent);
        List<ToolCallback> llmToolCallbacks = getToolCallbacks(llmNode);
        
        assertNotNull(llmToolCallbacks, "llmNode toolCallbacks should not be null");
        assertEquals(2, llmToolCallbacks.size(), "llmNode should have 2 tools (interceptor + direct)");
        assertTrue(llmToolCallbacks.stream().anyMatch(t -> "interceptorTool".equals(t.getToolDefinition().name())),
                "llmNode should contain interceptorTool from interceptor");
        assertTrue(llmToolCallbacks.stream().anyMatch(t -> "directTool".equals(t.getToolDefinition().name())),
                "llmNode should contain directTool");
    }
    
    /**
     * Test case: Empty interceptors list - should not cause errors
     */
    @Test
    void testWithEmptyInterceptors_noErrors() throws Exception {
        NacosReactAgentBuilder builder = new NacosReactAgentBuilder();
        ReactAgent agent = builder.nacosOptions(nacosOptions).name("agent_without_interceptors").build();
        
        // Should build successfully without errors
        assertNotNull(agent, "Agent should be built successfully");
        
        // Verify interceptor lists exist but may be empty
        List<ModelInterceptor> modelInterceptors = getModelInterceptors(builder);
        List<ToolInterceptor> toolInterceptors = getToolInterceptors(builder);
        
        // Lists should exist (initialized in Builder)
        assertNotNull(modelInterceptors, "modelInterceptors should not be null");
        assertNotNull(toolInterceptors, "toolInterceptors should not be null");
    }
    
    /**
     * Test case: Combining hooks, interceptors and tools - all sources should work together
     */
    @Test
    void testWithHooksInterceptorsAndTools_allSourcesWorkTogether() throws Exception {
        // Hook with tool
        ToolCallback hookTool = createTestToolCallback("hookTool", "Hook tool");
        Hook testHook = new TestHookWithTools("testHook", hookTool);
        
        // Interceptor with tool
        ToolCallback interceptorTool = createTestToolCallback("interceptorTool", "Interceptor tool");
        TestModelInterceptor modelInterceptor = new TestModelInterceptor("testModelInterceptor", interceptorTool);
        TestToolInterceptor toolInterceptor = new TestToolInterceptor("testToolInterceptor");
        
        // Direct tool
        ToolCallback directTool = createTestToolCallback("directTool", "Direct tool");
        
        NacosReactAgentBuilder builder = new NacosReactAgentBuilder();
        ReactAgent agent = builder.nacosOptions(nacosOptions).name("agent_with_all_sources").hooks(testHook)
                .interceptors(modelInterceptor, toolInterceptor).tools(directTool).build();
        
        // Verify interceptors are separated correctly
        List<ModelInterceptor> modelInterceptors = getModelInterceptors(builder);
        List<ToolInterceptor> toolInterceptors = getToolInterceptors(builder);
        
        assertEquals(1, modelInterceptors.size(), "Should have 1 model interceptor");
        assertEquals(1, toolInterceptors.size(), "Should have 1 tool interceptor");
        
        // Verify all tools are present
        AgentLlmNode llmNode = getLlmNode(agent);
        List<ToolCallback> llmToolCallbacks = getToolCallbacks(llmNode);
        
        assertNotNull(llmToolCallbacks, "llmNode toolCallbacks should not be null");
        assertEquals(3, llmToolCallbacks.size(), "llmNode should have 3 tools (hook + interceptor + direct)");
        assertTrue(llmToolCallbacks.stream().anyMatch(t -> "hookTool".equals(t.getToolDefinition().name())),
                "llmNode should contain hookTool");
        assertTrue(llmToolCallbacks.stream().anyMatch(t -> "interceptorTool".equals(t.getToolDefinition().name())),
                "llmNode should contain interceptorTool");
        assertTrue(llmToolCallbacks.stream().anyMatch(t -> "directTool".equals(t.getToolDefinition().name())),
                "llmNode should contain directTool");
    }
    
    /**
     * Test case: Multiple ModelInterceptors with tools - all interceptor tools should be collected
     */
    @Test
    void testWithMultipleInterceptorTools_allToolsCollected() throws Exception {
        ToolCallback tool1 = createTestToolCallback("interceptorTool1", "Interceptor tool 1");
        ToolCallback tool2 = createTestToolCallback("interceptorTool2", "Interceptor tool 2");
        TestModelInterceptor modelInterceptor1 = new TestModelInterceptor("interceptor1", tool1);
        TestModelInterceptor modelInterceptor2 = new TestModelInterceptor("interceptor2", tool2);
        
        NacosReactAgentBuilder builder = new NacosReactAgentBuilder();
        ReactAgent agent = builder.nacosOptions(nacosOptions).name("agent_with_multiple_interceptor_tools")
                .interceptors(modelInterceptor1, modelInterceptor2).build();
        
        AgentLlmNode llmNode = getLlmNode(agent);
        List<ToolCallback> llmToolCallbacks = getToolCallbacks(llmNode);
        
        assertNotNull(llmToolCallbacks, "llmNode toolCallbacks should not be null");
        assertEquals(2, llmToolCallbacks.size(), "llmNode should have 2 tools from interceptors");
        assertTrue(llmToolCallbacks.stream().anyMatch(t -> "interceptorTool1".equals(t.getToolDefinition().name())),
                "llmNode should contain interceptorTool1");
        assertTrue(llmToolCallbacks.stream().anyMatch(t -> "interceptorTool2".equals(t.getToolDefinition().name())),
                "llmNode should contain interceptorTool2");
    }
    
}
