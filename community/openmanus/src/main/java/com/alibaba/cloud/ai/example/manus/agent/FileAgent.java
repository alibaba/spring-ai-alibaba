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

import com.alibaba.cloud.ai.example.manus.config.ManusProperties;
import com.alibaba.cloud.ai.example.manus.config.startUp.ManusConfiguration.ToolCallBackContext;
import com.alibaba.cloud.ai.example.manus.llm.LlmService;
import com.alibaba.cloud.ai.example.manus.tool.Bash;
import com.alibaba.cloud.ai.example.manus.tool.DocLoaderTool;
import com.alibaba.cloud.ai.example.manus.tool.FileSaver;
import com.alibaba.cloud.ai.example.manus.tool.TerminateTool;
import com.alibaba.cloud.ai.example.manus.recorder.PlanExecutionRecorder;
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

public class FileAgent extends ToolCallAgent {

	private static final Logger log = LoggerFactory.getLogger(FileAgent.class);

	private final String workingDirectory;

	private final AtomicReference<Map<String, Object>> currentFileState = new AtomicReference<>();

	public FileAgent(LlmService llmService, ToolCallingManager toolCallingManager, String workingDirectory,
			PlanExecutionRecorder record, ManusProperties manusProperties,
			Map<String, ToolCallBackContext> toolCallbackMap) {
		super(llmService, toolCallingManager, record, manusProperties, toolCallbackMap);
		this.workingDirectory = workingDirectory;
	}

	@Override
	protected String getNextStepPromptString() {
		String nextStepPrompt = """
				What should I do next to achieve my goal?

				Remember:
				1. Check file existence before operations
				2. Handle different file types appropriately
				3. Validate file paths and content
				4. Keep track of file operations
				5. Handle potential errors
				6. IMPORTANT: You MUST use at least one tool in your response to make progress!

				Think step by step:
				1. What file operation is needed?
				2. Which tool is most appropriate?
				3. How to handle potential errors?
				4. What's the expected outcome?
				""";
		return nextStepPrompt;
	}

	@Override
	protected String act() {
		String result = super.act();
		updateFileState("file_operation", result);
		return result;
	}

	@Override
	protected Message addThinkPrompt(List<Message> messages) {
		super.addThinkPrompt(messages);
		String systemPrompt = """
				You are an AI agent specialized in file operations. Your goal is to handle file-related tasks effectively and safely.

				# Response Rules

				3. FILE OPERATIONS:
				- Always validate file paths
				- Check file existence
				- Handle different file types
				- Process content appropriately

				4. ERROR HANDLING:
				- Check file permissions
				- Handle missing files
				- Validate content format
				- Monitor operation status

				5. TASK COMPLETION:
				- Track progress in memory
				- Verify file operations
				- Clean up if necessary
				- Provide clear summaries

				6. BEST PRACTICES:
				- Use absolute paths when possible
				- Handle large files carefully
				- Maintain operation logs
				- Follow file naming conventions
				""";

		SystemPromptTemplate promptTemplate = new SystemPromptTemplate(systemPrompt);
		Message systemMessage = promptTemplate.createMessage(getData());
		messages.add(systemMessage);
		return systemMessage;
	}

	@Override
	public String getName() {
		return "FILE_AGENT";
	}

	@Override
	public String getDescription() {
		return "A file operations agent that can read and write various types of files";
	}

	@Override
	public List<ToolCallback> getToolCallList() {
		return List.of(Bash.getFunctionToolCallback(workingDirectory), DocLoaderTool.getFunctionToolCallback(),
				FileSaver.getFunctionToolCallback(), TerminateTool.getFunctionToolCallback(this));
	}

	@Override
	protected void setData(Map<String, Object> oldData) {
		Map<String, Object> data = new HashMap<>();
		Map<String, Object> parentData = super.getData();
		if (parentData != null) {
			data.putAll(parentData);
		}

		data.put("working_directory", workingDirectory);

		// 获取当前文件操作状态
		Map<String, Object> state = currentFileState.get();
		if (state != null) {
			data.put("last_operation", state.get("operation"));
			data.put("operation_result", state.get("result"));
		} else {
			data.put("last_operation", "No previous operation");
			data.put("operation_result", null);
		}
		super.setData(data);
	}

	/**
	 * 更新文件操作状态
	 */
	public void updateFileState(String operation, String result) {
		Map<String, Object> state = new HashMap<>();
		state.put("operation", operation);
		state.put("result", result);
		currentFileState.set(state);
	}

}
