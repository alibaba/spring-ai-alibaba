/*
 * Copyright 2025 the original author or authors.
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
package com.alibaba.cloud.ai.example.manus.dynamic.mcp.model.vo;

import com.alibaba.cloud.ai.example.manus.dynamic.mcp.service.McpStateHolderService;
import com.alibaba.cloud.ai.example.manus.tool.innerStorage.ISmartContentSavingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.ToolCallback;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class McpToolTest {

	@Mock
	private ToolCallback toolCallback;

	@Mock
	private McpStateHolderService mcpStateHolderService;

	@Mock
	private ISmartContentSavingService smartContentSavingService;

	@Mock
	private ToolDefinition toolDefinition;

	private ObjectMapper objectMapper;

	@BeforeEach
	void setUp() {
		objectMapper = new ObjectMapper();
		when(toolCallback.getToolDefinition()).thenReturn(toolDefinition);
	}

	@Test
	void testGetName_WithServerName_ShouldReturnPrefixedName() {

		String serverName = "test-server";
		String originalToolName = "search";
		String expectedPrefixedName = "test-server_tools_search";

		when(toolDefinition.name()).thenReturn(originalToolName);

		McpTool mcpTool = new McpTool(toolCallback, serverName, "plan-1", mcpStateHolderService,
				smartContentSavingService, objectMapper);

		assertEquals(expectedPrefixedName, mcpTool.getName());
	}

	@Test
	void testGetName_WithEmptyServerName_ShouldReturnOriginalName() {

		String serverName = "";
		String originalToolName = "search";

		when(toolDefinition.name()).thenReturn(originalToolName);

		McpTool mcpTool = new McpTool(toolCallback, serverName, "plan-1", mcpStateHolderService,
				smartContentSavingService, objectMapper);

		assertEquals(originalToolName, mcpTool.getName());
	}

	@Test
	void testGetName_WithNullServerName_ShouldReturnOriginalName() {

		String serverName = null;
		String originalToolName = "search";

		when(toolDefinition.name()).thenReturn(originalToolName);

		McpTool mcpTool = new McpTool(toolCallback, serverName, "plan-1", mcpStateHolderService,
				smartContentSavingService, objectMapper);

		assertEquals(originalToolName, mcpTool.getName());
	}

	@Test
	void testGetName_WithWhitespaceServerName_ShouldReturnOriginalName() {

		String serverName = "   ";
		String originalToolName = "search";

		when(toolDefinition.name()).thenReturn(originalToolName);

		McpTool mcpTool = new McpTool(toolCallback, serverName, "plan-1", mcpStateHolderService,
				smartContentSavingService, objectMapper);

		assertEquals(originalToolName, mcpTool.getName());
	}

	@Test
	void testGetName_DifferentServersSameToolName_ShouldHaveDifferentPrefixedNames() {

		String originalToolName = "search";
		when(toolDefinition.name()).thenReturn(originalToolName);

		McpTool mcpTool1 = new McpTool(toolCallback, "server-1", "plan-1", mcpStateHolderService,
				smartContentSavingService, objectMapper);
		McpTool mcpTool2 = new McpTool(toolCallback, "server-2", "plan-1", mcpStateHolderService,
				smartContentSavingService, objectMapper);

		assertEquals("server-1_tools_search", mcpTool1.getName());
		assertEquals("server-2_tools_search", mcpTool2.getName());
		assertNotEquals(mcpTool1.getName(), mcpTool2.getName());
	}

	@Test
	void testGetName_ComplexServerNameAndToolName_ShouldGenerateCorrectPrefix() {

		String serverName = "my-complex-server-name";
		String originalToolName = "complex_tool_name";
		String expectedPrefixedName = "my-complex-server-name_tools_complex_tool_name";

		when(toolDefinition.name()).thenReturn(originalToolName);

		McpTool mcpTool = new McpTool(toolCallback, serverName, "plan-1", mcpStateHolderService,
				smartContentSavingService, objectMapper);

		assertEquals(expectedPrefixedName, mcpTool.getName());
	}

	@Test
	void testGetDescription_ShouldReturnToolCallbackDescription() {

		String description = "This is a test tool";
		when(toolDefinition.description()).thenReturn(description);
		when(toolDefinition.name()).thenReturn("test-tool");

		McpTool mcpTool = new McpTool(toolCallback, "server", "plan-1", mcpStateHolderService,
				smartContentSavingService, objectMapper);

		assertEquals(description, mcpTool.getDescription());
	}

	@Test
	void testGetServiceGroup_ShouldReturnServiceNameString() {

		String serviceName = "test-service";
		when(toolDefinition.name()).thenReturn("test-tool");

		McpTool mcpTool = new McpTool(toolCallback, serviceName, "plan-1", mcpStateHolderService,
				smartContentSavingService, objectMapper);

		assertEquals(serviceName, mcpTool.getServiceGroup());
	}

}
