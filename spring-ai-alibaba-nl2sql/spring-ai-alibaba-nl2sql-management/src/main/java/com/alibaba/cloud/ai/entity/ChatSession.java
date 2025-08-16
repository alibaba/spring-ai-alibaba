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
 * Chat Session Entity Class
 */
@TableName("chat_session")
public class ChatSession {

	@TableId(value = "id", type = IdType.ASSIGN_UUID)
	private String id; // UUID

	@TableField("agent_id")
	private Integer agentId;

	@TableField("title")
	private String title;

	@TableField("status")
	private String status; // active, archived, deleted

	@TableField("is_pinned")
	private Boolean isPinned; // Whether pinned

	@TableField("user_id")
	private Long userId;

	@TableField(value = "create_time", fill = FieldFill.INSERT)
	private LocalDateTime createTime;

	@TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
	private LocalDateTime updateTime;

	public ChatSession() {
	}

	public ChatSession(String id, Integer agentId, String title, String status, Long userId) {
		this.id = id;
		this.agentId = agentId;
		this.title = title;
		this.status = status;
		this.isPinned = false;
		this.userId = userId;
	}

	// Getters and Setters
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public Integer getAgentId() {
		return agentId;
	}

	public void setAgentId(Integer agentId) {
		this.agentId = agentId;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public Boolean getIsPinned() {
		return isPinned;
	}

	public void setIsPinned(Boolean isPinned) {
		this.isPinned = isPinned;
	}

	public Long getUserId() {
		return userId;
	}

	public void setUserId(Long userId) {
		this.userId = userId;
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
		return "ChatSession{" + "id='" + id + '\'' + ", agentId=" + agentId + ", title='" + title + '\'' + ", status='"
				+ status + '\'' + ", isPinned=" + isPinned + ", userId=" + userId + ", createTime=" + createTime
				+ ", updateTime=" + updateTime + '}';
	}

}
