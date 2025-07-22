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

import java.util.Collection;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.IntStream;

import static java.lang.String.format;
import static java.util.Collections.unmodifiableCollection;
import static java.util.Optional.ofNullable;

public class MemorySaver implements BaseCheckpointSaver {

	final ConcurrentHashMap<String, LinkedList<Checkpoint>> _checkpointsByThread = new ConcurrentHashMap<>();

	// 线程id和锁的映射
	final ConcurrentHashMap<String, ReentrantLock> _locksByThread = new ConcurrentHashMap<>();

	public MemorySaver() {
	}

	private Lock getLock(String threadId) {
		return _locksByThread.computeIfAbsent(threadId, k -> new ReentrantLock());
	}

	final LinkedList<Checkpoint> getCheckpoints(RunnableConfig config) {
		var threadId = config.threadId().orElse(THREAD_ID_DEFAULT);
		return _checkpointsByThread.computeIfAbsent(threadId, k -> new LinkedList<>());
	}

	public final Optional<Checkpoint> getLast(LinkedList<Checkpoint> checkpoints, RunnableConfig config) {
		return (checkpoints.isEmpty()) ? Optional.empty() : ofNullable(checkpoints.peek());
	}

	@Override
	public Collection<Checkpoint> list(RunnableConfig config) {
		var threadId = config.threadId().orElse(THREAD_ID_DEFAULT);
		Lock lock = getLock(threadId);
		lock.lock();
		try {
			final LinkedList<Checkpoint> checkpoints = getCheckpoints(config);
			return unmodifiableCollection(new LinkedList<>(checkpoints)); // 返回快照，防止并发修改
		}
		finally {
			lock.unlock();
		}
	}

	@Override
	public Optional<Checkpoint> get(RunnableConfig config) {
		var threadId = config.threadId().orElse(THREAD_ID_DEFAULT);
		Lock lock = getLock(threadId);
		lock.lock();
		try {
			final LinkedList<Checkpoint> checkpoints = getCheckpoints(config);
			if (config.checkPointId().isPresent()) {
				return config.checkPointId()
					.flatMap(
							id -> checkpoints.stream().filter(checkpoint -> checkpoint.getId().equals(id)).findFirst());
			}
			return getLast(checkpoints, config);
		}
		finally {
			lock.unlock();
		}
	}

	@Override
	public RunnableConfig put(RunnableConfig config, Checkpoint checkpoint) throws Exception {
		var threadId = config.threadId().orElse(THREAD_ID_DEFAULT);
		Lock lock = getLock(threadId);
		lock.lock();
		try {
			final LinkedList<Checkpoint> checkpoints = getCheckpoints(config);
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
			lock.unlock();
		}
	}

	@Override
	public boolean clear(RunnableConfig config) {
		var threadId = config.threadId().orElse(THREAD_ID_DEFAULT);
		Lock lock = getLock(threadId);
		lock.lock();
		try {
			LinkedList<Checkpoint> checkpoints = _checkpointsByThread.get(threadId);
			if (checkpoints != null) {
				checkpoints.clear();
				return true;
			}
			return false;
		}
		finally {
			lock.unlock();
		}
	}

	@Override
	public Tag release(RunnableConfig config) throws Exception {
		var threadId = config.threadId().orElse(THREAD_ID_DEFAULT);
		Lock lock = getLock(threadId);
		lock.lock();
		try {
			LinkedList<Checkpoint> removed = _checkpointsByThread.remove(threadId);
			_locksByThread.remove(threadId);
			return new Tag(threadId, removed);
		}
		finally {
			lock.unlock();
		}
	}

	public ConcurrentHashMap<String, LinkedList<Checkpoint>> get_checkpointsByThread() {
		return _checkpointsByThread;
	}

}
