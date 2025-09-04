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
package com.alibaba.cloud.ai.studio.admin.generator.model;

import java.util.Map;

/**
 * Variable is often used to represent the output of a node, or pre-defined variables in
 * an App.
 */
public class Variable {

	private String name;

	private Object value;

	private VariableType valueType;

	private String description;

	private Map<String, Object> extraProperties;

	private Strategy variableStrategy = Strategy.REPLACE;

	public enum Strategy {

		REPLACE("KeyStrategy.REPLACE"), APPEND("KeyStrategy.APPEND"), MERGE("KeyStrategy.MERGE");

		private final String code;

		public String getCode() {
			return code;
		}

		Strategy(String code) {
			this.code = code;
		}

	}

	public Variable() {
	}

	/**
	 * Only name and valueType is required
	 * @param name a valid variable name
	 * @param valueType a {@link VariableType} value
	 */
	public Variable(String name, VariableType valueType) {
		this.name = name;
		this.valueType = valueType;
	}

	public String getName() {
		return name;
	}

	public Variable setName(String name) {
		this.name = name;
		return this;
	}

	public Object getValue() {
		return value;
	}

	public Variable setValue(Object value) {
		this.value = value;
		return this;
	}

	public VariableType getValueType() {
		return valueType;
	}

	public Variable setValueType(VariableType valueType) {
		this.valueType = valueType;
		return this;
	}

	public String getDescription() {
		return description;
	}

	public Variable setDescription(String description) {
		this.description = description;
		return this;
	}

	public Map<String, Object> getExtraProperties() {
		return extraProperties;
	}

	public Variable setExtraProperties(Map<String, Object> extraProperties) {
		this.extraProperties = extraProperties;
		return this;
	}

	public Variable setVariableStrategy(Strategy variableStrategy) {
		this.variableStrategy = variableStrategy;
		return this;
	}

	public Strategy getVariableStrategy() {
		return variableStrategy;
	}

}
