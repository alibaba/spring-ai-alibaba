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

package com.alibaba.cloud.ai.studio.runtime.domain.app;

import com.alibaba.cloud.ai.studio.runtime.enums.AppStatus;
import com.alibaba.cloud.ai.studio.runtime.enums.AppType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;

/**
 * Represents an application in the Spring AI Alibaba Studio system.
 *
 * @since 1.0.0.3
 */
@Data
public class Application implements Serializable {

	/** Unique identifier for the application */
	@JsonProperty("app_id")
	private String appId;

	/** Name of the application */
	private String name;

	/** Description of the application */
	private String description;

	/** Type of the application */
	private AppType type;

	/** Current status of the application */
	private AppStatus status;

	/** Raw configuration string */
	@JsonIgnore
	private String configStr;

	/** Raw public configuration string */
	@JsonIgnore
	private String pubConfigStr;

	/** Application configuration map */
	private Map<String, Object> config;

	/** published configuration map */
	@JsonProperty("pub_config")
	private Map<String, Object> pubConfig;

	/** Application icon */
	private String icon;

	/** Source of the application, defaults to "console" */
	private String source = "console";

	/** Creation timestamp */
	@JsonProperty("gmt_create")
	private Date gmtCreate;

	/** Last modification timestamp */
	@JsonProperty("gmt_modified")
	private Date gmtModified;

}
