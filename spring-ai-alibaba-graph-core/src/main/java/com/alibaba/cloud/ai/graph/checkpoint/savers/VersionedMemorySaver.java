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
import com.alibaba.cloud.ai.graph.checkpoint.HasVersions;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.locks.ReentrantLock;

import static java.lang.String.format;
import static java.util.Optional.ofNullable;

/**
 * VersionedMemorySaver is a class that implements {@link BaseCheckpointSaver} and
 * {@link HasVersions}. It provides methods to save checkpoints with versioning and
 * retrieve them based on thread IDs and versions. Experimental feature
 */
public class VersionedMemorySaver implements BaseCheckpointSaver, HasVersions {

	final Map<String, TreeMap<Integer, Tag>> _checkpointsHistoryByThread = new HashMap<>();

	final MemorySaver noVersionSaver = new MemorySaver();

	private final ReentrantLock _lock = new ReentrantLock();

	/**
	 * Default constructor for the {@link VersionedMemorySaver} class. Initializes a new
	 * instance of the class with default settings.
	 */
	public VersionedMemorySaver() {
	}

	/**
	 * Retrieves the checkpoint history for a specific thread.
	 * @param threadId The ID of the thread whose checkpoint history is to be retrieved.
	 * @return An {@link Optional} containing the {@link TreeMap<Integer, Tag>}
	 * representing the checkpoint history if the thread exists; otherwise, an empty
	 * {@code Optional}.
	 */
	private Optional<TreeMap<Integer, Tag>> getCheckpointHistoryByThread(String threadId) {
		return ofNullable(_checkpointsHistoryByThread.get(threadId));
		// .orElseThrow( () -> new IllegalArgumentException( format("Thread %s not found",
		// threadId )) );
	}

	/**
	 * Retrieves an optional tag based on the provided version.
	 * @param checkpointsHistory the map containing historical tags indexed by versions
	 * @param threadVersion the version to retrieve the tag for
	 * @return an {@link Optional} containing the tag associated with the given version,
	 * or an empty optional if not found
	 */
	final Optional<Tag> getTagByVersion(TreeMap<Integer, Tag> checkpointsHistory, int threadVersion) {
		_lock.lock();
		try {
			return ofNullable(checkpointsHistory.get(threadVersion));

		}
		finally {
			_lock.unlock();
		}

	}

	/**
	 * Retrieves the checkpoints for a specific version of a thread.
	 * @param threadId the ID of the thread
	 * @param threadVersion the version of the thread
	 * @return a collection of checkpoints for the specified thread version
	 * @throws IllegalArgumentException if the version is not found for the given thread
	 */
	final Collection<Checkpoint> getCheckpointsByVersion(String threadId, int threadVersion) {

		_lock.lock();
		try {
			return getCheckpointHistoryByThread(threadId).map(history -> history.get(threadVersion))
				.map(Tag::checkpoints)
				.orElseThrow(() -> new IllegalArgumentException(
						format("Version %s for thread %s not found", threadVersion, threadId)));

		}
		finally {
			_lock.unlock();
		}
	}

	/**
	 * Returns a collection of versions associated with the specified thread ID.
	 * @param threadId the ID of the thread to retrieve versions for; if {@code null},
	 * uses a default value
	 * @return a collection of versions, or an empty collection if no versions are found
	 */
	@Override
	public Collection<Integer> versionsByThreadId(String threadId) {
		return getCheckpointHistoryByThread(ofNullable(threadId).orElse(THREAD_ID_DEFAULT))
			.map(history -> (Collection<Integer>) history.keySet())
			.orElse(Collections.emptyList());
	}

	/**
	 * Retrieves the last version by thread ID.
	 * @param threadId the ID of the thread to retrieve the last version for, or
	 * {@code null} if not specified
	 * @return an {@link Optional<Integer>} containing the last version number, or an
	 * empty(Optional) if no versions are found
	 */
	@Override
	public Optional<Integer> lastVersionByThreadId(String threadId) {
		return getCheckpointHistoryByThread(ofNullable(threadId).orElse(THREAD_ID_DEFAULT)).map(TreeMap::lastKey);
	}

	/**
	 * Lists checkpoints based on the provided configuration.
	 * @param config The {@link RunnableConfig} object containing configuration details.
	 * @return A collection of {@link Checkpoint} objects.
	 * @throws RuntimeException If an error occurs during the listing process.
	 */
	@Override
	public Collection<Checkpoint> list(RunnableConfig config) {
		_lock.lock();
		try {
			return noVersionSaver.list(config);
		}
		finally {
			_lock.unlock();
		}
	}

	/**
	 * Retrieves an optional checkpoint for the given configuration.
	 * @param config The configuration to retrieve the checkpoint for, not null.
	 * @return Optional containing the checkpoint if found, or empty if not found.
	 */
	@Override
	public Optional<Checkpoint> get(RunnableConfig config) {

		_lock.lock();
		try {

			return noVersionSaver.get(config);

		}
		finally {
			_lock.unlock();
		}
	}

	/**
	 * Updates or inserts the given {@code RunnableConfig} with the specified
	 * {@link Checkpoint}.
	 * @param config the {@code RunnableConfig} to be updated or inserted
	 * @param checkpoint the {@link Checkpoint} associated with the {@code RunnableConfig}
	 * @return the previous {@code RunnableConfig} if present, otherwise null
	 * @throws Exception if an error occurs during the update or insertion
	 */
	@Override
	public RunnableConfig put(RunnableConfig config, Checkpoint checkpoint) throws Exception {

		_lock.lock();
		try {
			return noVersionSaver.put(config, checkpoint);
		}
		finally {
			_lock.unlock();
		}
	}

	@Override
	public boolean clear(RunnableConfig config) {
		return false;
	}

	/**
	 * Releases a {@link Tag} based on the provided {@link RunnableConfig}.
	 * @param config The configuration for the release operation.
	 * @return A {@link Tag} representing the released tag.
	 * @throws Exception If an error occurs during the release process.
	 */
	@Override
	public Tag release(RunnableConfig config) throws Exception {

		_lock.lock();
		try {

			var threadId = config.threadId().orElse(THREAD_ID_DEFAULT);

			var tag = noVersionSaver.release(config);

			var checkpointsHistory = _checkpointsHistoryByThread.computeIfAbsent(threadId, k -> new TreeMap<>());

			var threadVersion = ofNullable(checkpointsHistory.lastEntry()).map(Map.Entry::getKey).orElse(0);

			checkpointsHistory.put(threadVersion + 1, tag);

			return tag;

		}
		finally {
			_lock.unlock();
		}
	}

}
