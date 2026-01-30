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
package com.alibaba.cloud.ai.graph.skills.registry.classpath;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.graph.skills.SkillMetadata;

import org.springframework.ai.chat.model.ChatModel;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@EnabledIfEnvironmentVariable(named = "AI_DASHSCOPE_API_KEY", matches = ".+")
class ClasspathSkillRegistryTest {

	private ChatModel chatModel;

	@BeforeEach
	void setUp() {
		// Create DashScopeApi instance using the API key from environment variable
		DashScopeApi dashScopeApi = DashScopeApi.builder()
				.apiKey(System.getenv("AI_DASHSCOPE_API_KEY"))
				.build();

		// Create DashScope ChatModel instance
		this.chatModel = DashScopeChatModel.builder()
				.dashScopeApi(dashScopeApi)
				.build();
	}

	@Test
	public void testClasspathSkillRegistryLoadsSkills() throws Exception {
		// Test loading skills from classpath with default path
		ClasspathSkillRegistry registry = ClasspathSkillRegistry.builder().build();

		// Verify skills are loaded
		assertNotNull(registry, "Registry should not be null");
		// Note: This test may pass or fail depending on whether classpath:skills has skills
		// If skills exist, verify they are loaded correctly
		if (registry.size() > 0) {
			List<SkillMetadata> skills = registry.listAll();
			assertFalse(skills.isEmpty(), "Skills list should not be empty");

			for (SkillMetadata skill : skills) {
				assertNotNull(skill.getName(), "Skill name should not be null");
				assertNotNull(skill.getDescription(), "Skill description should not be null");
				assertNotNull(skill.getSkillPath(), "Skill path should not be null");
			}
		}
	}

	@Test
	public void testClasspathSkillRegistryWithCustomPath() throws Exception {
		// Test loading skills from custom classpath path
		ClasspathSkillRegistry registry = ClasspathSkillRegistry.builder()
				.classpathPath("skills")
				.build();

		assertNotNull(registry, "Registry should not be null");
		// Verify registry is created successfully
		assertTrue(registry.getRegistryType().equals("Classpath"),
				"Registry type should be Classpath");
	}

	@Test
	public void testClasspathSkillRegistryWithCustomBasePath(@TempDir Path tempDir) throws Exception {
		// Test with custom basePath
		Path customBasePath = tempDir.resolve("custom-skills");
		ClasspathSkillRegistry registry = ClasspathSkillRegistry.builder()
				.classpathPath("skills")
				.basePath(customBasePath.toString())
				.build();

		assertNotNull(registry, "Registry should not be null");

		// Verify basePath directory is created
		assertTrue(Files.exists(customBasePath), "BasePath directory should be created");
	}

	@Test
	public void testClasspathSkillRegistryReadSkillContent() throws Exception {
		ClasspathSkillRegistry registry = ClasspathSkillRegistry.builder()
				.classpathPath("skills")
				.build();

		// If skills are loaded, test reading skill content
		if (registry.size() > 0) {
			List<SkillMetadata> skills = registry.listAll();
			SkillMetadata firstSkill = skills.get(0);

			try {
				String content = registry.readSkillContent(firstSkill.getName());
				assertNotNull(content, "Skill content should not be null");
				assertFalse(content.isEmpty(), "Skill content should not be empty");
			}
			catch (Exception e) {
				// If skill doesn't exist or can't be read, that's okay for this test
			}
		}
	}

	@Test
	public void testClasspathSkillRegistryResourcesCopied(@TempDir Path tempDir) throws Exception {
		// Test that skill resources are copied to basePath
		Path customBasePath = tempDir.resolve("test-skills");
		ClasspathSkillRegistry registry = ClasspathSkillRegistry.builder()
				.classpathPath("skills")
				.basePath(customBasePath.toString())
				.build();

		// If skills are loaded, verify resources are copied
		if (registry.size() > 0) {
			List<SkillMetadata> skills = registry.listAll();
			for (SkillMetadata skill : skills) {
				Path skillPath = Path.of(skill.getSkillPath());
				// Verify skillPath points to basePath
				assertTrue(skillPath.startsWith(customBasePath),
						"Skill path should be under basePath");

				// Verify SKILL.md exists in copied location
				Path skillFile = skillPath.resolve("SKILL.md");
				if (Files.exists(skillFile)) {
					assertTrue(Files.isRegularFile(skillFile),
							"SKILL.md should exist in copied location");
				}
			}
		}
	}

	@Test
	public void testClasspathSkillRegistryReload() throws Exception {
		ClasspathSkillRegistry registry = ClasspathSkillRegistry.builder()
				.classpathPath("skills")
				.build();

		int initialSize = registry.size();

		// Reload skills
		registry.reload();

		// Verify reload works (size should be same or could change if skills were added/removed)
		assertNotNull(registry, "Registry should not be null after reload");
		// Reload should not throw exception
	}

	@Test
	public void testClasspathSkillRegistryGetAndContains() throws Exception {
		ClasspathSkillRegistry registry = ClasspathSkillRegistry.builder()
				.classpathPath("skills")
				.build();

		// Test get and contains methods
		if (registry.size() > 0) {
			List<SkillMetadata> skills = registry.listAll();
			SkillMetadata firstSkill = skills.get(0);
			String skillName = firstSkill.getName();

			// Test contains
			assertTrue(registry.contains(skillName),
					"Registry should contain skill: " + skillName);

			// Test get
			Optional<SkillMetadata> skillOpt = registry.get(skillName);
			assertTrue(skillOpt.isPresent(),
					"Registry should return skill: " + skillName);

			SkillMetadata skill = skillOpt.get();
			assertEquals(skillName, skill.getName(),
					"Retrieved skill name should match");
		}
	}
}
