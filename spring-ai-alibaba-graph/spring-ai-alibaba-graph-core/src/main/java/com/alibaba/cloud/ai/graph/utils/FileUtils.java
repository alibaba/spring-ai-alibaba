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

import lombok.SneakyThrows;

/**
 * @author HeYQ
 * @since 2024-11-28 11:47
 */
public class FileUtils {

	private FileUtils() {
		throw new IllegalStateException("Utility class");
	}

	/**
	 * Writes the given code string to a file specified by the filename. The file is
	 * created in the provided working directory. Intermediate directories in the path
	 * will be created if they do not exist.
	 * @param workDir The working directory where the file needs to be created.
	 * @param filename The name of the file to write the code to.
	 * @param code The code to write to the file. Must not be null.
	 */
	@SneakyThrows(IOException.class)
	public static void writeCodeToFile(String workDir, String filename, String code) {
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

	/**
	 * Deletes the file specified by the filename from the provided working directory.
	 * @param workDir The working directory where the file to be deleted is located.
	 * @param filename The name of the file to be deleted.
	 */
	@SneakyThrows(IOException.class)
	public static void deleteFile(String workDir, String filename) {
		Path filepath = Path.of(workDir, filename);
		Files.deleteIfExists(filepath);
	}

}
