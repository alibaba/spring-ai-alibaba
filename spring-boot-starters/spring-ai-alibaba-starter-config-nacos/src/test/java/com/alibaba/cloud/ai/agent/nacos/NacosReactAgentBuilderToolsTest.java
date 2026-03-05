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
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.ai.tool.resolution.ToolCallbackResolver;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests to verify that tools provided via methodTools(), tools(), toolNames(),
 * and resolver() methods are correctly injected into AgentToolNode and AgentLlmNode
 * when using NacosReactAgentBuilder.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NacosReactAgentBuilderToolsTest {

	@Mock
	private NacosConfigService nacosConfigService;

	@Mock
	private NacosMcpOperationService mcpOperationService;

	private NacosOptions nacosOptions;

	private MockedStatic<ChatOptionsProxy> chatOptionsProxyMockedStatic;

	/**
	 * Simple ToolCallbackResolver implementation that provides tools.
	 */
	static class SimpleToolCallbackResolver implements ToolCallbackResolver, ToolCallbackProvider {
		private final Map<String, ToolCallback> tools = new HashMap<>();

		public SimpleToolCallbackResolver(ToolCallback... toolCallbacks) {
			for (ToolCallback tool : toolCallbacks) {
				tools.put(tool.getToolDefinition().name(), tool);
			}
		}

		@Override
		public ToolCallback resolve(String toolName) {
			return tools.get(toolName);
		}

		@Override
		public ToolCallback[] getToolCallbacks() {
			return tools.values().toArray(new ToolCallback[0]);
		}
	}

	/**
	 * Test tool class with @Tool annotation for methodTools() testing.
	 */
	static class TestToolClass {
		@Tool(description = "A test greeting tool")
		public String greet(String name) {
			return "Hello, " + name + "!";
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
	 * Create a simple tool callback for testing.
	 */
	private static ToolCallback createTestToolCallback(String name, String description) {
		return FunctionToolCallback.builder(name, new EchoFunction())
				.description(description)
				.inputType(String.class)
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
		when(nacosConfigService.getConfig(eq("agent-base.json"), anyString(), anyLong()))
				.thenReturn(JSON.toJSONString(agentVO));

		// Mock PromptVO config
		PromptVO promptVO = new PromptVO();
		promptVO.setPromptKey("test-prompt");
		promptVO.setVersion("1.0");
		promptVO.setTemplate("You are a test assistant");
		when(nacosConfigService.getConfig(eq("prompt-test-prompt.json"), anyString(), anyLong()))
				.thenReturn(JSON.toJSONString(promptVO));

		// Mock ModelVO config
		ModelVO modelVO = new ModelVO();
		modelVO.setBaseUrl("https://api.openai.com/v1");
		modelVO.setApiKey("test-api-key");
		modelVO.setModel("gpt-4");
		modelVO.setTemperature("0.7");
		when(nacosConfigService.getConfig(eq("model.json"), anyString(), anyLong()))
				.thenReturn(JSON.toJSONString(modelVO));

		// Mock McpServersVO config - empty MCP servers
		McpServersVO mcpServersVO = new McpServersVO();
		mcpServersVO.setMcpServers(Collections.emptyList());
		when(nacosConfigService.getConfig(eq("mcp-servers.json"), anyString(), anyLong()))
				.thenReturn(JSON.toJSONString(mcpServersVO));
	}

	/**
	 * Create a mock OpenAiChatOptions that implements ObservationMetadataAwareOptions.
	 */
	private OpenAiChatOptions createMockChatOptions() {
		OpenAiChatOptions options = OpenAiChatOptions.builder()
				.model("gpt-4")
				.temperature(0.7)
				.build();
		return options;
	}

	@BeforeEach
	void setUp() throws Exception {
		nacosOptions = createMockNacosOptions();
		setupMockNacosConfigs();

		// Mock the static ChatOptionsProxy.createProxy method to avoid CGLIB issues
		chatOptionsProxyMockedStatic = mockStatic(ChatOptionsProxy.class);
		chatOptionsProxyMockedStatic.when(() -> ChatOptionsProxy.createProxy(any(ChatOptions.class), anyMap()))
				.thenAnswer(invocation -> {
					ChatOptions originalOptions = invocation.getArgument(0);
					// Return a mock that implements both interfaces
					OpenAiChatOptions mockOptions = mock(OpenAiChatOptions.class, withSettings()
							.extraInterfaces(ObservationMetadataAwareOptions.class));

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
	 * Test case: Using tools() method - both AgentToolNode and AgentLlmNode should have tools
	 */
	@Test
	void testNacosReactAgentBuilderWithTools_nodesHaveTools() throws Exception {
		ToolCallback tool1 = createTestToolCallback("tool1", "Test tool 1");
		ToolCallback tool2 = createTestToolCallback("tool2", "Test tool 2");

		NacosReactAgentBuilder builder = new NacosReactAgentBuilder();
		ReactAgent agent = builder
				.nacosOptions(nacosOptions)
				.name("agent_with_tools")
				.tools(tool1, tool2)
				.build();

		// Get nodes through reflection
		AgentLlmNode llmNode = getLlmNode(agent);
		AgentToolNode toolNode = getToolNode(agent);

		// Verify llmNode has tool definitions
		List<ToolCallback> llmToolCallbacks = getToolCallbacks(llmNode);
		assertNotNull(llmToolCallbacks, "llmNode toolCallbacks should not be null");
		assertEquals(2, llmToolCallbacks.size(), "llmNode should have 2 tools");
		assertTrue(llmToolCallbacks.stream().anyMatch(t -> "tool1".equals(t.getToolDefinition().name())),
				"llmNode should contain tool1");
		assertTrue(llmToolCallbacks.stream().anyMatch(t -> "tool2".equals(t.getToolDefinition().name())),
				"llmNode should contain tool2");

		// Verify toolNode has tool definitions
		List<ToolCallback> toolNodeCallbacks = toolNode.getToolCallbacks();
		assertNotNull(toolNodeCallbacks, "toolNode toolCallbacks should not be null");
		assertEquals(2, toolNodeCallbacks.size(), "toolNode should have 2 tools");
		assertTrue(toolNodeCallbacks.stream().anyMatch(t -> "tool1".equals(t.getToolDefinition().name())),
				"toolNode should contain tool1");
		assertTrue(toolNodeCallbacks.stream().anyMatch(t -> "tool2".equals(t.getToolDefinition().name())),
				"toolNode should contain tool2");
	}

	/**
	 * Test case: Using tools(List) method - both nodes should have tools
	 */
	@Test
	void testNacosReactAgentBuilderWithToolsList_nodesHaveTools() throws Exception {
		ToolCallback tool1 = createTestToolCallback("listTool1", "List test tool 1");
		ToolCallback tool2 = createTestToolCallback("listTool2", "List test tool 2");

		NacosReactAgentBuilder builder = new NacosReactAgentBuilder();
		ReactAgent agent = builder
				.nacosOptions(nacosOptions)
				.name("agent_with_tools_list")
				.tools(List.of(tool1, tool2))
				.build();

		AgentLlmNode llmNode = getLlmNode(agent);
		AgentToolNode toolNode = getToolNode(agent);

		List<ToolCallback> llmToolCallbacks = getToolCallbacks(llmNode);
		assertEquals(2, llmToolCallbacks.size(), "llmNode should have 2 tools from list");

		List<ToolCallback> toolNodeCallbacks = toolNode.getToolCallbacks();
		assertEquals(2, toolNodeCallbacks.size(), "toolNode should have 2 tools from list");
	}

	/**
	 * Test case: Using methodTools() method - both nodes should have tools
	 */
	@Test
	void testNacosReactAgentBuilderWithMethodTools_nodesHaveTools() throws Exception {
		TestToolClass testToolClass = new TestToolClass();

		NacosReactAgentBuilder builder = new NacosReactAgentBuilder();
		ReactAgent agent = builder
				.nacosOptions(nacosOptions)
				.name("agent_with_method_tools")
				.methodTools(testToolClass)
				.build();

		AgentLlmNode llmNode = getLlmNode(agent);
		AgentToolNode toolNode = getToolNode(agent);

		List<ToolCallback> llmToolCallbacks = getToolCallbacks(llmNode);
		assertNotNull(llmToolCallbacks, "llmNode toolCallbacks should not be null");
		assertFalse(llmToolCallbacks.isEmpty(), "llmNode should have tools from methodTools");
		assertTrue(llmToolCallbacks.stream().anyMatch(t -> "greet".equals(t.getToolDefinition().name())),
				"llmNode should contain greet tool from @Tool annotation");

		List<ToolCallback> toolNodeCallbacks = toolNode.getToolCallbacks();
		assertNotNull(toolNodeCallbacks, "toolNode toolCallbacks should not be null");
		assertFalse(toolNodeCallbacks.isEmpty(), "toolNode should have tools from methodTools");
		assertTrue(toolNodeCallbacks.stream().anyMatch(t -> "greet".equals(t.getToolDefinition().name())),
				"toolNode should contain greet tool from @Tool annotation");
	}

	/**
	 * Test case: Using resolver() method only - both nodes should have tools extracted from resolver
	 */
	@Test
	void testNacosReactAgentBuilderWithResolverOnly_nodesHaveTools() throws Exception {
		ToolCallback resolverTool = createTestToolCallback("resolverTool", "Tool from resolver");
		SimpleToolCallbackResolver resolver = new SimpleToolCallbackResolver(resolverTool);

		NacosReactAgentBuilder builder = new NacosReactAgentBuilder();
		ReactAgent agent = builder
				.nacosOptions(nacosOptions)
				.name("agent_with_resolver")
				.resolver(resolver)
				.build();

		AgentLlmNode llmNode = getLlmNode(agent);
		AgentToolNode toolNode = getToolNode(agent);

		List<ToolCallback> llmToolCallbacks = getToolCallbacks(llmNode);
		assertNotNull(llmToolCallbacks, "llmNode toolCallbacks should not be null");
		assertFalse(llmToolCallbacks.isEmpty(), "llmNode should have tools from resolver");
		assertTrue(llmToolCallbacks.stream().anyMatch(t -> "resolverTool".equals(t.getToolDefinition().name())),
				"llmNode should contain resolverTool");

		List<ToolCallback> toolNodeCallbacks = toolNode.getToolCallbacks();
		assertNotNull(toolNodeCallbacks, "toolNode toolCallbacks should not be null");
		assertFalse(toolNodeCallbacks.isEmpty(), "toolNode should have tools from resolver");
		assertTrue(toolNodeCallbacks.stream().anyMatch(t -> "resolverTool".equals(t.getToolDefinition().name())),
				"toolNode should contain resolverTool");
	}

	/**
	 * Test case: Using toolNames() with resolver - tools should be resolved by name
	 */
	@Test
	void testNacosReactAgentBuilderWithToolNames_nodesHaveTools() throws Exception {
		ToolCallback tool1 = createTestToolCallback("namedTool1", "Named tool 1");
		ToolCallback tool2 = createTestToolCallback("namedTool2", "Named tool 2");
		ToolCallback tool3 = createTestToolCallback("namedTool3", "Named tool 3");
		SimpleToolCallbackResolver resolver = new SimpleToolCallbackResolver(tool1, tool2, tool3);

		NacosReactAgentBuilder builder = new NacosReactAgentBuilder();
		ReactAgent agent = builder
				.nacosOptions(nacosOptions)
				.name("agent_with_tool_names")
				.resolver(resolver)
				.toolNames("namedTool1", "namedTool2")  // Only request 2 of 3 tools
				.build();

		AgentLlmNode llmNode = getLlmNode(agent);
		AgentToolNode toolNode = getToolNode(agent);

		List<ToolCallback> llmToolCallbacks = getToolCallbacks(llmNode);
		assertNotNull(llmToolCallbacks, "llmNode toolCallbacks should not be null");
		assertEquals(2, llmToolCallbacks.size(), "llmNode should have 2 tools resolved by name");
		assertTrue(llmToolCallbacks.stream().anyMatch(t -> "namedTool1".equals(t.getToolDefinition().name())),
				"llmNode should contain namedTool1");
		assertTrue(llmToolCallbacks.stream().anyMatch(t -> "namedTool2".equals(t.getToolDefinition().name())),
				"llmNode should contain namedTool2");

		List<ToolCallback> toolNodeCallbacks = toolNode.getToolCallbacks();
		assertNotNull(toolNodeCallbacks, "toolNode toolCallbacks should not be null");
		assertEquals(2, toolNodeCallbacks.size(), "toolNode should have 2 tools resolved by name");
	}

	/**
	 * Test case: Combining tools() and methodTools() - both sources should be merged
	 */
	@Test
	void testNacosReactAgentBuilderWithCombinedToolsAndMethodTools_nodesHaveMergedTools() throws Exception {
		ToolCallback directTool = createTestToolCallback("directTool", "Direct tool");
		TestToolClass testToolClass = new TestToolClass();

		NacosReactAgentBuilder builder = new NacosReactAgentBuilder();
		ReactAgent agent = builder
				.nacosOptions(nacosOptions)
				.name("agent_with_combined_tools")
				.tools(directTool)
				.methodTools(testToolClass)
				.build();

		AgentLlmNode llmNode = getLlmNode(agent);
		AgentToolNode toolNode = getToolNode(agent);

		List<ToolCallback> llmToolCallbacks = getToolCallbacks(llmNode);
		assertNotNull(llmToolCallbacks, "llmNode toolCallbacks should not be null");
		assertEquals(2, llmToolCallbacks.size(), "llmNode should have 2 tools (direct + method)");
		assertTrue(llmToolCallbacks.stream().anyMatch(t -> "directTool".equals(t.getToolDefinition().name())),
				"llmNode should contain directTool");
		assertTrue(llmToolCallbacks.stream().anyMatch(t -> "greet".equals(t.getToolDefinition().name())),
				"llmNode should contain greet from methodTools");

		List<ToolCallback> toolNodeCallbacks = toolNode.getToolCallbacks();
		assertEquals(2, toolNodeCallbacks.size(), "toolNode should have 2 tools (direct + method)");
	}

	/**
	 * Test case: Combining tools(), methodTools(), and resolver() - all sources should be merged
	 */
	@Test
	void testNacosReactAgentBuilderWithAllToolSources_nodesHaveAllTools() throws Exception {
		ToolCallback directTool = createTestToolCallback("directTool", "Direct tool");
		TestToolClass testToolClass = new TestToolClass();
		ToolCallback resolverTool = createTestToolCallback("resolverTool", "Resolver tool");
		SimpleToolCallbackResolver resolver = new SimpleToolCallbackResolver(resolverTool);

		NacosReactAgentBuilder builder = new NacosReactAgentBuilder();
		ReactAgent agent = builder
				.nacosOptions(nacosOptions)
				.name("agent_with_all_tool_sources")
				.tools(directTool)
				.methodTools(testToolClass)
				.resolver(resolver)
				.toolNames("resolverTool")
				.build();

		AgentLlmNode llmNode = getLlmNode(agent);
		AgentToolNode toolNode = getToolNode(agent);

		List<ToolCallback> llmToolCallbacks = getToolCallbacks(llmNode);
		assertNotNull(llmToolCallbacks, "llmNode toolCallbacks should not be null");
		assertEquals(3, llmToolCallbacks.size(), "llmNode should have 3 tools from all sources");
		assertTrue(llmToolCallbacks.stream().anyMatch(t -> "directTool".equals(t.getToolDefinition().name())),
				"llmNode should contain directTool");
		assertTrue(llmToolCallbacks.stream().anyMatch(t -> "greet".equals(t.getToolDefinition().name())),
				"llmNode should contain greet from methodTools");
		assertTrue(llmToolCallbacks.stream().anyMatch(t -> "resolverTool".equals(t.getToolDefinition().name())),
				"llmNode should contain resolverTool from resolver");

		List<ToolCallback> toolNodeCallbacks = toolNode.getToolCallbacks();
		assertEquals(3, toolNodeCallbacks.size(), "toolNode should have 3 tools from all sources");
	}

	/**
	 * Test case: Using toolCallbackProviders() - tools from providers should be added
	 */
	@Test
	void testNacosReactAgentBuilderWithToolCallbackProviders_nodesHaveTools() throws Exception {
		ToolCallback providerTool1 = createTestToolCallback("providerTool1", "Provider tool 1");
		ToolCallback providerTool2 = createTestToolCallback("providerTool2", "Provider tool 2");

		ToolCallbackProvider provider = () -> new ToolCallback[]{providerTool1, providerTool2};

		NacosReactAgentBuilder builder = new NacosReactAgentBuilder();
		ReactAgent agent = builder
				.nacosOptions(nacosOptions)
				.name("agent_with_providers")
				.toolCallbackProviders(provider)
				.build();

		AgentLlmNode llmNode = getLlmNode(agent);
		AgentToolNode toolNode = getToolNode(agent);

		List<ToolCallback> llmToolCallbacks = getToolCallbacks(llmNode);
		assertNotNull(llmToolCallbacks, "llmNode toolCallbacks should not be null");
		assertEquals(2, llmToolCallbacks.size(), "llmNode should have 2 tools from provider");
		assertTrue(llmToolCallbacks.stream().anyMatch(t -> "providerTool1".equals(t.getToolDefinition().name())),
				"llmNode should contain providerTool1");
		assertTrue(llmToolCallbacks.stream().anyMatch(t -> "providerTool2".equals(t.getToolDefinition().name())),
				"llmNode should contain providerTool2");

		List<ToolCallback> toolNodeCallbacks = toolNode.getToolCallbacks();
		assertEquals(2, toolNodeCallbacks.size(), "toolNode should have 2 tools from provider");
	}

	/**
	 * Test case: Verify that AgentToolNode and AgentLlmNode have the same tools
	 */
	@Test
	void testToolNodeAndLlmNodeHaveSameTools() throws Exception {
		ToolCallback tool1 = createTestToolCallback("syncTool1", "Sync tool 1");
		ToolCallback tool2 = createTestToolCallback("syncTool2", "Sync tool 2");

		NacosReactAgentBuilder builder = new NacosReactAgentBuilder();
		ReactAgent agent = builder
				.nacosOptions(nacosOptions)
				.name("agent_sync_tools")
				.tools(tool1, tool2)
				.build();

		AgentLlmNode llmNode = getLlmNode(agent);
		AgentToolNode toolNode = getToolNode(agent);

		List<ToolCallback> llmToolCallbacks = getToolCallbacks(llmNode);
		List<ToolCallback> toolNodeCallbacks = toolNode.getToolCallbacks();

		assertEquals(llmToolCallbacks.size(), toolNodeCallbacks.size(),
				"LlmNode and ToolNode should have the same number of tools");

		for (ToolCallback llmTool : llmToolCallbacks) {
			String toolName = llmTool.getToolDefinition().name();
			assertTrue(toolNodeCallbacks.stream().anyMatch(t -> toolName.equals(t.getToolDefinition().name())),
					"ToolNode should also contain tool: " + toolName);
		}
	}

	/**
	 * Test case: Verify local tools are stored and used for MCP listener updates
	 */
	@Test
	void testLocalToolsAreStoredForMcpUpdates() throws Exception {
		ToolCallback localTool1 = createTestToolCallback("localTool1", "Local tool 1");
		ToolCallback localTool2 = createTestToolCallback("localTool2", "Local tool 2");

		NacosReactAgentBuilder builder = new NacosReactAgentBuilder();
		ReactAgent agent = builder
				.nacosOptions(nacosOptions)
				.name("agent_with_local_tools")
				.tools(localTool1, localTool2)
				.build();

		// Verify that localTools field in builder is correctly populated
		Field localToolsField = NacosReactAgentBuilder.class.getDeclaredField("localTools");
		localToolsField.setAccessible(true);
		@SuppressWarnings("unchecked")
		List<ToolCallback> localTools = (List<ToolCallback>) localToolsField.get(builder);

		assertNotNull(localTools, "localTools should not be null");
		assertEquals(2, localTools.size(), "localTools should have 2 tools");
		assertTrue(localTools.stream().anyMatch(t -> "localTool1".equals(t.getToolDefinition().name())),
				"localTools should contain localTool1");
		assertTrue(localTools.stream().anyMatch(t -> "localTool2".equals(t.getToolDefinition().name())),
				"localTools should contain localTool2");
	}

	/**
	 * Test case: Using hooks() method with tools - both AgentToolNode and AgentLlmNode should have hook tools
	 */
	@Test
	void testNacosReactAgentBuilderWithHooks_nodesHaveHookTools() throws Exception {
		ToolCallback hookTool1 = createTestToolCallback("hookTool1", "Hook tool 1");
		ToolCallback hookTool2 = createTestToolCallback("hookTool2", "Hook tool 2");

		Hook testHook = new TestHookWithTools("testHook", hookTool1, hookTool2);

		NacosReactAgentBuilder builder = new NacosReactAgentBuilder();
		ReactAgent agent = builder
				.nacosOptions(nacosOptions)
				.name("agent_with_hooks")
				.hooks(testHook)
				.build();

		AgentLlmNode llmNode = getLlmNode(agent);
		AgentToolNode toolNode = getToolNode(agent);

		// Verify llmNode has hook tools
		List<ToolCallback> llmToolCallbacks = getToolCallbacks(llmNode);
		assertNotNull(llmToolCallbacks, "llmNode toolCallbacks should not be null");
		assertEquals(2, llmToolCallbacks.size(), "llmNode should have 2 tools from hook");
		assertTrue(llmToolCallbacks.stream().anyMatch(t -> "hookTool1".equals(t.getToolDefinition().name())),
				"llmNode should contain hookTool1");
		assertTrue(llmToolCallbacks.stream().anyMatch(t -> "hookTool2".equals(t.getToolDefinition().name())),
				"llmNode should contain hookTool2");

		// Verify toolNode has hook tools
		List<ToolCallback> toolNodeCallbacks = toolNode.getToolCallbacks();
		assertNotNull(toolNodeCallbacks, "toolNode toolCallbacks should not be null");
		assertEquals(2, toolNodeCallbacks.size(), "toolNode should have 2 tools from hook");
		assertTrue(toolNodeCallbacks.stream().anyMatch(t -> "hookTool1".equals(t.getToolDefinition().name())),
				"toolNode should contain hookTool1");
		assertTrue(toolNodeCallbacks.stream().anyMatch(t -> "hookTool2".equals(t.getToolDefinition().name())),
				"toolNode should contain hookTool2");
	}

	/**
	 * Test case: Using hooks() with multiple hooks - tools from all hooks should be merged
	 */
	@Test
	void testNacosReactAgentBuilderWithMultipleHooks_nodesHaveAllHookTools() throws Exception {
		ToolCallback hook1Tool = createTestToolCallback("hook1Tool", "Hook 1 tool");
		ToolCallback hook2Tool = createTestToolCallback("hook2Tool", "Hook 2 tool");

		Hook hook1 = new TestHookWithTools("hook1", hook1Tool);
		Hook hook2 = new TestHookWithTools("hook2", hook2Tool);

		NacosReactAgentBuilder builder = new NacosReactAgentBuilder();
		ReactAgent agent = builder
				.nacosOptions(nacosOptions)
				.name("agent_with_multiple_hooks")
				.hooks(hook1, hook2)
				.build();

		AgentLlmNode llmNode = getLlmNode(agent);
		AgentToolNode toolNode = getToolNode(agent);

		List<ToolCallback> llmToolCallbacks = getToolCallbacks(llmNode);
		assertNotNull(llmToolCallbacks, "llmNode toolCallbacks should not be null");
		assertEquals(2, llmToolCallbacks.size(), "llmNode should have 2 tools from both hooks");
		assertTrue(llmToolCallbacks.stream().anyMatch(t -> "hook1Tool".equals(t.getToolDefinition().name())),
				"llmNode should contain hook1Tool");
		assertTrue(llmToolCallbacks.stream().anyMatch(t -> "hook2Tool".equals(t.getToolDefinition().name())),
				"llmNode should contain hook2Tool");

		List<ToolCallback> toolNodeCallbacks = toolNode.getToolCallbacks();
		assertEquals(2, toolNodeCallbacks.size(), "toolNode should have 2 tools from both hooks");
	}

	/**
	 * Test case: Combining hooks(), tools(), and methodTools() - all sources should be merged correctly
	 */
	@Test
	void testNacosReactAgentBuilderWithHooksAndOtherTools_toolsAreMergedCorrectly() throws Exception {
		// Hook tools
		ToolCallback hookTool = createTestToolCallback("hookTool", "Hook tool");
		Hook testHook = new TestHookWithTools("testHook", hookTool);

		// Direct tools
		ToolCallback directTool = createTestToolCallback("directTool", "Direct tool");

		// Method tools
		TestToolClass testToolClass = new TestToolClass();

		NacosReactAgentBuilder builder = new NacosReactAgentBuilder();
		ReactAgent agent = builder
				.nacosOptions(nacosOptions)
				.name("agent_with_hooks_and_tools")
				.hooks(testHook)
				.tools(directTool)
				.methodTools(testToolClass)
				.build();

		AgentLlmNode llmNode = getLlmNode(agent);
		AgentToolNode toolNode = getToolNode(agent);

		// Verify llmNode has all tools (hook + direct + method)
		List<ToolCallback> llmToolCallbacks = getToolCallbacks(llmNode);
		assertNotNull(llmToolCallbacks, "llmNode toolCallbacks should not be null");
		assertEquals(3, llmToolCallbacks.size(), "llmNode should have 3 tools (hook + direct + method)");
		assertTrue(llmToolCallbacks.stream().anyMatch(t -> "hookTool".equals(t.getToolDefinition().name())),
				"llmNode should contain hookTool from hook");
		assertTrue(llmToolCallbacks.stream().anyMatch(t -> "directTool".equals(t.getToolDefinition().name())),
				"llmNode should contain directTool");
		assertTrue(llmToolCallbacks.stream().anyMatch(t -> "greet".equals(t.getToolDefinition().name())),
				"llmNode should contain greet from methodTools");

		// Verify toolNode has all tools
		List<ToolCallback> toolNodeCallbacks = toolNode.getToolCallbacks();
		assertEquals(3, toolNodeCallbacks.size(), "toolNode should have 3 tools (hook + direct + method)");
	}

	/**
	 * Test case: Verify hook tools are prioritized (added first) in the combined tools list
	 */
	@Test
	void testHookToolsArePrioritizedInCombinedList() throws Exception {
		ToolCallback hookTool = createTestToolCallback("hookTool", "Hook tool");
		Hook testHook = new TestHookWithTools("testHook", hookTool);

		ToolCallback directTool = createTestToolCallback("directTool", "Direct tool");

		NacosReactAgentBuilder builder = new NacosReactAgentBuilder();
		ReactAgent agent = builder
				.nacosOptions(nacosOptions)
				.name("agent_hook_priority")
				.hooks(testHook)
				.tools(directTool)
				.build();

		AgentLlmNode llmNode = getLlmNode(agent);
		List<ToolCallback> llmToolCallbacks = getToolCallbacks(llmNode);

		assertNotNull(llmToolCallbacks, "llmNode toolCallbacks should not be null");
		assertEquals(2, llmToolCallbacks.size(), "llmNode should have 2 tools");

		// Hook tools should come before regular tools (index 0)
		assertEquals("hookTool", llmToolCallbacks.get(0).getToolDefinition().name(),
				"Hook tool should be first in the list (prioritized)");
		assertEquals("directTool", llmToolCallbacks.get(1).getToolDefinition().name(),
				"Direct tool should come after hook tool");
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
		@SuppressWarnings("unchecked")
		List<ToolCallback> toolCallbacks = (List<ToolCallback>) toolCallbacksField.get(llmNode);
		return toolCallbacks;
	}

}
