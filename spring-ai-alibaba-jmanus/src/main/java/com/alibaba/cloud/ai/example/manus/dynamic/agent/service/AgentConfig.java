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
package com.alibaba.cloud.ai.example.manus.dynamic.agent.service;

import java.util.List;

public class AgentConfig {

	private String id;

	private String name;

	private String description;

	private String systemPrompt;

	private String nextStepPrompt;

	private List<String> availableTools;

	private String className;

	// Getters and Setters
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getSystemPrompt() {
		return systemPrompt;
	}

	public void setSystemPrompt(String systemPrompt) {
		this.systemPrompt = systemPrompt;
	}

	public String getNextStepPrompt() {
		return nextStepPrompt;
	}

	public void setNextStepPrompt(String nextStepPrompt) {
		this.nextStepPrompt = nextStepPrompt;
	}

	public List<String> getAvailableTools() {
		return availableTools;
	}

	public void setAvailableTools(List<String> availableTools) {
		this.availableTools = availableTools;
	}

	public String getClassName() {
		return className;
	}

	public void setClassName(String className) {
		this.className = className;
	}

}
