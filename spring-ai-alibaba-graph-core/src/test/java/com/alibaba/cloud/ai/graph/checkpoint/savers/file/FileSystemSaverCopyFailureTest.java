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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class FileSystemSaverCopyFailureTest {

	@TempDir
	Path directory;

	private final RunnableConfig config = RunnableConfig.builder().threadId("copy").build();

	private Path target() {
		return directory.resolve("thread-copy.saver");
	}

	@ParameterizedTest
	@ValueSource(ints = { 0, 1 })
	void failedInsertRestoresCompleteHistoryAndAllowsRetry(int cacheSize) throws Exception {
		assumePosix();
		FaultySaver saver = saver(cacheSize);
		Checkpoint first = checkpoint("first");
		Checkpoint second = checkpoint("second");
		saver.put(config, first);
		saver.put(config, second);
		byte[] before = Files.readAllBytes(target());
		Object identity = Files.getAttribute(target(), "unix:ino");
		saver.failure = new IOException("partial target copy");

		assertSame(saver.failure, assertThrows(IOException.class, () -> saver.put(config, checkpoint("new"))));

		assertArrayEquals(before, Files.readAllBytes(target()));
		assertEquals(identity, Files.getAttribute(target(), "unix:ino"));
		assertEquals(second.getId(), saver.get(config).orElseThrow().getId());
		assertEquals(List.of(second.getId(), first.getId()),
				freshReader().list(config).stream().map(Checkpoint::getId).toList());
		assertOnlyTargetRemains();
		saver.failure = null;
		Checkpoint retry = checkpoint("retry");
		saver.put(config, retry);
		assertEquals(List.of(retry.getId(), second.getId(), first.getId()),
				freshReader().list(config).stream().map(Checkpoint::getId).toList());
		assertOnlyTargetRemains();
	}

	@ParameterizedTest
	@ValueSource(ints = { 0, 1 })
	void failedUpdatePreservesStoredAndCachedCheckpoint(int cacheSize) throws Exception {
		assumePosix();
		FaultySaver saver = saver(cacheSize);
		Checkpoint original = checkpoint("original");
		RunnableConfig saved = saver.put(config, original);
		byte[] before = Files.readAllBytes(target());
		saver.failure = new IllegalStateException("unchecked copy failure");

		assertSame(saver.failure,
				assertThrows(IllegalStateException.class, () -> saver.put(saved, checkpoint("replacement"))));

		assertArrayEquals(before, Files.readAllBytes(target()));
		assertEquals(original.getState(), saver.get(config).orElseThrow().getState());
		assertEquals(original.getState(), freshReader().get(saved).orElseThrow().getState());
		assertOnlyTargetRemains();
	}

	@Test
	void controlledErrorRestoresOldBytesWithoutChangingItsType() throws Exception {
		assumePosix();
		FaultySaver saver = saver(0);
		saver.put(config, checkpoint("old"));
		byte[] before = Files.readAllBytes(target());
		saver.failure = new AssertionError("controlled copy error");

		assertSame(saver.failure, assertThrows(AssertionError.class, () -> saver.put(config, checkpoint("new"))));

		assertArrayEquals(before, Files.readAllBytes(target()));
		assertOnlyTargetRemains();
	}

	@ParameterizedTest
	@ValueSource(ints = { 0, 1 })
	void failedRollbackRetainsPrivateExactOldImage(int cacheSize) throws Exception {
		assumePosix();
		FaultySaver saver = saver(cacheSize);
		saver.put(config, checkpoint("old"));
		byte[] before = Files.readAllBytes(target());
		saver.failure = new IOException("partial copy");
		saver.failRollback = true;

		IOException failure = assertThrows(IOException.class, () -> saver.put(config, checkpoint("new")));

		assertSame(saver.failure, failure);
		assertTrue(List.of(failure.getSuppressed())
			.stream()
			.anyMatch(x -> x.getMessage().contains("outcome is uncertain")));
		Path backup = retainedBackup();
		assertArrayEquals(before, Files.readAllBytes(backup));
		assertEquals(PosixFilePermissions.fromString("rw-------"), Files.getPosixFilePermissions(backup));
		assertFalse(java.util.Arrays.equals(before, Files.readAllBytes(target())));
	}

	@ParameterizedTest
	@ValueSource(ints = { 0, 1 })
	void closeUncertaintyRetainsBackupAndDoesNotPublishCache(int cacheSize) throws Exception {
		assumePosix();
		FaultySaver saver = saver(cacheSize);
		Checkpoint old = checkpoint("old");
		saver.put(config, old);
		byte[] before = Files.readAllBytes(target());
		saver.failClose = true;
		Checkpoint next = checkpoint("new");

		IOException failure = assertThrows(IOException.class, () -> saver.put(config, next));

		assertTrue(List.of(failure.getSuppressed())
			.stream()
			.anyMatch(x -> x.getMessage().contains("outcome is uncertain")));
		assertArrayEquals(before, Files.readAllBytes(retainedBackup()));
		assertEquals(next.getId(), freshReader().get(config).orElseThrow().getId());
		if (cacheSize > 0) {
			assertEquals(old.getId(), saver.get(config).orElseThrow().getId());
		}
	}

	@Test
	void recoveryKeepsExistingHardlinkAndPermissions() throws Exception {
		assumePosix();
		FaultySaver saver = saver(0);
		saver.put(config, checkpoint("old"));
		Path alias = Files.createLink(directory.resolve("alias"), target());
		var permissions = PosixFilePermissions.fromString("rw-r-----");
		Files.setPosixFilePermissions(target(), permissions);
		Object inode = Files.getAttribute(target(), "unix:ino");

		saver.put(config, checkpoint("new"));

		assertEquals(inode, Files.getAttribute(target(), "unix:ino"));
		assertEquals(permissions, Files.getPosixFilePermissions(target()));
		assertArrayEquals(Files.readAllBytes(alias), Files.readAllBytes(target()));
	}

	@Test
	void firstSaveStillFollowsDanglingSymlink() throws Exception {
		assumePosix();
		Files.createSymbolicLink(target(), Path.of("referent.saver"));
		Checkpoint checkpoint = checkpoint("first");

		saver(0).put(config, checkpoint);

		assertTrue(Files.isSymbolicLink(target()));
		assertEquals(checkpoint.getId(), freshReader().get(config).orElseThrow().getId());
		assertTrue(Files.exists(directory.resolve("referent.saver")));
	}

	@Test
	void readOnlyTargetFailsBeforeChangingOldBytes() throws Exception {
		assumePosix();
		FaultySaver saver = saver(0);
		saver.put(config, checkpoint("old"));
		byte[] before = Files.readAllBytes(target());
		Files.setPosixFilePermissions(target(), PosixFilePermissions.fromString("r--r--r--"));
		assumeTrue(!Files.isWritable(target()), "Current user can still write to the read-only target");

		assertThrows(IOException.class, () -> saver.put(config, checkpoint("new")));

		assertArrayEquals(before, Files.readAllBytes(target()));
		assertOnlyTargetRemains();
	}

	@Test
	void validEmptyHistoryStillAcceptsFirstCheckpoint() throws Exception {
		assumePosix();
		try (var output = new java.io.ObjectOutputStream(Files.newOutputStream(target()))) {
			output.writeInt(0);
		}
		Checkpoint checkpoint = checkpoint("first");

		saver(0).put(config, checkpoint);

		assertEquals(checkpoint.getId(), freshReader().get(config).orElseThrow().getId());
		assertOnlyTargetRemains();
	}

	@Test
	void existingSymlinkRetainsItsReferentAndLinkObject() throws Exception {
		assumePosix();
		FaultySaver saver = saver(0);
		saver.put(config, checkpoint("old"));
		Path referent = directory.resolve("referent.saver");
		Files.move(target(), referent);
		Files.createSymbolicLink(target(), referent.getFileName());
		Object inode = Files.getAttribute(referent, "unix:ino");

		saver.put(config, checkpoint("new"));

		assertTrue(Files.isSymbolicLink(target()));
		assertEquals(inode, Files.getAttribute(referent, "unix:ino"));
		assertEquals(2, freshReader().list(config).size());
	}

	@Test
	void nonPosixCopyFallbackStillWritesExistingTarget() throws Exception {
		Path archive = directory.resolve("control.zip");
		try (var filesystem = java.nio.file.FileSystems.newFileSystem(java.net.URI.create("jar:" + archive.toUri()),
				Map.of("create", "true"))) {
			Path staged = filesystem.getPath("/new");
			Path target = filesystem.getPath("/old");
			Files.writeString(staged, "new image");
			Files.writeString(target, "old image");

			saver(0).replaceFromStaged(staged, target);

			assertEquals("new image", Files.readString(target));
			try (var paths = Files.list(filesystem.getPath("/"))) {
				assertEquals(2, paths.count());
			}
		}
	}

	private void assumePosix() throws IOException {
		assumeTrue(Files.getFileStore(directory).supportsFileAttributeView("posix"));
	}

	private FaultySaver saver(int cacheSize) {
		return new FaultySaver(FileSystemSaver.builder()
			.targetFolder(directory)
			.stateSerializer(new TestSerializer())
			.maxCachedThreads(cacheSize));
	}

	private FileSystemSaver freshReader() {
		return FileSystemSaver.builder()
			.targetFolder(directory)
			.stateSerializer(new TestSerializer())
			.maxCachedThreads(0)
			.build();
	}

	private static Checkpoint checkpoint(String value) {
		return Checkpoint.builder().nodeId("before").nextNodeId("after").state(Map.of("value", value)).build();
	}

	private Path retainedBackup() throws IOException {
		try (var paths = Files.list(directory)) {
			List<Path> backups = paths.filter(x -> x.getFileName().toString().startsWith(".checkpoint-recovery-"))
				.toList();
			assertEquals(1, backups.size());
			return backups.get(0);
		}
	}

	private void assertOnlyTargetRemains() throws IOException {
		try (var paths = Files.list(directory)) {
			assertEquals(List.of(target()), paths.toList());
		}
	}

	private static class TestSerializer extends StateSerializer {

		TestSerializer() {
			super(OverAllState::new);
		}

		@Override
		public void writeData(Map<String, Object> data, ObjectOutput output) throws IOException {
			output.writeObject(data);
		}

		@Override
		@SuppressWarnings("unchecked")
		public Map<String, Object> readData(ObjectInput input) throws IOException, ClassNotFoundException {
			return (Map<String, Object>) input.readObject();
		}

	}

	private static class FaultySaver extends FileSystemSaver {

		private Throwable failure;

		private boolean failRollback;

		private boolean failClose;

		private boolean failedCopy;

		FaultySaver(Builder builder) {
			super(builder);
		}

		@Override
		void copyToCheckpoint(Path source, FileChannel target) throws IOException {
			assertEquals(PosixFilePermissions.fromString("rw-------"), Files.getPosixFilePermissions(source));
			if (failure != null && (!failedCopy || failRollback)) {
				target.write(ByteBuffer.wrap(new byte[] { 1, 2, 3 }));
				if (failedCopy) {
					throw new IOException("rollback fixture");
				}
				failedCopy = true;
				if (failure instanceof IOException io) {
					throw io;
				}
				if (failure instanceof RuntimeException runtime) {
					throw runtime;
				}
				throw (Error) failure;
			}
			super.copyToCheckpoint(source, target);
		}

		@Override
		void closeCheckpoint(FileChannel channel) throws IOException {
			super.closeCheckpoint(channel);
			if (failClose) {
				throw new IOException("uncertain close fixture");
			}
		}

	}

}
