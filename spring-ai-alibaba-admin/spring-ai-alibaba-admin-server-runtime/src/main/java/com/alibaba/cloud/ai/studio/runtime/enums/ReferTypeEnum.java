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

package com.alibaba.cloud.ai.studio.runtime.enums;

import lombok.Getter;

/**
 * Enum representing different types of references in the system
 *
 * @author guning.lt
 * @since 1.0.0.3
 */
@Getter
public enum ReferTypeEnum {

	/**
	 * Main entity type: Agent
	 */
	MAIN_TYPE_AGENT(2),

	/**
	 * Main entity type: Flow
	 */
	MAIN_TYPE_FLOW(3),

	/**
	 * Referenced entity type: Plugin
	 */
	REFER_TYPE_PLUGIN(10),

	/**
	 * Referenced entity type: Component Agent
	 */
	REFER_TYPE_COMPONENT_AGENT(20),

	/**
	 * Referenced entity type: Component Workflow
	 */
	REFER_TYPE_COMPONENT_WORKFLOW(30);

	/**
	 * Type identifier
	 */
	private final Integer type;

	ReferTypeEnum(Integer type) {
		this.type = type;
	}

}
