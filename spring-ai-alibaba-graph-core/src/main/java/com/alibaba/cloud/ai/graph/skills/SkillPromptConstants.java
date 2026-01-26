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
				skillList.append(String.format("- **%s**: %s", skill.getName(), skill.getDescription()));
				skillList.append(String.format("  → Supporting files that skill uses (scripts, references, etc.) are located at directory `%s`, use this path to form the absolute path when reading supporting files. \n", skill.getSkillPath()));
			}
			skillList.append("\n");
		}

		if (!projectSkills.isEmpty()) {
			skillList.append("**Project Skills:**\n");
			for (SkillMetadata skill : projectSkills) {
				skillList.append(String.format("- **%s**: %s", skill.getName(), skill.getDescription()));
				skillList.append(String.format("  → Supporting files that skill uses (scripts, references, etc.) are located at directory `%s`, use this path to form the absolute path when reading supporting files.\n", skill.getSkillPath()));
			}
			skillList.append("\n");
		}

		Map<String, Object> context = new HashMap<>();
		context.put("skills_list", skillList.toString());
		context.put("skills_load_instructions", skillRegistry.getSkillLoadInstructions());
		return systemPromptTemplate.render(context);
	}
}
