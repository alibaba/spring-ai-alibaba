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

import java.util.List;

import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;

import com.alibaba.cloud.ai.example.manus.llm.LlmService;

public abstract class ReActAgent extends BaseAgent {

	public ReActAgent(LlmService llmService) {
		super(llmService);
	}

	protected abstract boolean think();

	protected abstract String act();

	@Override
	public String step() {
		boolean shouldAct = think();
		if (!shouldAct) {
			return "Thinking complete - no action needed";
		}
		return act();
	}

	/**
	 * 获取当前的思考过程提示词
	 * @return 返回适用于当前状态的思考提示词
	 */
	protected Message addThinkPrompt(List<Message> messages) {
		String prompt =  """
			Given the current state and available tools, let's think step by step:
			1. Analyze: What is the current situation?
			2. Goal: What are we trying to achieve?
			3. Options: What tools and actions are available?
			4. Decision: What's the best next step?
			5. Validation: How will we know if it works?
			""";

		messages.add(new SystemMessage(prompt));
		return new SystemMessage(prompt);
	}

}
