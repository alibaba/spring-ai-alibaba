/*
 * Copyright 2024-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alibaba.cloud.ai.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * VariableSelector is the reference of a variable in State.
 */
@Data
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
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

	/**
	 * Only namespace and name is required for a valid selector.
	 * @param namespace An isolation domain of the variable
	 * @param name Name of the variable
	 */
	public VariableSelector(String namespace, String name) {
		this.namespace = namespace;
		this.name = name;
	}

}
