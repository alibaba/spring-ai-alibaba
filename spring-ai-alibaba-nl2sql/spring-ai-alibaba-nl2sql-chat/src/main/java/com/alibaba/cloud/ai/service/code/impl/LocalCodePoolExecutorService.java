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

package com.alibaba.cloud.ai.service.code.impl;

import com.alibaba.cloud.ai.config.CodeExecutorProperties;
import com.alibaba.cloud.ai.service.code.CodePoolExecutorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 使用本地Python3环境运行代码的实现类，要求本地的Python3需要有pandas等数据分析库。
 *
 * @author vlsmb
 * @since 2025/8/23
 */
public class LocalCodePoolExecutorService extends AbstractCodePoolExecutorService implements CodePoolExecutorService {

	private static final Logger logger = LoggerFactory.getLogger(LocalCodePoolExecutorService.class);

	private final ConcurrentHashMap<String, Path> containers;

	private static final String[] pythonNames = new String[] { "python3", "pypy3", "py3", "python", "pypy", "py" };

	private static final String[] pipNames = new String[] { "pip3", "pip" };

	// 对于本地运行这个实现类，“容器”为临时文件夹
	public LocalCodePoolExecutorService(CodeExecutorProperties properties) {
		super(properties);
		this.containers = new ConcurrentHashMap<>();
		if (this.checkProgramExists(pythonNames) == null) {
			throw new IllegalStateException(
					"No valid Python interpreter was found for the current system environment variables. Please install Python3 into the system environment variables first.");
		}
	}

	@Override
	protected String createNewContainer() throws Exception {
		Path container = Files.createTempDirectory(this.properties.getContainerNamePrefix());
		String containerId = container.toString();
		this.containers.put(containerId, container);
		return containerId;
	}

	@Override
	protected TaskResponse execTaskInContainer(TaskRequest request, String containerId) {
		Path container = this.containers.get(containerId);

		// 写入Py代码和标准输入
		Path scriptFile = container.resolve("script.py");
		Path stdinFile = container.resolve("stdin.txt");
		Path requirementFile = container.resolve("requirements.txt");
		try {
			Files.write(scriptFile, Optional.ofNullable(request.code()).orElse("").getBytes());
			Files.write(stdinFile, Optional.ofNullable(request.input()).orElse("").getBytes());
			Files.write(requirementFile, Optional.ofNullable(request.requirement()).orElse("").getBytes());
		}
		catch (Exception e) {
			logger.error("Create temp file failed: {}", e.getMessage(), e);
			return TaskResponse.exception(e.getMessage());
		}

		// 如果有requirements，则先安装依赖
		if (this.checkProgramExists(pipNames) != null && StringUtils.hasText(request.requirement())) {
			ProcessBuilder pip = new ProcessBuilder(this.checkProgramExists(pipNames), "install", "--no-cache-dir",
					"-r", requirementFile.toAbsolutePath().toString(), ">", "/dev/null");
			Process process = null;

			try {
				process = pip.start();
				boolean completed = process.waitFor(this.properties.getContainerTimeout(), TimeUnit.MINUTES);
				if (!completed) {
					process.destroy();
					if (process.isAlive()) {
						process.destroyForcibly();
					}
					throw new RuntimeException("Pip command timed out.");
				}
			}
			catch (Exception e) {
				// 即使PIP安装失败，仍然尝试运行Python代码
				logger.warn("Pip install failed: {}", e.getMessage(), e);
			}
			finally {
				if (process != null && process.isAlive()) {
					process.destroyForcibly();
				}
			}
		}

		// 运行Python代码
		Process process = null;
		try {
			ProcessBuilder pb = new ProcessBuilder(this.checkProgramExists(pythonNames),
					scriptFile.toAbsolutePath().toString());
			pb.directory(container.toFile());
			pb.redirectInput(stdinFile.toFile());
			process = pb.start();

			// 读取stdout和stderr
			StringWriter stdoutWriter = new StringWriter();
			StringWriter stderrWriter = new StringWriter();
			try (BufferedReader stdoutReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
					BufferedReader stderrReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
				CompletableFuture<Void> stdoutFuture = CompletableFuture.runAsync(() -> {
					try {
						stdoutReader.transferTo(stdoutWriter);
					}
					catch (IOException e) {
						stderrWriter.write("Error reading stdout: " + e.getMessage());
					}
				});
				CompletableFuture<Void> stderrFuture = CompletableFuture.runAsync(() -> {
					try {
						stderrReader.transferTo(stderrWriter);
					}
					catch (IOException e) {
						stderrWriter.write("Error reading stderr: " + e.getMessage());
					}
				});

				// 等待进程完成，带超时限制
				boolean completed = process.waitFor(this.parseToMilliseconds(this.properties.getCodeTimeout()),
						TimeUnit.MILLISECONDS);
				if (!completed) {
					process.destroy();
					if (process.isAlive()) {
						process.destroyForcibly();
					}
					return TaskResponse.failure("", "python code timeout, Killed.");
				}

				// 等待输出读取完成，给输出读取额外2秒时间
				CompletableFuture.allOf(stdoutFuture, stderrFuture).get(2, TimeUnit.SECONDS);
			}

			// 返回结果
			int exitCode = process.exitValue();
			String stdout = stdoutWriter.toString();
			String stderr = stderrWriter.toString();
			if (exitCode != 0) {
				return TaskResponse.failure(stdout, stderr);
			}
			else {
				return TaskResponse.success(stdout);
			}

		}
		catch (Exception e) {
			logger.error("Python execution failed: {}", e.getMessage(), e);
			return TaskResponse.exception(e.getMessage());
		}
		finally {
			if (process != null && process.isAlive()) {
				process.destroyForcibly();
			}
		}
	}

	@Override
	protected void stopContainer(String containerId) throws Exception {
		// 临时文件夹没有停止方法
	}

	@Override
	protected void removeContainer(String containerId) throws Exception {
		Path container = this.containers.remove(containerId);
		this.clearTempDir(container);
	}

	/**
	 * 按顺序检查多个程序是否存在
	 * @param programNames 程序名称，按优先级顺序
	 * @return 第一个找到的程序名称，如果都没找到返回null
	 */
	private String checkProgramExists(String... programNames) {
		if (programNames == null)
			return null;

		String pathEnv = System.getenv("PATH");
		if (pathEnv == null)
			return null;

		String[] pathDirs = pathEnv.split(File.pathSeparator);
		boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");

		for (String program : programNames) {
			for (String dir : pathDirs) {
				if (dir == null || dir.trim().isEmpty())
					continue;

				// 检查原始程序名
				Path path = Paths.get(dir, program);
				if (Files.exists(path) && Files.isExecutable(path)) {
					return program;
				}

				// 在Windows上检查.exe后缀
				if (isWindows) {
					Path exePath = Paths.get(dir, program + ".exe");
					if (Files.exists(exePath) && Files.isExecutable(exePath)) {
						return program;
					}
				}
			}
		}
		return null;
	}

	private long parseToMilliseconds(String timeString) {
		Pattern pattern = Pattern.compile("(\\d+)(ms|[smhd])");
		Matcher matcher = pattern.matcher(timeString.toLowerCase());

		if (matcher.find()) {
			long value = Long.parseLong(matcher.group(1));
			String unit = matcher.group(2);
			return switch (unit) {
				case "ms" -> value;
				case "s" -> value * 1000;
				case "m" -> value * 60 * 1000;
				case "h" -> value * 60 * 60 * 1000;
				case "d" -> value * 24 * 60 * 60 * 1000;
				default -> {
					logger.warn("Unknown time unit: {}", unit);
					// 返回默认值60s
					yield 60 * 1000;
				}
			};
		}
		logger.warn("Invalid time format: {}", timeString);
		return 60 * 1000;
	}

}
