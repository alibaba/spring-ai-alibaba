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
import com.alibaba.cloud.ai.graph.checkpoint.Checkpoint;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AbstractJdbcCheckpointSaverTest {

	@Test
	void shouldCacheLatestCheckpointAndReloadEvictedThread() throws Exception {
		var saver = new FakeJdbcCheckpointSaver(2);
		var firstCheckpoint = checkpoint("first");
		var firstConfig = config("thread-1");

		saver.put(firstConfig, firstCheckpoint);
		saver.put(config("thread-2"), checkpoint("second"));
		saver.put(config("thread-3"), checkpoint("third"));

		var reloaded = saver.get(firstConfig);

		assertTrue(reloaded.isPresent());
		assertEquals(firstCheckpoint.getId(), reloaded.get().getId());
		assertEquals(1, saver.latestCheckpointSelects);
	}

	@Test
	void shouldBypassLatestCacheWhenCheckpointIdIsProvided() throws Exception {
		var saver = new FakeJdbcCheckpointSaver(16);
		String threadId = "thread-by-id";
		var firstCheckpoint = checkpoint("first");
		var secondCheckpoint = checkpoint("second");

		saver.put(config(threadId), firstCheckpoint);
		saver.put(config(threadId), secondCheckpoint);

		var first = saver.get(config(threadId, firstCheckpoint.getId()));

		assertTrue(first.isPresent());
		assertEquals(firstCheckpoint.getId(), first.get().getId());
		assertEquals(1, saver.checkpointByIdSelects);
		assertEquals(0, saver.latestCheckpointSelects);
	}

	@Test
	void shouldRefreshLatestCacheWhenLatestCheckpointIsUpdated() throws Exception {
		var saver = new FakeJdbcCheckpointSaver(16);
		String threadId = "thread-update";
		var originalCheckpoint = checkpoint("original");
		var updatedCheckpoint = checkpoint("updated");
		var checkpointConfig = saver.put(config(threadId), originalCheckpoint);

		saver.put(checkpointConfig, updatedCheckpoint);
		var latest = saver.get(config(threadId));

		assertTrue(latest.isPresent());
		assertEquals(updatedCheckpoint.getId(), latest.get().getId());
		assertEquals(0, saver.latestCheckpointSelects);
	}

	@Test
	void shouldClearLatestCacheWhenThreadIsReleased() throws Exception {
		var saver = new FakeJdbcCheckpointSaver(16);
		String threadId = "thread-release";
		saver.put(config(threadId), checkpoint("released"));

		var released = saver.release(config(threadId));
		var latest = saver.get(config(threadId));

		assertEquals(threadId, released.threadId());
		assertEquals(1, released.checkpoints().size());
		assertTrue(latest.isEmpty());
		assertEquals(1, saver.latestCheckpointSelects);
	}

	@Test
	void shouldDisableLatestCacheWhenMaxCachedThreadsIsZero() throws Exception {
		var saver = new FakeJdbcCheckpointSaver(0);
		String threadId = "thread-no-cache";
		var checkpoint = checkpoint("latest");
		saver.put(config(threadId), checkpoint);

		saver.get(config(threadId));
		saver.get(config(threadId));

		assertEquals(2, saver.latestCheckpointSelects);
	}

	@Test
	void shouldDeleteOlderCheckpointsWhenRetentionIsConfigured() throws Exception {
		var saver = new FakeJdbcCheckpointSaver(16);
		var config = RunnableConfig.builder()
				.threadId("thread-retained")
				.checkpointsNumRetained(2)
				.build();
		var firstCheckpoint = checkpoint("first");
		var secondCheckpoint = checkpoint("second");
		var thirdCheckpoint = checkpoint("third");

		saver.put(config, firstCheckpoint);
		saver.put(config, secondCheckpoint);
		saver.put(config, thirdCheckpoint);

		var checkpoints = saver.list(config);
		assertEquals(2, checkpoints.size());
		assertEquals(thirdCheckpoint.getId(), checkpoints.stream().findFirst().orElseThrow().getId());
		assertTrue(saver.get(config("thread-retained", firstCheckpoint.getId())).isEmpty());
	}

	@Test
	void shouldRejectNegativeMaxCachedThreads() {
		assertThrows(IllegalArgumentException.class, () -> new FakeJdbcCheckpointSaver(-1));
	}

	private static RunnableConfig config(String threadId) {
		return RunnableConfig.builder().threadId(threadId).build();
	}

	private static RunnableConfig config(String threadId, String checkpointId) {
		return RunnableConfig.builder().threadId(threadId).checkPointId(checkpointId).build();
	}

	private static Checkpoint checkpoint(String value) {
		return Checkpoint.builder()
				.id(UUID.randomUUID().toString())
				.nodeId("node")
				.nextNodeId("next")
				.state(Map.of("value", value))
				.build();
	}

	private static final class FakeJdbcCheckpointSaver extends AbstractJdbcCheckpointSaver {

		private final Map<String, LinkedList<Checkpoint>> checkpoints = new HashMap<>();

		private final Set<String> releasedThreads = new HashSet<>();

		private int latestCheckpointSelects;

		private int checkpointByIdSelects;

		private FakeJdbcCheckpointSaver(int maxCachedThreads) {
			super(maxCachedThreads);
		}

		@Override
		protected LinkedList<Checkpoint> selectCheckpoints(String threadId) {
			if (releasedThreads.contains(threadId)) {
				return new LinkedList<>();
			}
			return new LinkedList<>(checkpoints.getOrDefault(threadId, new LinkedList<>()));
		}

		@Override
		protected Optional<Checkpoint> selectLatestCheckpoint(String threadId) {
			latestCheckpointSelects++;
			return selectCheckpoints(threadId).stream().findFirst();
		}

		@Override
		protected Optional<Checkpoint> selectCheckpointById(String threadId, String checkpointId) {
			checkpointByIdSelects++;
			return selectCheckpoints(threadId).stream()
					.filter(checkpoint -> checkpoint.getId().equals(checkpointId))
					.findFirst();
		}

		@Override
		protected void insertCheckpoint(String threadId, Checkpoint checkpoint) {
			checkpoints.computeIfAbsent(threadId, key -> new LinkedList<>()).addFirst(checkpoint);
		}

		@Override
		protected void updateCheckpoint(String threadId, String checkpointId, Checkpoint checkpoint) {
			LinkedList<Checkpoint> history = checkpoints.getOrDefault(threadId, new LinkedList<>());
			for (int i = 0; i < history.size(); i++) {
				if (history.get(i).getId().equals(checkpointId)) {
					history.set(i, checkpoint);
					return;
				}
			}
			throw new NoSuchElementException("Checkpoint with id %s not found!".formatted(checkpointId));
		}

		@Override
		protected void deleteCheckpoints(String threadId, java.util.Collection<String> checkpointIds) {
			LinkedList<Checkpoint> history = checkpoints.getOrDefault(threadId, new LinkedList<>());
			history.removeIf(checkpoint -> checkpointIds.contains(checkpoint.getId()));
		}

		@Override
		protected void releaseThread(String threadId) {
			releasedThreads.add(threadId);
		}

	}

}
