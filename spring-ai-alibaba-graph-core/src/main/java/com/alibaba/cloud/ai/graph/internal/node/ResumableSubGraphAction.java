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

import com.alibaba.cloud.ai.graph.CompileConfig;
import com.alibaba.cloud.ai.graph.RunnableConfig;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static com.alibaba.cloud.ai.graph.RunnableConfig.HUMAN_FEEDBACK_METADATA_KEY;
import static java.lang.String.format;

/**
 * Interface for actions that can resume sub-graphs.
 * Provides a method to get the resume sub-graph identifier.
 */
public interface ResumableSubGraphAction {
	static final String OUTPUT_KEY_TO_PARENT_SUFFIX = "_compiled_graph";
	String CAPTURE_WRITES = "_graph_capture_subgraph_writes_";

	String USE_CHILD_CHECKPOINT = "child_checkpoint";

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

	static String outputKeyToParent(String nodeId) {
		return format("%s_%s", subGraphId(nodeId), OUTPUT_KEY_TO_PARENT_SUFFIX);
	}

	static boolean shouldResume(String nodeId, RunnableConfig config) {
		return config.metadata(resumeSubGraphId(nodeId))
			.filter(value -> Boolean.TRUE.equals(value) || USE_CHILD_CHECKPOINT.equals(value))
			.isPresent();
	}

	static String taskThreadId(RunnableConfig parent, String nodeId, String taskId) {
		String namespace = parent.threadId().orElse("") + ":" + taskId + ":" + nodeId;
		return "task-" + UUID.nameUUIDFromBytes(namespace.getBytes(StandardCharsets.UTF_8));
	}

	static RunnableConfig.Builder childInvocation(RunnableConfig parent, boolean resume) {
		var builder = RunnableConfig.builder(parent).checkPointId(null).nextNode(null).removeMetadata(CAPTURE_WRITES);
		if (!resume) {
			builder.removeMetadata(HUMAN_FEEDBACK_METADATA_KEY)
					.removeMetadata(RunnableConfig.STATE_UPDATE_METADATA_KEY);
		}
		return builder;
	}

	static RunnableConfig.Builder childConfig(String nodeId, RunnableConfig parent,
			CompileConfig parentCompileConfig, CompileConfig childCompileConfig) {
		boolean resume = shouldResume(nodeId, parent);
		var builder = childInvocation(parent, resume).removeMetadata(resumeSubGraphId(nodeId))
				.addMetadata(CAPTURE_WRITES, true);
		var parentSaver = parentCompileConfig.checkpointSaver();
		var childSaver = childCompileConfig.checkpointSaver();
		if (childSaver.isPresent()) {
			if (parentSaver.isEmpty()) {
				throw new IllegalStateException("Missing CheckpointSaver in parent graph!");
			}
			var taskId = parent.taskId();
			if (taskId.isPresent()) {
				builder.threadId(taskThreadId(parent, nodeId, taskId.get()));
			}
			else if (parentSaver.get() == childSaver.get()) {
				builder.threadId(parent.threadId()
						.map(threadId -> format("%s_%s", threadId, subGraphId(nodeId)))
						.orElseGet(() -> subGraphId(nodeId)));
			}
		}
		if (resume && parent.metadata(HUMAN_FEEDBACK_METADATA_KEY).isEmpty()) {
			builder.resume();
		}
		return builder;
	}

}

