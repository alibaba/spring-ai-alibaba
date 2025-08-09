/*
 * Copyright 2024-2025 the original author or authors.
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

package com.alibaba.cloud.ai.entity;

import java.time.LocalDateTime;

/**
 * User-defined prompt configuration entity class
 *
 * @author Makoto
 */
public class UserPromptConfig {

	/**
	 * Configuration ID
	 */
	private String id;

	/**
	 * Configuration name
	 */
	private String name;

	/**
	 * Prompt type (e.g., report-generator, planner, etc.)
	 */
	private String promptType;

	/**
	 * User-defined system prompt content
	 */
	private String systemPrompt;

	/**
	 * Whether to enable this configuration
	 */
	private Boolean enabled;

	/**
	 * Configuration description
	 */
	private String description;

	/**
	 * Creation time
	 */
	private LocalDateTime createTime;

	/**
	 * Update time
	 */
	private LocalDateTime updateTime;

	/**
	 * Creator
	 */
	private String creator;

	// Constructors
	public UserPromptConfig() {
		this.enabled = true;
		this.createTime = LocalDateTime.now();
		this.updateTime = LocalDateTime.now();
	}

	public UserPromptConfig(String promptType, String systemPrompt) {
		this();
		this.promptType = promptType;
		this.systemPrompt = systemPrompt;
	}

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

	public String getPromptType() {
		return promptType;
	}

	public void setPromptType(String promptType) {
		this.promptType = promptType;
	}

	public String getSystemPrompt() {
		return systemPrompt;
	}

	public void setSystemPrompt(String systemPrompt) {
		this.systemPrompt = systemPrompt;
		this.updateTime = LocalDateTime.now();
	}

	public Boolean getEnabled() {
		return enabled;
	}

	public void setEnabled(Boolean enabled) {
		this.enabled = enabled;
		this.updateTime = LocalDateTime.now();
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
		this.updateTime = LocalDateTime.now();
	}

	public LocalDateTime getCreateTime() {
		return createTime;
	}

	public void setCreateTime(LocalDateTime createTime) {
		this.createTime = createTime;
	}

	public LocalDateTime getUpdateTime() {
		return updateTime;
	}

	public void setUpdateTime(LocalDateTime updateTime) {
		this.updateTime = updateTime;
	}

	public String getCreator() {
		return creator;
	}

	public void setCreator(String creator) {
		this.creator = creator;
	}

	@Override
	public String toString() {
		return "UserPromptConfig{" + "id='" + id + '\'' + ", name='" + name + '\'' + ", promptType='" + promptType
				+ '\'' + ", enabled=" + enabled + ", description='" + description + '\'' + ", createTime=" + createTime
				+ ", updateTime=" + updateTime + ", creator='" + creator + '\'' + '}';
	}

}
