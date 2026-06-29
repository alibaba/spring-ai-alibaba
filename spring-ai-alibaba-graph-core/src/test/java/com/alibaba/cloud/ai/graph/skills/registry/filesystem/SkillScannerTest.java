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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class SkillScannerTest {

	@TempDir
	Path tempDir;

	private Path skillsDir;

	private SkillScanner scanner;

	@BeforeEach
	void setUp() {
		skillsDir = tempDir.resolve("skills");
		scanner = new SkillScanner();
	}

	@Test
	void parsesAllOptionalFields() throws Exception {
		writeSkill("full-skill", """
				---
				name: full-skill
				description: A skill with all optional fields.
				license: MIT
				compatibility: Spring AI 1.0+
				metadata:
				  version: "1.0"
				  author: test-author
				allowed-tools:
				  - tool-a
				  - tool-b
				---

				# Full Skill
				""");

		SkillMetadata skill = scanner.loadSkill(skillsDir.resolve("full-skill"));
		assertNotNull(skill);
		assertEquals("MIT", skill.getLicense());
		assertEquals("Spring AI 1.0+", skill.getCompatibility());
		assertEquals(Map.of("version", "1.0", "author", "test-author"), skill.getMetaData());
		assertEquals(List.of("tool-a", "tool-b"), skill.getAllowedTools());
	}

	@Test
	void emptyLicenseAndCompatibilityTreatedAsNull() throws Exception {
		writeSkill("empty-opts-skill", """
				---
				name: empty-opts-skill
				description: A skill with empty optional fields.
				license: ""
				compatibility: ""
				---

				# Empty Opts
				""");

		SkillMetadata skill = scanner.loadSkill(skillsDir.resolve("empty-opts-skill"));
		assertNotNull(skill);
		assertNull(skill.getLicense());
		assertNull(skill.getCompatibility());
	}

	@Test
	void compatibilityTruncatedAt500Chars() throws Exception {
		writeSkill("long-compat-skill", """
				---
				name: long-compat-skill
				description: A skill with long compatibility.
				compatibility: %s
				---

				# Long Compat
				""".formatted("x".repeat(600)));

		SkillMetadata skill = scanner.loadSkill(skillsDir.resolve("long-compat-skill"));
		assertEquals(500, skill.getCompatibility().length());
	}

	@Test
	void nonMapMetadataIgnored() throws Exception {
		writeSkill("bad-meta-skill", """
				---
				name: bad-meta-skill
				description: A skill with non-map metadata.
				metadata: not-a-map
				---

				# Bad Meta
				""");

		assertEquals(Collections.emptyMap(), scanner.loadSkill(skillsDir.resolve("bad-meta-skill")).getMetaData());
	}

	@Test
	void metadataCoercesValuesToStrings() throws Exception {
		writeSkill("coerce-skill", """
				---
				name: coerce-skill
				description: Mixed type metadata.
				metadata:
				  count: 42
				  active: true
				---

				# Coerce
				""");

		Map<String, String> meta = scanner.loadSkill(skillsDir.resolve("coerce-skill")).getMetaData();
		assertEquals("42", meta.get("count"));
		assertEquals("true", meta.get("active"));
	}

	@Test
	void validateMetadataReturnsEmptyForNonMap() {
		assertEquals(Collections.emptyMap(), scanner.validateMetadata("string", Path.of("test")));
		assertEquals(Collections.emptyMap(), scanner.validateMetadata(42, Path.of("test")));
		assertEquals(Collections.emptyMap(), scanner.validateMetadata(List.of("a"), Path.of("test")));
	}

	@Test
	void validateMetadataCoercesEntries() {
		Map<Object, Object> input = new LinkedHashMap<>();
		input.put("key1", "value1");
		input.put(42, "numeric-key");
		input.put("nullVal", null);

		Map<String, String> result = scanner.validateMetadata(input, Path.of("test"));
		assertEquals("value1", result.get("key1"));
		assertEquals("numeric-key", result.get("42"));
		assertEquals("", result.get("nullVal"));
	}

	private void writeSkill(String name, String content) throws Exception {
		Path skillDir = skillsDir.resolve(name);
		Files.createDirectories(skillDir);
		Files.writeString(skillDir.resolve("SKILL.md"), content);
	}
}
