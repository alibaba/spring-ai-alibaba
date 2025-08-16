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

package com.alibaba.cloud.ai.studio.runtime.domain.component;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * Custom parameter model for application components and plugins
 *
 * @author guning.lt
 * @since 1.0.0.3
 **/
@Data
public class CustomParam implements Serializable {

	/** Unique identifier code */
	private String code;

	/** Display name */
	private String name;

	/** Detailed description */
	private String description;

	/** Parameter type */
	private String type;

	/** List of parameter definitions */
	private List<Param> params;

	/** Additional information in JSON format */
	private String extendInfo;

	/**
	 * Parameter definition class
	 */
	@Data
	public static class Param {

		/** Field name */
		private String field;

		/** Data type */
		private String type;

		/** Parameter value */
		private Object value;

		/** Field description */
		private String description;

		/** Whether the field is required */
		private boolean required;

	}

	/**
	 * Parameter type enumeration
	 */
	public enum ParamType {

		flow, plugin, agentComponent, workflowComponent

	}

}
