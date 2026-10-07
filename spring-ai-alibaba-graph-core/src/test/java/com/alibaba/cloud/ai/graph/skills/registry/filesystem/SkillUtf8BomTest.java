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

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class SkillUtf8BomTest {

	private static final String BODY = "\uFEFF# Instructions\n\nKeep \uFEFF inside the body.";

	@TempDir
	Path tempDir;

	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	void loadsSkillWithOrWithoutBom(boolean withBom) throws Exception {
		Path skillDir = writeSkill(withBom);

		SkillMetadata skill = new SkillScanner().loadSkill(skillDir);

		assertNotNull(skill);
		assertEquals("bom-skill", skill.getName());
		assertEquals("A skill with Unicode instructions.", skill.getDescription());
		assertEquals(BODY, skill.getFullContent());
	}

	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	void loadsFullContentWithOrWithoutBom(boolean withBom) throws Exception {
		Path skillDir = writeSkill(withBom);
		SkillMetadata skill = SkillMetadata.builder()
			.name("bom-skill")
			.description("A skill with Unicode instructions.")
			.skillPath(skillDir.toString())
			.build();

		assertEquals(BODY, skill.loadFullContent());
	}

	private Path writeSkill(boolean withBom) throws Exception {
		Path skillDir = Files.createDirectory(tempDir.resolve("bom-skill"));
		String content = """
				---
				name: bom-skill
				description: A skill with Unicode instructions.
				---

				""" + BODY;
		Files.writeString(skillDir.resolve("SKILL.md"), (withBom ? "\uFEFF" : "") + content);
		return skillDir;
	}

}
