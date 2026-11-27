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
package com.alibaba.cloud.ai.graph.internal.node;

import com.alibaba.cloud.ai.graph.CompileConfig;
import com.alibaba.cloud.ai.graph.action.MultiCommand;
import com.alibaba.cloud.ai.graph.action.Send;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.internal.edge.EdgeCondition;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import static com.alibaba.cloud.ai.graph.internal.node.ParallelNode.formatNodeId;

/**
 * Resolves conditional branches and hands them to the same executor as static branches.
 */
public class ConditionalParallelNode extends Node {

	public ConditionalParallelNode(String sourceNodeId, String targetNodeId, EdgeCondition condition,
			Map<String, ActionFactory> nodeFactories) {
		super(formatNodeId(sourceNodeId), compileConfig -> (state, config) -> {
			var restored = config.metadata(ParallelNode.RESTORED_DISPATCH_KEY)
					.filter(MultiCommand.class::isInstance).map(MultiCommand.class::cast);
			return restored.map(CompletableFuture::completedFuture)
				.orElseGet(() -> condition.multiAction().apply(state.snapShot().orElseThrow(), config))
				.thenApply(command -> {
					List<ParallelNode.Branch> branches = Stream.concat(
								command.gotoNodes().stream().map(Send::new), command.sends().stream())
							.map(send -> createBranch(resolveTarget(sourceNodeId, send.node(), condition.mappings()),
									send.input(), nodeFactories, compileConfig))
							.toList();
					return ParallelNode.schedule(formatNodeId(sourceNodeId), targetNodeId, branches, command.update(),
							config);
				});
		});
	}

	private static String resolveTarget(String sourceNodeId, String route, Map<String, String> mappings) {
		String mappedNode = mappings.get(route);
		if (mappedNode != null) {
			return mappedNode;
		}
		if (mappings.containsValue(route)) {
			return route;
		}
		throw new IllegalArgumentException("Unknown parallel route '" + route + "' from node '" + sourceNodeId + "'");
	}

	private static ParallelNode.Branch createBranch(String nodeId, Map<String, Object> input,
			Map<String, ActionFactory> nodeFactories, CompileConfig compileConfig) {
		try {
			return new ParallelNode.Branch(nodeId, nodeFactories.get(nodeId).apply(compileConfig), input);
		}
		catch (GraphStateException ex) {
			throw new IllegalStateException("Cannot create parallel branch for node '" + nodeId + "'", ex);
		}
	}

	@Override
	public final boolean isParallel() {
		return true;
	}

}
