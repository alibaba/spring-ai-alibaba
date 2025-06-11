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

package com.alibaba.cloud.ai.graph.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * @author HeYQ
 * @since 2024-11-28 11:47
 */
public class FileUtils {

	private FileUtils() {
		throw new IllegalStateException("Utility class");
	}

	public static void writeCodeToFile(String workDir, String filename, String code) {
		try {
			if (code == null) {
				throw new IllegalArgumentException("Code must not be null");
			}
			Path filepath = Path.of(workDir, filename);
			// ensure the parent directory exists
			Path fileDir = filepath.getParent();
			if (fileDir != null && !Files.exists(fileDir)) {
				Files.createDirectories(fileDir);
			}
			// write the code to the file
			Files.writeString(filepath, code);
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Deletes the file specified by the filename from the provided working directory.
	 * @param workDir The working directory where the file to be deleted is located.
	 * @param filename The name of the file to be deleted.
	 */
	public static void deleteFile(String workDir, String filename) {
		try {
			Path filepath = Path.of(workDir, filename);
			Files.deleteIfExists(filepath);
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Copies the spring-ai-alibaba-graph-core JAR file to the specified working directory.
	 * @param workDir The target working directory where the JAR file will be copied.
	 */
	public static void copyResourceJarToWorkDir(String workDir) {
		try {
			// Get the path of the current JAR file
			String jarPath = FileUtils.class.getProtectionDomain().getCodeSource().getLocation().getPath();
			Path sourcePath = Path.of(jarPath);
			
			// Create target directory if it doesn't exist
			Path targetDir = Path.of(workDir);
			if (!Files.exists(targetDir)) {
				Files.createDirectories(targetDir);
			}
			
			// Copy the JAR file to the working directory
			Path targetPath = targetDir.resolve(sourcePath.getFileName());
			Files.copy(sourcePath, targetPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
		}
		catch (IOException e) {
			throw new RuntimeException("Failed to copy JAR file to working directory", e);
		}
	}

	/**
	 * Deletes the spring-ai-alibaba-graph-core JAR file from the specified working directory.
	 * @param workDir The working directory from which the JAR file will be deleted.
	 */
	public static void deleteResourceJarFromWorkDir(String workDir) {
		try {
			// Get the name of the current JAR file
			String jarPath = FileUtils.class.getProtectionDomain().getCodeSource().getLocation().getPath();
			Path sourcePath = Path.of(jarPath);
			String jarFileName = sourcePath.getFileName().toString();
			
			// Delete the JAR file from the working directory
			Path targetPath = Path.of(workDir, jarFileName);
			Files.deleteIfExists(targetPath);
		}
		catch (IOException e) {
			throw new RuntimeException("Failed to delete JAR file from working directory", e);
		}
	}

}
