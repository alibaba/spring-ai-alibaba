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

package com.alibaba.cloud.ai.example.deepresearch.tool;

import com.alibaba.cloud.ai.example.deepresearch.config.PythonCoderProperties;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.ref.Cleaner;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.logging.Logger;

/**
 * Run Python Code in Docker
 *
 * @author vlsmb
 */
@Service
public class PythonReplTool {

	private static final Logger logger = Logger.getLogger(PythonReplTool.class.getName());

	private final PythonCoderProperties coderProperties;

	public PythonReplTool(PythonCoderProperties coderProperties) {
		this.coderProperties = coderProperties;
	}

	@SneakyThrows
	@Tool(description = "Execute Python code and return the result.")
	public String executePythonCode(@ToolParam(description = "python code") String code,
			@ToolParam(description = "requirements.txt", required = false) String requirements) {
		if (code == null || code.trim().isEmpty()) {
			return "Error: Code must be a non-empty string.";
		}
		if (!StringUtils.hasText(coderProperties.getContainNamePrefix())
				|| !StringUtils.hasText(coderProperties.getCpuCore())
				|| !StringUtils.hasText(coderProperties.getLimitMemory())
				|| !StringUtils.hasText(coderProperties.getCodeTimeout())
				|| !StringUtils.hasText(coderProperties.getImageName())) {
			return "Error: Some Config is not set. You should reporter it to developer.";
		}
		try {
			// Create temp dir and files
			Path tempDir = Files.createTempDirectory(coderProperties.getContainNamePrefix());
			Cleaner.create().register(tempDir, () -> {
				try {
					if (!Files.exists(tempDir))
						return;
					Files.walkFileTree(tempDir, new SimpleFileVisitor<>() {
						@NotNull
						@Override
						public FileVisitResult visitFile(@NotNull Path file, @NotNull BasicFileAttributes attrs)
								throws IOException {
							Files.delete(file);
							return super.visitFile(file, attrs);
						}

						@NotNull
						@Override
						public FileVisitResult postVisitDirectory(@NotNull Path dir, @Nullable IOException exc)
								throws IOException {
							if (exc != null)
								throw exc;
							Files.delete(dir);
							return super.postVisitDirectory(dir, exc);
						}
					});
				}
				catch (IOException e) {
					logger.warning("Error when deleting temp directory: {}" + tempDir.toAbsolutePath());
				}
			});
			Path requirementsPath = tempDir.resolve("requirements.txt");
			Path scriptPath = tempDir.resolve("script.py");
			Files.createFile(requirementsPath);
			Files.createFile(scriptPath);
			Files.write(requirementsPath, StringUtils.hasText(requirements) ? requirements.getBytes() : "".getBytes());
			Files.write(scriptPath, code.getBytes());

			// Build a docker to run
			String containName = tempDir.getFileName().toString();
			String tempDirPath = tempDir.toAbsolutePath().toString();

			if (!coderProperties.isEnableNetwork() && StringUtils.hasText(requirements)) {
				// If Python code is restricted from network access but requires
				// third-party dependencies, we need to provision a docker for pip to
				// install the dependencies.
				List<String> pipCommands = List.of("docker", "run", "--rm", "--name", containName, "-v",
						String.format("%s/requirements.txt:/app/requirements.txt:ro", tempDirPath), "-v",
						String.format("%s/tmp:/tmp", tempDirPath), "-v",
						String.format("%s/dependency:/app/site-packages", tempDirPath), "-w", "/app",
						coderProperties.getImageName(), "sh", "-c",
						"pip3 install --target=/app/site-packages --no-cache-dir -r requirements.txt > /dev/null");
				ProcessBuilder pipPb = new ProcessBuilder(pipCommands);
				Process pipProcess = pipPb.start();
				BufferedReader pipError = new BufferedReader(new InputStreamReader(pipProcess.getErrorStream()));
				if (pipProcess.waitFor() != 0) {
					StringBuilder error = new StringBuilder();
					String line;
					while ((line = pipError.readLine()) != null) {
						error.append(line).append("\n");
					}
					return "Error installing requirements:\n```\n" + code + "\n```\nError:\n" + error;
				}
			}

			/*
			 * Command Sample: docker run --rm --name py-runner --memory="100M" \
			 * --cpus="0.5" --network none --read-only --cap-drop=ALL -v \
			 * "./script.py:/app/script.py:ro" -v \
			 * "./requirements.txt:/app/requirements.txt:ro" -v "./tmp:/tmp" -v \
			 * "./dependency:/app/site-packages" -w /app python:3-slim sh -c "pip3 \
			 * install --target=/app/site-packages --no-cache-dir -r requirements.txt > \
			 * /dev/null && export PYTHONPATH="/app/site-packages:$PYTHONPATH" && \
			 * timeout -s SIGKILL 60s python3 script.py"
			 */
			List<String> commands = List.of("docker", "run", "--rm", "--name", containName,
					String.format("--memory=%s", coderProperties.getLimitMemory()),
					String.format("--cpus=%s", coderProperties.getCpuCore()), "--network",
					coderProperties.isEnableNetwork() ? "bridge" : "none", "--read-only", "--cap-drop=ALL", "-v",
					String.format("%s/script.py:/app/script.py:ro", tempDirPath), "-v",
					String.format("%s/requirements.txt:/app/requirements.txt:ro", tempDirPath), "-v",
					String.format("%s/tmp:/tmp", tempDirPath), "-v",
					String.format("%s/dependency:/app/site-packages", tempDirPath), "-w", "/app",
					coderProperties.getImageName(), "sh", "-c",
					String.format(((coderProperties.isEnableNetwork() && StringUtils.hasText(requirements))
							? "pip3 install --target=/app/site-packages --no-cache-dir -r requirements.txt > /dev/null && "
							: "")
							+ "export PYTHONPATH=\"/app/site-packages:$PYTHONPATH\" && timeout -s SIGKILL %s python3 script.py",
							coderProperties.getCodeTimeout()));
			ProcessBuilder pb = new ProcessBuilder(commands);
			Process process = pb.start();

			// get stdout and stderr
			BufferedReader stdOutput = new BufferedReader(new InputStreamReader(process.getInputStream()));
			BufferedReader stdError = new BufferedReader(new InputStreamReader(process.getErrorStream()));
			int exitCode = process.waitFor();
			StringBuilder output = new StringBuilder();
			String line;
			while ((line = stdOutput.readLine()) != null) {
				output.append(line).append("\n");
			}
			StringBuilder error = new StringBuilder();
			while ((line = stdError.readLine()) != null) {
				error.append(line).append("\n");
			}
			if (exitCode == 0) {
				logger.info("Python code executed successfully.");
				return "Successfully executed:\n```\n" + code + "\n```\nStdout:\n" + output;
			}
			else {
				logger.warning("Python code execution failed.");
				return "Error executing code:\n```\n" + code + "\n```\nError:\n" + error;
			}
		}
		catch (IOException | InterruptedException e) {
			logger.severe("Exception during execution: " + e.getMessage());
			return "Exception occurred while executing code:\n```\n" + code + "\n```\nError:\n" + e.getMessage();
		}
	}

}
