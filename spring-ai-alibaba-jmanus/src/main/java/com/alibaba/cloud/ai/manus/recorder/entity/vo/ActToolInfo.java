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
package com.alibaba.cloud.ai.manus.recorder.entity.vo;

/**
 * Standalone class for storing action tool information. Records details about tools used
 * during action execution phases.
 *
 * This class was previously an inner class of ThinkActRecord and has been extracted to
 * provide better reusability and cleaner architecture.
 *
 * This class is used to track: - Tool name and parameters - Execution results - Tool
 * identification
 */
public class ActToolInfo {

	// Tool name
	private String name;

	// Tool parameters (serialized)
	private String parameters;

	// Result of tool execution
	private String result;

	// Unique identifier for the tool call
	private String id;

	/**
	 * Default constructor
	 */
	public ActToolInfo() {
	}

	/**
	 * Constructor with required fields
	 * @param name Tool name
	 * @param parameters Tool parameters
	 * @param id Tool call ID
	 */
	public ActToolInfo(String name, String parameters, String id) {
		this.name = name;
		this.parameters = parameters;
		this.id = id;
	}

	/**
	 * Constructor with name and parameters only
	 * @param name Tool name
	 * @param parameters Tool parameters
	 */
	public ActToolInfo(String name, String parameters) {
		this.name = name;
		this.parameters = parameters;
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

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	@Override
	public String toString() {
		return "ActToolInfo{" + "name='" + name + '\'' + ", parameters='" + parameters + '\'' + ", result='" + result
				+ '\'' + ", id='" + id + '\'' + '}';
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;

		ActToolInfo that = (ActToolInfo) o;

		if (name != null ? !name.equals(that.name) : that.name != null)
			return false;
		if (parameters != null ? !parameters.equals(that.parameters) : that.parameters != null)
			return false;
		if (result != null ? !result.equals(that.result) : that.result != null)
			return false;
		return id != null ? id.equals(that.id) : that.id == null;
	}

	@Override
	public int hashCode() {
		int result1 = name != null ? name.hashCode() : 0;
		result1 = 31 * result1 + (parameters != null ? parameters.hashCode() : 0);
		result1 = 31 * result1 + (result != null ? result.hashCode() : 0);
		result1 = 31 * result1 + (id != null ? id.hashCode() : 0);
		return result1;
	}

}
