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
 * Business Knowledge Management Entity Class
 */
@TableName("business_knowledge")
public class BusinessKnowledge {

	@TableId(value = "id", type = IdType.AUTO)
	private Long id;

	@TableField("business_term")
	private String businessTerm; // Business term

	@TableField("description")
	private String description; // Description

	@TableField("synonyms")
	private String synonyms; // Synonyms, comma-separated

	@TableField("is_recall")
	private Boolean defaultRecall; // Default recall

	@TableField("data_set_id")
	private String datasetId; // Associated dataset ID

	@TableField("agent_id")
	private String agentId; // Associated agent ID

	@TableField(value = "created_time", fill = FieldFill.INSERT)
	private LocalDateTime createTime;

	@TableField(value = "updated_time", fill = FieldFill.INSERT_UPDATE)
	private LocalDateTime updateTime;

	public BusinessKnowledge() {
	}

	public BusinessKnowledge(String businessTerm, String description, String synonyms, Boolean defaultRecall,
			String datasetId) {
		this.businessTerm = businessTerm;
		this.description = description;
		this.synonyms = synonyms;
		this.defaultRecall = defaultRecall;
		this.datasetId = datasetId;
		this.agentId = null; // Defaults to null for backward compatibility
	}

	public BusinessKnowledge(String businessTerm, String description, String synonyms, Boolean defaultRecall,
			String datasetId, String agentId) {
		this.businessTerm = businessTerm;
		this.description = description;
		this.synonyms = synonyms;
		this.defaultRecall = defaultRecall;
		this.datasetId = datasetId;
		this.agentId = agentId;
	}

	public BusinessKnowledge(Long id, String businessTerm, String description, String synonyms, Boolean defaultRecall,
			String datasetId, String agentId, LocalDateTime createTime, LocalDateTime updateTime) {
		this.id = id;
		this.businessTerm = businessTerm;
		this.description = description;
		this.synonyms = synonyms;
		this.defaultRecall = defaultRecall;
		this.datasetId = datasetId;
		this.agentId = agentId;
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

	public String getBusinessTerm() {
		return businessTerm;
	}

	public void setBusinessTerm(String businessTerm) {
		this.businessTerm = businessTerm;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getSynonyms() {
		return synonyms;
	}

	public void setSynonyms(String synonyms) {
		this.synonyms = synonyms;
	}

	public Boolean getDefaultRecall() {
		return defaultRecall;
	}

	public void setDefaultRecall(Boolean defaultRecall) {
		this.defaultRecall = defaultRecall;
	}

	public String getDatasetId() {
		return datasetId;
	}

	public void setDatasetId(String datasetId) {
		this.datasetId = datasetId;
	}

	public String getAgentId() {
		return agentId;
	}

	public void setAgentId(String agentId) {
		this.agentId = agentId;
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
