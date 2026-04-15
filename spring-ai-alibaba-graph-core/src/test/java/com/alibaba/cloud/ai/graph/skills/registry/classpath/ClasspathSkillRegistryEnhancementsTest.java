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

import com.alibaba.cloud.ai.graph.skills.SkillMetadata;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClasspathSkillRegistryEnhancementsTest {

	@Test
	void classpathRegistrySupportsAllowedToolsSearchDisableAndReadByPath(@TempDir Path tempDir) throws Exception {
		ClasspathSkillRegistry registry = ClasspathSkillRegistry.builder()
				.classpathPath("skills")
				.basePath(tempDir.toString())
				.build();

		SkillMetadata skill = registry.get("sample-skill").orElseThrow();
		assertEquals(List.of("lookup_docs", "record_result"), skill.getAllowedTools());
		assertEquals("sample-skill", registry.getByPath(skill.getSkillPath()).orElseThrow().getName());
		assertEquals(List.of("sample-skill"), registry.search("sample").stream().map(SkillMetadata::getName).toList());
		assertEquals(List.of("sample-skill"),
				registry.search("classpath registry enhancement").stream().map(SkillMetadata::getName).toList());
		assertTrue(registry.readSkillContentByPath(skill.getSkillPath()).contains("# Sample Skill"));

		assertTrue(registry.disable("sample-skill"));
		assertTrue(registry.isDisabled("sample-skill"));
		assertFalse(registry.contains("sample-skill"));
		assertTrue(registry.search("sample").isEmpty());
		assertThrows(IllegalStateException.class, () -> registry.readSkillContentByPath(skill.getSkillPath()));
	}

	@Test
	void classpathRegistryParsesLicenseCompatibilityMetadata(@TempDir Path tempDir) {
		ClasspathSkillRegistry registry = ClasspathSkillRegistry.builder()
				.classpathPath("skills")
				.basePath(tempDir.toString())
				.build();

		SkillMetadata skill = registry.get("sample-skill").orElseThrow();
		assertEquals("MIT", skill.getLicense());
		assertEquals("Spring AI 1.0+", skill.getCompatibility());
		Map<String, String> metadata = skill.getMetaData();
		assertEquals(2, metadata.size());
		assertEquals("1.0", metadata.get("version"));
		assertEquals("test-author", metadata.get("author"));
	}

}
