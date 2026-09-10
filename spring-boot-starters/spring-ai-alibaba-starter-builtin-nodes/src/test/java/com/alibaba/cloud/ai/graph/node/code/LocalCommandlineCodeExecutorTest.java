/*
 * Copyright 2024-2026 the original author or authors.
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@EnabledIf("isPowerShellAvailable")
class LocalCommandlineCodeExecutorTest {

	@TempDir
	Path tempDir;

	static boolean isPowerShellAvailable() throws InterruptedException {
		try {
			Process process = new ProcessBuilder("pwsh", "-NoProfile", "-NonInteractive", "-Command", "exit 0")
				.start();
			try {
				return process.waitFor(10, TimeUnit.SECONDS) && process.exitValue() == 0;
			}
			finally {
				process.destroyForcibly();
			}
		}
		catch (IOException e) {
			return false;
		}
	}

	@Test
	void executesPowerShellScriptInDirectoryWithSpaces() throws Exception {
		Path workDir = Files.createDirectory(tempDir.resolve("code execution"));
		CodeExecutionConfig config = new CodeExecutionConfig().setWorkDir(workDir.toString());
		CodeExecutionResult result = new LocalCommandlineCodeExecutor().executeCode("powershell",
				"$values = @(1, 2, 3); Write-Output (($values | Measure-Object -Sum).Sum)", config);

		assertEquals(0, result.exitCode(), result.logs());
		assertEquals("6", result.logs());
		try (var files = Files.list(workDir)) {
			assertEquals(0, files.count());
		}
	}

	@Test
	void stopsAfterPowerShellFailure() throws Exception {
		CodeExecutionConfig config = new CodeExecutionConfig().setWorkDir(tempDir.toString());
		List<CodeBlock> blocks = List.of(new CodeBlock("powershell", "exit 23"),
				new CodeBlock("powershell", "Set-Content -Path should-not-exist.txt -Value executed"));
		CodeExecutionResult result = new LocalCommandlineCodeExecutor().executeCodeBlocks(blocks, config);

		assertEquals(23, result.exitCode());
		assertFalse(Files.exists(tempDir.resolve("should-not-exist.txt")));
	}

}
