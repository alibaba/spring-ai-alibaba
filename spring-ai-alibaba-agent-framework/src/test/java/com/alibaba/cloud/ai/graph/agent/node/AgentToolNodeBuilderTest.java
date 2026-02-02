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
package com.alibaba.cloud.ai.graph.agent.node;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.execution.ToolExecutionExceptionProcessor;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests for AgentToolNode.Builder validation.
 *
 * <p>
 * Covers Issue 4 fix: Builder should validate required fields (toolExecutionTimeout,
 * toolExecutionExceptionProcessor) with requireNonNull checks.
 * </p>
 *
 * @author Claude
 * @since 1.0.0
 */
@DisplayName("AgentToolNode Builder Tests")
class AgentToolNodeBuilderTest {

	private static final ToolExecutionExceptionProcessor DEFAULT_PROCESSOR = e -> "Error: " + e.getMessage();

	@Test
	@DisplayName("build() should throw NullPointerException when toolExecutionTimeout is null")
	void build_throwsNPE_whenToolExecutionTimeoutIsNull() {
		AgentToolNode.Builder builder = AgentToolNode.builder()
			.agentName("test-agent")
			.toolExecutionTimeout(null)
			.toolExecutionExceptionProcessor(DEFAULT_PROCESSOR);

		NullPointerException ex = assertThrows(NullPointerException.class, builder::build);
		assertEquals("toolExecutionTimeout must not be null", ex.getMessage());
	}

	@Test
	@DisplayName("build() should throw NullPointerException when toolExecutionExceptionProcessor is null")
	void build_throwsNPE_whenToolExecutionExceptionProcessorIsNull() {
		AgentToolNode.Builder builder = AgentToolNode.builder()
			.agentName("test-agent")
			.toolExecutionTimeout(Duration.ofMinutes(5));

		NullPointerException ex = assertThrows(NullPointerException.class, builder::build);
		assertEquals("toolExecutionExceptionProcessor must not be null", ex.getMessage());
	}

	@Test
	@DisplayName("build() should throw NullPointerException when both required fields are null")
	void build_throwsNPE_whenBothRequiredFieldsAreNull() {
		AgentToolNode.Builder builder = AgentToolNode.builder()
			.agentName("test-agent")
			.toolExecutionTimeout(null)
			.toolExecutionExceptionProcessor(null);

		// toolExecutionTimeout is checked first
		NullPointerException ex = assertThrows(NullPointerException.class, builder::build);
		assertEquals("toolExecutionTimeout must not be null", ex.getMessage());
	}

	@Test
	@DisplayName("build() should succeed with all required fields set")
	void build_succeeds_withAllRequiredFieldsSet() {
		AgentToolNode node = assertDoesNotThrow(() -> AgentToolNode.builder()
			.agentName("test-agent")
			.toolExecutionTimeout(Duration.ofMinutes(5))
			.toolExecutionExceptionProcessor(DEFAULT_PROCESSOR)
			.build());

		assertNotNull(node);
	}

	@Test
	@DisplayName("build() should succeed with default timeout and explicit processor")
	void build_succeeds_withDefaultTimeoutAndExplicitProcessor() {
		// Default timeout is already set in builder
		AgentToolNode node = assertDoesNotThrow(() -> AgentToolNode.builder()
			.agentName("test-agent")
			.toolExecutionExceptionProcessor(DEFAULT_PROCESSOR)
			.build());

		assertNotNull(node);
	}

	@Test
	@DisplayName("build() should fail when only timeout is set without processor")
	void build_fails_whenOnlyTimeoutIsSet() {
		AgentToolNode.Builder builder = AgentToolNode.builder()
			.agentName("test-agent")
			.toolExecutionTimeout(Duration.ofSeconds(30));
		// processor is not set

		NullPointerException ex = assertThrows(NullPointerException.class, builder::build);
		assertEquals("toolExecutionExceptionProcessor must not be null", ex.getMessage());
	}

	@Test
	@DisplayName("maxParallelTools() should throw IllegalArgumentException for values less than 1")
	void maxParallelTools_throwsIAE_forInvalidValues() {
		AgentToolNode.Builder builder = AgentToolNode.builder();

		assertThrows(IllegalArgumentException.class, () -> builder.maxParallelTools(0));
		assertThrows(IllegalArgumentException.class, () -> builder.maxParallelTools(-1));
		assertThrows(IllegalArgumentException.class, () -> builder.maxParallelTools(-100));
	}

	@Test
	@DisplayName("maxParallelTools() should accept valid values")
	void maxParallelTools_acceptsValidValues() {
		AgentToolNode node = assertDoesNotThrow(() -> AgentToolNode.builder()
			.agentName("test-agent")
			.maxParallelTools(1)
			.toolExecutionTimeout(Duration.ofMinutes(5))
			.toolExecutionExceptionProcessor(DEFAULT_PROCESSOR)
			.build());

		assertNotNull(node);

		AgentToolNode node2 = assertDoesNotThrow(() -> AgentToolNode.builder()
			.agentName("test-agent")
			.maxParallelTools(100)
			.toolExecutionTimeout(Duration.ofMinutes(5))
			.toolExecutionExceptionProcessor(DEFAULT_PROCESSOR)
			.build());

		assertNotNull(node2);
	}

	@Test
	@DisplayName("builder should support fluent API chaining")
	void builder_supportsFluentApiChaining() {
		AgentToolNode node = AgentToolNode.builder()
			.agentName("chained-agent")
			.enableActingLog(true)
			.parallelToolExecution(true)
			.maxParallelTools(10)
			.toolExecutionTimeout(Duration.ofSeconds(60))
			.toolCallbacks(List.of())
			.toolExecutionExceptionProcessor(DEFAULT_PROCESSOR)
			.build();

		assertNotNull(node);
		assertEquals("_AGENT_TOOL_", node.getName());
	}

}
