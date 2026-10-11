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
import com.alibaba.cloud.ai.graph.utils.TypeRef;

import java.util.Map;
import java.util.Optional;

import static com.alibaba.cloud.ai.graph.internal.node.ResumableSubGraphAction.resumeSubGraphId;
import static com.alibaba.cloud.ai.graph.internal.node.ResumableSubGraphAction.subGraphId;
import static java.lang.String.format;

/**
 * Shared parent→child {@link RunnableConfig} bridging for embedded compiled subgraphs.
 *
 * <p>
 * Resume intent is read from the <strong>parent</strong> config only. Child configs are
 * stripped of resume markers immediately so paths that only call
 * {@link #prepareChildRunnableConfig} (e.g. A2A) never leak parent
 * {@code HUMAN_FEEDBACK}/{@code resume_subgraph_*} into the child namespace.
 * </p>
 */
public final class SubGraphRunnableConfigBridge {

	/**
	 * Per-namespace provenance: set when this bridge has already applied
	 * {@code _<namespaceKey>} to the thread id. Nested embeddings use different keys and
	 * still append; same-key re-entry does not double-suffix.
	 */
	static final String NS_APPLIED_METADATA_PREFIX = "_saa_ns_applied:";

	private SubGraphRunnableConfigBridge() {
	}

	/**
	 * Builds a child-namespace config: copies parent metadata (except resume markers),
	 * clears the checkpoint cursor, and rewrites {@code threadId} when parent/child share
	 * a saver (or {@code forceNamespace}).
	 *
	 * <p>
	 * Run-scoped {@linkplain RunnableConfig#context() context} is copied (not shared) so
	 * the child can read parent values without leaking writes back (#4645). This method
	 * does <strong>not</strong> call {@code clearContext()}; callers that need an empty
	 * child context (e.g. ReactAgent adapter) must clear on their own path.
	 * </p>
	 */
	public static RunnableConfig prepareChildRunnableConfig(RunnableConfig parentConfig, String nodeId,
			CompileConfig parentCompileConfig, CompileConfig childCompileConfig) {
		return prepareChildRunnableConfig(parentConfig, nodeId, subGraphId(nodeId), parentCompileConfig,
				childCompileConfig, false);
	}

	/**
	 * Same as {@link #prepareChildRunnableConfig(RunnableConfig, String, CompileConfig, CompileConfig)}
	 * but uses a custom subgraph namespace key (e.g. A2A {@code subgraph_<agentCardName>}).
	 */
	public static RunnableConfig prepareChildRunnableConfig(RunnableConfig parentConfig, String nodeId,
			String subGraphNamespaceKey, CompileConfig parentCompileConfig, CompileConfig childCompileConfig) {
		return prepareChildRunnableConfig(parentConfig, nodeId, subGraphNamespaceKey, parentCompileConfig,
				childCompileConfig, false);
	}

	/**
	 * @param forceNamespace when {@code true}, always rewrite threadId with the namespace
	 * key (A2A {@code shareState=false}), even if parent/child savers differ or the parent
	 * has no saver at all. When {@code false}, a child saver without a parent saver is
	 * rejected (shared-saver embedding requires both).
	 */
	public static RunnableConfig prepareChildRunnableConfig(RunnableConfig parentConfig, String nodeId,
			String subGraphNamespaceKey, CompileConfig parentCompileConfig, CompileConfig childCompileConfig,
			boolean forceNamespace) {
		var parentSaver = parentCompileConfig.checkpointSaver();
		var childSaver = childCompileConfig.checkpointSaver();

		boolean shareSaver = childSaver.isPresent() && parentSaver.isPresent() && parentSaver.get() == childSaver.get();
		// Shared-saver namespacing requires a parent saver. forceNamespace (A2A shareState=false)
		// only rewrites threadId for remote isolation and must not demand a parent saver —
		// that embedding worked before the bridge (child can keep its own saver alone).
		if (childSaver.isPresent() && parentSaver.isEmpty() && !forceNamespace) {
			throw new IllegalStateException("Missing CheckpointSaver in parent graph!");
		}

		boolean shouldNamespace = forceNamespace || shareSaver;
		RunnableConfig.Builder builder = RunnableConfig.builder(parentConfig).checkPointId(null).nextNode(null);
		if (shouldNamespace) {
			if (!alreadyAppliedNamespace(parentConfig, subGraphNamespaceKey)) {
				builder.threadId(parentConfig.threadId()
						.map(threadId -> format("%s_%s", threadId, subGraphNamespaceKey))
						.orElse(subGraphNamespaceKey));
			}
			builder.addMetadata(nsAppliedKey(subGraphNamespaceKey), Boolean.TRUE);
		}

		RunnableConfig childConfig = builder.build();
		stripParentResumeMetadata(childConfig, nodeId);
		return childConfig;
	}

	/**
	 * Restores child execution from checkpoint only when the parent is resuming
	 * <em>and</em> the child namespace already has a checkpoint. Otherwise cold-starts.
	 * When the child is resumed, forwards real {@link InterruptionMetadata} for HITL
	 * hooks (never the {@code resume()} placeholder string).
	 *
	 * <p>
	 * Parent resume intent is {@code resume_subgraph_*} <strong>or</strong>
	 * {@code HUMAN_FEEDBACK} on the parent config. The latter is required for
	 * {@code ReactAgent.asNode()} because {@code node_async} wrapping usually prevents
	 * the parent runner from setting {@code resume_subgraph_*}.
	 * </p>
	 */
	public static RunnableConfig resolveForCompiledChildResume(Map<String, Object> stateForChild,
			CompiledGraph childGraph, RunnableConfig preparedChildConfig, RunnableConfig parentConfig, String nodeId)
			throws Exception {
		boolean parentResuming = hasParentResumeIntent(parentConfig, nodeId);
		var childSaver = childGraph.compileConfig.checkpointSaver();
		boolean childCheckpointExists = childSaver.isPresent()
				&& childSaver.get().get(preparedChildConfig).isPresent();

		RunnableConfig resolved = preparedChildConfig;
		boolean childResumed = false;
		if (parentResuming && childCheckpointExists) {
			resolved = childGraph.updateState(preparedChildConfig, stateForChild);
			childResumed = true;
		}
		return withInterruptionMetadataForHooks(parentConfig, resolved, childResumed);
	}

	/**
	 * Forwards {@link InterruptionMetadata} for agent hooks only when the child
	 * checkpoint was actually resumed; does not forward {@code resume()} placeholder.
	 */
	public static RunnableConfig withInterruptionMetadataForHooks(RunnableConfig parentConfig,
			RunnableConfig childConfig, boolean childCheckpointResumed) {
		if (!childCheckpointResumed) {
			return childConfig;
		}
		Optional<Object> feedback = parentConfig.metadata(RunnableConfig.HUMAN_FEEDBACK_METADATA_KEY);
		if (feedback.isEmpty() || !(feedback.get() instanceof InterruptionMetadata metadata)) {
			return childConfig;
		}
		return RunnableConfig.builder(childConfig)
			.addMetadata(RunnableConfig.HUMAN_FEEDBACK_METADATA_KEY, metadata)
			.build();
	}

	public static void stripParentResumeMetadata(RunnableConfig childConfig, String nodeId) {
		childConfig.metadata().ifPresent(m -> {
			m.remove(RunnableConfig.HUMAN_FEEDBACK_METADATA_KEY);
			m.remove(resumeSubGraphId(nodeId));
		});
	}

	static boolean hasParentResumeIntent(RunnableConfig parentConfig, String nodeId) {
		if (parentConfig.metadata(RunnableConfig.HUMAN_FEEDBACK_METADATA_KEY).isPresent()) {
			return true;
		}
		return parentConfig.metadata(resumeSubGraphId(nodeId), new TypeRef<Boolean>() {
		}).orElse(false);
	}

	static boolean alreadyAppliedNamespace(RunnableConfig config, String namespaceKey) {
		return config.metadata(nsAppliedKey(namespaceKey)).map(v -> Boolean.TRUE.equals(v)).orElse(false);
	}

	static String nsAppliedKey(String namespaceKey) {
		return NS_APPLIED_METADATA_PREFIX + namespaceKey;
	}

}
