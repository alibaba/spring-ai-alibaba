/*
 * Copyright 2025-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.cloud.ai.examples.multiagents.skills;

import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.hook.skills.SkillsAgentHook;
import com.alibaba.cloud.ai.graph.skills.registry.SkillRegistry;
import com.alibaba.cloud.ai.graph.skills.registry.classpath.ClasspathSkillRegistry;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Configures the SQL assistant agent using the framework's built-in Skills support:
 * {@link ClasspathSkillRegistry} loads skills from classpath {@code skills/} (SKILL.md per skill),
 * {@link SkillsAgentHook} provides the {@code read_skill} tool and injects skill descriptions
 * into the system prompt via {@link com.alibaba.cloud.ai.graph.agent.interceptor.skills.SkillsInterceptor}.
 */
@Configuration
public class SkillsConfig {

	private static final String SYSTEM_PROMPT = """
			You are a SQL query assistant that helps users write queries against business databases.
			Use the read_skill tool when you need detailed schema or business logic for a specific domain.
			""";

	@Bean
	public SkillRegistry skillRegistry() {
		return ClasspathSkillRegistry.builder()
				.classpathPath("skills")
				.build();
	}

	@Bean
	public SkillsAgentHook skillsAgentHook(SkillRegistry skillRegistry) {
		return SkillsAgentHook.builder()
				.skillRegistry(skillRegistry)
				.build();
	}

	@Bean
	public ReactAgent sqlAssistantAgent(ChatModel chatModel, SkillsAgentHook skillsAgentHook) {
		return ReactAgent.builder()
				.name("sql_assistant")
				.systemPrompt(SYSTEM_PROMPT)
				.model(chatModel)
				.hooks(List.of(skillsAgentHook))
				.build();
	}
}
