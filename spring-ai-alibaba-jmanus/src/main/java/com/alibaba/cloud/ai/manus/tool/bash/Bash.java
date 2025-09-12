/*
 * Copyright 2025 the original author or authors.
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
package com.alibaba.cloud.ai.manus.tool.bash;

import com.alibaba.cloud.ai.manus.tool.AbstractBaseTool;

import com.alibaba.cloud.ai.manus.tool.code.ToolExecuteResult;
import com.alibaba.cloud.ai.manus.tool.filesystem.UnifiedDirectoryManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class Bash extends AbstractBaseTool<Bash.BashInput> {

	private final ObjectMapper objectMapper;

	private static final Logger log = LoggerFactory.getLogger(Bash.class);

	/**
	 * Internal input class for defining Bash tool input parameters
	 */
	public static class BashInput {

		private String command;

		public BashInput() {
		}

		public BashInput(String command) {
			this.command = command;
		}

		public String getCommand() {
			return command;
		}

		public void setCommand(String command) {
			this.command = command;
		}

	}

	/**
	 * Unified directory manager for directory operations
	 */
	private final UnifiedDirectoryManager unifiedDirectoryManager;

	// Add operating system information
	private static final String osName = System.getProperty("os.name");

	private final String name = "bash";

	public Bash(UnifiedDirectoryManager unifiedDirectoryManager, ObjectMapper objectMapper) {
		this.unifiedDirectoryManager = unifiedDirectoryManager;
		this.objectMapper = objectMapper;
	}

	private String lastCommand = "";

	private String lastResult = "";

	@Override
	public ToolExecuteResult run(BashInput input) {
		String command = input.getCommand();
		log.info("Bash command: {}", command);
		log.info("Current operating system: {}", osName);
		this.lastCommand = command;

		List<String> commandList = new ArrayList<>();
		commandList.add(command);

		try {
			// Use ShellExecutorFactory to create executor for corresponding operating
			// system
			ShellCommandExecutor executor = ShellExecutorFactory.createExecutor();
			log.info("Using shell executor for OS: {}", osName);
			List<String> result = executor.execute(commandList, unifiedDirectoryManager.getWorkingDirectoryPath());
			this.lastResult = String.join("\n", result);
			return new ToolExecuteResult(objectMapper.writeValueAsString(result));
		}
		catch (Exception e) {
			log.error("Error executing bash command", e);
			return new ToolExecuteResult("Error executing command: " + e.getMessage());
		}
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getDescription() {
		return String.format(
				"""
						Execute bash commands in terminal (current OS: %s).
						* Long-running commands: For commands that may run indefinitely, they should be run in background with output redirected to file, e.g.: command = `python3 app.py > server.log 2>&1 &`.
						* Interactive commands: If bash command returns exit code `-1`, this means the process is not yet complete. Assistant must send a second terminal call with empty `command` (this will retrieve any additional logs), or can send additional text (set `command` to text) to the running process's STDIN, or can send command=`ctrl+c` to interrupt the process.
						* Timeout handling: If command execution result shows "Command timed out. Sending SIGINT to the process", assistant should try to re-run the command in background.
						""",
				osName);
	}

	@Override
	public String getParameters() {
		return """
				{
				    "type": "object",
				    "properties": {
				        "command": {
				            "type": "string",
				            "description": "The bash command to execute. Can be empty to view additional logs when previous exit code is `-1`. Can be `ctrl+c` to interrupt the currently running process."
				        }
				    },
				    "required": ["command"]
				}
				""";
	}

	@Override
	public Class<BashInput> getInputType() {
		return BashInput.class;
	}

	@Override
	public String getServiceGroup() {
		return "default-service-group";
	}

	@Override
	public String getCurrentToolStateString() {
		return String.format("""
				            Current File Operation State:
				            - Working Directory:
				%s

				            - Last File Operation:
				%s

				            - Last Operation Result:
				%s

				            """, unifiedDirectoryManager.getWorkingDirectoryPath(),
				lastCommand.isEmpty() ? "No command executed yet" : lastCommand,
				lastResult.isEmpty() ? "No result yet" : lastResult);
	}

	@Override
	public void cleanup(String planId) {
		log.info("Cleaned up resources for plan: {}", planId);
	}

}
