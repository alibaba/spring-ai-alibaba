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
package com.alibaba.cloud.ai.graph.node.code;

import com.alibaba.cloud.ai.graph.node.code.entity.CodeBlock;
import com.alibaba.cloud.ai.graph.node.code.entity.CodeExecutionConfig;
import com.alibaba.cloud.ai.graph.node.code.entity.CodeExecutionResult;
import com.alibaba.cloud.ai.graph.utils.CodeUtils;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallbackTemplate;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.Volume;
import com.github.dockerjava.core.DockerClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.commons.codec.digest.DigestUtils;
import com.alibaba.cloud.ai.graph.utils.FileUtils;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static com.github.dockerjava.api.model.HostConfig.newHostConfig;

/**
 * @author HeYQ
 * @since 2025-06-01 20:15
 */

public class DockerCodeExecutor implements CodeExecutor {

	private static final Logger logger = LoggerFactory.getLogger(DockerCodeExecutor.class);

	@Override
	public CodeExecutionResult executeCodeBlocks(List<CodeBlock> codeBlockList, CodeExecutionConfig codeExecutionConfig)
			throws Exception {
		StringBuilder allLogs = new StringBuilder();
		CodeExecutionResult result;

		// Create Docker client
		try (DockerClient dockerClient = DockerClientBuilder.getInstance().build()) {

			for (CodeBlock codeBlock : codeBlockList) {
				String language = codeBlock.language();
				String code = codeBlock.code();
				logger.info("\n>>>>>>>> EXECUTING CODE BLOCK (inferred language is {})...", language);

				// Generate unique filename for each code block
				String codeHash = DigestUtils.md5Hex(code);
				String fileExt = CodeUtils.getFileExtForLanguage(language);
				String filename = String.format("tmp_code_%s.%s", codeHash, fileExt);

				// Write code to working directory
				String hostWorkDir = codeExecutionConfig.getWorkDir();
				FileUtils.writeCodeToFile(hostWorkDir, filename, code);

				// Create and configure container
				// Mount host directory to container's /workspace directory
				Volume containerVolume = new Volume("/workspace");
				Bind volumeBind = new Bind(hostWorkDir, containerVolume);

				CreateContainerCmd createContainerCmd = dockerClient.createContainerCmd(codeExecutionConfig.getDocker())
					.withName(codeExecutionConfig.getContainerName() + "_" + codeBlockList.indexOf(codeBlock))
					.withCmd(CodeUtils.getExecutableForLanguage(language), filename)
					.withWorkingDir("/workspace")
					.withHostConfig(newHostConfig().withBinds(volumeBind));
				CreateContainerResponse container = createContainerCmd.exec();

				try {
					// Start container
					dockerClient.startContainerCmd(container.getId()).exec();

					// Wait for container execution to complete
					dockerClient.waitContainerCmd(container.getId())
						.start()
						.awaitCompletion(codeExecutionConfig.getTimeout(), TimeUnit.SECONDS);

					// Get container logs
					String logs = dockerClient.logContainerCmd(container.getId())
						.withStdOut(true)
						.withStdErr(true)
						.exec(new LogContainerResultCallback())
						.toString();

					// Get container exit code
					InspectContainerResponse containerInfo = dockerClient.inspectContainerCmd(container.getId()).exec();
					int exitCode = Objects.requireNonNull(containerInfo.getState().getExitCodeLong()).intValue();

					// Append logs
					allLogs.append("\n").append(logs.trim());

					// If execution failed, return result immediately
					if (exitCode != 0) {
						return new CodeExecutionResult(exitCode, allLogs.toString());
					}
				}
				finally {
					// Clean up container
					dockerClient.removeContainerCmd(container.getId()).withForce(true).exec();
					// Delete temporary file
					FileUtils.deleteFile(codeExecutionConfig.getWorkDir(), filename);
				}
			}

			return new CodeExecutionResult(0, allLogs.toString());
		}
		catch (Exception e) {
			logger.error("Error executing code in Docker container", e);
			throw new RuntimeException("Error executing code in Docker container: " + e.getMessage(), e);
		}
	}

	@Override
	public void restart() {

	}

	private static class LogContainerResultCallback extends ResultCallbackTemplate<LogContainerResultCallback, Frame> {

		private final StringBuilder log = new StringBuilder();

		@Override
		public void onNext(Frame frame) {
			log.append(new String(frame.getPayload()));
		}

		@Override
		public String toString() {
			return log.toString();
		}

	}

}
