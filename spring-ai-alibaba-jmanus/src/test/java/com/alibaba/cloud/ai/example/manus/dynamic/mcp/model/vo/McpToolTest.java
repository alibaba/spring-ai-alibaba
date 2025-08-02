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
	void testGetName_WithServerName_ShouldReturnPrefixedNameWithInstanceId() {

		String serverName = "test-server";
		String originalToolName = "search";

		when(toolDefinition.name()).thenReturn(originalToolName);

		McpTool mcpTool = new McpTool(toolCallback, serverName, "plan-1", mcpStateHolderService,
				smartContentSavingService, objectMapper);

		String actualName = mcpTool.getName();
		assertTrue(actualName.startsWith("test-server_tools_search_"));
		assertTrue(actualName.matches("test-server_tools_search_\\d+"));
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

		String name1 = mcpTool1.getName();
		String name2 = mcpTool2.getName();

		// Both should have different prefixes and instance IDs
		assertTrue(name1.startsWith("server-1_tools_search_"));
		assertTrue(name2.startsWith("server-2_tools_search_"));
		assertNotEquals(name1, name2);
	}

	@Test
	void testGetName_ComplexServerNameAndToolName_ShouldGenerateCorrectPrefix() {

		String serverName = "my-complex-server-name";
		String originalToolName = "complex_tool_name";

		when(toolDefinition.name()).thenReturn(originalToolName);

		McpTool mcpTool = new McpTool(toolCallback, serverName, "plan-1", mcpStateHolderService,
				smartContentSavingService, objectMapper);

		String actualName = mcpTool.getName();
		// Should start with the expected prefix and end with instance ID
		assertTrue(actualName.startsWith("my-complex-server-name_tools_complex_tool_name_"));
		assertTrue(actualName.matches("my-complex-server-name_tools_complex_tool_name_\\d+"));
	}

	@Test
	void testGetName_SameServerSameToolName_ShouldHaveDifferentInstanceIds() {

		String serverName = "cmapi011178";
		String originalToolName = "search";

		when(toolDefinition.name()).thenReturn(originalToolName);

		// Create multiple tools with same server and tool name
		McpTool mcpTool1 = new McpTool(toolCallback, serverName, "plan-1", mcpStateHolderService,
				smartContentSavingService, objectMapper);
		McpTool mcpTool2 = new McpTool(toolCallback, serverName, "plan-1", mcpStateHolderService,
				smartContentSavingService, objectMapper);
		McpTool mcpTool3 = new McpTool(toolCallback, serverName, "plan-1", mcpStateHolderService,
				smartContentSavingService, objectMapper);

		String name1 = mcpTool1.getName();
		String name2 = mcpTool2.getName();
		String name3 = mcpTool3.getName();

		// All should have same prefix but different instance IDs
		assertTrue(name1.startsWith("cmapi011178_tools_search_"));
		assertTrue(name2.startsWith("cmapi011178_tools_search_"));
		assertTrue(name3.startsWith("cmapi011178_tools_search_"));

		// All names should be different
		assertNotEquals(name1, name2);
		assertNotEquals(name2, name3);
		assertNotEquals(name1, name3);

		// Verify instance IDs are incremental
		assertTrue(name1.matches("cmapi011178_tools_search_\\d+"));
		assertTrue(name2.matches("cmapi011178_tools_search_\\d+"));
		assertTrue(name3.matches("cmapi011178_tools_search_\\d+"));
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
