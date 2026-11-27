/*
 * Copyright 2025-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.cloud.ai.graph.agent.hook.skills;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;

import com.alibaba.cloud.ai.graph.GraphLifecycleListener;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.context.ContextPage;
import com.alibaba.cloud.ai.graph.agent.tool.ContextSourceTool;
import com.alibaba.cloud.ai.graph.agent.tools.ToolContextHelper;
import com.alibaba.cloud.ai.graph.skills.SkillMetadata;
import com.alibaba.cloud.ai.graph.skills.registry.SkillRegistry;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.util.StringUtils;

/** Searches skill descriptors and reads skill content explicitly by registered name. */
public final class OnDemandSkillTools {

	static final String STATE_KEY = "_ON_DEMAND_SKILLS_";

	private static final int MAX_DESCRIPTION_CHARACTERS = 256;

	private final SkillRegistry registry;

	private final int maxSearchResults;

	private final int pageCharacters;

	OnDemandSkillTools(SkillRegistry registry, int maxSearchResults, int pageCharacters) {
		this.registry = registry;
		this.maxSearchResults = maxSearchResults;
		this.pageCharacters = pageCharacters;
	}

	List<ToolCallback> callbacks() {
		return List.of(new ContextSourceTool(FunctionToolCallback.builder(SearchSkillsTool.SEARCH_SKILLS, this::search)
				.description("Find registered skills by name or purpose. Returns names and short descriptions.")
				.inputType(SearchRequest.class).build()),
			new ContextSourceTool(FunctionToolCallback.builder(ReadSkillTool.READ_SKILL,
					(BiFunction<ReadRequest, ToolContext, Object>) this::read)
				.description("Read a registered skill by name. Use nextOffset to continue reading the same skill.")
				.inputType(ReadRequest.class).build()));
	}

	private List<Descriptor> search(SearchRequest request) {
		return this.registry.search(request.query()).stream().limit(this.maxSearchResults)
			.map(skill -> new Descriptor(skill.getName(),
					ContextPage.read(Objects.toString(skill.getDescription(), ""), 0, MAX_DESCRIPTION_CHARACTERS).content()))
			.toList();
	}

	private Object read(ReadRequest request, ToolContext context) {
		if (!StringUtils.hasText(request.skillName()) || this.registry.get(request.skillName()).isEmpty()) {
			return Map.of("error", "Skill is unavailable");
		}
		ContextPage page;
		try {
			page = ContextPage.read(this.registry.readSkillContent(request.skillName()),
					request.offset(), this.pageCharacters);
		}
		catch (IOException | IllegalStateException error) {
			return Map.of("error", "Skill content could not be read");
		}
		catch (IllegalArgumentException error) {
			return Map.of("error", "Invalid skill read request");
		}
		var execution = ToolContextHelper.getState(context)
			.flatMap(state -> state.value(GraphLifecycleListener.EXECUTION_ID_KEY, String.class));
		var owner = ToolContextHelper.getConfig(context)
			.flatMap(config -> config.metadata(RunnableConfig.AGENT_NAME_KEY)).filter(String.class::isInstance)
			.map(String.class::cast);
		var updates = ToolContextHelper.getStateForUpdate(context);
		if (execution.isPresent() && owner.isPresent() && updates.isPresent()) {
			updates.get().put(STATE_KEY, Map.of(scopePrefix(owner.get()) + request.skillName(), execution.get()));
		}
		return page;
	}

	/** Resolves only skills successfully read by this agent in the current execution. */
	public static List<SkillMetadata> activeSkills(SkillRegistry registry, Map<String, Object> context) {
		if (context == null || !(context.get(RunnableConfig.AGENT_NAME_KEY) instanceof String owner)
				|| !(context.get(GraphLifecycleListener.EXECUTION_ID_KEY) instanceof String execution)
				|| !(context.get(STATE_KEY) instanceof Map<?, ?> readSkills)) {
			return List.of();
		}
		String prefix = scopePrefix(owner);
		return readSkills.entrySet().stream().filter(entry -> execution.equals(entry.getValue()))
			.map(Map.Entry::getKey).filter(String.class::isInstance).map(String.class::cast)
			.filter(key -> key.startsWith(prefix)).map(key -> key.substring(prefix.length())).sorted()
			.map(registry::get).flatMap(Optional::stream).toList();
	}

	private static String scopePrefix(String agentName) {
		return agentName.length() + ":" + agentName + ":";
	}

	private record Descriptor(String name, String description) {
	}

	private record SearchRequest(@JsonPropertyDescription("Words describing the skill you need") String query) {
	}

	private record ReadRequest(@JsonProperty(value = "skill_name", required = true) String skillName,
			@JsonPropertyDescription("Use the nextOffset returned by a previous read") Integer offset) {
	}

}
