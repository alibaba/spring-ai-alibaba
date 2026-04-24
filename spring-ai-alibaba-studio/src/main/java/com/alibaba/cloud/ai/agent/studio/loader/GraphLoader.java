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

import java.util.List;

import jakarta.annotation.Nonnull;

/**
 * Interface for loading graphs used by Spring AI Alibaba Studio.
 *
 * <p><strong>Default behavior:</strong> If you do not define a {@code GraphLoader} bean,
 * Studio automatically uses {@link ContextScanningGraphLoader}, which discovers all
 * {@link CompiledGraph} beans from the Spring {@link org.springframework.context.ApplicationContext}
 * and exposes them by their graph name (from {@link com.alibaba.cloud.ai.graph.StateGraph#getName()}
 * or bean name). You can therefore use Studio without implementing this interface: just define your
 * graphs as {@code @Bean}s (e.g. from {@link com.alibaba.cloud.ai.graph.StateGraph#compile()})
 * and they will appear in Studio.
 *
 * <p><strong>Custom loader:</strong> To control which graphs are visible or how they are named,
 * define your own {@code GraphLoader} bean.
 *
 * <p><strong>Thread safety:</strong> Implementations must be thread-safe; they are used as
 * singleton beans and accessed concurrently by multiple HTTP requests.
 */
public interface GraphLoader {

	/**
	 * Returns a list of available graph names.
	 *
	 * @return Immutable list of graph names. Must not return null - return an empty list if no
	 *     graphs are available.
	 */
	@Nonnull
	List<String> listGraphs();

	/**
	 * Loads the CompiledGraph instance for the specified graph name.
	 *
	 * @param name the name of the graph to load
	 * @return CompiledGraph instance for the given name
	 * @throws java.util.NoSuchElementException if the graph doesn't exist
	 * @throws IllegalStateException if the graph exists but fails to load
	 */
	CompiledGraph loadGraph(String name);
}
