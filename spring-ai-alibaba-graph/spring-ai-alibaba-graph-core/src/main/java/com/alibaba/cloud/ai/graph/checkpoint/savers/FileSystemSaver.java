/*
 * Copyright 2024-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alibaba.cloud.ai.graph.checkpoint.savers;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Optional;

import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.checkpoint.Checkpoint;
import com.alibaba.cloud.ai.graph.serializer.Serializer;
import com.alibaba.cloud.ai.graph.serializer.StateSerializer;
import com.alibaba.cloud.ai.graph.serializer.check_point.CheckPointSerializer;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

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
@Slf4j
public class FileSystemSaver extends MemorySaver {

	private final Path targetFolder;

	private final Serializer<Checkpoint> serializer;

	@SuppressWarnings("unchecked")
	public FileSystemSaver(@NonNull Path targetFolder, @NonNull StateSerializer stateSerializer) {
		File targetFolderAsFile = targetFolder.toFile();

		if (targetFolderAsFile.exists()) {
			if (targetFolderAsFile.isFile()) {
				throw new IllegalArgumentException(format("targetFolder '%s' must be a folder", targetFolder)); // TODO:
			}
		}
		else {
			if (!targetFolderAsFile.mkdirs()) {
				throw new IllegalArgumentException(format("targetFolder '%s' cannot be created", targetFolder)); // TODO:
			}
		}

		this.targetFolder = targetFolder;
		this.serializer = new CheckPointSerializer(stateSerializer);
	}

	private File getFile(RunnableConfig config) {
		return config.threadId()
			.map(threadId -> Paths.get(targetFolder.toString(), format("thread-%s.saver", threadId)))
			.orElseGet(() -> Paths.get(targetFolder.toString(), "thread-$default.saver"))
			.toFile();

	}

	private void serialize(@NonNull LinkedList<Checkpoint> checkpoints, @NonNull File outFile) throws IOException {

		try (ObjectOutputStream oos = new ObjectOutputStream(Files.newOutputStream(outFile.toPath()))) {

			oos.writeInt(checkpoints.size());
			for (Checkpoint checkpoint : checkpoints) {
				serializer.write(checkpoint, oos);
			}
		}
	}

	private void deserialize(@NonNull File file, @NonNull LinkedList<Checkpoint> result)
			throws IOException, ClassNotFoundException {

		try (ObjectInputStream ois = new ObjectInputStream(Files.newInputStream(file.toPath()))) {
			int size = ois.readInt();
			for (int i = 0; i < size; i++) {
				result.add(serializer.read(ois));
			}
		}
	}

	@Override
	protected LinkedList<Checkpoint> getCheckpoints(RunnableConfig config) {
		LinkedList<Checkpoint> result = super.getCheckpoints(config);

		File targetFile = getFile(config);
		if (targetFile.exists() && result.isEmpty()) {
			try {
				deserialize(targetFile, result);
			}
			catch (IOException | ClassNotFoundException e) {
				throw new RuntimeException(e);
			}
		}
		return result;
	}

	/**
	 * Clears the checkpoint file associated with the given RunnableConfig.
	 * @param config the RunnableConfig for which the checkpoint file should be cleared
	 * @return true if the file existed and was successfully deleted, false otherwise
	 */
	@Override
	public boolean clear(RunnableConfig config) {
		File targetFile = getFile(config);
		return targetFile.exists() && targetFile.delete();
	}

	@Override
	public Collection<Checkpoint> list(RunnableConfig config) {
		return super.list(config);
	}

	@Override
	public Optional<Checkpoint> get(RunnableConfig config) {
		return super.get(config);
	}

	@Override
	public RunnableConfig put(RunnableConfig config, Checkpoint checkpoint) throws Exception {
		RunnableConfig result = super.put(config, checkpoint);

		File targetFile = getFile(config);
		serialize(super.getCheckpoints(config), targetFile);
		return result;
	}

}
