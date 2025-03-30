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
import com.alibaba.cloud.ai.example.manus.recorder.PlanExecutionRecorder;
import com.alibaba.cloud.ai.example.manus.service.ChromeDriverService;
import com.alibaba.cloud.ai.example.manus.tool.BrowserUseTool;
import com.alibaba.cloud.ai.example.manus.tool.FileSaver;
import com.alibaba.cloud.ai.example.manus.tool.GoogleSearch;
import com.alibaba.cloud.ai.example.manus.tool.PythonExecute;
import com.alibaba.cloud.ai.example.manus.tool.TerminateTool;

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

public class BrowserAgent extends ToolCallAgent {

	private static final Logger log = LoggerFactory.getLogger(BrowserAgent.class);

	private final ChromeDriverService chromeService;


	// New constructor with PlanExecutionRecord
	public BrowserAgent(LlmService llmService, ToolCallingManager toolCallingManager,
			ChromeDriverService chromeService, PlanExecutionRecorder record) {
		super(llmService, toolCallingManager,record);
		this.chromeService = chromeService;
	}

	private final AtomicReference<Map<String, Object>> currentStepBrowserCache = new AtomicReference<>();

	@Override
	protected boolean think() {
		// 在开始思考前清空缓存
		currentStepBrowserCache.set(null);
		return super.think();
	}

	private Map<String, Object> getBrowserState() {
		try {
			// 首先尝试从缓存获取
			Map<String, Object> cachedState = currentStepBrowserCache.get();
			if (cachedState != null) {
				log.debug("Using cached browser state");
				return cachedState;
			}

			// 如果缓存为空，则获取新状态
			BrowserUseTool browserTool = BrowserUseTool.getInstance(chromeService,this.getPlanId());
			if (browserTool == null) {
				log.error("Failed to get browser tool instance");
				return null;
			}

			Map<String, Object> newState = browserTool.getCurrentState();
			// 更新缓存
			currentStepBrowserCache.set(newState);
			log.debug("Updated browser state cache");

			return newState;
		}
		catch (Exception e) {
			log.error("Failed to get browser state", e);
			return null;
		}
	}

	protected Message getNextStepMessage() {

		String nextStepPrompt = """
				What should I do next to achieve my goal?



				When you see [Current state starts here], focus on the following:
				- Current URL and page title:
				{url_placeholder}

				- Available tabs:
				{tabs_placeholder}

				- Interactive elements and their indices:
				{interactive_elements}

				- Content above {content_above_placeholder} or below {content_below_placeholder} the viewport (if indicated)

				- Any action results or errors:
				{results_placeholder}


				Remember:
				1. Use 'get_text' action to obtain page content instead of scrolling
				2. Don't worry about content visibility or viewport position
				3. Focus on text-based information extraction
				4. Process the obtained text data directly
				5. IMPORTANT: You MUST use at least one tool in your response to make progress!


				Consider both what's visible and what might be beyond the current viewport.
				Be methodical - remember your progress and what you've learned so far.
				""";
		PromptTemplate promptTemplate = new PromptTemplate(nextStepPrompt);
		Message userMessage = promptTemplate.createMessage(getData());
		return userMessage;
	}

	/**
	 * - To navigate: browser_use with action="go_to_url", url="..." - To click:
	 * browser_use with action="click_element", index=N - To type: browser_use with
	 * action="input_text", index=N, text="..." - To get page source: browser_use with
	 * action="get_html" - To get visible text: browser_use with action="get_text" , if
	 * you need to extract text from the page, use this action first
	 *
	 */
	@Override
	protected Message addThinkPrompt(List<Message> messages) {
		super.addThinkPrompt(messages);
		String systemPrompt = """
				You are an AI agent designed to automate browser tasks. Your goal is to accomplish the ultimate task following the rules.

				# Input Format
				Task
				Previous steps
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
				1. RESPONSE FORMAT: You must ALWAYS respond with valid JSON in this exact format:
				\\{"current_state": \\{"evaluation_previous_goal": "Success|Failed|Unknown - Analyze the current elements and the image to check if the previous goals/actions are successful like intended by the task. Mention if something unexpected happened. Shortly state why/why not",
				"memory": "Description of what has been done and what you need to remember. Be very specific. Count here ALWAYS how many times you have done something and how many remain. E.g. 0 out of 10 websites analyzed. Continue with abc and xyz",
				"next_goal": "What needs to be done with the next immediate action"\\},
				"action":[\\{"one_action_name": \\{// action-specific parameter\\}\\}, // ... more actions in sequence]\\}

				2. ACTIONS: You can specify multiple actions in a sequence, but one action name per item
				- Form filling: [\\{"input_text": \\{"index": 1, "text": "username"\\}\\}, \\{"click_element": \\{"index": 3\\}\\}]
				- Navigation: [\\{"go_to_url": \\{"url": "https://example.com"\\}\\}, \\{"extract_content": \\{"goal": "names"\\}\\}]

				3. ELEMENT INTERACTION:
				- Only use indexed elements
				- Watch for non-interactive elements

				4. NAVIGATION & ERROR HANDLING:
				- Try alternative approaches if stuck
				- Handle popups and cookies
				- Use scroll for hidden elements
				- Open new tabs for research
				- Handle captchas or find alternatives
				- Wait for page loads

				5. TASK COMPLETION:
				- Track progress in memory
				- Count iterations for repeated tasks
				- Include all findings in results
				- Use done action appropriately

				6. VISUAL CONTEXT:
				- Use provided screenshots
				- Reference element indices

				7. FORM FILLING:
				- Handle dynamic field changes

				8. EXTRACTION:
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
		return List.of(FileSaver.getFunctionToolCallback(),
				PythonExecute.getFunctionToolCallback(), BrowserUseTool.getFunctionToolCallback(chromeService,this.getPlanId()),
				TerminateTool.getFunctionToolCallback(this));
	}

	@Override
	Map<String, Object> getData() {
		Map<String, Object> data = new HashMap<>();
		Map<String, Object> parentData = super.getData();
		if (parentData != null) {
			data.putAll(parentData);
		}

		Map<String, Object> browserState = getBrowserState();
		if (browserState != null) {
			// 格式化 URL 和标题信息
			String urlInfo = String.format("\n   URL: %s\n   Title: %s", browserState.get("url"),
					browserState.get("title"));
			data.put("url_placeholder", urlInfo);

			// 格式化标签页信息
			@SuppressWarnings("unchecked")
			List<Map<String, Object>> tabs = (List<Map<String, Object>>) browserState.get("tabs");
			if (tabs != null && !tabs.isEmpty()) {
				data.put("tabs_placeholder", String.format("\n   %d tab(s) available", tabs.size()));
			}
			else {
				data.put("tabs_placeholder", "");
			}

			// 格式化滚动信息
			@SuppressWarnings("unchecked")
			Map<String, Object> scrollInfo = (Map<String, Object>) browserState.get("scroll_info");
			if (scrollInfo != null) {
				Long pixelsAbove = (Long) scrollInfo.get("pixels_above");
				Long pixelsBelow = (Long) scrollInfo.get("pixels_below");

				data.put("content_above_placeholder",
						pixelsAbove > 0 ? String.format(" (%d pixels)", pixelsAbove) : "");
				data.put("content_below_placeholder",
						pixelsBelow > 0 ? String.format(" (%d pixels)", pixelsBelow) : "");
			}

			// 添加交互元素信息
			String interactiveElements = (String) browserState.get("interactive_elements");
			if (interactiveElements != null && !interactiveElements.isEmpty()) {
				data.put("interactive_elements", interactiveElements);
			}
			else {
				data.put("interactive_elements", "");
			}

			// 添加结果信息占位符
			data.put("results_placeholder", "");

			// 添加帮助信息
			data.put("help", browserState.get("help"));

			// 保存截图信息（如果需要）
			String screenshot = (String) browserState.get("screenshot");
			if (screenshot != null) {
				data.put("screenshot", screenshot);
			}
		}

		return data;
	}

}
