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

package com.alibaba.cloud.ai.studio.runtime.domain.account;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;

/**
 * Represents a workspace entity in the system.
 *
 * @since 1.0.0.3
 */
@Data
public class Workspace implements Serializable {

	/** Unique identifier of the workspace */
	@JsonProperty("workspace_id")
	private String workspaceId;

	/** Name of the workspace */
	private String name;

	/** Description of the workspace */
	private String description;

	/** Configuration settings for the workspace */
	private Map<String, Object> config;

	/** Associated account identifier */
	@JsonProperty("account_id")
	private String accountId;

	/** Creation timestamp */
	@JsonProperty("gmt_create")
	private Date gmtCreate;

	/** Last modification timestamp */
	@JsonProperty("gmt_modified")
	private Date gmtModified;

	/** Username of the creator */
	private String creator;

	/** Username of the last modifier */
	private String modifier;

}
