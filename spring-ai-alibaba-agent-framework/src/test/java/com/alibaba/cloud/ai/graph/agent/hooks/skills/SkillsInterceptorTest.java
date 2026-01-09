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
package com.alibaba.cloud.ai.graph.agent.hooks.skills;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.hook.shelltool.ShellToolAgentHook;
import com.alibaba.cloud.ai.graph.agent.interceptor.skills.SkillMetadata;
import com.alibaba.cloud.ai.graph.agent.interceptor.skills.SkillRegistry;
import com.alibaba.cloud.ai.graph.agent.interceptor.skills.SkillScanner;
import com.alibaba.cloud.ai.graph.agent.interceptor.skills.SkillsInterceptor;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelCallHandler;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelRequest;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelResponse;
import com.alibaba.cloud.ai.graph.agent.tools.ShellTool;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.tool.ToolCallback;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class SkillsInterceptorTest {

	private static final String TEST_SKILLS_DIR = "src/test/resources/skills";

	@Nested
	class SkillMetadataTests {

		@Test
		void testBuilder() {
			SkillMetadata skill = SkillMetadata.builder()
				.name("test-skill")
				.description("Test description")
				.skillPath("/path/to/skill")
				.allowedTools(List.of("read", "write"))
				.model("gpt-4")
				.source("user")
				.build();

			assertEquals("test-skill", skill.getName());
			assertEquals("Test description", skill.getDescription());
			assertEquals("/path/to/skill", skill.getSkillPath());
			assertEquals(2, skill.getAllowedTools().size());
			assertEquals("gpt-4", skill.getModel());
			assertEquals("user", skill.getSource());
		}

		@Test
		void testBuilderValidation() {
			assertThrows(IllegalStateException.class, () -> SkillMetadata.builder().build());
			assertThrows(IllegalStateException.class, () -> SkillMetadata.builder().name("test").build());
			assertThrows(IllegalStateException.class, () -> 
				SkillMetadata.builder().name("test").description("desc").build());
		}

		@Test
		void testLazyLoadContent(@TempDir Path tempDir) throws IOException {
			Path skillDir = tempDir.resolve("test-skill");
			Files.createDirectories(skillDir);
			
			String content = "---\nname: test\ndescription: test\n---\n\n# Test Skill\n\nInstructions here.";
			Files.writeString(skillDir.resolve("SKILL.md"), content);

			SkillMetadata skill = SkillMetadata.builder()
				.name("test-skill")
				.description("Test")
				.skillPath(skillDir.toString())
				.build();

			String loaded = skill.loadFullContent();
			assertNotNull(loaded);
			assertTrue(loaded.contains("# Test Skill"));
			assertFalse(loaded.contains("---"));
			
			String loadedAgain = skill.loadFullContent();
			assertSame(loaded, loadedAgain);
		}

		@Test
		void testLoadContentWithoutFrontmatter(@TempDir Path tempDir) throws IOException {
			Path skillDir = tempDir.resolve("test-skill");
			Files.createDirectories(skillDir);
			
			String content = "# Test Skill\n\nNo frontmatter.";
			Files.writeString(skillDir.resolve("SKILL.md"), content);

			SkillMetadata skill = SkillMetadata.builder()
				.name("test-skill")
				.description("Test")
				.skillPath(skillDir.toString())
				.build();

			String loaded = skill.loadFullContent();
			assertEquals(content, loaded);
		}

		@Test
		void testLoadContentFileNotFound() {
			SkillMetadata skill = SkillMetadata.builder()
				.name("test-skill")
				.description("Test")
				.skillPath("/nonexistent/path")
				.build();

			assertThrows(IOException.class, skill::loadFullContent);
		}
	}

	@Nested
	class SkillScannerTests {

		@Test
		void testScanDirectory(@TempDir Path tempDir) throws Exception {
			Path skill1 = tempDir.resolve("skill1");
			Files.createDirectories(skill1);
			Files.writeString(skill1.resolve("SKILL.md"), 
				"---\nname: skill1\ndescription: First skill\nallowed-tools: [read, write]\n---\n# Skill 1");

			Path skill2 = tempDir.resolve("skill2");
			Files.createDirectories(skill2);
			Files.writeString(skill2.resolve("SKILL.md"), 
				"---\nname: skill2\ndescription: Second skill\n---\n# Skill 2");

			SkillScanner scanner = new SkillScanner();
			List<SkillMetadata> skills = scanner.scan(tempDir.toString(), "user");

			assertEquals(2, skills.size());
			
			SkillMetadata s1 = skills.stream().filter(s -> s.getName().equals("skill1")).findFirst().orElse(null);
			assertNotNull(s1);
			assertEquals("First skill", s1.getDescription());
			assertEquals(2, s1.getAllowedTools().size());
			assertEquals("user", s1.getSource());

			SkillMetadata s2 = skills.stream().filter(s -> s.getName().equals("skill2")).findFirst().orElse(null);
			assertNotNull(s2);
			assertEquals("Second skill", s2.getDescription());
			assertEquals("user", s2.getSource());
		}

		@Test
		void testScanNonexistentDirectory() {
			SkillScanner scanner = new SkillScanner();
			List<SkillMetadata> skills = scanner.scan("/nonexistent/path", "user");
			assertTrue(skills.isEmpty());
		}

		@Test
		void testScanEmptyDirectory(@TempDir Path tempDir) {
			SkillScanner scanner = new SkillScanner();
			List<SkillMetadata> skills = scanner.scan(tempDir.toString(), "user");
			assertTrue(skills.isEmpty());
		}

		@Test
		void testLoadSkillWithMissingFields(@TempDir Path tempDir) throws Exception {
			Path skillDir = tempDir.resolve("invalid-skill");
			Files.createDirectories(skillDir);
			Files.writeString(skillDir.resolve("SKILL.md"), 
				"---\nname: test\n---\n# Missing description");

			SkillScanner scanner = new SkillScanner();
			SkillMetadata skill = scanner.loadSkill(skillDir, "user");
			
			assertNull(skill);
		}

		@Test
		void testLoadSkillWithInvalidYaml(@TempDir Path tempDir) throws Exception {
			Path skillDir = tempDir.resolve("invalid-yaml");
			Files.createDirectories(skillDir);
			Files.writeString(skillDir.resolve("SKILL.md"), 
				"---\ninvalid: yaml: content:\n---\n# Test");

			SkillScanner scanner = new SkillScanner();
			SkillMetadata skill = scanner.loadSkill(skillDir, "user");
			
			assertNull(skill);
		}

		@Test
		void testLoadSkillWithAllowedToolsAsString(@TempDir Path tempDir) throws Exception {
			Path skillDir = tempDir.resolve("tools-string");
			Files.createDirectories(skillDir);
			Files.writeString(skillDir.resolve("SKILL.md"), 
				"---\nname: test\ndescription: Test\nallowed-tools: read, write, shell\n---\n# Test");

			SkillScanner scanner = new SkillScanner();
			SkillMetadata skill = scanner.loadSkill(skillDir, "user");
			
			assertNotNull(skill);
			assertEquals(3, skill.getAllowedTools().size());
			assertTrue(skill.getAllowedTools().contains("read"));
		}

		@Test
		void testScanWithDifferentSources(@TempDir Path tempDir) throws Exception {
			Path skillDir = tempDir.resolve("test-skill");
			Files.createDirectories(skillDir);
			Files.writeString(skillDir.resolve("SKILL.md"), 
				"---\nname: test\ndescription: Test\n---\n# Test");

			SkillScanner scanner = new SkillScanner();
			
			List<SkillMetadata> userSkills = scanner.scan(tempDir.toString(), "user");
			assertEquals("user", userSkills.get(0).getSource());

			List<SkillMetadata> projectSkills = scanner.scan(tempDir.toString(), "project");
			assertEquals("project", projectSkills.get(0).getSource());
		}
	}

	@Nested
	class SkillRegistryTests {

		private SkillRegistry registry;

		@BeforeEach
		void setUp() {
			registry = new SkillRegistry();
		}

		@Test
		void testRegisterAndGet() {
			SkillMetadata skill = SkillMetadata.builder()
				.name("test-skill")
				.description("Test")
				.skillPath("/path")
				.build();

			registry.register(skill);

			Optional<SkillMetadata> retrieved = registry.get("test-skill");
			assertTrue(retrieved.isPresent());
			assertEquals("test-skill", retrieved.get().getName());
		}

		@Test
		void testRegisterNull() {
			assertThrows(IllegalArgumentException.class, () -> registry.register(null));
		}

		@Test
		void testRegisterOverwrite() {
			SkillMetadata skill1 = SkillMetadata.builder()
				.name("test")
				.description("First")
				.skillPath("/path1")
				.build();

			SkillMetadata skill2 = SkillMetadata.builder()
				.name("test")
				.description("Second")
				.skillPath("/path2")
				.build();

			registry.register(skill1);
			registry.register(skill2);

			assertEquals(1, registry.size());
			assertEquals("Second", registry.get("test").get().getDescription());
		}

		@Test
		void testRegisterAll() {
			List<SkillMetadata> skills = List.of(
				SkillMetadata.builder().name("skill1").description("D1").skillPath("/p1").build(),
				SkillMetadata.builder().name("skill2").description("D2").skillPath("/p2").build()
			);

			registry.registerAll(skills);

			assertEquals(2, registry.size());
			assertTrue(registry.contains("skill1"));
			assertTrue(registry.contains("skill2"));
		}

		@Test
		void testGetAllRequiredTools() {
			registry.register(SkillMetadata.builder()
				.name("s1")
				.description("D")
				.skillPath("/p")
				.allowedTools(List.of("read", "write"))
				.build());

			registry.register(SkillMetadata.builder()
				.name("s2")
				.description("D")
				.skillPath("/p")
				.allowedTools(List.of("write", "shell"))
				.build());

			Set<String> tools = registry.getAllRequiredTools();
			assertEquals(3, tools.size());
			assertTrue(tools.contains("read"));
			assertTrue(tools.contains("write"));
			assertTrue(tools.contains("shell"));
		}
	}

	@Nested
	class SkillsInterceptorUnitTests {

		@Test
		void testAutoScanOnBuild(@TempDir Path tempDir) throws Exception {
			Path skillDir = tempDir.resolve("test-skill");
			Files.createDirectories(skillDir);
			Files.writeString(skillDir.resolve("SKILL.md"), 
				"---\nname: test-skill\ndescription: Test\n---\n# Test");

			SkillsInterceptor interceptor = SkillsInterceptor.builder()
				.userSkillsDirectory(tempDir.toString())
				.autoScan(true)
				.build();

			assertEquals(1, interceptor.getSkillCount());
			assertTrue(interceptor.hasSkill("test-skill"));
		}

		@Test
		void testNoAutoScan(@TempDir Path tempDir) throws Exception {
			Path skillDir = tempDir.resolve("test-skill");
			Files.createDirectories(skillDir);
			Files.writeString(skillDir.resolve("SKILL.md"), 
				"---\nname: test-skill\ndescription: Test\n---\n# Test");

			SkillsInterceptor interceptor = SkillsInterceptor.builder()
				.userSkillsDirectory(tempDir.toString())
				.autoScan(false)
				.build();

			assertEquals(0, interceptor.getSkillCount());
		}

		@Test
		void testLazyLoadOnFirstCall(@TempDir Path tempDir) throws Exception {
			Path skillDir = tempDir.resolve("test-skill");
			Files.createDirectories(skillDir);
			Files.writeString(skillDir.resolve("SKILL.md"), 
				"---\nname: test-skill\ndescription: Test\n---\n# Test");

			SkillsInterceptor interceptor = SkillsInterceptor.builder()
				.userSkillsDirectory(tempDir.toString())
				.autoScan(false)
				.build();

			assertEquals(0, interceptor.getSkillCount());

			ModelRequest request = ModelRequest.builder()
				.messages(List.of())
				.context(new HashMap<>())
				.build();

			ModelCallHandler handler = req -> {
				assertNotNull(req.getSystemMessage());
				assertTrue(req.getSystemMessage().getText().contains("test-skill"));
				return new ModelResponse(new ChatResponse(List.of(new Generation(new AssistantMessage("ok")))));
			};

			interceptor.interceptModel(request, handler);

			assertEquals(1, interceptor.getSkillCount());
		}

		@Test
		void testTwoLevelDirectories(@TempDir Path tempDir) throws Exception {
			Path userDir = tempDir.resolve("user");
			Path projectDir = tempDir.resolve("project");
			Files.createDirectories(userDir);
			Files.createDirectories(projectDir);

			Path userSkill = userDir.resolve("test-skill");
			Files.createDirectories(userSkill);
			Files.writeString(userSkill.resolve("SKILL.md"), 
				"---\nname: test-skill\ndescription: User skill\n---\n# User");

			Path projectSkill = projectDir.resolve("test-skill");
			Files.createDirectories(projectSkill);
			Files.writeString(projectSkill.resolve("SKILL.md"), 
				"---\nname: test-skill\ndescription: Project skill\n---\n# Project");

			SkillsInterceptor interceptor = SkillsInterceptor.builder()
				.userSkillsDirectory(userDir.toString())
				.projectSkillsDirectory(projectDir.toString())
				.autoScan(true)
				.build();

			assertEquals(1, interceptor.getSkillCount());
			
			List<SkillMetadata> skills = interceptor.listSkills();
			assertEquals("project", skills.get(0).getSource());
			assertEquals("Project skill", skills.get(0).getDescription());
		}

		@Test
		void testSystemPromptInjection(@TempDir Path tempDir) throws Exception {
			Path skillDir = tempDir.resolve("test-skill");
			Files.createDirectories(skillDir);
			Files.writeString(skillDir.resolve("SKILL.md"), 
				"---\nname: test-skill\ndescription: Test skill\n---\n# Test");

			SkillsInterceptor interceptor = SkillsInterceptor.builder()
				.userSkillsDirectory(tempDir.toString())
				.autoScan(true)
				.build();

			ModelRequest request = ModelRequest.builder()
				.messages(List.of())
				.context(new HashMap<>())
				.build();

			ModelCallHandler handler = req -> {
				SystemMessage sysMsg = req.getSystemMessage();
				assertNotNull(sysMsg);
				assertTrue(sysMsg.getText().contains("Skills System"));
				assertTrue(sysMsg.getText().contains("test-skill"));
				assertTrue(sysMsg.getText().contains("Test skill"));
				return new ModelResponse(new ChatResponse(List.of(new Generation(new AssistantMessage("ok")))));
			};

			interceptor.interceptModel(request, handler);
		}

		@Test
		void testSystemPromptAppend(@TempDir Path tempDir) throws Exception {
			Path skillDir = tempDir.resolve("test-skill");
			Files.createDirectories(skillDir);
			Files.writeString(skillDir.resolve("SKILL.md"), 
				"---\nname: test-skill\ndescription: Test\n---\n# Test");

			SkillsInterceptor interceptor = SkillsInterceptor.builder()
				.userSkillsDirectory(tempDir.toString())
				.autoScan(true)
				.build();

			ModelRequest request = ModelRequest.builder()
				.systemMessage(new SystemMessage("Existing prompt"))
				.messages(List.of())
				.context(new HashMap<>())
				.build();

			ModelCallHandler handler = req -> {
				SystemMessage sysMsg = req.getSystemMessage();
				assertTrue(sysMsg.getText().contains("Existing prompt"));
				assertTrue(sysMsg.getText().contains("Skills System"));
				return new ModelResponse(new ChatResponse(List.of(new Generation(new AssistantMessage("ok")))));
			};

			interceptor.interceptModel(request, handler);
		}

		@Test
		void testRuntimeLoadSkill(@TempDir Path tempDir) throws Exception {
			SkillsInterceptor interceptor = SkillsInterceptor.builder()
				.userSkillsDirectory(tempDir.toString())
				.autoScan(true)
				.build();

			assertEquals(0, interceptor.getSkillCount());

			Path skillDir = tempDir.resolve("new-skill");
			Files.createDirectories(skillDir);
			Files.writeString(skillDir.resolve("SKILL.md"), 
				"---\nname: new-skill\ndescription: New\n---\n# New");

			boolean loaded = interceptor.loadSkill(skillDir.toString());
			assertTrue(loaded);
			assertEquals(1, interceptor.getSkillCount());
		}

		@Test
		void testRuntimeUnloadSkill(@TempDir Path tempDir) throws Exception {
			Path skillDir = tempDir.resolve("test-skill");
			Files.createDirectories(skillDir);
			Files.writeString(skillDir.resolve("SKILL.md"), 
				"---\nname: test-skill\ndescription: Test\n---\n# Test");

			SkillsInterceptor interceptor = SkillsInterceptor.builder()
				.userSkillsDirectory(tempDir.toString())
				.autoScan(true)
				.build();

			assertEquals(1, interceptor.getSkillCount());

			boolean unloaded = interceptor.unloadSkill("test-skill");
			assertTrue(unloaded);
			assertEquals(0, interceptor.getSkillCount());
		}

		@Test
		void testReloadSkills(@TempDir Path tempDir) throws Exception {
			Path skillDir = tempDir.resolve("test-skill");
			Files.createDirectories(skillDir);
			Files.writeString(skillDir.resolve("SKILL.md"), 
				"---\nname: test-skill\ndescription: Original\n---\n# Test");

			SkillsInterceptor interceptor = SkillsInterceptor.builder()
				.userSkillsDirectory(tempDir.toString())
				.autoScan(true)
				.build();

			assertEquals(1, interceptor.getSkillCount());
			assertEquals("Original", interceptor.listSkills().get(0).getDescription());

			Files.writeString(skillDir.resolve("SKILL.md"), 
				"---\nname: test-skill\ndescription: Updated\n---\n# Test");

			interceptor.reloadSkills();

			assertEquals(1, interceptor.getSkillCount());
			assertEquals("Updated", interceptor.listSkills().get(0).getDescription());
		}
	}

	@Nested
	@EnabledIfEnvironmentVariable(named = "AI_DASHSCOPE_API_KEY", matches = ".+")
	class IntegrationTests {

		@Test
		void testSkillsInterceptorIntegration(@TempDir Path tempDir) throws Exception {
			DashScopeApi dashScopeApi = DashScopeApi.builder()
				.apiKey(System.getenv("AI_DASHSCOPE_API_KEY"))
				.build();
			ChatModel chatModel = DashScopeChatModel.builder()
				.dashScopeApi(dashScopeApi)
				.build();

			SkillsInterceptor interceptor = SkillsInterceptor.builder()
				.userSkillsDirectory(TEST_SKILLS_DIR)
				.autoScan(true)
				.build();

			assertEquals(1, interceptor.getSkillCount());
			assertTrue(interceptor.hasSkill("pdf-extractor"));

			Path testPdf = tempDir.resolve("test-report.pdf");
			Files.writeString(testPdf, "Mock PDF content for testing");

			Path skillsDir = tempDir.resolve("skills").resolve("pdf-extractor").resolve("scripts");
			Files.createDirectories(skillsDir);
			Path scriptPath = skillsDir.resolve("extract_pdf.py");

			Path sourceScript = Path.of(TEST_SKILLS_DIR, "pdf-extractor", "scripts", "extract_pdf.py");
			Files.copy(sourceScript, scriptPath);

			List<ToolCallback> tools = new ArrayList<>(interceptor.getTools());
			tools.add(ShellTool.builder(tempDir.toString()).build());

			ReactAgent agent = ReactAgent.builder()
				.name("skills-test-agent")
				.model(chatModel)
				.saver(new MemorySaver())
				.hooks(
					ShellToolAgentHook.builder()
						.shellToolName("shell")
						.build()
				)
				.interceptors(interceptor)
				.tools(tools)
				.build();

			List<Message> messages = List.of(
				new UserMessage("Extract content from " + testPdf.toAbsolutePath() + 
					". Script at " + scriptPath.toAbsolutePath())
			);

			RunnableConfig config = RunnableConfig.builder()
				.threadId("test-integration-thread")
				.build();

			Optional<OverAllState> result = agent.invoke(messages, config);

			assertTrue(result.isPresent());

			@SuppressWarnings("unchecked")
			List<Message> finalMessages = (List<Message>) result.get().value("messages").get();
			assertNotNull(finalMessages);

			boolean hasAssistantResponse = finalMessages.stream()
				.anyMatch(m -> m instanceof AssistantMessage);
			assertTrue(hasAssistantResponse);
		}
	}
}
