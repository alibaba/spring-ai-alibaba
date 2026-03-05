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

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registers default loaders when none are defined by the application.
 * <ul>
 * <li>{@link AgentLoader}: discovers all {@link com.alibaba.cloud.ai.graph.agent.Agent} beans.</li>
 * <li>{@link GraphLoader}: discovers all {@link CompiledGraph} beans (when graph-core is on classpath).</li>
 * </ul>
 */
@Configuration
public class StudioLoaderAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean(AgentLoader.class)
	public AgentLoader contextScanningAgentLoader(ApplicationContext applicationContext) {
		return new ContextScanningAgentLoader(applicationContext);
	}

	@Bean
	@ConditionalOnMissingBean(GraphLoader.class)
	public GraphLoader contextScanningGraphLoader(ApplicationContext applicationContext) {
		return new ContextScanningGraphLoader(applicationContext);
	}
}
