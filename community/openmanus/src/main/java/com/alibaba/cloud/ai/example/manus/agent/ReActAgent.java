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

import com.alibaba.cloud.ai.example.manus.llm.LlmService;

/**
 * ReAct（Reasoning + Acting）模式的智能体基类 实现了思考(Reasoning)和行动(Acting)交替执行的智能体模式
 */
public abstract class ReActAgent extends BaseAgent {

	/**
	 * 构造函数
	 * @param llmService LLM服务实例，用于处理自然语言交互
	 */
	public ReActAgent(LlmService llmService) {
		super(llmService);
	}

	/**
	 * 执行思考过程，判断是否需要采取行动
	 *
	 * 子类实现要求： 1. 分析当前状态和上下文 2. 进行逻辑推理，得出下一步行动的决策 3. 返回是否需要执行行动
	 *
	 * 示例实现： - 如果需要调用工具，返回true - 如果当前步骤已完成，返回false
	 * @return true表示需要执行行动，false表示当前不需要行动
	 */
	protected abstract boolean think();

	/**
	 * 执行具体的行动
	 *
	 * 子类实现要求： 1. 基于think()的决策执行具体操作 2. 可以是工具调用、状态更新等具体行为 3. 返回执行结果的描述信息
	 *
	 * 示例实现： - ToolCallAgent：执行选定的工具调用 - BrowserAgent：执行浏览器操作
	 * @return 行动执行的结果描述
	 */
	protected abstract String act();

	/**
	 * 执行一个完整的思考-行动步骤
	 * @return 如果不需要行动则返回思考完成的消息，否则返回行动的执行结果
	 */
	@Override
	public String step() {
		boolean shouldAct = think();
		if (!shouldAct) {
			return "Thinking complete - no action needed";
		}
		return act();
	}

	/**
	 * 添加思考过程的提示词到消息列表
	 *
	 * 实现说明： 1. 构建引导智能体进行思考的系统提示词 2. 提示词应包含： - 当前执行状态 - 任务目标 - 思考方向指导 3.
	 * 可由子类进行扩展，添加特定的思考模式
	 *
	 * 子类扩展参考： - ToolCallAgent：添加工具选择相关的思考提示 - BrowserAgent：添加网页操作相关的思考提示
	 * @param messages 当前的消息列表
	 * @return 系统提示消息对象
	 */
	protected Message addThinkPrompt(List<Message> messages) {
		String prompt = "";
		// TODO: 根据当前状态和消息列表生成合适的提示词
		return null;
	}

}
