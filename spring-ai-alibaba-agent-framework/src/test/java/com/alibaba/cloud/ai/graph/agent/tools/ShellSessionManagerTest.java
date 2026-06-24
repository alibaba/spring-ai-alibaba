/*
 * Copyright 2025-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
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
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link ShellSessionManager} command output collection.
 *
 * <p>
 * Reproduces #4740: a command whose output does not end with a newline (e.g. {@code cat}
 * of a file without a trailing newline) caused the completion marker to be merged onto the
 * last output line, so it was never detected and {@code collectOutput} hung until the
 * command timed out.
 */
@DisabledOnOs(OS.WINDOWS)
class ShellSessionManagerTest {

	private ShellSessionManager newManager(Path workspace) {
		return ShellSessionManager.builder()
			.workspaceRoot(workspace)
			.commandTimeout(10000)
			.build();
	}

	@Test
	@Timeout(value = 20, unit = TimeUnit.SECONDS)
	void outputWithoutTrailingNewlineIsCapturedAndDoesNotHang() throws Exception {
		Path workspace = Files.createTempDirectory("shell_session_test_");
		ShellSessionManager manager = newManager(workspace);
		RunnableConfig config = RunnableConfig.builder().threadId("test-thread").build();
		manager.initialize(config);
		try {
			// printf without \n leaves no trailing newline, so the marker is merged onto
			// this line. Before the fix this hung until the command timed out.
			ShellSessionManager.CommandResult result = manager
				.executeCommand("printf 'hello-no-newline'", config);

			assertFalse(result.isTimedOut(), "command should not time out");
			assertEquals("hello-no-newline", result.getOutput());
			assertEquals(0, result.getExitCode());
		}
		finally {
			manager.cleanup(config);
		}
	}

	@Test
	@Timeout(value = 20, unit = TimeUnit.SECONDS)
	void exitCodeIsParsedWhenMergedWithOutput() throws Exception {
		Path workspace = Files.createTempDirectory("shell_session_test_");
		ShellSessionManager manager = newManager(workspace);
		RunnableConfig config = RunnableConfig.builder().threadId("test-thread").build();
		manager.initialize(config);
		try {
			// Output has no trailing newline AND the command fails: the non-zero exit code
			// must still be parsed from the line the marker was merged onto.
			ShellSessionManager.CommandResult result = manager
				.executeCommand("printf 'oops' && false", config);

			assertFalse(result.isTimedOut(), "command should not time out");
			assertEquals("oops", result.getOutput());
			assertEquals(1, result.getExitCode());
		}
		finally {
			manager.cleanup(config);
		}
	}

	@Test
	@Timeout(value = 20, unit = TimeUnit.SECONDS)
	void normalOutputWithTrailingNewlineStillWorks() throws Exception {
		Path workspace = Files.createTempDirectory("shell_session_test_");
		ShellSessionManager manager = newManager(workspace);
		RunnableConfig config = RunnableConfig.builder().threadId("test-thread").build();
		manager.initialize(config);
		try {
			ShellSessionManager.CommandResult result = manager.executeCommand("printf 'a\\nb\\n'", config);

			assertFalse(result.isTimedOut(), "command should not time out");
			assertEquals("a\nb", result.getOutput());
			assertEquals(0, result.getExitCode());
		}
		finally {
			manager.cleanup(config);
		}
	}

	@Test
	@Timeout(value = 30, unit = TimeUnit.SECONDS)
	void stdinReadingCommandDoesNotFalselyCompleteOnEchoedMarker() throws Exception {
		Path workspace = Files.createTempDirectory("shell_session_test_");
		// Short timeout: this command can never complete normally, so keep the expected
		// timeout/restart fast.
		ShellSessionManager manager = ShellSessionManager.builder()
			.workspaceRoot(workspace)
			.commandTimeout(3000)
			.build();
		RunnableConfig config = RunnableConfig.builder().threadId("test-thread").build();
		manager.initialize(config);
		try {
			// `cat` with no argument reads from the session's stdin and consumes/echoes the
			// injected marker command before it ever runs. The echoed line contains the marker
			// substring but no valid trailing exit code, so it must NOT be treated as completion
			// — otherwise the command would be falsely reported as a successful (exitCode == null)
			// run while still owning the session. It should time out and restart instead.
			ShellSessionManager.CommandResult result = manager.executeCommand("cat", config);

			assertTrue(result.isTimedOut(), "stdin-consuming command must not falsely complete on echoed marker");
		}
		finally {
			manager.cleanup(config);
		}
	}

}
