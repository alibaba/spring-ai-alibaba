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

package com.alibaba.cloud.ai.studio.runtime.domain.mcp;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * Response model for MCP server tool calls
 *
 * @author guning.lt
 * @since 1.0.0.3
 */
@Data
public class McpServerCallToolResponse implements Serializable {

	/**
	 * Indicates if the tool call resulted in an error
	 */
	@JsonProperty("is_error")
	private Boolean isError;

	/**
	 * List of content items returned by the tool call
	 */
	private List<Content> content;

}
