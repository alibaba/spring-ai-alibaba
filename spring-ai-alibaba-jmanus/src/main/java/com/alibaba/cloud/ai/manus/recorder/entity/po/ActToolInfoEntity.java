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
package com.alibaba.cloud.ai.manus.recorder.entity.po;

import jakarta.persistence.*;

/**
 * Entity class for storing action tool information. Records details about tools used
 * during action execution phases.
 *
 * This entity is used to track: - Tool name and parameters - Execution results - Tool
 * identification
 */
@Entity
@Table(name = "act_tool_info")
public class ActToolInfoEntity {

	// Unique identifier for the tool call
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	// Tool name
	@Column(name = "name")
	private String name;

	// Tool parameters (serialized)
	@Column(name = "parameters", columnDefinition = "LONGTEXT")
	private String parameters;

	// Result of tool execution
	@Column(name = "result", columnDefinition = "LONGTEXT")
	private String result;

	// Tool call id
	@Column(name = "tool_call_id")
	private String toolCallId;

	/**
	 * Constructor with required fields
	 * @param name Tool name
	 * @param parameters Tool parameters
	 */
	public ActToolInfoEntity(String name, String parameters, String toolCallId) {
		this.name = name;
		this.parameters = parameters;
		this.toolCallId = toolCallId;
	}

	/**
	 * Default constructor required by Hibernate/JPA
	 */
	public ActToolInfoEntity() {
	}

	// Getters and Setters

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getParameters() {
		return parameters;
	}

	public void setParameters(String parameters) {
		this.parameters = parameters;
	}

	public String getResult() {
		return result;
	}

	public void setResult(String result) {
		this.result = result;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getToolCallId() {
		return toolCallId;
	}

	public void setToolCallId(String toolCallId) {
		this.toolCallId = toolCallId;
	}

	@Override
	public String toString() {
		return "ActToolInfoEntity{" + "name='" + name + '\'' + ", parameters='" + parameters + '\'' + ", result='"
				+ result + '\'' + ", id='" + id + '\'' + '}';
	}

}
