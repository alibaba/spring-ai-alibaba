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
package com.alibaba.cloud.ai.manus.coordinator.vo;

import com.alibaba.cloud.ai.manus.coordinator.entity.CoordinatorToolEntity;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Coordinator Tool VO Class
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CoordinatorToolVO {

	private Long id;

	private String toolName;

	private String toolDescription;

	private String inputSchema;

	private String mcpSchema;

	private String planTemplateId;

	private String endpoint;

	private String publishStatus;

	public CoordinatorToolVO() {
	}

	public CoordinatorToolVO(Long id) {
		this.id = id;
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

	public String getPublishStatus() {
		return publishStatus;
	}

	public void setPublishStatus(String publishStatus) {
		this.publishStatus = publishStatus;
	}

	/**
	 * Convert from Entity to VO
	 */
	public static CoordinatorToolVO fromEntity(CoordinatorToolEntity entity) {
		if (entity == null) {
			return null;
		}
		CoordinatorToolVO vo = new CoordinatorToolVO();
		vo.setId(entity.getId());
		vo.setToolName(entity.getToolName());
		vo.setToolDescription(entity.getToolDescription());
		vo.setInputSchema(entity.getInputSchema());
		vo.setMcpSchema(entity.getMcpSchema());
		vo.setPlanTemplateId(entity.getPlanTemplateId());
		vo.setEndpoint(entity.getEndpoint());
		vo.setPublishStatus(entity.getPublishStatus() != null ? entity.getPublishStatus().name() : null);
		// Explicitly do not set createTime and updateTime fields
		return vo;
	}

	/**
	 * Convert to Entity
	 */
	public CoordinatorToolEntity toEntity() {
		CoordinatorToolEntity entity = new CoordinatorToolEntity();
		entity.setId(this.id);
		entity.setToolName(this.toolName);
		entity.setToolDescription(this.toolDescription);
		entity.setInputSchema(this.inputSchema);
		entity.setMcpSchema(this.mcpSchema);
		entity.setPlanTemplateId(this.planTemplateId);
		entity.setEndpoint(this.endpoint);
		if (this.publishStatus != null) {
			entity.setPublishStatus(CoordinatorToolEntity.PublishStatus.valueOf(this.publishStatus));
		}
		return entity;
	}

	@Override
	public String toString() {
		return "CoordinatorToolVO{" + "id=" + id + ", toolName='" + toolName + '\'' + ", toolDescription='"
				+ toolDescription + '\'' + ", planTemplateId='" + planTemplateId + '\'' + ", endpoint='" + endpoint
				+ '\'' + ", publishStatus='" + publishStatus + '}';
	}

}