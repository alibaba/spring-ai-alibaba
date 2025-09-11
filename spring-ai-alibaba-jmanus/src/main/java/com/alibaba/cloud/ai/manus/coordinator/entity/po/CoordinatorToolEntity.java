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
package com.alibaba.cloud.ai.manus.coordinator.entity.po;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Coordinator Tool Entity Class
 */
@Entity
@Table(name = "coordinator_tools", uniqueConstraints = @UniqueConstraint(columnNames = { "toolName", "serviceGroup" }))
public class CoordinatorToolEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 50)
	private String toolName;

	@Column(nullable = false, length = 200)
	private String toolDescription;

	@Column(columnDefinition = "VARCHAR(2048)")
	private String inputSchema;

	@Column(nullable = false, length = 50)
	private String planTemplateId;

	@Column(length = 100)
	private String httpEndpoint;

	@Column(length = 100)
	private String mcpEndpoint;

	@Column(length = 100)
	private String serviceGroup;

	@Column(nullable = false)
	private Boolean enableInternalToolcall = true;

	@Column(nullable = false)
	private Boolean enableHttpService = false;

	@Column(nullable = false)
	private Boolean enableMcpService = false;

	@Column(nullable = false)
	private LocalDateTime createTime;

	@Column(nullable = false)
	private LocalDateTime updateTime;

	public CoordinatorToolEntity() {
		this.createTime = LocalDateTime.now();
		this.updateTime = LocalDateTime.now();
		this.enableInternalToolcall = true;
		this.enableHttpService = false;
		this.enableMcpService = false;
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

	public String getPlanTemplateId() {
		return planTemplateId;
	}

	public void setPlanTemplateId(String planTemplateId) {
		this.planTemplateId = planTemplateId;
	}

	public String getHttpEndpoint() {
		return httpEndpoint;
	}

	public void setHttpEndpoint(String httpEndpoint) {
		this.httpEndpoint = httpEndpoint;
	}

	public String getMcpEndpoint() {
		return mcpEndpoint;
	}

	public void setMcpEndpoint(String mcpEndpoint) {
		this.mcpEndpoint = mcpEndpoint;
	}

	public Boolean getEnableInternalToolcall() {
		return enableInternalToolcall;
	}

	public void setEnableInternalToolcall(Boolean enableInternalToolcall) {
		this.enableInternalToolcall = enableInternalToolcall;
	}

	public Boolean getEnableHttpService() {
		return enableHttpService;
	}

	public void setEnableHttpService(Boolean enableHttpService) {
		this.enableHttpService = enableHttpService;
	}

	public Boolean getEnableMcpService() {
		return enableMcpService;
	}

	public void setEnableMcpService(Boolean enableMcpService) {
		this.enableMcpService = enableMcpService;
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

	public String getServiceGroup() {
		return serviceGroup;
	}

	public void setServiceGroup(String serviceGroup) {
		this.serviceGroup = serviceGroup;
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
				+ toolDescription + '\'' + ", planTemplateId='" + planTemplateId + '\'' + ", httpEndpoint='"
				+ httpEndpoint + '\'' + ", mcpEndpoint='" + mcpEndpoint + '\'' + ", enableInternalToolcall="
				+ enableInternalToolcall + ", enableHttpService=" + enableHttpService + ", enableMcpService="
				+ enableMcpService + ", createTime=" + createTime + ", updateTime=" + updateTime + '}';
	}

}
