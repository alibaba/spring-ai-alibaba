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

import com.alibaba.cloud.ai.studio.runtime.domain.BaseQuery;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

/**
 * Query model for tool-related operations.
 *
 * @since 1.0.0.3
 */

@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ToolQuery extends BaseQuery {

	/** ID of the plugin */
	@JsonProperty("plugin_id")
	private String pluginId;

	/** List of tool IDs */
	@JsonProperty("tool_ids")
	private List<String> toolIds;

	/** Flag to indicate if all fields should be returned */
	@JsonProperty("full_fields")
	private Boolean fullFields = false;

}
