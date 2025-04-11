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
package com.alibaba.cloud.ai.model;

/**
 * VariableSelector is the reference of a variable in State.
 */
public class VariableSelector {

	/**
	 * An isolation domain of the variable, Could be the node id.
	 */
	private String namespace;

	/**
	 * Name of the variable.
	 */
	private String name;

	/**
	 * Label of the variable.
	 */
	private String label;

	public VariableSelector() {
	}

	/**
	 * Only namespace and name is required for a valid selector.
	 * @param namespace An isolation domain of the variable
	 * @param name Name of the variable
	 */
	public VariableSelector(String namespace, String name) {
		this.namespace = namespace;
		this.name = name;
	}

	public VariableSelector(String namespace, String name, String label) {
		this.namespace = namespace;
		this.name = name;
		this.label = label;
	}

	public String getNamespace() {
		return namespace;
	}

	public VariableSelector setNamespace(String namespace) {
		this.namespace = namespace;
		return this;
	}

	public String getName() {
		return name;
	}

	public VariableSelector setName(String name) {
		this.name = name;
		return this;
	}

	public String getLabel() {
		return label;
	}

	public VariableSelector setLabel(String label) {
		this.label = label;
		return this;
	}

}
