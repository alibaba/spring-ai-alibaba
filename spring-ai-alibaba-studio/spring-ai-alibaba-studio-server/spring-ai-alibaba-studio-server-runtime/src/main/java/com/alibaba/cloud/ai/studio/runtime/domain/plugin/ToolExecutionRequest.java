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

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Represents a request for tool execution in the agent system.
 *
 * @since 1.0.0.3
 */

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ToolExecutionRequest {

	/** Unique identifier for the request */
	@JsonProperty("request_id")
	private String requestId;

	/** Identifier of the plugin that contains the tool */
	@JsonProperty("plugin_id")
	private String pluginId;

	/** Identifier of the tool to be executed */
	@JsonProperty("tool_id")
	private String toolId;

	/** The tool instance to be executed */
	private Tool tool;

	/** Arguments to be passed to the tool execution */
	@JsonProperty("arguments")
	private Map<String, Object> arguments;

}
