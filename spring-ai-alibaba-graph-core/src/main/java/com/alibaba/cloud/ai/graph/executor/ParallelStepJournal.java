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
package com.alibaba.cloud.ai.graph.executor;

import com.alibaba.cloud.ai.graph.GraphRunnerContext;
import com.alibaba.cloud.ai.graph.action.InterruptionMetadata;
import com.alibaba.cloud.ai.graph.action.MultiCommand;
import com.alibaba.cloud.ai.graph.action.Send;
import com.alibaba.cloud.ai.graph.checkpoint.CheckpointWrite;
import com.alibaba.cloud.ai.graph.checkpoint.InterruptionCheckpointCodec;
import com.alibaba.cloud.ai.graph.internal.node.ParallelNode;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.alibaba.cloud.ai.graph.checkpoint.CheckpointWrite.decode;
import static com.alibaba.cloud.ai.graph.checkpoint.CheckpointWrite.encode;

/**
 * Stores a parallel step's dispatch and completed writes in its predecessor checkpoint.
 */
final class ParallelStepJournal {

	private static final String DISPATCH = "parallelDispatch";

	private static final String ROUTING_UPDATE = "parallelRoutingUpdate";

	private static final String COMPLETED_TASKS = "completedTasks";

	private static final String INTERRUPTED_TASKS = "interruptedTasks";

	private static final String PAUSED_WRITES = "pausedTaskWrites";

	private static final String TASK_ATTEMPTS = "taskAttempts";

	private static final String WINNING_WRITES = "winningTaskWrites";

	private final GraphRunnerContext context;

	ParallelStepJournal(GraphRunnerContext context) {
		this.context = context;
	}

	void begin(ParallelNode.Execution execution) {
		if (context.stepCheckpointData().containsKey(DISPATCH)) {
			return;
		}
		context.updateStepCheckpoint(before -> {
			List<Map<String, Object>> dispatch = execution.branches().stream()
					.map(branch -> Map.<String, Object>of("node", branch.nodeId(), "input", encode(branch.input())))
					.toList();
			var after = new HashMap<>(before);
			after.put(DISPATCH, dispatch);
			after.put(ROUTING_UPDATE, encode(execution.routingUpdate()));
			return after;
		});
	}

	@SuppressWarnings("unchecked")
	Optional<MultiCommand> restoreDispatch() {
		var data = context.stepCheckpointData();
		var dispatch = (List<Map<String, Object>>) data.get(DISPATCH);
		if (dispatch == null || dispatch.isEmpty()) {
			return Optional.empty();
		}
		List<Send> sends = dispatch.stream()
				.map(task -> new Send((String) task.get("node"), decode((Map<String, Object>) task.get("input"))))
				.toList();
		return Optional.of(MultiCommand.dispatch(sends, decode((Map<String, Object>) data.get(ROUTING_UPDATE))));
	}

	void saveWrites(String taskId, List<Map<String, Object>> writes) {
		context.updateStepCheckpoint(before -> {
			var completed = new HashMap<>(completedTasks(before));
			completed.put(taskId, writes.stream().map(CheckpointWrite::encode).toList());
			var after = new HashMap<>(before);
			after.put(COMPLETED_TASKS, completed);
			var paused = new HashMap<>(interruptedTasks(before));
			paused.remove(taskId);
			after.put(INTERRUPTED_TASKS, paused);
			var pendingWrites = new HashMap<>(pausedWrites(before));
			pendingWrites.remove(taskId);
			after.put(PAUSED_WRITES, pendingWrites);
			return after;
		});
	}

	void saveInterruption(String taskId, InterruptionMetadata interruption, List<Map<String, Object>> writes) {
		context.updateStepCheckpoint(before -> {
			var paused = new HashMap<>(interruptedTasks(before));
			paused.put(taskId, InterruptionCheckpointCodec.encodePause(interruption));
			var after = new HashMap<>(before);
			after.put(INTERRUPTED_TASKS, paused);
			if (!writes.isEmpty()) {
				var pendingWrites = new HashMap<>(pausedWrites(before));
				pendingWrites.put(taskId, writes.stream().map(CheckpointWrite::encode).toList());
				after.put(PAUSED_WRITES, pendingWrites);
			}
			return after;
		});
	}

	Map<String, InterruptionMetadata> restoreInterruptions() {
		Map<String, InterruptionMetadata> restored = new HashMap<>();
		interruptedTasks(context.stepCheckpointData()).forEach(
				(taskId, data) -> restored.put(taskId, InterruptionCheckpointCodec.decodePause(data)));
		return restored;
	}

	Optional<List<Map<String, Object>>> restoreWrites(String taskId) {
		return Optional.ofNullable(completedTasks(context.stepCheckpointData()).get(taskId))
				.map(CheckpointWrite::decodeAll);
	}

	List<Map<String, Object>> restorePausedWrites(String taskId) {
		return CheckpointWrite.decodeAll(pausedWrites(context.stepCheckpointData()).get(taskId));
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Object> pausedWrites(Map<String, Object> data) {
		return (Map<String, Object>) data.getOrDefault(PAUSED_WRITES, Map.of());
	}

	Optional<List<Map<String, Object>>> restoreWinningWrites() {
		return Optional.ofNullable(context.stepCheckpointData().get(WINNING_WRITES)).map(CheckpointWrite::decodeAll);
	}

	void saveWinningWrites(List<Map<String, Object>> writes) {
		context.updateStepCheckpoint(before -> {
			var after = new HashMap<>(before);
			after.put(WINNING_WRITES, writes.stream().map(CheckpointWrite::encode).toList());
			return after;
		});
	}

	int attempts(String taskId) {
		return attempts(context.stepCheckpointData()).getOrDefault(taskId, 0).intValue();
	}

	void saveAttempt(String taskId, int attempt) {
		context.updateStepCheckpoint(before -> {
			var attempts = new HashMap<>(attempts(before));
			attempts.put(taskId, attempt);
			var after = new HashMap<>(before);
			after.put(TASK_ATTEMPTS, attempts);
			return after;
		});
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Number> attempts(Map<String, Object> data) {
		return (Map<String, Number>) data.getOrDefault(TASK_ATTEMPTS, Map.of());
	}

	@SuppressWarnings("unchecked")
	private static Map<String, List<Map<String, Object>>> completedTasks(Map<String, Object> data) {
		return (Map<String, List<Map<String, Object>>>) data.getOrDefault(COMPLETED_TASKS, Map.of());
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Map<String, Object>> interruptedTasks(Map<String, Object> data) {
		return (Map<String, Map<String, Object>>) data.getOrDefault(INTERRUPTED_TASKS, Map.of());
	}

}
