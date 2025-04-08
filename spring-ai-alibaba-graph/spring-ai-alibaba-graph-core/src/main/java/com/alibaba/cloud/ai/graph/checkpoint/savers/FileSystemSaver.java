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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

import static java.lang.String.format;

public class FileSystemSaver extends MemorySaver {

	private static final Logger logger = LoggerFactory.getLogger(FileSystemSaver.class);

	private final Path targetFolder;

	private final Serializer<Checkpoint> serializer;

	@SuppressWarnings("unchecked")
	public FileSystemSaver(Path targetFolder, StateSerializer stateSerializer) {
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

	private void serialize(LinkedList<Checkpoint> checkpoints, File outFile) throws IOException {

		try (ObjectOutputStream oos = new ObjectOutputStream(Files.newOutputStream(outFile.toPath()))) {

			oos.writeInt(checkpoints.size());
			for (Checkpoint checkpoint : checkpoints) {
				serializer.write(checkpoint, oos);
			}
		}
	}

	private void deserialize(File file, LinkedList<Checkpoint> result) throws IOException, ClassNotFoundException {

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
