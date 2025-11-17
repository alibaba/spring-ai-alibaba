package com.alibaba.cloud.ai.examples.chatbot;

import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.tools.ShellTool;
import com.alibaba.cloud.ai.graph.agent.extension.tools.filesystem.ReadFileTool;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.BiFunction;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

@Configuration
public class ChatbotAgent {

    private static final String INSTRUCTION = """
You are a helpful assistant named SAA.
You have access to tools that can help you execute shell commands, run Python code, and view text files.
Use these tools to assist users with their tasks.
""";

    @Bean
    public ReactAgent chatbotReactAgent(ChatModel chatModel,
                                       ToolCallback executeShellCommand,
                                       ToolCallback executePythonCode,
                                       ToolCallback viewTextFile) {
        return ReactAgent.builder()
            .name("SAA")
            .model(chatModel)
            .instruction(INSTRUCTION)
            .enableLogging(true)
            .tools(
                executeShellCommand,
                executePythonCode,
                viewTextFile
            )
            .build();
    }

    // Tool: execute_shell_command
    @Bean
    public ToolCallback executeShellCommand() {
        // Use ShellTool with a temporary workspace directory
        String workspaceRoot = System.getProperty("java.io.tmpdir") + File.separator + "agent-workspace";
        return ShellTool.builder(workspaceRoot)
            .withName("execute_shell_command")
            .withDescription("Execute a shell command inside a persistent session. Before running a command, " +
                    "confirm the working directory is correct (e.g., inspect with `ls` or `pwd`) and ensure " +
                    "any parent directories exist. Prefer absolute paths and quote paths containing spaces, " +
                    "such as `cd \"/path/with spaces\"`. Chain multiple commands with `&&` or `;` instead of " +
                    "embedding newlines. Avoid unnecessary `cd` usage unless explicitly required so the " +
                    "session remains stable. Outputs may be truncated when they become very large, and long " +
                    "running commands will be terminated once their configured timeout elapses.")
            .build();
    }

    // Tool: execute_python_code
    @Bean
    public ToolCallback executePythonCode() {
        return FunctionToolCallback.builder("execute_python_code", new PythonTool())
            .description(PythonTool.DESCRIPTION)
            .inputType(PythonTool.PythonRequest.class)
            .build();
    }

    // Tool: view_text_file
    @Bean
    public ToolCallback viewTextFile() {
        // Create a custom wrapper to match the original tool name
        ReadFileTool readFileTool = new ReadFileTool();
        return FunctionToolCallback.builder("view_text_file", readFileTool)
            .description("View the contents of a text file. The file_path parameter must be an absolute path. " +
                    "You can specify offset and limit to read specific portions of the file. " +
                    "By default, reads up to 500 lines starting from the beginning of the file.")
            .inputType(ReadFileTool.ReadFileRequest.class)
            .build();
    }

}

