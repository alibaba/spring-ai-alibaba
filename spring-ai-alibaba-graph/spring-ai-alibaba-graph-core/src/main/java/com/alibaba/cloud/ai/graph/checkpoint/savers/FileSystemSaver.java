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
package com.alibaba.cloud.ai.graph.checkpoint.savers;

import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.checkpoint.Checkpoint;
import com.alibaba.cloud.ai.graph.serializer.Serializer;
import com.alibaba.cloud.ai.graph.serializer.StateSerializer;
import com.alibaba.cloud.ai.graph.serializer.check_point.CheckPointSerializer;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.LinkedList;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.String.format;

/**
 * A CheckpointSaver that stores Checkpoints in the filesystem.
 *
 * <p>
 * Each RunnableConfig is associated with a file in the provided targetFolder. The file is
 * named "thread-<i>threadId</i>.saver" if the RunnableConfig has a threadId, or
 * "thread-$default.saver" if it doesn't.
 * </p>
 *
 */
public class FileSystemSaver extends MemorySaver {

	private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(FileSystemSaver.class);

	public static final String EXTENSION = ".saver";

	private final Path targetFolder;

	private final Serializer<Checkpoint> serializer;

	@SuppressWarnings("unchecked")
	public FileSystemSaver(Path targetFolder, StateSerializer stateSerializer) {

		Objects.requireNonNull(stateSerializer, "stateSerializer cannot be null");
		this.targetFolder = Objects.requireNonNull(targetFolder, "targetFolder cannot be null");
		this.serializer = new CheckPointSerializer(stateSerializer);

		File targetFolderAsFile = targetFolder.toFile();

		if (targetFolderAsFile.exists()) {
			if (targetFolderAsFile.isFile()) {
				throw new IllegalArgumentException(format("targetFolder '%s' must be a folder", targetFolder)); // TODO:
																												// format"targetFolder
																												// must
																												// be
																												// a
																												// directory");
			}
		}
		else {
			if (!targetFolderAsFile.mkdirs()) {
				throw new IllegalArgumentException(format("targetFolder '%s' cannot be created", targetFolder)); // TODO:
																													// format"targetFolder
																													// cannot
																													// be
																													// created");
			}
		}

	}

	private String getBaseName(RunnableConfig config) {
		var threadId = config.threadId().orElse(THREAD_ID_DEFAULT);
		return format("thread-%s", threadId);
	}

	private Path getPath(RunnableConfig config) {
		return Paths.get(targetFolder.toString(), getBaseName(config).concat(EXTENSION));
	}

	private File getFile(RunnableConfig config) {
		return getPath(config).toFile();
	}

	private void serialize(LinkedList<Checkpoint> checkpoints, File outFile) throws IOException {
		Objects.requireNonNull(checkpoints, "checkpoints cannot be null");
		Objects.requireNonNull(outFile, "outFile cannot be null");
		try (ObjectOutputStream oos = new ObjectOutputStream(Files.newOutputStream(outFile.toPath()))) {

			oos.writeInt(checkpoints.size());
			for (Checkpoint checkpoint : checkpoints) {
				serializer.write(checkpoint, oos);
			}
		}
	}

	private void deserialize(File file, LinkedList<Checkpoint> result) throws IOException, ClassNotFoundException {
		Objects.requireNonNull(file, "file cannot be null");
		Objects.requireNonNull(result, "result cannot be null");

		try (ObjectInputStream ois = new ObjectInputStream(Files.newInputStream(file.toPath()))) {
			int size = ois.readInt();
			for (int i = 0; i < size; i++) {
				result.add(serializer.read(ois));
			}
		}
	}

	@Override
	protected LinkedList<Checkpoint> loadedCheckpoints(RunnableConfig config, LinkedList<Checkpoint> checkpoints)
			throws Exception {

		File targetFile = getFile(config);
		if (targetFile.exists() && checkpoints.isEmpty()) {
			deserialize(targetFile, checkpoints);
		}
		return checkpoints;

	}

	@Override
	protected void insertedCheckpoint(RunnableConfig config, LinkedList<Checkpoint> checkpoints, Checkpoint checkpoint)
			throws Exception {
		File targetFile = getFile(config);
		serialize(checkpoints, targetFile);
	}

	@Override
	protected void updatedCheckpoint(RunnableConfig config, LinkedList<Checkpoint> checkpoints, Checkpoint checkpoint)
			throws Exception {
		insertedCheckpoint(config, checkpoints, checkpoint);
	}

	/**
	 * Releases the checkpoints associated with the given configuration. This involves
	 * copying the current checkpoint file (e.g., "thread-123.saver") to a versioned
	 * backup file (e.g., "thread-123-v1.saver", "thread-123-v2.saver", etc.) based on
	 * existing versioned files, deleting the original unversioned file, and then clearing
	 * the in-memory checkpoints.
	 * @param config The configuration for which to release checkpoints.
	 * @param checkpoints released checkpoints
	 * @param releaseTag released Tag
	 * @throws Exception If an error occurs during file operations or releasing from
	 * memory.
	 */
	@Override
	protected void releasedCheckpoints(RunnableConfig config, LinkedList<Checkpoint> checkpoints, Tag releaseTag)
			throws Exception {
		var currentPath = getPath(config);

		if (!Files.exists(currentPath)) {
			log.warn("file {} doesn't exist. Skipping file operations.", currentPath);
			return;
		}

		var versionPattern = Pattern.compile(format("%s-v(\\d+)\\%s$", getBaseName(config), EXTENSION));

		int maxVersion = 0;
		try (var stream = Files.list(targetFolder)) {
			maxVersion = stream.map(path -> path.getFileName().toString())
				.map(versionPattern::matcher)
				.filter(Matcher::matches)
				.mapToInt(matcher -> Integer.parseInt(matcher.group(1)))
				.max()
				.orElse(0); // Default to 0 if no versioned files found
		}
		catch (IOException e) {
			log.error(
					"Failed to list directory {} to determine next version number for backup. Skipping file operations.",
					targetFolder, e);
			return;
		}

		int nextVersion = maxVersion + 1;
		var backupFilename = format("%s-v%d%s", getBaseName(config), nextVersion, EXTENSION);
		Path backupPath = targetFolder.resolve(backupFilename);

		Files.copy(currentPath, backupPath, StandardCopyOption.REPLACE_EXISTING);

		Files.delete(currentPath);

	}

	/**
	 * delete the checkpoint file associated with the given RunnableConfig.
	 * @param config the RunnableConfig for which the checkpoint file should be cleared
	 * @return true if the file existed and was successfully deleted, false otherwise
	 */
	public boolean deleteFile(RunnableConfig config) {
		File targetFile = getFile(config);
		return targetFile.exists() && targetFile.delete();
	}

}
