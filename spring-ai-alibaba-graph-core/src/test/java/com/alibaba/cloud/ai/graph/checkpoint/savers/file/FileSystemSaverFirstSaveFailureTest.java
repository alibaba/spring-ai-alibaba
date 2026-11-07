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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.AccessDeniedException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class FileSystemSaverFirstSaveFailureTest {

	@TempDir
	Path directory;

	private final RunnableConfig config = RunnableConfig.builder().threadId("first").build();

	private Path target() {
		return directory.resolve("thread-first.saver");
	}

	@ParameterizedTest
	@ValueSource(ints = { 0, 1 })
	void failedFirstCopyRemovesPartialFileWithoutPublishingCacheAndAllowsRetry(int cache) throws Exception {
		assumePosix();
		FaultySaver saver = saver(cache);
		saver.failure = new IOException("partial first copy");

		assertSame(saver.failure, assertThrows(IOException.class, () -> saver.put(config, checkpoint("first"))));
		assertFalse(Files.exists(target()));
		assertTrue(saver.get(config).isEmpty());
		assertTrue(reader().list(config).isEmpty());
		try (var files = Files.list(directory)) {
			assertEquals(0, files.count());
		}

		saver.failure = null;
		saver.put(config, checkpoint("retry"));
		assertEquals("retry", saver.get(config).orElseThrow().getId());
		assertEquals(List.of("retry"), reader().list(config).stream().map(Checkpoint::getId).toList());
	}

	@Test
	void failedCreationNeverDeletesAnExistingEntry() throws Exception {
		assumePosix();
		byte[] original = new byte[] { 7, 8, 9 };
		Files.write(target(), original);
		Path staged = Files.write(directory.resolve("input"), new byte[] { 1 });

		assertThrows(FileAlreadyExistsException.class, () -> saver(0).createFromStaged(staged, target()));
		assertArrayEquals(original, Files.readAllBytes(target()));
		assertTrue(Files.exists(staged));
	}

	@Test
	void danglingSymlinkStillCreatesItsReferent() throws Exception {
		assumePosix();
		Path referent = directory.resolve("referent");
		Files.createSymbolicLink(target(), referent.getFileName());
		FileSystemSaver saver = saver(1);

		saver.put(config, checkpoint("first"));

		assertTrue(Files.isSymbolicLink(target()));
		assertTrue(Files.isRegularFile(referent));
		assertEquals("first", saver.get(config).orElseThrow().getId());
		assertEquals(List.of("first"), reader().list(config).stream().map(Checkpoint::getId).toList());
	}

	@ParameterizedTest
	@ValueSource(ints = { 0, 1 })
	void missingIdentityRetainsTheUnverifiedPathAndReportsUncertainty(int cache) throws Exception {
		assumePosix();
		FaultySaver saver = saver(cache);
		saver.unknownIdentity = true;
		saver.failure = new IOException("partial first copy");

		assertSame(saver.failure, assertThrows(IOException.class, () -> saver.put(config, checkpoint("first"))));
		assertArrayEquals(new byte[] { 1, 2, 3 }, Files.readAllBytes(target()));
		assertTrue(Arrays.stream(saver.failure.getSuppressed())
			.anyMatch(x -> x.getMessage().contains("identity is unverified")));
	}

	@ParameterizedTest
	@ValueSource(strings = { "file", "link" })
	void observedReplacementIsLeftUntouched(String replacement) throws Exception {
		assumePosix();
		FaultySaver saver = saver(1);
		saver.replacement = replacement;
		saver.failure = new IOException("partial first copy");

		assertSame(saver.failure, assertThrows(IOException.class, () -> saver.put(config, checkpoint("first"))));
		assertArrayEquals(new byte[] { 7, 8, 9 }, Files.readAllBytes(target()));
		assertEquals(replacement.equals("link"), Files.isSymbolicLink(target()));
		assertArrayEquals(new byte[] { 1, 2, 3 }, Files.readAllBytes(directory.resolve("owned-partial")));
		assertTrue(Arrays.stream(saver.failure.getSuppressed())
			.anyMatch(x -> x.getMessage().contains("identity is unverified")));
	}

	@Test
	void cleanupFailurePreservesTheCopyExceptionAndReportsTheRemainingFile() throws Exception {
		assumePosix();
		var permissions = Files.getPosixFilePermissions(directory);
		FaultySaver saver = saver(0);
		saver.denyCleanup = true;
		saver.failure = new IOException("partial first copy");
		try {
			Files.setPosixFilePermissions(directory, PosixFilePermissions.fromString("r-x------"));
			assumeTrue(!Files.isWritable(directory), "requires enforced directory write permissions");
			Files.setPosixFilePermissions(directory, permissions);
			assertSame(saver.failure, assertThrows(IOException.class, () -> saver.put(config, checkpoint("first"))));
			assertArrayEquals(new byte[] { 1, 2, 3 }, Files.readAllBytes(target()));
			assertTrue(Arrays.stream(saver.failure.getSuppressed())
				.anyMatch(x -> x.getMessage().contains("cleanup is uncertain")
						&& x.getCause() instanceof AccessDeniedException));
		}
		finally {
			Files.setPosixFilePermissions(directory, permissions);
		}
	}

	@Test
	void uncertainFirstCloseRetainsThePossibleCheckpointAndDoesNotHideTheError() throws Exception {
		assumePosix();
		FaultySaver saver = saver(1);
		saver.closeFailure = new IOException("uncertain first close");

		assertSame(saver.closeFailure, assertThrows(IOException.class, () -> saver.put(config, checkpoint("first"))));
		assertTrue(Files.exists(target()));
		assertTrue(Arrays.stream(saver.closeFailure.getSuppressed())
			.anyMatch(x -> x.getMessage().contains("close is uncertain")));
		assertEquals(List.of("first"), reader().list(config).stream().map(Checkpoint::getId).toList());
	}

	@Test
	void copyAndCloseFailureKeepTheCopyErrorPrimary() throws Exception {
		assumePosix();
		FaultySaver saver = saver(1);
		saver.failure = new IOException("partial first copy");
		saver.closeFailure = new IOException("uncertain first close");

		assertSame(saver.failure, assertThrows(IOException.class, () -> saver.put(config, checkpoint("first"))));
		assertTrue(Arrays.asList(saver.failure.getSuppressed()).contains(saver.closeFailure));
		assertArrayEquals(new byte[] { 1, 2, 3 }, Files.readAllBytes(target()));
	}

	@Test
	void firstTargetKeepsDefaultCreationPermissions() throws Exception {
		assumePosix();
		Path ordinary = Files.createFile(directory.resolve("ordinary"));
		saver(0).put(config, checkpoint("first"));
		assertEquals(Files.getPosixFilePermissions(ordinary), Files.getPosixFilePermissions(target()));
	}

	private void assumePosix() throws IOException {
		assumeTrue(Files.getFileStore(directory).supportsFileAttributeView("posix"));
	}

	private FaultySaver saver(int cache) {
		return new FaultySaver(FileSystemSaver.builder()
			.targetFolder(directory)
			.stateSerializer(new TestSerializer())
			.maxCachedThreads(cache));
	}

	private FileSystemSaver reader() {
		return FileSystemSaver.builder()
			.targetFolder(directory)
			.stateSerializer(new TestSerializer())
			.maxCachedThreads(0)
			.build();
	}

	private static Checkpoint checkpoint(String id) {
		return Checkpoint.builder().id(id).nodeId("before").nextNodeId("after").state(Map.of("value", id)).build();
	}

	private static class TestSerializer extends StateSerializer {

		TestSerializer() {
			super(OverAllState::new);
		}

		public void writeData(Map<String, Object> data, ObjectOutput out) throws IOException {
			out.writeObject(data);
		}

		@SuppressWarnings("unchecked")
		public Map<String, Object> readData(ObjectInput in) throws IOException, ClassNotFoundException {
			return (Map<String, Object>) in.readObject();
		}

	}

	private class FaultySaver extends FileSystemSaver {

		private IOException failure;

		private IOException closeFailure;

		private boolean unknownIdentity;

		private boolean denyCleanup;

		private String replacement;

		FaultySaver(Builder builder) {
			super(builder);
		}

		@Override
		Object checkpointFileKey(Path path) throws IOException {
			return unknownIdentity ? null : super.checkpointFileKey(path);
		}

		@Override
		void copyToCheckpoint(Path source, FileChannel channel) throws IOException {
			assertEquals(PosixFilePermissions.fromString("rw-------"), Files.getPosixFilePermissions(source));
			if (failure != null) {
				channel.write(ByteBuffer.wrap(new byte[] { 1, 2, 3 }));
				if (replacement != null) {
					Files.move(target(), directory.resolve("owned-partial"));
					if (replacement.equals("link")) {
						Path foreign = Files.write(directory.resolve("foreign"), new byte[] { 7, 8, 9 });
						Files.createSymbolicLink(target(), foreign.getFileName());
					}
					else {
						Files.write(target(), new byte[] { 7, 8, 9 }, StandardOpenOption.CREATE_NEW);
					}
				}
				if (denyCleanup)
					Files.setPosixFilePermissions(directory, PosixFilePermissions.fromString("r-x------"));
				throw failure;
			}
			super.copyToCheckpoint(source, channel);
		}

		@Override
		void closeCheckpoint(FileChannel channel) throws IOException {
			super.closeCheckpoint(channel);
			if (closeFailure != null)
				throw closeFailure;
		}

	}

}
