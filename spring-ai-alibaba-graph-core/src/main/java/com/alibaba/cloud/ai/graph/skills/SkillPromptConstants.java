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

import com.alibaba.cloud.ai.graph.skills.registry.SkillRegistry;

import org.springframework.ai.chat.prompt.SystemPromptTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SkillPromptConstants {

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
			}
			else {
				userSkills.add(skill);
			}
		}

		StringBuilder skillList = new StringBuilder();
		if (!userSkills.isEmpty()) {
			skillList.append("**User Skills:**\n");
			for (SkillMetadata skill : userSkills) {
				skillList.append(formatSkillEntry(skill));
			}
			skillList.append("\n");
		}

		if (!projectSkills.isEmpty()) {
			skillList.append("**Project Skills:**\n");
			for (SkillMetadata skill : projectSkills) {
				skillList.append(formatSkillEntry(skill));
			}
			skillList.append("\n");
		}

		Map<String, Object> context = new HashMap<>();
		context.put("skills_list", skillList.toString());
		context.put("skills_load_instructions", skillRegistry.getSkillLoadInstructions());
		return systemPromptTemplate.render(context);
	}

	/**
	 * Format a single skill entry for display in the system prompt.
	 *
	 * Includes the skill name, description, optional annotations (license and compatibility),
	 * allowed tools if present, and supporting files path.
	 * @param skill the skill metadata to format
	 * @return the formatted skill entry string
	 */
	static String formatSkillEntry(SkillMetadata skill) {
		String annotations = formatSkillAnnotations(skill);
		StringBuilder entry = new StringBuilder();
		entry.append(String.format("- **%s**: %s", skill.getName(), skill.getDescription()));
		if (!annotations.isEmpty()) {
			entry.append(" (").append(annotations).append(")");
		}
		entry.append("\n");
		if (skill.getAllowedTools() != null && !skill.getAllowedTools().isEmpty()) {
			entry.append(String.format("  -> Allowed tools: %s\n", String.join(", ", skill.getAllowedTools())));
		}
		entry.append(String.format("  -> Supporting files that skill uses (scripts, references, etc.) are located at directory `%s`, use this path to form the absolute path when reading supporting files. \n", skill.getSkillPath()));
		return entry.toString();
	}

	/**
	 * Build a parenthetical annotation string from optional skill fields.
	 *
	 * Combines license and compatibility into a comma-separated string for
	 * display in the system prompt skill listing, matching the Python
	 * _format_skill_annotations pattern.
	 * @param skill the skill metadata to extract annotations from
	 * @return annotation string like "License: MIT, Compatibility: Python 3.10+",
	 * or empty string if neither field is set
	 */
	static String formatSkillAnnotations(SkillMetadata skill) {
		List<String> parts = new ArrayList<>();
		if (skill.getLicense() != null && !skill.getLicense().isEmpty()) {
			parts.add("License: " + skill.getLicense());
		}
		if (skill.getCompatibility() != null && !skill.getCompatibility().isEmpty()) {
			parts.add("Compatibility: " + skill.getCompatibility());
		}
		return String.join(", ", parts);
	}
}
