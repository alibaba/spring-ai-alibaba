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
 * Semantic Model Configuration Entity Class
 */
@TableName("semantic_model")
public class SemanticModel {

	@TableId(value = "id", type = IdType.AUTO)
	private Long id;

	@TableField("agent_id")
	private Long agentId; // Agent ID

	@TableField("origin_name")
	private String originalFieldName; // Original field name

	@TableField("field_name")
	private String agentFieldName; // Agent field name

	@TableField("synonyms")
	private String fieldSynonyms; // Field name synonyms, comma-separated

	@TableField("description")
	private String fieldDescription; // Field description

	@TableField("type")
	private String fieldType; // Field type

	@TableField("origin_description")
	private String originalDescription; // Original field description

	@TableField("is_recall")
	private Boolean defaultRecall; // Default recall

	@TableField("status")
	private Boolean enabled; // Whether enabled

	@TableField(value = "created_time", fill = FieldFill.INSERT)
	private LocalDateTime createTime;

	@TableField(value = "updated_time", fill = FieldFill.INSERT_UPDATE)
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
