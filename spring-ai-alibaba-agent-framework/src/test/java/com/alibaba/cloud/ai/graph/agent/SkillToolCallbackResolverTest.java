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

import com.alibaba.cloud.ai.graph.agent.hook.skills.ReadSkillTool;
import com.alibaba.cloud.ai.graph.agent.hook.skills.SkillToolCallbackResolver;
import com.alibaba.cloud.ai.graph.agent.hook.skills.SkillsAgentHook;
import com.alibaba.cloud.ai.graph.agent.node.AgentToolNode;
import com.alibaba.cloud.ai.graph.skills.registry.filesystem.FileSystemSkillRegistry;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.ai.tool.resolution.ToolCallbackResolver;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.BiFunction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@DisplayName("Skill tool resolver tests")
class SkillToolCallbackResolverTest {

	@TempDir
	Path tempDir;

	static class MockChatModel implements ChatModel {
		@Override
		public ChatResponse call(Prompt prompt) {
			return new ChatResponse(List.of(new Generation(new AssistantMessage("Mock response"))));
		}

		@Override
		public Flux<ChatResponse> stream(Prompt prompt) {
			return Flux.just(new ChatResponse(List.of(new Generation(new AssistantMessage("Mock stream response")))));
		}
	}

	static class EchoTool implements BiFunction<String, ToolContext, String> {
		@Override
		public String apply(String input, ToolContext toolContext) {
			return input;
		}
	}

	private FileSystemSkillRegistry createRegistry(String skillName, String skillBody) throws IOException {
		Path skillsDir = tempDir.resolve("skills");
		Path skillDir = skillsDir.resolve(skillName);
		Files.createDirectories(skillDir);
		Files.writeString(skillDir.resolve("SKILL.md"), skillBody, StandardCharsets.UTF_8);
		return FileSystemSkillRegistry.builder()
				.projectSkillsDirectory(skillsDir.toString())
				.build();
	}

	private String buildSkillMarkdown(String skillName, String description, String body) {
		return "---\n" +
				"name: " + skillName + "\n" +
				"description: " + description + "\n" +
				"---\n\n" +
				body + "\n";
	}

	@Test
	@DisplayName("SkillToolCallbackResolver resolves read_skill and ignores unknown tools")
	void resolvesReadSkillTool() throws Exception {
		String skillName = "pdf-extractor";
		String body = "# PDF Extractor\nUse this skill to extract PDFs.";
		FileSystemSkillRegistry registry = createRegistry(
				skillName,
				buildSkillMarkdown(skillName, "Extract PDFs", body)
		);

		String loadedContent = registry.readSkillContent(skillName);
		assertEquals(body, loadedContent);

		SkillToolCallbackResolver resolver = new SkillToolCallbackResolver(registry);
		ToolCallback tool = resolver.resolve(ReadSkillTool.READ_SKILL);
		assertNotNull(tool);
		assertEquals(ReadSkillTool.READ_SKILL, tool.getToolDefinition().name());
		assertNull(resolver.resolve("unknown_tool"));
	}

	@Test
	@DisplayName("Resolver chain resolves both user tools and skill tools")
	void chainedResolverResolvesSkillAndUserTools() throws Exception {
		String skillName = "skill-a";
		FileSystemSkillRegistry registry = createRegistry(
				skillName,
				buildSkillMarkdown(skillName, "Skill A", "# Skill A\nContent")
		);
		SkillsAgentHook skillsHook = SkillsAgentHook.builder().skillRegistry(registry).build();

		ToolCallback echoTool = FunctionToolCallback.builder("echo", new EchoTool())
				.description("Echo input")
				.inputType(String.class)
				.build();
		ToolCallbackResolver userResolver = toolName -> "echo".equals(toolName) ? echoTool : null;

		TestableBuilder builder = new TestableBuilder();
		builder.resolver(userResolver);
		builder.hooks(skillsHook);

		ToolCallbackResolver effectiveResolver = builder.resolveEffectiveResolver();
		assertNotNull(effectiveResolver);
		assertNotNull(effectiveResolver.resolve("echo"));
		assertNotNull(effectiveResolver.resolve(ReadSkillTool.READ_SKILL));
	}

	@Test
	@DisplayName("DefaultBuilder configures skill resolver when SkillsAgentHook is present")
	void buildConfiguresSkillResolver() throws Exception {
		String skillName = "skill-b";
		FileSystemSkillRegistry registry = createRegistry(
				skillName,
				buildSkillMarkdown(skillName, "Skill B", "# Skill B\nContent")
		);
		SkillsAgentHook skillsHook = SkillsAgentHook.builder().skillRegistry(registry).build();

		ReactAgent agent = ReactAgent.builder()
				.name("skill-agent")
				.model(new MockChatModel())
				.hooks(skillsHook)
				.build();

		AgentToolNode toolNode = getToolNode(agent);
		ToolCallbackResolver resolver = getToolCallbackResolver(toolNode);
		assertNotNull(resolver);
		ToolCallback tool = resolver.resolve(ReadSkillTool.READ_SKILL);
		assertNotNull(tool);
		assertEquals(ReadSkillTool.READ_SKILL, tool.getToolDefinition().name());
	}

	private AgentToolNode getToolNode(ReactAgent agent) throws Exception {
		Field toolNodeField = ReactAgent.class.getDeclaredField("toolNode");
		toolNodeField.setAccessible(true);
		return (AgentToolNode) toolNodeField.get(agent);
	}

	private ToolCallbackResolver getToolCallbackResolver(AgentToolNode toolNode) throws Exception {
		Field resolverField = AgentToolNode.class.getDeclaredField("toolCallbackResolver");
		resolverField.setAccessible(true);
		return (ToolCallbackResolver) resolverField.get(toolNode);
	}

	private static class TestableBuilder extends DefaultBuilder {
		ToolCallbackResolver resolveEffectiveResolver() {
			return resolveToolCallbackResolver();
		}
	}

}
