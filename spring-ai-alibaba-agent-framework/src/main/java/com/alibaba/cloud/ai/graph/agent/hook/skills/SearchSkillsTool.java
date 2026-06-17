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

import java.util.List;
import java.util.stream.Collectors;
import java.util.function.BiFunction;

/**
 * Tool for searching the current SkillRegistry by name, description, or path.
 */
public class SearchSkillsTool implements BiFunction<SearchSkillsTool.SearchSkillsRequest, ToolContext, String> {

	public static final String SEARCH_SKILLS = "search_skills";

	public static final String DESCRIPTION = """
			Searches the current SkillRegistry by skill name, description, or skill path.
			
			Usage:
			- Provide a query to search the locally registered skills
			- Matching is performed against name, description, and path
			- Returns matching skills with their descriptions and paths
			""";

	private final SkillRegistry skillRegistry;

	public SearchSkillsTool(SkillRegistry skillRegistry) {
		if (skillRegistry == null) {
			throw new IllegalArgumentException("SkillRegistry cannot be null");
		}
		this.skillRegistry = skillRegistry;
	}

	public static ToolCallback createSearchSkillsToolCallback(SkillRegistry skillRegistry, String description) {
		return FunctionToolCallback.builder(SEARCH_SKILLS, new SearchSkillsTool(skillRegistry))
				.description(description != null ? description : DESCRIPTION)
				.inputType(SearchSkillsRequest.class)
				.build();
	}

	@Override
	public String apply(SearchSkillsRequest request, ToolContext toolContext) {
		String query = request != null ? request.query : null;
		List<SkillMetadata> skills = skillRegistry.search(query);
		if (skills.isEmpty()) {
			return "No skills found.";
		}
		return skills.stream()
				.map(skill -> "- name=%s | description=%s | skill_path=%s | source=%s | allowed_tools=%s".formatted(
						skill.getName(),
						skill.getDescription(),
						skill.getSkillPath(),
						skill.getSource(),
						skill.getAllowedTools()))
				.collect(Collectors.joining("\n"));
	}

	public static class SearchSkillsRequest {

		@JsonProperty("query")
		@JsonPropertyDescription("Search query matched against skill name, description, and skill path")
		public String query;

		public SearchSkillsRequest() {
		}

		public SearchSkillsRequest(String query) {
			this.query = query;
		}
	}

}
