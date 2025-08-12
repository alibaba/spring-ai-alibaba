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
 * 用户提示词优化配置实体类
 *
 * @author Makoto
 */
public class UserPromptConfig {

	/**
	 * 配置ID
	 */
	private String id;

	/**
	 * 配置名称
	 */
	private String name;

	/**
	 * 提示词类型（如：report-generator, planner等）
	 */
	private String promptType;

	/**
	 * 用户添加的优化提示词内容（附加到原始模板）
	 */
	private String optimizationPrompt;

	/**
	 * 是否启用该配置
	 */
	private Boolean enabled;

	/**
	 * 配置描述
	 */
	private String description;

	/**
	 * 创建时间
	 */
	private LocalDateTime createTime;

	/**
	 * 更新时间
	 */
	private LocalDateTime updateTime;

	/**
	 * 创建者
	 */
	private String creator;

	// Constructors
	public UserPromptConfig() {
		this.enabled = true;
		this.createTime = LocalDateTime.now();
		this.updateTime = LocalDateTime.now();
	}

	public UserPromptConfig(String promptType, String optimizationPrompt) {
		this();
		this.promptType = promptType;
		this.optimizationPrompt = optimizationPrompt;
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

	public String getOptimizationPrompt() {
		return optimizationPrompt;
	}

	public void setOptimizationPrompt(String optimizationPrompt) {
		this.optimizationPrompt = optimizationPrompt;
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
