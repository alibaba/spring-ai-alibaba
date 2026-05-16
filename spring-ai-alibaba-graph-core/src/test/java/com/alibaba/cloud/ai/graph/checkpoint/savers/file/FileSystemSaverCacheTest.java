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
package com.alibaba.cloud.ai.graph.checkpoint.savers.file;

import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.checkpoint.Checkpoint;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FileSystemSaverCacheTest {

	@TempDir
	Path tempDir;

	private static RunnableConfig config(String threadId) {
		return RunnableConfig.builder().threadId(threadId).build();
	}

	private static Checkpoint checkpoint(String value) {
		return Checkpoint.builder()
				.nodeId("node")
				.nextNodeId("next")
				.state(Map.of("value", value))
				.build();
	}

	@Test
	public void shouldLoadLatestCheckpointWhenCacheDisabled() throws Exception {
		FileSystemSaver saver = FileSystemSaver.builder()
				.targetFolder(tempDir)
				.maxCachedThreads(0)
				.build();
		RunnableConfig config = config("thread-1");
		Checkpoint checkpoint = checkpoint("v1");

		RunnableConfig checkpointConfig = saver.put(config, checkpoint);

		assertEquals(checkpoint.getId(), saver.get(config).orElseThrow().getId());
		assertEquals(checkpoint.getId(), saver.get(checkpointConfig).orElseThrow().getId());
		assertEquals(1, saver.list(config).size());
	}

	@Test
	public void shouldLoadEvictedLatestCheckpointFromFile() throws Exception {
		FileSystemSaver saver = FileSystemSaver.builder()
				.targetFolder(tempDir)
				.maxCachedThreads(1)
				.build();
		RunnableConfig firstThread = config("thread-1");
		RunnableConfig secondThread = config("thread-2");
		Checkpoint firstCheckpoint = checkpoint("v1");
		Checkpoint secondCheckpoint = checkpoint("v2");

		saver.put(firstThread, firstCheckpoint);
		saver.put(secondThread, secondCheckpoint);

		assertEquals(firstCheckpoint.getId(), saver.get(firstThread).orElseThrow().getId());
		assertEquals(secondCheckpoint.getId(), saver.get(secondThread).orElseThrow().getId());
	}

	@Test
	public void releaseShouldReturnHistoryAndClearLatestCache() throws Exception {
		FileSystemSaver saver = FileSystemSaver.builder()
				.targetFolder(tempDir)
				.build();
		RunnableConfig config = config("thread-1");
		Checkpoint firstCheckpoint = checkpoint("v1");
		Checkpoint secondCheckpoint = checkpoint("v2");

		saver.put(config, firstCheckpoint);
		saver.put(config, secondCheckpoint);

		assertEquals(secondCheckpoint.getId(), saver.get(config).orElseThrow().getId());

		var tag = saver.release(config);

		assertEquals("thread-1", tag.threadId());
		assertEquals(2, tag.checkpoints().size());
		assertTrue(saver.get(config).isEmpty());
		assertFalse(Files.exists(tempDir.resolve("thread-thread-1.saver")));
		assertTrue(Files.exists(tempDir.resolve("thread-thread-1-v1.saver")));
	}

	@Test
	public void deleteFileShouldClearLatestCache() throws Exception {
		FileSystemSaver saver = FileSystemSaver.builder()
				.targetFolder(tempDir)
				.build();
		RunnableConfig config = config("thread-1");
		Checkpoint checkpoint = checkpoint("v1");

		saver.put(config, checkpoint);
		assertEquals(checkpoint.getId(), saver.get(config).orElseThrow().getId());

		assertTrue(saver.deleteFile(config));
		assertTrue(saver.get(config).isEmpty());
	}

}
