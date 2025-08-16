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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Configuration for MCP server deployment
 *
 * @author guning.lt
 * @since 1.0.0.3
 */
@Data
public class McpServerDeployConfig implements Serializable {

	/** Startup command for the server */
	private List<String> startCmd;

	/** Installation script for npx package or corresponding value */
	@JsonProperty("install_config")
	private String installConfig;

	/** List of environment variable names */
	private List<String> envs;

	/** Map of environment variable values */
	private Map<String, String> envValue;

	/** Authentication key for server access */
	private String authorization;

	/** Remote server address */
	@JsonProperty("remote_address")
	private String remoteAddress;

	/** Remote endpoint path */
	@JsonProperty("remote_endpoint")
	private String remoteEndpoint;

	/** Headers for SSE (Server-Sent Events) connection */
	private HashMap<String, String> remoteHeader;

}
