/*
 * Copyright 2024-2025 the original author or authors.
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
package com.alibaba.cloud.ai.graph.checkpoint.savers;

import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.checkpoint.BaseCheckpointSaver;
import com.alibaba.cloud.ai.graph.checkpoint.Checkpoint;
import com.alibaba.cloud.ai.graph.utils.TryFunction;

import java.util.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.IntStream;

import static java.lang.String.format;

public class MemorySaver implements BaseCheckpointSaver {

	final Map<String, LinkedList<Checkpoint>> _checkpointsByThread = new HashMap<>();

	private final ReentrantLock _lock = new ReentrantLock();

	public MemorySaver() {
	}

	protected LinkedList<Checkpoint> loadedCheckpoints(RunnableConfig config, LinkedList<Checkpoint> checkpoints)
			throws Exception {
		return checkpoints;
	}

	protected void insertedCheckpoint(RunnableConfig config, LinkedList<Checkpoint> checkpoints, Checkpoint checkpoint)
			throws Exception {
	}

	protected void updatedCheckpoint(RunnableConfig config, LinkedList<Checkpoint> checkpoints, Checkpoint checkpoint)
			throws Exception {
	}

	protected void releasedCheckpoints(RunnableConfig config, LinkedList<Checkpoint> checkpoints, Tag releaseTag)
			throws Exception {
	}

	protected final <T> T loadOrInitCheckpoints(RunnableConfig config,
			TryFunction<LinkedList<Checkpoint>, T, Exception> transformer) throws Exception {
		_lock.lock();
		try {
			var threadId = config.threadId().orElse(THREAD_ID_DEFAULT);
			return transformer.tryApply(
					loadedCheckpoints(config, _checkpointsByThread.computeIfAbsent(threadId, k -> new LinkedList<>())));

		}
		finally {
			_lock.unlock();
		}
	}

	public Map<String, LinkedList<Checkpoint>> get_checkpointsByThread() {
		return _checkpointsByThread;
	}

	@Override
	public boolean clear(RunnableConfig config) {
		_lock.lock();
		try {
			var threadId = config.threadId().orElse(THREAD_ID_DEFAULT);
			LinkedList<Checkpoint> checkpoints = _checkpointsByThread.get(threadId);
			if (checkpoints != null) {
				checkpoints.clear();
				return true;
			}
			return false;
		}
		finally {
			_lock.unlock();
		}
	}

	protected final Collection<Checkpoint> remove(String threadId) {
		return _checkpointsByThread.remove(Objects.requireNonNull(threadId));
	}

	@Override
	public final Collection<Checkpoint> list(RunnableConfig config) {
		try {
			return loadOrInitCheckpoints(config, Collections::unmodifiableCollection);
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public final Optional<Checkpoint> get(RunnableConfig config) {

		try {
			return loadOrInitCheckpoints(config, checkpoints -> {
				if (config.checkPointId().isPresent()) {
					return config.checkPointId()
						.flatMap(id -> checkpoints.stream()
							.filter(checkpoint -> checkpoint.getId().equals(id))
							.findFirst());
				}
				return getLast(checkpoints, config);

			});
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public final RunnableConfig put(RunnableConfig config, Checkpoint checkpoint) throws Exception {

		return loadOrInitCheckpoints(config, checkpoints -> {

			if (config.checkPointId().isPresent()) { // Replace Checkpoint
				String checkPointId = config.checkPointId().get();
				int index = IntStream.range(0, checkpoints.size())
					.filter(i -> checkpoints.get(i).getId().equals(checkPointId))
					.findFirst()
					.orElseThrow(() -> (new NoSuchElementException(
							format("Checkpoint with id %s not found!", checkPointId))));
				checkpoints.set(index, checkpoint);
				updatedCheckpoint(config, checkpoints, checkpoint);
				return config;
			}

			checkpoints.push(checkpoint); // Add Checkpoint
			insertedCheckpoint(config, checkpoints, checkpoint);

			return RunnableConfig.builder(config).checkPointId(checkpoint.getId()).build();

		});
	}

	@Override
	public final Tag release(RunnableConfig config) throws Exception {

		return loadOrInitCheckpoints(config, checkpoints -> {

			var threadId = config.threadId().orElse(THREAD_ID_DEFAULT);

			var tag = new Tag(threadId, remove(threadId));

			releasedCheckpoints(config, checkpoints, tag);

			return tag;
		});
	}

}
