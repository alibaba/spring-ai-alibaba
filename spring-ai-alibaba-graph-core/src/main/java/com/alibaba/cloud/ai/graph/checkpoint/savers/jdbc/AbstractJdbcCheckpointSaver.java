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
package com.alibaba.cloud.ai.graph.checkpoint.savers.jdbc;

import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.checkpoint.BaseCheckpointSaver;
import com.alibaba.cloud.ai.graph.checkpoint.Checkpoint;
import com.alibaba.cloud.ai.graph.checkpoint.savers.common.LatestCheckpointCache;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Base implementation for JDBC checkpoint savers.
 * <p>
 * This class owns the common saver lifecycle and latest-checkpoint cache behavior.
 * Subclasses keep database-specific SQL, transaction details and row mapping logic.
 */
public abstract class AbstractJdbcCheckpointSaver implements BaseCheckpointSaver {

	private final LatestCheckpointCache latestCheckpointCache;

	private final ReentrantLock lock = new ReentrantLock();

	/**
	 * Creates a JDBC saver base with a bounded latest-checkpoint cache.
	 *
	 * @param maxCachedThreads maximum number of thread latest-checkpoint entries to
	 * cache, or 0 to disable the cache
	 */
	protected AbstractJdbcCheckpointSaver(int maxCachedThreads) {
		this.latestCheckpointCache = new LatestCheckpointCache(maxCachedThreads);
	}

	/**
	 * Lists active checkpoints for a thread and refreshes the latest-checkpoint cache
	 * from the first item in the returned history.
	 *
	 * @param config runnable config that identifies the target thread
	 * @return immutable active checkpoint history ordered by the concrete saver
	 */
	@Override
	public final Collection<Checkpoint> list(RunnableConfig config) {
		lock.lock();
		try {
			String threadId = threadId(config);
			LinkedList<Checkpoint> checkpoints = selectCheckpoints(threadId);
			if (!checkpoints.isEmpty()) {
				latestCheckpointCache.put(threadId, checkpoints.peek());
			}
			return Collections.unmodifiableCollection(checkpoints);
		}
		catch (Exception ex) {
			throw new RuntimeException(ex);
		}
		finally {
			lock.unlock();
		}
	}

	/**
	 * Gets a checkpoint by explicit checkpoint id, or returns the latest checkpoint
	 * using the bounded cache before falling back to the backing database.
	 *
	 * @param config runnable config that identifies the thread and optional checkpoint
	 * id
	 * @return matching checkpoint when it exists
	 */
	@Override
	public final Optional<Checkpoint> get(RunnableConfig config) {
		lock.lock();
		try {
			String threadId = threadId(config);
			if (config.checkPointId().isPresent()) {
				return selectCheckpointById(threadId, config.checkPointId().get());
			}

			Optional<Checkpoint> cached = latestCheckpointCache.get(threadId);
			if (cached.isPresent()) {
				return cached;
			}

			Optional<Checkpoint> latest = selectLatestCheckpoint(threadId);
			latest.ifPresent(checkpoint -> latestCheckpointCache.put(threadId, checkpoint));
			return latest;
		}
		catch (Exception ex) {
			throw new RuntimeException(ex);
		}
		finally {
			lock.unlock();
		}
	}

	/**
	 * Inserts a new checkpoint when no checkpoint id is present, otherwise updates
	 * the configured checkpoint and keeps the cached latest entry coherent.
	 *
	 * @param config runnable config that identifies the thread and optional checkpoint
	 * id
	 * @param checkpoint checkpoint data to insert or use as the replacement value
	 * @return config with the inserted checkpoint id, or the original config for
	 * updates
	 * @throws Exception when the concrete saver cannot persist the checkpoint
	 */
	@Override
	public final RunnableConfig put(RunnableConfig config, Checkpoint checkpoint) throws Exception {
		lock.lock();
		try {
			String threadId = threadId(config);
			if (config.checkPointId().isPresent()) {
				String checkpointId = config.checkPointId().get();
				updateCheckpoint(threadId, checkpointId, checkpoint);
				deleteRetainedCheckpoints(threadId, config);
				latestCheckpointCache.get(threadId)
						.filter(latest -> latest.getId().equals(checkpointId))
						.ifPresent(latest -> latestCheckpointCache.put(threadId, checkpoint));
				return config;
			}

			insertCheckpoint(threadId, checkpoint);
			deleteRetainedCheckpoints(threadId, config);
			latestCheckpointCache.put(threadId, checkpoint);
			return RunnableConfig.builder(config)
					.checkPointId(checkpoint.getId())
					.build();
		}
		finally {
			lock.unlock();
		}
	}

	/**
	 * Releases the active thread after loading the released checkpoint history, then
	 * removes the thread from the latest-checkpoint cache.
	 *
	 * @param config runnable config that identifies the target thread
	 * @return released thread id and its checkpoint history
	 * @throws Exception when the concrete saver cannot release the thread
	 */
	@Override
	public final Tag release(RunnableConfig config) throws Exception {
		lock.lock();
		try {
			String threadId = threadId(config);
			LinkedList<Checkpoint> checkpoints = selectCheckpoints(threadId);
			releaseThread(threadId);
			latestCheckpointCache.remove(threadId);
			return new Tag(threadId, checkpoints);
		}
		finally {
			lock.unlock();
		}
	}

	/**
	 * Selects the active checkpoint history for a thread from the backing database.
	 *
	 * @param threadId thread name/id used by the concrete saver schema
	 * @return checkpoint history in latest-first order
	 * @throws Exception when the concrete saver cannot read checkpoint history
	 */
	protected abstract LinkedList<Checkpoint> selectCheckpoints(String threadId) throws Exception;

	/**
	 * Selects only the latest active checkpoint for a thread.
	 *
	 * @param threadId thread name/id used by the concrete saver schema
	 * @return latest checkpoint when one exists
	 * @throws Exception when the concrete saver cannot read the latest checkpoint
	 */
	protected abstract Optional<Checkpoint> selectLatestCheckpoint(String threadId) throws Exception;

	/**
	 * Selects an active checkpoint by id for a thread.
	 *
	 * @param threadId thread name/id used by the concrete saver schema
	 * @param checkpointId checkpoint id to look up
	 * @return matching checkpoint when one exists
	 * @throws Exception when the concrete saver cannot read the checkpoint
	 */
	protected abstract Optional<Checkpoint> selectCheckpointById(String threadId, String checkpointId) throws Exception;

	/**
	 * Inserts a new active checkpoint for a thread.
	 *
	 * @param threadId thread name/id used by the concrete saver schema
	 * @param checkpoint checkpoint to persist
	 * @throws Exception when the concrete saver cannot insert the checkpoint
	 */
	protected abstract void insertCheckpoint(String threadId, Checkpoint checkpoint) throws Exception;

	/**
	 * Replaces an existing active checkpoint for a thread.
	 *
	 * @param threadId thread name/id used by the concrete saver schema
	 * @param checkpointId checkpoint id to replace
	 * @param checkpoint replacement checkpoint data
	 * @throws Exception when the concrete saver cannot update the checkpoint
	 */
	protected abstract void updateCheckpoint(String threadId, String checkpointId, Checkpoint checkpoint) throws Exception;

	/**
	 * Deletes active checkpoints by id for a thread.
	 *
	 * @param threadId thread name/id used by the concrete saver schema
	 * @param checkpointIds checkpoint ids to delete
	 * @throws Exception when the concrete saver cannot delete checkpoints
	 */
	protected abstract void deleteCheckpoints(String threadId, Collection<String> checkpointIds) throws Exception;

	private void deleteRetainedCheckpoints(String threadId, RunnableConfig config) throws Exception {
		Optional<Integer> retained = checkpointsNumRetained(config);
		if (retained.isEmpty()) {
			return;
		}
		LinkedList<Checkpoint> checkpoints = selectCheckpoints(threadId);
		if (checkpoints.size() <= retained.get()) {
			return;
		}
		List<String> checkpointIds = checkpoints.stream()
				.skip(retained.get())
				.map(Checkpoint::getId)
				.toList();
		deleteCheckpoints(threadId, checkpointIds);
	}

	/**
	 * Marks the active thread as released in the backing database.
	 *
	 * @param threadId thread name/id used by the concrete saver schema
	 * @throws Exception when the concrete saver cannot release the thread
	 */
	protected abstract void releaseThread(String threadId) throws Exception;

	/**
	 * Resolves the configured thread id, falling back to the default saver thread.
	 *
	 * @param config runnable config supplied by graph execution
	 * @return configured thread id or {@link BaseCheckpointSaver#THREAD_ID_DEFAULT}
	 */
	private String threadId(RunnableConfig config) {
		return config.threadId().orElse(THREAD_ID_DEFAULT);
	}

}
