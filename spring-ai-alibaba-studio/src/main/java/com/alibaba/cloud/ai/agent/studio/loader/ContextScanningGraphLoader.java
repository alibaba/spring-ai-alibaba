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

import com.alibaba.cloud.ai.graph.CompiledGraph;

import java.util.Map;

import org.springframework.context.ApplicationContext;

/**
 * Default {@link GraphLoader} that discovers all {@link CompiledGraph} beans from the
 * Spring {@link ApplicationContext} and exposes them by their graph name (from
 * {@link com.alibaba.cloud.ai.graph.StateGraph#getName()} or bean name).
 *
 * <p>This loader is registered automatically when no other {@link GraphLoader} bean,
 * and when {@link CompiledGraph} beans exist, is defined.
 */
public final class ContextScanningGraphLoader extends AbstractGraphLoader {

	private final ApplicationContext applicationContext;

	/**
	 * Creates a loader that discovers all CompiledGraph beans from the given context.
	 *
	 * @param applicationContext the Spring application context
	 */
	public ContextScanningGraphLoader(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

	@Override
	protected Map<String, CompiledGraph> loadGraphMap() {
		return discoverFromContext(applicationContext);
	}
}
