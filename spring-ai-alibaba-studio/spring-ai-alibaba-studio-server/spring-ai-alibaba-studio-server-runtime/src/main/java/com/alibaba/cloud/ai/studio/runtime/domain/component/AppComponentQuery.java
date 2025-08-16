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

import com.alibaba.cloud.ai.studio.runtime.domain.BaseQuery;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * Query parameters for application component operations
 *
 * @author guning.lt
 * @since 1.0.0.3
 */

@EqualsAndHashCode(callSuper = true)
@Data
public class AppComponentQuery extends BaseQuery {

	/** Component code */
	private String code;

	/** List of component codes */
	private List<String> codes;

	/** Application name */
	@JsonProperty("app_name")
	private String appName;

	/** Component type */
	private String type;

	/** Application ID */
	@JsonProperty("app_id")
	private String appId;

	/** Component configuration */
	private String config;

	/** Component description */
	private String description;

	/** Component status */
	private Integer status;

}
