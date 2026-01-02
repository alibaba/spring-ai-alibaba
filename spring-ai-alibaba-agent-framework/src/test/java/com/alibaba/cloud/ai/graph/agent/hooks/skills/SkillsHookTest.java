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
import com.alibaba.cloud.ai.graph.agent.hook.skills.SkillsHook;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Integration tests for SkillsHook with ReactAgent.
 * 
 * These tests verify the end-to-end behavior of Skills integration,
 * testing through the public API only.
 */
class SkillsHookTest {

	private static final String TEST_SKILLS_DIR = "src/test/resources/skills";

	private ChatModel mockChatModel;

	@BeforeEach
	void setUp() {
		// Create mock ChatModel for unit tests
		mockChatModel = mock(ChatModel.class);
		ChatResponse mockResponse = new ChatResponse(
			List.of(new Generation(new AssistantMessage("I'll help you with that task.")))
		);
		when(mockChatModel.call(any(Prompt.class))).thenReturn(mockResponse);
	}
	@Test
	void testSkillsHookBuilderWithMultipleDirectories(@TempDir Path tempDir) throws IOException {
		// Create a temporary skill
		Path skillDir = tempDir.resolve("temp-skill");
		Files.createDirectories(skillDir);
		
		String skillContent = """
			---
			name: temp-skill
			description: Temporary test skill
			keywords: [temp, test]
			---
			# Temp Skill
			Test content
			""";
		Files.writeString(skillDir.resolve("SKILL.md"), skillContent);

		// Build hook with multiple directories
		SkillsHook hook = SkillsHook.builder()
			.skillsDirectory(TEST_SKILLS_DIR)
			.skillsDirectory(tempDir.toString())
			.build();

		assertNotNull(hook);
		assertEquals(4, hook.getSkillCount());
		assertTrue(hook.hasSkill("temp-skill"));
	}

	/**
	 * Test 3: Builder with auto-scan disabled
	 */
	@Test
	void testSkillsHookBuilderWithAutoScanDisabled() {
		SkillsHook hook = SkillsHook.builder()
			.skillsDirectory(TEST_SKILLS_DIR)
			.autoScan(false)
			.build();

		assertNotNull(hook);
		assertEquals(0, hook.getSkillCount());
	}

	/**
	 * Test 4: Integration with ReactAgent - verify skills are injected into messages
	 */
	@Test
	void testSkillsHookWithReactAgent() throws Exception {
		// Create hook with skills
		SkillsHook hook = SkillsHook.builder()
			.skillsDirectory(TEST_SKILLS_DIR)
			.build();

		// Create ReactAgent with skills hook
		ReactAgent agent = ReactAgent.builder()
			.name("test-skills-agent")
			.model(mockChatModel)
			.saver(new MemorySaver())
			.hooks(hook)
			.build();

		assertNotNull(agent);

		// Test agent invocation with PDF extraction request
		List<Message> messages = new ArrayList<>();
		messages.add(new UserMessage("Extract tables from report.pdf"));

		Optional<OverAllState> result = agent.invoke(messages);

		assertTrue(result.isPresent());
		
		// Verify messages were processed
		Optional<Object> resultMessages = result.get().value("messages");
		assertTrue(resultMessages.isPresent());
		
		@SuppressWarnings("unchecked")
		List<Message> finalMessages = (List<Message>) resultMessages.get();
		
		// Should have system messages with skills information
		long systemMessageCount = finalMessages.stream()
			.filter(m -> m instanceof SystemMessage)
			.count();
		assertTrue(systemMessageCount > 0, "Should have system messages with skills");
		
		// Verify skills list was injected
		boolean hasSkillsList = finalMessages.stream()
			.filter(m -> m instanceof SystemMessage)
			.anyMatch(m -> m.getText().contains("Available Skills") || 
						   m.getText().contains("pdf-extractor"));
		assertTrue(hasSkillsList, "Should have skills list in system messages");
	}

	/**
	 * Test 5: Skills matching - verify relevant skills are loaded based on user request
	 */
	@Test
	void testSkillsMatchingForPDFExtraction() throws Exception {
		SkillsHook hook = SkillsHook.builder()
			.skillsDirectory(TEST_SKILLS_DIR)
			.build();

		ReactAgent agent = ReactAgent.builder()
			.name("test-pdf-agent")
			.model(mockChatModel)
			.saver(new MemorySaver())
			.hooks(hook)
			.build();

		// Request related to PDF extraction
		List<Message> messages = new ArrayList<>();
		messages.add(new UserMessage("Extract tables from report.pdf"));

		RunnableConfig config = RunnableConfig.builder()
			.threadId("test-pdf-thread")
			.build();

		Optional<OverAllState> result = agent.invoke(messages, config);

		assertTrue(result.isPresent());
		
		@SuppressWarnings("unchecked")
		List<Message> finalMessages = (List<Message>) result.get().value("messages").get();
		
		// Verify pdf-extractor skill was loaded
		boolean hasPdfSkill = finalMessages.stream()
			.filter(m -> m instanceof SystemMessage)
			.anyMatch(m -> m.getText().contains("pdf-extractor") || 
						   m.getText().contains("Active Skill"));
		assertTrue(hasPdfSkill, "Should load pdf-extractor skill for PDF-related request");
	}

	/**
	 * Test 6: Skills matching - verify code review skill is loaded
	 */
	@Test
	void testSkillsMatchingForCodeReview() throws Exception {
		SkillsHook hook = SkillsHook.builder()
			.skillsDirectory(TEST_SKILLS_DIR)
			.build();

		ReactAgent agent = ReactAgent.builder()
			.name("test-code-review-agent")
			.model(mockChatModel)
			.saver(new MemorySaver())
			.hooks(hook)
			.build();

		// Request related to code review
		List<Message> messages = new ArrayList<>();
		messages.add(new UserMessage("Review this code for security issues"));

		RunnableConfig config = RunnableConfig.builder()
			.threadId("test-code-thread")
			.build();

		Optional<OverAllState> result = agent.invoke(messages, config);

		assertTrue(result.isPresent());
		
		@SuppressWarnings("unchecked")
		List<Message> finalMessages = (List<Message>) result.get().value("messages").get();
		
		// Verify code-reviewer skill was loaded
		boolean hasCodeReviewSkill = finalMessages.stream()
			.filter(m -> m instanceof SystemMessage)
			.anyMatch(m -> m.getText().contains("code-reviewer") || 
						   m.getText().contains("Active Skill"));
		assertTrue(hasCodeReviewSkill, "Should load code-reviewer skill for code review request");
	}

	/**
	 * Multi-round conversation - verify skills are not redundantly loaded
	 */
	@Test
	void testSkillsNotRedundantlyLoaded() throws Exception {
		SkillsHook hook = SkillsHook.builder()
			.skillsDirectory(TEST_SKILLS_DIR)
			.build();

		ReactAgent agent = ReactAgent.builder()
			.name("test-multi-round-agent")
			.model(mockChatModel)
			.saver(new MemorySaver())
			.hooks(hook)
			.build();

		String threadId = "test-multi-round-thread";
		RunnableConfig config = RunnableConfig.builder()
			.threadId(threadId)
			.build();

		// First round - PDF extraction
		List<Message> messages1 = new ArrayList<>();
		messages1.add(new UserMessage("Extract tables from report.pdf"));
		Optional<OverAllState> result1 = agent.invoke(messages1, config);
		assertTrue(result1.isPresent());

		// Second round - same topic (PDF)
		List<Message> messages2 = new ArrayList<>();
		messages2.add(new UserMessage("Also extract the images from the PDF"));
		Optional<OverAllState> result2 = agent.invoke(messages2, config);
		assertTrue(result2.isPresent());

		// Verify skills were loaded in first round but not redundantly in second
		@SuppressWarnings("unchecked")
		List<Message> finalMessages = (List<Message>) result2.get().value("messages").get();
		
		// Count how many times pdf-extractor appears
		long pdfSkillCount = finalMessages.stream()
			.filter(m -> m instanceof SystemMessage)
			.filter(m -> m.getText().contains("Active Skill") && 
						 m.getText().contains("pdf-extractor"))
			.count();
		
		// Should only appear once (not redundantly loaded)
		assertTrue(pdfSkillCount <= 1, "pdf-extractor skill should not be redundantly loaded");
	}

	/**
	 * Test 8: Empty directory handling
	 */
	@Test
	void testEmptyDirectory(@TempDir Path tempDir) {
		SkillsHook hook = SkillsHook.builder()
			.skillsDirectory(tempDir.toString())
			.build();

		assertNotNull(hook);
		assertEquals(0, hook.getSkillCount());
	}

	/**
	 * Non-existent directory handling
	 */
	@Test
	void testNonExistentDirectory() {
		SkillsHook hook = SkillsHook.builder()
			.skillsDirectory("/non/existent/path")
			.build();

		assertNotNull(hook);
		assertEquals(0, hook.getSkillCount());
	}

	/**
	 * Integration test with real DashScope API
	 * Tests end-to-end skills integration with actual LLM
	 */
	@Test
	@EnabledIfEnvironmentVariable(named = "AI_DASHSCOPE_API_KEY", matches = ".+")
	void testSkillsWithRealLLM() throws Exception {
		DashScopeApi dashScopeApi = DashScopeApi.builder()
			.apiKey(System.getenv("AI_DASHSCOPE_API_KEY"))
			.build();
		ChatModel chatModel = DashScopeChatModel.builder()
			.dashScopeApi(dashScopeApi)
			.build();

		SkillsHook hook = SkillsHook.builder()
			.skillsDirectory(TEST_SKILLS_DIR)
			.build();

		// Create ReactAgent with skills hook
		ReactAgent agent = ReactAgent.builder()
			.name("test-real-llm-agent")
			.model(chatModel)
			.saver(new MemorySaver())
			.hooks(hook)
			.build();

		System.out.println("\n=== 测试Skills与真实LLM集成 ===");

		List<Message> messages = new ArrayList<>();
		messages.add(new UserMessage("我需要从PDF文件中提取表格数据，应该怎么做？"));

		RunnableConfig config = RunnableConfig.builder()
			.threadId("test-real-llm-thread")
			.build();

		Optional<OverAllState> result = agent.invoke(messages, config);

		assertTrue(result.isPresent(), "结果应该存在");
		
		@SuppressWarnings("unchecked")
		List<Message> finalMessages = (List<Message>) result.get().value("messages").get();
		
		System.out.println("返回消息数量: " + finalMessages.size());

		for (int i = 0; i < finalMessages.size(); i++) {
			Message msg = finalMessages.get(i);
			System.out.println("Message " + i + " (" + msg.getClass().getSimpleName() + "): " + 
				msg.getText().substring(0, Math.min(100, msg.getText().length())) + "...");
		}

		boolean hasSkillsInfo = finalMessages.stream()
			.filter(m -> m instanceof SystemMessage)
			.anyMatch(m -> m.getText().contains("pdf-extractor") || 
						   m.getText().contains("Available Skills"));
		assertTrue(hasSkillsInfo, "应该包含skills信息");

		boolean hasAssistantResponse = finalMessages.stream()
			.anyMatch(m -> m instanceof AssistantMessage);
		assertTrue(hasAssistantResponse, "应该有LLM的回复");

		System.out.println("✓ Skills成功集成到ReactAgent中");
	}
}
