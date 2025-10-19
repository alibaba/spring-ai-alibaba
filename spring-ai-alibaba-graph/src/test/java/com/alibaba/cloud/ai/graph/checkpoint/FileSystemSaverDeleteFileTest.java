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

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.checkpoint.savers.FileSystemSaver;
import com.alibaba.cloud.ai.graph.serializer.std.ObjectStreamStateSerializer;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link FileSystemSaver#deleteFile(RunnableConfig)}.
 */
public class FileSystemSaverDeleteFileTest {

	@Test
	public void deleteFileShouldRemoveExistingCheckpoint() throws Exception {
		Path root = Files.createTempDirectory("checkpoint");
		FileSystemSaver saver = new FileSystemSaver(root, new ObjectStreamStateSerializer(OverAllState::new));
		RunnableConfig config = RunnableConfig.builder().threadId("t1").build();

		Path checkpointFile = root.resolve("thread-t1.saver");
		Files.createFile(checkpointFile);

		assertTrue(Files.exists(checkpointFile));
		assertTrue(saver.deleteFile(config));
		assertFalse(Files.exists(checkpointFile));
		assertFalse(saver.deleteFile(config));
	}

}
