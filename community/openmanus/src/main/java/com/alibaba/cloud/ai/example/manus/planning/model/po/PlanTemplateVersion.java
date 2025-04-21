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
package com.alibaba.cloud.ai.example.manus.planning.model.po;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 计划模板版本实体类，用于存储计划模板的各个版本
 */
@Entity
@Table(name = "plan_template_version")
public class PlanTemplateVersion {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "plan_template_id", nullable = false, length = 50)
	private String planTemplateId;

	@Column(name = "version_index", nullable = false)
	private Integer versionIndex;

	@Column(name = "plan_json", columnDefinition = "TEXT", nullable = false)
	private String planJson;

	@Column(name = "create_time", nullable = false)
	private LocalDateTime createTime;

	// 构造函数
	public PlanTemplateVersion() {
	}

	public PlanTemplateVersion(String planTemplateId, Integer versionIndex, String planJson) {
		this.planTemplateId = planTemplateId;
		this.versionIndex = versionIndex;
		this.planJson = planJson;
		this.createTime = LocalDateTime.now();
	}

	// Getters and Setters
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

	public Integer getVersionIndex() {
		return versionIndex;
	}

	public void setVersionIndex(Integer versionIndex) {
		this.versionIndex = versionIndex;
	}

	public String getPlanJson() {
		return planJson;
	}

	public void setPlanJson(String planJson) {
		this.planJson = planJson;
	}

	public LocalDateTime getCreateTime() {
		return createTime;
	}

	public void setCreateTime(LocalDateTime createTime) {
		this.createTime = createTime;
	}

}
