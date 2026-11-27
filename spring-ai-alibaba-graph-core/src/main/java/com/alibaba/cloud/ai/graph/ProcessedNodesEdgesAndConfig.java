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
package com.alibaba.cloud.ai.graph;

import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.internal.edge.EdgeValue;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static java.lang.String.format;

/**
 * The type Processed nodes edges and config.
 */
public record ProcessedNodesEdgesAndConfig(StateGraph.Nodes nodes, StateGraph.Edges edges, Set<String> interruptsBefore,
		Set<String> interruptsAfter, Map<String, KeyStrategy> keyStrategyMap, Map<String, String> parallelJoins,
		Map<String, List<String>> inlineGraphPaths) {

	public ProcessedNodesEdgesAndConfig(StateGraph.Nodes nodes, StateGraph.Edges edges, Set<String> interruptsBefore,
			Set<String> interruptsAfter, Map<String, KeyStrategy> keyStrategyMap, Map<String, String> parallelJoins) {
		this(nodes, edges, interruptsBefore, interruptsAfter, keyStrategyMap, parallelJoins, Map.of());
	}

	public ProcessedNodesEdgesAndConfig(StateGraph.Nodes nodes, StateGraph.Edges edges, Set<String> interruptsBefore,
			Set<String> interruptsAfter, Map<String, KeyStrategy> keyStrategyMap) {
		this(nodes, edges, interruptsBefore, interruptsAfter, keyStrategyMap, Map.of());
	}

	/**
	 * Process processed nodes edges and config.
	 * 
	 * @param stateGraph the state graph
	 * @param config     the config
	 * @return the processed nodes edges and config
	 * @throws GraphStateException the graph state exception
	 */
	static ProcessedNodesEdgesAndConfig process(StateGraph stateGraph, CompileConfig config)
			throws GraphStateException {

		var subgraphNodes = stateGraph.nodes.onlySubStateGraphNodes();
		Map<String, KeyStrategy> keyStrategyMap = new LinkedHashMap<>();
		for (var node : stateGraph.nodes.elements) {
			if (node instanceof SubGraphNode subGraphNode) {
				subGraphNode.keyStrategies().forEach(keyStrategyMap::putIfAbsent);
			}
		}

		if (subgraphNodes.isEmpty()) {
			return new ProcessedNodesEdgesAndConfig(stateGraph.nodes, stateGraph.edges, config.interruptsBefore(),
					config.interruptsAfter(), keyStrategyMap, Map.copyOf(stateGraph.parallelJoins));
		}

		var interruptsBefore = config.interruptsBefore();
		var interruptsAfter = config.interruptsAfter();
		var nodes = new StateGraph.Nodes(stateGraph.nodes.exceptSubStateGraphNodes());
		var edges = new StateGraph.Edges(stateGraph.edges.elements);

		var parallelJoins = new LinkedHashMap<>(stateGraph.parallelJoins);
		var inlinePaths = new LinkedHashMap<String, List<String>>();

		for (var subgraphNode : subgraphNodes) {

			var sgWorkflow = subgraphNode.subGraph();

			ProcessedNodesEdgesAndConfig processedSubGraph = process(sgWorkflow, config);
			processedSubGraph.keyStrategyMap().forEach(keyStrategyMap::putIfAbsent);

			StateGraph.Nodes processedSubGraphNodes = processedSubGraph.nodes;
			StateGraph.Edges processedSubGraphEdges = processedSubGraph.edges;

			//
			// Process START Node
			//
			var sgEdgeStart = processedSubGraphEdges.edgeBySourceId(START).orElseThrow();

			if (sgEdgeStart.isParallel()) {
				throw new GraphStateException("subgraph not support start with parallel branches yet!");
			}

			var sgEdgeStartTarget = sgEdgeStart.target();

			if (sgEdgeStartTarget.id() == null) {
				throw new GraphStateException(format("the target for node '%s' is null!", subgraphNode.id()));
			}

			var sgEdgeStartRealTargetId = subgraphNode.formatId(sgEdgeStartTarget.id());
			parallelJoins.replaceAll((fork, join) -> Objects.equals(join, subgraphNode.id()) ? sgEdgeStartRealTargetId : join);

			// Process Interruption (Before) Subgraph(s)
			interruptsBefore = interruptsBefore.stream()
					.map(interrupt -> Objects.equals(subgraphNode.id(), interrupt) ? sgEdgeStartRealTargetId
							: interrupt)
					.collect(Collectors.toUnmodifiableSet());

			var edgesWithSubgraphTargetId = edges.edgesByTargetId(subgraphNode.id());

			if (edgesWithSubgraphTargetId.isEmpty()) {
				throw new GraphStateException(
						format("the node '%s' is not present as target in graph!", subgraphNode.id()));
			}

			for (var edgeWithSubgraphTargetId : edgesWithSubgraphTargetId) {

				var newEdge = edgeWithSubgraphTargetId.withSourceAndTargetIdsUpdated(subgraphNode, Function.identity(),
						id -> new EdgeValue((Objects.equals(id, subgraphNode.id())
								? subgraphNode.formatId(sgEdgeStartTarget.id())
								: id)));
				edges.elements.remove(edgeWithSubgraphTargetId);
				edges.elements.add(newEdge);
			}
			//
			// Process END Nodes
			//
			var sgEdgesEnd = processedSubGraphEdges.edgesByTargetId(END);

			var edgeWithSubgraphSourceId = edges.edgeBySourceId(subgraphNode.id()).orElseThrow();

			if (edgeWithSubgraphSourceId.isParallel()) {
				throw new GraphStateException("subgraph not support routes to parallel branches yet!");
			}
			for (var join : processedSubGraph.parallelJoins().entrySet()) {
				String joinTarget = END.equals(join.getValue()) ? edgeWithSubgraphSourceId.target().id()
						: subgraphNode.formatId(join.getValue());
				if (joinTarget == null) {
					throw new GraphStateException("An inline subgraph that joins at END requires a direct exit edge");
				}
				parallelJoins.put(subgraphNode.formatId(join.getKey()), joinTarget);
			}

			// Process Interruption (After) Subgraph(s)
			if (interruptsAfter.contains(subgraphNode.id())) {

				var exceptionMessage = (edgeWithSubgraphSourceId.target()
						.id() == null) ? "'interruption after' on subgraph is not supported yet!"
								: format(
										"'interruption after' on subgraph is not supported yet! consider to use 'interruption before' node: '%s'",
										edgeWithSubgraphSourceId.target().id());
				throw new GraphStateException(exceptionMessage);
			}

			sgEdgesEnd.stream()
					.map(e -> e.withSourceAndTargetIdsUpdated(subgraphNode, subgraphNode::formatId,
							id -> (Objects.equals(id, END) ? edgeWithSubgraphSourceId.target()
									: new EdgeValue(subgraphNode.formatId(id)))))
					.forEach(edges.elements::add);
			edges.elements.remove(edgeWithSubgraphSourceId);

			//
			// Process edges
			//
			processedSubGraphEdges.elements.stream()
					.filter(e -> !Objects.equals(e.sourceId(), START))
					.filter(e -> !e.anyMatchByTargetId(END))
					.map(e -> e.withSourceAndTargetIdsUpdated(subgraphNode, subgraphNode::formatId,
							id -> new EdgeValue(subgraphNode.formatId(id))))
					.forEach(edges.elements::add);

			//
			// Process nodes
			//
			for (var node : processedSubGraphNodes.elements) {
				var path = new ArrayList<String>();
				path.add(subgraphNode.id());
				path.addAll(processedSubGraph.inlineGraphPaths().getOrDefault(node.id(), List.of()));
				inlinePaths.put(subgraphNode.formatId(node.id()), List.copyOf(path));
				nodes.elements.add(node.withIdUpdated(subgraphNode::formatId));
			}
		}

		return new ProcessedNodesEdgesAndConfig(nodes, edges, interruptsBefore, interruptsAfter, keyStrategyMap,
				Map.copyOf(parallelJoins), Map.copyOf(inlinePaths));
	}
}
