/*
 * Copyright 2025 the original author or authors.
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
package com.alibaba.cloud.ai.example.manus.tool.support.llmbash;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BashProcess {

	private static final Logger log = LoggerFactory.getLogger(BashProcess.class);

	/**
	 * 执行命令
	 * @param commandList
	 * @param workingDirectoryPath
	 * @return
	 */
	public static List<String> executeCommand(List<String> commandList, String workingDirectoryPath) {
		return commandList.stream().map(commandLine -> {
			try {
				ProcessBuilder pb = new ProcessBuilder("bash", "-c", commandLine);
				if (!StringUtils.isEmpty(workingDirectoryPath)) {
					pb.directory(new File(workingDirectoryPath));
				}

				// 启动进程
				Process process = pb.start();

				// 获取命令输出
				BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
				StringBuilder builder = new StringBuilder();
				String line;
				while ((line = reader.readLine()) != null) {
					log.warn(line);
					builder.append(line);
					builder.append("\n");
				}

				// 等待命令执行完成
				int exitCode = process.waitFor();

				if (exitCode == 0) {
					log.warn("Bash command executed successfully.");
				}
				else {
					log.error("Failed to execute Bash command.");
				}
				return builder.toString();
			}
			catch (Throwable e) {
				e.printStackTrace();
			}
			return null;
		}).collect(Collectors.toList());
	}

}
