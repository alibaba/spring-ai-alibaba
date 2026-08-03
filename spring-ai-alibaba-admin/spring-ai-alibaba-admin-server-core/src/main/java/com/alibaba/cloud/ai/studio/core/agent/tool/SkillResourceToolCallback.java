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
 * Tool callback for reading files under a skill directory.
 *
 * @since 1.0.0.3
 */
@RequiredArgsConstructor
public class SkillResourceToolCallback implements AgentToolCallback {

	public static final String TOOL_NAME = "read_skill_resource";

	private static final String DESCRIPTION = """
			Reads a file under a skill directory by relative_path (e.g. references/guide.md).
			Path traversal outside the skill root is rejected.
			""";

	private static final String INPUT_SCHEMA = """
			{
			  "type": "object",
			  "properties": {
			    "skill_name": {
			      "type": "string",
			      "description": "Skill name from the available skills list"
			    },
			    "relative_path": {
			      "type": "string",
			      "description": "Relative path under the skill directory"
			    }
			  },
			  "required": ["skill_name", "relative_path"],
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
			JsonNode node = JsonUtils.fromJson(functionInput);
			String skillName = text(node, "skill_name", "skillName");
			String relativePath = text(node, "relative_path", "relativePath");
			if (StringUtils.isBlank(skillName) || StringUtils.isBlank(relativePath)) {
				return "Error: skill_name and relative_path are required";
			}
			return skillRegistry.readSkillResource(skillName.trim(), relativePath.trim());
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
			return node.get(primary).asText();
		}
		if (node.has(secondary) && !node.get(secondary).isNull()) {
			return node.get(secondary).asText();
		}
		return null;
	}

}
