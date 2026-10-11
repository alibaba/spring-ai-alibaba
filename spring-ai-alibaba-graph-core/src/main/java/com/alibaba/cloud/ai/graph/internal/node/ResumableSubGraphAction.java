/*
 * Copyright 2024-2026 the original author or authors.
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

package com.alibaba.cloud.ai.graph.internal.node;

import com.alibaba.cloud.ai.graph.RunnableConfig;

import static java.lang.String.format;

/**
 * Interface for actions that can resume sub-graphs.
 * Provides a method to get the resume sub-graph identifier.
 */
public interface ResumableSubGraphAction {
	static final String OUTPUT_KEY_TO_PARENT_SUFFIX = "_compiled_graph";

	/**
	 * Gets the resume sub-graph identifier.
	 * @return the resume sub-graph ID
	 */
	String getResumeSubGraphId();

	static String subGraphId(String nodeId) {
		return format("subgraph_%s", nodeId);
	}

	static String resumeSubGraphId(String nodeId) {
		return format("resume_%s", subGraphId(nodeId));
	}

	/**
	 * Removes parent resume metadata when the current subgraph is not the resume target.
	 * Parent graph metadata is copied for every child invocation, but human feedback
	 * belongs only to the interrupted child. Forwarding it to a later child makes that
	 * child incorrectly initialize in resume mode.
	 * @param config the isolated configuration created for the child graph
	 * @param resumeSubgraph whether this child is the interrupted resume target
	 * @return the scoped child configuration
	 */
	static RunnableConfig scopeResumeMetadata(RunnableConfig config, boolean resumeSubgraph) {
		if (!resumeSubgraph) {
			config.metadata().ifPresent(metadata -> metadata.remove(RunnableConfig.HUMAN_FEEDBACK_METADATA_KEY));
		}
		return config;
	}

	static String outputKeyToParent(String nodeId) {
		return format("%s_%s", subGraphId(nodeId), OUTPUT_KEY_TO_PARENT_SUFFIX);
	}

}
