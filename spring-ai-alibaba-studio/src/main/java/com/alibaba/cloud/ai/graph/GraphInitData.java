/*
 * Copyright 2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.cloud.ai.graph;

import java.util.List;
import java.util.Map;

import com.alibaba.cloud.ai.graph.state.AgentState;

/**
 * Initialization data for the graph.
 *
 * @param title the title of the graph.
 * @param graph the graph content.
 * @param args the arguments for the graph.
 * @param threads the thread entries.
 */
public record GraphInitData(String title, String graph, List<ArgumentMetadata> args, List<ThreadEntry> threads) {

	public GraphInitData(String title, String graph, List<ArgumentMetadata> args) {
		this(title, graph, args, List.of(new ThreadEntry("default", List.of())));
	}

	/**
	 * Metadata for an argument in a request.
	 *
	 * @param name the name of the argument.
	 * @param type the type of the argument.
	 * @param required whether the argument is required.
	 */
	public record ArgumentMetadata(String name, ArgumentType type, boolean required) {
		public enum ArgumentType {

			STRING, IMAGE

		};
	}

	/**
	 * Represents an entry in a thread with its outputs.
	 *
	 * @param id the ID of the thread.
	 * @param entries the outputs of the thread.
	 */
	public record ThreadEntry(String id, List<? extends NodeOutput> entries) {
	}

}
