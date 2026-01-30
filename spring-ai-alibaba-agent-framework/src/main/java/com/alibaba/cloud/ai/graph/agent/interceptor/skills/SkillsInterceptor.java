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

import com.alibaba.cloud.ai.graph.agent.hook.skills.SkillsAgentHook;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelCallHandler;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelInterceptor;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelRequest;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelResponse;
import com.alibaba.cloud.ai.graph.skills.SkillMetadata;
import com.alibaba.cloud.ai.graph.skills.registry.SkillRegistry;

import org.springframework.ai.chat.messages.SystemMessage;

import java.util.List;

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
 * <p><b>Manual registration:</b>
 * <pre>
 * FileSystemSkillRegistry registry = FileSystemSkillRegistry.builder().build();
 * SkillsInterceptor interceptor = SkillsInterceptor.builder()
 *     .skillRegistry(registry)
 *     .build();
 * // Register the interceptor manually with your agent configuration
 * </pre>
 */
public class SkillsInterceptor extends ModelInterceptor {

	private static final Logger logger = LoggerFactory.getLogger(SkillsInterceptor.class);

	private final SkillRegistry skillRegistry;

	private SkillsInterceptor(Builder builder) {
		if (builder.skillRegistry == null) {
			throw new IllegalArgumentException("SkillRegistry must be provided. Use SkillsAgentHook to load skills.");
		}
		this.skillRegistry = builder.skillRegistry;
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

		String skillsPrompt = buildSkillsPrompt(skills, skillRegistry, skillRegistry.getSystemPromptTemplate());
		SystemMessage enhanced = enhanceSystemMessage(request.getSystemMessage(), skillsPrompt);

		if (logger.isDebugEnabled()) {
			logger.debug("Enhanced system message:\n{}", enhanced.getText());
		}

		ModelRequest modified = ModelRequest.builder(request)
				.systemMessage(enhanced)
				.build();

		return handler.call(modified);
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
