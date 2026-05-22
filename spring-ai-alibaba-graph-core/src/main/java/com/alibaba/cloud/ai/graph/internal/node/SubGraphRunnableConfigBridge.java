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
import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.action.InterruptionMetadata;

import java.util.Map;
import java.util.Optional;

import static com.alibaba.cloud.ai.graph.internal.node.ResumableSubGraphAction.resumeSubGraphId;
import static com.alibaba.cloud.ai.graph.internal.node.ResumableSubGraphAction.subGraphId;
import static java.lang.String.format;

/**
 * Shared parent→child {@link RunnableConfig} bridging for embedded compiled subgraphs.
 */
public final class SubGraphRunnableConfigBridge {

	private SubGraphRunnableConfigBridge() {
	}

	/**
	 * Builds a child-namespace config: copies parent metadata (except resume markers), clears
	 * checkpoint cursor, and rewrites {@code threadId} when parent/child share a saver.
	 */
	public static RunnableConfig prepareChildRunnableConfig(RunnableConfig parentConfig, String nodeId,
			CompileConfig parentCompileConfig, CompileConfig childCompileConfig) {
		RunnableConfig childConfig = RunnableConfig.builder(parentConfig)
			.checkPointId(null)
			.nextNode(null)
			.build();
		childConfig.clearContext();
		stripParentResumeMetadata(childConfig, nodeId);

		var parentSaver = parentCompileConfig.checkpointSaver();
		var childSaver = childCompileConfig.checkpointSaver();
		if (childSaver.isPresent()) {
			if (parentSaver.isEmpty()) {
				throw new IllegalStateException("Missing CheckpointSaver in parent graph!");
			}
			if (parentSaver.get() == childSaver.get()) {
				String namespace = subGraphId(nodeId);
				childConfig = RunnableConfig.builder(parentConfig)
					.threadId(parentConfig.threadId()
						.map(threadId -> namespacedThreadId(threadId, namespace))
						.orElse(namespace))
					.nextNode(null)
					.checkPointId(null)
					.build();
				childConfig.clearContext();
				stripParentResumeMetadata(childConfig, nodeId);
			}
		}
		return childConfig;
	}

	/**
	 * Same as {@link #prepareChildRunnableConfig} but uses a custom subgraph namespace key
	 * (e.g. A2A {@code subgraph_<agentCardName>}).
	 */
	public static RunnableConfig prepareChildRunnableConfig(RunnableConfig parentConfig, String nodeId,
			String subGraphNamespaceKey, CompileConfig parentCompileConfig,
			CompileConfig childCompileConfig) {
		if (subGraphNamespaceKey.equals(subGraphId(nodeId))) {
			return prepareChildRunnableConfig(parentConfig, nodeId, parentCompileConfig, childCompileConfig);
		}
		RunnableConfig childConfig = RunnableConfig.builder(parentConfig)
			.checkPointId(null)
			.nextNode(null)
			.build();
		childConfig.clearContext();
		stripParentResumeMetadata(childConfig, nodeId);

		var parentSaver = parentCompileConfig.checkpointSaver();
		var childSaver = childCompileConfig.checkpointSaver();
		if (childSaver.isPresent()) {
			if (parentSaver.isEmpty()) {
				throw new IllegalStateException("Missing CheckpointSaver in parent graph!");
			}
			if (parentSaver.get() == childSaver.get()) {
				childConfig = RunnableConfig.builder(parentConfig)
					.threadId(parentConfig.threadId()
						.map(threadId -> namespacedThreadId(threadId, subGraphNamespaceKey))
						.orElse(subGraphNamespaceKey))
					.nextNode(null)
					.checkPointId(null)
					.build();
				childConfig.clearContext();
				stripParentResumeMetadata(childConfig, nodeId);
			}
		}
		return childConfig;
	}

	/**
	 * Restores child execution from checkpoint only when the child namespace already has one.
	 * Parent {@code resume_subgraph_*} without a child checkpoint must cold-start.
	 */
	public static RunnableConfig resolveForCompiledChildResume(Map<String, Object> stateForChild,
			CompiledGraph childGraph, RunnableConfig preparedChildConfig) throws Exception {
		// Use the child graph's saver: checkpoints after a child interrupt live in the child
		// namespace (or namespaced threadId when parent/child share the same saver instance).
		var childSaver = childGraph.compileConfig.checkpointSaver();
		boolean childCheckpointExists = childSaver.isPresent()
				&& childSaver.get().get(preparedChildConfig).isPresent();
		if (!childCheckpointExists) {
			return preparedChildConfig;
		}
		return childGraph.updateState(preparedChildConfig, stateForChild);
	}

	/**
	 * Forwards {@link InterruptionMetadata} for agent hooks; does not forward {@code resume()}
	 * placeholder.
	 */
	public static RunnableConfig withInterruptionMetadataForHooks(RunnableConfig parentConfig,
			RunnableConfig childConfig) {
		Optional<Object> feedback = parentConfig.metadata(RunnableConfig.HUMAN_FEEDBACK_METADATA_KEY);
		if (feedback.isEmpty() || !(feedback.get() instanceof InterruptionMetadata metadata)) {
			return childConfig;
		}
		RunnableConfig withFeedback = RunnableConfig.builder(childConfig)
			.addMetadata(RunnableConfig.HUMAN_FEEDBACK_METADATA_KEY, metadata)
			.build();
		withFeedback.clearContext();
		return withFeedback;
	}

	public static void stripParentResumeMetadata(RunnableConfig childConfig, String nodeId) {
		childConfig.metadata().ifPresent(m -> {
			m.remove(RunnableConfig.HUMAN_FEEDBACK_METADATA_KEY);
			m.remove(resumeSubGraphId(nodeId));
		});
	}

	/** Avoid double suffix when config is prepared more than once (e.g. A2a inner graph re-entry). */
	static String namespacedThreadId(String threadId, String namespaceKey) {
		String suffix = "_" + namespaceKey;
		if (threadId.equals(namespaceKey) || threadId.endsWith(suffix)) {
			return threadId;
		}
		return format("%s_%s", threadId, namespaceKey);
	}

}
