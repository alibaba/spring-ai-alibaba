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

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.checkpoint.Checkpoint;
import com.alibaba.cloud.ai.graph.serializer.StateSerializer;
import com.alibaba.cloud.ai.graph.serializer.plain_text.jackson.SpringAIJacksonStateSerializer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FileSystemSaverSerializationFailureTest {

	@TempDir
	Path tempDir;

	private final RunnableConfig config = RunnableConfig.builder().threadId("failure").build();

	private Path target() {
		return tempDir.resolve("thread-failure.saver");
	}

	@ParameterizedTest
	@ValueSource(ints = { 0, 1 })
	void shouldPreserveCheckpointWhenInsertSerializationFails(int cacheSize) throws Exception {
		FailingSerializer serializer = new FailingSerializer(target(), false);
		FileSystemSaver saver = saver(serializer, cacheSize);
		Checkpoint original = checkpoint("original");
		RunnableConfig saved = saver.put(config, original);
		assertEquals(1, serializer.writeCalls);
		byte[] before = Files.readAllBytes(target());

		IOException failure = assertThrows(IOException.class, () -> saver.put(config, checkpoint("fail")));

		assertSame(serializer.failure, failure);
		assertArrayEquals(before, serializer.targetDuringFailure);
		assertArrayEquals(before, Files.readAllBytes(target()));
		assertCheckpoint(original, saver.get(config).orElseThrow());
		assertCheckpoint(original, saver.get(saved).orElseThrow());
		assertCheckpoint(original, freshReader().get(config).orElseThrow());
		assertEquals(List.of(original.getId()), freshReader().list(config).stream().map(Checkpoint::getId).toList());
		assertOnlyTargetRemains();

		Checkpoint recovered = checkpoint("recovered");
		saver.put(config, recovered);
		assertEquals(4, serializer.writeCalls);
		assertCheckpoint(recovered, saver.get(config).orElseThrow());
		assertCheckpoint(recovered, freshReader().get(config).orElseThrow());
		assertCheckpoint(original, freshReader().get(saved).orElseThrow());
		assertOnlyTargetRemains();
	}

	@ParameterizedTest
	@ValueSource(ints = { 0, 1 })
	void shouldPreserveCheckpointWhenUpdateSerializationFails(int cacheSize) throws Exception {
		FailingSerializer serializer = new FailingSerializer(target(), false);
		FileSystemSaver saver = saver(serializer, cacheSize);
		Checkpoint original = checkpoint("original");
		RunnableConfig saved = saver.put(config, original);
		byte[] before = Files.readAllBytes(target());

		IOException failure = assertThrows(IOException.class, () -> saver.put(saved, checkpoint("fail")));

		assertSame(serializer.failure, failure);
		assertArrayEquals(before, serializer.targetDuringFailure);
		assertArrayEquals(before, Files.readAllBytes(target()));
		assertCheckpoint(original, saver.get(config).orElseThrow());
		assertCheckpoint(original, saver.get(saved).orElseThrow());
		assertCheckpoint(original, freshReader().get(saved).orElseThrow());
		assertEquals(List.of(original.getId()), freshReader().list(config).stream().map(Checkpoint::getId).toList());
		assertOnlyTargetRemains();
	}

	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	void shouldLeaveNoCheckpointOrTemporaryFileAfterInitialFailure(boolean runtimeFailure) throws Exception {
		FailingSerializer serializer = new FailingSerializer(target(), runtimeFailure);
		FileSystemSaver saver = saver(serializer, 1);

		Exception failure = assertThrows(Exception.class, () -> saver.put(config, checkpoint("fail")));

		assertSame(serializer.failure, failure);
		assertNull(serializer.targetDuringFailure);
		assertFalse(Files.exists(target()));
		assertEquals(0, saver.list(config).size());
		assertEquals(0, freshReader().list(config).size());
		try (var files = Files.list(tempDir)) {
			assertEquals(List.of(), files.toList());
		}
	}

	@Test
	void shouldPreserveCompleteHistoryWhenLaterCheckpointSerializationFails() throws Exception {
		FailingSerializer serializer = new FailingSerializer(target(), true);
		FileSystemSaver saver = saver(serializer, 1);
		Checkpoint first = checkpoint("first");
		RunnableConfig firstConfig = saver.put(config, first);
		Checkpoint second = checkpoint("second");
		saver.put(config, second);
		byte[] before = Files.readAllBytes(target());

		IllegalStateException failure = assertThrows(IllegalStateException.class,
				() -> saver.put(firstConfig, checkpoint("fail")));

		assertSame(serializer.failure, failure);
		assertArrayEquals(before, serializer.targetDuringFailure);
		assertArrayEquals(before, Files.readAllBytes(target()));
		assertCheckpoint(second, saver.get(config).orElseThrow());
		assertCheckpoint(first, saver.get(firstConfig).orElseThrow());
		assertCheckpoint(first, freshReader().get(firstConfig).orElseThrow());
		assertEquals(List.of(second.getId(), first.getId()),
				freshReader().list(config).stream().map(Checkpoint::getId).toList());
		assertOnlyTargetRemains();
	}

	private FileSystemSaver saver(StateSerializer serializer, int cacheSize) {
		return FileSystemSaver.builder()
			.targetFolder(tempDir)
			.stateSerializer(serializer)
			.maxCachedThreads(cacheSize)
			.build();
	}

	private FileSystemSaver freshReader() {
		return saver(new SpringAIJacksonStateSerializer(OverAllState::new), 0);
	}

	private void assertOnlyTargetRemains() throws IOException {
		try (var files = Files.list(tempDir)) {
			assertEquals(List.of(target()), files.toList());
		}
	}

	private static Checkpoint checkpoint(String value) {
		return Checkpoint.builder().nodeId("before").nextNodeId("after").state(Map.of("value", value)).build();
	}

	private static void assertCheckpoint(Checkpoint expected, Checkpoint actual) {
		assertEquals(expected.getId(), actual.getId());
		assertEquals(expected.getNodeId(), actual.getNodeId());
		assertEquals(expected.getNextNodeId(), actual.getNextNodeId());
		assertEquals(expected.getState(), actual.getState());
	}

	private static class FailingSerializer extends StateSerializer {

		private final SpringAIJacksonStateSerializer delegate = new SpringAIJacksonStateSerializer(OverAllState::new);

		private final Path target;

		private final Exception failure;

		private int writeCalls;

		private byte[] targetDuringFailure;

		FailingSerializer(Path target, boolean runtimeFailure) {
			super(OverAllState::new);
			this.target = target;
			this.failure = runtimeFailure ? new IllegalStateException("serialization fixture")
					: new IOException("serialization fixture");
		}

		@Override
		public void writeData(Map<String, Object> data, ObjectOutput out) throws IOException {
			writeCalls++;
			if ("fail".equals(data.get("value"))) {
				out.writeInt(1234);
				out.writeUTF("partial serialized output");
				out.flush();
				targetDuringFailure = Files.exists(target) ? Files.readAllBytes(target) : null;
				if (failure instanceof IOException ioFailure) {
					throw ioFailure;
				}
				throw (IllegalStateException) failure;
			}
			delegate.writeData(data, out);
		}

		@Override
		public Map<String, Object> readData(ObjectInput in) throws IOException, ClassNotFoundException {
			return delegate.readData(in);
		}

	}

}
