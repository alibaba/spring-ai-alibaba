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
package com.alibaba.cloud.ai.graph.agent;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.resolution.ToolCallbackResolver;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for tool-name resolution in {@link DefaultBuilder#gatherLocalTools()}.
 *
 * <p>
 * When a configured tool name cannot be resolved (e.g. the model/skill referenced a
 * non-existent tool), the agent build should not fail; the unresolved name is skipped and
 * a warning is logged, consistent with the runtime fallback in {@code AgentToolNode}.
 * </p>
 *
 * @see <a href="https://github.com/alibaba/spring-ai-alibaba/issues/4703">#4703</a>
 */
@DisplayName("DefaultBuilder tool-name resolution Tests")
class DefaultBuilderToolResolutionTest {

	/**
	 * Test helper exposing the protected {@link DefaultBuilder#gatherLocalTools()}.
	 */
	private static class TestableBuilder extends DefaultBuilder {

		List<ToolCallback> callGatherLocalTools() {
			return gatherLocalTools();
		}

	}

	private static ToolCallback mockToolNamed(String name) {
		ToolDefinition definition = mock(ToolDefinition.class);
		when(definition.name()).thenReturn(name);
		ToolCallback toolCallback = mock(ToolCallback.class);
		when(toolCallback.getToolDefinition()).thenReturn(definition);
		return toolCallback;
	}

	@Test
	@DisplayName("Unresolved tool name is skipped instead of failing the build")
	void unresolvedToolName_isSkipped_notThrown() {
		ToolCallbackResolver resolver = mock(ToolCallbackResolver.class);
		when(resolver.resolve("does-not-exist")).thenReturn(null);

		TestableBuilder builder = new TestableBuilder();
		builder.toolNames("does-not-exist");
		builder.resolver(resolver);

		List<ToolCallback> tools = assertDoesNotThrow(builder::callGatherLocalTools);
		assertTrue(tools.isEmpty(), "Unresolved tool name should be skipped");
	}

	@Test
	@DisplayName("Resolvable tool name is still added (regression)")
	void resolvableToolName_isAdded() {
		ToolCallback resolved = mockToolNamed("real-tool");
		ToolCallbackResolver resolver = mock(ToolCallbackResolver.class);
		when(resolver.resolve("real-tool")).thenReturn(resolved);

		TestableBuilder builder = new TestableBuilder();
		builder.toolNames("real-tool");
		builder.resolver(resolver);

		List<ToolCallback> tools = builder.callGatherLocalTools();
		assertEquals(1, tools.size());
		assertEquals(resolved, tools.get(0));
	}

	@Test
	@DisplayName("A resolvable tool is kept even when another name is unresolved")
	void mixedToolNames_keepResolvedSkipUnresolved() {
		ToolCallback resolved = mockToolNamed("real-tool");
		ToolCallbackResolver resolver = mock(ToolCallbackResolver.class);
		when(resolver.resolve("real-tool")).thenReturn(resolved);
		when(resolver.resolve("ghost-tool")).thenReturn(null);

		TestableBuilder builder = new TestableBuilder();
		builder.toolNames("real-tool", "ghost-tool");
		builder.resolver(resolver);

		List<ToolCallback> tools = assertDoesNotThrow(builder::callGatherLocalTools);
		assertEquals(1, tools.size());
		assertEquals(resolved, tools.get(0));
	}

	@Test
	@DisplayName("Null resolver with tool names still fails fast (unchanged behavior)")
	void nullResolver_withToolNames_stillThrows() {
		TestableBuilder builder = new TestableBuilder();
		builder.toolNames("any-tool");

		assertThrows(IllegalStateException.class, builder::callGatherLocalTools);
	}

}
