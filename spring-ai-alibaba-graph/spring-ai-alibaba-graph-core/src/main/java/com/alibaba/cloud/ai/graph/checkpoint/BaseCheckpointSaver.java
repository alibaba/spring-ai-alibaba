package com.alibaba.cloud.ai.graph.checkpoint;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.alibaba.cloud.ai.graph.RunnableConfig;

import static java.util.Optional.ofNullable;

public interface BaseCheckpointSaver {

	Collection<Checkpoint> list(RunnableConfig config);

	Optional<Checkpoint> get(RunnableConfig config);

	RunnableConfig put(RunnableConfig config, Checkpoint checkpoint) throws Exception;

	boolean clear(RunnableConfig config);

	default Optional<Checkpoint> getLast(LinkedList<Checkpoint> checkpoints, RunnableConfig config) {
		return (checkpoints == null || checkpoints.isEmpty()) ? Optional.empty() : ofNullable(checkpoints.peek());
	}

	default LinkedList<Checkpoint> getLinkedList(List<Checkpoint> checkpoints) {
		return Objects.nonNull(checkpoints) ? new LinkedList<>(checkpoints) : new LinkedList<>();
	}

}
