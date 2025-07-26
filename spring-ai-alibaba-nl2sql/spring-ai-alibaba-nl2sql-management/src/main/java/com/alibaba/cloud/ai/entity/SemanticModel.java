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
 * 语义模型配置实体类
 */
public class SemanticModel {

	private Long id;

	private String datasetId; // 数据集ID

	private String originalFieldName; // 原始字段名

	private String agentFieldName; // 智能体字段名称

	private String fieldSynonyms; // 字段名称同义词，逗号分隔

	private String fieldDescription; // 字段描述

	private Boolean defaultRecall; // 默认召回

	private Boolean enabled; // 是否启用

	private String fieldType; // 字段类型

	private String originalDescription; // 原始字段描述

	private LocalDateTime createTime;

	private LocalDateTime updateTime;

	public SemanticModel() {
	}

	public SemanticModel(String datasetId, String originalFieldName, String agentFieldName, String fieldSynonyms,
			String fieldDescription, Boolean defaultRecall, Boolean enabled, String fieldType,
			String originalDescription) {
		this.datasetId = datasetId;
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

	public SemanticModel(Long id, String datasetId, String originalFieldName, String agentFieldName,
			String fieldSynonyms, String fieldDescription, Boolean defaultRecall, Boolean enabled, String fieldType,
			String originalDescription, LocalDateTime createTime, LocalDateTime updateTime) {
		this.id = id;
		this.datasetId = datasetId;
		this.originalFieldName = originalFieldName;
		this.agentFieldName = agentFieldName;
		this.fieldSynonyms = fieldSynonyms;
		this.fieldDescription = fieldDescription;
		this.defaultRecall = defaultRecall;
		this.enabled = enabled;
		this.fieldType = fieldType;
		this.originalDescription = originalDescription;
		this.createTime = createTime;
		this.updateTime = updateTime;
	}

	// Getters and Setters
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getDatasetId() {
		return datasetId;
	}

	public void setDatasetId(String datasetId) {
		this.datasetId = datasetId;
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
