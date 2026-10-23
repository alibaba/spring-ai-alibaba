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
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mockStatic;

class FileSystemSaverAtomicReplacementTest {

	@TempDir
	Path tempDir;

	private final RunnableConfig config = RunnableConfig.builder().threadId("atomic").build();

	private Path target() {
		return tempDir.resolve("thread-atomic.saver").toAbsolutePath();
	}

	private FileSystemSaver saver(int cacheSize) {
		return FileSystemSaver.builder().targetFolder(tempDir).maxCachedThreads(cacheSize).build();
	}

	@ParameterizedTest
	@ValueSource(ints = { 0, 1 })
	void shouldPublishCompleteInsertAndUpdateHistory(int cacheSize) throws Exception {
		FileSystemSaver saver = saver(cacheSize);
		Checkpoint first = checkpoint("first");
		RunnableConfig saved = saver.put(config, first);
		Object firstKey = fileKey(target());
		Checkpoint second = checkpoint("second");
		saver.put(config, second);
		assertReplaced(firstKey, target());
		assertCheckpoint(second, saver.get(config).orElseThrow());
		assertCheckpoint(first, saver(0).get(saved).orElseThrow());
		Object secondKey = fileKey(target());
		Checkpoint replacement = checkpoint("replacement");
		saver.put(saved, replacement);
		assertReplaced(secondKey, target());
		assertCheckpoint(second, saver.get(config).orElseThrow());
		assertCheckpoint(replacement, saver(0).get(saved.withCheckPointId(replacement.getId())).orElseThrow());
		assertEquals(List.of(second.getId(), replacement.getId()),
				saver(0).list(config).stream().map(Checkpoint::getId).toList());
		assertOnlyTargetRemains();
	}

	static Stream<Arguments> failedReplacements() {
		return Stream.of(0, 1)
			.flatMap(cacheSize -> Stream.of(false, true)
				.flatMap(update -> Stream.of(false, true)
					.map(unsupported -> Arguments.of(cacheSize, update, unsupported))));
	}

	@ParameterizedTest
	@MethodSource("failedReplacements")
	void shouldPreserveFileHistoryAndCacheWhenReplacementFails(int cacheSize, boolean update, boolean unsupported)
			throws Exception {
		FileSystemSaver saver = saver(cacheSize);
		Checkpoint original = checkpoint("original");
		RunnableConfig saved = saver.put(config, original);
		byte[] before = Files.readAllBytes(target());
		Object key = fileKey(target());
		IOException failure = unsupported
				? new AtomicMoveNotSupportedException("stage", target().toString(), "fixture: unsupported")
				: new IOException("fixture: replacement I/O failure");
		try (MockedStatic<Files> files = mockStatic(Files.class, CALLS_REAL_METHODS)) {
			files.when(() -> Files.move(any(Path.class), eq(target()), eq(ATOMIC_MOVE), eq(REPLACE_EXISTING)))
				.thenThrow(failure);
			assertSame(failure,
					assertThrows(IOException.class, () -> saver.put(update ? saved : config, checkpoint("rejected"))));
		}
		assertArrayEquals(before, Files.readAllBytes(target()));
		assertEquals(key, fileKey(target()));
		assertCheckpoint(original, saver.get(config).orElseThrow());
		assertCheckpoint(original, saver(0).get(saved).orElseThrow());
		assertEquals(List.of(original.getId()), saver(0).list(config).stream().map(Checkpoint::getId).toList());
		assertOnlyTargetRemains();
		Checkpoint recovered = checkpoint("recovered");
		saver.put(config, recovered);
		assertCheckpoint(recovered, saver.get(config).orElseThrow());
		assertCheckpoint(recovered, saver(0).get(config).orElseThrow());
		assertCheckpoint(original, saver(0).get(saved).orElseThrow());
		assertOnlyTargetRemains();
	}

	@Test
	void shouldLeaveNoFileOrCacheWhenInitialAtomicReplacementIsUnsupported() throws Exception {
		FileSystemSaver saver = saver(1);
		IOException failure = new AtomicMoveNotSupportedException("stage", target().toString(), "fixture");
		try (MockedStatic<Files> files = mockStatic(Files.class, CALLS_REAL_METHODS)) {
			files.when(() -> Files.move(any(Path.class), eq(target()), eq(ATOMIC_MOVE), eq(REPLACE_EXISTING)))
				.thenThrow(failure);
			assertSame(failure, assertThrows(IOException.class, () -> saver.put(config, checkpoint("rejected"))));
		}
		assertFalse(Files.exists(target()));
		assertTrue(saver.get(config).isEmpty());
		assertTrue(saver(0).list(config).isEmpty());
		assertDirectoryContains(List.of());
	}

	@Test
	void shouldPreserveCheckpointWhenStagingCloseFails() throws Exception {
		FileSystemSaver saver = saver(1);
		Checkpoint original = checkpoint("original");
		saver.put(config, original);
		byte[] before = Files.readAllBytes(target());
		IOException failure = new IOException("fixture: staging close failure");
		try (MockedStatic<Files> files = mockStatic(Files.class, CALLS_REAL_METHODS)) {
			files.when(() -> Files.newOutputStream(any(Path.class))).thenAnswer(invocation -> {
				return new FilterOutputStream((OutputStream) invocation.callRealMethod()) {
					@Override
					public void close() throws IOException {
						super.close();
						throw failure;
					}
				};
			});
			assertSame(failure, assertThrows(IOException.class, () -> saver.put(config, checkpoint("rejected"))));
		}
		assertArrayEquals(before, Files.readAllBytes(target()));
		assertCheckpoint(original, saver.get(config).orElseThrow());
		assertCheckpoint(original, saver(0).get(config).orElseThrow());
		assertOnlyTargetRemains();
	}

	@Test
	void shouldPreserveCheckpointWhenStagingCreationFails() throws Exception {
		FileSystemSaver saver = saver(1);
		Checkpoint original = checkpoint("original");
		saver.put(config, original);
		byte[] before = Files.readAllBytes(target());
		IOException failure = new IOException("fixture: staging creation failure");
		try (MockedStatic<Files> files = mockStatic(Files.class, CALLS_REAL_METHODS)) {
			files.when(() -> Files.createTempFile(eq(tempDir.toAbsolutePath()), eq(".checkpoint-"), eq(".tmp")))
				.thenThrow(failure);
			assertSame(failure, assertThrows(IOException.class, () -> saver.put(config, checkpoint("rejected"))));
		}
		assertArrayEquals(before, Files.readAllBytes(target()));
		assertCheckpoint(original, saver.get(config).orElseThrow());
		assertCheckpoint(original, saver(0).get(config).orElseThrow());
		assertOnlyTargetRemains();
	}

	@Test
	void shouldReportCleanupFailureWithoutMaskingReplacementFailure() throws Exception {
		FileSystemSaver saver = saver(1);
		Checkpoint original = checkpoint("original");
		saver.put(config, original);
		byte[] before = Files.readAllBytes(target());
		IOException failure = new AtomicMoveNotSupportedException("stage", target().toString(), "fixture");
		IOException cleanupFailure = new IOException("fixture: cleanup failure");
		AtomicReference<Path> stage = new AtomicReference<>();
		try (MockedStatic<Files> files = mockStatic(Files.class, CALLS_REAL_METHODS)) {
			files.when(() -> Files.move(any(Path.class), eq(target()), eq(ATOMIC_MOVE), eq(REPLACE_EXISTING)))
				.thenAnswer(invocation -> {
					stage.set(invocation.getArgument(0));
					throw failure;
				});
			files.when(() -> Files.deleteIfExists(any(Path.class))).thenThrow(cleanupFailure);
			assertSame(failure, assertThrows(IOException.class, () -> saver.put(config, checkpoint("rejected"))));
		}
		assertArrayEquals(new Throwable[] { cleanupFailure }, failure.getSuppressed());
		assertArrayEquals(before, Files.readAllBytes(target()));
		assertCheckpoint(original, saver.get(config).orElseThrow());
		assertCheckpoint(original, saver(0).get(config).orElseThrow());
		assertTrue(Files.size(stage.get()) > 0);
		Files.delete(stage.get());
		assertOnlyTargetRemains();
	}

	@Test
	void shouldCleanStageAfterRealDirectoryCollision() throws Exception {
		StateSerializer serializer = new StateSerializer(OverAllState::new) {
			private final SpringAIJacksonStateSerializer delegate = new SpringAIJacksonStateSerializer(
					OverAllState::new);

			@Override
			public void writeData(Map<String, Object> data, ObjectOutput out) throws IOException {
				delegate.writeData(data, out);
				Files.createDirectory(target());
				Files.writeString(target().resolve("marker"), "existing directory");
			}

			@Override
			public Map<String, Object> readData(ObjectInput in) throws IOException, ClassNotFoundException {
				return delegate.readData(in);
			}
		};
		FileSystemSaver saver = FileSystemSaver.builder()
			.targetFolder(tempDir)
			.stateSerializer(serializer)
			.maxCachedThreads(1)
			.build();
		assertThrows(IOException.class, () -> saver.put(config, checkpoint("rejected")));
		assertEquals("existing directory", Files.readString(target().resolve("marker")));
		assertOnlyTargetRemains();
		Files.delete(target().resolve("marker"));
		Files.delete(target());
		assertTrue(saver.get(config).isEmpty());
		assertTrue(saver(0).get(config).isEmpty());
	}

	@Test
	void shouldReplaceSymlinkWithoutChangingItsReferent() throws Exception {
		FileSystemSaver saver = saver(1);
		Checkpoint original = checkpoint("original");
		saver.put(config, original);
		Path alias = tempDir.resolve("thread-alias.saver");
		Files.move(target(), alias);
		try {
			Files.createSymbolicLink(target(), alias.getFileName());
		}
		catch (UnsupportedOperationException | IOException failure) {
			assumeTrue(false, "Symbolic links unavailable: " + failure);
		}
		byte[] before = Files.readAllBytes(alias);
		Object oldKey = fileKey(alias);
		Checkpoint replacement = checkpoint("replacement");
		saver.put(config, replacement);
		assertFalse(Files.isSymbolicLink(target()));
		assertReplaced(oldKey, target());
		assertEquals(oldKey, fileKey(alias));
		assertArrayEquals(before, Files.readAllBytes(alias));
		assertCheckpoint(replacement, saver.get(config).orElseThrow());
		assertCheckpoint(replacement, saver(0).get(config).orElseThrow());
		assertCheckpoint(original, saver(0).get(RunnableConfig.builder().threadId("alias").build()).orElseThrow());
		assertDirectoryContains(List.of(target(), alias));
	}

	@Test
	void shouldReplaceHardlinkWithoutUpdatingOtherAliases() throws Exception {
		FileSystemSaver saver = saver(1);
		Checkpoint original = checkpoint("original");
		saver.put(config, original);
		Path alias = tempDir.resolve("thread-alias.saver");
		try {
			Files.createLink(alias, target());
		}
		catch (UnsupportedOperationException | IOException failure) {
			assumeTrue(false, "Hard links unavailable: " + failure);
		}
		byte[] before = Files.readAllBytes(alias);
		Object oldKey = fileKey(alias);
		Checkpoint replacement = checkpoint("replacement");
		saver.put(config, replacement);
		assertFalse(Files.isSameFile(alias, target()));
		assertReplaced(oldKey, target());
		assertEquals(oldKey, fileKey(alias));
		assertArrayEquals(before, Files.readAllBytes(alias));
		assertCheckpoint(replacement, saver.get(config).orElseThrow());
		assertCheckpoint(replacement, saver(0).get(config).orElseThrow());
		assertCheckpoint(original, saver(0).get(RunnableConfig.builder().threadId("alias").build()).orElseThrow());
		assertDirectoryContains(List.of(target(), alias));
	}

	private static Object fileKey(Path path) throws IOException {
		return Files.readAttributes(path, BasicFileAttributes.class).fileKey();
	}

	private static void assertReplaced(Object oldKey, Path path) throws IOException {
		if (oldKey != null) {
			assertNotEquals(oldKey, fileKey(path));
		}
	}

	private void assertOnlyTargetRemains() throws IOException {
		assertDirectoryContains(List.of(target()));
	}

	private void assertDirectoryContains(List<Path> expected) throws IOException {
		try (var paths = Files.list(tempDir)) {
			assertEquals(expected.stream().sorted().toList(), paths.sorted().toList());
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

}
