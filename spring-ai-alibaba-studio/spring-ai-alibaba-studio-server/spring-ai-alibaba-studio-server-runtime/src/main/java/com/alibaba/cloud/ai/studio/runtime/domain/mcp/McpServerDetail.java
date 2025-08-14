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

import com.alibaba.cloud.ai.studio.runtime.enums.McpInstallTypeEnum;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * MCP server detail information
 *
 * @author guning.lt
 * @since 1.0.0.3
 **/
@Data
public class McpServerDetail implements Serializable {

	/** Server code identifier */
	@JsonProperty("server_code")
	private String serverCode;

	/** Server name */
	private String name;

	/** Deployment configuration */
	@JsonProperty("deploy_config")
	private String deployConfig;

	/** Detailed configuration */
	@JsonProperty("detail_config")
	private String detailConfig;

	/** Server status */
	private Integer status;

	/** Server type */
	private String type;

	/** Business type */
	@JsonProperty("biz_type")
	private String bizType;

	/** Description */
	private String description;

	/** Installation method, defaults to SSE */
	@JsonProperty("install_type")
	private String installType = McpInstallTypeEnum.SSE.name();

	/** List of available tools */
	private List<McpTool> tools;

	/** Deployment environment */
	@JsonProperty("deploy_env")
	private String deployEnv;

	/** Source information */
	private String source;

	/** Flag indicating if tools are required */
	@JsonProperty("need_tools")
	private Boolean needTools = false;

	/** Last modification timestamp */
	@JsonProperty("gmt_modified")
	private Date gmtModified;

}
