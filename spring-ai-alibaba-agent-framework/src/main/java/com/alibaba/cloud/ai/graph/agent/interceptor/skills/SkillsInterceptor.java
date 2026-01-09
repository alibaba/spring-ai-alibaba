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
import org.springframework.ai.tool.ToolCallback;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
		sb.append("You have access to a skills library that provides specialized capabilities and domain knowledge.\n\n");

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
				sb.append(String.format("- **%s**: %s\n", skill.getName(), skill.getDescription()));
				sb.append(String.format("  → Read `%s/SKILL.md` for full instructions\n", skill.getSkillPath()));
			}
			sb.append("\n");
		}

		if (!projectSkills.isEmpty()) {
			sb.append("*Project Skills:*\n");
			for (SkillMetadata skill : projectSkills) {
				sb.append(String.format("- **%s**: %s\n", skill.getName(), skill.getDescription()));
				sb.append(String.format("  → Read `%s/SKILL.md` for full instructions\n", skill.getSkillPath()));
			}
			sb.append("\n");
		}

		sb.append("**How to Use Skills (Progressive Disclosure):**\n\n");
		sb.append("Skills follow a **progressive disclosure** pattern - you know they exist (name + description above), ");
		sb.append("but you only read the full instructions when needed:\n\n");
		sb.append("1. **Recognize when a skill applies**: Check if the user's task matches any skill's description\n");
		sb.append("2. **Read the skill's full instructions**: Use read_file tool with the path shown above\n");
		sb.append("3. **Follow the skill's instructions**: SKILL.md contains step-by-step workflows, best practices, and examples\n");
		sb.append("4. **Access supporting files**: Skills may include Python scripts, configs, or additional markdown files - ");
		sb.append("use absolute paths and read_file tool to access them as referenced in SKILL.md\n\n");
		sb.append("**Important Notes:**\n");
		sb.append("- Do not mention the skill name unless asked - seamlessly apply its logic\n");
		sb.append("- If a skill references other files (e.g., \"See extraction.md for details\"), read them using read_file\n");
		sb.append("- Use absolute paths for all file operations within skills\n");

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

	public Set<String> getRequiredTools() {
		return skillRegistry.getAllRequiredTools();
	}

	public synchronized void reloadSkills() {
		logger.info("Reloading skills...");
		skillRegistry.clear();
		skillsLoaded = false;
		loadSkills();
	}

	public synchronized boolean loadSkill(String skillDirectory) {
		try {
			SkillScanner scanner = new SkillScanner();
			SkillMetadata skill = scanner.loadSkill(Path.of(skillDirectory));
			
			if (skill != null) {
				skillRegistry.register(skill);
				logger.info("Loaded skill '{}' from {}", skill.getName(), skillDirectory);
				return true;
			} else {
				logger.warn("Failed to load skill from {}", skillDirectory);
				return false;
			}
		} catch (Exception e) {
			logger.error("Error loading skill from {}: {}", skillDirectory, e.getMessage(), e);
			return false;
		}
	}

	public synchronized boolean unloadSkill(String skillName) {
		boolean removed = skillRegistry.unregister(skillName);
		if (removed) {
			logger.info("Unloaded skill '{}'", skillName);
		}
		return removed;
	}

	@Override
	public List<ToolCallback> getTools() {
		Set<String> requiredTools = getRequiredTools();
		List<ToolCallback> tools = new ArrayList<>();

		for (String toolName : requiredTools) {
			ToolCallback tool = createToolCallback(toolName);
			if (tool != null) {
				tools.add(tool);
			}
		}

		return tools;
	}

	private ToolCallback createToolCallback(String toolName) {
		String normalizedName = toolName.toLowerCase();
		
		return switch (normalizedName) {
			case "shell" -> null;
			
			case "read", "read_file" -> com.alibaba.cloud.ai.graph.agent.extension.tools.filesystem.ReadFileTool
				.createReadFileToolCallback(
					com.alibaba.cloud.ai.graph.agent.extension.tools.filesystem.ReadFileTool.DESCRIPTION
				);
			
			case "write", "write_file" -> com.alibaba.cloud.ai.graph.agent.extension.tools.filesystem.WriteFileTool
				.createWriteFileToolCallback(
					com.alibaba.cloud.ai.graph.agent.extension.tools.filesystem.WriteFileTool.DESCRIPTION
				);
			
			case "list", "list_files" -> com.alibaba.cloud.ai.graph.agent.extension.tools.filesystem.ListFilesTool
				.createListFilesToolCallback(
					com.alibaba.cloud.ai.graph.agent.extension.tools.filesystem.ListFilesTool.DESCRIPTION
				);
			
			case "grep", "grep_search" -> com.alibaba.cloud.ai.graph.agent.extension.tools.filesystem.GrepTool
				.createGrepToolCallback(
					com.alibaba.cloud.ai.graph.agent.extension.tools.filesystem.GrepTool.DESCRIPTION
				);
			
			case "glob", "glob_search" -> com.alibaba.cloud.ai.graph.agent.extension.tools.filesystem.GlobTool
				.createGlobToolCallback(
					com.alibaba.cloud.ai.graph.agent.extension.tools.filesystem.GlobTool.DESCRIPTION
				);
			
			case "edit", "edit_file" -> com.alibaba.cloud.ai.graph.agent.extension.tools.filesystem.EditFileTool
				.createEditFileToolCallback(
					com.alibaba.cloud.ai.graph.agent.extension.tools.filesystem.EditFileTool.DESCRIPTION
				);
			
			default -> {
				logger.warn("Unknown tool '{}' required by skills, skipping", toolName);
				yield null;
			}
		};
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
