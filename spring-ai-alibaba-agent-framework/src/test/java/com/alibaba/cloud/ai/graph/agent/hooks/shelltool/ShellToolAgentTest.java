/*
 * Copyright 2024-2025 the original author or authors.
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
package com.alibaba.cloud.ai.graph.agent.hooks.shelltool;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.graph.CompileConfig;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.hook.shelltool.ShellToolAgentHook;
import com.alibaba.cloud.ai.graph.agent.tools.ShellTool;
import com.alibaba.cloud.ai.graph.checkpoint.config.SaverConfig;
import com.alibaba.cloud.ai.graph.checkpoint.constant.SaverEnum;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@EnabledIfEnvironmentVariable(named = "AI_DASHSCOPE_API_KEY", matches = ".+")
public class ShellToolAgentTest {

	private ChatModel chatModel;

	@BeforeEach
	void setUp() {
		// Create DashScopeApi instance using the API key from environment variable
		DashScopeApi dashScopeApi = DashScopeApi.builder().apiKey(System.getenv("AI_DASHSCOPE_API_KEY")).build();

		// Create DashScope ChatModel instance
		this.chatModel = DashScopeChatModel.builder().dashScopeApi(dashScopeApi).build();
	}

	@Test
	public void testShellToolWithShellToolAgentHook() throws Exception {
		Path tempWorkspace = Files.createTempDirectory("shelltool_test");
		
		try {

			ShellTool.Builder shellToolBuilder = ShellTool.builder(tempWorkspace.toString())
				.withCommandTimeout(30000)  // 30秒超时
				.withMaxOutputLines(500);

			if (System.getProperty("os.name").toLowerCase().contains("windows")) {
				shellToolBuilder = shellToolBuilder.withShellCommand(List.of("powershell.exe"));
			}
			
			ToolCallback shellToolCallback = shellToolBuilder.build();
			
			List<ToolCallback> tools = List.of(shellToolCallback);

			ShellToolAgentHook shellToolAgentHook = new ShellToolAgentHook();

			ReactAgent agent = ReactAgent.builder()
				.name("shell-tool-agent-example")
				.model(chatModel)
				.instruction("You are a helpful assistant that can execute shell commands when needed. " +
					"Use the shell tool to run commands and report the results. " +
					"When creating files, always put them in the current directory unless specified otherwise. " +
					"Prefer using 'ls' or 'dir' command to see the current directory contents.")
				.tools(tools)
				.hooks(List.of(shellToolAgentHook))
				.compileConfig(getCompileConfig())
				.build();

			List<Message> messages = new ArrayList<>();
			String testCommand = System.getProperty("os.name").toLowerCase().contains("windows") ?
				"请帮我创建一个名为 test.txt 的文件，内容为 'Hello from ShellTool!'，然后列出当前目录的文件，最后显示 test.txt 文件的内容。请使用Windows PowerShell兼容的命令。" :
				"请帮我完成以下任务：\n" +
				"1. 创建一个名为 test.txt 的文件，内容为 'Hello from ShellTool!'\n" +
				"2. 列出当前目录的文件\n" +
				"3. 显示 test.txt 文件的内容\n" +
				"请按步骤执行，每步执行完告诉我结果。";

			messages.add(new UserMessage(testCommand));

			// 执行 Agent
			System.out.println("开始执行 Agent...");
			Optional<OverAllState> result = agent.invoke(messages);

			assertTrue(result.isPresent(), "Agent 应该返回结果");
			Object messagesObj = result.get().value("messages").get();
			assertNotNull(messagesObj, "返回的消息不应该为 null");
			
			System.out.println("Agent 执行成功，返回消息数量: " + 
				(messagesObj instanceof List ? ((List<?>) messagesObj).size() : "未知"));
			System.out.println("Agent 结果: " + messagesObj);
			System.out.println("✓ ShellTool 与 ShellToolAgentHook 集成测试执行成功");
		}
		finally {
			// 清理临时目录
			deleteDirectory(tempWorkspace);
		}
	}

	private static CompileConfig getCompileConfig() {
		SaverConfig saverConfig = SaverConfig.builder()
			.register(SaverEnum.MEMORY.getValue(), new MemorySaver())
			.build();
		return CompileConfig.builder().saverConfig(saverConfig).build();
	}

	private void deleteDirectory(Path directory) throws IOException {
		if (Files.exists(directory)) {
			Files.walk(directory)
				.sorted(java.util.Comparator.reverseOrder())
				.map(Path::toFile)
				.forEach(java.io.File::delete);
		}
	}
}
