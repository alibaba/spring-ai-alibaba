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
package com.alibaba.cloud.ai.example.manus.dynamic.model.entity;

import com.alibaba.cloud.ai.example.manus.dynamic.agent.entity.DynamicAgentEntity;
import com.alibaba.cloud.ai.example.manus.dynamic.model.model.vo.ModelConfig;
import jakarta.persistence.*;

import java.util.List;
import java.util.Map;

@Entity
@Table(name = "dynamic_models")
public class DynamicModelEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private String baseUrl;

	@Column(nullable = false)
	private String apiKey;

	@Convert(converter = MapToStringConverter.class)
	@Column(columnDefinition = "VARCHAR")
	private Map<String, String> headers;

	@Column(nullable = false)
	private String modelName;

	@Column(nullable = false, length = 1000)
	private String modelDescription;

	@Column(nullable = false)
	private String type;

	@Column(nullable = false, columnDefinition = "boolean default false")
	private boolean allowChange;

	@OneToMany(mappedBy = "model")
	private List<DynamicAgentEntity> agents;

	public DynamicModelEntity() {
	}

	public DynamicModelEntity(Long id) {
		this.id = id;
	}

	// Getters and Setters
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getBaseUrl() {
		return baseUrl;
	}

	public void setBaseUrl(String baseUrl) {
		this.baseUrl = baseUrl;
	}

	public String getApiKey() {
		return apiKey;
	}

	public void setApiKey(String apiKey) {
		this.apiKey = apiKey;
	}

	public String getModelName() {
		return modelName;
	}

	public void setModelName(String modelName) {
		this.modelName = modelName;
	}

	public String getModelDescription() {
		return modelDescription;
	}

	public void setModelDescription(String modelDescription) {
		this.modelDescription = modelDescription;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public List<DynamicAgentEntity> getAgents() {
		return agents;
	}

	public void setAgents(List<DynamicAgentEntity> agents) {
		this.agents = agents;
	}

	public Map<String, String> getHeaders() {
		return headers;
	}

	public void setHeaders(Map<String, String> headers) {
		this.headers = headers;
	}

	public Boolean isAllowChange() {
		return allowChange;
	}

	public void setAllowChange(Boolean allowChange) {
		this.allowChange = allowChange;
	}

	public ModelConfig mapToModelConfig() {
		ModelConfig config = new ModelConfig();
		config.setId(this.getId());
		config.setHeaders(this.getHeaders());
		config.setBaseUrl(this.getBaseUrl());
		config.setApiKey(maskValue(this.getApiKey()));
		config.setModelName(this.getModelName());
		config.setModelDescription(this.getModelDescription());
		config.setType(this.getType());
		return config;
	}

	/**
	 * Obscures the string, keeping the first 4 and last 4 characters visible, replacing
	 * the rest with asterisks (*)
	 */
	private String maskValue(String value) {
		if (value == null || value.length() <= 8) {
			return "*";
		}
		int length = value.length();
		String front = value.substring(0, 4);
		String end = value.substring(length - 4);
		return front + "*".repeat(length - 8) + end;
	}

}
