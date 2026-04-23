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

import com.alibaba.cloud.ai.graph.skills.registry.SkillRegistry;

import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.resolution.ToolCallbackResolver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ToolCallbackResolver for resolving skill-based tools from persisted state.
 *
 * This resolver is used to reconstruct ToolCallback objects for skill tools
 * after checkpoint restore, when the original SkillsAgentHook may not be
 * available in memory.
 *
 * <p>Currently supports resolving the {@code read_skill} tool which allows
 * agents to read skill content from the SkillRegistry.
 *
 * <p><b>Usage:</b>
 * <pre>{@code
 * SkillToolCallbackResolver resolver = new SkillToolCallbackResolver(skillRegistry);
 * ToolCallback tool = resolver.resolve("read_skill");
 * }</pre>
 *
 * @see SkillsAgentHook
 * @see ReadSkillTool
 */
public class SkillToolCallbackResolver implements ToolCallbackResolver {

	private static final Logger logger = LoggerFactory.getLogger(SkillToolCallbackResolver.class);

	/**
	 * The name of the read_skill tool.
	 */
	public static final String READ_SKILL_TOOL_NAME = ReadSkillTool.READ_SKILL;

	private final SkillRegistry skillRegistry;

	/**
	 * Creates a new SkillToolCallbackResolver with the given SkillRegistry.
	 *
	 * @param skillRegistry the skill registry to use for resolving tools (must not be null)
	 * @throws IllegalArgumentException if skillRegistry is null
	 */
	public SkillToolCallbackResolver(SkillRegistry skillRegistry) {
		if (skillRegistry == null) {
			throw new IllegalArgumentException("SkillRegistry cannot be null");
		}
		this.skillRegistry = skillRegistry;
	}

	/**
	 * Resolves a tool callback by name.
	 *
	 * <p>Currently only supports the {@code read_skill} tool. Returns null for
	 * unknown tool names to allow fallback to other resolvers.
	 *
	 * @param toolName the name of the tool to resolve
	 * @return the ToolCallback if found, null otherwise
	 */
	@Override
	public ToolCallback resolve(String toolName) {
		if (toolName == null || toolName.isEmpty()) {
			logger.debug("Cannot resolve null or empty tool name");
			return null;
		}

		if (READ_SKILL_TOOL_NAME.equals(toolName)) {
			logger.debug("Resolving {} tool from SkillRegistry", toolName);
			return ReadSkillTool.createReadSkillToolCallback(skillRegistry, ReadSkillTool.DESCRIPTION);
		}

		logger.debug("Tool {} is not a skill tool, returning null", toolName);
		return null;
	}

	/**
	 * Gets the SkillRegistry used by this resolver.
	 *
	 * @return the skill registry
	 */
	public SkillRegistry getSkillRegistry() {
		return skillRegistry;
	}
}
