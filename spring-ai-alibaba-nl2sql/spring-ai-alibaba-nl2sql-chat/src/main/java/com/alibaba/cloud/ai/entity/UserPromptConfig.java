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

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;

/**
 * User-defined prompt configuration entity class
 *
 * @author Makoto
 */
@TableName("user_prompt_config")
public class UserPromptConfig {

	/**
	 * Configuration ID
	 */
	@TableId(value = "id", type = IdType.ASSIGN_UUID)
	private String id;

	/**
	 * Configuration name
	 */
	@TableField("name")
	private String name;

	/**
	 * Prompt type (e.g., report-generator, planner, etc.)
	 */
	@TableField("prompt_type")
	private String promptType;

	/**
	 * User-defined system prompt content
	 */
	@TableField("system_prompt")
	private String systemPrompt;

	/**
	 * Whether to enable this configuration
	 */
	@TableField("enabled")
	private Boolean enabled;

	/**
	 * Configuration description
	 */
	@TableField("description")
	private String description;

	/**
	 * Creation time
	 */
	@TableField(value = "create_time", fill = FieldFill.INSERT)
	private LocalDateTime createTime;

	/**
	 * Update time
	 */
	@TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
	private LocalDateTime updateTime;

	/**
	 * Creator
	 */
	@TableField("creator")
	private String creator;

	// Constructors
	public UserPromptConfig() {
		this.enabled = true;
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
	}

	public Boolean getEnabled() {
		return enabled;
	}

	public void setEnabled(Boolean enabled) {
		this.enabled = enabled;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
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

	public String getOptimizationPrompt() {
		return this.systemPrompt;
	}

	public void setOptimizationPrompt(String optimizationPrompt) {
		this.systemPrompt = optimizationPrompt;
	}

	@Override
	public String toString() {
		return "UserPromptConfig{" + "id='" + id + '\'' + ", name='" + name + '\'' + ", promptType='" + promptType
				+ '\'' + ", enabled=" + enabled + ", description='" + description + '\'' + ", createTime=" + createTime
				+ ", updateTime=" + updateTime + ", creator='" + creator + '\'' + '}';
	}

}
