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

import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.hook.HookPosition;
import com.alibaba.cloud.ai.graph.agent.hook.ModelHook;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.tool.ToolCallback;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Hook for integrating Claude-style Skills into ReactAgent.
 * 
 * This hook implements progressive disclosure:
 * 1. On first call: Injects lightweight skill list (name + description) into system prompt
 * 2. On each call: Analyzes user request and injects full content of matched skills
 * 3. Tracks loaded skills per thread to avoid redundant loading
 * 
 * Skills are automatically discovered and applied by the LLM based on relevance.
 */
public class SkillsHook extends ModelHook {

	private static final Logger logger = LoggerFactory.getLogger(SkillsHook.class);

	private final SkillRegistry skillRegistry;

	private final Map<String, Set<String>> loadedSkillsPerThread = new ConcurrentHashMap<>();

	private final Map<String, Boolean> skillsListInjectedPerThread = new ConcurrentHashMap<>();

	private SkillsHook(SkillRegistry skillRegistry) {
		if (skillRegistry == null) {
			throw new IllegalArgumentException("SkillRegistry cannot be null");
		}
		this.skillRegistry = skillRegistry;
	}

	/**
	 * Create a new builder for SkillsHook.
	 * 
	 * <p>Example usage:
	 * <pre>{@code
	 * SkillsHook hook = SkillsHook.builder()
	 *     .skillsDirectory("./skills")
	 *     .skillsDirectory("~/.claude/skills")
	 *     .build();
	 * 
	 * ReactAgent agent = ReactAgent.builder()
	 *     .model(chatModel)
	 *     .hooks(hook)
	 *     .build();
	 * }</pre>
	 * 
	 * @return a new Builder instance
	 */
	public static Builder builder() {
		return new Builder();
	}

	@Override
	public String getName() {
		return "skills";
	}

	@Override
	public HookPosition[] getHookPositions() {
		return new HookPosition[] { HookPosition.BEFORE_MODEL };
	}

	@Override
	public CompletableFuture<Map<String, Object>> beforeModel(OverAllState state, RunnableConfig config) {
		String threadId = config.threadId().orElse("default");
		
		List<Message> messages = extractMessages(state);
		if (messages.isEmpty()) {
			return CompletableFuture.completedFuture(Map.of());
		}

		boolean modified = false;
		List<Message> newMessages = new ArrayList<>(messages);

		if (!skillsListInjectedPerThread.getOrDefault(threadId, false)) {
			String skillsListPrompt = skillRegistry.generateSkillsListPrompt();
			if (!skillsListPrompt.isEmpty()) {
				SystemMessage skillsListMessage = new SystemMessage(skillsListPrompt);
				newMessages.add(0, skillsListMessage);
				skillsListInjectedPerThread.put(threadId, true);
				modified = true;
				logger.debug("Thread {} Injected skills list with {} skills",
					threadId, skillRegistry.size());
			}
		}

		String userRequest = extractLastUserMessage(messages);
		if (userRequest != null && !userRequest.isEmpty()) {
			List<SkillMetadata> matchedSkills = skillRegistry.matchSkills(userRequest);
			
			Set<String> loadedSkills = loadedSkillsPerThread.computeIfAbsent(
				threadId, k -> new HashSet<>());

			for (SkillMetadata skill : matchedSkills) {
				if (!loadedSkills.contains(skill.getName())) {
					try {
						String fullContent = skill.loadFullContent();
						String skillPrompt = formatSkillContent(skill.getName(), fullContent);
						
						SystemMessage skillMessage = new SystemMessage(skillPrompt);
						// Insert before the last user message
						newMessages.add(newMessages.size() - 1, skillMessage);
						
						loadedSkills.add(skill.getName());
						modified = true;
						
						logger.info("[Thread {}] Loaded skill '{}' for request", 
							threadId, skill.getName());
						
					} catch (IOException e) {
						logger.error("[Thread {}] Failed to load skill '{}': {}", 
							threadId, skill.getName(), e.getMessage(), e);
					}
				}
			}
		}

		if (modified) {
			Map<String, Object> update = new HashMap<>();
			update.put("messages", newMessages);
			return CompletableFuture.completedFuture(update);
		}

		return CompletableFuture.completedFuture(Map.of());
	}

	@Override
	public Map<String, KeyStrategy> getKeyStrategys() {
		// No custom key strategies needed
		return Map.of();
	}

	/**
	 * Extract messages from state.
	 */
	@SuppressWarnings("unchecked")
	private List<Message> extractMessages(OverAllState state) {
		return state.value("messages")
			.map(m -> (List<Message>) m)
			.orElse(new ArrayList<>());
	}

	/**
	 * Extract the last user message from the message list.
	 */
	private String extractLastUserMessage(List<Message> messages) {
		for (int i = messages.size() - 1; i >= 0; i--) {
			Message msg = messages.get(i);
			if (msg instanceof UserMessage userMessage) {
				return userMessage.getText();
			}
		}
		return null;
	}

	/**
	 * Format skill content for injection into the prompt.
	 */
	private String formatSkillContent(String skillName, String content) {
		StringBuilder sb = new StringBuilder();
		sb.append("\n## Active Skill: ").append(skillName).append("\n\n");
		sb.append(content);
		sb.append("\n");
		return sb.toString();
	}

	/**
	 * Get the number of registered skills.
	 * For testing purposes only.
	 * 
	 * @return the number of skills
	 */
	public int getSkillCount() {
		return skillRegistry.size();
	}

	/**
	 * Check if a skill is registered.
	 * For testing purposes only.
	 * 
	 * @param skillName the skill name
	 * @return true if the skill is registered
	 */
	public boolean hasSkill(String skillName) {
		return skillRegistry.contains(skillName);
	}

	/**
	 * Get all required tools from registered skills.
	 * This method analyzes all skills' allowed-tools and returns a set of tool names.
	 * 
	 * <p>Example usage:
	 * <pre>{@code
	 * SkillsHook hook = SkillsHook.builder()
	 *     .skillsDirectory("./skills")
	 *     .build();
	 * 
	 * Set<String> requiredTools = hook.getRequiredTools();
	 * // Returns: ["shell", "read", "write", ...]
	 * }</pre>
	 * 
	 * @return set of required tool names
	 */
	public Set<String> getRequiredTools() {
		return skillRegistry.getAllRequiredTools();
	}

	/**
	 * Create tool callbacks for all required tools.
	 * This is a convenience method that automatically creates ToolCallback instances
	 * for all tools required by registered skills.
	 * 
	 * <p>Example usage:
	 * <pre>{@code
	 * SkillsHook hook = SkillsHook.builder()
	 *     .skillsDirectory("./skills")
	 *     .build();
	 * 
	 * ReactAgent agent = ReactAgent.builder()
	 *     .model(chatModel)
	 *     .hooks(hook)
	 *     .tools(hook.createRequiredTools("/workspace"))  // Auto-create all required tools
	 *     .build();
	 * }</pre>
	 * 
	 * @param workspaceRoot the workspace root directory for tools like ShellTool
	 * @return list of ToolCallback instances
	 */
	public List<ToolCallback> createRequiredTools(String workspaceRoot) {
		Set<String> requiredTools = getRequiredTools();
		List<ToolCallback> tools = new ArrayList<>();

		for (String toolName : requiredTools) {
			ToolCallback tool = createToolCallback(toolName, workspaceRoot);
			if (tool != null) {
				tools.add(tool);
			}
		}

		logger.info("Created {} tools for skills: {}", tools.size(), requiredTools);
		return tools;
	}

	/**
	 * Create a single tool callback by name.
	 */
	private ToolCallback createToolCallback(String toolName, String workspaceRoot) {
		String normalizedName = toolName.toLowerCase();
		
		return switch (normalizedName) {
			case "shell" -> com.alibaba.cloud.ai.graph.agent.tools.ShellTool.builder(workspaceRoot)
				.build();
			
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

	/**
	 * Builder for creating SkillsHook instances.
	 * 
	 * <p>Provides a fluent API for configuring skills:
	 * <ul>
	 *   <li>Add skill directories to scan</li>
	 *   <li>Manually register skills</li>
	 *   <li>Control auto-scan behavior</li>
	 * </ul>
	 */
	public static class Builder {
		private final SkillRegistry registry;
		private final SkillScanner scanner;
		private final List<String> directories;
		private boolean autoScan = true;

		private Builder() {
			this.registry = new SkillRegistry();
			this.scanner = new SkillScanner();
			this.directories = new ArrayList<>();
		}

		/**
		 * Add a skills directory to scan.
		 * Can be called multiple times to add multiple directories.
		 * 
		 * @param directory the directory path containing skill folders
		 * @return this builder
		 */
		public Builder skillsDirectory(String directory) {
			if (directory != null && !directory.isEmpty()) {
				this.directories.add(directory);
			}
			return this;
		}

		/**
		 * Add multiple skills directories to scan.
		 * 
		 * @param directories the directory paths
		 * @return this builder
		 */
		public Builder skillsDirectories(String... directories) {
			if (directories != null) {
				for (String dir : directories) {
					skillsDirectory(dir);
				}
			}
			return this;
		}

		/**
		 * Manually register a skill.
		 * 
		 * @param skill the skill metadata
		 * @return this builder
		 */
		public Builder skill(SkillMetadata skill) {
			if (skill != null) {
				this.registry.register(skill);
			}
			return this;
		}

		/**
		 * Manually register multiple skills.
		 * 
		 * @param skills the skill metadata list
		 * @return this builder
		 */
		public Builder skills(List<SkillMetadata> skills) {
			if (skills != null) {
				this.registry.registerAll(skills);
			}
			return this;
		}

		/**
		 * Set whether to automatically scan directories.
		 * Default: true
		 * 
		 * @param autoScan true to scan directories, false to skip
		 * @return this builder
		 */
		public Builder autoScan(boolean autoScan) {
			this.autoScan = autoScan;
			return this;
		}

		/**
		 * Build the SkillsHook instance.
		 * 
		 * <p>This method:
		 * <ol>
		 *   <li>Scans all configured directories (if autoScan is true)</li>
		 *   <li>Registers discovered skills</li>
		 *   <li>Creates and returns the SkillsHook</li>
		 * </ol>
		 * 
		 * @return the configured SkillsHook instance
		 */
		public SkillsHook build() {
			if (autoScan && !directories.isEmpty()) {
				for (String directory : directories) {
					try {
						List<SkillMetadata> discoveredSkills = scanner.scan(directory);
						registry.registerAll(discoveredSkills);
					} catch (Exception e) {
						logger.warn("Failed to scan skill directory '{}': {}", directory, e.getMessage());
					}
				}
			}

			return new SkillsHook(registry);
		}
	}
}
