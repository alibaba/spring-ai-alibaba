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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

import static com.alibaba.cloud.ai.graph.agent.tools.ToolContextConstants.AGENT_CONFIG_CONTEXT_KEY;

/**
 * A tool for executing shell commands.
 *
 * This tool allows for the execution of shell commands within a managed session.
 * The session's lifecycle and configuration are handled by the {@link ShellSessionManager}.
 *
 * Example of creating a callback for this tool:
 * <pre>
 * ToolCallback shellToolCallback = ShellTool.createShellToolCallback("/tmp/agent-workspace");
 * </pre>
 */
public class ShellTool implements BiFunction<ShellTool.Request, ToolContext, String> {

	private static final Logger log = LoggerFactory.getLogger(ShellTool.class);

	public static final String DEFAULT_TOOL_DESCRIPTION =
			"Execute a shell command inside a persistent session. Before running a command, "
					+ "confirm the working directory is correct (e.g., inspect with `ls` or `pwd`) and ensure "
					+ "any parent directories exist. Prefer absolute paths and quote paths containing spaces, "
					+ "such as `cd \"/path/with spaces\"`. Chain multiple commands with `&&` or `;` instead of "
					+ "embedding newlines. Avoid unnecessary `cd` usage unless explicitly required so the "
					+ "session remains stable. Outputs may be truncated when they become very large, and long "
					+ "running commands will be terminated once their configured timeout elapses.";


	private final ShellSessionManager sessionManager;

	/**
	 * Constructs a new ShellTool.
	 *
	 * @param sessionManager The manager for the shell session. Must not be null.
	 */
	public ShellTool(ShellSessionManager sessionManager) {
		if (sessionManager == null) {
			throw new IllegalArgumentException("ShellSessionManager cannot be null");
		}
		this.sessionManager = sessionManager;
	}

	/**
	 * Defines the parameters for a shell tool request.
	 *
	 * @param command The shell command to execute. Can be null if only restarting the session.
	 * @param restart If true, the shell session will be restarted before executing any command.
	 */
	public record Request(
			@JsonProperty("command")
			@JsonPropertyDescription("The command to execute in the shell.")
			String command,

			@JsonProperty(value = "restart", defaultValue = "false")
			@JsonPropertyDescription("Restart the shell session before executing the command.")
			Boolean restart
	) {}

	@Override
	public String apply(Request request, ToolContext toolContext) {
		try {
			RunnableConfig config = (RunnableConfig) toolContext.getContext().get(AGENT_CONFIG_CONTEXT_KEY);
			// Handle restart request
			if (Boolean.TRUE.equals(request.restart())) {
				log.info("Restarting shell session as requested.");
				sessionManager.restartSession(config);
				if (request.command() == null || request.command().trim().isEmpty()) {
					return "Shell session restarted successfully.";
				}
			}

			// Execute command
			String command = request.command();
			if (command == null || command.trim().isEmpty()) {
				return "Error: Command cannot be empty.";
			}

			log.info("Executing shell command: {}", command);
			ShellSessionManager.CommandResult result = sessionManager.executeCommand(command, config);

			// Format the output
			return formatResult(result);

		} catch (Exception e) {
			log.error("Shell command execution failed unexpectedly.", e);
			return "Error: " + e.getMessage();
		}
	}

	private String formatResult(ShellSessionManager.CommandResult result) {
		StringBuilder outputBuilder = new StringBuilder();

		// Handle timeout
		if (result.isTimedOut()) {
			outputBuilder.append("Error: Command timed out.");
			return outputBuilder.toString();
		}

		// Append standard output/error
		String output = result.getOutput();
		outputBuilder.append(output.isEmpty() ? "<no output>" : output);

		// Add truncation info
		if (result.isTruncatedByLines()) {
			outputBuilder.append(String.format("\n\n... Output truncated at %d lines (observed %d).",
					sessionManager.getMaxOutputLines(), result.getTotalLines()));
		}
		if (result.isTruncatedByBytes() && sessionManager.getMaxOutputBytes() != null) {
			outputBuilder.append(String.format("\n\n... Output truncated at %d bytes (observed %d).",
					sessionManager.getMaxOutputBytes(), result.getTotalBytes()));
		}

		// Add exit code for non-zero exit codes
		if (result.getExitCode() != null && result.getExitCode() != 0) {
			outputBuilder.append("\n\nExit code: ").append(result.getExitCode());
		}

		return outputBuilder.toString();
	}

	public ShellSessionManager getSessionManager() {
		return sessionManager;
	}

	public static Builder builder(String workspaceRoot) {
		return new Builder(workspaceRoot);
	}

	public static class Builder {

		private final String workspaceRoot;

		private String name = "shell";

		private String description = DEFAULT_TOOL_DESCRIPTION;

		private List<String> startupCommands;

		private List<String> shutdownCommands;

		private long commandTimeout = 60000;

		private int maxOutputLines = 1000;

		private List<String> shellCommand;

		private Map<String, String> environment;

		public Builder(String workspaceRoot) {
			this.workspaceRoot = workspaceRoot;
		}

		public Builder withName(String name) {
			this.name = name;
			return this;
		}

		public Builder withDescription(String description) {
			this.description = description;
			return this;
		}

		public Builder withStartupCommands(List<String> startupCommands) {
			this.startupCommands = startupCommands;
			return this;
		}

		public Builder withShutdownCommands(List<String> shutdownCommands) {
			this.shutdownCommands = shutdownCommands;
			return this;
		}

		public Builder withCommandTimeout(long commandTimeout) {
			this.commandTimeout = commandTimeout;
			return this;
		}

		public Builder withMaxOutputLines(int maxOutputLines) {
			this.maxOutputLines = maxOutputLines;
			return this;
		}

		public Builder withShellCommand(List<String> shellCommand) {
			this.shellCommand = shellCommand;
			return this;
		}

		public Builder withEnvironment(Map<String, String> environment) {
			this.environment = environment;
			return this;
		}

		public ToolCallback build() {
			ShellSessionManager.Builder sessionManagerBuilder = ShellSessionManager.builder()
				.workspaceRoot(Path.of(workspaceRoot))
				.commandTimeout(commandTimeout)
				.maxOutputLines(maxOutputLines);

			if (startupCommands != null) {
				sessionManagerBuilder.setStartupCommand(startupCommands);
			}
			if (shutdownCommands != null) {
				sessionManagerBuilder.setShutdownCommand(shutdownCommands);
			}
			if (shellCommand != null) {
				sessionManagerBuilder.shellCommand(shellCommand);
			}
			if (environment != null) {
				sessionManagerBuilder.environment(environment);
			}

			ShellSessionManager sessionManager = sessionManagerBuilder.build();
			ShellTool shellTool = new ShellTool(sessionManager);

			return FunctionToolCallback.builder(name, shellTool)
				.description(description)
				.inputType(Request.class)
				.build();
		}

	}

}
