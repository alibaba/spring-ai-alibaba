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

package com.alibaba.cloud.ai.tool;

import com.alibaba.cloud.ai.config.PythonCoderProperties;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.command.LogContainerCmd;
import com.github.dockerjava.api.model.*;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.zerodep.ZerodepDockerHttpClient;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import static com.github.dockerjava.api.model.HostConfig.newHostConfig;

@Service
public class PythonReplTool {

	private static final Logger logger = Logger.getLogger(PythonReplTool.class.getName());

	private final PythonCoderProperties coderProperties;

	public PythonReplTool(PythonCoderProperties coderProperties) {
		this.coderProperties = coderProperties;
	}

	@Tool(description = "Execute Python code and return the result.")
	public String executePythonCode(@ToolParam(description = "python code") String code,
			@ToolParam(description = "requirements.txt", required = false) String requirements,
			@ToolParam(description = "input data for the python script", required = false) String data) {
		if (code == null || code.trim().isEmpty()) {
			return "Error: Code must be a non-empty string.";
		}
		if (!StringUtils.hasText(coderProperties.getContainNamePrefix())
				|| !StringUtils.hasText(coderProperties.getDockerHost())
				|| (coderProperties.getCpuCore() == null || coderProperties.getCpuCore() <= 0)
				|| (coderProperties.getLimitMemory() == null || coderProperties.getLimitMemory() <= 0)
				|| !StringUtils.hasText(coderProperties.getCodeTimeout())
				|| !StringUtils.hasText(coderProperties.getImageName())) {
			return "Error: Some Config is not set. You should reporter it to developer.";
		}

		// 自动检测操作系统并设置合适的Docker Host
		String dockerHost = getDockerHostForCurrentOS();

		DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
			.withDockerHost(dockerHost)
			.withDockerTlsVerify(false)
			.build();

		Path tempDir = null; // Declare tempDir outside try-with-resources for finally
								// block
		String volumeName = null; // Declare volumeName outside try-with-resources for
									// finally block
		DockerClient dockerClient = null; // Declare dockerClient outside
											// try-with-resources

		try {
			// 使用带回退机制的Docker客户端创建方法
			dockerClient = createDockerClientWithFallback(config);

			// Create temp dir and files
			tempDir = Files.createTempDirectory(coderProperties.getContainNamePrefix());
			Files.createFile(tempDir.resolve("requirements.txt"));
			Files.createFile(tempDir.resolve("script.py"));
			Files.write(tempDir.resolve("requirements.txt"),
					StringUtils.hasText(requirements) ? requirements.getBytes() : "".getBytes());
			Files.write(tempDir.resolve("script.py"), code.getBytes());

			boolean hasData = StringUtils.hasText(data);
			if (hasData) {
				Files.createFile(tempDir.resolve("input_data.txt"));
				Files.write(tempDir.resolve("input_data.txt"), data.getBytes());
			}

			// Build a docker to run
			String containName = generateUniqueContainerName();

			// create a volume to save third-party dependencies
			volumeName = containName.concat("-volume");

			// 先清理可能存在的同名容器和卷
			cleanupExistingResources(dockerClient, containName, volumeName);

			dockerClient.createVolumeCmd().withName(volumeName).withDriver("local").exec();

			if (!coderProperties.isEnableNetwork() && StringUtils.hasText(requirements)) {
				// If Python code is restricted from network access but requires
				// third-party dependencies, we need to provision a docker for pip to
				// install the dependencies.
				HostConfig hostConfig = createHostConfig(tempDir, volumeName, hasData);

				String pipContainerName = containName + "-pip";
				CreateContainerResponse pipContainer = dockerClient.createContainerCmd(coderProperties.getImageName())
					.withName(pipContainerName)
					.withWorkingDir("/app")
					.withHostConfig(hostConfig)
					.withCmd("sh", "-c",
							"pip3 install --target=/app/dependency --no-cache-dir -r requirements.txt > /dev/null")
					.exec();
				try {
					this.execDockerContainer(dockerClient, pipContainer);
				}
				catch (Exception e) {
					return "Error installing requirements: " + e.getMessage();
				}
				finally {
					// 立即清理pip容器
					try {
						dockerClient.removeContainerCmd(pipContainer.getId()).withForce(true).exec();
						logger.info("Removed pip container: " + pipContainerName);
					}
					catch (Exception ignore) {
						// Ignore cleanup errors
					}
				}
			}

			// run python code in docker
			HostConfig hostConfig = createHostConfig(tempDir, volumeName, hasData)
				.withNetworkMode(coderProperties.isEnableNetwork() ? "bridge" : "none");

			String execContainerName = containName + "-exec";
			CreateContainerResponse container = dockerClient.createContainerCmd(coderProperties.getImageName())
				.withName(execContainerName)
				.withWorkingDir("/app")
				.withHostConfig(hostConfig)
				.withCmd("sh", "-c", String.format(((coderProperties.isEnableNetwork() && StringUtils
					.hasText(requirements))
							? "pip3 install --target=/app/dependency --no-cache-dir -r requirements.txt > /dev/null && "
							: "")
						+ "export PYTHONPATH=\"/app/dependency:$PYTHONPATH\" && timeout -s SIGKILL %s python3 script.py",
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
			finally {
				// This finally block will execute before the outer one, ensuring
				// container is removed
				// before tempDir and volume are cleaned up.
				try {
					dockerClient.removeContainerCmd(container.getId()).withForce(true).exec();
				}
				catch (Exception ignore) {
					// Ignore exceptions during container removal
				}
			}
		}
		catch (Exception e) {
			logger.warning("Exception during execution: " + e.getMessage());
			return "Error executing code:\n```\n" + code + "\n```\nError:\n" + e.getMessage();
		}
		finally {
			// Ensure temp resources are cleaned up even if an exception occurs earlier
			if (tempDir != null && volumeName != null && dockerClient != null) {
				this.clearTempResource(tempDir, dockerClient, volumeName);
			}
		}
	}

	/**
	 * clean some temp resource before return
	 */
	private void clearTempResource(Path tempDir, DockerClient dockerClient, String volumeName) {
		try {
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
			logger.info("Temp directory has been deleted.");
		}
		catch (Exception e) {
			logger.warning("Exception in clean temp directory: " + e.getMessage());
		}
		try {
			// remove volume before return
			dockerClient.removeVolumeCmd(volumeName).exec();
		}
		catch (Exception ignore) {
		}
	}

	/**
	 * Create container's HostConfig
	 */
	private HostConfig createHostConfig(Path tempDir, String volumeName, boolean hasData) {
		List<Bind> binds = new ArrayList<>();
		binds.add(new Bind(tempDir.resolve("script.py").toAbsolutePath().toString(), new Volume("/app/script.py"),
				AccessMode.ro));
		binds.add(new Bind(tempDir.resolve("requirements.txt").toAbsolutePath().toString(),
				new Volume("/app/requirements.txt"), AccessMode.ro));
		binds.add(new Bind(volumeName, new Volume("/app/dependency")));

		if (hasData) {
			binds.add(new Bind(tempDir.resolve("input_data.txt").toAbsolutePath().toString(),
					new Volume("/app/input_data.txt"), AccessMode.ro));
		}

		return newHostConfig().withMemory(coderProperties.getLimitMemory() * 1024L * 1024L)
			.withCpuCount(coderProperties.getCpuCore())
			.withCapDrop(Capability.ALL)
			.withAutoRemove(false)
			.withBinds(binds.toArray(new Bind[0]))
			.withTmpFs(Map.of("/tmp", ""));
	}

	/**
	 * Run a Docker container and return its stdout. Throw a RuntimeException if errors
	 * occur.
	 */
	private String execDockerContainer(DockerClient dockerClient, CreateContainerResponse container)
			throws RuntimeException {
		// catch stdout and stderr
		ByteArrayOutputStream stdout = new ByteArrayOutputStream();
		ByteArrayOutputStream stderr = new ByteArrayOutputStream();

		try {
			// start docker
			dockerClient.startContainerCmd(container.getId()).exec();
			LogContainerCmd logContainerCmd = dockerClient.logContainerCmd(container.getId())
				.withStdOut(true)
				.withStdErr(true)
				.withFollowStream(true)
				.withTailAll();
			dockerClient.waitContainerCmd(container.getId())
				.start()
				.awaitCompletion(coderProperties.getDockerTimeout(), TimeUnit.SECONDS);

			// get stdout and stderr
			logContainerCmd.exec(new ResultCallback.Adapter<Frame>() {
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
					catch (Exception ignore) {
					}
				}
			}).awaitCompletion();

			// get exit code
			InspectContainerResponse inspectResponse = dockerClient.inspectContainerCmd(container.getId()).exec();
			int exitCode = Objects.requireNonNull(inspectResponse.getState().getExitCodeLong()).intValue();
			if (exitCode != 0) {
				String errorMessage = "Docker exit code " + exitCode + ". Stderr: "
						+ stderr.toString(Charset.defaultCharset()) + ". Stdout: "
						+ stdout.toString(Charset.defaultCharset());
				throw new RuntimeException(errorMessage);
			}
		}
		catch (Exception e) {
			logger.severe("Error when creating container in docker: {}" + e.getMessage());
			throw new RuntimeException(e);
		}
		finally {
			// Container removal is now handled in the outer try-catch-finally block
			// to ensure it's removed even if execDockerContainer throws an exception.
		}
		return stdout.toString(Charset.defaultCharset());
	}

	/**
	 * 根据当前操作系统自动选择合适的Docker Host地址
	 * @return Docker Host URI
	 */
	private String getDockerHostForCurrentOS() {
		String osName = System.getProperty("os.name").toLowerCase();
		logger.info("Detected operating system: " + osName);

		if (osName.contains("win")) {
			// Windows系统
			logger.info("Using Windows Docker configuration");
			// 在Windows上优先尝试TCP连接，更稳定
			return "tcp://localhost:2375";
		}
		else if (osName.contains("nix") || osName.contains("nux") || osName.contains("aix")) {
			// Linux/Unix系统
			logger.info("Using Linux/Unix Docker configuration");
			return "unix:///var/run/docker.sock";
		}
		else if (osName.contains("mac")) {
			// macOS系统
			logger.info("Using macOS Docker configuration");
			return "unix:///var/run/docker.sock";
		}
		else {
			// 未知系统，使用配置文件中的默认值
			logger.warning("Unknown operating system: " + osName + ", using configured docker host");
			return coderProperties.getDockerHost();
		}
	}

	/**
	 * 创��Docker客户端，支持多种连接方式的回退机制
	 * @param config Docker客户端配置
	 * @return DockerClient实例
	 * @throws Exception 如果所有连接方式都失败
	 */
	private DockerClient createDockerClientWithFallback(DockerClientConfig config) throws Exception {
		String osName = System.getProperty("os.name").toLowerCase();

		if (osName.contains("win")) {
			// Windows系统：尝试多种连接方式
			String[] windowsHosts = { "tcp://localhost:2375", // TCP方式（需要在Docker
																// Desktop中启用）
					"npipe://./pipe/docker_engine" // 命名管道方式
			};

			for (String dockerHost : windowsHosts) {
				try {
					logger.info("Attempting to connect to Docker using: " + dockerHost);

					DockerClientConfig testConfig = DefaultDockerClientConfig.createDefaultConfigBuilder()
						.withDockerHost(dockerHost)
						.withDockerTlsVerify(false)
						.build();

					ZerodepDockerHttpClient httpClient = new ZerodepDockerHttpClient.Builder()
						.dockerHost(testConfig.getDockerHost())
						.sslConfig(testConfig.getSSLConfig())
						.build();

					DockerClient dockerClient = DockerClientImpl.getInstance(testConfig, httpClient);

					// 测试连接是否正常
					dockerClient.pingCmd().exec();
					logger.info("Successfully connected to Docker using: " + dockerHost);
					return dockerClient;

				}
				catch (Exception e) {
					logger.warning("Failed to connect using " + dockerHost + ": " + e.getMessage());
				}
			}

			// 如果所有Windows连接方式都失败
			throw new Exception(
					"Failed to connect to Docker on Windows. Please ensure Docker Desktop is running and either:\n"
							+ "1. Enable 'Expose daemon on tcp://localhost:2375 without TLS' in Docker Desktop settings, or\n"
							+ "2. Ensure Docker Desktop's named pipe is available");

		}
		else {
			// Linux/Unix/macOS系统：使用标准Unix socket
			try {
				ZerodepDockerHttpClient httpClient = new ZerodepDockerHttpClient.Builder()
					.dockerHost(config.getDockerHost())
					.sslConfig(config.getSSLConfig())
					.build();

				DockerClient dockerClient = DockerClientImpl.getInstance(config, httpClient);
				dockerClient.pingCmd().exec(); // 测试连接
				logger.info("Successfully connected to Docker using: " + config.getDockerHost());
				return dockerClient;

			}
			catch (Exception e) {
				throw new Exception("Failed to connect to Docker on " + osName + ": " + e.getMessage()
						+ "\nPlease ensure Docker is running and accessible at: " + config.getDockerHost());
			}
		}
	}

	/**
	 * 清理已存在的同名容器和卷
	 */
	private void cleanupExistingResources(DockerClient dockerClient, String containName, String volumeName) {
		try {
			// 尝试删除同名容器
			dockerClient.removeContainerCmd(containName).withForce(true).exec();
			logger.info("Removed existing container: " + containName);
		}
		catch (Exception e) {
			logger.warning("Failed to remove container " + containName + ": " + e.getMessage());
		}

		try {
			// 尝试删除同名卷
			dockerClient.removeVolumeCmd(volumeName).exec();
			logger.info("Removed existing volume: " + volumeName);
		}
		catch (Exception e) {
			logger.warning("Failed to remove volume " + volumeName + ": " + e.getMessage());
		}
	}

	/**
	 * 生成唯一的容器名称
	 */
	private String generateUniqueContainerName() {
		return coderProperties.getContainNamePrefix() + "_" + System.currentTimeMillis();
	}

}
