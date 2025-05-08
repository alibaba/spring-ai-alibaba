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
					在终端中执行bash命令（当前操作系统：%s）。
						* 长时间运行的命令：对于可能无限期运行的命令，应该在后台运行并将输出重定向到文件，例如：command = `python3 app.py > server.log 2>&1 &`。
						* 交互式命令：如果bash命令返回退出码`-1`，这意味着进程尚未完成。助手必须发送第二个带有空`command`的终端调用（这将检索任何其他日志），或者可以发送额外的文本（将`command`设置为文本）到运行进程的STDIN，或者可以发送command=`ctrl+c`中断进程。
						* 超时处理：如果命令执行结果显示"Command timed out. Sending SIGINT to the process"，助手应尝试在后台重新运行该命令。

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
		log.info("Bash toolInput:{}", toolInput);
		log.info("Current operating system: {}", osName);
		Map<String, Object> toolInputMap = JSON.parseObject(toolInput, new TypeReference<Map<String, Object>>() {
		});
		String command = (String) toolInputMap.get("command");
		this.lastCommand = command;

		List<String> commandList = new ArrayList<>();
		commandList.add(command);

		// 使用ShellExecutorFactory创建对应操作系统的执行器
		ShellCommandExecutor executor = ShellExecutorFactory.createExecutor();
		log.info("Using shell executor for OS: {}", osName);
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

	@Override
	public String getServiceGroup() {
		return "default-service-group";
	}

	private String planId;

	@Override
	public void setPlanId(String planId) {
		this.planId = planId;
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
		log.info("Cleaned up resources for plan: {}", planId);
	}

}
