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
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

/**
 * Query parameters for MCP (Model Control Panel) operations
 *
 * @author guning.lt
 * @since 1.0.0.3
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class McpQuery {

	/** Whether tools are needed */
	@JsonProperty("need_tools")
	private Boolean needTools = false;

	/** List of server codes to query */
	@JsonProperty("server_codes")
	private List<String> serverCodes;

	/** Query status */
	private Integer status;

	/** Query name */
	private String name;

	/** Current page number */
	private Integer current = 1;

	/** Page size */
	private Integer size = 10;

}
