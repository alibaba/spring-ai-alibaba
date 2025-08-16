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
import com.alibaba.cloud.ai.graph.utils.FileUtils;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.PumpStreamHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * @author HeYQ
 * @since 2024-12-02 17:23
 */
public class LocalCommandlineCodeExecutor implements CodeExecutor {

	private static final Logger logger = LoggerFactory.getLogger(LocalCommandlineCodeExecutor.class);

	@Override
	public CodeExecutionResult executeCodeBlocks(List<CodeBlock> codeBlockList, CodeExecutionConfig codeExecutionConfig)
			throws Exception {
		StringBuilder allLogs = new StringBuilder();
		CodeExecutionResult result;
		for (int i = 0; i < codeBlockList.size(); i++) {
			CodeBlock codeBlock = codeBlockList.get(i);
			String language = codeBlock.language();
			String code = codeBlock.code();
			logger.info("\n>>>>>>>> EXECUTING CODE BLOCK {} (inferred language is {})...", i + 1, language);
			// "bash", "shell", "sh", "python"
			result = executeCode(language, code, codeExecutionConfig);
			allLogs.append("\n").append(result.logs());
			if (result.exitCode() != 0) {
				return new CodeExecutionResult(result.exitCode(), allLogs.toString());
			}
		}
		return new CodeExecutionResult(0, allLogs.toString());
	}

	@Override
	public void restart() {

		logger.warn("Restarting local command line code executor is not supported. No action is taken.");
	}

	public CodeExecutionResult executeCode(String language, String code, CodeExecutionConfig config) throws Exception {
		if (Objects.isNull(language) || Objects.isNull(code)) {
			throw new Exception("Either language or code must be provided.");
		}
		String workDir = config.getWorkDir();
		String codeHash = DigestUtils.md5Hex(code);
		String fileExt = CodeUtils.getFileExtForLanguage(language);
		String filename = String.format("tmp_code_%s.%s", codeHash, fileExt);

		// write the code string to a file specified by the filename.
		FileUtils.writeCodeToFile(workDir, filename, code);

		// Copy required JAR files to workDir if language is Java
		if ("java".equals(language)) {
			FileUtils.copyResourceJarToWorkDir(workDir);
		}

		CodeExecutionResult executionResult = executeCodeLocally(language, workDir, filename, config);

		FileUtils.deleteFile(workDir, filename);

		// Delete JAR files if language is Java
		if ("java".equals(language)) {
			FileUtils.deleteResourceJarFromWorkDir(workDir);
		}
		return executionResult;
	}

	private CodeExecutionResult executeCodeLocally(String language, String workDir, String filename,
			CodeExecutionConfig config) throws Exception {
		// Set up command line based on language
		String executable = CodeUtils.getExecutableForLanguage(language);
		CommandLine commandLine = new CommandLine(executable);

		if ("java".equals(language)) {
			commandLine.addArgument("-cp");
			StringBuilder classPathBuilder = new StringBuilder();
			classPathBuilder.append(".").append(File.pathSeparator).append(workDir);

			// Add all JAR files in workDir to classpath
			try {
				Path workDirPath = Path.of(workDir);
				if (Files.exists(workDirPath)) {
					try (var stream = Files.walk(workDirPath)) {
						stream.filter(path -> path.toString().endsWith(".jar")).forEach(jarPath -> {
							classPathBuilder.append(File.pathSeparator).append(jarPath.toString());
						});
					}
				}
			}
			catch (IOException e) {
				logger.warn("Failed to scan JAR files in work directory", e);
			}

			if (config.getClassPath() != null && !config.getClassPath().isEmpty()) {
				classPathBuilder.append(File.pathSeparator).append(config.getClassPath());
			}

			String classPath = classPathBuilder.toString();
			commandLine.addArgument(classPath).addArgument(filename);
		}
		else {
			commandLine.addArgument(filename);
		}

		// Configure executor
		DefaultExecutor executor = new DefaultExecutor();
		executor.setWorkingDirectory(new File(workDir));
		executor.setExitValue(0);

		// Set up stream handling
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		ByteArrayOutputStream errorStream = new ByteArrayOutputStream();
		executor.setStreamHandler(new PumpStreamHandler(outputStream, errorStream));

		// Set timeout
		executor.setWatchdog(new ExecuteWatchdog(TimeUnit.SECONDS.toMillis(config.getTimeout())));

		try {
			executor.execute(commandLine);
			return new CodeExecutionResult(0, outputStream.toString().trim());
		}
		catch (ExecuteException e) {
			String errorOutput = errorStream.toString()
				.replace(Path.of(workDir).toAbsolutePath() + File.separator, "")
				.trim();
			return new CodeExecutionResult(e.getExitValue(), errorOutput);
		}
		catch (IOException e) {
			throw new Exception("Failed to execute code", e);
		}
		finally {
			// Cleanup Java class files
			if ("java".equals(language)) {
				FileUtils.deleteFile(workDir, filename.replace(".java", ".class"));
			}
		}
	}

}
