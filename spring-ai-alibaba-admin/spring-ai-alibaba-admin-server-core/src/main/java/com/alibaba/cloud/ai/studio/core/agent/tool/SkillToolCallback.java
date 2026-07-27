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

package com.alibaba.cloud.ai.studio.core.agent.tool;

import com.alibaba.cloud.ai.studio.core.agent.skill.SkillMetadata;
import com.alibaba.cloud.ai.studio.core.agent.skill.WorkspaceSkillRegistry;
import com.alibaba.cloud.ai.studio.runtime.domain.chat.ToolCallType;
import com.alibaba.cloud.ai.studio.runtime.utils.JsonUtils;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.metadata.ToolMetadata;

/**
 * Tool callback for reading full SKILL.md content.
 *
 * @since 1.0.0.3
 */
@RequiredArgsConstructor
public class SkillToolCallback implements AgentToolCallback {

	public static final String TOOL_NAME = "read_skill";

	private static final String DESCRIPTION = """
			Reads the full content of a skill (SKILL.md body without frontmatter).
			Provide skill_name and/or skill_path from the available skills list.
			""";

	private static final String INPUT_SCHEMA = """
			{
			  "type": "object",
			  "properties": {
			    "skill_name": {
			      "type": "string",
			      "description": "Skill name from the available skills list"
			    },
			    "skill_path": {
			      "type": "string",
			      "description": "Absolute skillPath from the available skills list"
			    }
			  },
			  "additionalProperties": false
			}
			""";

	private final WorkspaceSkillRegistry skillRegistry;

	@NotNull
	@Override
	public ToolDefinition getToolDefinition() {
		return ToolDefinition.builder().name(TOOL_NAME).description(DESCRIPTION).inputSchema(INPUT_SCHEMA).build();
	}

	@NotNull
	@Override
	public String call(@NotNull String functionInput) {
		try {
			JsonNode node = StringUtils.isBlank(functionInput) ? null : JsonUtils.fromJson(functionInput);
			String skillName = text(node, "skill_name", "skillName");
			String skillPath = text(node, "skill_path", "skillPath");
			if (skillName == null && skillPath == null) {
				return "Error: Either skill_name or skill_path is required";
			}
			if (skillName != null && skillPath != null) {
				SkillMetadata byName = skillRegistry.get(skillName)
					.orElseThrow(() -> new IllegalStateException("Skill not found: " + skillName));
				SkillMetadata byPath = skillRegistry.getByPath(skillPath)
					.orElseThrow(() -> new IllegalStateException("Skill not found: " + skillPath));
				if (!byName.getName().equals(byPath.getName())) {
					return "Error: skill_name and skill_path must refer to the same skill";
				}
				return skillRegistry.readSkillContent(byName.getName());
			}
			if (skillName != null) {
				return skillRegistry.readSkillContent(skillName);
			}
			return skillRegistry.readSkillContentByPath(skillPath);
		}
		catch (Exception e) {
			return "Error: " + e.getMessage();
		}
	}

	@NotNull
	@Override
	public ToolMetadata getToolMetadata() {
		return ToolMetadata.builder().returnDirect(false).build();
	}

	@Override
	public String getId() {
		return TOOL_NAME;
	}

	@Override
	public ToolCallType getToolCallType() {
		return ToolCallType.SKILL_TOOL_CALL;
	}

	private static String text(JsonNode node, String primary, String secondary) {
		if (node == null) {
			return null;
		}
		if (node.has(primary) && !node.get(primary).isNull()) {
			String value = node.get(primary).asText();
			return StringUtils.isBlank(value) ? null : value.trim();
		}
		if (node.has(secondary) && !node.get(secondary).isNull()) {
			String value = node.get(secondary).asText();
			return StringUtils.isBlank(value) ? null : value.trim();
		}
		return null;
	}

}
