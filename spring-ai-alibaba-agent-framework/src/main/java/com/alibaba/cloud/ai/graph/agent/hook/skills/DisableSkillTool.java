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
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.util.StringUtils;

import java.util.Optional;
import java.util.function.BiFunction;

/**
 * Tool for disabling a skill in the current SkillRegistry instance.
 */
public class DisableSkillTool implements BiFunction<DisableSkillTool.DisableSkillRequest, ToolContext, String> {

	public static final String DISABLE_SKILL = "disable_skill";

	public static final String DESCRIPTION = """
			Disables a skill in the current SkillRegistry instance without deleting any files.
			
			Usage:
			- Provide either skill_name or skill_path
			- If both are provided, they must refer to the same skill
			- Disabled skills are hidden from the current registry's listings and reads
			""";

	private final SkillRegistry skillRegistry;

	public DisableSkillTool(SkillRegistry skillRegistry) {
		if (skillRegistry == null) {
			throw new IllegalArgumentException("SkillRegistry cannot be null");
		}
		this.skillRegistry = skillRegistry;
	}

	public static ToolCallback createDisableSkillToolCallback(SkillRegistry skillRegistry, String description) {
		return FunctionToolCallback.builder(DISABLE_SKILL, new DisableSkillTool(skillRegistry))
				.description(description != null ? description : DESCRIPTION)
				.inputType(DisableSkillRequest.class)
				.build();
	}

	@Override
	public String apply(DisableSkillRequest request, ToolContext toolContext) {
		String skillName = normalize(request != null ? request.skillName : null);
		String skillPath = normalize(request != null ? request.skillPath : null);
		if (skillName == null && skillPath == null) {
			return "Error: Either skill_name or skill_path is required";
		}

		if (skillName != null && skillPath != null) {
			Optional<SkillMetadata> skillByName = skillRegistry.get(skillName);
			Optional<SkillMetadata> skillByPath = skillRegistry.getByPath(skillPath);
			if (skillByName.isEmpty() || skillByPath.isEmpty()) {
				return "Error: Skill not found or already disabled";
			}
			if (!skillByName.get().getName().equals(skillByPath.get().getName())) {
				return "Error: skill_name and skill_path must refer to the same skill";
			}
		}

		boolean disabled = skillName != null ? skillRegistry.disable(skillName) : skillRegistry.disableByPath(skillPath);
		if (disabled) {
			String target = skillName != null ? skillName : skillPath;
			return "Skill disabled in current registry: " + target;
		}

		if (skillName != null && skillRegistry.isDisabled(skillName)) {
			return "Skill already disabled in current registry: " + skillName;
		}
		return "Error: Skill not found or already disabled";
	}

	private static String normalize(String value) {
		return StringUtils.hasText(value) ? value.trim() : null;
	}

	public static class DisableSkillRequest {

		@JsonProperty("skill_name")
		@JsonPropertyDescription("The registered skill name to disable")
		public String skillName;

		@JsonProperty("skill_path")
		@JsonPropertyDescription("The skill directory path to disable")
		public String skillPath;

		public DisableSkillRequest() {
		}

		public DisableSkillRequest(String skillName, String skillPath) {
			this.skillName = skillName;
			this.skillPath = skillPath;
		}
	}

}
