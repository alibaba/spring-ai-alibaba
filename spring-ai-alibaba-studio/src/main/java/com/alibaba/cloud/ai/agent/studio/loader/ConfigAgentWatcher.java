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
package com.alibaba.cloud.ai.agent.studio.loader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * File watcher for monitoring changes to YAML configuration files in agent directories.
 *
 * <p>This class monitors individual agent folders containing `root_agent.yaml` files and triggers
 * callbacks when files are created, modified, or deleted.
 *
 * <p>The watcher polls for changes at regular intervals rather than using native filesystem events
 * for better cross-platform compatibility.
 */
class ConfigAgentWatcher {
	private static final Logger logger = LoggerFactory.getLogger(ConfigAgentWatcher.class);

	private final Map<Path, ChangeCallback> watchedFolders = new ConcurrentHashMap<>();
	private final Map<Path, Map<Path, Long>> watchedYamlFiles = new ConcurrentHashMap<>();
	private final ScheduledExecutorService fileWatcher = Executors.newSingleThreadScheduledExecutor();
	private volatile boolean started = false;

	/**
	 * Starts watching for file changes.
	 *
	 * @throws IllegalStateException if the watcher is already started
	 */
	synchronized void start() {
		if (started) {
			throw new IllegalStateException("ConfigAgentWatcher is already started");
		}

		logger.info("Starting ConfigAgentWatcher");
		fileWatcher.scheduleAtFixedRate(this::checkForChanges, 2, 2, TimeUnit.SECONDS);
		started = true;

		Runtime.getRuntime().addShutdownHook(new Thread(this::stop));
		logger.info(
				"ConfigAgentWatcher started successfully. Watching {} folders.", watchedFolders.size());
	}

	/** Stops the file watcher. */
	synchronized void stop() {
		if (!started) {
			return;
		}

		logger.info("Stopping ConfigAgentWatcher...");
		fileWatcher.shutdown();
		try {
			if (!fileWatcher.awaitTermination(5, TimeUnit.SECONDS)) {
				fileWatcher.shutdownNow();
			}
		}
		catch (InterruptedException e) {
			fileWatcher.shutdownNow();
			Thread.currentThread().interrupt();
		}
		started = false;
		logger.info("ConfigAgentWatcher stopped.");
	}

	/**
	 * Adds a folder to be watched for changes to any YAML files within it.
	 *
	 * @param agentDirPath The path to the agent configuration directory
	 * @param callback The callback to invoke when changes are detected
	 * @throws IllegalArgumentException if the folder doesn't exist
	 */
	void watch(Path agentDirPath, ChangeCallback callback) {
		if (!Files.isDirectory(agentDirPath)) {
			throw new IllegalArgumentException("Config folder does not exist: " + agentDirPath);
		}

		watchedFolders.put(agentDirPath, callback);

		// Scan and track all YAML files in the directory
		Map<Path, Long> yamlFiles = scanYamlFiles(agentDirPath);
		watchedYamlFiles.put(agentDirPath, yamlFiles);

		logger.debug("Now watching {} YAML files in agent folder: {}", yamlFiles.size(), agentDirPath);
	}

	/**
	 * Scans a directory recursively for all YAML files and returns their last modified times.
	 *
	 * @param agentDirPath The directory to scan recursively
	 * @return A map of YAML file paths to their last modified times
	 */
	private Map<Path, Long> scanYamlFiles(Path agentDirPath) {
		Map<Path, Long> yamlFiles = new HashMap<>();
		try (Stream<Path> files = Files.walk(agentDirPath)) {
			files
					.filter(Files::isRegularFile)
					.filter(
							path ->
									path.toString().toLowerCase().endsWith(".yaml")
											|| path.toString().toLowerCase().endsWith(".yml"))
					.forEach(
							yamlFile -> {
								long lastModified = getLastModified(yamlFile);
								yamlFiles.put(yamlFile, lastModified);
								logger.trace("Found YAML file: {} (modified: {})", yamlFile, lastModified);
							});
		}
		catch (IOException e) {
			logger.warn("Failed to scan YAML files in: {}", agentDirPath, e);
		}
		return yamlFiles;
	}

	/**
	 * Returns whether the watcher is currently running.
	 *
	 * @return true if the watcher is started, false otherwise
	 */
	public boolean isStarted() {
		return started;
	}

	/** Checks all watched files for changes and triggers callbacks if needed. */
	private void checkForChanges() {
		for (Map.Entry<Path, ChangeCallback> entry : new HashMap<>(watchedFolders).entrySet()) {
			Path agentDirPath = entry.getKey();
			ChangeCallback callback = entry.getValue();

			try {
				checkDirectoryForChanges(agentDirPath, callback);
			}
			catch (Exception e) {
				logger.error("Error checking directory for changes: {}", agentDirPath, e);
			}
		}
	}

	/**
	 * Checks a specific agent directory for YAML file changes.
	 *
	 * @param agentDirPath The agent directory to check
	 * @param callback The callback for this directory
	 */
	private void checkDirectoryForChanges(Path agentDirPath, ChangeCallback callback) {
		if (!Files.isDirectory(agentDirPath)) {
			// Directory was deleted
			handleDirectoryDeleted(agentDirPath);
			return;
		}

		Map<Path, Long> currentYamlFiles = watchedYamlFiles.get(agentDirPath);
		if (currentYamlFiles == null) {
			return; // No tracked files for this directory
		}

		// Scan current YAML files in the directory
		Map<Path, Long> freshYamlFiles = scanYamlFiles(agentDirPath);
		boolean hasChanges = false;

		// Check for new or modified files
		for (Map.Entry<Path, Long> freshEntry : freshYamlFiles.entrySet()) {
			Path yamlFile = freshEntry.getKey();
			long currentModified = freshEntry.getValue();
			Long previousModified = currentYamlFiles.get(yamlFile);

			if (previousModified == null) {
				// New file
				logger.info("Detected new YAML file: {}", yamlFile);
				hasChanges = true;
			}
			else if (currentModified > previousModified) {
				// Modified file
				logger.info("Detected change in YAML file: {}", yamlFile);
				hasChanges = true;
			}
		}

		// Check for deleted files
		for (Path trackedFile : currentYamlFiles.keySet()) {
			if (!freshYamlFiles.containsKey(trackedFile)) {
				logger.info("Detected deleted YAML file: {}", trackedFile);
				hasChanges = true;
			}
		}

		// Update tracked files and trigger callback if there were changes
		if (hasChanges) {
			watchedYamlFiles.put(agentDirPath, freshYamlFiles);
			callback.onConfigChanged(agentDirPath);
		}
	}

	/**
	 * Handles the deletion of a watched agent directory.
	 *
	 * @param agentDirPath The path of the deleted agent directory
	 */
	private void handleDirectoryDeleted(Path agentDirPath) {
		logger.info("Agent directory deleted: {}", agentDirPath);
		ChangeCallback callback = watchedFolders.remove(agentDirPath);
		watchedYamlFiles.remove(agentDirPath);

		if (callback != null) {
			callback.onConfigChanged(agentDirPath);
		}
	}

	/**
	 * Gets the last modified time of a file, handling potential I/O errors.
	 *
	 * @param path The file path
	 * @return The last modified time in milliseconds, or 0 if there's an error
	 */
	private long getLastModified(Path path) {
		try {
			return Files.getLastModifiedTime(path).toMillis();
		}
		catch (IOException e) {
			logger.warn("Could not get last modified time for: {}", path, e);
			return 0;
		}
	}

	/** Callback interface for handling file change events. */
	@FunctionalInterface
	interface ChangeCallback {
		/**
		 * Called when a watched YAML file changes, is created, or is deleted.
		 *
		 * @param agentDirPath The path to the agent configuration directory
		 */
		void onConfigChanged(Path agentDirPath);
	}
}
