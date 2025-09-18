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
package com.alibaba.cloud.ai.manus.tool.bash;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.List;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Windows command executor implementation
 */
public class WindowsShellExecutor implements ShellCommandExecutor {

	private static final Logger log = LoggerFactory.getLogger(WindowsShellExecutor.class);

	private Process currentProcess;

	private static final int DEFAULT_TIMEOUT = 60; // Default timeout (seconds)

	private BufferedWriter processInput;

	@Override
	public List<String> execute(List<String> commands, String workingDir) {
		return commands.stream().map(command -> {
			try {
				// If empty command, return additional logs from current process
				if (command.trim().isEmpty() && currentProcess != null) {
					return processOutput(currentProcess);
				}

				// If ctrl+c command, send interrupt signal
				if ("ctrl+c".equalsIgnoreCase(command.trim()) && currentProcess != null) {
					terminate();
					return "Process terminated by ctrl+c";
				}

				// Windows background commands need special handling
				if (command.endsWith("&")) {
					command = "start /B " + command.substring(0, command.length() - 1);
				}

				ProcessBuilder pb = new ProcessBuilder("cmd.exe", "/c", command);
				if (!StringUtils.isEmpty(workingDir)) {
					pb.directory(new File(workingDir));
				}

				// Windows-specific environment variable setup
				pb.environment().put("PATHEXT", ".COM;.EXE;.BAT;.CMD");
				pb.environment().put("SystemRoot", System.getenv("SystemRoot"));

				currentProcess = pb.start();
				processInput = new BufferedWriter(new OutputStreamWriter(currentProcess.getOutputStream()));

				// Set timeout handling
				try {
					if (!command.startsWith("start /B")) { // Only set timeout for
															// non-background commands
						if (!currentProcess.waitFor(DEFAULT_TIMEOUT, TimeUnit.SECONDS)) {
							log.warn("Command timed out. Sending termination signal to the process");
							terminate();
							// Retry command in background
							command = "start /B " + command;
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
				log.error("Exception executing Windows command", e);
				return "Error: " + e.getClass().getSimpleName() + " - " + e.getMessage();
			}
		}).collect(Collectors.toList());
	}

	@Override
	public void terminate() {
		if (currentProcess != null && currentProcess.isAlive()) {
			try {
				// Windows uses taskkill command to ensure process and its child processes
				// are terminated
				Runtime.getRuntime().exec("taskkill /F /T /PID " + currentProcess.pid());
				// Wait for process termination
				if (!currentProcess.waitFor(5, TimeUnit.SECONDS)) {
					currentProcess.destroyForcibly();
				}
			}
			catch (Exception e) {
				log.error("Error terminating Windows process", e);
				currentProcess.destroyForcibly();
			}
			log.info("Windows process terminated");
		}
	}

	private String processOutput(Process process) throws IOException, InterruptedException {
		StringBuilder outputBuilder = new StringBuilder();
		StringBuilder errorBuilder = new StringBuilder();

		// Read standard output
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), "GBK"))) { // Windows
																													// uses
																													// GBK
																													// encoding
																													// by
																													// default
			String line;
			while ((line = reader.readLine()) != null) {
				log.info(line);
				outputBuilder.append(line).append("\n");
			}
		}

		// Read error output
		try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream(), "GBK"))) {
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
