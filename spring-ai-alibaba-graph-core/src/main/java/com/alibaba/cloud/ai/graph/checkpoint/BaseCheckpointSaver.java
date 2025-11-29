package com.alibaba.cloud.ai.graph.checkpoint;

import com.alibaba.cloud.ai.graph.RunnableConfig;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import static java.util.Optional.ofNullable;

public interface BaseCheckpointSaver {
	String THREAD_ID_DEFAULT = "$default";

	default Optional<Checkpoint> getLast(LinkedList<Checkpoint> checkpoints, RunnableConfig config) {
		return (checkpoints.isEmpty()) ? Optional.empty() : ofNullable(checkpoints.peek());
	}

	Collection<Checkpoint> list(RunnableConfig config);

	Optional<Checkpoint> get(RunnableConfig config);

	RunnableConfig put(RunnableConfig config, Checkpoint checkpoint) throws Exception;

	Tag release(RunnableConfig config) throws Exception;

	record Tag(String threadId, Collection<Checkpoint> checkpoints) {
		public Tag(String threadId, Collection<Checkpoint> checkpoints) {
			this.threadId = threadId;
			this.checkpoints = ofNullable(checkpoints).map(List::copyOf).orElseGet(List::of);
		}
	}

}
