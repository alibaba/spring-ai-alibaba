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

import com.alibaba.cloud.ai.graph.agent.interceptor.ModelCallHandler;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelInterceptor;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelRequest;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelResponse;
import com.alibaba.cloud.ai.graph.skills.SkillMetadata;
import com.alibaba.cloud.ai.graph.skills.registry.SkillRegistry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.alibaba.cloud.ai.graph.skills.SkillPrompt.SKILLS_SYSTEM_PROMPT_TEMPLATE;

/**
 * Interceptor for integrating Claude-style Skills into ReactAgent.
 * 
 * This interceptor injects skills metadata into system prompt, following progressive disclosure pattern:
 * - Injects lightweight skills list (name + description + path)
 * - LLM reads full SKILL.md content when needed using read_file tool
 * 
 * Skills loading is handled by SkillsAgentHook in beforeAgent.
 * This interceptor reads from a shared SkillRegistry to inject skills into the system prompt.
 * 
 * Usage with SkillsAgentHook:
 * <pre>
 * FileSystemSkillRegistry registry = FileSystemSkillRegistry.builder().build();
 * SkillsAgentHook hook = SkillsAgentHook.builder()
 *     .skillRegistry(registry)
 *     .userSkillsDirectory("~/.spring-ai/skills")
 *     .projectSkillsDirectory("./.spring-ai/skills")
 *     .build();
 * SkillsInterceptor interceptor = SkillsInterceptor.builder()
 *     .skillRegistry(registry)
 *     .build();
 * </pre>
 */
public class SkillsInterceptor extends ModelInterceptor {

	private static final Logger logger = LoggerFactory.getLogger(SkillsInterceptor.class);

	private final SkillRegistry skillRegistry;
	private final SystemPromptTemplate systemPromptTemplate;

	private SkillsInterceptor(Builder builder) {
		if (builder.skillRegistry == null) {
			throw new IllegalArgumentException("SkillRegistry must be provided. Use SkillsAgentHook to load skills.");
		}
		this.skillRegistry = builder.skillRegistry;
		this.systemPromptTemplate = SystemPromptTemplate.builder().template(SKILLS_SYSTEM_PROMPT_TEMPLATE).build();
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

		String skillsPrompt = buildSkillsPrompt(skills);
		SystemMessage enhanced = enhanceSystemMessage(request.getSystemMessage(), skillsPrompt);

		if (logger.isDebugEnabled()) {
			logger.debug("Enhanced system message:\n{}", enhanced.getText());
		}

		ModelRequest modified = ModelRequest.builder(request)
			.systemMessage(enhanced)
			.build();

		return handler.call(modified);
	}

	private String buildSkillsPrompt(List<SkillMetadata> skills) {
		List<SkillMetadata> userSkills = new ArrayList<>();
		List<SkillMetadata> projectSkills = new ArrayList<>();
		for (SkillMetadata skill : skills) {
			if ("project".equals(skill.getSource())) {
				projectSkills.add(skill);
			} else {
				userSkills.add(skill);
			}
		}

		StringBuilder skillLocations = new StringBuilder();
		if (!userSkills.isEmpty() || !projectSkills.isEmpty()) {
			if (!userSkills.isEmpty()) {
				skillLocations.append(String.format("- **User Skills**: `%s`\n", this.skillRegistry.getUserSkillsDirectory()));
			}
			if (!projectSkills.isEmpty()) {
				skillLocations.append(String.format("- **Project Skills**: `%s` (override user skills with same name)\n", this.skillRegistry.getProjectSkillsDirectory()));
			}
			skillLocations.append("\n");
		}


		StringBuilder skillList = new StringBuilder();
		if (!userSkills.isEmpty()) {
			skillList.append("**User Skills:**\n");
			for (SkillMetadata skill : userSkills) {
				skillList.append(String.format("- **%s**: %s", skill.getName(), skill.getDescription()));
				skillList.append(String.format("  → MUST use `read_skill` tool to read `%s/SKILL.md` first to learn how to use this skill \n", skill.getSkillPath()));
			}
			skillList.append("\n");
		}

		if (!projectSkills.isEmpty()) {
			skillList.append("**Project Skills:**\n");
			for (SkillMetadata skill : projectSkills) {
				skillList.append(String.format("- **%s**: %s", skill.getName(), skill.getDescription()));
				skillList.append(String.format("  → MUST use `read_skill` tool to read `%s/SKILL.md` first to learn how to use this skill\n", skill.getSkillPath()));
			}
			skillList.append("\n");
		}

		Map<String, Object> context = new HashMap<>();
		context.put("skills_locations", skillLocations.toString());
		context.put("skills_list", skillList.toString());
		return systemPromptTemplate.render(context);
	}

	private SystemMessage enhanceSystemMessage(SystemMessage existing, String skillsSection) {
		if (existing == null) {
			return new SystemMessage(skillsSection);
		}
		return new SystemMessage(existing.getText() + "\n\n" + skillsSection);
	}

	public int getSkillCount() {
		return skillRegistry.size();
	}

	public boolean hasSkill(String skillName) {
		return skillRegistry.contains(skillName);
	}

	public List<SkillMetadata> listSkills() {
		return skillRegistry.listAll();
	}

	/**
	 * Get the SkillRegistry instance used by this interceptor.
	 * 
	 * @return the SkillRegistry instance
	 */
	public SkillRegistry getSkillRegistry() {
		return skillRegistry;
	}

	/**
	 * Reloads all skills from the registry.
	 * Delegates to the SkillRegistry's reload() method.
	 * 
	 * @throws UnsupportedOperationException if the registry does not support reloading
	 */
	public void reloadSkills() {
		skillRegistry.reload();
	}

	@Override
	public String getName() {
		return getClass().getSimpleName();
	}

	public static class Builder {
		private SkillRegistry skillRegistry;

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

		public SkillsInterceptor build() {
			return new SkillsInterceptor(this);
		}
	}
}
