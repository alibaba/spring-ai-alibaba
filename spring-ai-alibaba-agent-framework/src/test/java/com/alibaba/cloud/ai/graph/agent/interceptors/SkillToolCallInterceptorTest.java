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

import com.alibaba.cloud.ai.graph.agent.hook.skills.SkillsAgentHook;
import com.alibaba.cloud.ai.graph.agent.interceptor.ToolCallRequest;
import com.alibaba.cloud.ai.graph.agent.interceptor.ToolCallResponse;
import com.alibaba.cloud.ai.graph.agent.interceptor.ToolInterceptor;
import com.alibaba.cloud.ai.graph.skills.registry.SkillRegistry;
import com.alibaba.cloud.ai.graph.skills.registry.filesystem.FileSystemSkillRegistry;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SkillToolCallInterceptorTest {

	@TempDir
	Path tempDir;

	private SkillRegistry registry;

	private ToolInterceptor interceptor;

	@BeforeEach
	void setUp() throws Exception {
		Path skillDir = tempDir.resolve("skills").resolve("allowed-tools-test");
		Files.createDirectories(skillDir);
		Files.writeString(skillDir.resolve("SKILL.md"), """
				---
				name: allowed-tools-test
				description: Skill fixture for fallback tests.
				---

				# Allowed Tools Test

				Fallback fixture content.
				""");
		registry = FileSystemSkillRegistry.builder().projectSkillsDirectory(tempDir.resolve("skills").toString()).build();
		interceptor = SkillsAgentHook.builder().skillRegistry(registry).build().getToolInterceptors().get(0);
	}

	@Test
	void readsSkillWhenModelUsesSkillNameAsUnresolvedTool() {
		ToolCallRequest request = request("allowed-tools-test");
		ToolCallResponse response = interceptor.interceptToolCall(request, ignored -> unresolved(request));

		assertEquals(ToolCallResponse.SUCCESS_STATUS, response.getStatus());
		assertEquals("allowed-tools-test", response.getToolName());
		assertTrue(response.getResult().contains("# Allowed Tools Test"));
	}

	@Test
	void preservesSuccessfulRealToolWithSameName() {
		ToolCallRequest request = request("allowed-tools-test");
		ToolCallResponse response = interceptor.interceptToolCall(request,
				ignored -> ToolCallResponse.success(request.getToolCallId(), request.getToolName(), "real tool result"));

		assertEquals("real tool result", response.getResult());
		assertEquals(ToolCallResponse.SUCCESS_STATUS, response.getStatus());
	}

	@Test
	void preservesUnresolvedResponseForUnknownNonSkillTool() {
		ToolCallRequest request = request("unknown-tool");
		ToolCallResponse response = interceptor.interceptToolCall(request, ignored -> unresolved(request));

		assertEquals("Tool not available: unknown-tool", response.getResult());
		assertEquals("error", response.getStatus());
	}

	private ToolCallRequest request(String toolName) {
		return ToolCallRequest.builder()
				.toolName(toolName)
				.arguments("{}")
				.toolCallId("call-1")
				.context(Map.of())
				.build();
	}

	private ToolCallResponse unresolved(ToolCallRequest request) {
		return ToolCallResponse.builder()
				.content("Tool not available: " + request.getToolName())
				.toolName(request.getToolName())
				.toolCallId(request.getToolCallId())
				.status("error")
				.metadata(Map.of("error", true, "unresolvedToolName", request.getToolName()))
				.build();
	}

}
