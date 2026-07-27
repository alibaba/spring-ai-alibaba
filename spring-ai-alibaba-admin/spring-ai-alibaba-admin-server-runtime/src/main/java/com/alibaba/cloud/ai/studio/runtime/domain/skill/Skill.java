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

package com.alibaba.cloud.ai.studio.runtime.domain.skill;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

/**
 * Agent skill domain model.
 *
 * @since 1.0.0.3
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Skill implements Serializable {

	@JsonProperty("skill_id")
	private String skillId;

	private String name;

	private String description;

	@JsonProperty("skill_name")
	private String skillName;

	@JsonProperty("storage_path")
	private String storagePath;

	private String source = "user";

	@JsonProperty("gmt_create")
	private Date gmtCreate;

	@JsonProperty("gmt_modified")
	private Date gmtModified;

}
