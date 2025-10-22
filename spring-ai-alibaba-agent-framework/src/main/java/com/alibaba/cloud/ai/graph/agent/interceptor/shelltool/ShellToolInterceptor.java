/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.cloud.ai.graph.agent.interceptor.shelltool;

import com.alibaba.cloud.ai.graph.agent.interceptor.ToolCallHandler;
import com.alibaba.cloud.ai.graph.agent.interceptor.ToolCallRequest;
import com.alibaba.cloud.ai.graph.agent.interceptor.ToolCallResponse;
import com.alibaba.cloud.ai.graph.agent.interceptor.ToolInterceptor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Pattern;

/**
 * Tool interceptor that provides persistent shell execution capabilities.
 *
 * This interceptor manages a long-lived shell session that can execute commands
 * sequentially, maintaining state between executions. It supports:
 * - Persistent shell sessions with state preservation
 * - Command timeout and output truncation
 * - Startup and shutdown command execution
 * - Working directory management
 * - Output redaction for sensitive data
 *
 * Example:
 * <pre>
 * ShellToolInterceptor interceptor = ShellToolInterceptor.builder()
 *     .workspaceRoot("/tmp/agent-workspace")
 *     .commandTimeout(30000)
 *     .maxOutputLines(1000)
 *     .addStartupCommand("source ~/.bashrc")
 *     .build();
 * </pre>
 */
public class ShellToolInterceptor extends ToolInterceptor {

	private static final Logger log = LoggerFactory.getLogger(ShellToolInterceptor.class);
	private static final String DONE_MARKER_PREFIX = "__LC_SHELL_DONE__";
	private static final String SHELL_TOOL_NAME = "shell";
	private static final ObjectMapper objectMapper = new ObjectMapper();

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

	// Session state (stored per-agent run)
	private final ThreadLocal<ShellSession> sessionHolder = new ThreadLocal<>();
	private final ThreadLocal<Path> tempDirHolder = new ThreadLocal<>();

	private ShellToolInterceptor(Builder builder) {
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

	@Override
	public String getName() {
		return "ShellTool";
	}

	/**
	 * Initialize shell session before agent execution starts.
	 */
	public void beforeAgent() {
		try {
			Path workspace = workspaceRoot;
			if (useTemporaryWorkspace) {
				Path tempDir = Files.createTempDirectory("shell_tool_");
				tempDirHolder.set(tempDir);
				workspace = tempDir;
			} else {
				Files.createDirectories(workspace);
			}

			ShellSession session = new ShellSession(workspace, shellCommand, environment);
			session.start();
			sessionHolder.set(session);

			log.info("Started shell session in workspace: {}", workspace);

			// Run startup commands
			for (String command : startupCommands) {
				CommandResult result = session.execute(command, startupTimeout);
				if (result.isTimedOut() || (result.getExitCode() != null && result.getExitCode() != 0)) {
					throw new RuntimeException("Startup command failed: " + command + ", exit code: " + result.getExitCode());
				}
			}
		} catch (Exception e) {
			cleanup();
			throw new RuntimeException("Failed to initialize shell session", e);
		}
	}

	/**
	 * Clean up shell session after agent execution completes.
	 */
	public void afterAgent() {
		try {
			ShellSession session = sessionHolder.get();
			if (session != null) {
				// Run shutdown commands
				for (String command : shutdownCommands) {
					try {
						session.execute(command, commandTimeout);
					} catch (Exception e) {
						log.warn("Shutdown command failed: {}", command, e);
					}
				}
			}
		} finally {
			cleanup();
		}
	}

	private void cleanup() {
		ShellSession session = sessionHolder.get();
		if (session != null) {
			session.stop(terminationTimeout);
			sessionHolder.remove();
		}

		Path tempDir = tempDirHolder.get();
		if (tempDir != null) {
			try {
				deleteDirectory(tempDir);
			} catch (IOException e) {
				log.warn("Failed to delete temporary directory: {}", tempDir, e);
			}
			tempDirHolder.remove();
		}
	}

	@Override
	public ToolCallResponse wrapToolCall(ToolCallRequest request, ToolCallHandler handler) {
		// Only intercept shell tool calls
		if (!SHELL_TOOL_NAME.equals(request.getToolName())) {
			return handler.call(request);
		}

		ShellSession session = sessionHolder.get();
		if (session == null) {
			throw new IllegalStateException("Shell session not initialized. Call beforeAgent() first.");
		}

		try {
			Map<String, Object> args = parseArguments(request.getArguments());

			// Handle restart request
			if (Boolean.TRUE.equals(args.get("restart"))) {
				log.info("Restarting shell session");
				session.restart();

				// Re-run startup commands
				for (String command : startupCommands) {
					session.execute(command, startupTimeout);
				}

				return ToolCallResponse.of(request.getToolCallId(), request.getToolName(),
					"Shell session restarted successfully.");
			}

			// Execute command
			String command = (String) args.get("command");
			if (command == null || command.trim().isEmpty()) {
				throw new IllegalArgumentException("Command cannot be empty");
			}

			log.info("Executing shell command: {}", command);
			CommandResult result = session.execute(command, commandTimeout);

			// Handle timeout
			if (result.isTimedOut()) {
				return ToolCallResponse.of(request.getToolCallId(), request.getToolName(),
					String.format("Error: Command timed out after %.1f seconds.", commandTimeout / 1000.0));
			}

			// Apply redactions
			String output = result.getOutput();
			for (RedactionRule rule : redactionRules) {
				output = rule.apply(output);
			}

			// Format output
			output = output.isEmpty() ? "<no output>" : output;

			if (result.isTruncatedByLines()) {
				output += String.format("\n\n... Output truncated at %d lines (observed %d).",
					maxOutputLines, result.getTotalLines());
			}

			if (result.isTruncatedByBytes() && maxOutputBytes != null) {
				output += String.format("\n\n... Output truncated at %d bytes (observed %d).",
					maxOutputBytes, result.getTotalBytes());
			}

			if (result.getExitCode() != null && result.getExitCode() != 0) {
				output += "\n\nExit code: " + result.getExitCode();
			}

			return ToolCallResponse.of(request.getToolCallId(), request.getToolName(), output);

		} catch (Exception e) {
			log.error("Shell command execution failed", e);
			return ToolCallResponse.of(request.getToolCallId(), request.getToolName(),
				"Error: " + e.getMessage());
		}
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> parseArguments(String arguments) {
		try {
			return objectMapper.readValue(arguments, Map.class);
		} catch (Exception e) {
			throw new RuntimeException("Failed to parse tool arguments", e);
		}
	}

	private void deleteDirectory(Path directory) throws IOException {
		if (Files.exists(directory)) {
			Files.walk(directory)
				.sorted(Comparator.reverseOrder())
				.forEach(path -> {
					try {
						Files.delete(path);
					} catch (IOException e) {
						log.warn("Failed to delete: {}", path, e);
					}
				});
		}
	}

	/**
	 * Persistent shell session that executes commands sequentially.
	 */
	private class ShellSession {
		private final Path workspace;
		private final List<String> command;
		private final Map<String, String> env;
		private Process process;
		private BufferedWriter stdin;
		private BlockingQueue<String> outputQueue;
		private volatile boolean terminated;

		ShellSession(Path workspace, List<String> command, Map<String, String> env) {
			this.workspace = workspace;
			this.command = command;
			this.env = env;
			this.outputQueue = new LinkedBlockingQueue<>();
		}

		void start() throws IOException {
			ProcessBuilder pb = new ProcessBuilder(command);
			pb.directory(workspace.toFile());
			pb.environment().putAll(env);
			pb.redirectErrorStream(true);

			process = pb.start();
			stdin = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));

			// Start output reader thread
			new Thread(() -> {
				try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
					String line;
					while ((line = reader.readLine()) != null) {
						outputQueue.offer(line);
					}
				} catch (IOException e) {
					log.debug("Output reader terminated", e);
				}
			}, "shell-output-reader").start();
		}

		void restart() {
			stop(terminationTimeout);
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

		CommandResult execute(String command, long timeoutMs) {
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
				stdin.write(String.format("printf '%s %%s\\n' $?\n", marker));
				stdin.flush();

				// Collect output
				return collectOutput(marker, deadline);

			} catch (IOException e) {
				throw new RuntimeException("Failed to execute command", e);
			}
		}

		private CommandResult collectOutput(String marker, long deadline) {
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
					String line = outputQueue.poll(remaining, TimeUnit.MILLISECONDS);
					if (line == null) {
						timedOut = true;
						restart();
						break;
					}

					// Check for completion marker
					if (line.startsWith(marker)) {
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
					totalBytes += line.getBytes().length + 1; // +1 for newline

					if (totalLines <= maxOutputLines) {
						if (maxOutputBytes == null || totalBytes <= maxOutputBytes) {
							lines.add(line);
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
	 * Result of command execution.
	 */
	private static class CommandResult {
		private final String output;
		private final Integer exitCode;
		private final boolean timedOut;
		private final boolean truncatedByLines;
		private final boolean truncatedByBytes;
		private final int totalLines;
		private final long totalBytes;

		CommandResult(String output, Integer exitCode, boolean timedOut,
					  boolean truncatedByLines, boolean truncatedByBytes,
					  int totalLines, long totalBytes) {
			this.output = output;
			this.exitCode = exitCode;
			this.timedOut = timedOut;
			this.truncatedByLines = truncatedByLines;
			this.truncatedByBytes = truncatedByBytes;
			this.totalLines = totalLines;
			this.totalBytes = totalBytes;
		}

		String getOutput() { return output; }
		Integer getExitCode() { return exitCode; }
		boolean isTimedOut() { return timedOut; }
		boolean isTruncatedByLines() { return truncatedByLines; }
		boolean isTruncatedByBytes() { return truncatedByBytes; }
		int getTotalLines() { return totalLines; }
		long getTotalBytes() { return totalBytes; }
	}

	/**
	 * Redaction rule for sanitizing command output.
	 */
	public interface RedactionRule {
		String apply(String content);
	}

	/**
	 * Simple pattern-based redaction rule.
	 */
	public static class PatternRedactionRule implements RedactionRule {
		private final Pattern pattern;
		private final String replacement;

		public PatternRedactionRule(String pattern, String replacement) {
			this.pattern = Pattern.compile(pattern);
			this.replacement = replacement;
		}

		@Override
		public String apply(String content) {
			return pattern.matcher(content).replaceAll(replacement);
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
		private List<String> shellCommand = Arrays.asList("/bin/bash");
		private final Map<String, String> environment = new HashMap<>();
		private final List<RedactionRule> redactionRules = new ArrayList<>();

		public Builder workspaceRoot(String path) {
			this.workspaceRoot = Paths.get(path);
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

		public ShellToolInterceptor build() {
			return new ShellToolInterceptor(this);
		}
	}
}

