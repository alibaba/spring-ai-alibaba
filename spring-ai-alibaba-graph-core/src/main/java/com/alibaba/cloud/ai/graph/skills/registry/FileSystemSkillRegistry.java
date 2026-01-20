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
package com.alibaba.cloud.ai.graph.skills.registry;

import com.alibaba.cloud.ai.graph.skills.SkillMetadata;
import com.alibaba.cloud.ai.graph.skills.SkillScanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * FileSystem-based implementation of SkillRegistry.
 * 
 * This implementation loads skills from the filesystem:
 * - User-level: ~/saa/skills/ (global skills)
 * - Project-level: classpath:resources/skills/ (project-specific skills, higher priority)
 * 
 * Skills are automatically loaded during initialization. The registry can be reloaded
 * manually using {@link #reload()}.
 * 
 * <p><b>Example Usage:</b>
 * <pre>{@code
 * // Use all defaults
 * FileSystemSkillRegistry registry = FileSystemSkillRegistry.builder().build();
 *
 * // Override user skills directory only
 * FileSystemSkillRegistry registry = FileSystemSkillRegistry.builder()
 *     .userSkillsDirectory("/custom/path/to/skills")
 *     .build();
 *
 * // Use Spring Resource for directories
 * FileSystemSkillRegistry registry = FileSystemSkillRegistry.builder()
 *     .userSkillsDirectory(new FileSystemResource("/custom/user/skills"))
 *     .projectSkillsDirectory(new ClassPathResource("skills"))
 *     .build();
 * }</pre>
 */
public class FileSystemSkillRegistry implements SkillRegistry {

	private static final Logger logger = LoggerFactory.getLogger(FileSystemSkillRegistry.class);

	private volatile Map<String, SkillMetadata> skills = new HashMap<>();
	private final String userSkillsDirectory;
	private final String projectSkillsDirectory;
	private final SkillScanner scanner = new SkillScanner();

	private FileSystemSkillRegistry(Builder builder) {
		// Set default userSkillsDirectory to ~/saa/skills if not provided
		if (builder.userSkillsDirectory == null || builder.userSkillsDirectory.isEmpty()) {
			this.userSkillsDirectory = System.getProperty("user.home") + "/saa/skills";
		} else {
			this.userSkillsDirectory = builder.userSkillsDirectory;
		}

		// Set default projectSkillsDirectory using ClassPathResource if not provided
		if (builder.projectSkillsDirectory == null || builder.projectSkillsDirectory.isEmpty()) {
			this.projectSkillsDirectory = resolveDefaultProjectSkillsDirectory();
		} else {
			this.projectSkillsDirectory = builder.projectSkillsDirectory;
		}

		// Load skills during initialization if auto-load is enabled (default: true)
		if (builder.autoLoad) {
			loadSkillsToRegistry();
		}
	}

	/**
	 * Resolves the default project skills directory.
	 * First tries to load from classpath (skills folder in resources),
	 * then falls back to current working directory if not found or not accessible.
	 *
	 * @return the resolved project skills directory path
	 */
	private String resolveDefaultProjectSkillsDirectory() {
		String path = null;
		try {
			// Try to load from classpath (e.g., src/main/resources/skills in dev, or packaged in jar)
			URL resource = getClass().getClassLoader().getResource("skills");
			if (resource != null && "file".equals(resource.getProtocol())) {
				return Path.of(resource.toURI()).toString();
			}
		} catch (Exception e) {
			// Resource might be inside a jar and cannot be converted to File
			logger.debug("Cannot access skills directory from classpath as file system path: {}", e.getMessage());
		}

		path = Path.of("").toAbsolutePath().resolve("skills").toString();

		return path;
	}

	/**
	 * Loads skills from configured directories into the registry.
	 * Uses Map to merge skills, ensuring project skills override user skills with the same name.
	 */
	private void loadSkillsToRegistry() {
		// Use Map to merge skills, ensuring project skills override user skills with the same name
		Map<String, SkillMetadata> mergedSkills = new HashMap<>();

		if (userSkillsDirectory != null && !userSkillsDirectory.isEmpty()) {
			Path userPath = Path.of(userSkillsDirectory);
			if (Files.exists(userPath)) {
				List<SkillMetadata> userSkills = scanner.scan(userSkillsDirectory, "user");
				for (SkillMetadata skill : userSkills) {
					mergedSkills.put(skill.getName(), skill);
				}
				logger.info("Loaded {} user-level skills from {}", userSkills.size(), userSkillsDirectory);
			}
		}

		if (projectSkillsDirectory != null && !projectSkillsDirectory.isEmpty()) {
			Path projectPath = Path.of(projectSkillsDirectory);
			if (Files.exists(projectPath)) {
				List<SkillMetadata> projectSkills = scanner.scan(projectSkillsDirectory, "project");
				for (SkillMetadata skill : projectSkills) {
					// Project skills override user skills with the same name
					mergedSkills.put(skill.getName(), skill);
				}
				logger.info("Loaded {} project-level skills from {}", projectSkills.size(), projectSkillsDirectory);
			}
		}

		// Register all merged skills to registry (will update existing or add new ones)
		int totalCount = mergedSkills.size();
		logger.info("Skills reloaded: {} total skills", totalCount);
		this.skills = mergedSkills;
	}

	@Override
	public Optional<SkillMetadata> get(String name) {
		return Optional.ofNullable(skills.get(name));
	}

	@Override
	public List<SkillMetadata> listAll() {
		return new ArrayList<>(skills.values());
	}

	@Override
	public boolean contains(String name) {
		return skills.containsKey(name);
	}

	@Override
	public int size() {
		return skills.size();
	}

	@Override
	public String getProjectSkillsDirectory() {
		return projectSkillsDirectory;
	}

	@Override
	public String getUserSkillsDirectory() {
		return userSkillsDirectory;
	}

	@Override
	public String readSkillContent(String name, String skillPath) throws IOException {
		if (name == null || name.isEmpty()) {
			throw new IllegalArgumentException("Skill name cannot be null or empty");
		}
		if (skillPath == null || skillPath.isEmpty()) {
			throw new IllegalArgumentException("Skill path cannot be null or empty");
		}

		// First verify the skill exists and matches the name
		Optional<SkillMetadata> skillOpt = get(name);
		if (skillOpt.isEmpty()) {
			throw new IllegalStateException("Skill not found: " + name);
		}

		SkillMetadata skill = skillOpt.get();
		// Verify the path matches
		if (!skillPath.equals(skill.getSkillPath())) {
			throw new IllegalStateException(
				String.format("Skill path mismatch: expected '%s', got '%s'", skill.getSkillPath(), skillPath));
		}

		// Load and return the full content
		return skill.loadFullContent();
	}

	/**
	 * Reloads all skills from configured directories.
	 * Clears existing skills and rescans the directories.
	 */
	@Override
	public synchronized void reload() {
		logger.info("Reloading skills...");
		loadSkillsToRegistry();
	}

	public static Builder builder() {
		return new Builder();
	}

	/**
	 * Builder for creating FileSystemSkillRegistry instances.
	 *
	 * <p>All configuration parameters are optional and have sensible defaults:
	 * <ul>
	 *   <li><b>userSkillsDirectory</b>: defaults to <code>~/saa/skills</code> - global skills available across all projects</li>
	 *   <li><b>projectSkillsDirectory</b>: defaults to <code>classpath:skills</code> (e.g., src/main/resources/skills) - project-specific skills</li>
	 *   <li><b>autoLoad</b>: defaults to <code>true</code> - automatically load skills during initialization</li>
	 * </ul>
	 *
	 * <p><b>Example Usage:</b>
	 * <pre>{@code
	 * // Use all defaults
	 * FileSystemSkillRegistry registry = FileSystemSkillRegistry.builder().build();
	 *
	 * // Override user skills directory only (String path)
	 * FileSystemSkillRegistry registry = FileSystemSkillRegistry.builder()
	 *     .userSkillsDirectory("/custom/path/to/skills")
	 *     .build();
	 *
	 * // Use Spring Resource for directories
	 * FileSystemSkillRegistry registry = FileSystemSkillRegistry.builder()
	 *     .userSkillsDirectory(new FileSystemResource("/custom/user/skills"))
	 *     .projectSkillsDirectory(new ClassPathResource("skills"))
	 *     .build();
	 *
	 * // Disable auto-loading
	 * FileSystemSkillRegistry registry = FileSystemSkillRegistry.builder()
	 *     .autoLoad(false)
	 *     .build();
	 * }</pre>
	 */
	public static class Builder {
		private String userSkillsDirectory;
		private String projectSkillsDirectory;
		private boolean autoLoad = true;

		/**
		 * Sets the user skills directory path.
		 * <p><b>Optional</b>: If not set, defaults to <code>~/saa/skills</code>
		 *
		 * @param directory the directory path for user-level skills
		 * @return this builder
		 */
		public Builder userSkillsDirectory(String directory) {
			this.userSkillsDirectory = directory;
			return this;
		}

		/**
		 * Sets the user skills directory from a Spring Resource.
		 * <p><b>Optional</b>: If not set, defaults to <code>~/saa/skills</code>
		 * <p>The Resource will be converted to a file system path. If the resource cannot be
		 * resolved to a file (e.g., it's inside a JAR), an IllegalArgumentException will be thrown.
		 *
		 * @param resource the Resource pointing to the user-level skills directory
		 * @return this builder
		 * @throws IllegalArgumentException if the resource cannot be converted to a file system path
		 */
		public Builder userSkillsDirectory(Resource resource) {
			try {
				if (resource != null && resource.exists()) {
					File file = resource.getFile();
					this.userSkillsDirectory = file.getAbsolutePath();
				}
			} catch (IOException e) {
				throw new IllegalArgumentException("Cannot convert resource to file system path: " + resource, e);
			}
			return this;
		}

		/**
		 * Sets the project skills directory path.
		 * <p><b>Optional</b>: If not set, defaults to <code>classpath:skills</code>
		 * (e.g., src/main/resources/skills in development, or packaged in jar for production).
		 * Falls back to <code>./skills</code> if classpath resource is not accessible.
		 *
		 * @param directory the directory path for project-level skills
		 * @return this builder
		 */
		public Builder projectSkillsDirectory(String directory) {
			this.projectSkillsDirectory = directory;
			return this;
		}

		/**
		 * Sets the project skills directory from a Spring Resource.
		 * <p><b>Optional</b>: If not set, defaults to <code>classpath:skills</code>
		 * <p>The Resource will be converted to a file system path. If the resource cannot be
		 * resolved to a file (e.g., it's inside a JAR), an IllegalArgumentException will be thrown.
		 *
		 * @param resource the Resource pointing to the project-level skills directory
		 * @return this builder
		 * @throws IllegalArgumentException if the resource cannot be converted to a file system path
		 */
		public Builder projectSkillsDirectory(Resource resource) {
			try {
				if (resource != null && resource.exists()) {
					File file = resource.getFile();
					this.projectSkillsDirectory = file.getAbsolutePath();
				}
			} catch (IOException e) {
				throw new IllegalArgumentException("Cannot convert resource to file system path: " + resource, e);
			}
			return this;
		}

		/**
		 * Sets whether to automatically load skills during initialization.
		 * <p><b>Optional</b>: Defaults to <code>true</code>
		 *
		 * @param autoLoad true to auto-load skills, false to skip auto-loading
		 * @return this builder
		 */
		public Builder autoLoad(boolean autoLoad) {
			this.autoLoad = autoLoad;
			return this;
		}

		/**
		 * Builds the FileSystemSkillRegistry instance with the configured parameters.
		 * <p>All parameters are optional and will use default values if not set.
		 *
		 * @return a new FileSystemSkillRegistry instance
		 */
		public FileSystemSkillRegistry build() {
			return new FileSystemSkillRegistry(this);
		}
	}
}
