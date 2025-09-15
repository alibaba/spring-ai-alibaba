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

/**
 * Business Knowledge Management Entity Class
 */
public class BusinessKnowledgeDTO {

	private String businessTerm; // Business term

	private String description; // Description

	private String synonyms; // Synonyms, comma separated

	private Boolean defaultRecall; // Default recall

	private String datasetId; // Associated dataset ID

	private String agentId; // Associated agent ID

	public BusinessKnowledgeDTO() {
	}

	public BusinessKnowledgeDTO(String businessTerm, String description, String synonyms, Boolean defaultRecall,
			String datasetId) {
		this.businessTerm = businessTerm;
		this.description = description;
		this.synonyms = synonyms;
		this.defaultRecall = defaultRecall;
		this.datasetId = datasetId;
		this.agentId = null; // Default to null for backward compatibility
	}

	public BusinessKnowledgeDTO(String businessTerm, String description, String synonyms, Boolean defaultRecall,
			String datasetId, String agentId) {
		this.businessTerm = businessTerm;
		this.description = description;
		this.synonyms = synonyms;
		this.defaultRecall = defaultRecall;
		this.datasetId = datasetId;
		this.agentId = agentId;
	}

	// Getters and Setters

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

}
