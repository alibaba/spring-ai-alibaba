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
			"构建执行计划的Prompt，如果分解任务做的不好，调这个",
			"Prompt for building execution plans, adjust this if task decomposition is not working well",
			"planning/plan-creation.txt"),
	AGENT_CURRENT_STEP_ENV("AGENT_CURRENT_STEP_ENV", MessageType.USER, PromptType.AGENT, true,
			"用来定义当前的环境信息，对应agent从过去调用的所有函数的结果，也就是当前的环境信息，因为要存到agent里面所以单独有一个项",
			"Defines current environment information, corresponding to results from all functions called by agent in the past, stored separately in agent",
			"agent/current-step-env.txt"),
	AGENT_STEP_EXECUTION("AGENT_STEP_EXECUTION", MessageType.USER, PromptType.AGENT, true,
			"每个agent执行步骤时候都会给agent的上下文信息，大部分的变量不要调（因为都是预制的），可以调整一些对他的建议，一个重点的agent步骤执行prompt",
			"Context information given to agent during each execution step, most variables are preset and shouldn't be changed, can adjust some suggestions, a key agent step execution prompt",
			"agent/step-execution.txt"),

	PLANNING_PLAN_FINALIZER("PLANNING_PLAN_FINALIZER", MessageType.USER, PromptType.PLANNING, true,
			"用来做最终总结的prompt，对应任务结束以后告知用户的那个动作，已合并用户请求信息",
			"Prompt for final summary, corresponds to the action of informing users after task completion, merged with user request information",
			"planning/plan-finalizer.txt"),

	DIRECT_RESPONSE("DIRECT_RESPONSE", MessageType.USER, PromptType.PLANNING, true,
			"用于直接反馈模式的prompt，当用户请求无需复杂规划时直接返回结果",
			"Prompt for direct response mode, directly returns results when user requests don't need complex planning",
			"planning/direct-response.txt"),

	// Agent相关的prompt
	AGENT_STUCK_ERROR("AGENT_STUCK_ERROR", MessageType.SYSTEM, PromptType.AGENT, true, "Agent执行卡住时的错误提示信息",
			"Error message when agent execution gets stuck", "agent/stuck-error.txt"),

	// 工作流相关的prompt
	SUMMARY_PLAN_TEMPLATE("SUMMARY_PLAN_TEMPLATE", MessageType.SYSTEM, PromptType.PLANNING, true, "内容总结执行计划的JSON模板",
			"JSON template for content summary execution plan", "workflow/summary-plan-template.txt"),

	// 工具相关的prompt
	MAPREDUCE_TOOL_DESCRIPTION("MAPREDUCE_TOOL_DESCRIPTION", MessageType.SYSTEM, PromptType.AGENT, true,
			"MapReduce计划工具的描述信息", "Description for MapReduce planning tool", "tool/mapreduce-tool-description.txt"),

	MAPREDUCE_TOOL_PARAMETERS("MAPREDUCE_TOOL_PARAMETERS", MessageType.SYSTEM, PromptType.AGENT, true,
			"MapReduce计划工具的参数定义JSON", "Parameter definition JSON for MapReduce planning tool",
			"tool/mapreduce-tool-parameters.txt"),

	// BaseAgent中的硬编码prompt
	AGENT_DEBUG_DETAIL_OUTPUT("AGENT_DEBUG_DETAIL_OUTPUT", MessageType.SYSTEM, PromptType.AGENT, true,
			"Agent调试模式下的详细输出要求", "Detailed output requirements for agent debug mode", "agent/debug-detail-output.txt"),

	AGENT_NORMAL_OUTPUT("AGENT_NORMAL_OUTPUT", MessageType.SYSTEM, PromptType.AGENT, true, "Agent正常模式下的输出要求",
			"Output requirements for agent normal mode", "agent/normal-output.txt"),

	AGENT_PARALLEL_TOOL_CALLS_RESPONSE("AGENT_PARALLEL_TOOL_CALLS_RESPONSE", MessageType.SYSTEM, PromptType.AGENT, true,
			"Agent并行工具调用的响应规则", "Response rules for agent parallel tool calls",
			"agent/parallel-tool-calls-response.txt"),

	FORM_INPUT_TOOL_DESCRIPTION("FORM_INPUT_TOOL_DESCRIPTION", MessageType.SYSTEM, PromptType.AGENT, true,
			"表单输入工具的描述信息", "Description for form input tool", "tool/form-input-tool-description.txt"),

	FORM_INPUT_TOOL_PARAMETERS("FORM_INPUT_TOOL_PARAMETERS", MessageType.SYSTEM, PromptType.AGENT, true,
			"表单输入工具的参数定义JSON", "Parameter definition JSON for form input tool", "tool/form-input-tool-parameters.txt");

	private String promptName;

	private MessageType messageType;

	private PromptType type;

	private Boolean builtIn;

	private String promptDescriptionZh;

	private String promptDescriptionEn;

	private String promptPath;

	public static final String[] SUPPORTED_LANGUAGES = { "zh", "en" };

	PromptEnum(String promptName, MessageType messageType, PromptType type, Boolean builtIn, String promptDescriptionZh,
			String promptDescriptionEn, String promptPath) {
		this.promptName = promptName;
		this.messageType = messageType;
		this.type = type;
		this.builtIn = builtIn;
		this.promptDescriptionZh = promptDescriptionZh;
		this.promptDescriptionEn = promptDescriptionEn;
		this.promptPath = promptPath;
	}

	public String getPromptPathForLanguage(String language) {
		if (language == null || language.trim().isEmpty()) {
			language = "en"; // 默认英文
		}
		return language + "/" + this.promptPath;
	}

	public String getPromptDescriptionForLanguage(String language) {
		if ("zh".equals(language)) {
			return this.promptDescriptionZh;
		}
		return this.promptDescriptionEn; // 默认返回英文描述
	}

	public static String[] getSupportedLanguages() {
		return SUPPORTED_LANGUAGES.clone();
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
		return promptDescriptionEn;
	}

	public void setPromptDescription(String promptDescription) {
		this.promptDescriptionEn = promptDescription;
	}

	public String getPromptDescriptionZh() {
		return promptDescriptionZh;
	}

	public void setPromptDescriptionZh(String promptDescriptionZh) {
		this.promptDescriptionZh = promptDescriptionZh;
	}

	public String getPromptDescriptionEn() {
		return promptDescriptionEn;
	}

	public void setPromptDescriptionEn(String promptDescriptionEn) {
		this.promptDescriptionEn = promptDescriptionEn;
	}

}
