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

package com.alibaba.cloud.ai.studio.runtime.domain.plugin;

import com.alibaba.cloud.ai.studio.runtime.enums.ToolStatus;
import com.alibaba.cloud.ai.studio.runtime.enums.ToolTestStatus;
import com.alibaba.cloud.ai.studio.runtime.domain.tool.ApiParameter;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * Plugin tool model class.
 *
 * @since 1.0.0.3
 */

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Tool {

	@JsonProperty("plugin_id")
	private String pluginId;

	@JsonProperty("tool_id")
	private String toolId;

	private Plugin plugin;

	/**
	 * Tool name
	 */
	private String name;

	/**
	 * Tool description
	 */
	private String description;

	private ToolConfig config;

	@JsonProperty("api_schema")
	private String apiSchema;

	private Boolean enabled;

	@JsonProperty("test_status")
	private ToolTestStatus testStatus;

	private ToolStatus status;

	@JsonProperty("gmt_create")
	private Date gmtCreate;

	@JsonProperty("gmt_modified")
	private Date gmtModified;

	/**
	 * Whether all tool parameters are required
	 */
	@JsonProperty("all_tool_param")
	private boolean allToolParam;

	/**
	 * Configuration for the tool
	 */
	@Data
	public static class ToolConfig implements Serializable {

		/**
		 * Tool path
		 */
		private String path;

		private String server;

		/**
		 * HTTP request method
		 */
		@JsonProperty("request_method")
		private String requestMethod;

		/**
		 * Content type for form submission (application/json or
		 * application/x-www-form-urlencoded)
		 */
		@JsonProperty("content_type")
		private String contentType;

		/**
		 * Input parameters
		 */
		@JsonProperty("input_params")
		private List<ApiParameter> inputParams;

		/**
		 * Output parameters
		 */
		@JsonProperty("output_params")
		private List<ApiParameter> outputParams;

		/**
		 * Usage examples
		 */
		private List<ToolExample> examples;

	}

}
