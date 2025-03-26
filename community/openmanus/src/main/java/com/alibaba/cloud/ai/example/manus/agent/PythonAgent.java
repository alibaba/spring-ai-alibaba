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
package com.alibaba.cloud.ai.example.manus.agent;

import com.alibaba.cloud.ai.example.manus.llm.LlmService;
import com.alibaba.cloud.ai.example.manus.tool.Bash;
import com.alibaba.cloud.ai.example.manus.tool.PythonExecute;
import com.alibaba.cloud.ai.example.manus.tool.Summary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.tool.ToolCallback;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class PythonAgent extends ToolCallAgent {

    private static final Logger log = LoggerFactory.getLogger(PythonAgent.class);
    private final String workingDirectory;
    private String lastResult;

    public PythonAgent(LlmService llmService, ToolCallingManager toolCallingManager, String workingDirectory) {
        super(llmService, toolCallingManager);
        this.workingDirectory = workingDirectory;
    }

    @Override
    protected boolean think() {
        // 在开始思考前清空缓存
        return super.think();
    }

    @Override
    protected Message getNextStepMessage() {
        String nextStepPrompt = """
                What should I do next to achieve my goal?

                Current Execution State:
                - Working Directory: {working_directory}
                - Last Execution Result: {last_result}

                Remember:
                1. Use PythonExecute for direct Python code execution
                2. IMPORTANT: You MUST use at least one tool in your response to make progress!

            
                """;

        PromptTemplate promptTemplate = new PromptTemplate(nextStepPrompt);
        Message userMessage = promptTemplate.createMessage(getData());
        return userMessage;
    }

    @Override
    protected Message addThinkPrompt(List<Message> messages) {
        super.addThinkPrompt(messages);
        String systemPrompt = """
                You are an AI agent specialized in Python programming and execution. Your goal is to accomplish Python-related tasks effectively and safely.

                # Response Rules
                1. CODE EXECUTION:
                - Always validate inputs
                - Handle exceptions properly
                - Use appropriate Python libraries
                - Follow Python best practices

                2. ERROR HANDLING:
                - Catch and handle exceptions
                - Validate inputs and outputs
                - Check for required dependencies
                - Monitor execution state

                3. TASK COMPLETION:
                - Track progress in memory
                - Verify results
                - Clean up resources
                - Provide clear summaries

                4. BEST PRACTICES:
                - Use virtual environments when needed
                - Install required packages
                - Follow PEP 8 guidelines
                - Document code properly
                """;

        SystemPromptTemplate promptTemplate = new SystemPromptTemplate(systemPrompt);
        Message systemMessage = promptTemplate.createMessage(getData());
        messages.add(systemMessage);
        return systemMessage;
    }

    @Override
    public String getName() {
        return "PYTHON_AGENT";
    }

    @Override
    public String getDescription() {
        return """
            PYTHON AGENT can directly execute Python code and return results. Supports libraries like math, numpy, numexpr, etc.
            Agent Usage:
            One agent step can execute Python code without need to write and run separately.
            Agent Input: What you want this Python code to do.
            Agent Output: The execution results of the Python code.
            """;
    }

    @Override
    protected String act() {
        String result = super.act();
        updateExecutionState(result);
        return result;
    }

    @Override
    public List<ToolCallback> getToolCallList() {
        return List.of(
            PythonExecute.getFunctionToolCallback(),
            Summary.getFunctionToolCallback(this, llmService.getMemory(), getConversationId())
        );
    }


    @Override
    Map<String, Object> getData() {
        Map<String, Object> data = new HashMap<>();
        Map<String, Object> parentData = super.getData();
        if (parentData != null) {
            data.putAll(parentData);
        }

        data.put("working_directory", workingDirectory);
        data.put("last_result", lastResult != null ? lastResult : "No previous execution");

        return data;
    }

    /**
     * 更新执行状态
     */
    public void updateExecutionState(String result) {
        this.lastResult = result;
    }
}
