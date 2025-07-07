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

	LLL_FINALIZE_SYSTEM("LLL_FINALIZE_SYSTEM", MessageType.SYSTEM, PromptType.LLM, true, "", "llm/finalize-system.txt"),
	LLL_MANUS_SYSTEM("LLL_MANUS_SYSTEM", MessageType.SYSTEM, PromptType.LLM, true, "", "llm/manus-system.txt"),
	LLL_PLANNING_SYSTEM("LLL_PLANNING_SYSTEM", MessageType.SYSTEM, PromptType.LLM, true, "", "llm/planning-system.txt"),

	AGENT_CURRENT_STEP_ENV("AGENT_CURRENT_STEP_ENV", MessageType.USER, PromptType.AGENT, true, "",
			"agent/current-step-env.txt"),
	AGENT_STEP_EXECUTION("AGENT_STEP_EXECUTION", MessageType.USER, PromptType.AGENT, true, "",
			"agent/step-execution.txt"),
	PLANNING_PLAN_CREATION("PLANNING_PLAN_CREATION", MessageType.SYSTEM, PromptType.PLANNING, true, "",
			"planning/plan-creation.txt"),
	PLANNING_PLAN_FINALIZER("PLANNING_PLAN_FINALIZER", MessageType.SYSTEM, PromptType.PLANNING, true, "",
			"planning/plan-finalizer.txt"),
	PLANNING_USER_REQUEST("PLANNING_USER_REQUEST", MessageType.USER, PromptType.PLANNING, true, "",
			"planning/user-request.txt"),;

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
