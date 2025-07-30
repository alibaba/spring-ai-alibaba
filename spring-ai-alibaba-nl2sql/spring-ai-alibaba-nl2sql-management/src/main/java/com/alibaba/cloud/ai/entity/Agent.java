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
 * 智能体实体类
 */
public class Agent {

	private Long id;

	private String name; // 智能体名称

	private String description; // 智能体描述

	private String avatar; // 头像URL

	private String status; // 状态：draft-待发布，published-已发布，offline-已下线

	private String prompt; // 自定义Prompt配置

	private String category; // 分类

	private Long adminId; // 管理员ID

	private String tags; // 标签，逗号分隔

	private LocalDateTime createTime;

	private LocalDateTime updateTime;

	public Agent() {
	}

	public Agent(String name, String description, String avatar, String status, String prompt, String category,
			Long adminId, String tags) {
		this.name = name;
		this.description = description;
		this.avatar = avatar;
		this.status = status;
		this.prompt = prompt;
		this.category = category;
		this.adminId = adminId;
		this.tags = tags;
		this.createTime = LocalDateTime.now();
		this.updateTime = LocalDateTime.now();
	}

	// Getters and Setters
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
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

	public String getAvatar() {
		return avatar;
	}

	public void setAvatar(String avatar) {
		this.avatar = avatar;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getPrompt() {
		return prompt;
	}

	public void setPrompt(String prompt) {
		this.prompt = prompt;
	}

	public String getCategory() {
		return category;
	}

	public void setCategory(String category) {
		this.category = category;
	}

	public Long getAdminId() {
		return adminId;
	}

	public void setAdminId(Long adminId) {
		this.adminId = adminId;
	}

	public String getTags() {
		return tags;
	}

	public void setTags(String tags) {
		this.tags = tags;
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

	@Override
	public String toString() {
		return "Agent{" + "id=" + id + ", name='" + name + '\'' + ", description='" + description + '\'' + ", avatar='"
				+ avatar + '\'' + ", status='" + status + '\'' + ", prompt='" + prompt + '\'' + ", category='"
				+ category + '\'' + ", adminId=" + adminId + ", tags='" + tags + '\'' + ", createTime=" + createTime
				+ ", updateTime=" + updateTime + '}';
	}

}
