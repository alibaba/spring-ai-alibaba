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
package com.alibaba.cloud.ai.graph.checkpoint.savers.common;

import com.alibaba.cloud.ai.graph.checkpoint.Checkpoint;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * A bounded LRU cache that stores the latest checkpoint for each thread.
 */
public final class LatestCheckpointCache {

	private final int maxCachedThreads;

	private final Map<String, Checkpoint> checkpoints;

	/**
	 * Creates a latest-checkpoint cache with an LRU thread limit.
	 *
	 * @param maxCachedThreads maximum number of thread entries to keep, or 0 to
	 * disable caching
	 */
	public LatestCheckpointCache(int maxCachedThreads) {
		if (maxCachedThreads < 0) {
			throw new IllegalArgumentException("maxCachedThreads must be greater than or equal to 0");
		}
		this.maxCachedThreads = maxCachedThreads;
		this.checkpoints = createCache(maxCachedThreads);
	}

	/**
	 * Gets the cached latest checkpoint for a thread.
	 *
	 * @param threadId thread name/id used by the owning saver
	 * @return cached checkpoint when caching is enabled and the thread is present
	 */
	public synchronized Optional<Checkpoint> get(String threadId) {
		if (maxCachedThreads == 0) {
			return Optional.empty();
		}
		return Optional.ofNullable(checkpoints.get(threadId));
	}

	/**
	 * Stores the latest checkpoint for a thread when caching is enabled.
	 *
	 * @param threadId thread name/id used by the owning saver
	 * @param checkpoint latest checkpoint to cache
	 */
	public synchronized void put(String threadId, Checkpoint checkpoint) {
		if (maxCachedThreads > 0) {
			checkpoints.put(threadId, checkpoint);
		}
	}

	/**
	 * Removes one thread from the cache when caching is enabled.
	 *
	 * @param threadId thread name/id used by the owning saver
	 */
	public synchronized void remove(String threadId) {
		if (maxCachedThreads > 0) {
			checkpoints.remove(threadId);
		}
	}

	/**
	 * Creates either an empty disabled cache or an access-ordered LRU map.
	 *
	 * @param maxCachedThreads maximum number of thread entries to keep
	 * @return map backing the cache
	 */
	private static Map<String, Checkpoint> createCache(int maxCachedThreads) {
		if (maxCachedThreads == 0) {
			return Collections.emptyMap();
		}
		return new LinkedHashMap<>(16, 0.75f, true) {
			/**
			 * Evicts the least recently used thread once the configured limit is
			 * exceeded.
			 *
			 * @param eldest eldest entry tracked by the access-ordered map
			 * @return true when the cache should evict the eldest entry
			 */
			@Override
			protected boolean removeEldestEntry(Map.Entry<String, Checkpoint> eldest) {
				return size() > maxCachedThreads;
			}
		};
	}

}
