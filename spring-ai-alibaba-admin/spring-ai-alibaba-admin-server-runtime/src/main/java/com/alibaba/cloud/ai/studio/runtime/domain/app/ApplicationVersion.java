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
package com.alibaba.cloud.ai.studio.runtime.domain.app;

import com.alibaba.cloud.ai.studio.runtime.enums.AppStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * Represents a version of an application in the workspace
 */
@Data
public class ApplicationVersion implements Serializable {

	/** Workspace identifier */
	@JsonProperty("workspace_id")
	private String workspaceId;

	/** Application identifier */
	@JsonProperty("app_id")
	private String appId;

	/** Current status of the application version */
	private AppStatus status;

	/** Application configuration */
	private String config;

	/** Version number, etc 1, 2, 3 */
	private String version;

	/** Version description */
	private String description;

	/** Creation timestamp */
	@JsonProperty("gmt_create")
	private Date gmtCreate;

	/** Last modification timestamp */
	@JsonProperty("gmt_modified")
	private Date gmtModified;

	/** Creator's identifier */
	private String creator;

	/** Last modifier's identifier */
	private String modifier;

}
