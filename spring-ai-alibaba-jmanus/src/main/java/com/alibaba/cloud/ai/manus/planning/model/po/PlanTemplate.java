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
package com.alibaba.cloud.ai.manus.planning.model.po;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * The entity class for the plan template, used to store the basic information of the plan
 * template
 */
@Entity
@Table(name = "plan_template")
public class PlanTemplate {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private Long id;

	@Column(name = "plan_template_id", length = 50, unique = true, nullable = false)
	private String planTemplateId;

	@Column(name = "title", length = 255)
	private String title;

	@Column(name = "user_request", length = 4000)
	private String userRequest;

	@Column(name = "create_time", nullable = false)
	private LocalDateTime createTime;

	@Column(name = "update_time", nullable = false)
	private LocalDateTime updateTime;

	@Column(name = "is_internal_toolcall", nullable = false)
	private boolean isInternalToolcall = false;

	// Constructor
	public PlanTemplate() {
	}

	public PlanTemplate(String planTemplateId, String title, String userRequest, boolean isInternalToolcall) {
		this.planTemplateId = planTemplateId;
		this.title = title;
		this.userRequest = userRequest;
		this.createTime = LocalDateTime.now();
		this.updateTime = LocalDateTime.now();
		this.isInternalToolcall = isInternalToolcall;
	}

	// Getters and setters
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getPlanTemplateId() {
		return planTemplateId;
	}

	public void setPlanTemplateId(String planTemplateId) {
		this.planTemplateId = planTemplateId;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getUserRequest() {
		return userRequest;
	}

	public void setUserRequest(String userRequest) {
		this.userRequest = userRequest;
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

	public boolean isInternalToolcall() {
		return isInternalToolcall;
	}

	public void setInternalToolcall(boolean isInternalToolcall) {
		this.isInternalToolcall = isInternalToolcall;
	}

}
