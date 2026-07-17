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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link ShellSessionManager}.
 * <p>
 * These tests verify the Session Registry functionality that enables shell session
 * recovery after Human-in-the-Loop (HITL) interrupts. When a graph is interrupted
 * and later resumed, the original RunnableConfig.context is lost, but the threadId
 * is preserved. The Session Registry allows the shell session to be recovered using
 * the threadId, preserving working directory and environment variables.
 * </p>
 *
 * @author AI Assistant
 */
class ShellSessionManagerTest {

	private static final String SESSION_INSTANCE_CONTEXT_KEY = "_SHELL_SESSION_";

	@TempDir
	Path tempDir;

	private ShellSessionManager sessionManager;
	private RunnableConfig config;
	private String threadId;

	@BeforeEach
	void setUp() {
		threadId = "test-thread-" + UUID.randomUUID();
		config = RunnableConfig.builder()
				.threadId(threadId)
				.build();
	}

	@AfterEach
	void tearDown() {
		if (sessionManager != null && config != null) {
			try {
				sessionManager.cleanup(config);
			} catch (Exception e) {
				// Ignore cleanup errors in teardown
			}
		}
		ShellSessionManager.clearSessionRegistry();
	}

	/**
	 * Create a ShellSessionManager using the platform default shell.
	 */
	private ShellSessionManager createSessionManager() {
		return ShellSessionManager.builder()
				.workspaceRoot(tempDir)
				.commandTimeout(10000)
				.startupTimeout(10000)
				.terminationTimeout(5000)
				.maxOutputLines(100)
				.build();
	}

	// ==================== Registry-Focused Tests (All OS) ====================
	// These tests verify the Session Registry mechanism using the platform default shell

	/**
	 * Test that session is registered in global registry on initialize with threadId.
	 */
	@Test
	void testSessionRegisteredOnInitialize() {
		sessionManager = createSessionManager();

		sessionManager.initialize(config);

		assertNotNull(config.context().get(SESSION_INSTANCE_CONTEXT_KEY),
				"Session should be stored in context after initialization");

		assertTrue(ShellSessionManager.isSessionInRegistry(threadId),
				"Session should be registered in global registry with threadId");
	}

	/**
	 * Test that session can be recovered from registry when context is empty (HITL scenario).
	 */
	@Test
	void testSessionRecoveredFromRegistryOnEmptyContext() {
		sessionManager = createSessionManager();

		// Phase 1: Initialize session
		sessionManager.initialize(config);
		Object originalSession = config.context().get(SESSION_INSTANCE_CONTEXT_KEY);
		assertNotNull(originalSession, "Original session should exist");

		// Phase 2: Simulate HITL resume - new config with same threadId but empty context
		RunnableConfig resumedConfig = RunnableConfig.builder()
				.threadId(threadId)
				.build();

		assertNull(resumedConfig.context().get(SESSION_INSTANCE_CONTEXT_KEY),
				"Resumed config should have empty context");

		// Execute any command - this should trigger recovery from registry
		boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");
		try {
			String cmd = isWindows ? "Write-Output 'test'" : "echo test";
			sessionManager.executeCommand(cmd, resumedConfig);
		} catch (Exception e) {
			// Command may fail/timeout, but session should still be recovered
		}

		// Verify session is now in resumed context (recovered from registry)
		Object recoveredSession = resumedConfig.context().get(SESSION_INSTANCE_CONTEXT_KEY);
		assertNotNull(recoveredSession,
				"Session should be recovered from registry to context");
		assertSame(originalSession, recoveredSession,
				"Recovered session should be the same instance as original session");

		config = resumedConfig;
	}

	/**
	 * Test that cleanup removes session from global registry.
	 */
	@Test
	void testCleanupRemovesFromRegistry() {
		sessionManager = createSessionManager();

		sessionManager.initialize(config);

		assertTrue(ShellSessionManager.isSessionInRegistry(threadId),
				"Session should be in registry after initialization");

		sessionManager.cleanup(config);

		assertFalse(ShellSessionManager.isSessionInRegistry(threadId),
				"Session should be removed from registry after cleanup");

		config = null; // Prevent double cleanup in tearDown
	}

	/**
	 * Test that config without threadId doesn't use registry but still works.
	 */
	@Test
	void testConfigWithoutThreadIdSkipsRegistry() {
		sessionManager = createSessionManager();

		RunnableConfig noThreadConfig = RunnableConfig.builder().build();

		sessionManager.initialize(noThreadConfig);

		assertNotNull(noThreadConfig.context().get(SESSION_INSTANCE_CONTEXT_KEY),
				"Session should be stored in context even without threadId");

		sessionManager.cleanup(noThreadConfig);

		config = null; // Prevent tearDown cleanup
	}

	/**
	 * Test that multiple sessions with different threadIds are isolated in registry.
	 */
	@Test
	void testMultipleThreadIdsAreIsolatedInRegistry() {
		sessionManager = createSessionManager();

		String threadId1 = "isolated-thread-1-" + UUID.randomUUID();
		String threadId2 = "isolated-thread-2-" + UUID.randomUUID();

		RunnableConfig config1 = RunnableConfig.builder().threadId(threadId1).build();
		RunnableConfig config2 = RunnableConfig.builder().threadId(threadId2).build();

		try {
			sessionManager.initialize(config1);
			sessionManager.initialize(config2);

			assertTrue(ShellSessionManager.isSessionInRegistry(threadId1), "Session 1 should be in registry");
			assertTrue(ShellSessionManager.isSessionInRegistry(threadId2), "Session 2 should be in registry");

			Object session1 = config1.context().get(SESSION_INSTANCE_CONTEXT_KEY);
			Object session2 = config2.context().get(SESSION_INSTANCE_CONTEXT_KEY);
			assertNotSame(session1, session2, "Sessions for different threadIds should be different");

			sessionManager.cleanup(config1);

			assertFalse(ShellSessionManager.isSessionInRegistry(threadId1), "Session 1 should be removed after cleanup");
			assertTrue(ShellSessionManager.isSessionInRegistry(threadId2), "Session 2 should still be in registry");
		} finally {
			try {
				sessionManager.cleanup(config2);
			} catch (Exception e) {
				// Ignore
			}
		}

		config = null; // Prevent tearDown cleanup
	}

	/**
	 * Test that session recovery doesn't remove the session from registry.
	 * This allows for multiple recovery attempts (multiple HITL resumes).
	 */
	@Test
	void testRecoveryDoesNotRemoveFromRegistry() {
		sessionManager = createSessionManager();

		sessionManager.initialize(config);
		Object originalSession = config.context().get(SESSION_INSTANCE_CONTEXT_KEY);

		boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");

		// Simulate 3 HITL resumes
		for (int i = 0; i < 3; i++) {
			RunnableConfig resumedConfig = RunnableConfig.builder()
					.threadId(threadId)
					.build();

			assertTrue(ShellSessionManager.isSessionInRegistry(threadId),
					"Session should still be in registry before recovery attempt " + (i + 1));

			try {
				String cmd = isWindows ? "Write-Output 'recovery-" + i + "'" : "echo recovery-" + i;
				sessionManager.executeCommand(cmd, resumedConfig);
			} catch (Exception e) {
				// Command may fail, but recovery should happen
			}

			// Verify session was recovered to context
			Object recoveredSession = resumedConfig.context().get(SESSION_INSTANCE_CONTEXT_KEY);
			assertSame(originalSession, recoveredSession,
					"Same session should be recovered on attempt " + (i + 1));

			assertTrue(ShellSessionManager.isSessionInRegistry(threadId),
					"Session should remain in registry after recovery attempt " + (i + 1));

			// Clear context to simulate next HITL
		}
	}

	/**
	 * Test fallback to new session when not found in registry or context.
	 */
	@Test
	void testFallbackCreatesNewSessionWhenNotInRegistry() {
		sessionManager = createSessionManager();

		String newThreadId = "fresh-thread-" + UUID.randomUUID();
		RunnableConfig freshConfig = RunnableConfig.builder()
				.threadId(newThreadId)
				.build();

		try {
			assertFalse(ShellSessionManager.isSessionInRegistry(newThreadId),
					"Fresh threadId should not be in registry");

			boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");
			try {
				String cmd = isWindows ? "Write-Output 'fallback'" : "echo fallback";
				sessionManager.executeCommand(cmd, freshConfig);
			} catch (Exception e) {
				// Command may fail
			}

			assertNotNull(freshConfig.context().get(SESSION_INSTANCE_CONTEXT_KEY),
					"New session should be created as fallback");

			assertTrue(ShellSessionManager.isSessionInRegistry(newThreadId),
					"New session should be registered in registry");
		} finally {
			sessionManager.cleanup(freshConfig);
		}

		config = null; // Prevent tearDown cleanup
	}

	/**
	 * Test that executeCommand throws IllegalStateException when called without
	 * threadId and without prior initialization. This prevents orphaned shell
	 * processes from being created when lifecycle management is not set up.
	 */
	@Test
	void testExecuteCommandThrowsWithoutThreadIdAndNoSession() {
		sessionManager = createSessionManager();

		RunnableConfig noThreadConfig = RunnableConfig.builder().build();

		// Attempting to execute command should throw IllegalStateException
		IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
			sessionManager.executeCommand("echo test", noThreadConfig);
		});

		assertTrue(exception.getMessage().contains("Shell session not initialized"),
				"Exception message should indicate uninitialized session");

		config = null; // Prevent tearDown cleanup
	}

}
