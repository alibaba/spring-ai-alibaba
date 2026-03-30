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

import com.alibaba.cloud.ai.graph.skills.SkillMetadata;
import com.alibaba.cloud.ai.graph.skills.registry.SkillRegistry;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.function.BiFunction;

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
			You can use this tool to read the complete content of any skill by providing its name or path.
			
			Usage:
			- Provide either skill_name or skill_path
			- If both are provided, they must refer to the same skill
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
			return readSkillContent(request);
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

	private String readSkillContent(ReadSkillRequest request) throws IOException {
		String skillName = normalize(request != null ? request.skillName : null);
		String skillPath = normalize(request != null ? request.skillPath : null);
		if (skillName == null && skillPath == null) {
			throw new IllegalArgumentException("Either skill_name or skill_path is required");
		}

		if (skillName != null && skillPath != null) {
			SkillMetadata skillByName = skillRegistry.get(skillName)
					.orElseThrow(() -> new IllegalStateException("Skill not found: " + skillName));
			SkillMetadata skillByPath = skillRegistry.getByPath(skillPath)
					.orElseThrow(() -> new IllegalStateException("Skill not found: " + skillPath));
			if (!skillByName.getName().equals(skillByPath.getName())) {
				throw new IllegalArgumentException("skill_name and skill_path must refer to the same skill");
			}
			return skillRegistry.readSkillContent(skillByName.getName());
		}

		if (skillName != null) {
			return skillRegistry.readSkillContent(skillName);
		}
		return skillRegistry.readSkillContentByPath(skillPath);
	}

	private static String normalize(String value) {
		return StringUtils.hasText(value) ? value.trim() : null;
	}

	/**
	 * Request structure for reading a skill.
	 */
	public static class ReadSkillRequest {

		@JsonProperty("skill_name")
		@JsonPropertyDescription("The name of the skill to read, must match one of the names in the Available Skills list")
		public String skillName;

		@JsonProperty("skill_path")
		@JsonPropertyDescription("The skill directory path to read")
		public String skillPath;

		public ReadSkillRequest() {
		}

		public ReadSkillRequest(String skillName) {
			this.skillName = skillName;
		}

		public ReadSkillRequest(String skillName, String skillPath) {
			this.skillName = skillName;
			this.skillPath = skillPath;
		}
	}
}
