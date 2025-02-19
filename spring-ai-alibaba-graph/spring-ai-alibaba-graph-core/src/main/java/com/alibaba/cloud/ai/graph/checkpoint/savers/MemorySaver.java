package com.alibaba.cloud.ai.graph.checkpoint.savers;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.IntStream;

import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.checkpoint.BaseCheckpointSaver;
import com.alibaba.cloud.ai.graph.checkpoint.Checkpoint;

import static java.lang.String.format;
import static java.util.Collections.unmodifiableCollection;

public class MemorySaver implements BaseCheckpointSaver {

	private final Map<String, LinkedList<Checkpoint>> _checkpointsByThread = new HashMap<>();

	private final LinkedList<Checkpoint> _defaultCheckpoints = new LinkedList<>();

	private final ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();

	private final Lock r = rwl.readLock();

	private final Lock w = rwl.writeLock();

	public MemorySaver() {
	}

	protected LinkedList<Checkpoint> getCheckpoints(RunnableConfig config) {
		return config.threadId()
			.map(threadId -> _checkpointsByThread.computeIfAbsent(threadId, k -> new LinkedList<>()))
			.orElse(_defaultCheckpoints);
	}

	@Override
	public Collection<Checkpoint> list(RunnableConfig config) {
		final LinkedList<Checkpoint> checkpoints = getCheckpoints(config);
		r.lock();
		try {
			return unmodifiableCollection(checkpoints); // immutable checkpoints;
		}
		finally {
			r.unlock();
		}
	}

	@Override
	public Optional<Checkpoint> get(RunnableConfig config) {
		final LinkedList<Checkpoint> checkpoints = getCheckpoints(config);
		r.lock();
		try {
			if (config.checkPointId().isPresent()) {
				return config.checkPointId()
					.flatMap(
							id -> checkpoints.stream().filter(checkpoint -> checkpoint.getId().equals(id)).findFirst());
			}
			return getLast(checkpoints, config);
		}
		finally {
			r.unlock();
		}
	}

	@Override
	public RunnableConfig put(RunnableConfig config, Checkpoint checkpoint) throws Exception {
		final LinkedList<Checkpoint> checkpoints = getCheckpoints(config);

		w.lock();
		try {

			if (config.checkPointId().isPresent()) { // Replace Checkpoint
				String checkPointId = config.checkPointId().get();
				int index = IntStream.range(0, checkpoints.size())
					.filter(i -> checkpoints.get(i).getId().equals(checkPointId))
					.findFirst()
					.orElseThrow(() -> (new NoSuchElementException(
							format("Checkpoint with id %s not found!", checkPointId))));
				checkpoints.set(index, checkpoint);
				return config;
			}

			checkpoints.push(checkpoint); // Add Checkpoint

			return RunnableConfig.builder(config).checkPointId(checkpoint.getId()).build();
		}
		finally {
			w.unlock();
		}
	}

	@Override
	public Optional<Checkpoint> getLast(LinkedList<Checkpoint> checkpoints, RunnableConfig config) {
		return BaseCheckpointSaver.super.getLast(checkpoints, config);
	}

	@Override
	public boolean clear(RunnableConfig config) {
		return false;
	}

}
