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
package com.alibaba.cloud.ai.graph.checkpoint;

import com.alibaba.cloud.ai.graph.RunnableConfig;

import java.util.Collection;
import java.util.Optional;

/**
 * Represents an entity that can have different versions associated with it. Experimental
 * feature
 */
public interface HasVersions {

	/**
	 * Retrieves a collection of integer versions associated with the specified thread ID.
	 * @param threadId the ID of the thread for which to retrieve version information
	 * @return a {@code Collection<Integer>} containing the versions, or an empty
	 * collection if no versions are found
	 */
	Collection<Integer> versionsByThreadId(String threadId);

	/**
	 * Retrieves the collection of versions associated with a specific thread ID from the
	 * given {@link RunnableConfig}.
	 * @param config The configuration object containing the thread ID information.
	 * @return A {@code Collection<Integer>} representing the versions associated with the
	 * thread ID, or an empty collection if not specified.
	 */
	default Collection<Integer> versionsByThreadId(RunnableConfig config) {
		return versionsByThreadId(config.threadId().orElse(null));
	}

	/**
	 * Retrieves the last version associated with a specific thread ID.
	 * @param threadId The unique identifier of the thread.
	 * @return An {@link Optional} containing the last version if found, otherwise an
	 * empty {@link Optional}.
	 */
	Optional<Integer> lastVersionByThreadId(String threadId);

	/**
	 * Retrieves the last version associated with a specific thread ID.
	 * @param config The configuration containing the thread ID.
	 * @return An {@link Optional} containing the last version if found, or an empty
	 * {@link Optional} otherwise.
	 */
	default Optional<Integer> lastVersionByThreadId(RunnableConfig config) {
		return lastVersionByThreadId(config.threadId().orElse(null));
	}

}
