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
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallbackTemplate;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.*;
import com.github.dockerjava.core.DockerClientBuilder;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.*;
import java.lang.ref.Cleaner;
import java.nio.charset.Charset;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import static com.github.dockerjava.api.model.HostConfig.newHostConfig;

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
				|| (coderProperties.getCpuCore() == null || coderProperties.getCpuCore() <= 0)
				|| (coderProperties.getLimitMemory() == null || coderProperties.getLimitMemory() <= 0)
				|| !StringUtils.hasText(coderProperties.getCodeTimeout())
				|| !StringUtils.hasText(coderProperties.getImageName())) {
			return "Error: Some Config is not set. You should reporter it to developer.";
		}
		try (DockerClient dockerClient = DockerClientBuilder.getInstance().build()) {
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
			Files.createFile(tempDir.resolve("requirements.txt"));
			Files.createFile(tempDir.resolve("script.py"));
			Files.createDirectory(tempDir.resolve("dependency"));
			Files.createDirectory(tempDir.resolve("tmp"));
			Files.write(tempDir.resolve("requirements.txt"),
					StringUtils.hasText(requirements) ? requirements.getBytes() : "".getBytes());
			Files.write(tempDir.resolve("script.py"), code.getBytes());

			// Build a docker to run
			String containName = tempDir.getFileName().toString();

			if (!coderProperties.isEnableNetwork() && StringUtils.hasText(requirements)) {
				// If Python code is restricted from network access but requires
				// third-party dependencies, we need to provision a docker for pip to
				// install the dependencies.
				HostConfig hostConfig = getHostConfig(tempDir);

				CreateContainerResponse container = dockerClient.createContainerCmd(coderProperties.getImageName())
					.withName(containName)
					.withWorkingDir("/app")
					.withHostConfig(hostConfig)
					.withCmd("sh", "-c",
							"pip3 install --target=/app/site-packages --no-cache-dir -r requirements.txt > /dev/null")
					.exec();
				try {
					this.execDockerContainer(dockerClient, container);
				}
				catch (Exception e) {
					return "Error installing requirements: " + e.getMessage();
				}
			}

			// run python code in docker
			HostConfig hostConfig = getHostConfig(tempDir)
				.withNetworkMode(coderProperties.isEnableNetwork() ? "bridge" : "none");

			CreateContainerResponse container = dockerClient.createContainerCmd(coderProperties.getImageName())
				.withName(containName)
				.withWorkingDir("/app")
				.withHostConfig(hostConfig)
				.withCmd("sh", "-c", String.format(((coderProperties.isEnableNetwork() && StringUtils
					.hasText(requirements))
							? "pip3 install --target=/app/site-packages --no-cache-dir -r requirements.txt > /dev/null && "
							: "")
						+ "export PYTHONPATH=\"/app/site-packages:$PYTHONPATH\" && timeout -s SIGKILL %s python3 script.py",
						coderProperties.getCodeTimeout()))
				.exec();
			try {
				String output = this.execDockerContainer(dockerClient, container);
				logger.info("Python code executed successfully.");
				return "Successfully executed:\n```\n" + code + "\n```\nStdout:\n" + output;
			}
			catch (Exception e) {
				logger.warning("Python code execution failed.");
				return "Error executing code:\n```\n" + code + "\n```\nError:\n" + e.getMessage();
			}
		}
		catch (Exception e) {
			logger.warning("Exception during execution: " + e.getMessage());
			return "Exception occurred while executing code:\n```\n" + code + "\n```\nError:\n" + e.getMessage();
		}
	}

	private HostConfig getHostConfig(Path tempDir) throws IOException {
		return newHostConfig().withMemory(coderProperties.getLimitMemory() * 1024L * 1024L)
			.withCpuCount(coderProperties.getCpuCore())
			.withReadonlyRootfs(true)
			.withCapDrop(Capability.ALL)
			.withAutoRemove(false)
			.withBinds(
					new Bind(tempDir.resolve("script.py").toAbsolutePath().toString(), new Volume("/app/script.py"),
							AccessMode.ro),
					new Bind(tempDir.resolve("requirements.txt").toAbsolutePath().toString(),
							new Volume("/app/requirements.txt"), AccessMode.ro),
					new Bind(tempDir.resolve("tmp").toAbsolutePath().toString(), new Volume("/tmp")),
					new Bind(tempDir.resolve("dependency").toAbsolutePath().toString(),
							new Volume("/app/site-packages")));
	}

	public abstract static class ContainerStdoutCallBack
			extends ResultCallbackTemplate<ContainerStdoutCallBack, Frame> {

	}

	/**
	 * Run a Docker container and return its stdout, throwing a RuntimeException if errors
	 * occur.
	 */
	private String execDockerContainer(DockerClient dockerClient, CreateContainerResponse container)
			throws RuntimeException {
		// catch stdout and stderr
		ByteArrayOutputStream stdout = new ByteArrayOutputStream();
		ByteArrayOutputStream stderr = new ByteArrayOutputStream();
		ContainerStdoutCallBack callback = new ContainerStdoutCallBack() {
			@Override
			public void onNext(Frame frame) {
				try {
					if (frame.getStreamType() == StreamType.STDOUT) {
						stdout.write(frame.getPayload());
					}
					else if (frame.getStreamType() == StreamType.STDERR) {
						stderr.write(frame.getPayload());
					}
				}
				catch (Exception e) {
					logger.warning("Exception during execution Stdout CallBack: " + e.getMessage());
				}
			}
		};

		// start docker
		try {
			dockerClient.logContainerCmd(container.getId())
				.withStdOut(true)
				.withStdErr(true)
				.withFollowStream(true)
				.withTimestamps(false)
				.exec(callback);
			dockerClient.startContainerCmd(container.getId()).exec();
			dockerClient.waitContainerCmd(container.getId())
				.start()
				.awaitCompletion(coderProperties.getDockerTimeout(), TimeUnit.SECONDS);
			callback.awaitCompletion(coderProperties.getDockerTimeout(), TimeUnit.SECONDS);
			InspectContainerResponse inspectResponse = dockerClient.inspectContainerCmd(container.getId()).exec();
			int exitCode = Objects.requireNonNull(inspectResponse.getState().getExitCodeLong()).intValue();
			if (exitCode != 0) {
				throw new RuntimeException(
						"Docker exit code " + exitCode + ". stderr: " + stderr.toString(Charset.defaultCharset()));
			}
		}
		catch (Exception e) {
			logger.severe("Error when creating container in docker: {}" + e.getMessage());
			throw new RuntimeException(e);
		}
		finally {
			dockerClient.removeContainerCmd(container.getId()).withForce(true).exec();
		}
		return stdout.toString(Charset.defaultCharset());
	}

}
