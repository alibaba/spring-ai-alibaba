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
import com.alibaba.cloud.ai.graph.skills.registry.AbstractSkillRegistry;

import org.springframework.ai.chat.prompt.SystemPromptTemplate;

import org.springframework.core.io.Resource;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * FileSystem-based implementation of SkillRegistry.
 *
 * This implementation loads skills from the filesystem:
 * - User-level: ~/saa/skills/ (global skills)
 * - Project-level: current working directory/skills/ (project-specific skills, higher priority)
 *
 * Skills are automatically loaded during initialization. The registry can be reloaded
 * manually using {@link #reload()}.
 *
 * <p><b>Example Usage:</b>
 * <pre>{@code
 * // Use all defaults (project directory: ./skills)
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
public class FileSystemSkillRegistry extends AbstractSkillRegistry {

	/**
	 * Default system prompt template for FileSystemSkillRegistry.
	 * This template defines how skills are presented in the system prompt.
	 * It supports the following variables:
	 * - {skills_registry}: The registry type name
	 * - {skills_list}: The formatted list of available skills
	 * - {skills_load_instructions}: Instructions on how skills are loaded
	 */
	public static final String DEFAULT_SYSTEM_PROMPT_TEMPLATE = """
			
			## Skills System
			
			You have access to a skills library that provides specialized capabilities and domain knowledge. All skills are stored in a Skill Registry with a file system based storage.
			
			### Available Skills
			
			{skills_list}
			
			### How to Use Skills (Progressive Disclosure)
			
			Skills follow a **progressive disclosure** pattern - you know they exist (name + description above), but you only read the full instructions when needed:
			
			1. **Recognize when a skill applies**: Check if the user's task matches any skill's description
			2. **Read the skill's full instructions**: The skill list above shows the exact skill id to use with `read_skill`
			3. **Follow the skill's instructions**: SKILL.md contains step-by-step workflows, best practices, and examples
			4. **Access supporting files**: Skills may include Python scripts, configs, or reference docs - use absolute paths
			
			#### How to Read The Full Skill Instruction
			
			You are currently using the file system based Skill Registry. Please follow the skill loading guidelines below:
			
			{skills_load_instructions}
			
			**Important:**
			
			  - **For SKILL.md files (skill instructions)**: Always use `read_skill` to read skill instructions. Do not attempt to access SKILL.md files through other methods.
			  - **For other supporting files that skill uses (scripts, references, etc.)**: You may use other appropriate tools to read or access these files as needed, always use absolute paths from the skill list.
			
			#### When to Use Skills
			
			  - When the user's request matches a skill's domain (e.g., "research X" → web-research skill)
			  - When you need specialized knowledge or structured workflows
			  - When a skill provides proven patterns for complex tasks
			
			#### Skills are Self-Documenting
			
			  - Each SKILL.md tells you exactly what the skill does and how to use it
			  - The skill list above shows the full path for each skill's SKILL.md file
			
			#### Executing Skill Scripts
			
			Skills may contain Python scripts or other executable files. Always use absolute paths from the skill list.
			
			### Example Workflow
			
			User: "Can you research the latest developments in quantum computing?"
			
			1. Check available skills above → See "web-research" skill with its skill id
			2. Read the skill using the id shown in the list
			3. Follow the skill's research workflow (search → organize → synthesize)
			4. Use any helper scripts with absolute paths
			
			Remember: Skills are tools to make you more capable and consistent. When in doubt, check if a skill exists for the task!
			""";
	private static final Logger logger = LoggerFactory.getLogger(FileSystemSkillRegistry.class);
	private final String userSkillsDirectory;
	private final String projectSkillsDirectory;
	private final SkillScanner scanner = new SkillScanner();
	private final SystemPromptTemplate systemPromptTemplate;

	private FileSystemSkillRegistry(Builder builder) {
		// Set default userSkillsDirectory to ~/saa/skills if not provided
		if (builder.userSkillsDirectory == null || builder.userSkillsDirectory.isEmpty()) {
			this.userSkillsDirectory = System.getProperty("user.home") + "/saa/skills";
		}
		else {
			this.userSkillsDirectory = builder.userSkillsDirectory;
		}

		// Set default projectSkillsDirectory to current working directory if not provided
		if (builder.projectSkillsDirectory == null || builder.projectSkillsDirectory.isEmpty()) {
			this.projectSkillsDirectory = Path.of("").toAbsolutePath().resolve("skills").toString();
		}
		else {
			this.projectSkillsDirectory = builder.projectSkillsDirectory;
		}

		// Set system prompt template - use provided or default
		if (builder.systemPromptTemplate != null) {
			this.systemPromptTemplate = builder.systemPromptTemplate;
		}
		else {
			this.systemPromptTemplate = SystemPromptTemplate.builder()
					.template(DEFAULT_SYSTEM_PROMPT_TEMPLATE)
					.build();
		}

		// Load skills during initialization if auto-load is enabled (default: true)
		if (builder.autoLoad) {
			loadSkillsToRegistry();
		}
	}

	public static Builder builder() {
		return new Builder();
	}

	/**
	 * Loads skills from configured directories into the registry.
	 * Uses Map to merge skills, ensuring project skills override user skills with the same name.
	 */
	@Override
	protected void loadSkillsToRegistry() {
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

		// Register all merged skills to registry
		int totalCount = mergedSkills.size();
		logger.info("Skills reloaded: {} total skills", totalCount);
		this.skills = mergedSkills;
	}

	/**
	 * Get the project skills directory path.
	 * This is an implementation-specific method, not part of the SkillRegistry interface.
	 *
	 * @return the project skills directory path
	 */
	public String getProjectSkillsDirectory() {
		return projectSkillsDirectory;
	}

	/**
	 * Get the user skills directory path.
	 * This is an implementation-specific method, not part of the SkillRegistry interface.
	 *
	 * @return the user skills directory path
	 */
	public String getUserSkillsDirectory() {
		return userSkillsDirectory;
	}

	@Override
	public String readSkillContent(String name) throws IOException {
		if (name == null || name.isEmpty()) {
			throw new IllegalArgumentException("Skill name cannot be null or empty");
		}

		// Get the skill by name
		Optional<SkillMetadata> skillOpt = get(name);
		if (skillOpt.isEmpty()) {
			throw new IllegalStateException("Skill not found: " + name);
		}

		SkillMetadata skill = skillOpt.get();

		// Use the normal loadFullContent method for filesystem skills
		return skill.loadFullContent();
	}

	@Override
	public String getSkillLoadInstructions() {
		List<SkillMetadata> skills = listAll();
		List<SkillMetadata> userSkills = new ArrayList<>();
		List<SkillMetadata> projectSkills = new ArrayList<>();
		for (SkillMetadata skill : skills) {
			if ("project".equals(skill.getSource())) {
				projectSkills.add(skill);
			}
			else {
				userSkills.add(skill);
			}
		}

		StringBuilder instructions = new StringBuilder();
		if (!userSkills.isEmpty() || !projectSkills.isEmpty()) {
			instructions.append("**Skill Locations:**\n");
			if (!userSkills.isEmpty()) {
				instructions.append(String.format("- **User Skills**: `%s`\n", getUserSkillsDirectory()));
			}
			if (!projectSkills.isEmpty()) {
				instructions.append(String.format("- **Project Skills**: `%s` (override user skills with same name)\n", getProjectSkillsDirectory()));
			}
			instructions.append("\n");
		}

		instructions.append("**Skill Path Format:**\n");
		instructions.append("Each skill has a unique path shown in the skill list above. ");
		instructions.append("Use the exact path shown when calling `read_skill` to read the SKILL.md file.\n");

		return instructions.toString();
	}

	@Override
	public String getRegistryType() {
		return "FileSystem";
	}

	@Override
	public SystemPromptTemplate getSystemPromptTemplate() {
		return systemPromptTemplate;
	}

	/**
	 * Builder for creating FileSystemSkillRegistry instances.
	 *
	 * <p>All configuration parameters are optional and have sensible defaults:
	 * <ul>
	 *   <li><b>userSkillsDirectory</b>: defaults to <code>~/saa/skills</code> - global skills available across all projects</li>
	 *   <li><b>projectSkillsDirectory</b>: defaults to <code>./skills</code> (current working directory) - project-specific skills</li>
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
		private SystemPromptTemplate systemPromptTemplate;

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
			}
			catch (IOException e) {
				throw new IllegalArgumentException("Cannot convert resource to file system path: " + resource, e);
			}
			return this;
		}

		/**
		 * Sets the project skills directory path.
		 * <p><b>Optional</b>: If not set, defaults to <code>./skills</code>
		 * (current working directory with "skills" subdirectory).
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
		 * <p><b>Optional</b>: If not set, defaults to <code>./skills</code> (current working directory)
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
			}
			catch (IOException e) {
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
		 * Sets a custom system prompt template for skills.
		 * <p><b>Optional</b>: If not set, uses the default template for FileSystemSkillRegistry.
		 * <p>The template should support the following variables:
		 * <ul>
		 *   <li><b>{skills_registry}</b>: The registry type name</li>
		 *   <li><b>{skills_list}</b>: The formatted list of available skills</li>
		 *   <li><b>{skills_load_instructions}</b>: Instructions on how skills are loaded</li>
		 * </ul>
		 *
		 * @param systemPromptTemplate the custom SystemPromptTemplate to use
		 * @return this builder
		 */
		public Builder systemPromptTemplate(SystemPromptTemplate systemPromptTemplate) {
			this.systemPromptTemplate = systemPromptTemplate;
			return this;
		}

		/**
		 * Sets a custom system prompt template from a template string.
		 * <p><b>Optional</b>: If not set, uses the default template for FileSystemSkillRegistry.
		 * <p>The template should support the following variables:
		 * <ul>
		 *   <li><b>{skills_registry}</b>: The registry type name</li>
		 *   <li><b>{skills_list}</b>: The formatted list of available skills</li>
		 *   <li><b>{skills_load_instructions}</b>: Instructions on how skills are loaded</li>
		 * </ul>
		 *
		 * @param template the template string
		 * @return this builder
		 */
		public Builder systemPromptTemplate(String template) {
			this.systemPromptTemplate = SystemPromptTemplate.builder()
					.template(template)
					.build();
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
