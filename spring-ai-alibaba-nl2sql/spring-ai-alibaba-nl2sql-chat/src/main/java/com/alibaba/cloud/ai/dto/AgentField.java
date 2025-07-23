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

import java.sql.Timestamp;

public class AgentField {

	private Integer id;

	private String fieldName; // 智能体字段名称

	private String synonyms; // 字段名称同义词

	private String originName; // 原始字段名

	private String fieldDescription; // 字段描述

	private String originDescription; // 字段描述

	private String type; // 字段类型 (integer, varchar...)

	private Integer isRecall; // 0 停用 1 启用

	private Integer status; // 0 停用 1 启用

	private String dataSetId; // 数据集ID

	private Timestamp createdTime; // 创建时间

	private Timestamp updatedTime; // 更新时间

	public AgentField() {
	}

	public AgentField(Integer id, String fieldName, String synonyms, String originName, String fieldDescription,
			String originDescription, String type, Integer isRecall, Integer status, String dataSetId,
			Timestamp createdTime, Timestamp updatedTime) {
		this.id = id;
		this.fieldName = fieldName;
		this.synonyms = synonyms;
		this.dataSetId = dataSetId;
		this.originName = originName;
		this.fieldDescription = fieldDescription;
		this.originDescription = originDescription;
		this.type = type;
		this.createdTime = createdTime;
		this.updatedTime = updatedTime;
		this.isRecall = isRecall;
		this.status = status;
	}

	// Getters and Setters

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getFieldName() {
		return fieldName;
	}

	public void setFieldName(String fieldName) {
		this.fieldName = fieldName;
	}

	public String getSynonyms() {
		return synonyms;
	}

	public void setSynonyms(String synonyms) {
		this.synonyms = synonyms;
	}

	public String getDataSetId() {
		return dataSetId;
	}

	public void setDataSetId(String dataSetId) {
		this.dataSetId = dataSetId;
	}

	public String getOriginName() {
		return originName;
	}

	public void setOriginName(String originName) {
		this.originName = originName;
	}

	public String getFieldDescription() {
		return fieldDescription;
	}

	public void setFieldDescription(String fieldDescription) {
		this.fieldDescription = fieldDescription;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public Timestamp getCreatedTime() {
		return createdTime;
	}

	public void setCreatedTime(Timestamp createdTime) {
		this.createdTime = createdTime;
	}

	public Timestamp getUpdatedTime() {
		return updatedTime;
	}

	public void setUpdatedTime(Timestamp updatedTime) {
		this.updatedTime = updatedTime;
	}

	public Integer getIsRecall() {
		return isRecall;
	}

	public void setIsRecall(Integer isRecall) {
		this.isRecall = isRecall;
	}

	public Integer getStatus() {
		return status;
	}

	public void setStatus(Integer status) {
		this.status = status;
	}

	public String getOriginDescription() {
		return originDescription;
	}

	public void setOriginDescription(String originDescription) {
		this.originDescription = originDescription;
	}

}
