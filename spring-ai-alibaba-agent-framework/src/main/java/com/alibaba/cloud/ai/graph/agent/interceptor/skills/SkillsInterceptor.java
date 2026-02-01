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
package com.alibaba.cloud.ai.graph.agent.interceptor.skills;

import com.alibaba.cloud.ai.graph.agent.hook.skills.ReadSkillTool;
import com.alibaba.cloud.ai.graph.agent.hook.skills.SkillsAgentHook;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelCallHandler;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelInterceptor;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelRequest;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelResponse;
import com.alibaba.cloud.ai.graph.skills.SkillMetadata;
import com.alibaba.cloud.ai.graph.skills.registry.SkillRegistry;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.util.json.JsonParser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.alibaba.cloud.ai.graph.skills.SkillPromptConstants.buildSkillsPrompt;

/**
 * Interceptor for integrating Claude-style Skills into ReactAgent.
 *
 * This interceptor injects skills metadata into system prompt, following progressive disclosure pattern:
 * - Injects lightweight skills list (name + description + path)
 * - Injects registry type and skill load instructions from the SkillRegistry
 * - LLM reads full SKILL.md content when needed using `read_skill` tool
 *
 * <p><b>Registration:</b>
 * <ul>
 *   <li><b>Recommended:</b> Usually registered automatically via {@link SkillsAgentHook}, which creates
 *       and configures this interceptor along with the `read_skill` tool.</li>
 *   <li><b>Manual:</b> Can also be manually created and registered if you need more control over
 *       the interceptor configuration.</li>
 * </ul>
 *
 * Skills loading is handled by SkillsAgentHook in beforeAgent (if using SkillsAgentHook).
 * This interceptor reads from a shared SkillRegistry to inject skills into the system prompt.
 * The interceptor uses the SkillRegistry's generic methods (getRegistryType(), getSkillLoadInstructions())
 * to build the prompt, making it compatible with any SkillRegistry implementation.
 *
 * <p><b>Usage Examples:</b>
 *
 * <p><b>Automatic registration via SkillsAgentHook (recommended):</b>
 * <pre>
 * FileSystemSkillRegistry registry = FileSystemSkillRegistry.builder().build();
 * SkillsAgentHook hook = SkillsAgentHook.builder()
 *     .skillRegistry(registry)
 *     .autoReload(true)
 *     .build();
 * // SkillsInterceptor is automatically created and registered by the hook
 * </pre>
 *
 * <p><b>Manual registration with grouped tools (skill name → tools for dynamic injection):</b>
 * <pre>
 * Map&lt;String, List&lt;ToolCallback&gt;&gt; groupedTools = Map.of("my-skill", List.of(myTool));
 * SkillsInterceptor interceptor = SkillsInterceptor.builder()
 *     .skillRegistry(registry)
 *     .groupedTools(groupedTools)
 *     .build();
 * </pre>
 *
 * <p>When {@link #groupedTools} is configured, this interceptor scans {@link ModelRequest} messages
 * for {@link org.springframework.ai.chat.messages.AssistantMessage} with tool calls named
 * {@value ReadSkillTool#READ_SKILL}. For each such call, the <i>skill_name</i> argument is recorded.
 * Tools from {@link #getGroupedTools()} for those skill names are then added to the request's
 * {@link ModelRequest#getDynamicToolCallbacks() dynamicToolCallbacks}.
 */
public class SkillsInterceptor extends ModelInterceptor {

	private static final Logger logger = LoggerFactory.getLogger(SkillsInterceptor.class);

	private final SkillRegistry skillRegistry;

	private final Map<String, List<ToolCallback>> groupedTools;

	private SkillsInterceptor(Builder builder) {
		if (builder.skillRegistry == null) {
			throw new IllegalArgumentException("SkillRegistry must be provided. Use SkillsAgentHook to load skills.");
		}
		this.skillRegistry = builder.skillRegistry;
		this.groupedTools = builder.groupedTools != null
				? builder.groupedTools
				: Collections.emptyMap();
	}

	public static Builder builder() {
		return new Builder();
	}

	@Override
	public ModelResponse interceptModel(ModelRequest request, ModelCallHandler handler) {
		List<SkillMetadata> skills = skillRegistry.listAll();

		if (skills.isEmpty()) {
			return handler.call(request);
		}

		// 1. Extract skill names from AssistantMessage with read_skill tool calls
		Set<String> readSkillNames = extractReadSkillNames(request.getMessages());

		// 2. Collect tools from getGroupedTools for those skill names
		List<ToolCallback> skillTools = new ArrayList<>(request.getDynamicToolCallbacks());
		Map<String, List<ToolCallback>> grouped = getGroupedTools();
		for (String skillName : readSkillNames) {
			List<ToolCallback> toolsForSkill = grouped.get(skillName);
			if (toolsForSkill != null && !toolsForSkill.isEmpty()) {
				skillTools.addAll(toolsForSkill);
				if (logger.isInfoEnabled()) {
					logger.info("SkillsInterceptor: added {} tool(s) for skill '{}' to dynamicToolCallbacks",
							toolsForSkill.size(), skillName);
				}
			}
		}

		String skillsPrompt = buildSkillsPrompt(skills, skillRegistry, skillRegistry.getSystemPromptTemplate());
		SystemMessage enhanced = enhanceSystemMessage(request.getSystemMessage(), skillsPrompt);

		if (logger.isDebugEnabled()) {
			logger.debug("Enhanced system message:\n{}", enhanced.getText());
		}

		ModelRequest modified = ModelRequest.builder(request)
				.systemMessage(enhanced)
				.dynamicToolCallbacks(skillTools)
				.build();

		return handler.call(modified);
	}

	/**
	 * Scan messages for AssistantMessage with tool calls named {@value ReadSkillTool#READ_SKILL},
	 * parse each call's arguments for <i>skill_name</i>, and return the set of skill names.
	 */
	private Set<String> extractReadSkillNames(List<Message> messages) {
		if (messages == null || messages.isEmpty()) {
			return Set.of();
		}
		Set<String> names = new LinkedHashSet<>();
		for (Message message : messages) {
			if (!(message instanceof AssistantMessage assistantMessage) || !assistantMessage.hasToolCalls()) {
				continue;
			}
			for (AssistantMessage.ToolCall toolCall : assistantMessage.getToolCalls()) {
				if (!ReadSkillTool.READ_SKILL.equals(toolCall.name())) {
					continue;
				}
				String skillName = parseSkillNameFromArguments(toolCall.arguments());
				if (skillName != null && !skillName.isEmpty()) {
					names.add(skillName);
				}
			}
		}
		return names;
	}

	private static String parseSkillNameFromArguments(String arguments) {
		if (arguments == null || arguments.isBlank()) {
			return null;
		}
		try {
			Object parsed = JsonParser.fromJson(arguments, Map.class);
			if (parsed instanceof Map<?, ?> map) {
				Object v = map.get("skill_name");
				return v != null ? v.toString().trim() : null;
			}
		}
		catch (Exception e) {
			if (logger.isDebugEnabled()) {
				logger.debug("Failed to parse read_skill arguments: {}", e.getMessage());
			}
		}
		return null;
	}

	public Map<String, List<ToolCallback>> getGroupedTools() {
		if (groupedTools.isEmpty()) {
			return Collections.emptyMap();
		}
		return groupedTools.entrySet().stream()
				.collect(Collectors.toMap(Map.Entry::getKey, e -> List.copyOf(e.getValue())));
	}


	private SystemMessage enhanceSystemMessage(SystemMessage existing, String skillsSection) {
		if (existing == null) {
			return new SystemMessage(skillsSection);
		}
		return new SystemMessage(existing.getText() + "\n\n" + skillsSection);
	}

	@Override
	public String getName() {
		return getClass().getSimpleName();
	}

	public static class Builder {
		private SkillRegistry skillRegistry;

		private Map<String, List<ToolCallback>> groupedTools;

		/**
		 * Set a shared SkillRegistry instance.
		 * This must be the same instance used by SkillsAgentHook to share skills data.
		 *
		 * @param skillRegistry the SkillRegistry to use (must not be null)
		 * @return this builder
		 */
		public Builder skillRegistry(SkillRegistry skillRegistry) {
			this.skillRegistry = skillRegistry;
			return this;
		}

		/**
		 * Set grouped tools: map from skill name to the list of tools for that skill.
		 * When the interceptor finds {@value ReadSkillTool#READ_SKILL} tool calls in
		 * AssistantMessage with a given skill_name, it adds the corresponding tools
		 * to the request's {@link ModelRequest#getDynamicToolCallbacks() dynamicToolCallbacks}.
		 *
		 * @param groupedTools map from skill name to list of ToolCallbacks (can be null or empty)
		 * @return this builder
		 */
		public Builder groupedTools(Map<String, List<ToolCallback>> groupedTools) {
			this.groupedTools = groupedTools;
			return this;
		}

		public SkillsInterceptor build() {
			return new SkillsInterceptor(this);
		}
	}
}
