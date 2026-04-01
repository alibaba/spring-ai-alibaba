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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Pattern;

/**
 * Manages shell sessions and command execution.
 * Provides persistent shell execution capabilities with state preservation.
 *
 * <p>This class maintains a global registry of shell sessions keyed by {@code threadId},
 * enabling session recovery after Human-in-the-Loop (HITL) interrupts. When a graph
 * resumes with a fresh {@link RunnableConfig} (which has an empty context), the session
 * can be looked up from the registry using the same threadId, preserving working directory,
 * environment variables, and other shell state.</p>
 */
public class ShellSessionManager {

	private static final Logger log = LoggerFactory.getLogger(ShellSessionManager.class);
	private static final String DONE_MARKER_PREFIX = "__LC_SHELL_DONE__";
	private static final String SESSION_INSTANCE_CONTEXT_KEY = "_SHELL_SESSION_";
	private static final String SESSION_PATH_CONTEXT_KEY = "_SHELL_PATH_";

	/**
	 * Global registry of shell sessions, keyed by threadId.
	 * This enables session recovery after HITL interrupts where the original
	 * RunnableConfig.context is lost but the threadId is preserved.
	 */
	private static final ConcurrentHashMap<String, SessionEntry> SESSION_REGISTRY = new ConcurrentHashMap<>();

	/**
	 * Entry in the session registry, holding both the session and its workspace path.
	 */
	private record SessionEntry(ShellSession session, Path workspacePath) {
	}

	private final Path workspaceRoot;
	private final boolean useTemporaryWorkspace;
	private final List<String> startupCommands;
	private final List<String> shutdownCommands;
	private final long commandTimeout;
	private final long startupTimeout;
	private final long terminationTimeout;
	private final int maxOutputLines;
	private final Long maxOutputBytes;
	private final List<String> shellCommand;
	private final Map<String, String> environment;
	private final List<RedactionRule> redactionRules;

	private ShellSessionManager(Builder builder) {
		this.workspaceRoot = builder.workspaceRoot;
		this.useTemporaryWorkspace = builder.workspaceRoot == null;
		this.startupCommands = new ArrayList<>(builder.startupCommands);
		this.shutdownCommands = new ArrayList<>(builder.shutdownCommands);
		this.commandTimeout = builder.commandTimeout;
		this.startupTimeout = builder.startupTimeout;
		this.terminationTimeout = builder.terminationTimeout;
		this.maxOutputLines = builder.maxOutputLines;
		this.maxOutputBytes = builder.maxOutputBytes;
		this.shellCommand = new ArrayList<>(builder.shellCommand);
		this.environment = new HashMap<>(builder.environment);
		this.redactionRules = new ArrayList<>(builder.redactionRules);
	}

	public static Builder builder() {
		return new Builder();
	}

	/**
	 * Initialize shell session.
	 * The session is registered in the global registry using {@code threadId} as the key,
	 * enabling recovery after HITL interrupts.
	 */
	public void initialize(RunnableConfig config) {
		try {
			Path workspace = workspaceRoot;
			if (useTemporaryWorkspace) {
				Path tempDir = Files.createTempDirectory("shell_tool_");
				config.context().put(SESSION_PATH_CONTEXT_KEY, tempDir);

				workspace = tempDir;
			} else {
				Files.createDirectories(workspace);
			}

			ShellSession session = new ShellSession(workspace, shellCommand, environment, terminationTimeout);
			session.start();
			config.context().put(SESSION_INSTANCE_CONTEXT_KEY, session);

			// Register in global registry for HITL recovery
			final Path finalWorkspace = workspace;
			config.threadId().ifPresent(threadId -> {
				SESSION_REGISTRY.put(threadId, new SessionEntry(session, finalWorkspace));
				log.debug("Registered shell session in global registry with threadId: {}", threadId);
			});

			log.info("Started shell session in workspace: {}", workspace);

			// Run startup commands
			for (String command : startupCommands) {
				CommandResult result = session.execute(command, startupTimeout, maxOutputLines, maxOutputBytes);
				if (result.isTimedOut() || (result.getExitCode() != null && result.getExitCode() != 0)) {
					throw new RuntimeException("Startup command failed: " + command + ", exit code: " + result.getExitCode());
				}
			}
		} catch (Exception e) {
			cleanup(config);
			throw new RuntimeException("Failed to initialize shell session", e);
		}
	}

	/**
	 * Clean up shell session.
	 * This removes the session from both the context and the global registry.
	 */
	public void cleanup(RunnableConfig config) {
		try {
			// Try to get session from context first, then from registry
			ShellSession session = (ShellSession) config.context().get(SESSION_INSTANCE_CONTEXT_KEY);
			if (session == null) {
				session = getSessionFromRegistry(config);
			}
			if (session != null) {
				// Run shutdown commands
				for (String command : shutdownCommands) {
					try {
						session.execute(command, commandTimeout, maxOutputLines, maxOutputBytes);
					} catch (Exception e) {
						log.warn("Shutdown command failed: {}", command, e);
					}
				}
			}
		} finally {
			doCleanup(config);
		}
	}

	private void doCleanup(RunnableConfig config) {
		// Try to get session from context first
		ShellSession session = (ShellSession) config.context().get(SESSION_INSTANCE_CONTEXT_KEY);
		Path tempDir = (Path) config.context().get(SESSION_PATH_CONTEXT_KEY);

		// If not in context, try to get from registry (HITL resume scenario)
		SessionEntry registryEntry = null;
		if (session == null && config.threadId().isPresent()) {
			registryEntry = SESSION_REGISTRY.remove(config.threadId().get());
			if (registryEntry != null) {
				session = registryEntry.session();
				tempDir = registryEntry.workspacePath();
				log.debug("Removed shell session from global registry for threadId: {}", config.threadId().get());
			}
		} else if (session != null && config.threadId().isPresent()) {
			// Also remove from registry if we found it in context
			SESSION_REGISTRY.remove(config.threadId().get());
		}

		if (session != null) {
			session.stop(terminationTimeout);
			config.context().remove(SESSION_INSTANCE_CONTEXT_KEY);
		}

		if (tempDir != null && useTemporaryWorkspace) {
			try {
				deleteDirectory(tempDir);
			} catch (IOException e) {
				log.warn("Failed to delete temporary directory: {}", tempDir, e);
			}
			config.context().remove(SESSION_PATH_CONTEXT_KEY);
		}
	}

	/**
	 * Attempt to recover a shell session from the global registry.
	 * This is used during HITL resume when the original context is lost.
	 *
	 * @param config the runnable config containing threadId
	 * @return the recovered session, or null if not found
	 */
	private ShellSession recoverSessionFromRegistry(RunnableConfig config) {
		return config.threadId().map(threadId -> {
			SessionEntry entry = SESSION_REGISTRY.get(threadId);
			if (entry != null) {
				log.info("Recovered shell session from global registry for threadId: {}. " +
						"Shell state (working directory, environment) is preserved.", threadId);
				// Put back into context for subsequent calls
				config.context().put(SESSION_INSTANCE_CONTEXT_KEY, entry.session());
				if (entry.workspacePath() != null) {
					config.context().put(SESSION_PATH_CONTEXT_KEY, entry.workspacePath());
				}
				return entry.session();
			}
			return null;
		}).orElse(null);
	}

	/**
	 * Get session from registry without removing it.
	 */
	private ShellSession getSessionFromRegistry(RunnableConfig config) {
		return config.threadId()
				.map(threadId -> SESSION_REGISTRY.get(threadId))
				.map(SessionEntry::session)
				.orElse(null);
	}

	/**
	 * Execute a command in the current shell session.
	 * <p>If the session is missing from the context (e.g. after a Human-in-the-Loop resume
	 * where a fresh {@link RunnableConfig} with an empty context is used), the method will:
	 * <ol>
	 *   <li>First attempt to recover the existing session from the global registry using threadId</li>
	 *   <li>If not found in registry, create a new session as fallback</li>
	 * </ol>
	 * This ensures shell state (working directory, environment variables) is preserved across HITL interrupts.</p>
	 */
	public CommandResult executeCommand(String command, RunnableConfig config) {
		ShellSession session = (ShellSession) config.context().get(SESSION_INSTANCE_CONTEXT_KEY);
		if (session == null) {
			// Try to recover from global registry using threadId
			session = recoverSessionFromRegistry(config);
			if (session == null) {
				// Only auto-initialize in the HITL recovery case (threadId present).
				// For truly uninitialized usage (no threadId), preserve the previous
				// behavior and fail fast rather than starting a new shell process
				// without lifecycle management.
				if (config.threadId().isPresent()) {
					log.warn("Shell session not found in context or registry for threadId {}. " +
							"Creating new session for HITL recovery.", config.threadId().get());
					initialize(config);
					session = (ShellSession) config.context().get(SESSION_INSTANCE_CONTEXT_KEY);
				}
				else {
					throw new IllegalStateException(
							"Shell session not initialized. Call initialize() before executeCommand() " +
									"or ensure lifecycle management (e.g., ShellToolAgentHook) is installed.");
				}
			}
		}

		log.info("Executing shell command: {}", command);
		CommandResult result = session.execute(command, commandTimeout, maxOutputLines, maxOutputBytes);

		// Apply redactions and track matches
		String output = result.getOutput();
		Map<String, List<String>> allMatches = new HashMap<>();

		for (RedactionRule rule : redactionRules) {
			RedactionResult redactionResult = rule.applyWithMatches(output);
			output = redactionResult.getRedactedContent();
			if (!redactionResult.getMatches().isEmpty()) {
				allMatches.computeIfAbsent(rule.getPiiType(), k -> new ArrayList<>())
					.addAll(redactionResult.getMatches());
			}
		}

		return new CommandResult(output, result.getExitCode(), result.isTimedOut(),
			result.isTruncatedByLines(), result.isTruncatedByBytes(),
			result.getTotalLines(), result.getTotalBytes(), allMatches);
	}

	/**
	 * Restart the shell session.
	 * <p>If the session is missing from the context (e.g. after HITL resume),
	 * it will first attempt to recover from the global registry.</p>
	 */
	public void restartSession(RunnableConfig config) {
		ShellSession session = (ShellSession) config.context().get(SESSION_INSTANCE_CONTEXT_KEY);
		if (session == null) {
			// Try to recover from global registry (HITL resume scenario)
			session = recoverSessionFromRegistry(config);
		}
		if (session == null) {
			throw new IllegalStateException("Shell session not initialized. " +
					"Cannot restart a session that does not exist.");
		}

		log.info("Restarting shell session");
		session.restart();

		// Re-run startup commands
		for (String command : startupCommands) {
			session.execute(command, startupTimeout, maxOutputLines, maxOutputBytes);
		}
	}

	public int getMaxOutputLines() {
		return maxOutputLines;
	}

	/**
	 * Clear the global session registry. Package-private for test isolation.
	 */
	static void clearSessionRegistry() {
		SESSION_REGISTRY.clear();
	}

	/**
	 * Check if a session is registered for the given threadId. Package-private for testing.
	 */
	static boolean isSessionInRegistry(String threadId) {
		return SESSION_REGISTRY.containsKey(threadId);
	}

	public Long getMaxOutputBytes() {
		return maxOutputBytes;
	}

	private void deleteDirectory(Path directory) throws IOException {
		if (Files.exists(directory)) {
			try (var stream = Files.walk(directory)) {
				stream.sorted(Comparator.reverseOrder())
					.forEach(path -> {
						try {
							Files.delete(path);
						} catch (IOException e) {
							log.warn("Failed to delete: {}", path, e);
						}
					});
			}
		}
	}

	/**
	 * Persistent shell session that executes commands sequentially.
	 * <p>This is a static nested class to avoid implicit reference to the outer
	 * {@link ShellSessionManager} instance, which could cause memory leaks when
	 * sessions are stored in the static {@link #SESSION_REGISTRY}.</p>
	 */
	private static class ShellSession {
		private static final Logger log = LoggerFactory.getLogger(ShellSession.class);
		private static final String DONE_MARKER_PREFIX = "__LC_SHELL_DONE__";

		private final Path workspace;
		private final List<String> command;
		private final Map<String, String> env;
		private final long terminationTimeout;
		private final boolean isWindows;
		private final boolean isPowerShell;
		private Process process;
		private BufferedWriter stdin;
		private BlockingQueue<OutputLine> outputQueue;
		private volatile boolean terminated;

		ShellSession(Path workspace, List<String> command, Map<String, String> env, long terminationTimeout) {
			this.workspace = workspace;
			this.command = command;
			this.env = env;
			this.terminationTimeout = terminationTimeout;
			this.outputQueue = new LinkedBlockingQueue<>();
			// Detect shell type
			String shellCmd = command.isEmpty() ? "" : command.get(0).toLowerCase();
			this.isPowerShell = shellCmd.contains("powershell");
			this.isWindows = shellCmd.contains("cmd") || isPowerShell;
		}

		void start() throws IOException {
			ProcessBuilder pb = new ProcessBuilder(command);
			pb.directory(workspace.toFile());
			pb.environment().putAll(env);
			pb.redirectErrorStream(false); // Keep stderr separate for better error tracking

			process = pb.start();
			stdin = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));

			// Start stdout reader thread
			new Thread(() -> {
				try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
					String line;
					while ((line = reader.readLine()) != null) {
						outputQueue.offer(new OutputLine("stdout", line));
					}
				} catch (IOException e) {
					log.debug("Stdout reader terminated", e);
				} finally {
					outputQueue.offer(new OutputLine("stdout", null)); // EOF marker
				}
			}, "shell-stdout-reader").start();

			// Start stderr reader thread
			new Thread(() -> {
				try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
					String line;
					while ((line = reader.readLine()) != null) {
						outputQueue.offer(new OutputLine("stderr", line));
					}
				} catch (IOException e) {
					log.debug("Stderr reader terminated", e);
				} finally {
					outputQueue.offer(new OutputLine("stderr", null)); // EOF marker
				}
			}, "shell-stderr-reader").start();
		}

		void restart() {
			stop(this.terminationTimeout);
			try {
				start();
			} catch (IOException e) {
				throw new RuntimeException("Failed to restart shell session", e);
			}
		}

		void stop(long timeoutMs) {
			if (process == null || !process.isAlive()) {
				return;
			}

			terminated = true;
			try {
				stdin.write("exit\n");
				stdin.flush();
			} catch (IOException e) {
				log.debug("Failed to send exit command", e);
			}

			try {
				if (!process.waitFor(timeoutMs, TimeUnit.MILLISECONDS)) {
					process.destroyForcibly();
				}
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				process.destroyForcibly();
			}

			try {
				stdin.close();
			} catch (IOException e) {
				log.debug("Failed to close stdin", e);
			}
		}

		CommandResult execute(String command, long timeoutMs, int maxOutputLines, Long maxOutputBytes) {
			if (process == null || !process.isAlive()) {
				throw new IllegalStateException("Shell session is not running");
			}

			String marker = DONE_MARKER_PREFIX + UUID.randomUUID().toString().replace("-", "");
			long deadline = System.currentTimeMillis() + timeoutMs;

			try {
				// Clear output queue
				outputQueue.clear();

				// Send command
				stdin.write(command);
				if (!command.endsWith("\n")) {
					stdin.write("\n");
				}
				
				// Send marker command based on shell type
				if (isPowerShell) {
					// PowerShell: use $LASTEXITCODE for exit code
					stdin.write(String.format("Write-Output \"%s $LASTEXITCODE\"\n", marker));
				} else if (isWindows) {
					// Windows cmd.exe: use ERRORLEVEL
					stdin.write(String.format("echo %s %%ERRORLEVEL%%\n", marker));
				} else {
					// Unix/Linux: use $? for exit code
					stdin.write(String.format("printf '%s %%s\\n' $?\n", marker));
				}
				stdin.flush();

				// Collect output
				return collectOutput(marker, deadline, maxOutputLines, maxOutputBytes);

			} catch (IOException e) {
				throw new RuntimeException("Failed to execute command", e);
			}
		}

		private CommandResult collectOutput(String marker, long deadline, int maxOutputLines, Long maxOutputBytes) {
			List<String> lines = new ArrayList<>();
			int totalLines = 0;
			long totalBytes = 0;
			boolean truncatedByLines = false;
			boolean truncatedByBytes = false;
			Integer exitCode = null;
			boolean timedOut = false;

			while (true) {
				long remaining = deadline - System.currentTimeMillis();
				if (remaining <= 0) {
					timedOut = true;
					log.warn("Command timed out, restarting session");
					restart();
					break;
				}

				try {
					OutputLine outputLine = outputQueue.poll(remaining, TimeUnit.MILLISECONDS);
					if (outputLine == null) {
						timedOut = true;
						restart();
						break;
					}

					// Skip EOF markers
					if (outputLine.content == null) {
						continue;
					}

					String line = outputLine.content;

					// Check for completion marker (only in stdout)
					if ("stdout".equals(outputLine.source) && line.startsWith(marker)) {
						String[] parts = line.split(" ", 2);
						if (parts.length > 1) {
							try {
								exitCode = Integer.parseInt(parts[1].trim());
							} catch (NumberFormatException e) {
								// Ignore
							}
						}
						break;
					}

					totalLines++;

					// Format line with stderr label if needed
					String formattedLine = line;
					if ("stderr".equals(outputLine.source)) {
						formattedLine = "[stderr] " + line;
					}

					totalBytes += formattedLine.getBytes().length + 1; // +1 for newline

					if (totalLines <= maxOutputLines) {
						if (maxOutputBytes == null || totalBytes <= maxOutputBytes) {
							lines.add(formattedLine);
						} else {
							truncatedByBytes = true;
						}
					} else {
						truncatedByLines = true;
					}

				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					break;
				}
			}

			String output = String.join("\n", lines);
			return new CommandResult(output, exitCode, timedOut, truncatedByLines,
				truncatedByBytes, totalLines, totalBytes);
		}
	}

	/**
	 * Output line with source (stdout/stderr).
	 */
	private static class OutputLine {
		final String source;
		final String content;

		OutputLine(String source, String content) {
			this.source = source;
			this.content = content;
		}
	}

	/**
	 * Result of command execution.
	 */
	public static class CommandResult {
		private final String output;
		private final Integer exitCode;
		private final boolean timedOut;
		private final boolean truncatedByLines;
		private final boolean truncatedByBytes;
		private final int totalLines;
		private final long totalBytes;
		private final Map<String, List<String>> redactionMatches;

		public CommandResult(String output, Integer exitCode, boolean timedOut,
					  boolean truncatedByLines, boolean truncatedByBytes,
					  int totalLines, long totalBytes) {
			this(output, exitCode, timedOut, truncatedByLines, truncatedByBytes,
				 totalLines, totalBytes, new HashMap<>());
		}

		public CommandResult(String output, Integer exitCode, boolean timedOut,
					  boolean truncatedByLines, boolean truncatedByBytes,
					  int totalLines, long totalBytes, Map<String, List<String>> redactionMatches) {
			this.output = output;
			this.exitCode = exitCode;
			this.timedOut = timedOut;
			this.truncatedByLines = truncatedByLines;
			this.truncatedByBytes = truncatedByBytes;
			this.totalLines = totalLines;
			this.totalBytes = totalBytes;
			this.redactionMatches = new HashMap<>(redactionMatches);
		}

		public String getOutput() { return output; }
		public Integer getExitCode() { return exitCode; }
		public boolean isTimedOut() { return timedOut; }
		public boolean isTruncatedByLines() { return truncatedByLines; }
		public boolean isTruncatedByBytes() { return truncatedByBytes; }
		public int getTotalLines() { return totalLines; }
		public long getTotalBytes() { return totalBytes; }
		public Map<String, List<String>> getRedactionMatches() { return new HashMap<>(redactionMatches); }

		public boolean isSuccess() {
			return !timedOut && (exitCode == null || exitCode == 0);
		}
	}

	/**
	 * Result of redaction operation with match information.
	 */
	public static class RedactionResult {
		private final String redactedContent;
		private final List<String> matches;

		public RedactionResult(String redactedContent, List<String> matches) {
			this.redactedContent = redactedContent;
			this.matches = new ArrayList<>(matches);
		}

		public String getRedactedContent() { return redactedContent; }
		public List<String> getMatches() { return new ArrayList<>(matches); }
	}

	/**
	 * Redaction rule for sanitizing command output.
	 */
	public interface RedactionRule {
		/**
		 * Apply redaction to content and return redacted content with matches.
		 */
		RedactionResult applyWithMatches(String content);

		/**
		 * Get the PII type this rule detects.
		 */
		String getPiiType();
	}

	/**
	 * Simple pattern-based redaction rule.
	 */
	public static class PatternRedactionRule implements RedactionRule {
		private final Pattern pattern;
		private final String replacement;
		private final String piiType;

		public PatternRedactionRule(String pattern, String replacement, String piiType) {
			this.pattern = Pattern.compile(pattern);
			this.replacement = replacement;
			this.piiType = piiType;
		}

		@Override
		public RedactionResult applyWithMatches(String content) {
			List<String> matches = new ArrayList<>();
			java.util.regex.Matcher matcher = pattern.matcher(content);

			while (matcher.find()) {
				matches.add(matcher.group());
			}

			String redacted = matcher.replaceAll(replacement);
			return new RedactionResult(redacted, matches);
		}

		@Override
		public String getPiiType() {
			return piiType;
		}
	}

	public static class Builder {
		private Path workspaceRoot;
		private final List<String> startupCommands = new ArrayList<>();
		private final List<String> shutdownCommands = new ArrayList<>();
		private long commandTimeout = 30000; // 30 seconds
		private long startupTimeout = 10000; // 10 seconds
		private long terminationTimeout = 5000; // 5 seconds
		private int maxOutputLines = 1000;
		private Long maxOutputBytes = null;
		private List<String> shellCommand = getDefaultShellCommand();
		private final Map<String, String> environment = new HashMap<>();
		private final List<RedactionRule> redactionRules = new ArrayList<>();

		/**
		 * Get default shell command based on the operating system.
		 */
		private static List<String> getDefaultShellCommand() {
			String os = System.getProperty("os.name").toLowerCase();
			
			if (os.contains("windows")) {
				// Windows: use PowerShell for better compatibility and features
				return Arrays.asList("powershell.exe");
			} else if (os.contains("mac")) {
				// macOS: prefer zsh (default since Catalina), fallback to bash, then sh
				if (new File("/bin/zsh").exists()) {
					return Arrays.asList("/bin/zsh");
				} else if (new File("/bin/bash").exists()) {
					return Arrays.asList("/bin/bash");
				} else {
					return Arrays.asList("/bin/sh");
				}
			} else {
				// Linux and other Unix-like systems: prefer bash, fallback to sh
				if (new File("/bin/bash").exists()) {
					return Arrays.asList("/bin/bash");
				} else {
					return Arrays.asList("/bin/sh");
				}
			}
		}

		public Builder workspaceRoot(String path) {
			this.workspaceRoot = Path.of(path);
			return this;
		}

		public Builder workspaceRoot(Path path) {
			this.workspaceRoot = path;
			return this;
		}

		public Builder addStartupCommand(String command) {
			this.startupCommands.add(command);
			return this;
		}

		public Builder addShutdownCommand(String command) {
			this.shutdownCommands.add(command);
			return this;
		}

		public Builder setStartupCommand(List<String> commands) {
			this.startupCommands.addAll(commands);
			return this;
		}

		public Builder setShutdownCommand(List<String> commands) {
			this.shutdownCommands.addAll(commands);
			return this;
		}

		public Builder commandTimeout(long millis) {
			this.commandTimeout = millis;
			return this;
		}

		public Builder startupTimeout(long millis) {
			this.startupTimeout = millis;
			return this;
		}

		public Builder terminationTimeout(long millis) {
			this.terminationTimeout = millis;
			return this;
		}

		public Builder maxOutputLines(int lines) {
			this.maxOutputLines = lines;
			return this;
		}

		public Builder maxOutputBytes(long bytes) {
			this.maxOutputBytes = bytes;
			return this;
		}

		public Builder shellCommand(List<String> command) {
			this.shellCommand = new ArrayList<>(command);
			return this;
		}

		public Builder environment(Map<String, String> env) {
			this.environment.putAll(env);
			return this;
		}

		public Builder addRedactionRule(RedactionRule rule) {
			this.redactionRules.add(rule);
			return this;
		}

		public ShellSessionManager build() {
			return new ShellSessionManager(this);
		}
	}
}

