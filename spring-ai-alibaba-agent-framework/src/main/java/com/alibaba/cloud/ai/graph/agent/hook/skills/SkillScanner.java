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
package com.alibaba.cloud.ai.graph.agent.hook.skills;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Scanner for discovering and loading Skills from the filesystem.
 * 
 * Scans a directory for skill folders, each containing a SKILL.md file with
 * YAML frontmatter defining the skill's metadata.
 */
public class SkillScanner {

	private static final Logger logger = LoggerFactory.getLogger(SkillScanner.class);

	private final Yaml yaml = new Yaml();

	/**
	 * Scan a directory for skills.
	 * 
	 * @param skillsDirectory the directory containing skill folders
	 * @return list of discovered skill metadata
	 */
	public List<SkillMetadata> scan(String skillsDirectory) {
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
						SkillMetadata metadata = loadSkill(skillDir);
						if (metadata != null) {
							skills.add(metadata);
							logger.info("Loaded skill: {} from {}", metadata.getName(), skillDir);
						}
					} catch (Exception e) {
						logger.error("Failed to load skill from {}: {}", skillDir, e.getMessage(), e);
					}
				});
		} catch (IOException e) {
			logger.error("Failed to scan skills directory {}: {}", skillsDirectory, e.getMessage(), e);
		}

		logger.info("Discovered {} skills from {}", skills.size(), skillsDirectory);
		return skills;
	}

	/**
	 * Load a single skill from a directory.
	 * 
	 * @param skillDir the skill directory containing SKILL.md
	 * @return the skill metadata, or null if the skill is invalid
	 */
	public SkillMetadata loadSkill(Path skillDir) {
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

			SkillMetadata.Builder builder = SkillMetadata.builder()
				.name(name)
				.description(description)
				.skillPath(skillDir.toString());

			// Optional fields
			if (frontmatter.containsKey("allowed-tools")) {
				Object allowedTools = frontmatter.get("allowed-tools");
				if (allowedTools instanceof List) {
					@SuppressWarnings("unchecked")
					List<String> tools = (List<String>) allowedTools;
					builder.allowedTools(tools);
				} else if (allowedTools instanceof String) {
					// Handle comma-separated string
					String toolsStr = (String) allowedTools;
					List<String> tools = List.of(toolsStr.split("\\s*,\\s*"));
					builder.allowedTools(tools);
				}
			}

			if (frontmatter.containsKey("model")) {
				builder.model((String) frontmatter.get("model"));
			}

			return builder.build();

		} catch (IOException e) {
			logger.error("Failed to read skill file {}: {}", skillFile, e.getMessage(), e);
			return null;
		} catch (Exception e) {
			logger.error("Failed to parse skill file {}: {}", skillFile, e.getMessage(), e);
			return null;
		}
	}

	/**
	 * Parse YAML frontmatter from skill content.
	 * Frontmatter is delimited by --- at the start and end.
	 * 
	 * @param content the full content of SKILL.md
	 * @return the parsed frontmatter as a map, or null if not found
	 */
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
		} catch (Exception e) {
			logger.error("Failed to parse YAML frontmatter: {}", e.getMessage(), e);
			return null;
		}
	}
}
