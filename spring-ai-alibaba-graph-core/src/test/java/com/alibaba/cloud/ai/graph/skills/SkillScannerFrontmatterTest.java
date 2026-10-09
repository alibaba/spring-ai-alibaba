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
package com.alibaba.cloud.ai.graph.skills;

import com.alibaba.cloud.ai.graph.skills.registry.filesystem.SkillScanner;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class SkillScannerFrontmatterTest {

	private SkillMetadata writeSkill(Path tmp, String dirName, String rawMarkdown) throws IOException {
		Path skillDir = tmp.resolve(dirName);
		Files.createDirectories(skillDir);
		Files.writeString(skillDir.resolve("SKILL.md"), rawMarkdown);
		return new SkillScanner().loadSkill(skillDir);
	}

	// The closing frontmatter fence must be recognized only at a line start, so a
	// frontmatter value that itself contains "---" must not truncate parsing.
	@Test
	void valueContainingDashesDoesNotTruncateFrontmatter(@TempDir Path tmp) throws IOException {
		String markdown = "---\n"
				+ "name: dash-skill\n"
				+ "description: supports ranges like 10---20 and flags\n"
				+ "---\n"
				+ "# Body\n"
				+ "hello\n";

		SkillMetadata meta = writeSkill(tmp, "dash-skill", markdown);

		assertNotNull(meta);
		assertEquals("dash-skill", meta.getName());
		assertEquals("supports ranges like 10---20 and flags", meta.getDescription());
		assertEquals("# Body\nhello", meta.getFullContent());
	}

	// A well-formed frontmatter (fence on its own line, no internal ---) still parses
	// and strips correctly after the fix.
	@Test
	void standardFrontmatterStillParses(@TempDir Path tmp) throws IOException {
		String markdown = "---\n"
				+ "name: web-research\n"
				+ "description: research the web\n"
				+ "allowed_tools:\n"
				+ "  - search\n"
				+ "  - fetch\n"
				+ "---\n"
				+ "Use the tools carefully.\n";

		SkillMetadata meta = writeSkill(tmp, "web-research", markdown);

		assertNotNull(meta);
		assertEquals("web-research", meta.getName());
		assertEquals("research the web", meta.getDescription());
		assertEquals("Use the tools carefully.", meta.getFullContent());
		assertEquals(java.util.List.of("search", "fetch"), meta.getAllowedTools());
	}
}
