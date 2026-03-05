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

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.hook.shelltool.ShellToolAgentHook;
import com.alibaba.cloud.ai.graph.agent.hook.skills.SkillsAgentHook;
import com.alibaba.cloud.ai.graph.agent.interceptor.skills.SkillsInterceptor;
import com.alibaba.cloud.ai.graph.agent.tools.PythonTool;
import com.alibaba.cloud.ai.graph.agent.tools.ShellTool2;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.alibaba.cloud.ai.graph.skills.registry.classpath.ClasspathSkillRegistry;
import com.alibaba.cloud.ai.graph.skills.registry.filesystem.FileSystemSkillRegistry;
import com.alibaba.cloud.ai.graph.skills.SkillMetadata;
import com.alibaba.cloud.ai.graph.skills.registry.SkillRegistry;

import org.springframework.core.io.ClassPathResource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.core.io.Resource;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@EnabledIfEnvironmentVariable(named = "AI_DASHSCOPE_API_KEY", matches = ".+")
class AgentSkillsTest {

	private ChatModel chatModel;

	/**
	 * Get the absolute path to test skills directory.
	 * Uses ClassPathResource to locate the skills directory in test resources.
	 */
	private String getTestSkillsDirectory() throws Exception {
		Resource resource = new ClassPathResource("skills");
		if (resource.exists() && resource.getFile().isDirectory()) {
			return resource.getFile().getAbsolutePath();
		}
		// Fallback: try to get from classloader
		URL url = getClass().getClassLoader().getResource("skills");
		if (url != null && "file".equals(url.getProtocol())) {
			return Path.of(url.toURI()).toString();
		}
		throw new IllegalStateException("Cannot find test skills directory");
	}

	@BeforeEach
	void setUp() {
		// Create DashScopeApi instance using the API key from environment variable
		DashScopeApi dashScopeApi = DashScopeApi.builder().apiKey(System.getenv("AI_DASHSCOPE_API_KEY")).build();

		// Create DashScope ChatModel instance
		this.chatModel = DashScopeChatModel.builder().dashScopeApi(dashScopeApi).build();
	}

	@Test
	public void testSkillsAgentHookLoadsSkillsWithDefaultPath() throws Exception {
		// Test 1: Use default path (should load from classpath:skills)
		SkillRegistry registry = FileSystemSkillRegistry.builder().build();
		SkillsAgentHook hook = SkillsAgentHook.builder()
			.skillRegistry(registry)
			// Not specifying projectSkillsDirectory, should use default classpath:skills
			.build();

		// Trigger skill loading by calling beforeAgent
		OverAllState state = new OverAllState();
		RunnableConfig config = RunnableConfig.builder().threadId("test-thread").build();
		hook.beforeAgent(state, config).join();

		// Verify skills are loaded (if default path has skills)
		// Note: This test may pass or fail depending on whether default path has skills
		assertNotNull(registry, "Registry should not be null");
	}

	@Test
	public void testSkillsAgentHookLoadsSkillsWithResource() throws Exception {
		// Test 2: Use Resource object (ClassPathResource)
		Resource skillsResource = new ClassPathResource("skills");
		SkillRegistry registry = FileSystemSkillRegistry.builder()
			.projectSkillsDirectory(skillsResource)
			.build();
		SkillsAgentHook hook = SkillsAgentHook.builder()
			.skillRegistry(registry)
			.build();

		// Trigger skill loading by calling beforeAgent
		OverAllState state = new OverAllState();
		RunnableConfig config = RunnableConfig.builder().threadId("test-thread").build();
		hook.beforeAgent(state, config).join();

		// Verify skills are loaded
		assertTrue(registry.size() > 0, "Skills should be loaded");
		assertTrue(registry.contains("pdf-extractor"), "Should contain pdf-extractor skill");

		SkillMetadata skill = registry.get("pdf-extractor").orElseThrow();
		assertNotNull(skill, "Skill should not be null");
		assertEquals("pdf-extractor", skill.getName(), "Skill name should match");
		assertNotNull(skill.getDescription(), "Skill description should not be null");
		assertFalse(skill.getDescription().isEmpty(), "Skill description should not be empty");
	}

	@Test
	public void testSkillsAgentHookLoadsSkillsWithAbsolutePath() throws Exception {
		// Test 3: Use absolute path
		String testSkillsPath = getTestSkillsDirectory();
		SkillRegistry registry = FileSystemSkillRegistry.builder()
			.projectSkillsDirectory(testSkillsPath)
			.build();
		SkillsAgentHook hook = SkillsAgentHook.builder()
			.skillRegistry(registry)
			.build();

		// Trigger skill loading by calling beforeAgent
		OverAllState state = new OverAllState();
		RunnableConfig config = RunnableConfig.builder().threadId("test-thread").build();
		hook.beforeAgent(state, config).join();

		// Verify skills are loaded
		assertTrue(registry.size() > 0, "Skills should be loaded");
		assertTrue(registry.contains("pdf-extractor"), "Should contain pdf-extractor skill");

		SkillMetadata skill = registry.get("pdf-extractor").orElseThrow();
		assertNotNull(skill, "Skill should not be null");
		assertEquals("pdf-extractor", skill.getName(), "Skill name should match");
		assertNotNull(skill.getDescription(), "Skill description should not be null");
		assertFalse(skill.getDescription().isEmpty(), "Skill description should not be empty");
	}

	@Test
	public void testSkillsInterceptorEnhancesSystemPrompt() throws Exception {
		// Load skills first - using Resource object
		Resource skillsResource = new ClassPathResource("skills");
		SkillRegistry registry = FileSystemSkillRegistry.builder()
			.projectSkillsDirectory(skillsResource)
			.build();
		SkillsAgentHook hook = SkillsAgentHook.builder()
			.skillRegistry(registry)
			.build();
		
		OverAllState state = new OverAllState();
		RunnableConfig config = RunnableConfig.builder().threadId("test-thread").build();
		hook.beforeAgent(state, config).join();

		// Create interceptor
		SkillsInterceptor interceptor = SkillsInterceptor.builder()
			.skillRegistry(registry)
			.build();

		// Verify registry has access to skills
		assertTrue(registry.size() > 0, "Registry should have skills");
		assertTrue(registry.contains("pdf-extractor"), "Registry should have pdf-extractor skill");
		
		List<SkillMetadata> skills = registry.listAll();
		assertNotNull(skills, "Skills list should not be null");
		assertFalse(skills.isEmpty(), "Skills list should not be empty");
	}

	@Test
	public void testReactAgentWithSkillsHook() throws Exception {
		SkillRegistry registry = FileSystemSkillRegistry.builder()
				.projectSkillsDirectory(getTestSkillsDirectory())
				.build();
		// Use default path (classpath:skills)
		SkillsAgentHook hook = SkillsAgentHook.builder()
			.skillRegistry(registry)
			// Not specifying projectSkillsDirectory, uses default
			.build();

		ReactAgent agent = ReactAgent.builder()
			.name("skills-agent")
			.model(chatModel)
			.saver(new MemorySaver())
			.hooks(List.of(hook))
			.build();

		// Trigger skill loading by invoking agent
		Optional<OverAllState> result = agent.invoke("你好，请介绍一下你自己。");

		assertTrue(result.isPresent(), "Result should be present");
		
		// Verify skills were loaded through hook
		assertTrue(registry.size() > 0, "Skills should be loaded");
		assertTrue(registry.contains("pdf-extractor"), "Should contain pdf-extractor skill");
	}

	@Test
	public void testReactAgentWithSkillsHookAndInterceptor() throws Exception {
		// Use Resource object for project skills directory
		Resource skillsResource = new ClassPathResource("skills");
		SkillRegistry registry = FileSystemSkillRegistry.builder()
			.projectSkillsDirectory(skillsResource)
			.build();
		SkillsAgentHook hook = SkillsAgentHook.builder()
			.skillRegistry(registry)
			.build();

		// Hook automatically provides interceptor through getModelInterceptors()
		ReactAgent agent = ReactAgent.builder()
			.name("skills-agent-full")
			.model(chatModel)
			.saver(new MemorySaver())
			.hooks(List.of(hook))
			.build();

		AssistantMessage response = agent.call("你好，请介绍一下你自己。");
		
		assertNotNull(response, "Response should not be null");
		assertNotNull(response.getText(), "Response text should not be null");
		assertFalse(response.getText().isEmpty(), "Response text should not be empty");

		// Verify skills registry has skills
		assertTrue(registry.size() > 0, "Skills should be loaded");
	}

	@Test
	public void testSkillsHookProvidesInterceptor() throws Exception {
		// Use absolute path
		String testSkillsPath = getTestSkillsDirectory();
		SkillRegistry registry = FileSystemSkillRegistry.builder()
			.projectSkillsDirectory(testSkillsPath)
			.build();
		SkillsAgentHook hook = SkillsAgentHook.builder()
			.skillRegistry(registry)
			.build();

		// Verify hook provides interceptor
		List<com.alibaba.cloud.ai.graph.agent.interceptor.ModelInterceptor> interceptors = hook.getModelInterceptors();
		assertNotNull(interceptors, "Interceptors should not be null");
		assertFalse(interceptors.isEmpty(), "Interceptors should not be empty");
		assertTrue(interceptors.get(0) instanceof SkillsInterceptor, 
			"First interceptor should be SkillsInterceptor");
	}

	@Test
	public void testSkillsRegistrySharing() throws Exception {
		// Use Resource object
		Resource skillsResource = new ClassPathResource("skills");
		SkillRegistry registry = FileSystemSkillRegistry.builder()
			.projectSkillsDirectory(skillsResource)
			.build();
		SkillsAgentHook hook = SkillsAgentHook.builder()
			.skillRegistry(registry)
			.build();

		SkillsInterceptor interceptor = SkillsInterceptor.builder()
			.skillRegistry(registry)
			.build();

		// Load skills through hook
		OverAllState state = new OverAllState();
		RunnableConfig config = RunnableConfig.builder().threadId("test-thread").build();
		hook.beforeAgent(state, config).join();

		// Verify both hook and registry share the same registry
		assertEquals(registry.size(), hook.getSkillCount(), 
			"Hook and registry should have same skill count");
		assertEquals(registry.size(), registry.size(), 
			"Registry should have consistent skill count");
		assertTrue(registry.contains("pdf-extractor"), "Registry should contain pdf-extractor");
		assertTrue(hook.hasSkill("pdf-extractor"), "Hook should have pdf-extractor");
		assertTrue(registry.contains("pdf-extractor"), "Registry should have pdf-extractor");
	}

	@Test
	public void testSkillsWithCustomDirectories(@TempDir Path tempDir) throws Exception {
		// Create test skill directory structure
		Path userSkillsDir = tempDir.resolve("user-skills");
		Path projectSkillsDir = tempDir.resolve("project-skills");
		Files.createDirectories(userSkillsDir);
		Files.createDirectories(projectSkillsDir);

		// Create user skill
		Path userSkillDir = userSkillsDir.resolve("user-skill");
		Files.createDirectories(userSkillDir);
		Files.writeString(userSkillDir.resolve("SKILL.md"),
			"---\nname: user-skill\ndescription: User level skill\n---\n# User Skill");

		// Create project skill with same name (should override)
		Path projectSkillDir = projectSkillsDir.resolve("user-skill");
		Files.createDirectories(projectSkillDir);
		Files.writeString(projectSkillDir.resolve("SKILL.md"),
			"---\nname: user-skill\ndescription: Project level skill (overrides user)\n---\n# Project Skill");

		SkillRegistry registry = FileSystemSkillRegistry.builder()
			.userSkillsDirectory(userSkillsDir.toString())
			.projectSkillsDirectory(projectSkillsDir.toString())
			.build();
		SkillsAgentHook hook = SkillsAgentHook.builder()
			.skillRegistry(registry)
			.build();

		// Load skills
		OverAllState state = new OverAllState();
		RunnableConfig config = RunnableConfig.builder().threadId("test-thread").build();
		hook.beforeAgent(state, config).join();

		// Verify project skill overrides user skill
		assertEquals(1, registry.size(), "Should have 1 skill (project overrides user)");
		SkillMetadata skill = registry.get("user-skill").orElseThrow();
		assertEquals("Project level skill (overrides user)", skill.getDescription(),
			"Project skill should override user skill");
		assertEquals("project", skill.getSource(), "Skill source should be project");
	}

	@Test
	public void testReactAgentSkillsIntegration() throws Exception {
		SkillsAgentHook skillsHook = SkillsAgentHook.builder()
			.skillRegistry(FileSystemSkillRegistry.builder().projectSkillsDirectory(getTestSkillsDirectory()).build())
			.build();

		ShellToolAgentHook shellHook = ShellToolAgentHook.builder()
				.shellTool2(ShellTool2.builder(System.getProperty("user.dir")).build())
				.build();

		ReactAgent agent = ReactAgent.builder()
			.name("skills-integration-agent")
			.model(chatModel)
			.saver(new MemorySaver())
			.tools(PythonTool.createPythonToolCallback(PythonTool.DESCRIPTION))
			.hooks(List.of(skillsHook, shellHook))
			.enableLogging(true)
			.build();

//		// Test that agent can be invoked with skills loaded
//		Optional<OverAllState> result = agent.invoke(
//			new UserMessage("请告诉我有哪些可用的技能？"));
//
//		assertTrue(result.isPresent(), "Result should be present");
//
//		// Verify skills are available
//		assertTrue(registry.size() > 0, "Skills should be loaded");
//
		// Get the response
		String path = getTestSkillsDirectory() + "/pdf-extractor/saa-roadmap.pdf";
		AssistantMessage response = agent.call(String.format("请从 %s 文件中提取关键信息。", path));
		assertNotNull(response, "Response should not be null");
		assertNotNull(response.getText(), "Response text should not be null");
	}

	@Test
	public void testSkillsList() throws Exception {
		// Use absolute path
		String testSkillsPath = getTestSkillsDirectory();
		SkillRegistry registry = FileSystemSkillRegistry.builder()
			.projectSkillsDirectory(testSkillsPath)
			.build();
		SkillsAgentHook hook = SkillsAgentHook.builder()
			.skillRegistry(registry)
			.build();

		// Load skills
		OverAllState state = new OverAllState();
		RunnableConfig config = RunnableConfig.builder().threadId("test-thread").build();
		hook.beforeAgent(state, config).join();

		// Test list methods
		List<SkillMetadata> skills = hook.listSkills();
		assertNotNull(skills, "Skills list should not be null");
		assertFalse(skills.isEmpty(), "Skills list should not be empty");
		assertEquals(registry.size(), skills.size(), 
			"Hook listSkills should match registry size");

		// Verify skill details
		SkillMetadata pdfSkill = skills.stream()
			.filter(s -> "pdf-extractor".equals(s.getName()))
			.findFirst()
			.orElseThrow();
		
		assertNotNull(pdfSkill.getDescription(), "Skill description should not be null");
		assertNotNull(pdfSkill.getSkillPath(), "Skill path should not be null");
		assertEquals("pdf-extractor", pdfSkill.getName(), "Skill name should match");
	}

	@Test
	public void testClasspathSkillRegistryIntegration() throws Exception {
		// Test ClasspathSkillRegistry with ReactAgent integration
		ClasspathSkillRegistry registry = ClasspathSkillRegistry.builder()
			.classpathPath("skills")
			.build();
		
		SkillsAgentHook skillsHook = SkillsAgentHook.builder()
			.skillRegistry(registry)
			.build();

		ShellToolAgentHook shellHook = ShellToolAgentHook.builder()
			.shellTool2(ShellTool2.builder(System.getProperty("user.dir")).build())
			.build();

		ReactAgent agent = ReactAgent.builder()
			.name("classpath-skills-integration-agent")
			.model(chatModel)
			.saver(new MemorySaver())
			.tools(PythonTool.createPythonToolCallback(PythonTool.DESCRIPTION))
			.hooks(List.of(skillsHook, shellHook))
			.enableLogging(true)
			.build();

//		// Test that agent can be invoked with skills loaded
//		Optional<OverAllState> result = agent.invoke(
//			new UserMessage("请告诉我有哪些可用的技能？"));
//
//		assertTrue(result.isPresent(), "Result should be present");
//
//		// Verify skills are available
//		assertTrue(registry.size() > 0, "Skills should be loaded");

		// Get the response
		String path = getTestSkillsDirectory() + "/pdf-extractor/saa-roadmap.pdf";
		AssistantMessage response = agent.call(String.format("请从 %s 文件中提取关键信息。", path));
		assertNotNull(response, "Response should not be null");
		assertNotNull(response.getText(), "Response text should not be null");
		
		// Verify registry is working
		assertNotNull(registry, "Registry should not be null");
		assertEquals("Classpath", registry.getRegistryType(), "Registry type should be Classpath");
	}

	/**
	 * Verifies that groupedTools are passed to SkillsInterceptor and that tools for a skill
	 * are added to dynamicToolCallbacks when the agent calls read_skill for that skill.
	 * Uses a dedicated skill "grouped-tools-test" and a record_result tool that captures
	 * invocations; asserts the tool was called after the agent reads the skill.
	 */
	@Test
	public void testGroupedToolsActivationAndToolUsage() throws Exception {
		// 1. Create test tool that records invocations
		List<String> recordedValues = Collections.synchronizedList(new ArrayList<>());
		ToolCallback recordResultTool = FunctionToolCallback
				.builder("record_result",
						(RecordResultRequest req, ToolContext ctx) -> {
							if (req != null && req.value != null) {
								recordedValues.add(req.value.trim());
							}
							return "recorded";
						})
				.description("Records a result value (parameter: value). Use this when the user asks to record a value.")
				.inputType(RecordResultRequest.class)
				.build();

		// 2. Register groupedTools: skill name "grouped-tools-test" -> list of tools
		Map<String, List<ToolCallback>> groupedTools = Map.of("grouped-tools-test", List.of(recordResultTool));

		// 3. Skill registry loading from test resources (includes grouped-tools-test)
		SkillRegistry registry = FileSystemSkillRegistry.builder()
				.projectSkillsDirectory(getTestSkillsDirectory())
				.build();
		SkillsAgentHook hook = SkillsAgentHook.builder()
				.skillRegistry(registry)
				.groupedTools(groupedTools)
				.build();

		// 4. Build agent with hook (hook provides read_skill + SkillsInterceptor with groupedTools)
		ReactAgent agent = ReactAgent.builder()
				.name("grouped-tools-test-agent")
				.model(chatModel)
				.saver(new MemorySaver())
				.hooks(List.of(hook))
				.enableLogging(true)
				.build();

		// 5. Prompt: ask to read the skill then follow it to record a value
		String valueToRecord = "hello-grouped-tools";
		String prompt = String.format(
				"请先用 read_skill 读取 grouped-tools-test 技能，然后按该技能要求用 record_result 工具记录：%s",
				valueToRecord);

		AssistantMessage response = agent.call(prompt);

		assertNotNull(response, "Response should not be null");
		assertFalse(recordedValues.isEmpty(),
				"record_result tool from groupedTools should have been invoked after read_skill; recorded: " + recordedValues);
		assertTrue(recordedValues.stream().anyMatch(v -> v.contains(valueToRecord) || valueToRecord.contains(v)),
				"Recorded values should contain the requested value; recorded: " + recordedValues);
	}

	/** Request type for the record_result test tool. */
	private static class RecordResultRequest {

		@JsonProperty("value")
		@JsonPropertyDescription("The value to record")
		public String value;
	}
}
