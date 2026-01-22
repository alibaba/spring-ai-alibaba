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
package com.alibaba.cloud.ai.graph.internal;

import com.alibaba.cloud.ai.graph.CompileConfig;
import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.ProcessedNodesEdgesAndConfig;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.action.AsyncNodeActionWithConfig;
import com.alibaba.cloud.ai.graph.exception.Errors;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.internal.edge.Edge;
import com.alibaba.cloud.ai.graph.internal.edge.EdgeValue;
import com.alibaba.cloud.ai.graph.internal.node.Node;
import com.alibaba.cloud.ai.graph.internal.node.ParallelNode;
import com.alibaba.cloud.ai.graph.internal.node.SubCompiledGraphNode;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static java.lang.String.format;

/**
 * Processor for handling advanced parallel edge detection and subgraph creation.
 * Handles both simple cases (A->B->Z) and complex cases (A->B->B1->Z) with nested parallelism.
 */
public class ParallelEdgeProcessor {

	/**
	 * Represents a path from a start node to a convergence (merging) node.
	 * The nodes list stores edge pairs sequentially: [source1, target1, source2, target2, ...]
	 * This allows proper restoration of parallel edges when creating subgraphs.
	 */
	public static class ParallelPath {
		final List<String> nodes;  // Edge pairs: [src1, tgt1, src2, tgt2, ...] for addEdge calls
		final String startNodeId;   // First node in the path (after the parallel branch start)
		final String convergenceNodeId;  // Last node where paths converge

		public ParallelPath(List<String> nodes, String startNodeId, String convergenceNodeId) {
			this.nodes = new ArrayList<>(nodes);
			this.startNodeId = startNodeId;
			this.convergenceNodeId = convergenceNodeId;
		}

		/**
		 * Returns the number of unique nodes in the path.
		 * For edge pairs [src1, tgt1, src2, tgt2, ...], this counts all unique nodes.
		 * For a path B->B1->Z: edgePairs = [B, B1, B1, Z], uniqueNodes = {B, B1, Z}, length = 3
		 */
		public int pathLength() {
			Set<String> uniqueNodes = new HashSet<>(nodes);
			// If edge pairs are empty (startNodeId == convergenceNodeId), return 1
			if (uniqueNodes.isEmpty()) {
				return 1;
			}
			return uniqueNodes.size();
		}

		/**
		 * Returns all unique nodes in this path.
		 */
		public Set<String> uniqueNodes() {
			return new HashSet<>(nodes);
		}
	}

	private final ProcessedNodesEdgesAndConfig processedData;
	private final Map<String, Node.ActionFactory> nodeFactories;
	private final CompileConfig compileConfig;
	private final Map<String, KeyStrategy> keyStrategyMap;
	private final StateGraph stateGraph;

	/**
	 * Creates a new ParallelEdgeProcessor with the required dependencies.
	 *
	 * @param processedData the processed nodes, edges and config
	 * @param nodeFactories the map of node factories
	 * @param compileConfig the compile configuration
	 * @param keyStrategyMap the key strategy map
	 * @param stateGraph the original state graph
	 */
	public ParallelEdgeProcessor(ProcessedNodesEdgesAndConfig processedData,
			Map<String, Node.ActionFactory> nodeFactories,
			CompileConfig compileConfig,
			Map<String, KeyStrategy> keyStrategyMap,
			StateGraph stateGraph) {
		this.processedData = processedData;
		this.nodeFactories = nodeFactories;
		this.compileConfig = compileConfig;
		this.keyStrategyMap = keyStrategyMap;
		this.stateGraph = stateGraph;
	}

	/**
	 * Processes parallel edges with advanced path detection and subgraph creation.
	 * Handles both simple cases (A->B->Z) and complex cases (A->B->B1->Z) with nested parallelism.
	 * 
	 * Implementation approach:
	 * 1. Find the first-level convergence node from the parallel start nodes
	 * 2. Find paths from each start node to the convergence node
	 * 3. If paths are simple (length=2), use ParallelNode
	 * 4. If paths are complex (length>2), create a subgraph containing all path nodes and edges
	 * 5. The subgraph is compiled into CompiledGraph, which will automatically handle
	 *    any nested parallel edges within the subgraph during compilation
	 *
	 * @param sourceNodeId the source node with parallel edges
	 * @param targets the list of parallel target edge values
	 * @param edgesUpdater callback to update edges map
	 * @param nodeFactoriesUpdater callback to update node factories map
	 * @throws GraphStateException if processing fails
	 */
	public void processAdvancedParallelEdges(String sourceNodeId, List<EdgeValue> targets,
			BiConsumer<String, EdgeValue> edgesUpdater,
			BiConsumer<String, Node.ActionFactory> nodeFactoriesUpdater) throws GraphStateException {
		// Filter valid targets (must have node factories)
		List<EdgeValue> validTargets = targets.stream()
				.filter(target -> target.id() != null && nodeFactories.containsKey(target.id()))
				.toList();

		if (validTargets.isEmpty()) {
			return;
		}

		// Check for conditional edges
		List<String> conditionalEdgeTargets = validTargets.stream()
				.filter(target -> target.value() != null)
				.map(EdgeValue::id)
				.filter(Objects::nonNull)
				.toList();
		if (!conditionalEdgeTargets.isEmpty()) {
			throw Errors.unsupportedConditionalEdgeOnParallelNode.exception(sourceNodeId,
					conditionalEdgeTargets);
		}

		// Get start node IDs for parallel branches
		Set<String> startNodeIds = validTargets.stream()
				.map(EdgeValue::id)
				.filter(Objects::nonNull)
				.collect(Collectors.toSet());

		// Find convergence node
		String convergenceNodeId = findConvergenceNode(startNodeIds);
		if (convergenceNodeId == null) {
			throw Errors.illegalMultipleTargetsOnParallelNode.exception(sourceNodeId,
					startNodeIds);
		}

		// Find paths from each start node to convergence node
		List<ParallelPath> paths = new ArrayList<>();
		for (String startNodeId : startNodeIds) {
			List<String> pathEdgePairs = findPathToConvergence(startNodeId, convergenceNodeId);
			// Empty edge pairs means startNodeId == convergenceNodeId (same node)
			// In this case, we still create a path but with empty edge pairs
			paths.add(new ParallelPath(pathEdgePairs, startNodeId, convergenceNodeId));
		}

		// Check if any path has length > 2 (i.e., more than just start->convergence)
		// Path length includes both start and convergence nodes, so:
		// - Length 2: start->convergence (simple case, use ParallelNode directly)
		// - Length > 2: start->...->convergence (complex case, create subgraph for each path)
		// Note: pathLength() counts unique nodes in the edge pairs list
		// For a path B->B1->Z: edgePairs = [B, B1, B1, Z], uniqueNodes = {B, B1, Z}, length = 3
		boolean needsSubgraph = paths.stream().anyMatch(path -> {
			// pathLength() returns unique node count from edge pairs
			// We need to check if there are intermediate nodes (length > 2 means start + convergence + at least one intermediate)
			return path.pathLength() > 2;
		});

		if (needsSubgraph) {
			// Create a subgraph for EACH path, then assemble them into a ParallelNode
			// This allows each path (including nested parallel edges) to be processed independently
			List<AsyncNodeActionWithConfig> subGraphActions = new ArrayList<>();
			List<String> subGraphNodeIds = new ArrayList<>();

			for (ParallelPath path : paths) {
				// Create subgraph for this single path
				StateGraph subStateGraph = createSubgraphForPath(path);

				// Compile the subgraph into CompiledGraph
				CompiledGraph subCompiledGraph = subStateGraph.compile(compileConfig);

				// Create SubCompiledGraphNode for this path
				String subGraphNodeId = format("__PARALLEL_SUBGRAPH__(%s->%s)", sourceNodeId, path.startNodeId);
				var subGraphNode = new SubCompiledGraphNode(subGraphNodeId, subCompiledGraph);

				// Collect actions and node IDs for ParallelNode
				try {
					subGraphActions.add(subGraphNode.actionFactory().apply(compileConfig));
				} catch (GraphStateException ex) {
					throw new RuntimeException("Failed to create subgraph action for path starting at: "
							+ path.startNodeId + ". Cause: " + ex.getMessage(), ex);
				}
				subGraphNodeIds.add(subGraphNodeId);
			}

			// Create a ParallelNode that executes all subgraphs in parallel
			var parallelNode = new ParallelNode(sourceNodeId, convergenceNodeId, subGraphActions, subGraphNodeIds,
					keyStrategyMap, compileConfig);

			nodeFactoriesUpdater.accept(parallelNode.id(), parallelNode.actionFactory());
			edgesUpdater.accept(sourceNodeId, new EdgeValue(parallelNode.id()));
			edgesUpdater.accept(parallelNode.id(), new EdgeValue(convergenceNodeId));
		} else {
			// Simple case: A->B->Z, A->C->Z - use ParallelNode
			var targetList = validTargets;

			var actions = targetList.stream()
					.map(target -> {
						try {
							return nodeFactories.get(target.id()).apply(compileConfig);
						} catch (GraphStateException ex) {
							throw new RuntimeException("Failed to create parallel node action for target: "
									+ target.id() + ". Cause: " + ex.getMessage(), ex);
						}
					})
					.toList();

			var actionNodeIds = targetList.stream().map(EdgeValue::id).toList();

			var parallelNode = new ParallelNode(sourceNodeId, convergenceNodeId, actions, actionNodeIds,
					keyStrategyMap, compileConfig);

			nodeFactoriesUpdater.accept(parallelNode.id(), parallelNode.actionFactory());
			edgesUpdater.accept(sourceNodeId, new EdgeValue(parallelNode.id()));
			edgesUpdater.accept(parallelNode.id(), new EdgeValue(convergenceNodeId));
		}
	}

	/**
	 * Finds the convergence node where all parallel paths from the given start nodes meet.
	 * Uses BFS to traverse from each start node until finding a common target.
	 * This method handles nested parallel edges by recursively processing them.
	 *
	 * @param startNodeIds the set of starting node IDs for parallel branches
	 * @return the convergence node ID, or null if no convergence found
	 */
	private String findConvergenceNode(Set<String> startNodeIds) {
		if (startNodeIds.isEmpty()) {
			return null;
		}
		if (startNodeIds.size() == 1) {
			// Single path, find its end
			String startNode = startNodeIds.iterator().next();
			return findPathEnd(startNode);
		}

		// Use BFS to find the first common node reachable from all start nodes
		// Track which start nodes can reach each node
		Map<String, Set<String>> reachableFrom = new HashMap<>();
		Deque<String> queue = new ArrayDeque<>();

		// Initialize: mark each start node as reachable from itself
		for (String startNode : startNodeIds) {
			reachableFrom.put(startNode, new HashSet<>(Set.of(startNode)));
			if (!queue.offer(startNode)) {
				throw new IllegalStateException("Failed to add start node to queue: " + startNode);
			}
		}

		while (!queue.isEmpty()) {
			String current = queue.poll();
			Set<String> sources = reachableFrom.get(current);
			if (sources == null) {
				continue;
			}

			// Get next nodes from current node
			Optional<Edge> edgeOpt = processedData.edges().edgeBySourceId(current);
			if (edgeOpt.isEmpty()) {
				// No outgoing edge - this could be an end point
				// Check if all paths reach here
				if (sources.size() == startNodeIds.size()) {
					return current;
				}
				continue;
			}

			Edge edge = edgeOpt.get();

			// Handle parallel edges: recursively find convergence for nested parallelism
			if (edge.isParallel()) {
				Set<String> parallelTargets = edge.targets().stream()
						.map(EdgeValue::id)
						.filter(Objects::nonNull)
						.collect(Collectors.toSet());

				if (!parallelTargets.isEmpty()) {
					String nestedConvergence = findConvergenceNode(parallelTargets);
					if (nestedConvergence != null) {
						// Update reachability: all sources that reached current also reach nested convergence
						Set<String> nestedSources = reachableFrom.computeIfAbsent(nestedConvergence, k -> new HashSet<>());
						int sizeBefore = nestedSources.size();
						nestedSources.addAll(sources);
						boolean sourcesChanged = nestedSources.size() > sizeBefore;

						if (nestedSources.size() == startNodeIds.size()) {
							return nestedConvergence;
						}

						// Only queue if sources actually changed (prevents infinite loops in cycles)
						if (sourcesChanged && nestedSources.size() < startNodeIds.size()) {
							if (!queue.offer(nestedConvergence)) {
								throw new IllegalStateException("Failed to add nested convergence node to queue: " + nestedConvergence);
							}
						}
					}
				}
				continue;
			}

			EdgeValue target = edge.target();
			if (target.id() == null) {
				// Conditional edge - skip for now
				continue;
			}

			String nextNode = target.id();

			// Update reachability: all sources that reached current also reach nextNode
			Set<String> nextSources = reachableFrom.computeIfAbsent(nextNode, k -> new HashSet<>());
			int sizeBefore = nextSources.size();
			nextSources.addAll(sources);
			boolean sourcesChanged = nextSources.size() > sizeBefore;

			// If this node is reachable from all start nodes, it's the convergence point
			if (nextSources.size() == startNodeIds.size()) {
				return nextNode;
			}

			// Only queue if sources actually changed (prevents infinite loops in cycles)
			if (sourcesChanged && nextSources.size() < startNodeIds.size()) {
				if (!queue.offer(nextNode)) {
					throw new IllegalStateException("Failed to add next node to queue: " + nextNode);
				}
			}
		}

		return null;
	}

	/**
	 * Finds the end of a path starting from the given node.
	 * Follows edges until reaching a node with no outgoing edges or END.
	 */
	private String findPathEnd(String startNode) {
		String current = startNode;
		Set<String> visited = new HashSet<>();

		while (current != null && !visited.contains(current)) {
			visited.add(current);
			Optional<Edge> edgeOpt = processedData.edges().edgeBySourceId(current);
			if (edgeOpt.isEmpty()) {
				return current;
			}

			Edge edge = edgeOpt.get();
			if (edge.isParallel() || edge.target().id() == null) {
				return current;
			}

			current = edge.target().id();
		}

		return current;
	}

	/**
	 * Finds all edges from a start node to the convergence node.
	 * The result is a list of edge pairs: [source1, target1, source2, target2, ...]
	 * This format allows proper restoration of parallel edges when creating subgraphs,
	 * because calling addEdge(source, target) for each pair will automatically merge
	 * parallel edges (just like StateGraph.addEdge does).
	 *
	 * @param startNodeId the starting node ID
	 * @param convergenceNodeId the convergence node ID
	 * @return list of edge pairs [src1, tgt1, src2, tgt2, ...], or empty if path not found
	 */
	private List<String> findPathToConvergence(String startNodeId, String convergenceNodeId) {
		if (Objects.equals(startNodeId, convergenceNodeId)) {
			// Same node: return empty list (no edges needed)
			return List.of();
		}

		// Collect all edges from startNodeId to convergenceNodeId using DFS
		List<String> edgePairs = new ArrayList<>();
		Set<String> visitedNodes = new HashSet<>();  // Track visited nodes to avoid cycles
		
		collectEdgesToConvergence(startNodeId, convergenceNodeId, edgePairs, visitedNodes);
		
		return edgePairs;
	}

	/**
	 * Recursively collects all edges from current node to convergence node.
	 * Handles nested parallel edges by collecting all branches.
	 * Uses DFS to find a path, but for parallel edges collects all branches.
	 */
	private void collectEdgesToConvergence(String currentNode, String convergenceNodeId,
			List<String> edgePairs, Set<String> visitedNodes) {
		if (Objects.equals(currentNode, convergenceNodeId)) {
			return;  // Reached convergence, stop
		}

		// Avoid cycles
		if (visitedNodes.contains(currentNode)) {
			return;
		}
		visitedNodes.add(currentNode);

		Optional<Edge> edgeOpt = processedData.edges().edgeBySourceId(currentNode);
		if (edgeOpt.isEmpty()) {
			return;
		}

		Edge edge = edgeOpt.get();

		if (edge.isParallel()) {
			// Parallel edge: collect all branches (needed to restore parallel structure in subgraph)
			for (EdgeValue target : edge.targets()) {
				if (target.id() == null) {
					continue;
				}
				
				// Add edge pair: [source, target]
				edgePairs.add(currentNode);
				edgePairs.add(target.id());
				
				// Recursively collect from target (use new visited set for each branch to allow parallel paths)
				Set<String> branchVisited = new HashSet<>(visitedNodes);
				collectEdgesToConvergence(target.id(), convergenceNodeId, edgePairs, branchVisited);
			}
		} else {
			// Simple edge: follow single path
			EdgeValue target = edge.target();
			if (target.id() == null) {
				return;  // Conditional edge, skip
			}
			
			// Add edge pair: [source, target]
			edgePairs.add(currentNode);
			edgePairs.add(target.id());
			
			// Recursively collect from target
			collectEdgesToConvergence(target.id(), convergenceNodeId, edgePairs, visitedNodes);
		}
	}

	/**
	 * Creates a subgraph for a single parallel path.
	 * The subgraph starts from START, contains the path's start node, and ends at END.
	 * 
	 * The path.nodes contains edge pairs [src1, tgt1, src2, tgt2, ...] that represent
	 * all edges from the path's start node to its convergence node.
	 * By iterating through edge pairs and calling addEdge(src, tgt), parallel edges
	 * are automatically restored (because addEdge merges edges with the same source).
	 * 
	 * When the subgraph is compiled, any nested parallel edges will be automatically
	 * handled by the ParallelEdgeProcessor during compilation.
	 * 
	 * @param path the parallel path containing edge pairs
	 * @return a StateGraph representing the subgraph (will be compiled into CompiledGraph)
	 */
	private StateGraph createSubgraphForPath(ParallelPath path) throws GraphStateException {
		StateGraph subGraph = new StateGraph(stateGraph.getKeyStrategyFactory(), stateGraph.getStateSerializer());

		// Collect all unique nodes from the path, but exclude convergenceNodeId
		// Convergence node should not be in subgraph - it's the merge point in the main graph
		Set<String> allNodes = path.uniqueNodes();
		allNodes.add(path.startNodeId);
		// Explicitly remove convergenceNodeId - it belongs to the main graph, not the subgraph
		allNodes.remove(path.convergenceNodeId);

		// Add all nodes to subgraph by copying from original nodes
		for (String nodeId : allNodes) {
			if (nodeFactories.containsKey(nodeId)) {
				Node originalNode = processedData.nodes().elements.stream()
						.filter(n -> Objects.equals(n.id(), nodeId))
						.findFirst()
						.orElse(null);
				if (originalNode != null) {
					// Create a new node with the same ID and action factory
					Node newNode = new Node(originalNode.id(), originalNode.actionFactory());
					subGraph.addNode(nodeId, newNode);
				}
			}
		}

		// Add edges from edge pairs: [src1, tgt1, src2, tgt2, ...]
		// This properly restores parallel edges because addEdge merges edges with the same source
		// Important: If target is convergenceNodeId, redirect it to END (convergence is in main graph)
		// Track which nodes already have edges to END to avoid duplicates
		Set<String> nodesWithEndEdge = new HashSet<>();
		List<String> edgePairs = path.nodes;
		for (int i = 0; i < edgePairs.size() - 1; i += 2) {
			String fromNode = edgePairs.get(i);
			String toNode = edgePairs.get(i + 1);
			
			// If target is convergence node, redirect to END (convergence happens in main graph)
			if (Objects.equals(toNode, path.convergenceNodeId)) {
				// Only add END edge once per source node to avoid duplicate targets
				if (!nodesWithEndEdge.contains(fromNode)) {
					subGraph.addEdge(fromNode, END);
					nodesWithEndEdge.add(fromNode);
				}
			} else {
				subGraph.addEdge(fromNode, toNode);
			}
		}

		// Connect START to the path's start node
		subGraph.addEdge(START, path.startNodeId);

		// Note: We don't add edge from convergenceNodeId to END here because:
		// 1. Convergence node is not in the subgraph
		// 2. Any edge pointing to convergenceNodeId has already been redirected to END above

		return subGraph;
	}
}
