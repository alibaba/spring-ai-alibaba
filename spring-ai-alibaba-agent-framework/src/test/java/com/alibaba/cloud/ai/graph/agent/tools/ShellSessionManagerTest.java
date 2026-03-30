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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ShellSessionManagerTest {

	@TempDir
	Path tempDir;

	@Test
	void testBuilderDefaults() {
		ShellSessionManager manager = ShellSessionManager.builder().build();
		assertNotNull(manager);
		assertEquals(1000, manager.getMaxOutputLines());
		assertNull(manager.getMaxOutputBytes());
	}

	@Test
	void testBuilderCustomValues() {
		ShellSessionManager manager = ShellSessionManager.builder()
				.workspaceRoot(tempDir)
				.maxOutputLines(500)
				.maxOutputBytes(1024L)
				.commandTimeout(10000)
				.startupTimeout(5000)
				.terminationTimeout(2000)
				.build();

		assertEquals(500, manager.getMaxOutputLines());
		assertEquals(1024L, manager.getMaxOutputBytes());
	}

	@Test
	void testInitializeWithWorkspace() throws IOException {
		ShellSessionManager manager = ShellSessionManager.builder()
				.workspaceRoot(tempDir)
				.build();
		RunnableConfig config = RunnableConfig.builder().build();

		manager.initialize(config);

		assertTrue(Files.exists(tempDir));
		assertNotNull(config.context().get("_SHELL_SESSION_"));

		manager.cleanup(config);
		assertNull(config.context().get("_SHELL_SESSION_"));
	}

	@Test
	void testInitializeWithTempWorkspace() {
		ShellSessionManager manager = ShellSessionManager.builder().build();
		RunnableConfig config = RunnableConfig.builder().build();

		manager.initialize(config);

		Path sessionPath = (Path) config.context().get("_SHELL_PATH_");
		assertNotNull(sessionPath);
		assertTrue(Files.exists(sessionPath));
		assertNotNull(config.context().get("_SHELL_SESSION_"));

		manager.cleanup(config);
		assertFalse(Files.exists(sessionPath));
		assertNull(config.context().get("_SHELL_SESSION_"));
		assertNull(config.context().get("_SHELL_PATH_"));
	}

	@Test
	void testExecuteCommand() {
		ShellSessionManager manager = ShellSessionManager.builder().build();
		RunnableConfig config = RunnableConfig.builder().build();

		manager.initialize(config);
		try {
			String cmd = isWindows() ? "echo hello" : "echo hello";
			ShellSessionManager.CommandResult result = manager.executeCommand(cmd, config);

			assertNotNull(result);
			assertTrue(result.getOutput().contains("hello"));
			assertEquals(0, result.getExitCode());
			assertFalse(result.isTimedOut());
		} finally {
			manager.cleanup(config);
		}
	}

	@Test
	void testExecuteCommandWithRedaction() {
		ShellSessionManager manager = ShellSessionManager.builder()
				.addRedactionRule(new ShellSessionManager.PatternRedactionRule("secret-\\d+", "REDACTED", "SECRET"))
				.build();
		RunnableConfig config = RunnableConfig.builder().build();

		manager.initialize(config);
		try {
			String cmd = isWindows() ? "echo my secret-123 is here" : "echo my secret-123 is here";
			ShellSessionManager.CommandResult result = manager.executeCommand(cmd, config);

			assertNotNull(result);
			assertTrue(result.getOutput().contains("my REDACTED is here"));
			Map<String, List<String>> matches = result.getRedactionMatches();
			assertTrue(matches.containsKey("SECRET"));
			assertEquals(List.of("secret-123"), matches.get("SECRET"));
		} finally {
			manager.cleanup(config);
		}
	}

	@Test
	void testOutputLineLimit() {
		ShellSessionManager manager = ShellSessionManager.builder()
				.maxOutputLines(2)
				.build();
		RunnableConfig config = RunnableConfig.builder().build();

		manager.initialize(config);
		try {
			String cmd;
			if (isWindows()) {
				cmd = "echo line1 & echo line2 & echo line3";
			} else {
				cmd = "echo line1 && echo line2 && echo line3";
			}
			ShellSessionManager.CommandResult result = manager.executeCommand(cmd, config);

			assertNotNull(result);
			String[] lines = result.getOutput().split("\n");
			// Some shells might echo the command or have extra output, but we expect at most 2 lines from our command
			// Actually, the implementation joins lines, so we check if it's truncated
			assertTrue(result.isTruncatedByLines());
			assertTrue(lines.length <= 2);
		} finally {
			manager.cleanup(config);
		}
	}

	@Test
	void testOutputByteLimit() {
		ShellSessionManager manager = ShellSessionManager.builder()
				.maxOutputBytes(10)
				.build();
		RunnableConfig config = RunnableConfig.builder().build();

		manager.initialize(config);
		try {
			String cmd = "echo 123456789012345";
			ShellSessionManager.CommandResult result = manager.executeCommand(cmd, config);

			assertNotNull(result);
			assertTrue(result.isTruncatedByBytes());
			assertTrue(result.getOutput().length() <= 10);
		} finally {
			manager.cleanup(config);
		}
	}

	@Test
	void testStartupCommands() {
		ShellSessionManager manager = ShellSessionManager.builder()
				.addStartupCommand(isWindows() ? "set TEST_VAR=startup" : "export TEST_VAR=startup")
				.build();
		RunnableConfig config = RunnableConfig.builder().build();

		manager.initialize(config);
		try {
			// In persistent shell, environment variables should persist
			String cmd = isWindows() ? "echo %TEST_VAR%" : "echo $TEST_VAR";
			ShellSessionManager.CommandResult result = manager.executeCommand(cmd, config);
			assertTrue(result.getOutput().contains("startup"));
		} finally {
			manager.cleanup(config);
		}
	}

	@Test
	void testFailedStartupCommand() {
		ShellSessionManager manager = ShellSessionManager.builder()
				.addStartupCommand("nonexistentcommand_should_fail")
				.build();
		RunnableConfig config = RunnableConfig.builder().build();

		assertThrows(RuntimeException.class, () -> manager.initialize(config));
		// Context should be cleaned up
		assertNull(config.context().get("_SHELL_SESSION_"));
	}

	@Test
	void testRestartSession() {
		ShellSessionManager manager = ShellSessionManager.builder()
				.addStartupCommand(isWindows() ? "set TEST_VAR=startup" : "export TEST_VAR=startup")
				.build();
		RunnableConfig config = RunnableConfig.builder().build();

		manager.initialize(config);
		try {
			Object firstSession = config.context().get("_SHELL_SESSION_");
			manager.restartSession(config);
			Object secondSession = config.context().get("_SHELL_SESSION_");

			// Session object should be different if restarted (actually ShellSession is private, but let's see)
			// Wait, restart() in ShellSession is internal. ShellSessionManager.restartSession calls session.restart()
			// Looking at code: session.restart() stops and starts the SAME session object's process.
			// So firstSession == secondSession.
			assertSame(firstSession, secondSession);

			String cmd = isWindows() ? "echo %TEST_VAR%" : "echo $TEST_VAR";
			ShellSessionManager.CommandResult result = manager.executeCommand(cmd, config);
			assertTrue(result.getOutput().contains("startup"));
		} finally {
			manager.cleanup(config);
		}
	}

	@Test
	void testUninitializedExecution() {
		ShellSessionManager manager = ShellSessionManager.builder().build();
		RunnableConfig config = RunnableConfig.builder().build();

		assertThrows(IllegalStateException.class, () -> manager.executeCommand("echo hello", config));
	}

	private boolean isWindows() {
		return System.getProperty("os.name").toLowerCase().contains("win");
	}
}
