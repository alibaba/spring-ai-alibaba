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
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.lang.reflect.Field;

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
 * <p>
 * Note: These tests focus on verifying the Registry mechanism. Shell command execution
 * tests are OS-dependent and may be skipped on some platforms due to shell compatibility.
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
		// Ensure cleanup after each test - must clean up before temp dir deletion
		if (sessionManager != null && config != null) {
			try {
				sessionManager.cleanup(config);
				// Give process time to terminate on Windows
				Thread.sleep(1000);
			} catch (Exception e) {
				// Ignore cleanup errors in teardown
			}
		}
		// Clear any leftover registry entries
		clearSessionRegistry();
	}

	/**
	 * Clear the static SESSION_REGISTRY using reflection.
	 * This ensures test isolation.
	 */
	@SuppressWarnings("unchecked")
	private void clearSessionRegistry() {
		try {
			Field registryField = ShellSessionManager.class.getDeclaredField("SESSION_REGISTRY");
			registryField.setAccessible(true);
			ConcurrentHashMap<String, ?> registry = (ConcurrentHashMap<String, ?>) registryField.get(null);
			registry.clear();
		} catch (Exception e) {
			// Ignore - field may not exist or be accessible
		}
	}

	/**
	 * Check if session is registered in the global registry using reflection.
	 */
	@SuppressWarnings("unchecked")
	private boolean isSessionInRegistry(String threadId) {
		try {
			Field registryField = ShellSessionManager.class.getDeclaredField("SESSION_REGISTRY");
			registryField.setAccessible(true);
			ConcurrentHashMap<String, ?> registry = (ConcurrentHashMap<String, ?>) registryField.get(null);
			return registry.containsKey(threadId);
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * Create a ShellSessionManager for Unix-like systems.
	 */
	private ShellSessionManager createUnixSessionManager() {
		return ShellSessionManager.builder()
				.workspaceRoot(tempDir)
				.shellCommand(Arrays.asList("/bin/bash"))
				.commandTimeout(10000)
				.startupTimeout(10000)
				.terminationTimeout(5000)
				.maxOutputLines(100)
				.build();
	}

	/**
	 * Create a ShellSessionManager for Windows systems using PowerShell.
	 * PowerShell provides better marker detection than cmd.exe.
	 */
	private ShellSessionManager createWindowsSessionManager() {
		return ShellSessionManager.builder()
				.workspaceRoot(tempDir)
				.shellCommand(Arrays.asList("powershell.exe", "-NoLogo", "-NoProfile"))
				.commandTimeout(15000)
				.startupTimeout(15000)
				.terminationTimeout(5000)
				.maxOutputLines(100)
				.build();
	}

	// ==================== Registry-Focused Tests (All OS) ====================
	// These tests verify the Session Registry mechanism without depending on shell execution

	/**
	 * Test that session is registered in global registry on initialize with threadId.
	 */
	@Test
	void testSessionRegisteredOnInitialize() {
		boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");
		sessionManager = isWindows
				? createWindowsSessionManager()
				: createUnixSessionManager();

		// Initialize session
		sessionManager.initialize(config);

		// Verify session is in context
		assertNotNull(config.context().get(SESSION_INSTANCE_CONTEXT_KEY),
				"Session should be stored in context after initialization");

		// Verify session is registered in global registry
		assertTrue(isSessionInRegistry(threadId),
				"Session should be registered in global registry with threadId");
	}

	/**
	 * Test that session can be recovered from registry when context is empty (HITL scenario).
	 */
	@Test
	void testSessionRecoveredFromRegistryOnEmptyContext() {
		boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");
		sessionManager = isWindows
				? createWindowsSessionManager()
				: createUnixSessionManager();

		// Phase 1: Initialize session
		sessionManager.initialize(config);
		Object originalSession = config.context().get(SESSION_INSTANCE_CONTEXT_KEY);
		assertNotNull(originalSession, "Original session should exist");

		// Phase 2: Simulate HITL resume - new config with same threadId but empty context
		RunnableConfig resumedConfig = RunnableConfig.builder()
				.threadId(threadId)
				.build();

		// Verify context is empty initially
		assertNull(resumedConfig.context().get(SESSION_INSTANCE_CONTEXT_KEY),
				"Resumed config should have empty context");

		// Execute any command - this should trigger recovery from registry
		// Note: Command may timeout, but recovery should still happen
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
		boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");
		sessionManager = isWindows
				? createWindowsSessionManager()
				: createUnixSessionManager();

		// Initialize session
		sessionManager.initialize(config);

		// Verify session is in registry before cleanup
		assertTrue(isSessionInRegistry(threadId),
				"Session should be in registry after initialization");

		// Cleanup
		sessionManager.cleanup(config);
		// Give process time to terminate
		try { Thread.sleep(1000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }

		// Verify session is removed from registry
		assertFalse(isSessionInRegistry(threadId),
				"Session should be removed from registry after cleanup");

		config = null; // Prevent double cleanup in tearDown
	}

	/**
	 * Test that config without threadId doesn't use registry but still works.
	 */
	@Test
	void testConfigWithoutThreadIdSkipsRegistry() {
		boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");
		sessionManager = isWindows
				? createWindowsSessionManager()
				: createUnixSessionManager();

		// Config without threadId
		RunnableConfig noThreadConfig = RunnableConfig.builder().build();

		// Initialize - should work but not use registry
		sessionManager.initialize(noThreadConfig);

		// Verify session is in context
		assertNotNull(noThreadConfig.context().get(SESSION_INSTANCE_CONTEXT_KEY),
				"Session should be stored in context even without threadId");

		// No way to check registry without threadId, but cleanup should still work
		sessionManager.cleanup(noThreadConfig);
		try { Thread.sleep(1000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }

		config = null; // Prevent tearDown cleanup
	}

	/**
	 * Test that multiple sessions with different threadIds are isolated in registry.
	 */
	@Test
	void testMultipleThreadIdsAreIsolatedInRegistry() {
		boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");
		sessionManager = isWindows
				? createWindowsSessionManager()
				: createUnixSessionManager();

		String threadId1 = "isolated-thread-1-" + UUID.randomUUID();
		String threadId2 = "isolated-thread-2-" + UUID.randomUUID();

		RunnableConfig config1 = RunnableConfig.builder().threadId(threadId1).build();
		RunnableConfig config2 = RunnableConfig.builder().threadId(threadId2).build();

		try {
			// Initialize both sessions
			sessionManager.initialize(config1);
			sessionManager.initialize(config2);

			// Verify both are registered
			assertTrue(isSessionInRegistry(threadId1), "Session 1 should be in registry");
			assertTrue(isSessionInRegistry(threadId2), "Session 2 should be in registry");

			// Get original sessions
			Object session1 = config1.context().get(SESSION_INSTANCE_CONTEXT_KEY);
			Object session2 = config2.context().get(SESSION_INSTANCE_CONTEXT_KEY);

			// Sessions should be different instances
			assertNotSame(session1, session2, "Sessions for different threadIds should be different");

			// Cleanup session 1 only
			sessionManager.cleanup(config1);
			try { Thread.sleep(500); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }

			// Verify only session 1 is removed
			assertFalse(isSessionInRegistry(threadId1), "Session 1 should be removed after cleanup");
			assertTrue(isSessionInRegistry(threadId2), "Session 2 should still be in registry");

		} finally {
			// Clean up session 2
			try {
				sessionManager.cleanup(config2);
				Thread.sleep(500);
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
		boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");
		sessionManager = isWindows
				? createWindowsSessionManager()
				: createUnixSessionManager();

		// Initialize session
		sessionManager.initialize(config);
		Object originalSession = config.context().get(SESSION_INSTANCE_CONTEXT_KEY);

		// Simulate 3 HITL resumes
		for (int i = 0; i < 3; i++) {
			RunnableConfig resumedConfig = RunnableConfig.builder()
					.threadId(threadId)
					.build();

			// Verify session is still in registry before recovery
			assertTrue(isSessionInRegistry(threadId),
					"Session should still be in registry before recovery attempt " + (i + 1));

			// Trigger recovery by executing command
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

			// Verify session is still in registry after recovery
			assertTrue(isSessionInRegistry(threadId),
					"Session should remain in registry after recovery attempt " + (i + 1));

			// Clear context to simulate next HITL
		}
	}

	/**
	 * Test fallback to new session when not found in registry or context.
	 */
	@Test
	void testFallbackCreatesNewSessionWhenNotInRegistry() {
		boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");
		sessionManager = isWindows
				? createWindowsSessionManager()
				: createUnixSessionManager();

		String newThreadId = "fresh-thread-" + UUID.randomUUID();
		RunnableConfig freshConfig = RunnableConfig.builder()
				.threadId(newThreadId)
				.build();

		try {
			// Verify not in registry initially
			assertFalse(isSessionInRegistry(newThreadId),
					"Fresh threadId should not be in registry");

			// Execute command - should create new session as fallback
			try {
				String cmd = isWindows ? "Write-Output 'fallback'" : "echo fallback";
				sessionManager.executeCommand(cmd, freshConfig);
			} catch (Exception e) {
				// Command may fail
			}

			// Verify new session was created
			assertNotNull(freshConfig.context().get(SESSION_INSTANCE_CONTEXT_KEY),
					"New session should be created as fallback");

			// Verify new session is now registered
			assertTrue(isSessionInRegistry(newThreadId),
					"New session should be registered in registry");

		} finally {
			sessionManager.cleanup(freshConfig);
			try { Thread.sleep(500); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
		}

		config = null; // Prevent tearDown cleanup
	}

}
