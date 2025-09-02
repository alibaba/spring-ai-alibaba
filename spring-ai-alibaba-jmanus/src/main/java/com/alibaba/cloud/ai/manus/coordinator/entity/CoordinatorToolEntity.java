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
package com.alibaba.cloud.ai.manus.coordinator.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Coordinator Tool Entity Class
 */
@Entity
@Table(name = "coordinator_tools")
public class CoordinatorToolEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 50)
	private String toolName;

	@Column(nullable = false, length = 200)
	private String toolDescription;

	@Column(nullable = false, columnDefinition = "VARCHAR(2048)")
	private String inputSchema;

	@Column(nullable = false, columnDefinition = "VARCHAR(2048)")
	private String mcpSchema;

	@Column(nullable = false, length = 50)
	private String planTemplateId;

	@Column(nullable = false, length = 20)
	private String endpoint;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private PublishStatus publishStatus;

	@Column(nullable = false)
	private LocalDateTime createTime;

	@Column(nullable = false)
	private LocalDateTime updateTime;

	/**
	 * Publish Status Enum
	 */
	public enum PublishStatus {

		PUBLISHED("published"), UNPUBLISHED("unpublished");

		private final String description;

		PublishStatus(String description) {
			this.description = description;
		}

		public String getDescription() {
			return description;
		}

	}

	public CoordinatorToolEntity() {
		this.createTime = LocalDateTime.now();
		this.updateTime = LocalDateTime.now();
		this.publishStatus = PublishStatus.UNPUBLISHED;
	}

	// Getters and Setters
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getToolName() {
		return toolName;
	}

	public void setToolName(String toolName) {
		this.toolName = toolName;
	}

	public String getToolDescription() {
		return toolDescription;
	}

	public void setToolDescription(String toolDescription) {
		this.toolDescription = toolDescription;
	}

	public String getInputSchema() {
		return inputSchema;
	}

	public void setInputSchema(String inputSchema) {
		this.inputSchema = inputSchema;
	}

	public String getMcpSchema() {
		return mcpSchema;
	}

	public void setMcpSchema(String mcpSchema) {
		this.mcpSchema = mcpSchema;
	}

	public String getPlanTemplateId() {
		return planTemplateId;
	}

	public void setPlanTemplateId(String planTemplateId) {
		this.planTemplateId = planTemplateId;
	}

	public String getEndpoint() {
		return endpoint;
	}

	public void setEndpoint(String endpoint) {
		this.endpoint = endpoint;
	}

	public PublishStatus getPublishStatus() {
		return publishStatus;
	}

	public void setPublishStatus(PublishStatus publishStatus) {
		this.publishStatus = publishStatus;
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

	/**
	 * Automatically set update time when updating
	 */
	@PreUpdate
	public void preUpdate() {
		this.updateTime = LocalDateTime.now();
	}

	@Override
	public String toString() {
		return "CoordinatorToolEntity{" + "id=" + id + ", toolName='" + toolName + '\'' + ", toolDescription='"
				+ toolDescription + '\'' + ", planTemplateId='" + planTemplateId + '\'' + ", endpoint='" + endpoint
				+ '\'' + ", publishStatus=" + publishStatus + ", createTime=" + createTime + ", updateTime="
				+ updateTime + '}';
	}

}
