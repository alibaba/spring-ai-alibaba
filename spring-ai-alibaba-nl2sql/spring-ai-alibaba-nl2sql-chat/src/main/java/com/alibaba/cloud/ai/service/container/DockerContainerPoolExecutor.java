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
package com.alibaba.cloud.ai.service.container;

import com.alibaba.cloud.ai.config.ContainerProperties;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.zerodep.ZerodepDockerHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.io.IOException;

/**
 * @author vlsmb
 * @since 2025/7/12
 */
public class DockerContainerPoolExecutor extends AbstractContainerPoolExecutor {

	private static final Logger log = LoggerFactory.getLogger(DockerContainerPoolExecutor.class);

	private final DockerClient dockerClient;

	public DockerContainerPoolExecutor(ContainerProperties properties) throws Exception {
		super(properties);
		// 初始化DockerClient
		String dockerHost = this.getDockerHostForCurrentOS(properties.getHost());
		DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
			.withDockerHost(dockerHost)
			.withDockerTlsVerify(false)
			.build();
		this.dockerClient = this.createDockerClientWithFallback(config);
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
				log.info("Successfully connected to Docker using: {}", config.getDockerHost());
				return dockerClient;

			}
			catch (Exception e) {
				throw new Exception("Failed to connect to Docker on " + osName + ": " + e.getMessage()
						+ "\nPlease ensure Docker is running and accessible at: " + config.getDockerHost());
			}
		}
	}

	@Override
	protected String createNewContainer() throws Exception {
		return null;
	}

	@Override
	protected TaskResponse execTaskInContainer(TaskRequest request, String containerId) throws Exception {
		return null;
	}

	@Override
	protected void stopContainer(String containerId) throws Exception {

	}

	@Override
	protected void removeContainer(String containerId) throws Exception {

	}

	@Override
	protected void shutdownPool() throws Exception {
		super.shutdownPool();
		try {
			this.dockerClient.close();
		}
		catch (IOException ignored) {

		}
	}

}
