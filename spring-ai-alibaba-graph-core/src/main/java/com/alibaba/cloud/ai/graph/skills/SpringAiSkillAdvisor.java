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

import com.alibaba.cloud.ai.graph.skills.registry.filesystem.FileSystemSkillRegistry;
import com.alibaba.cloud.ai.graph.skills.registry.SkillRegistry;

import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;

import reactor.core.scheduler.Scheduler;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Advisor for integrating Claude-style Skills into Spring AI ChatClient.
 * 
 * This advisor injects skills metadata into system prompt, following progressive disclosure pattern:
 * - Injects lightweight skills list (name + description + path)
 * - LLM reads full SKILL.md content when needed using read_skill tool
 * 
 * Skills are loaded from configured directories during advisor initialization.
 *
 */
public class SpringAiSkillAdvisor implements BaseAdvisor {

	private static final Logger logger = LoggerFactory.getLogger(SpringAiSkillAdvisor.class);

	private final SkillRegistry skillRegistry;
	private final int order;
	private final Scheduler scheduler;
	private final String userSkillsDirectory;
	private final String projectSkillsDirectory;
	private final boolean lazyLoad;

	private SpringAiSkillAdvisor(Builder builder) {
		// Initialize SkillRegistry - use provided or create FileSystemSkillRegistry
		if (builder.skillRegistry != null) {
			this.skillRegistry = builder.skillRegistry;
		} else {
			// Build FileSystemSkillRegistry with provided settings
			FileSystemSkillRegistry.Builder registryBuilder = FileSystemSkillRegistry.builder();
			
			if (builder.userSkillsDirectory != null && !builder.userSkillsDirectory.isEmpty()) {
				registryBuilder.userSkillsDirectory(builder.userSkillsDirectory);
			}
			
			if (builder.projectSkillsDirectory != null && !builder.projectSkillsDirectory.isEmpty()) {
				registryBuilder.projectSkillsDirectory(builder.projectSkillsDirectory);
			}
			
			// Auto-load is enabled by default in FileSystemSkillRegistry
			// If lazy load is enabled, disable auto-load and load manually in before()
			if (builder.lazyLoad) {
				registryBuilder.autoLoad(false);
			}
			
			this.skillRegistry = registryBuilder.build();
		}
		
		// Get directories from registry if it's a FileSystemSkillRegistry (for display purposes)
		if (skillRegistry instanceof FileSystemSkillRegistry) {
			FileSystemSkillRegistry fsRegistry = (FileSystemSkillRegistry) skillRegistry;
			this.userSkillsDirectory = fsRegistry.getUserSkillsDirectory();
			this.projectSkillsDirectory = fsRegistry.getProjectSkillsDirectory();
		} else {
			this.userSkillsDirectory = null;
			this.projectSkillsDirectory = null;
		}
		
		// Set order and scheduler
		this.order = builder.order != null ? builder.order : Advisor.DEFAULT_CHAT_MEMORY_PRECEDENCE_ORDER;
		this.scheduler = builder.scheduler != null ? builder.scheduler : BaseAdvisor.DEFAULT_SCHEDULER;
		this.lazyLoad = builder.lazyLoad;
	}

	/**
	 * Loads all skills from configured directories into the registry.
	 * Only works if the registry is a FileSystemSkillRegistry.
	 */
	private void loadSkillsToRegistry() {
		try {
			skillRegistry.reload();
		} catch (UnsupportedOperationException e) {
			logger.debug("Reload not supported for registry type: {}", skillRegistry.getClass().getName());
		}
	}

	@Override
	public ChatClientRequest before(ChatClientRequest chatClientRequest, AdvisorChain advisorChain) {
		Assert.notNull(chatClientRequest, "chatClientRequest cannot be null");
		
		// Load skills from directories (lazy loading if enabled)
		loadSkillsToRegistry();
		
		List<SkillMetadata> skills = skillRegistry.listAll();
		
		// If no skills, return request as-is
		if (skills.isEmpty()) {
			return chatClientRequest;
		}
		
		// Build skills prompt
		String skillsPrompt = buildSkillsPrompt(skills, skillRegistry, skillRegistry.getSystemPromptTemplate());
		
		// Enhance system message
		SystemMessage systemMessage = chatClientRequest.prompt().getSystemMessage();
		SystemMessage enhanced = enhanceSystemMessage(systemMessage, skillsPrompt);
		
		if (logger.isDebugEnabled()) {
			logger.debug("Enhanced system message with {} skills:\n{}", skills.size(), enhanced.getText());
		}
		
		// Create new request with enhanced system message
		return chatClientRequest.mutate()
			.prompt(chatClientRequest.prompt().augmentSystemMessage(enhanced.getText()))
			.build();
	}

	@Override
	public ChatClientResponse after(ChatClientResponse chatClientResponse, AdvisorChain advisorChain) {
		// No post-processing needed for skills advisor
		return chatClientResponse;
	}


	/**
	 * Enhances the system message with skills prompt.
	 * 
	 * @param existing the existing system message (may be null)
	 * @param skillsSection the skills section to append
	 * @return the enhanced system message
	 */
	private SystemMessage enhanceSystemMessage(SystemMessage existing, String skillsSection) {
		if (existing == null) {
			return new SystemMessage(skillsSection);
		}
		return new SystemMessage(existing.getText() + "\n\n" + skillsSection);
	}

	@Override
	public String getName() {
		return this.getClass().getSimpleName();
	}

	@Override
	public int getOrder() {
		return this.order;
	}

	@Override
	public Scheduler getScheduler() {
		return this.scheduler;
	}

	/**
	 * Get the SkillRegistry instance used by this advisor.
	 * 
	 * @return the SkillRegistry instance
	 */
	public SkillRegistry getSkillRegistry() {
		return skillRegistry;
	}

	/**
	 * Get the number of loaded skills.
	 * 
	 * @return the number of skills
	 */
	public int getSkillCount() {
		return skillRegistry.size();
	}

	/**
	 * Check if a skill exists by name.
	 * 
	 * @param skillName the skill name
	 * @return true if the skill exists
	 */
	public boolean hasSkill(String skillName) {
		return skillRegistry.contains(skillName);
	}

	/**
	 * List all loaded skills.
	 * 
	 * @return list of skill metadata
	 */
	public List<SkillMetadata> listSkills() {
		return skillRegistry.listAll();
	}

	/**
	 * Builds the skills prompt string from the list of skills.
	 *
	 * This method processes the skills list, separates user skills from project skills,
	 * formats them into a skills list, and renders the system prompt template with
	 * the appropriate context variables.
	 *
	 * @param skills the list of skills to include in the prompt
	 * @param skillRegistry the SkillRegistry instance to get registry type and load instructions
	 * @param systemPromptTemplate the SystemPromptTemplate to render the prompt
	 * @return the formatted skills prompt string
	 */
	public static String buildSkillsPrompt(List<SkillMetadata> skills, SkillRegistry skillRegistry, SystemPromptTemplate systemPromptTemplate) {
		List<SkillMetadata> userSkills = new ArrayList<>();
		List<SkillMetadata> projectSkills = new ArrayList<>();
		for (SkillMetadata skill : skills) {
			if ("project".equals(skill.getSource())) {
				projectSkills.add(skill);
			} else {
				userSkills.add(skill);
			}
		}

		StringBuilder skillList = new StringBuilder();
		if (!userSkills.isEmpty()) {
			skillList.append("**User Skills:**\n");
			for (SkillMetadata skill : userSkills) {
				skillList.append(String.format("- **%s**: %s", skill.getName(), skill.getDescription()));
				skillList.append("  → MUST use `read_skill` tool to read `SKILL.md` first to learn how to use this skill \n");
			}
			skillList.append("\n");
		}

		if (!projectSkills.isEmpty()) {
			skillList.append("**Project Skills:**\n");
			for (SkillMetadata skill : projectSkills) {
				skillList.append(String.format("- **%s**: %s", skill.getName(), skill.getDescription()));
				skillList.append("  → MUST use `read_skill` tool to read `SKILL.md` first to learn how to use this skill\n");
			}
			skillList.append("\n");
		}

		Map<String, Object> context = new HashMap<>();
		context.put("skills_list", skillList.toString());
		context.put("skills_load_instructions", skillRegistry.getSkillLoadInstructions());
		return systemPromptTemplate.render(context);
	}

	/**
	 * Builder for creating SpringAiSkillAdvisor instances.
	 *
	 * <p>All configuration parameters are optional and have sensible defaults:
	 * <ul>
	 *   <li><b>userSkillsDirectory</b>: defaults to <code>~/saa/skills</code> - global skills available across all projects</li>
	 *   <li><b>projectSkillsDirectory</b>: defaults to <code>classpath:skills</code> (e.g., src/main/resources/skills) - project-specific skills</li>
	 *   <li><b>skillRegistry</b>: defaults to a new SkillRegistry instance</li>
	 *   <li><b>order</b>: defaults to Advisor.DEFAULT_CHAT_MEMORY_PRECEDENCE_ORDER</li>
	 *   <li><b>scheduler</b>: defaults to BaseAdvisor.DEFAULT_SCHEDULER</li>
	 * </ul>
	 *
	 */
	public static class Builder {
		private String userSkillsDirectory;
		private String projectSkillsDirectory;
		private SkillRegistry skillRegistry;
		private Integer order;
		private Scheduler scheduler;
		private boolean lazyLoad = false;

		/**
		 * Set the user skills directory.
		 * <p><b>Optional</b>: Defaults to <code>~/saa/skills</code> if not specified.
		 *
		 * @param userSkillsDirectory the user skills directory path
		 * @return this builder
		 */
		public Builder userSkillsDirectory(String userSkillsDirectory) {
			this.userSkillsDirectory = userSkillsDirectory;
			return this;
		}

		/**
		 * Set the user skills directory from a Spring Resource.
		 * <p><b>Optional</b>: Defaults to <code>~/saa/skills</code> if not specified.
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
		 * Set the project skills directory.
		 * <p><b>Optional</b>: Defaults to <code>classpath:skills</code> or <code>./skills</code> if not specified.
		 *
		 * @param projectSkillsDirectory the project skills directory path
		 * @return this builder
		 */
		public Builder projectSkillsDirectory(String projectSkillsDirectory) {
			this.projectSkillsDirectory = projectSkillsDirectory;
			return this;
		}

		/**
		 * Set the project skills directory from a Spring Resource.
		 * <p><b>Optional</b>: Defaults to <code>classpath:skills</code> if not specified.
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
		 * Set a shared SkillRegistry instance.
		 * If not set, a new SkillRegistry will be created.
		 * 
		 * @param skillRegistry the SkillRegistry to use
		 * @return this builder
		 */
		public Builder skillRegistry(SkillRegistry skillRegistry) {
			this.skillRegistry = skillRegistry;
			return this;
		}

		/**
		 * Set the order for this advisor.
		 * Defaults to Advisor.DEFAULT_CHAT_MEMORY_PRECEDENCE_ORDER if not specified.
		 * 
		 * @param order the order value
		 * @return this builder
		 */
		public Builder order(int order) {
			this.order = order;
			return this;
		}

		/**
		 * Set the scheduler for streaming operations.
		 * Defaults to BaseAdvisor.DEFAULT_SCHEDULER if not specified.
		 * 
		 * @param scheduler the scheduler to use
		 * @return this builder
		 */
		public Builder scheduler(Scheduler scheduler) {
			this.scheduler = scheduler;
			return this;
		}

		/**
		 * Set whether to enable lazy loading of skills.
		 * <p><b>Optional</b>: Defaults to <code>false</code>
		 * <p>If true, skills will be loaded on first use (in before() method).
		 * If false, skills are loaded during initialization (if using FileSystemSkillRegistry).
		 *
		 * @param lazyLoad true to enable lazy loading, false to disable
		 * @return this builder
		 */
		public Builder lazyLoad(boolean lazyLoad) {
			this.lazyLoad = lazyLoad;
			return this;
		}

		/**
		 * Build the SpringAiSkillAdvisor instance.
		 * 
		 * @return the configured SpringAiSkillAdvisor
		 */
		public SpringAiSkillAdvisor build() {
			return new SpringAiSkillAdvisor(this);
		}
	}

	/**
	 * Create a new builder instance.
	 * 
	 * @return a new Builder instance
	 */
	public static Builder builder() {
		return new Builder();
	}
}
