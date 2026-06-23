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
package com.alibaba.cloud.ai.graph.agent.tools;

import com.alibaba.cloud.ai.graph.RunnableConfig;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

class ShellSessionManagerTest {

	@TempDir
	Path tempDir;

	@Test
	void executeCommandShouldCompleteWhenOutputHasNoTrailingNewline() throws Exception {
		Path file = tempDir.resolve("without-newline.txt");
		Files.writeString(file, "content-without-newline");
		boolean isWindows = System.getProperty("os.name").toLowerCase().contains("windows");
		String command = isWindows ? "echo|set /p=content-without-newline&ver >nul"
				: "cat without-newline.txt || true";
		RunnableConfig config = RunnableConfig.builder().build();
		ShellSessionManager manager = ShellSessionManager.builder()
			.workspaceRoot(tempDir)
			.shellCommand(isWindows ? Arrays.asList("cmd.exe", "/Q") : Arrays.asList("/bin/sh"))
			.commandTimeout(5000)
			.build();

		manager.initialize(config);
		try {
			ShellSessionManager.CommandResult result = manager.executeCommand(command, config);

			assertFalse(result.isTimedOut());
			assertTrue(result.isSuccess());
			assertEquals(0, result.getExitCode());
			assertFalse(result.getOutput().contains("__LC_SHELL_DONE__"));
			if (isWindows) {
				// Interactive cmd.exe may emit banner or prompt text to stdout.
				assertTrue(result.getOutput().contains("content-without-newline"));
			}
			else {
				assertEquals("content-without-newline", result.getOutput());
			}
		}
		finally {
			manager.cleanup(config);
		}
	}

	@Test
	void executeCommandShouldNotTruncateNoNewlineOutputAtExactByteLimit() throws Exception {
		boolean isWindows = System.getProperty("os.name").toLowerCase().contains("windows");
		assumeFalse(isWindows, "cmd.exe emits banner or prompt text in interactive mode");
		Path file = tempDir.resolve("one-byte.txt");
		Files.writeString(file, "x");
		RunnableConfig config = RunnableConfig.builder().build();
		ShellSessionManager manager = ShellSessionManager.builder()
			.workspaceRoot(tempDir)
			.shellCommand(Arrays.asList("/bin/sh"))
			.commandTimeout(5000)
			.maxOutputBytes(1)
			.build();

		manager.initialize(config);
		try {
			ShellSessionManager.CommandResult result = manager.executeCommand("cat one-byte.txt || true", config);

			assertFalse(result.isTimedOut());
			assertTrue(result.isSuccess());
			assertFalse(result.isTruncatedByBytes());
			assertEquals("x", result.getOutput());
			assertEquals(1, result.getTotalBytes());
		}
		finally {
			manager.cleanup(config);
		}
	}

}
