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

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.extension.tools.filesystem.ReadFileTool;
import com.alibaba.cloud.ai.graph.agent.interceptor.skills.SkillsInterceptor;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelCallHandler;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelRequest;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelResponse;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class SkillsInterceptorTest {

	private static final String TEST_SKILLS_DIR = "src/test/resources/skills";

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
		assertEquals("Project skill", interceptor.listSkills().get(0).getDescription());
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
			assertTrue(sysMsg.getText().contains("CRITICAL: Skills are NOT tools!"));
			assertTrue(sysMsg.getText().contains("MANDATORY Process"));
			assertTrue(sysMsg.getText().contains("MUST read"));
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
	void testRuntimeManagement(@TempDir Path tempDir) throws Exception {
		Path skillDir = tempDir.resolve("test-skill");
		Files.createDirectories(skillDir);
		Files.writeString(skillDir.resolve("SKILL.md"),
			"---\nname: test-skill\ndescription: Original\n---\n# Test");

		SkillsInterceptor interceptor = SkillsInterceptor.builder()
			.userSkillsDirectory(tempDir.toString())
			.autoScan(true)
			.build();

		assertEquals(1, interceptor.getSkillCount());

		// Test unload
		interceptor.unloadSkill("test-skill");
		assertEquals(0, interceptor.getSkillCount());

		// Test load
		interceptor.loadSkill(skillDir.toString());
		assertEquals(1, interceptor.getSkillCount());

		// Test reload with updated content
		Files.writeString(skillDir.resolve("SKILL.md"),
			"---\nname: test-skill\ndescription: Updated\n---\n# Test");

		interceptor.reloadSkills();
		assertEquals("Updated", interceptor.listSkills().get(0).getDescription());
	}

	@Test
	void testLoadSkillWithInvalidDirectory(@TempDir Path tempDir) {
		SkillsInterceptor interceptor = SkillsInterceptor.builder()
			.userSkillsDirectory(tempDir.toString())
			.autoScan(false)
			.build();

		// Test null directory
		assertThrows(IllegalArgumentException.class, () -> {
			interceptor.loadSkill(null);
		});

		// Test empty directory
		assertThrows(IllegalArgumentException.class, () -> {
			interceptor.loadSkill("");
		});

		// Test non-existent directory
		assertThrows(RuntimeException.class, () -> {
			interceptor.loadSkill(tempDir.resolve("non-existent").toString());
		});
	}

	@Test
	void testUnloadSkillWithInvalidName(@TempDir Path tempDir) throws Exception {
		Path skillDir = tempDir.resolve("test-skill");
		Files.createDirectories(skillDir);
		Files.writeString(skillDir.resolve("SKILL.md"),
			"---\nname: test-skill\ndescription: Test\n---\n# Test");

		SkillsInterceptor interceptor = SkillsInterceptor.builder()
			.userSkillsDirectory(tempDir.toString())
			.autoScan(true)
			.build();

		// Test null skill name
		assertThrows(IllegalArgumentException.class, () -> {
			interceptor.unloadSkill(null);
		});

		// Test empty skill name
		assertThrows(IllegalArgumentException.class, () -> {
			interceptor.unloadSkill("");
		});

		// Test non-existent skill
		assertThrows(IllegalStateException.class, () -> {
			interceptor.unloadSkill("non-existent-skill");
		});
	}

	@Test
	@EnabledIfEnvironmentVariable(named = "AI_DASHSCOPE_API_KEY", matches = ".+")
	void testIntegrationWithReactAgent(@TempDir Path tempDir) throws Exception {
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

		// Create a simple test file
		Path testFile = tempDir.resolve("test.txt");
		Files.writeString(testFile, "Hello, this is a test file for skills integration.");

		// Users must manually configure tools when creating agents
		ReactAgent agent = ReactAgent.builder()
			.name("skills-test-agent")
			.model(chatModel)
			.saver(new MemorySaver())
			.interceptors(interceptor)
			.tools(ReadFileTool.createReadFileToolCallback(ReadFileTool.DESCRIPTION))
			.enableLogging(true)
			.build();

		// Simple task: read a file
		List<Message> messages = List.of(
			new UserMessage("Read the file at " + testFile.toAbsolutePath() + " and tell me what it contains.")
		);

		RunnableConfig config = RunnableConfig.builder()
			.threadId("test-thread")
			.build();

		Optional<OverAllState> result = agent.invoke(messages, config);

		assertTrue(result.isPresent());
		@SuppressWarnings("unchecked")
		List<Message> finalMessages = (List<Message>) result.get().value("messages").get();
		assertNotNull(finalMessages);
		
		// Verify that the agent used read_file tool
		boolean hasToolResponse = finalMessages.stream()
			.anyMatch(m -> m instanceof ToolResponseMessage);
		assertTrue(hasToolResponse, "Agent should have used read_file tool");
		
		// Verify final response mentions the file content
		Message lastMessage = finalMessages.get(finalMessages.size() - 1);
		assertTrue(lastMessage instanceof AssistantMessage);
		assertTrue(lastMessage.getText().toLowerCase().contains("test file"), 
			"Response should mention the file content");
	}
}
