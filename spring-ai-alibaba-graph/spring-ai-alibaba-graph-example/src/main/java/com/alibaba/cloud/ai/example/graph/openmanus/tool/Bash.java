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
package com.alibaba.cloud.ai.example.graph.openmanus.tool;

import com.alibaba.cloud.ai.example.graph.openmanus.tool.support.ToolExecuteResult;
import com.alibaba.cloud.ai.example.graph.openmanus.tool.support.llmbash.BashProcess;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.ai.tool.function.FunctionToolCallback;

public class Bash implements Function<String, ToolExecuteResult> {

	private static final Logger log = LoggerFactory.getLogger(Bash.class);

	/**
	 * bash执行工作目录
	 */
	private String workingDirectoryPath;

	public static final String PARAMETERS = """
			{
			    "type": "object",
			    "properties": {
			        "command": {
			            "description": "The bash command to execute. Can be empty to view additional logs when previous exit code is `-1`. Can be `ctrl+c` to interrupt the currently running process.",
			            "type": "string"
			        }
			    },
			    "required": ["command"],
			    "additionalProperties": false
			}
			""";

	private static final String name = "bash";

	public static final String description = """
			Execute a bash command in the terminal.
			* Long running commands: For commands that may run indefinitely, it should be run in the background and the output should be redirected to a file, e.g. command = `python3 app.py > server.log 2>&1 &`.
			* Interactive: If a bash command returns exit code `-1`, this means the process is not yet finished. The assistant must then send a second call to terminal with an empty `command` (which will retrieve any additional logs), or it can send additional text (set `command` to the text) to STDIN of the running process, or it can send command=`ctrl+c` to interrupt the process.
			* Timeout: If a command execution result says "Command timed out. Sending SIGINT to the process", the assistant should retry running the command in the background.
			""";

	public static OpenAiApi.FunctionTool getToolDefinition() {
		OpenAiApi.FunctionTool.Function function = new OpenAiApi.FunctionTool.Function(description, name, PARAMETERS);
		OpenAiApi.FunctionTool functionTool = new OpenAiApi.FunctionTool(function);
		return functionTool;
	}

	public static FunctionToolCallback getFunctionToolCallback(String workingDirectoryPath) {
		return FunctionToolCallback.builder(name, new Bash(workingDirectoryPath))
			.description(description)
			.inputSchema(PARAMETERS)
			.inputType(String.class)
			.build();
	}

	public Bash(String workingDirectoryPath) {
		this.workingDirectoryPath = workingDirectoryPath;
	}

	public ToolExecuteResult run(String toolInput) {
		log.info("Bash toolInput:{}", toolInput);
		Map<String, Object> toolInputMap = JSON.parseObject(toolInput, new TypeReference<Map<String, Object>>() {
		});
		String command = (String) toolInputMap.get("command");
		List<String> commandList = new ArrayList<>();
		commandList.add(command);
		List<String> result = BashProcess.executeCommand(commandList, workingDirectoryPath);
		return new ToolExecuteResult(JSON.toJSONString(result));
	}

	public String getWorkingDirectoryPath() {
		return workingDirectoryPath;
	}

	public void setWorkingDirectoryPath(String workingDirectoryPath) {
		this.workingDirectoryPath = workingDirectoryPath;
	}

	@Override
	public ToolExecuteResult apply(@ToolParam(description = Bash.PARAMETERS) String s) {
		return run(s);
	}

}
