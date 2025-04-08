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
