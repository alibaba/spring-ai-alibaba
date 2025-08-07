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
 * Semantic Model Configuration Entity Class
 */
public class SemanticModel {

	private Long id;

	private Long agentId; // Agent ID

	private String originalFieldName; // Original field name

	private String agentFieldName; // Agent field name

	private String fieldSynonyms; // Field name synonyms, comma separated

	private String fieldDescription; // Field description

	private String fieldType; // Field type

	private String originalDescription; // Original field description

	private Boolean defaultRecall; // Default recall

	private Boolean enabled; // Whether enabled

	private LocalDateTime createTime;

	private LocalDateTime updateTime;

	public SemanticModel() {
	}

	public SemanticModel(Long agentId, String originalFieldName, String agentFieldName, String fieldSynonyms,
			String fieldDescription, String fieldType, String originalDescription, Boolean defaultRecall,
			Boolean enabled) {
		this.agentId = agentId;
		this.originalFieldName = originalFieldName;
		this.agentFieldName = agentFieldName;
		this.fieldSynonyms = fieldSynonyms;
		this.fieldDescription = fieldDescription;
		this.fieldType = fieldType;
		this.originalDescription = originalDescription;
		this.defaultRecall = defaultRecall;
		this.enabled = enabled;
		this.createTime = LocalDateTime.now();
		this.updateTime = LocalDateTime.now();
	}

	public SemanticModel(Long id, Long agentId, String originalFieldName, String agentFieldName, String fieldSynonyms,
			String fieldDescription, String fieldType, String originalDescription, Boolean defaultRecall,
			Boolean enabled) {
		this.id = id;
		this.agentId = agentId;
		this.originalFieldName = originalFieldName;
		this.agentFieldName = agentFieldName;
		this.fieldSynonyms = fieldSynonyms;
		this.fieldDescription = fieldDescription;
		this.defaultRecall = defaultRecall;
		this.enabled = enabled;
		this.fieldType = fieldType;
		this.originalDescription = originalDescription;
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

	public Long getAgentId() {
		return agentId;
	}

	public void setAgentId(Long agentId) {
		this.agentId = agentId;
	}

	public String getOriginalFieldName() {
		return originalFieldName;
	}

	public void setOriginalFieldName(String originalFieldName) {
		this.originalFieldName = originalFieldName;
	}

	public String getAgentFieldName() {
		return agentFieldName;
	}

	public void setAgentFieldName(String agentFieldName) {
		this.agentFieldName = agentFieldName;
	}

	public String getFieldSynonyms() {
		return fieldSynonyms;
	}

	public void setFieldSynonyms(String fieldSynonyms) {
		this.fieldSynonyms = fieldSynonyms;
	}

	public String getFieldDescription() {
		return fieldDescription;
	}

	public void setFieldDescription(String fieldDescription) {
		this.fieldDescription = fieldDescription;
	}

	public Boolean getDefaultRecall() {
		return defaultRecall;
	}

	public void setDefaultRecall(Boolean defaultRecall) {
		this.defaultRecall = defaultRecall;
	}

	public Boolean getEnabled() {
		return enabled;
	}

	public void setEnabled(Boolean enabled) {
		this.enabled = enabled;
	}

	public String getFieldType() {
		return fieldType;
	}

	public void setFieldType(String fieldType) {
		this.fieldType = fieldType;
	}

	public String getOriginalDescription() {
		return originalDescription;
	}

	public void setOriginalDescription(String originalDescription) {
		this.originalDescription = originalDescription;
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

}
