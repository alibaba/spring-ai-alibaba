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

import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.internal.edge.Edge;
import com.alibaba.cloud.ai.graph.internal.edge.EdgeValue;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import static com.alibaba.cloud.ai.graph.StateGraph.END;

/** Determines the boundaries of each fork without changing the original graph. */
public final class ParallelEdgeProcessor {

	private final Map<String, Edge> edges = new HashMap<>();

	private final Map<String, Set<String>> successors = new HashMap<>();

	private final Map<String, Set<String>> predecessors = new HashMap<>();

	public ParallelEdgeProcessor(StateGraph.Edges graphEdges) {
		for (Edge edge : graphEdges.elements) {
			edges.put(edge.sourceId(), edge);
			Set<String> targets = new LinkedHashSet<>();
			for (EdgeValue target : edge.targets()) {
				if (target.id() == null) {
					targets.addAll(target.value().mappings().values());
				}
				else {
					targets.add(target.id());
				}
			}
			successors.put(edge.sourceId(), targets);
			targets.forEach(target ->
					predecessors.computeIfAbsent(target, ignored -> new LinkedHashSet<>()).add(edge.sourceId()));
		}
	}

	public Plan plan(String fork, Collection<String> branches, String declaredJoin) throws GraphStateException {
		if (declaredJoin != null) {
			if (!isValidJoin(fork, branches, declaredJoin)) {
				throw new GraphStateException("Parallel join '%s' for '%s' must bound every branch before they overlap or end"
						.formatted(declaredJoin, fork));
			}
			return new Plan(declaredJoin, directBranches(branches, declaredJoin));
		}

		var distances = branches.stream().map(branch -> distances(branch, successors)).toList();
		var shared = new HashSet<>(distances.get(0).keySet());
		distances.forEach(reachable -> shared.retainAll(reachable.keySet()));
		String join = shared.stream().sorted(Comparator
				.<String>comparingInt(node -> distances.stream().mapToInt(distance -> distance.get(node)).max().orElseThrow())
				.thenComparingInt(node -> distances.stream().mapToInt(distance -> distance.get(node)).sum())
				.thenComparing(Comparator.naturalOrder()))
				.filter(candidate -> isValidJoin(fork, branches, candidate))
				.findFirst()
				.orElseThrow(() -> new GraphStateException("No shared parallel join for '%s' with branches %s"
						.formatted(fork, branches)));
		return new Plan(join, directBranches(branches, join));
	}

	private static Map<String, Integer> distances(String start, Map<String, Set<String>> adjacency) {
		var visited = new HashMap<String, Integer>();
		var remaining = new ArrayDeque<String>();
		visited.put(start, 0);
		remaining.add(start);
		while (!remaining.isEmpty()) {
			String node = remaining.remove();
			int nextDistance = visited.get(node) + 1;
			for (String successor : adjacency.getOrDefault(node, Set.of())) {
				if (visited.putIfAbsent(successor, nextDistance) == null) {
					remaining.add(successor);
				}
			}
		}
		return visited;
	}

	private boolean isValidJoin(String fork, Collection<String> branches, String join) {
		if (fork.equals(join)) {
			return false;
		}
		Set<String> canReachJoin = distances(join, predecessors).keySet();
		var visited = new HashSet<String>();
		for (String branch : branches) {
			if (branch.equals(join)) {
				continue;
			}
			var remaining = new ArrayDeque<String>();
			var inBranch = new HashSet<String>();
			remaining.add(branch);
			inBranch.add(branch);
			while (!remaining.isEmpty()) {
				String node = remaining.remove();
				if (END.equals(node) || fork.equals(node) || !canReachJoin.contains(node) || !visited.add(node)) {
					return false;
				}
				for (String successor : successors.getOrDefault(node, Set.of())) {
					if (!join.equals(successor) && inBranch.add(successor)) {
						remaining.add(successor);
					}
				}
			}
		}
		return true;
	}

	private Set<String> directBranches(Collection<String> branches, String join) {
		var direct = new HashSet<String>();
		for (String branch : branches) {
			Edge edge = edges.get(branch);
			if (edge != null && !edge.isParallel() && join.equals(edge.target().id())) {
				direct.add(branch);
			}
		}
		return Set.copyOf(direct);
	}

	public record Plan(String join, Set<String> directBranches) {
	}

}
