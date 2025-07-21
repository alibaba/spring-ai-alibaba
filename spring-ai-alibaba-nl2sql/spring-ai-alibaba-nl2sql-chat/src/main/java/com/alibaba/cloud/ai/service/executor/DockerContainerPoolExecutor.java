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
package com.alibaba.cloud.ai.service.executor;

import com.alibaba.cloud.ai.config.ContainerProperties;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.command.LogContainerCmd;
import com.github.dockerjava.api.command.PullImageResultCallback;
import com.github.dockerjava.api.model.AccessMode;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.Capability;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.StreamType;
import com.github.dockerjava.api.model.Volume;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.zerodep.ZerodepDockerHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static com.github.dockerjava.api.model.HostConfig.newHostConfig;

/**
 * 运行Python任务的容器池（Docker实现类）
 *
 * @author vlsmb
 * @since 2025/7/12
 */
public class DockerContainerPoolExecutor extends AbstractContainerPoolExecutor implements ContainerPoolExecutor {

	private static final Logger log = LoggerFactory.getLogger(DockerContainerPoolExecutor.class);

	private final DockerClient dockerClient;

	private final ConcurrentHashMap<String, Path> containerTempPath;

	public DockerContainerPoolExecutor(ContainerProperties properties) {
		super(properties);
		// 初始化DockerClient
		String dockerHost = this.getDockerHostForCurrentOS(properties.getHost());
		DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
			.withDockerHost(dockerHost)
			.withDockerTlsVerify(false)
			.build();
		this.dockerClient = this.createDockerClientWithFallback(config);
		this.containerTempPath = new ConcurrentHashMap<>();

		// 检查镜像是否已存在本地
		boolean imageExists = this.dockerClient.listImagesCmd()
			.withImageNameFilter(properties.getImageName())
			.exec()
			.stream()
			.anyMatch(image -> Arrays.asList(image.getRepoTags()).contains(properties.getImageName()));

		if (!imageExists) {
			// 拉取镜像
			try {
				this.dockerClient.pullImageCmd(properties.getImageName())
					.exec(new PullImageResultCallback())
					.awaitCompletion();
				log.info("pull image {} success", properties.getImageName());
			}
			catch (Exception e) {
				log.error("pull image {} error", properties.getImageName(), e);
				throw new RuntimeException(e);
			}
		}
	}

	/**
	 * 根据当前操作系统自动选择合适的Docker Host地址
	 * @return Docker Host URI
	 */
	private String getDockerHostForCurrentOS(String dockerHost) {
		// 如果配置对象里有值，则直接使用配置对象的值
		if (StringUtils.hasText(dockerHost)) {
			return dockerHost;
		}
		String osName = System.getProperty("os.name").toLowerCase();
		log.info("Detected operating system: {}", osName);

		if (osName.contains("win")) {
			// Windows系统
			log.info("Using Windows Docker configuration");
			// 在Windows上优先尝试TCP连接，更稳定
			return "tcp://localhost:2375";
		}
		else if (osName.contains("nix") || osName.contains("nux") || osName.contains("aix")) {
			// Linux/Unix系统
			log.info("Using Linux/Unix Docker configuration");
			return "unix:///var/run/docker.sock";
		}
		else if (osName.contains("mac")) {
			// macOS系统
			log.info("Using macOS Docker configuration");
			return "unix:///var/run/docker.sock";
		}
		else {
			// 未知系统，使用配置文件中的默认值
			log.warn("Unknown operating system: {}, using default docker host", osName);
			return "unix:///var/run/docker.sock";
		}
	}

	/**
	 * 创建Docker客户端，支持多种连接方式的回退机制
	 * @param config Docker客户端配置
	 * @return DockerClient实例
	 * @throws Exception 如果所有连接方式都失败
	 */
	private DockerClient createDockerClientWithFallback(DockerClientConfig config) {
		String osName = System.getProperty("os.name").toLowerCase();

		if (osName.contains("win")) {
			// Windows系统：尝试多种连接方式
			String[] windowsHosts = { "tcp://localhost:2375", // TCP方式（需要在Docker
					// Desktop中启用）
					"npipe://./pipe/docker_engine" // 命名管道方式
			};

			for (String dockerHost : windowsHosts) {
				try {
					log.info("Attempting to connect to Docker using: {}", dockerHost);

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
					log.info("Successfully connected to Docker using: {}", dockerHost);
					return dockerClient;

				}
				catch (Exception e) {
					log.warn("Failed to connect using {}: {}", dockerHost, e.getMessage());
				}
			}

			// 如果所有Windows连接方式都失败
			throw new RuntimeException(
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
				log.info("Successfully connected to Docker using: {}", config.getDockerHost());
				return dockerClient;

			}
			catch (Exception e) {
				throw new RuntimeException("Failed to connect to Docker on " + osName + ": " + e.getMessage()
						+ "\nPlease ensure Docker is running and accessible at: " + config.getDockerHost());
			}
		}
	}

	/**
	 * Create container's HostConfig
	 */
	private HostConfig createHostConfig(Path tempDir) {
		List<Bind> binds = new ArrayList<>();
		binds.add(new Bind(tempDir.resolve("script.py").toAbsolutePath().toString(), new Volume("/app/script.py"),
				AccessMode.ro));
		binds.add(new Bind(tempDir.resolve("requirements.txt").toAbsolutePath().toString(),
				new Volume("/app/requirements.txt"), AccessMode.ro));
		binds.add(new Bind(tempDir.resolve("input_data.txt").toAbsolutePath().toString(),
				new Volume("/app/input_data.txt"), AccessMode.ro));

		return newHostConfig().withMemory(this.properties.getLimitMemory() * 1024L * 1024L)
			.withCpuCount(this.properties.getCpuCore())
			.withCapDrop(Capability.ALL)
			.withAutoRemove(false)
			.withBinds(binds.toArray(new Bind[0]))
			.withTmpFs(Map.of("/tmp", ""))
			.withNetworkMode(this.properties.getNetworkMode());
	}

	/**
	 * 清理已存在的同名容器
	 */
	private void cleanupExistingResources(String containName) {
		try {
			// 尝试删除同名容器
			dockerClient.removeContainerCmd(containName).withForce(true).exec();
			log.info("Removed existing container: {}", containName);
		}
		catch (Exception e) {
			log.warn("Failed to remove container {}: {}", containName, e.getMessage());
		}
	}

	@Override
	protected String createNewContainer() throws Exception {
		String containerName = this.generateContainerName();
		// 先清理可能存在的同名容器
		this.cleanupExistingResources(containerName);

		// 生成临时目录和文件
		Path tempDir = Files.createTempDirectory(containerName);
		Files.createFile(tempDir.resolve("requirements.txt"));
		Files.createFile(tempDir.resolve("script.py"));
		Files.createFile(tempDir.resolve("input_data.txt"));

		// 创建容器
		HostConfig hostConfig = this.createHostConfig(tempDir);
		CreateContainerResponse container = dockerClient.createContainerCmd(properties.getImageName())
			.withName(containerName)
			.withWorkingDir("/app")
			.withHostConfig(hostConfig)
			.withCmd("sh", "-c", String.format(
					"if [ -s requirements.txt ]; then pip install --no-cache-dir -r requirements.txt > /dev/null; fi && timeout -s SIGKILL %s python3 script.py < input_data.txt",
					properties.getCodeTimeout()))
			.exec();
		String containerId = container.getId();
		// 保存临时目录对象
		this.containerTempPath.put(containerId, tempDir);
		return containerId;
	}

	@Override
	protected TaskResponse execTaskInContainer(TaskRequest request, String containerId) throws Exception {
		// 获取临时目录对象，将数据写入临时目录
		Path tempDir = this.containerTempPath.get(containerId);
		if (tempDir == null) {
			log.error("Container '{}' does not exist work dir", containerId);
			return TaskResponse.error("Container '" + containerId + "' does not exist work dir");
		}
		Files.write(tempDir.resolve("script.py"),
				StringUtils.hasText(request.code()) ? request.code().getBytes() : "".getBytes());
		Files.write(tempDir.resolve("requirements.txt"),
				StringUtils.hasText(request.requirement()) ? request.requirement().getBytes() : "".getBytes());
		Files.write(tempDir.resolve("input_data.txt"),
				StringUtils.hasText(request.input()) ? request.input().getBytes() : "".getBytes());

		// catch stdout and stderr
		ByteArrayOutputStream stdout = new ByteArrayOutputStream();
		ByteArrayOutputStream stderr = new ByteArrayOutputStream();

		try {
			// start docker
			dockerClient.startContainerCmd(containerId).exec();
			LogContainerCmd logContainerCmd = dockerClient.logContainerCmd(containerId)
				.withStdOut(true)
				.withStdErr(true)
				.withFollowStream(true)
				.withTailAll();
			dockerClient.waitContainerCmd(containerId)
				.start()
				.awaitCompletion(this.properties.getContainerTimeout(), TimeUnit.SECONDS);

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
			InspectContainerResponse inspectResponse = dockerClient.inspectContainerCmd(containerId).exec();
			int exitCode = Objects.requireNonNull(inspectResponse.getState().getExitCodeLong()).intValue();
			if (exitCode != 0) {
				String errorMessage = "Docker exit code " + exitCode + ". Stderr: "
						+ stderr.toString(Charset.defaultCharset()) + ". Stdout: "
						+ stdout.toString(Charset.defaultCharset());
				log.error("Error executing Docker container {}: {}", containerId, errorMessage);
				return TaskResponse.error(errorMessage);
			}
		}
		catch (Exception e) {
			log.error("Error when creating container in docker: {}", e.getMessage());
			return TaskResponse.error(e.getMessage());
		}
		return new TaskResponse(stdout.toString(Charset.defaultCharset()));
	}

	@Override
	protected void stopContainer(String containerId) throws Exception {
		try {
			this.dockerClient.stopContainerCmd(containerId).exec();
			log.info("Successfully stopped container: {}", containerId);
		}
		catch (Exception e) {
			log.warn("Failed to stop container: {}, message: {}", containerId, e.getMessage());
		}
	}

	@Override
	protected void removeContainer(String containerId) throws Exception {
		try {
			this.dockerClient.removeContainerCmd(containerId).withForce(true).exec();
			Path tempDir = containerTempPath.get(containerId);
			if (tempDir != null) {
				this.clearTempDir(tempDir);
			}
			containerTempPath.remove(containerId);
			log.info("Successfully removed container: {}", containerId);
		}
		catch (Exception e) {
			log.warn("Failed to remove container: {}, message: {}", containerId, e.getMessage());
		}
	}

	@Override
	protected void shutdownPool() throws Exception {
		try {
			super.shutdownPool();
			this.dockerClient.close();
		}
		catch (IOException ignored) {

		}
	}

}
