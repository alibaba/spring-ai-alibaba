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

package com.alibaba.cloud.ai.manus.coordinator.entity.vo;

import java.util.List;

/**
 * CoordinatorConfigVO Output Data Structure
 */
public class CoordinatorConfigVO {

	private String id; // Configuration ID, corresponds to planId

	private String name; // Configuration name, corresponds to title

	private String description; // Configuration description, corresponds to userRequest

	private String endpoint; // Endpoint address

	private List<CoordinatorParameterVO> parameters; // Parameter list

	// Constructor
	public CoordinatorConfigVO() {
	}

	public CoordinatorConfigVO(String id, String name, String description, List<CoordinatorParameterVO> parameters) {
		this.id = id;
		this.name = name;
		this.description = description;
		this.parameters = parameters;
	}

	public CoordinatorConfigVO(String id, String name, String description, String endpoint,
			List<CoordinatorParameterVO> parameters) {
		this.id = id;
		this.name = name;
		this.description = description;
		this.endpoint = endpoint;
		this.parameters = parameters;
	}

	// Getter and Setter methods
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getEndpoint() {
		return endpoint;
	}

	public void setEndpoint(String endpoint) {
		this.endpoint = endpoint;
	}

	public List<CoordinatorParameterVO> getParameters() {
		return parameters;
	}

	public void setParameters(List<CoordinatorParameterVO> parameters) {
		this.parameters = parameters;
	}

	@Override
	public String toString() {
		return "CoordinatorConfigVO{" + "id='" + id + '\'' + ", name='" + name + '\'' + ", description='" + description
				+ '\'' + ", endpoint='" + endpoint + '\'' + ", parameters=" + parameters + '}';
	}

}
