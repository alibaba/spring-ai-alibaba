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

}
