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
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.checkpoint.BaseCheckpointSaver;
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
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

import static java.lang.String.format;

/**
 * A CheckpointSaver that stores Checkpoints in the filesystem.
 * <p>
 * Each RunnableConfig is associated with a file in the provided targetFolder. The file is
 * named "thread-<i>threadId</i>.saver" if the RunnableConfig has a threadId, or
 * "thread-$default.saver" if it doesn't.
 * </p>
 * <p>
 * Full checkpoint history is loaded from the file on demand. In memory this saver keeps
 * only a bounded latest-checkpoint cache.
 * </p>
 */
public class FileSystemSaver implements BaseCheckpointSaver {

	public static final String EXTENSION = ".saver";
	private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(FileSystemSaver.class);
	private final Path targetFolder;

	private final Serializer<Checkpoint> serializer;

	private final Map<String, Checkpoint> latestCheckpointCache;
	private final ReentrantLock lock = new ReentrantLock();
	private final int maxCachedThreads;

	/**
	 * Protected constructor for FileSystemSaver.
	 * Use {@link #builder()} to create instances.
	 * @param targetFolder the directory where checkpoint files are stored
	 * @param stateSerializer the serializer used to persist checkpoint state
	 */
	protected FileSystemSaver(Path targetFolder, StateSerializer stateSerializer) {
		this(new Builder()
				.targetFolder(targetFolder)
				.stateSerializer(stateSerializer));
	}

	@SuppressWarnings("unchecked")
	protected FileSystemSaver(Builder builder) {
		StateSerializer stateSerializer = builder.stateSerializer;
		if (stateSerializer == null) {
			this.serializer = new CheckPointSerializer(StateGraph.DEFAULT_JACKSON_SERIALIZER);
		}
		else {
			this.serializer = new CheckPointSerializer(stateSerializer);
		}
		this.targetFolder = Objects.requireNonNull(builder.targetFolder, "targetFolder cannot be null");
		this.maxCachedThreads = builder.maxCachedThreads;
		this.latestCheckpointCache = createLatestCheckpointCache(builder.maxCachedThreads);

		try {
			if (Files.exists(this.targetFolder) && !Files.isDirectory(this.targetFolder)) {
				throw new IllegalArgumentException(format("targetFolder '%s' must be a directory", this.targetFolder));
			}
			Files.createDirectories(this.targetFolder);
		}
		catch (IOException ex) {
			throw new IllegalArgumentException(format("targetFolder '%s' cannot be created", this.targetFolder), ex);
		}

	}

	/**
	 * Creates a new builder for FileSystemSaver.
	 * @return a new Builder instance
	 */
	public static Builder builder() {
		return new Builder();
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

	private String getThreadId(RunnableConfig config) {
		return config.threadId().orElse(THREAD_ID_DEFAULT);
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

	private LinkedList<Checkpoint> deserialize(File file) throws IOException, ClassNotFoundException {
		LinkedList<Checkpoint> checkpoints = new LinkedList<>();
		if (file.exists()) {
			deserialize(file, checkpoints);
		}
		return checkpoints;
	}

	private Optional<Checkpoint> deserializeLatest(File file) throws IOException, ClassNotFoundException {
		if (!file.exists()) {
			return Optional.empty();
		}

		try (ObjectInputStream ois = new ObjectInputStream(Files.newInputStream(file.toPath()))) {
			int size = ois.readInt();
			if (size == 0) {
				return Optional.empty();
			}
			return Optional.of(serializer.read(ois));
		}
	}

	private Optional<Checkpoint> deserializeCheckpoint(File file, String checkpointId)
			throws IOException, ClassNotFoundException {
		if (!file.exists()) {
			return Optional.empty();
		}

		try (ObjectInputStream ois = new ObjectInputStream(Files.newInputStream(file.toPath()))) {
			int size = ois.readInt();
			for (int i = 0; i < size; i++) {
				Checkpoint checkpoint = serializer.read(ois);
				if (checkpoint.getId().equals(checkpointId)) {
					return Optional.of(checkpoint);
				}
			}
			return Optional.empty();
		}
	}

	private LinkedList<Checkpoint> loadCheckpoints(RunnableConfig config) throws IOException, ClassNotFoundException {
		return deserialize(getFile(config));
	}

	private void insertCheckpoint(RunnableConfig config, Checkpoint checkpoint) throws Exception {
		LinkedList<Checkpoint> checkpoints = loadCheckpoints(config);
		checkpoints.push(checkpoint);
		File targetFile = getFile(config);
		serialize(checkpoints, targetFile);
	}

	private void updateCheckpoint(RunnableConfig config, String checkpointId, Checkpoint checkpoint) throws Exception {
		LinkedList<Checkpoint> checkpoints = loadCheckpoints(config);
		int index = IntStream.range(0, checkpoints.size())
				.filter(i -> checkpoints.get(i).getId().equals(checkpointId))
				.findFirst()
				.orElseThrow(() -> new NoSuchElementException(format("Checkpoint with id %s not found!", checkpointId)));
		checkpoints.set(index, checkpoint);
		serialize(checkpoints, getFile(config));
	}

	/**
	 * Lists active checkpoints for the configured thread.
	 */
	@Override
	public Collection<Checkpoint> list(RunnableConfig config) {
		lock.lock();
		try {
			LinkedList<Checkpoint> checkpoints = loadCheckpoints(config);
			if (!checkpoints.isEmpty()) {
				cacheLatest(getThreadId(config), checkpoints.peek());
			}
			return Collections.unmodifiableCollection(checkpoints);
		}
		catch (Exception ex) {
			throw new RuntimeException(ex);
		}
		finally {
			lock.unlock();
		}
	}

	/**
	 * Gets a checkpoint for the configured thread.
	 */
	@Override
	public Optional<Checkpoint> get(RunnableConfig config) {
		lock.lock();
		try {
			String threadId = getThreadId(config);
			File targetFile = getFile(config);
			if (config.checkPointId().isPresent()) {
				return deserializeCheckpoint(targetFile, config.checkPointId().get());
			}

			Optional<Checkpoint> cached = getCachedLatest(threadId);
			if (cached.isPresent()) {
				return cached;
			}

			Optional<Checkpoint> latest = deserializeLatest(targetFile);
			latest.ifPresent(checkpoint -> cacheLatest(threadId, checkpoint));
			return latest;
		}
		catch (Exception ex) {
			throw new RuntimeException(ex);
		}
		finally {
			lock.unlock();
		}
	}

	/**
	 * Inserts or updates a checkpoint.
	 */
	@Override
	public RunnableConfig put(RunnableConfig config, Checkpoint checkpoint) throws Exception {
		lock.lock();
		try {
			String threadId = getThreadId(config);
			if (config.checkPointId().isPresent()) {
				String checkpointId = config.checkPointId().get();
				updateCheckpoint(config, checkpointId, checkpoint);
				getCachedLatest(threadId)
						.filter(latest -> latest.getId().equals(checkpointId))
						.ifPresent(latest -> cacheLatest(threadId, checkpoint));
				return config;
			}

			insertCheckpoint(config, checkpoint);
			cacheLatest(threadId, checkpoint);
			return RunnableConfig.builder(config)
					.checkPointId(checkpoint.getId())
					.build();
		}
		finally {
			lock.unlock();
		}
	}

	/**
	 * Releases the active thread and returns the released checkpoints.
	 */
	@Override
	public Tag release(RunnableConfig config) throws Exception {
		lock.lock();
		try {
			String threadId = getThreadId(config);
			LinkedList<Checkpoint> checkpoints = loadCheckpoints(config);
			releaseFile(config);
			removeCachedLatest(threadId);
			return new Tag(threadId, checkpoints);
		}
		finally {
			lock.unlock();
		}
	}

	/**
	 * Releases the checkpoints associated with the given configuration. This involves
	 * copying the current checkpoint file (e.g., "thread-123.saver") to a versioned
	 * backup file (e.g., "thread-123-v1.saver", "thread-123-v2.saver", etc.) based on
	 * existing versioned files, deleting the original unversioned file, and then clearing
	 * the in-memory checkpoints.
	 * @param config The configuration for which to release checkpoints.
	 * @throws Exception If an error occurs during file operations or releasing from
	 * memory.
	 */
	private void releaseFile(RunnableConfig config) throws Exception {
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
	 * Delete the checkpoint file associated with the given RunnableConfig.
	 * @param config the RunnableConfig for which the checkpoint file should be cleared
	 * @return true if the file existed and was successfully deleted, false otherwise
	 */
	public boolean deleteFile(RunnableConfig config) {
		Path path = getPath(config);
		try {
			boolean deleted = Files.deleteIfExists(path);
			removeCachedLatest(getThreadId(config));
			return deleted;
		}
		catch (IOException e) {
			log.warn("Failed to delete checkpoint file {}", path, e);
			return false;
		}
	}

	/**
	 * Creates a bounded LRU cache for latest checkpoints.
	 */
	private static Map<String, Checkpoint> createLatestCheckpointCache(int maxCachedThreads) {
		if (maxCachedThreads == 0) {
			return Collections.emptyMap();
		}
		return new LinkedHashMap<>(16, 0.75f, true) {
			@Override
			protected boolean removeEldestEntry(Map.Entry<String, Checkpoint> eldest) {
				return size() > maxCachedThreads;
			}
		};
	}

	private Optional<Checkpoint> getCachedLatest(String threadId) {
		if (maxCachedThreads == 0) {
			return Optional.empty();
		}
		return Optional.ofNullable(latestCheckpointCache.get(threadId));
	}

	private void cacheLatest(String threadId, Checkpoint checkpoint) {
		if (maxCachedThreads > 0) {
			latestCheckpointCache.put(threadId, checkpoint);
		}
	}

	private void removeCachedLatest(String threadId) {
		if (maxCachedThreads > 0) {
			latestCheckpointCache.remove(threadId);
		}
	}

	/**
	 * Builder class for FileSystemSaver.
	 */
	public static class Builder {
		private Path targetFolder;
		private StateSerializer stateSerializer;
		private int maxCachedThreads = 1024;

		public Builder targetFolder(Path targetFolder) {
			this.targetFolder = targetFolder;
			return this;
		}

		public Builder stateSerializer(StateSerializer stateSerializer) {
			this.stateSerializer = stateSerializer;
			return this;
		}

		/**
		 * Sets the maximum number of latest checkpoints retained in memory.
		 * @param maxCachedThreads max cached threads, or 0 to disable the cache
		 * @return this builder
		 */
		public Builder maxCachedThreads(int maxCachedThreads) {
			if (maxCachedThreads < 0) {
				throw new IllegalArgumentException("maxCachedThreads must be greater than or equal to 0");
			}
			this.maxCachedThreads = maxCachedThreads;
			return this;
		}

		/**
		 * Builds a new FileSystemSaver instance.
		 * @return a new FileSystemSaver instance
		 * @throws IllegalArgumentException if targetFolder is null
		 */
		public FileSystemSaver build() {
			return new FileSystemSaver(this);
		}
	}

}
