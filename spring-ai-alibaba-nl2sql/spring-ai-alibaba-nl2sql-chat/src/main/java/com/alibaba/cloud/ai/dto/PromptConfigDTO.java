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

package com.alibaba.cloud.ai.dto;

/**
 * 提示词配置请求DTO
 *
 * @author Makoto
 */
public class PromptConfigDTO {

	/**
	 * 配置ID（更新时需要）
	 */
	private String id;

	/**
	 * 配置名称
	 */
	private String name;

	/**
	 * 提示词类型
	 */
	private String promptType;

	/**
	 * 用户自定义的系统提示词内容
	 */
	private String systemPrompt;

	/**
	 * 是否启用该配置
	 */
	private Boolean enabled;

	/**
	 * 配置描述
	 */
	private String description;

	/**
	 * 创建者
	 */
	private String creator;

	// Constructors
	public PromptConfigDTO() {
	}

	public PromptConfigDTO(String promptType, String systemPrompt) {
		this.promptType = promptType;
		this.systemPrompt = systemPrompt;
		this.enabled = true;
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

	public String getCreator() {
		return creator;
	}

	public void setCreator(String creator) {
		this.creator = creator;
	}

	@Override
	public String toString() {
		return "PromptConfigDTO{" + "id='" + id + '\'' + ", name='" + name + '\'' + ", promptType='" + promptType + '\''
				+ ", enabled=" + enabled + ", description='" + description + '\'' + ", creator='" + creator + '\''
				+ '}';
	}

}
