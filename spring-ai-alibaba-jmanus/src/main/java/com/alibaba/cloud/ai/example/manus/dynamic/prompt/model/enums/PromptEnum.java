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
package com.alibaba.cloud.ai.example.manus.dynamic.prompt.model.enums;

import org.springframework.ai.chat.messages.MessageType;

public enum PromptEnum {

	// LLL_FINALIZE_SYSTEM("LLL_FINALIZE_SYSTEM", MessageType.SYSTEM, PromptType.LLM,
	// true, "用来做最终总结的prompt，对应任务结束以后告知用户的那个动作", "llm/finalize-system.txt"),
	// LLL_MANUS_SYSTEM("LLL_MANUS_SYSTEM", MessageType.SYSTEM, PromptType.LLM, true,
	// "用来做开始的用户任务分解用的prompt", "llm/manus-system.txt"),
	// LLL_PLANNING_SYSTEM("LLL_PLANNING_SYSTEM", MessageType.SYSTEM, PromptType.LLM,
	// true, "", "llm/planning-system.txt"),
	PLANNING_PLAN_CREATION("PLANNING_PLAN_CREATION", MessageType.SYSTEM, PromptType.PLANNING, true,
			"构建执行计划的Prompt，如果分解任务做的不好，调这个 / Prompt for building execution plans, adjust this if task decomposition is not working well",
			"planning/plan-creation.txt"),
	AGENT_CURRENT_STEP_ENV("AGENT_CURRENT_STEP_ENV", MessageType.USER, PromptType.AGENT, true,
			"用来定义当前的环境信息，对应agent从过去调用的所有函数的结果，也就是当前的环境信息，因为要存到agent里面所以单独有一个项 / Defines current environment information, corresponding to results from all functions called by agent in the past, stored separately in agent",
			"agent/current-step-env.txt"),
	AGENT_STEP_EXECUTION("AGENT_STEP_EXECUTION", MessageType.USER, PromptType.AGENT, true,
			"每个agent执行步骤时候都会给agent的上下文信息，大部分的变量不要调（因为都是预制的），可以调整一些对他的建议，一个重点的agent步骤执行prompt / Context information given to agent during each execution step, most variables are preset and shouldn't be changed, can adjust some suggestions, a key agent step execution prompt",
			"agent/step-execution.txt"),

	PLANNING_PLAN_FINALIZER("PLANNING_PLAN_FINALIZER", MessageType.USER, PromptType.PLANNING, true,
			"用来做最终总结的prompt，对应任务结束以后告知用户的那个动作，已合并用户请求信息 / Prompt for final summary, corresponds to the action of informing users after task completion, merged with user request information",
			"planning/plan-finalizer.txt"),;

	private String promptName;

	private MessageType messageType;

	private PromptType type;

	private Boolean builtIn;

	private String promptDescription;

	private String promptPath;

	PromptEnum(String promptName, MessageType messageType, PromptType type, Boolean builtIn, String promptDescription,
			String promptPath) {
		this.promptName = promptName;
		this.messageType = messageType;
		this.type = type;
		this.builtIn = builtIn;
		this.promptDescription = promptDescription;
		this.promptPath = promptPath;
	}

	public String getPromptName() {
		return promptName;
	}

	public void setPromptName(String promptName) {
		this.promptName = promptName;
	}

	public MessageType getMessageType() {
		return messageType;
	}

	public Boolean getBuiltIn() {
		return builtIn;
	}

	public void setBuiltIn(Boolean builtIn) {
		this.builtIn = builtIn;
	}

	public void setMessageType(MessageType messageType) {
		this.messageType = messageType;
	}

	public PromptType getType() {
		return type;
	}

	public void setType(PromptType type) {
		this.type = type;
	}

	public String getPromptPath() {
		return promptPath;
	}

	public void setPromptPath(String promptPath) {
		this.promptPath = promptPath;
	}

	public boolean isBuiltIn() {
		return builtIn;
	}

	public void setBuiltIn(boolean builtIn) {
		this.builtIn = builtIn;
	}

	public String getPromptDescription() {
		return promptDescription;
	}

	public void setPromptDescription(String promptDescription) {
		this.promptDescription = promptDescription;
	}

}
