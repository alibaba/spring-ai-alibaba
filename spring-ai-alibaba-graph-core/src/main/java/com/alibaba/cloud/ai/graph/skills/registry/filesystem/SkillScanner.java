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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

/**
 * Scanner for discovering and loading Skills from the filesystem.
 *
 * Scans a directory for skill folders, each containing a SKILL.md file with
 * YAML frontmatter defining the skill's metadata.
 *
 * Validates skills according to Agent Skills spec (https://agentskills.io/specification):
 * - Skill name: max 64 chars, lowercase alphanumeric with single hyphens only
 * - Skill description: max 1024 chars (truncated if exceeded)
 */
public class SkillScanner {

	private static final Logger logger = LoggerFactory.getLogger(SkillScanner.class);

	// Agent Skills spec constraints (https://agentskills.io/specification)
	private static final int MAX_SKILL_NAME_LENGTH = 64;
	private static final int MAX_SKILL_DESCRIPTION_LENGTH = 1024;

	// Pattern: lowercase alphanumeric, single hyphens between segments, no start/end hyphen
	private static final Pattern SKILL_NAME_PATTERN = Pattern.compile("^[a-z0-9]+(-[a-z0-9]+)*$");

	private final Yaml yaml = new Yaml();

	public List<SkillMetadata> scan(String skillsDirectory) {
		return scan(skillsDirectory, "user");
	}

	public List<SkillMetadata> scan(String skillsDirectory, String source) {
		List<SkillMetadata> skills = new ArrayList<>();
		Path skillsPath = Path.of(skillsDirectory);

		if (!Files.exists(skillsPath)) {
			logger.warn("Skills directory does not exist: {}", skillsDirectory);
			return skills;
		}

		if (!Files.isDirectory(skillsPath)) {
			logger.warn("Skills path is not a directory: {}", skillsDirectory);
			return skills;
		}

		try (Stream<Path> paths = Files.list(skillsPath)) {
			paths.filter(Files::isDirectory)
					.forEach(skillDir -> {
						try {
							SkillMetadata metadata = loadSkill(skillDir, source);
							if (metadata != null) {
								skills.add(metadata);
								logger.info("Loaded skill: {} from {}", metadata.getName(), skillDir);
							}
						}
						catch (Exception e) {
							logger.error("Failed to load skill from {}: {}", skillDir, e.getMessage(), e);
						}
					});
		}
		catch (IOException e) {
			logger.error("Failed to scan skills directory {}: {}", skillsDirectory, e.getMessage(), e);
		}

		logger.info("Discovered {} skills from {}", skills.size(), skillsDirectory);
		return skills;
	}

	public SkillMetadata loadSkill(Path skillDir) {
		return loadSkill(skillDir, "user");
	}

	public SkillMetadata loadSkill(Path skillDir, String source) {
		Path skillFile = skillDir.resolve("SKILL.md");

		if (!Files.exists(skillFile)) {
			logger.warn("SKILL.md not found in {}", skillDir);
			return null;
		}

		try {
			String content = Files.readString(skillFile);
			Map<String, Object> frontmatter = parseFrontmatter(content);

			if (frontmatter == null || frontmatter.isEmpty()) {
				logger.warn("No frontmatter found in {}", skillFile);
				return null;
			}

			String name = (String) frontmatter.get("name");
			String description = (String) frontmatter.get("description");

			if (name == null || name.isEmpty()) {
				logger.warn("Skill name is missing in {}", skillFile);
				return null;
			}

			if (description == null || description.isEmpty()) {
				logger.warn("Skill description is missing in {}", skillFile);
				return null;
			}

			// Validate name format per Agent Skills spec (warn but still load for backwards compatibility)
			String directoryName = skillDir.getFileName().toString();
			validateSkillName(name, directoryName, skillFile);

			// Validate and truncate description length (spec: max 1024 chars)
			String descriptionStr = String.valueOf(description);
			if (descriptionStr.length() > MAX_SKILL_DESCRIPTION_LENGTH) {
				logger.warn(
						"Description exceeds {} chars in {}, truncating",
						MAX_SKILL_DESCRIPTION_LENGTH,
						skillFile
				);
				descriptionStr = descriptionStr.substring(0, MAX_SKILL_DESCRIPTION_LENGTH);
			}

			// Remove frontmatter from content to get fullContent
			String fullContent = removeFrontmatter(content);

			SkillMetadata.Builder builder = SkillMetadata.builder()
					.name(name)
					.description(descriptionStr)
					.skillPath(skillDir.toString())
					.source(source)
					.fullContent(fullContent);

			return builder.build();

		}
		catch (IOException e) {
			logger.error("Failed to read skill file {}: {}", skillFile, e.getMessage(), e);
			return null;
		}
		catch (Exception e) {
			logger.error("Failed to parse skill file {}: {}", skillFile, e.getMessage(), e);
			return null;
		}
	}

	/**
	 * Validate skill name per Agent Skills spec.
	 *
	 * Requirements:
	 * - Max 64 characters
	 * - Lowercase alphanumeric and hyphens only (a-z, 0-9, -)
	 * - Cannot start or end with hyphen
	 * - No consecutive hyphens
	 * - Must match parent directory name
	 *
	 * If validation fails, logs a warning but still allows loading for backwards compatibility.
	 *
	 * @param name the skill name from YAML frontmatter
	 * @param directoryName the parent directory name
	 * @param skillFile the path to the SKILL.md file (for logging)
	 */
	private void validateSkillName(String name, String directoryName, Path skillFile) {
		if (name.length() > MAX_SKILL_NAME_LENGTH) {
			logger.warn(
					"Skill '{}' in {} does not follow Agent Skills spec: name exceeds {} characters. " +
							"Consider renaming to be spec-compliant.",
					name,
					skillFile,
					MAX_SKILL_NAME_LENGTH
			);
			return;
		}

		if (!SKILL_NAME_PATTERN.matcher(name).matches()) {
			logger.warn(
					"Skill '{}' in {} does not follow Agent Skills spec: name must be lowercase " +
							"alphanumeric with single hyphens only (cannot start or end with hyphen). " +
							"Consider renaming to be spec-compliant.",
					name,
					skillFile
			);
			return;
		}

		if (!name.equals(directoryName)) {
			logger.warn(
					"Skill '{}' in {} does not follow Agent Skills spec: name '{}' must match " +
							"directory name '{}'. Consider renaming to be spec-compliant.",
					name,
					skillFile,
					name,
					directoryName
			);
		}
	}

	private Map<String, Object> parseFrontmatter(String content) {
		if (!content.startsWith("---")) {
			return null;
		}

		int endIndex = content.indexOf("---", 3);
		if (endIndex == -1) {
			return null;
		}

		String frontmatterStr = content.substring(3, endIndex).trim();

		try {
			@SuppressWarnings("unchecked")
			Map<String, Object> frontmatter = yaml.load(frontmatterStr);
			return frontmatter;
		}
		catch (Exception e) {
			logger.error("Failed to parse YAML frontmatter: {}", e.getMessage(), e);
			return null;
		}
	}

	private String removeFrontmatter(String content) {
		if (!content.startsWith("---")) {
			return content;
		}

		int endIndex = content.indexOf("---", 3);
		if (endIndex == -1) {
			return content;
		}

		return content.substring(endIndex + 3).trim();
	}
}
