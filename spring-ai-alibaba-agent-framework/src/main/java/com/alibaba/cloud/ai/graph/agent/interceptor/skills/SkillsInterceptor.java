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
package com.alibaba.cloud.ai.graph.agent.interceptor.skills;

import com.alibaba.cloud.ai.graph.agent.interceptor.ModelCallHandler;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelInterceptor;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelRequest;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.SystemMessage;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Interceptor for integrating Claude-style Skills into ReactAgent.
 * 
 * This interceptor injects skills metadata into system prompt, following progressive disclosure pattern:
 * - Injects lightweight skills list (name + description + path)
 * - LLM reads full SKILL.md content when needed using read_file tool
 * 
 * Supports two-level directory structure:
 * - User-level: ~/.spring-ai/skills/ (global skills)
 * - Project-level: ./.spring-ai/skills/ (project-specific skills, higher priority)
 */
public class SkillsInterceptor extends ModelInterceptor {

	private static final Logger logger = LoggerFactory.getLogger(SkillsInterceptor.class);

	private final SkillRegistry skillRegistry;
	private final String userSkillsDirectory;
	private final String projectSkillsDirectory;
	private volatile boolean skillsLoaded = false;

	private SkillsInterceptor(Builder builder) {
		this.skillRegistry = new SkillRegistry();
		this.userSkillsDirectory = builder.userSkillsDirectory;
		this.projectSkillsDirectory = builder.projectSkillsDirectory;
		
		if (builder.autoScan) {
			loadSkills();
		}
	}

	public static Builder builder() {
		return new Builder();
	}

	@Override
	public ModelResponse interceptModel(ModelRequest request, ModelCallHandler handler) {
		if (!skillsLoaded) {
			synchronized (this) {
				if (!skillsLoaded) {
					loadSkills();
				}
			}
		}

		List<SkillMetadata> skills = skillRegistry.listAll();
		if (skills.isEmpty()) {
			return handler.call(request);
		}

		String skillsPrompt = buildSkillsPrompt(skills);
		SystemMessage enhanced = enhanceSystemMessage(request.getSystemMessage(), skillsPrompt);

		if (logger.isDebugEnabled()) {
			logger.debug("Enhanced system message:\n{}", enhanced.getText());
		}

		ModelRequest modified = ModelRequest.builder(request)
			.systemMessage(enhanced)
			.build();

		return handler.call(modified);
	}

	private void loadSkills() {
		SkillScanner scanner = new SkillScanner();
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
					mergedSkills.put(skill.getName(), skill);
				}
				logger.info("Loaded {} project-level skills from {}", projectSkills.size(), projectSkillsDirectory);
			}
		}

		skillRegistry.registerAll(new ArrayList<>(mergedSkills.values()));
		skillsLoaded = true;
		logger.info("Total {} skills loaded", mergedSkills.size());
	}

	private String buildSkillsPrompt(List<SkillMetadata> skills) {
		StringBuilder sb = new StringBuilder();
		sb.append("\n## Skills System\n\n");
		sb.append("You have access to a skills library that provides specialized knowledge and workflows.\n\n");
		
		sb.append("**CRITICAL: Skills are NOT tools!**\n");
		sb.append("Skills are instruction documents that guide you on how to use your available tools. ");
		sb.append("You cannot directly call a skill - you must first read its SKILL.md file to understand the workflow.\n\n");

		List<SkillMetadata> userSkills = new ArrayList<>();
		List<SkillMetadata> projectSkills = new ArrayList<>();
		
		for (SkillMetadata skill : skills) {
			if ("project".equals(skill.getSource())) {
				projectSkills.add(skill);
			} else {
				userSkills.add(skill);
			}
		}

		if (!userSkills.isEmpty() || !projectSkills.isEmpty()) {
			sb.append("**Skills Locations:**\n");
			if (!userSkills.isEmpty()) {
				sb.append("- User Skills: Global skills available across all projects\n");
			}
			if (!projectSkills.isEmpty()) {
				sb.append("- Project Skills: Project-specific skills (override user skills with same name)\n");
			}
			sb.append("\n");
		}

		sb.append("**Available Skills:**\n\n");

		if (!userSkills.isEmpty()) {
			sb.append("*User Skills:*\n");
			for (SkillMetadata skill : userSkills) {
				sb.append(String.format("- **%s** (skill guide): %s\n", skill.getName(), skill.getDescription()));
				sb.append(String.format("  → MUST read `%s/SKILL.md` first to learn how to use this skill\n", skill.getSkillPath()));
			}
			sb.append("\n");
		}

		if (!projectSkills.isEmpty()) {
			sb.append("*Project Skills:*\n");
			for (SkillMetadata skill : projectSkills) {
				sb.append(String.format("- **%s** (skill guide): %s\n", skill.getName(), skill.getDescription()));
				sb.append(String.format("  → MUST read `%s/SKILL.md` first to learn how to use this skill\n", skill.getSkillPath()));
			}
			sb.append("\n");
		}

		sb.append("**How to Use Skills (MANDATORY Process):**\n\n");
		sb.append("When a user's request matches a skill's description, you MUST follow this process:\n\n");
		sb.append("1. **Read the SKILL.md file**: Use read_file tool with the path shown above\n");
		sb.append("2. **Understand the workflow**: The SKILL.md contains step-by-step instructions\n");
		sb.append("3. **Use your available tools**: Follow the skill's instructions to use tools like shell, read_file, write_file, etc.\n");
		sb.append("4. **Access supporting files**: If the skill references other files, read them using read_file with absolute paths\n\n");
		sb.append("**Example Workflow:**\n");
		sb.append("User asks: \"Search for papers about transformers\"\n");
		sb.append("→ You recognize arxiv-search skill applies\n");
		sb.append("→ You call: read_file(\"path/to/arxiv-search/SKILL.md\")\n");
		sb.append("→ You learn the skill requires executing a Python script with shell tool\n");
		sb.append("→ You call: shell(command=\"python3 path/to/arxiv_search.py 'transformers'\")\n\n");
		sb.append("**Important Notes:**\n");
		sb.append("- Never try to call a skill directly as a tool (e.g., arxiv-search() is WRONG)\n");
		sb.append("- Always read SKILL.md first - it contains the actual instructions\n");
		sb.append("- Skills guide you to use your existing tools in specific ways\n");
		sb.append("- Do not mention the skill name to users unless asked - seamlessly apply its logic\n");

		return sb.toString();
	}

	private SystemMessage enhanceSystemMessage(SystemMessage existing, String skillsSection) {
		if (existing == null) {
			return new SystemMessage(skillsSection);
		}
		return new SystemMessage(existing.getText() + "\n\n" + skillsSection);
	}

	public int getSkillCount() {
		return skillRegistry.size();
	}

	public boolean hasSkill(String skillName) {
		return skillRegistry.contains(skillName);
	}

	public List<SkillMetadata> listSkills() {
		return skillRegistry.listAll();
	}

	/**
	 * Reloads all skills from configured directories.
	 * Clears existing skills and rescans the directories.
	 */
	public synchronized void reloadSkills() {
		logger.info("Reloading skills...");
		skillRegistry.clear();
		skillsLoaded = false;
		loadSkills();
	}

	/**
	 * Loads a skill from the specified directory.
	 * 
	 * @param skillDirectory The directory containing SKILL.md (must not be null or empty)
	 * @throws IllegalArgumentException if skillDirectory is null or empty
	 * @throws IllegalStateException if SKILL.md not found or skill loading fails
	 * @throws RuntimeException if an unexpected error occurs during loading
	 */
	public synchronized void loadSkill(String skillDirectory) {
		if (skillDirectory == null || skillDirectory.isEmpty()) {
			throw new IllegalArgumentException("Skill directory cannot be null or empty");
		}
		
		try {
			SkillScanner scanner = new SkillScanner();
			SkillMetadata skill = scanner.loadSkill(Path.of(skillDirectory));
			
			if (skill == null) {
				throw new IllegalStateException("Failed to load skill from " + skillDirectory);
			}
			
			skillRegistry.register(skill);
			logger.info("Loaded skill '{}' from {}", skill.getName(), skillDirectory);
			
		} catch (IllegalArgumentException | IllegalStateException e) {
			// Re-throw validation and state exceptions
			throw e;
		} catch (Exception e) {
			logger.error("Error loading skill from {}: {}", skillDirectory, e.getMessage(), e);
			throw new RuntimeException("Failed to load skill from " + skillDirectory, e);
		}
	}

	/**
	 * Unloads a skill by name.
	 * 
	 * @param skillName The name of the skill to unload (must not be null or empty)
	 * @throws IllegalArgumentException if skillName is null or empty
	 * @throws IllegalStateException if skill does not exist
	 */
	public synchronized void unloadSkill(String skillName) {
		if (skillName == null || skillName.isEmpty()) {
			throw new IllegalArgumentException("Skill name cannot be null or empty");
		}
		
		if (!skillRegistry.contains(skillName)) {
			throw new IllegalStateException("Skill not found: " + skillName + 
				". Use hasSkill() to check if skill exists before unloading.");
		}
		
		skillRegistry.unregister(skillName);
		logger.info("Unloaded skill '{}'", skillName);
	}

	@Override
	public String getName() {
		return "skills";
	}

	public static class Builder {
		private String userSkillsDirectory;
		private String projectSkillsDirectory;
		private boolean autoScan = true;

		public Builder userSkillsDirectory(String directory) {
			this.userSkillsDirectory = directory;
			return this;
		}

		public Builder projectSkillsDirectory(String directory) {
			this.projectSkillsDirectory = directory;
			return this;
		}

		public Builder autoScan(boolean autoScan) {
			this.autoScan = autoScan;
			return this;
		}

		public SkillsInterceptor build() {
			return new SkillsInterceptor(this);
		}
	}
}
