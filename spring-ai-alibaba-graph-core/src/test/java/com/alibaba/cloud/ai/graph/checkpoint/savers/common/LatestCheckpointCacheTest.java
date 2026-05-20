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

import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LatestCheckpointCacheTest {

	private static Checkpoint checkpoint(String value) {
		return Checkpoint.builder()
				.nodeId("node")
				.nextNodeId("next")
				.state(Map.of("value", value))
				.build();
	}

	@Test
	void shouldDisableCacheWhenMaxCachedThreadsIsZero() {
		LatestCheckpointCache cache = new LatestCheckpointCache(0);
		Checkpoint checkpoint = checkpoint("v1");

		cache.put("thread-1", checkpoint);

		assertTrue(cache.get("thread-1").isEmpty());
	}

	@Test
	void shouldEvictLeastRecentlyUsedThread() {
		LatestCheckpointCache cache = new LatestCheckpointCache(1);
		Checkpoint firstCheckpoint = checkpoint("v1");
		Checkpoint secondCheckpoint = checkpoint("v2");

		cache.put("thread-1", firstCheckpoint);
		cache.put("thread-2", secondCheckpoint);

		assertTrue(cache.get("thread-1").isEmpty());
		assertEquals(secondCheckpoint.getId(), cache.get("thread-2").orElseThrow().getId());
	}

	@Test
	void shouldRejectNegativeMaxCachedThreads() {
		assertThrows(IllegalArgumentException.class, () -> new LatestCheckpointCache(-1));
	}

}
