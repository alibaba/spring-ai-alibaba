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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.alibaba.cloud.ai.graph.node.code.entity.CodeBlock;
import com.alibaba.cloud.ai.graph.node.code.entity.CodeExecutionConfig;
import com.alibaba.cloud.ai.graph.node.code.entity.CodeExecutionResult;
import com.alibaba.cloud.ai.graph.utils.FileUtils;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.PumpStreamHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

			if (Set.of("bash", "shell", "sh", "python").contains(language.toLowerCase())) {
				result = executeCode(language, code, codeExecutionConfig);
			}
			else {
				// the language is not supported, then return an error message.
				result = new CodeExecutionResult(1, "unknown language " + language);
			}

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
		String fileExt = language.startsWith("python") ? "py" : language;
		String filename = String.format("tmp_code_%s.%s", codeHash, fileExt);

		// write the code string to a file specified by the filename.
		FileUtils.writeCodeToFile(workDir, filename, code);

		CodeExecutionResult executionResult = executeCodeLocally(language, workDir, filename, config.getTimeout());

		FileUtils.deleteFile(workDir, filename);
		return executionResult;
	}

	private CodeExecutionResult executeCodeLocally(String language, String workDir, String filename, int timeout)
			throws Exception {
		// set up the command based on language
		String executable = getExecutableForLanguage(language);
		CommandLine commandLine = new CommandLine(executable);
		commandLine.addArgument(filename);

		// set up the execution environment
		DefaultExecutor executor = new DefaultExecutor();
		executor.setWorkingDirectory(new File(workDir));
		executor.setExitValue(0);

		// set up the streams for the output of the subprocess
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		ByteArrayOutputStream errorStream = new ByteArrayOutputStream();
		PumpStreamHandler streamHandler = new PumpStreamHandler(outputStream, errorStream);
		executor.setStreamHandler(streamHandler);

		// set up a watchdog to terminate the process if it exceeds the timeout
		ExecuteWatchdog watchdog = new ExecuteWatchdog(TimeUnit.SECONDS.toMillis(timeout));
		executor.setWatchdog(watchdog);

		try {
			// execute the command
			executor.execute(commandLine);
			// process completed before the watchdog terminated it
			String output = outputStream.toString();
			return new CodeExecutionResult(0, output.trim());
		}
		catch (ExecuteException e) {
			// process finished with an exit value (possibly non-zero)
			String errorOutput = errorStream.toString().replace(Path.of(workDir).toAbsolutePath() + File.separator, "");

			return new CodeExecutionResult(e.getExitValue(), errorOutput.trim());
		}
		catch (IOException e) {
			// returns a special result if the process was killed by the watchdog
			throw new Exception("Error executing code.", e);
		}
	}

	private String getExecutableForLanguage(String language) throws Exception {
		return switch (language) {
			case "python" -> language;
			case "shell", "bash", "sh", "powershell" -> "sh";
			default -> throw new Exception("Language not recognized in code execution:" + language);
		};
	}

}
