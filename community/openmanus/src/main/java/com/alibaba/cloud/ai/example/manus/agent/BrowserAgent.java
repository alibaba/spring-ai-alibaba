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
import com.alibaba.cloud.ai.example.manus.recorder.PlanExecutionRecorder;
import com.alibaba.cloud.ai.example.manus.service.ChromeDriverService;
import com.alibaba.cloud.ai.example.manus.tool.BrowserUseTool;
import com.alibaba.cloud.ai.example.manus.tool.FileSaver;
import com.alibaba.cloud.ai.example.manus.tool.TerminateTool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.tool.ToolCallback;

import java.util.List;
import java.util.Map;

public class BrowserAgent extends ToolCallAgent {

	private static final Logger log = LoggerFactory.getLogger(BrowserAgent.class);

	private final ChromeDriverService chromeService;

	// New constructor with PlanExecutionRecord
	public BrowserAgent(LlmService llmService, ToolCallingManager toolCallingManager, ChromeDriverService chromeService,
			PlanExecutionRecorder record, ManusProperties manusProperties,
			Map<String, ToolCallBackContext> toolCallbackMap) {
		super(llmService, toolCallingManager, record, manusProperties, toolCallbackMap);
		this.chromeService = chromeService;
	}

	@Override
	protected boolean think() {
		return super.think();
	}

	@Override
	protected String getNextStepPromptString() {
		return """
				What should I do for next action to achieve my goal?
				Remember:
				1. Use 'get_text' action to obtain page content instead of scrolling
				2. Don't worry about content visibility or viewport position
				3. Focus on text-based information extraction
				4. Process the obtained text data directly
				5. IMPORTANT: You MUST use at least one tool in your response to make progress!


				Consider both what's visible and what might be beyond the current viewport.
				Be methodical - remember your progress and what you've learned so far.
				""";

	}

	@Override
	protected Message addThinkPrompt(List<Message> messages) {
		super.addThinkPrompt(messages);
		String systemPrompt = """
				You are an AI agent designed to automate browser tasks. Your goal is to accomplish the ultimate task following the rules.

				# Input Format
				Task
				Previous actions
				Current URL
				Open Tabs
				Interactive Elements
				[index]<type>text</type>
				- index: Numeric identifier for interaction
				- type: HTML element type (button, input, etc.)
				- text: Element description
				Example:
				[33]<button>Submit Form</button>

				- Only elements with numeric indexes in [] are interactive
				- elements without [] provide only context

				# Response Rules
				1. ACTIONS: You can specify multiple actions in a sequence, but one action name per item
				- Form filling: [\\{"input_text": \\{"index": 1, "text": "username"\\}\\}, \\{"click_element": \\{"index": 3\\}\\}]
				- Navigation: [\\{"go_to_url": \\{"url": "https://example.com"\\}\\}, \\{"extract_content": \\{"goal": "names"\\}\\}]

				2. ELEMENT INTERACTION:
				- Only use indexed elements
				- Watch for non-interactive elements

				3. NAVIGATION & ERROR HANDLING:
				- Try alternative approaches if stuck
				- Handle popups and cookies
				- Use scroll for hidden elements
				- Open new tabs for research
				- Handle captchas or find alternatives
				- Wait for page loads

				4. TASK COMPLETION:
				- Track progress in memory
				- Count iterations for repeated tasks
				- Include all findings in results
				- Use done action appropriately

				5. VISUAL CONTEXT:
				- Use provided screenshots
				- Reference element indices

				6. FORM FILLING:
				- Handle dynamic field changes

				7. EXTRACTION:
				- Use extract_content for information gathering
				""";
		SystemPromptTemplate promptTemplate = new SystemPromptTemplate(systemPrompt);

		Message systemMessage = promptTemplate.createMessage(getData());

		messages.add(systemMessage);
		return systemMessage;
	}

	@Override
	public String getName() {
		return "BROWSER_AGENT";
	}

	@Override
	public String getDescription() {
		return "A browser agent that can control a browser to accomplish tasks ";
	}

	public List<ToolCallback> getToolCallList() {
		return List.of(FileSaver.getFunctionToolCallback(), BrowserUseTool.getFunctionToolCallback(chromeService),
				TerminateTool.getFunctionToolCallback(this));
	}

}
