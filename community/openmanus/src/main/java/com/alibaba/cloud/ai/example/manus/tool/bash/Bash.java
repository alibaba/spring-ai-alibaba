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
package com.alibaba.cloud.ai.example.manus.tool.bash;

import com.alibaba.cloud.ai.example.manus.agent.BaseAgent;
import com.alibaba.cloud.ai.example.manus.tool.ToolCallBiFunctionDef;
import com.alibaba.cloud.ai.example.manus.tool.code.ToolExecuteResult;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.tool.function.FunctionToolCallback;

public class Bash implements ToolCallBiFunctionDef {

	private static final Logger log = LoggerFactory.getLogger(Bash.class);

	/**
	 * bash执行工作目录
	 */
	private String workingDirectoryPath;

	// 添加操作系统信息
	private static final String osName = System.getProperty("os.name");

	private static String PARAMETERS = """
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

	private final String name = "bash";

	private final String description = String.format(
			"""
					Execute a bash command in the terminal (Current OS: %s).
						* Long running commands: For commands that may run indefinitely, it should be run in the background and the output should be redirected to a file, e.g. command = `python3 app.py > server.log 2>&1 &`.
						* Interactive: If a bash command returns exit code `-1`, this means the process is not yet finished. The assistant must then send a second call to terminal with an empty `command` (which will retrieve any additional logs), or it can send additional text (set `command` to the text) to STDIN of the running process, or it can send command=`ctrl+c` to interrupt the process.
						* Timeout: If a command execution result says "Command timed out. Sending SIGINT to the process", the assistant should retry running the command in the background.

					""",
			osName);

	public OpenAiApi.FunctionTool getToolDefinition() {
		OpenAiApi.FunctionTool.Function function = new OpenAiApi.FunctionTool.Function(description, name, PARAMETERS);
		OpenAiApi.FunctionTool functionTool = new OpenAiApi.FunctionTool(function);
		return functionTool;
	}

	public FunctionToolCallback getFunctionToolCallback(String workingDirectoryPath) {
		return FunctionToolCallback.builder(name, new Bash(workingDirectoryPath))
			.description(description)
			.inputSchema(PARAMETERS)
			.inputType(String.class)
			.build();
	}

	public Bash(String workingDirectoryPath) {
		this.workingDirectoryPath = workingDirectoryPath;
	}

	private String lastCommand = "";

	private String lastResult = "";

	public ToolExecuteResult run(String toolInput) {
		log.info("Bash toolInput:" + toolInput);
		log.info("Current operating system: " + osName);
		Map<String, Object> toolInputMap = JSON.parseObject(toolInput, new TypeReference<Map<String, Object>>() {
		});
		String command = (String) toolInputMap.get("command");
		this.lastCommand = command;

		List<String> commandList = new ArrayList<>();
		commandList.add(command);

		// 使用ShellExecutorFactory创建对应操作系统的执行器
		ShellCommandExecutor executor = ShellExecutorFactory.createExecutor();
		log.info("Using shell executor for OS: " + osName);
		List<String> result = executor.execute(commandList, workingDirectoryPath);
		this.lastResult = String.join("\n", result);
		return new ToolExecuteResult(JSON.toJSONString(result));
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getDescription() {
		return description;
	}

	@Override
	public String getParameters() {
		return PARAMETERS;
	}

	@Override
	public Class<?> getInputType() {
		return String.class;
	}

	@Override
	public boolean isReturnDirect() {
		return false;
	}

	@Override
	public ToolExecuteResult apply(String s, ToolContext toolContext) {
		return run(s);
	}

	private BaseAgent agent;

	@Override
	public void setAgent(BaseAgent agent) {
		this.agent = agent;
	}

	public BaseAgent getAgent() {
		return this.agent;
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

				            """, workingDirectoryPath, lastCommand.isEmpty() ? "No command executed yet" : lastCommand,
				lastResult.isEmpty() ? "No result yet" : lastResult);
	}

	@Override
	public void cleanup(String planId) {
		log.info("Cleaned up resources for plan: " + planId);
	}

}
