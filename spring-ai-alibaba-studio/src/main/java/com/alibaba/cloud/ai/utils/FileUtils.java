/*
 * Copyright 2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alibaba.cloud.ai.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
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
