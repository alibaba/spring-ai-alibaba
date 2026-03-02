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
package com.alibaba.cloud.ai.agent.studio.loader;

import java.util.Map;

import com.alibaba.cloud.ai.graph.agent.Agent;

import org.springframework.context.ApplicationContext;

/**
 * Default {@link AgentLoader} that discovers all {@link Agent} beans from the
 * Spring {@link ApplicationContext} and exposes them by their {@link Agent#name()}.
 *
 * <p>This loader is registered automatically when no other {@link AgentLoader} bean
 * is defined. You can therefore use Studio without implementing {@link AgentLoader}:
 * define your agents as Spring beans (e.g. {@link com.alibaba.cloud.ai.graph.agent.ReactAgent}
 * from {@code @Bean} methods), and they will appear in Studio under their agent name.
 *
 * <p>To take precedence over this default, define your own {@link AgentLoader} bean
 * (e.g. by extending {@link AbstractAgentLoader} or implementing {@link AgentLoader} directly).
 */
public final class ContextScanningAgentLoader extends AbstractAgentLoader {

	private final ApplicationContext applicationContext;

	/**
	 * Creates a loader that discovers all Agent beans from the given context.
	 *
	 * @param applicationContext the Spring application context
	 */
	public ContextScanningAgentLoader(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

	@Override
	protected Map<String, Agent> loadAgentMap() {
		return discoverFromContext(applicationContext);
	}
}
