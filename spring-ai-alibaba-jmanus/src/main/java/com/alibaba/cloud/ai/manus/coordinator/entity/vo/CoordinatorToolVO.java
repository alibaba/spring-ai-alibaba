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
package com.alibaba.cloud.ai.manus.coordinator.entity.vo;

import com.alibaba.cloud.ai.manus.coordinator.entity.po.CoordinatorToolEntity;
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

	private String planTemplateId;

	private String httpEndpoint;

	private String mcpEndpoint;

	private String publishStatus;

	private String serviceGroup = null;

	private Boolean enableInternalToolcall = false;

	private Boolean enableHttpService = false;

	private Boolean enableMcpService = false;

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

	public String getPublishStatus() {
		return publishStatus;
	}

	public void setPublishStatus(String publishStatus) {
		this.publishStatus = publishStatus;
	}

	public String getServiceGroup() {
		return serviceGroup;
	}

	public void setServiceGroup(String serviceGroup) {
		this.serviceGroup = serviceGroup;
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
		vo.setPlanTemplateId(entity.getPlanTemplateId());
		vo.setHttpEndpoint(entity.getHttpEndpoint());
		vo.setMcpEndpoint(entity.getMcpEndpoint());
		vo.setServiceGroup(entity.getServiceGroup());
		vo.setEnableInternalToolcall(entity.getEnableInternalToolcall());
		vo.setEnableHttpService(entity.getEnableHttpService());
		vo.setEnableMcpService(entity.getEnableMcpService());
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
		entity.setPlanTemplateId(this.planTemplateId);
		entity.setHttpEndpoint(this.httpEndpoint);
		entity.setMcpEndpoint(this.mcpEndpoint);

		entity.setServiceGroup(this.serviceGroup);
		entity.setEnableInternalToolcall(this.enableInternalToolcall);
		entity.setEnableHttpService(this.enableHttpService);
		entity.setEnableMcpService(this.enableMcpService);
		return entity;
	}

	@Override
	public String toString() {
		return "CoordinatorToolVO{" + "id=" + id + ", toolName='" + toolName + '\'' + ", toolDescription='"
				+ toolDescription + '\'' + ", planTemplateId='" + planTemplateId + '\'' + ", httpEndpoint='"
				+ httpEndpoint + '\'' + ", mcpEndpoint='" + mcpEndpoint + '\'' + ", publishStatus='" + publishStatus
				+ "', serviceGroup='" + serviceGroup + "', enableInternalToolcall=" + enableInternalToolcall
				+ ", enableHttpService=" + enableHttpService + ", enableMcpService=" + enableMcpService + '}';
	}

}
