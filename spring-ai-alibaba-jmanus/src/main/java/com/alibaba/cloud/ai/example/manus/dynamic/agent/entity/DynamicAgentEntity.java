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
package com.alibaba.cloud.ai.example.manus.dynamic.agent.entity;

import java.util.List;

import com.alibaba.cloud.ai.example.manus.dynamic.model.entity.DynamicModelEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "dynamic_agents")
public class DynamicAgentEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true)
	private String agentName;

	@Column(nullable = false, length = 1000)
	private String agentDescription;

	@Column(nullable = true, length = 40000)
	@Deprecated
	private String systemPrompt = "";

	@Column(nullable = false, length = 40000)
	private String nextStepPrompt;

	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(name = "dynamic_agent_tools", joinColumns = @JoinColumn(name = "agent_id"))
	@Column(name = "tool_key")
	private List<String> availableToolKeys;

	@Column(nullable = false)
	private String className;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "model_id")
	private DynamicModelEntity model;

	@Column(nullable = true)
	private String namespace;

	// Getters and Setters
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getAgentName() {
		return agentName;
	}

	public void setAgentName(String agentName) {
		this.agentName = agentName;
	}

	public String getAgentDescription() {
		return agentDescription;
	}

	public void setAgentDescription(String agentDescription) {
		this.agentDescription = agentDescription;
	}

	/**
	 * This is no longer used, merge the two parts into one nextStepPrompt. The current
	 * implementation will ignore this content.
	 * @return
	 */
	@Deprecated
	public String getSystemPrompt() {
		return systemPrompt;
	}

	/**
	 * This is no longer used, merge the two parts into one nextStepPrompt. The current
	 * implementation will ignore this content.
	 * @return
	 */
	@Deprecated
	public void setSystemPrompt(String systemPrompt) {
		this.systemPrompt = systemPrompt;
	}

	public String getNextStepPrompt() {
		return nextStepPrompt;
	}

	public void setNextStepPrompt(String nextStepPrompt) {
		this.nextStepPrompt = nextStepPrompt;
	}

	public List<String> getAvailableToolKeys() {
		return availableToolKeys;
	}

	public void setAvailableToolKeys(List<String> availableToolKeys) {
		this.availableToolKeys = availableToolKeys;
	}

	public String getClassName() {
		return className;
	}

	public void setClassName(String className) {
		this.className = className;
	}

	public DynamicModelEntity getModel() {
		return model;
	}

	public void setModel(DynamicModelEntity model) {
		this.model = model;
	}

	public String getNamespace() {
		return namespace;
	}

	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}

}
