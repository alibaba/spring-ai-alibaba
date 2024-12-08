package com.alibaba.cloud.ai.utils;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @Description:
 * @Author: 肖云涛
 * @Date: 2024/12/8
 */

public class FileUtils {

	private static final Logger logger = Logger.getLogger(FileUtils.class.getName());

	public static void createFileIfNotExists(Path outputPath) throws IOException {
		if (!Files.exists(outputPath)) {
			Files.createFile(outputPath);
		}
	}

	public static List<String> readLines(Path outputPath) throws IOException {
		return Files.readAllLines(outputPath);
	}

}
