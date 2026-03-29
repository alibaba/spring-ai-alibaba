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
package com.alibaba.cloud.ai.graph.skills.registry.filesystem;

import com.alibaba.cloud.ai.graph.skills.SkillMetadata;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileSystemSkillRegistryEnhancementsTest {

	@TempDir
	Path tempDir;

	private Path skillsDir;

	private Path allowedToolsSkillDir;

	@BeforeEach
	void setUp() throws Exception {
		skillsDir = tempDir.resolve("skills");
		allowedToolsSkillDir = writeSkill("allowed-tools-skill",
				"Skill fixture that documents additional allowed tools.",
				List.of("record_result", "lookup_docs"));
		writeSkill("copy-helper", "Copy editing helper skill for search coverage.", List.of());
	}

	@Test
	void parsesAllowedToolsAndSupportsPathBasedRead() throws Exception {
		FileSystemSkillRegistry registry = FileSystemSkillRegistry.builder()
				.projectSkillsDirectory(skillsDir.toString())
				.build();

		SkillMetadata skill = registry.get("allowed-tools-skill").orElseThrow();
		assertEquals(List.of("record_result", "lookup_docs"), skill.getAllowedTools());
		assertEquals("allowed-tools-skill", registry.getByPath(allowedToolsSkillDir.toString()).orElseThrow().getName());
		assertTrue(registry.readSkillContentByPath(allowedToolsSkillDir.toString()).contains("# allowed-tools-skill"));
	}

	@Test
	void searchMatchesNameDescriptionAndPath() throws Exception {
		FileSystemSkillRegistry registry = FileSystemSkillRegistry.builder()
				.projectSkillsDirectory(skillsDir.toString())
				.build();

		assertEquals(List.of("allowed-tools-skill"),
				registry.search("allowed-tools").stream().map(SkillMetadata::getName).toList());
		assertEquals(List.of("copy-helper"), registry.search("editing").stream().map(SkillMetadata::getName).toList());
		assertEquals(List.of("allowed-tools-skill"),
				registry.search(allowedToolsSkillDir.getFileName().toString()).stream().map(SkillMetadata::getName).toList());
	}

	@Test
	void disableHidesSkillFromReadsAndSearch() throws Exception {
		FileSystemSkillRegistry registry = FileSystemSkillRegistry.builder()
				.projectSkillsDirectory(skillsDir.toString())
				.build();

		assertTrue(registry.disableByPath(allowedToolsSkillDir.toString()));
		assertTrue(registry.isDisabled("allowed-tools-skill"));
		assertFalse(registry.contains("allowed-tools-skill"));
		assertTrue(registry.get("allowed-tools-skill").isEmpty());
		assertTrue(registry.getByPath(allowedToolsSkillDir.toString()).isEmpty());
		assertTrue(registry.search("allowed-tools").isEmpty());
		assertThrows(IllegalStateException.class, () -> registry.readSkillContent("allowed-tools-skill"));
		assertThrows(IllegalStateException.class, () -> registry.readSkillContentByPath(allowedToolsSkillDir.toString()));
		assertFalse(registry.isDisabled("copy-helper"));
	}

	private Path writeSkill(String name, String description, List<String> allowedTools) throws Exception {
		Path skillDir = skillsDir.resolve(name);
		Files.createDirectories(skillDir);
		String allowedToolsBlock = allowedTools.isEmpty()
				? ""
				: "\nallowed_tools:\n" + allowedTools.stream().map(tool -> "  - " + tool).reduce("", (a, b) -> a + b + "\n");
		Files.writeString(skillDir.resolve("SKILL.md"), """
				---
				name: %s
				description: %s%s---
				
				# %s
				
				Filesystem test skill content.
				""".formatted(name, description, allowedToolsBlock, name));
		return skillDir;
	}

}
