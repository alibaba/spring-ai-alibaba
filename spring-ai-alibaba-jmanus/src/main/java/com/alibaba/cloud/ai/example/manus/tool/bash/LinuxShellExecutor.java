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
package com.alibaba.cloud.ai.example.manus.tool.bash;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.List;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Linux命令执行器实现
 */
public class LinuxShellExecutor implements ShellCommandExecutor {

	private static final Logger log = LoggerFactory.getLogger(LinuxShellExecutor.class);

	private Process currentProcess;

	private static final int DEFAULT_TIMEOUT = 60; // 默认超时时间(秒)

	private BufferedWriter processInput;

	@Override
	public List<String> execute(List<String> commands, String workingDir) {
		return commands.stream().map(command -> {
			try {
				// 如果是空命令，返回当前进程的额外日志
				if (command.trim().isEmpty() && currentProcess != null) {
					return processOutput(currentProcess);
				}

				// 如果是ctrl+c命令，发送中断信号
				if ("ctrl+c".equalsIgnoreCase(command.trim()) && currentProcess != null) {
					terminate();
					return "Process terminated by ctrl+c";
				}

				ProcessBuilder pb = new ProcessBuilder("/bin/bash", "-c", command);
				if (!StringUtils.isEmpty(workingDir)) {
					pb.directory(new File(workingDir));
				}

				// 设置Linux环境变量
				pb.environment().put("LANG", "en_US.UTF-8");
				pb.environment().put("SHELL", "/bin/bash");
				pb.environment().put("PATH", System.getenv("PATH") + ":/usr/local/bin");

				currentProcess = pb.start();
				processInput = new BufferedWriter(new OutputStreamWriter(currentProcess.getOutputStream()));

				// 设置超时处理
				try {
					if (!command.endsWith("&")) { // 不是后台命令才设置超时
						if (!currentProcess.waitFor(DEFAULT_TIMEOUT, TimeUnit.SECONDS)) {
							log.warn("Command timed out. Sending SIGINT to the process");
							terminate();
							// 在后台重试命令
							if (!command.endsWith("&")) {
								command += " &";
							}
							return execute(Collections.singletonList(command), workingDir).get(0);
						}
					}
					return processOutput(currentProcess);
				}
				catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					return "Error: Process interrupted - " + e.getMessage();
				}
			}
			catch (Throwable e) {
				log.error("Exception executing Linux command", e);
				return "Error: " + e.getClass().getSimpleName() + " - " + e.getMessage();
			}
		}).collect(Collectors.toList());
	}

	@Override
	public void terminate() {
		if (currentProcess != null && currentProcess.isAlive()) {
			// 首先尝试发送SIGINT (ctrl+c)
			currentProcess.destroy();
			try {
				// 等待进程响应SIGINT
				if (!currentProcess.waitFor(5, TimeUnit.SECONDS)) {
					// 如果进程没有响应SIGINT，强制终止
					currentProcess.destroyForcibly();
				}
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				currentProcess.destroyForcibly();
			}
			log.info("Linux process terminated");
		}
	}

	private String processOutput(Process process) throws IOException, InterruptedException {
		StringBuilder outputBuilder = new StringBuilder();
		StringBuilder errorBuilder = new StringBuilder();

		// 读取标准输出
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), "UTF-8"))) {
			String line;
			while ((line = reader.readLine()) != null) {
				log.info(line);
				outputBuilder.append(line).append("\n");
			}
		}

		// 读取错误输出
		try (BufferedReader errorReader = new BufferedReader(
				new InputStreamReader(process.getErrorStream(), "UTF-8"))) {
			String line;
			while ((line = errorReader.readLine()) != null) {
				log.error(line);
				errorBuilder.append(line).append("\n");
			}
		}

		int exitCode = process.isAlive() ? -1 : process.exitValue();
		if (exitCode == 0) {
			return outputBuilder.toString();
		}
		else if (exitCode == -1) {
			return "Process is still running. Use empty command to get more logs, or 'ctrl+c' to terminate.";
		}
		else {
			return "Error (Exit Code " + exitCode + "): "
					+ (errorBuilder.length() > 0 ? errorBuilder.toString() : outputBuilder.toString());
		}
	}

}
