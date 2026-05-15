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

package com.alibaba.cloud.ai.graph.utils;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.Enumeration;

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
	 * Copies all JAR files from the resources/lib directory to the specified working
	 * directory.
	 * @param workDir The target working directory where the JAR files will be copied.
	 */
	public static void copyResourceJarToWorkDir(String workDir) {
		copyResourceJarToWorkDir(workDir, FileUtils.class.getClassLoader());
	}

	/**
	 * Copies all JAR files from the resources/lib directory (including resources packaged
	 * inside dependency JARs) to the specified working directory.
	 * @param workDir The target working directory where the JAR files will be copied.
	 * @param classLoader The class loader used to locate lib resources.
	 */
	static void copyResourceJarToWorkDir(String workDir, ClassLoader classLoader) {
		try {
			Path targetDir = Path.of(workDir);
			if (!Files.exists(targetDir)) {
				Files.createDirectories(targetDir);
			}

			Enumeration<URL> libUrls = classLoader.getResources("lib");
			if (!libUrls.hasMoreElements()) {
				return;
			}

			while (libUrls.hasMoreElements()) {
				URL libUrl = libUrls.nextElement();
				if ("file".equals(libUrl.getProtocol())) {
					copyJarFilesFromDir(Path.of(libUrl.toURI()), targetDir);
					continue;
				}
				if ("jar".equals(libUrl.getProtocol())) {
					String jarUrl = libUrl.toString();
					int separatorIndex = jarUrl.indexOf("!/");
					if (separatorIndex <= 0) {
						continue;
					}
					URI jarFileUri = URI.create(jarUrl.substring(0, separatorIndex));
					try (FileSystem fs = FileSystems.newFileSystem(jarFileUri, Collections.emptyMap())) {
						copyJarFilesFromDir(fs.getPath("/lib"), targetDir);
					}
				}
			}
		}
		catch (Exception e) {
			throw new RuntimeException("Failed to copy JAR files to working directory", e);
		}
	}

	/**
	 * Copies all JAR files under the source directory to the target directory.
	 * @param sourceDir The source directory containing JAR files.
	 * @param targetDir The target directory where JAR files will be copied.
	 * @throws IOException if I/O operations fail while scanning or copying files.
	 */
	private static void copyJarFilesFromDir(Path sourceDir, Path targetDir) throws IOException {
		if (!Files.exists(sourceDir)) {
			return;
		}
		try (var stream = Files.walk(sourceDir)) {
			stream.filter(path -> path.toString().endsWith(".jar")).forEach(jarPath -> {
				try {
					Path targetPath = targetDir.resolve(jarPath.getFileName().toString());
					Files.copy(jarPath, targetPath, StandardCopyOption.REPLACE_EXISTING);
				}
				catch (IOException e) {
					throw new RuntimeException("Failed to copy JAR file: " + jarPath, e);
				}
			});
		}
	}

	/**
	 * Deletes all JAR files from the specified working directory.
	 * @param workDir The working directory from which the JAR files will be deleted.
	 */
	public static void deleteResourceJarFromWorkDir(String workDir) {
		try {
			Path workDirPath = Path.of(workDir);
			if (Files.exists(workDirPath)) {
				try (var stream = Files.walk(workDirPath)) {
					stream.filter(path -> path.toString().endsWith(".jar")).forEach(jarPath -> {
						try {
							Files.deleteIfExists(jarPath);
						}
						catch (IOException e) {
							throw new RuntimeException("Failed to delete JAR file: " + jarPath, e);
						}
					});
				}
			}
		}
		catch (IOException e) {
			throw new RuntimeException("Failed to delete JAR files from working directory", e);
		}
	}

}
