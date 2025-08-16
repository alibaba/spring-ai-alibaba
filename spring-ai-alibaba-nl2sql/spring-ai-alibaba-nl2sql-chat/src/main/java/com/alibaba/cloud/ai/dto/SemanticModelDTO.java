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
 * Semantic model configuration entity class
 */
public class SemanticModelDTO {

	private Long id; // Unique identifier

	private String agentId; // Agent ID

	private String originalFieldName; // Original field name

	private String agentFieldName; // Agent field name

	private String fieldSynonyms; // Field name synonyms, comma separated

	private String fieldDescription; // Field description

	private Boolean defaultRecall; // Default recall

	private Boolean enabled; // Whether enabled

	private String fieldType; // Field type

	private String originalDescription; // Original field description

	public SemanticModelDTO() {
	}

	public SemanticModelDTO(String agentId, String originalFieldName, String agentFieldName, String fieldSynonyms,
			String fieldDescription, Boolean defaultRecall, Boolean enabled, String fieldType,
			String originalDescription) {
		this.agentId = agentId;
		this.originalFieldName = originalFieldName;
		this.agentFieldName = agentFieldName;
		this.fieldSynonyms = fieldSynonyms;
		this.fieldDescription = fieldDescription;
		this.defaultRecall = defaultRecall;
		this.enabled = enabled;
		this.fieldType = fieldType;
		this.originalDescription = originalDescription;
	}

	public SemanticModelDTO(String originalFieldName, String agentFieldName, String fieldSynonyms,
			String fieldDescription, String fieldType, String originalDescription, Boolean defaultRecall,
			Boolean enabled) {
		this.originalFieldName = originalFieldName;
		this.agentFieldName = agentFieldName;
		this.fieldSynonyms = fieldSynonyms;
		this.fieldDescription = fieldDescription;
		this.fieldType = fieldType;
		this.originalDescription = originalDescription;
		this.defaultRecall = defaultRecall;
		this.enabled = enabled;
	}

	// Getters and Setters
	public String getAgentId() {
		return agentId;
	}

	public void setAgentId(String agentId) {
		this.agentId = agentId;
	}

	// Getters and Setters
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
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

	@Override
	public String toString() {
		return String.format("智能体字段名: %s, 数据库字段名: %s, 字段同义词: %s, 智能体字段描述: %s, 字段类型: %s, 数据库字段描述: %s",
				getAgentFieldName(), getOriginalFieldName(), getFieldSynonyms(), getFieldDescription(), getFieldType(),
				getOriginalDescription());
	}

}
