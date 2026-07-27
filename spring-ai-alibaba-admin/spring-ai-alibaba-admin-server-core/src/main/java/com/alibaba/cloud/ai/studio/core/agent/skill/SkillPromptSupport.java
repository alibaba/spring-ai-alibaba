/*
 * Copyright 2025 the original author or authors.
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

package com.alibaba.cloud.ai.studio.core.agent.skill;

import org.apache.commons.lang3.StringUtils;

import java.util.List;

/**
 * Builds progressive-disclosure skill prompt sections.
 *
 * @since 1.0.0.3
 */
public final class SkillPromptSupport {

	public static final String LOAD_INSTRUCTIONS = """
			## Skill loading instructions
			- The list above only contains skill name, description and skillPath.
			- When a task matches a skill, call tool `read_skill` with skill_name (or skill_path) to load the full SKILL.md body.
			- After loading a skill, you may call `read_skill_resource` with skill_name and a relative_path under that skill directory (e.g. references/guide.md) to read bundled files.
			- Do not invent skill content; always load via tools when needed.
			""".trim();

	private SkillPromptSupport() {
	}

	public static String buildPromptSection(WorkspaceSkillRegistry registry) {
		if (registry == null || registry.isEmpty()) {
			return "";
		}
		StringBuilder sb = new StringBuilder();
		sb.append("## Available skills\n");
		List<SkillMetadata> skills = registry.listAll();
		for (SkillMetadata skill : skills) {
			sb.append("- name: ").append(skill.getName()).append('\n');
			sb.append("  description: ").append(StringUtils.defaultString(skill.getDescription())).append('\n');
			sb.append("  skillPath: ").append(skill.getSkillPath()).append('\n');
		}
		sb.append('\n').append(LOAD_INSTRUCTIONS);
		return sb.toString();
	}

	public static String appendToInstructions(String instructions, WorkspaceSkillRegistry registry) {
		String section = buildPromptSection(registry);
		if (StringUtils.isBlank(section)) {
			return instructions;
		}
		if (StringUtils.isBlank(instructions)) {
			return section;
		}
		return instructions + "\n\n" + section;
	}

}
