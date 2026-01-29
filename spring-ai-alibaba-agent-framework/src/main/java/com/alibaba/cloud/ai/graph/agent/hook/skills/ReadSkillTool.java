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

import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;

import java.io.IOException;
import java.util.function.BiFunction;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tool for reading skill content from SkillRegistry.
 *
 * This tool allows the agent to read the full content of a skill by providing
 * the skill name and path. It works with any SkillRegistry implementation.
 */
public class ReadSkillTool implements BiFunction<ReadSkillTool.ReadSkillRequest, ToolContext, String> {

	public static final String READ_SKILL = "read_skill";
	public static final String DESCRIPTION = """
			Reads the full content of a skill from the SkillRegistry.
			You can use this tool to read the complete content of any skill by providing its name.
			
			Usage:
			- The skill_name parameter must match the name of the skill as registered in the registry
			- The tool returns the full content of the skill file (e.g., SKILL.md) without frontmatter
			- If the skill is not found, an error will be returned
			
			Example:
			- read_skill("pdf-extractor")
			""";
	private static final Logger logger = LoggerFactory.getLogger(ReadSkillTool.class);
	private final SkillRegistry skillRegistry;

	public ReadSkillTool(SkillRegistry skillRegistry) {
		if (skillRegistry == null) {
			throw new IllegalArgumentException("SkillRegistry cannot be null");
		}
		this.skillRegistry = skillRegistry;
	}

	/**
	 * Create a ToolCallback for the read skill tool.
	 */
	public static ToolCallback createReadSkillToolCallback(SkillRegistry skillRegistry, String description) {
		return FunctionToolCallback.builder(READ_SKILL, new ReadSkillTool(skillRegistry))
				.description(description != null ? description : DESCRIPTION)
				.inputType(ReadSkillRequest.class)
				.build();
	}

	@Override
	public String apply(ReadSkillRequest request, ToolContext toolContext) {
		try {
			if (request.skillName == null || request.skillName.isEmpty()) {
				return "Error: skill_name is required";
			}

			String content = skillRegistry.readSkillContent(request.skillName);
			return content;
		}
		catch (IllegalArgumentException e) {
			logger.warn("Invalid request for read_skill: {}", e.getMessage());
			return "Error: " + e.getMessage();
		}
		catch (IllegalStateException e) {
			logger.warn("Skill not found: {}", e.getMessage());
			return "Error: " + e.getMessage();
		}
		catch (IOException e) {
			logger.error("Error reading skill content: {}", e.getMessage(), e);
			return "Error reading skill file: " + e.getMessage();
		}
		catch (Exception e) {
			logger.error("Unexpected error reading skill: {}", e.getMessage(), e);
			return "Error: " + e.getMessage();
		}
	}

	/**
	 * Request structure for reading a skill.
	 */
	public static class ReadSkillRequest {

		@JsonProperty(required = true, value = "skill_name")
		@JsonPropertyDescription("The name of the skill to read, must match one of the names in the Available Skills list")
		public String skillName;

		public ReadSkillRequest() {
		}

		public ReadSkillRequest(String skillName) {
			this.skillName = skillName;
		}
	}
}
